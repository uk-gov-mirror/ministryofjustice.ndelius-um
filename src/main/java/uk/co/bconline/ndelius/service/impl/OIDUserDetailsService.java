package uk.co.bconline.ndelius.service.impl;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.co.bconline.ndelius.model.SearchResult;
import uk.co.bconline.ndelius.model.ldap.OIDUser;
import uk.co.bconline.ndelius.model.ldap.OIDUserPreferences;
import uk.co.bconline.ndelius.model.ldap.projections.OIDUserHomeArea;
import uk.co.bconline.ndelius.repository.oid.OIDUserPreferencesRepository;
import uk.co.bconline.ndelius.repository.oid.OIDUserRepository;
import uk.co.bconline.ndelius.service.OIDUserService;
import uk.co.bconline.ndelius.service.UserRoleService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.springframework.ldap.query.LdapQueryBuilder.query;
import static org.springframework.ldap.query.SearchScope.ONELEVEL;
import static uk.co.bconline.ndelius.util.NameUtils.join;

@Slf4j
@Service
public class OIDUserDetailsService implements OIDUserService, UserDetailsService
{
	private static final String USER_BASE = OIDUser.class.getAnnotation(Entry.class).base();

	@Value("${oid.base}")
	private String oidBase;

	private final OIDUserRepository userRepository;
	private final OIDUserPreferencesRepository preferencesRepository;
	private final UserRoleService userRoleService;

	@Autowired
	public OIDUserDetailsService(
			OIDUserRepository userRepository,
			OIDUserPreferencesRepository preferencesRepository,
			UserRoleService userRoleService)
	{
		this.userRepository = userRepository;
		this.preferencesRepository = preferencesRepository;
		this.userRoleService = userRoleService;
	}

	@Override
	public UserDetails loadUserByUsername(String username)
	{
		return getBasicUser(username)
				.orElseThrow(() -> new UsernameNotFoundException(String.format("User '%s' not found", username)));
	}

	@Override
	public boolean usernameExists(String username)
	{
		return userRepository.findByUsername(username).isPresent();
	}

	/**
	 * Search for a list of users with a single text query.
	 *
	 * The search query will be tokenized on space, then each token will be AND matched with wildcards.
	 *
	 * eg.
	 *
	 * "john"		-> (|(givenName=*john*)(sn=*john*)(cn=*john*))
	 * "john smith"	-> (&(|(givenName=*john*)(sn=*john*)(cn=*john*))(|(givenName=*smith*)(sn=*smith*)(cn=*smith*)))
	 *
	 * @param query space-delimited query string
	 * @return a set of matching users from OID
	 */
	@Override
	public List<SearchResult> search(String query)
	{
		AndFilter filter = Stream.of(query.split(" "))
				.map(token -> query().where("givenName").whitespaceWildcardsLike(token)
							.or("sn").whitespaceWildcardsLike(token)
							.or("cn").whitespaceWildcardsLike(token))
				.collect(AndFilter::new, (f, q) -> f.and(q.filter()), AndFilter::and);

		if (log.isDebugEnabled())
		{
			val filterString = filter.encode();
			log.debug("Searching OID: {}", filterString);
		}

		val t = LocalDateTime.now();
		val results = stream(userRepository
				.findAll(query()
						.searchScope(ONELEVEL)
						.base(USER_BASE)
						.filter(filter))
				.spliterator(), true)
				.map(u -> SearchResult.builder()
						.username(u.getUsername())
						.score(deriveScore(query, u))
						.build())
				.collect(toList());
		log.debug("Found {} OID results in {}ms", results.size(), MILLIS.between(t, LocalDateTime.now()));
		return results;
	}

	@Override
	public Optional<OIDUser> getBasicUser(String username)
	{
		val t = LocalDateTime.now();
		val r = userRepository.findByUsername(username);
		log.trace("--{}ms	OID lookup", MILLIS.between(t, LocalDateTime.now()));
		return r;
	}

	@Override
	public Optional<OIDUser> getUser(String username)
	{
		return getBasicUser(username)
				.map(u -> u.toBuilder()
						.roles(userRoleService.getUserRoles(username))
						.build());
	}

	public Optional<String> getUsernameByEmail(String email)
	{
		return userRepository
				.findByEmail(email)
				.map(OIDUser::getUsername);
	}

	@Override
	public String getUserHomeArea(String username)
	{
		return userRepository.getOIDUserHomeAreaByUsername(username).map(OIDUserHomeArea::getHomeArea).orElse(null);
	}

	@Override
	public void save(OIDUser user)
	{
		// Save user
		log.debug("Saving user: {}", user.getUsername());
		userRepository.save(user);

		// Preferences
		log.debug("Checking if user preferences exist");
		if (!preferencesRepository.findOne(query()
				.searchScope(ONELEVEL)
				.base(getDn(user.getUsername()))
				.where("objectclass").isPresent()).isPresent())
		{
			log.debug("Creating user preferences");
			preferencesRepository.save(new OIDUserPreferences(user.getUsername()));
		}

		// Role associations
		userRoleService.updateUserRoles(user.getUsername(), user.getRoles());
	}

	private float deriveScore(String query, OIDUser u)
	{
		return (float) Stream.of(query.split(" "))
				.map(String::toLowerCase)
				.mapToDouble(token -> Stream.of(u.getUsername(), u.getForenames(), u.getSurname())
						.filter(str -> !StringUtils.isEmpty(str))
						.filter(str -> str.toLowerCase().contains(token))
						.mapToDouble(item -> (double) token.length() / item.length())
						.max().orElse(0.0))
				.sum();
	}

	private String getDn(String username)
	{
		return join(",", "cn=" + username, USER_BASE);
	}
}
