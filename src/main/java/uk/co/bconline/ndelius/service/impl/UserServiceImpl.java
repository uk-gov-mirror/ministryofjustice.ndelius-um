package uk.co.bconline.ndelius.service.impl;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.co.bconline.ndelius.exception.AppException;
import uk.co.bconline.ndelius.model.SearchResult;
import uk.co.bconline.ndelius.model.User;
import uk.co.bconline.ndelius.model.entity.UserEntity;
import uk.co.bconline.ndelius.model.ldap.OIDUser;
import uk.co.bconline.ndelius.service.DBUserService;
import uk.co.bconline.ndelius.service.DatasetService;
import uk.co.bconline.ndelius.service.UserService;
import uk.co.bconline.ndelius.transformer.SearchResultTransformer;
import uk.co.bconline.ndelius.transformer.UserTransformer;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.time.LocalDate.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.*;
import static java.util.stream.Collectors.toList;
import static org.springframework.core.NestedExceptionUtils.getMostSpecificCause;
import static uk.co.bconline.ndelius.util.AuthUtils.isNational;
import static uk.co.bconline.ndelius.util.AuthUtils.myUsername;

@Slf4j
@Service
public class UserServiceImpl implements UserService
{
	private final DBUserService dbService;
	private final OIDUserDetailsService oidService;
	private final DatasetService datasetService;
	private final UserTransformer transformer;
	private final SearchResultTransformer searchResultTransformer;

	@Autowired
	public UserServiceImpl(
			DBUserService dbService,
			OIDUserDetailsService oidService,
			DatasetService datasetService,
			UserTransformer transformer,
			SearchResultTransformer searchResultTransformer)
	{
		this.dbService = dbService;
		this.oidService = oidService;
		this.datasetService = datasetService;
		this.transformer = transformer;
		this.searchResultTransformer = searchResultTransformer;
	}

	@Override
	public List<SearchResult> search(String query, int page, int pageSize, boolean includeInactiveUsers)
	{
		if (StringUtils.isEmpty(query) || query.length() < 3) return emptyList();

		Set<String> myDatasets = new HashSet<>();
		if (!isNational()) {
			// We only need to filter on datasets for non-national (local) users, so don't bother fetching them for national users
			myDatasets.addAll(datasetService.getDatasetCodes(myUsername()));
			myDatasets.add(oidService.getUserHomeArea(myUsername()));
		}

		val dbFuture = supplyAsync(() -> dbService.search(query, includeInactiveUsers, myDatasets));
		val oidFuture = supplyAsync(() -> oidService.search(query, includeInactiveUsers, myDatasets));

		try
		{
			return allOf(oidFuture, dbFuture)
					.thenApply(v -> Stream.of(oidFuture.join(), dbFuture.join())
							.flatMap(Collection::stream)
							.collect(HashMap<String, SearchResult>::new, (map, result) -> {
								map.put(result.getUsername(), ofNullable(map.get(result.getUsername()))
										.map(r -> searchResultTransformer.reduce(r, result))
										.orElse(result));
							}, HashMap::putAll)
							.values()
							.stream()
							.filter(result -> includeInactiveUsers || result.getEndDate() == null || !result.getEndDate().isBefore(now()))
							.sorted(comparing(SearchResult::getScore, Float::compare).reversed())
							.skip((long) (page-1) * pageSize)
							.limit(pageSize)
							.peek(result -> log.debug("SearchResult: username={}, score={}, endDate={}", result.getUsername(), result.getScore(), result.getEndDate()))
							.collect(toList()))
					.get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			throw new AppException(String.format("Unable to complete user search for %s", query), e);
		}
	}

	@Override
	public boolean usernameExists(String username)
	{
		val dbFuture = supplyAsync(() -> dbService.usernameExists(username));
		val oidFuture = supplyAsync(() -> oidService.usernameExists(username));

		try
		{
			return allOf(dbFuture, oidFuture)
					.thenApply(v -> dbFuture.join() || oidFuture.join()).get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			throw new AppException(String.format("Unable to check whether user exists with username %s", username), e);
		}
	}

	@Override
	public Optional<User> getUser(String username)
	{
		val dbFuture = supplyAsync(() -> dbService.getUser(username).orElse(null));
		val oidFuture = supplyAsync(() -> oidService.getUser(username).orElse(null));

		try
		{
			val datasetsFilter = datasetsFilter();
			return allOf(dbFuture, oidFuture)
					.thenApply(v -> transformer.combine(dbFuture.join(), oidFuture.join())).get()
					.filter(user -> datasetsFilter.test(user.getUsername()));
		}
		catch (InterruptedException | ExecutionException e)
		{
			throw new AppException(String.format("Unable to retrieve user details for %s", username), e);
		}
	}

	@Override
	public Optional<User> getUserByStaffCode(String staffCode)
	{
		return dbService.getUserByStaffCode(staffCode).flatMap(transformer::map);
	}

	@Override
	public void addUser(User user)
	{
		val dbFuture = runAsync(() -> dbService.save(transformer.mapToUserEntity(user, new UserEntity())));
		val oidFuture = runAsync(() -> oidService.save(transformer.mapToOIDUser(user, new OIDUser())));

		try
		{
			allOf(dbFuture, oidFuture).get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			throw new AppException(String.format("Unable to create user (%s)", getMostSpecificCause(e).getMessage()), e);
		}
	}

	@Override
	public void updateUser(User user)
	{
		val dbFuture = runAsync(() -> {
			log.debug("Fetching existing DB value");
			val existingUser = dbService.getUser(user.getExistingUsername()).orElse(new UserEntity());
			log.debug("Transforming into DB user");
			val updatedUser = transformer.mapToUserEntity(user, existingUser);
			dbService.save(updatedUser);
		});
		val oidFuture = runAsync(() -> {
			log.debug("Fetching existing OID value");
			val existingUser = oidService.getUser(user.getExistingUsername()).orElse(new OIDUser());
			log.debug("Transforming into OID user");
			val updatedUser = transformer.mapToOIDUser(user, existingUser);
			oidService.save(user.getExistingUsername(), updatedUser);
		});

		try
		{
			allOf(dbFuture, oidFuture).get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			throw new AppException(String.format("Unable to update user (%s)", getMostSpecificCause(e).getMessage()), e);
		}
	}

	private Predicate<String> datasetsFilter()
	{
		if (isNational()) return (String username) -> true;

		val myDatasets = datasetService.getDatasetCodes(myUsername());
		myDatasets.add(oidService.getUserHomeArea(myUsername()));

		return (String username) -> {
			val t = LocalDateTime.now();
			val theirDatasets = datasetService.getDatasetCodes(username);
			val theirHomeArea = oidService.getUserHomeArea(username);
			if (theirHomeArea != null) theirDatasets.add(theirHomeArea);
			val r = theirDatasets.isEmpty() || myDatasets.stream().anyMatch(theirDatasets::contains);
			log.trace("--{}ms	Dataset filter", MILLIS.between(t, LocalDateTime.now()));
			return r;
		};
	}
}
