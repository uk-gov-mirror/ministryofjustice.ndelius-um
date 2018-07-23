package uk.co.bconline.ndelius.service.impl;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.springframework.ldap.query.LdapQueryBuilder.query;
import static org.springframework.ldap.query.SearchScope.ONELEVEL;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.Filter;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import uk.co.bconline.ndelius.model.ldap.OIDBusinessTransaction;
import uk.co.bconline.ndelius.model.ldap.OIDBusinessTransactionAlias;
import uk.co.bconline.ndelius.model.ldap.OIDUser;
import uk.co.bconline.ndelius.model.ldap.projections.OIDUserHomeArea;
import uk.co.bconline.ndelius.repository.oid.OIDRoleAliasRepository;
import uk.co.bconline.ndelius.repository.oid.OIDRoleRepository;
import uk.co.bconline.ndelius.repository.oid.OIDUserRepository;
import uk.co.bconline.ndelius.service.OIDUserService;

@Slf4j
@Service
public class OIDUserDetailsService implements OIDUserService, UserDetailsService
{
	@Value("${oid.base}")
	private String oidBase;

	private final OIDUserRepository userRepository;
	private final OIDRoleRepository roleRepository;
	private final OIDRoleAliasRepository roleAliasRepository;

	@Autowired
	public OIDUserDetailsService(
			OIDUserRepository userRepository,
			OIDRoleRepository roleRepository,
			OIDRoleAliasRepository roleAliasRepository)
	{
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.roleAliasRepository = roleAliasRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
	{
		return userRepository
				.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException(String.format("User '%s' not found", username)));
	}

	/**
	 * Return a list of business interactions for a given user.
	 *
	 *
	 * In NDelius, the interactions/roles are stored as uibusinessinteraction attributes on NDRole objects within the
	 * ndRoleCatalogue. These are then aliased as sub-entries for each user to assign a set of allowed interactions.
	 *
	 * Note: The directory structure for NDelius in OID is (see schema.ldif for example):
	 * dc=...
	 * 	cn=Users
	 * 		cn=ndRoleCatalogue
	 * 			cn=XXBT001
	 * 			objectclass: NDRole
	 * 			uibusinessinteraction: XXBI001
	 * 			uibusinessinteraction: XXBI002
	 * 			...
	 * 		cn=user1
	 * 			cn=XXBT001
	 * 			objectclass: alias
	 * 			aliasedObjectName: cn=XXBT001,cn=ndRoleCatalogue,cn=Users,dc=...
	 * 			...
	 * 		cn=user2
	 * 			...
	 *
	 * @param username The cn of the user to retrieve the roles for
	 * @return A list of roles eg. [XXBI001, XXBI002]
	 */
	@Override
	public List<String> getUserRoles(String username)
	{
		return getUserTransactions(username).stream()
				.flatMap(bt -> bt.getRoles().stream())
				.collect(toList());
	}

	@Override
	public List<OIDBusinessTransaction> getUserTransactions(String username)
	{
		return stream(roleRepository
				.findAll(query()
						.searchScope(ONELEVEL)
						.base(String.format("cn=%s,%s", username, OIDUser.class.getAnnotation(Entry.class).base()))
						.where("objectclass").like("NDRole*"))
				.spliterator(), false)
				.collect(toList());
	}

	/**
	 * Search for a list of users with a single text query.
	 *
	 * The search query will be tokenized on space, then each token will be AND matched with wildcards. If any token
	 * is a single character, it will be treated as an initial on givenName.
	 *
	 * eg.
	 *
	 * "john"		-> (|(givenName=*john*)(sn=*john*)(cn=*john*))
	 * "john smith"	-> (&(|(givenName=*john*)(sn=*john*)(cn=*john*))(|(givenName=*smith*)(sn=*smith*)(cn=*smith*)))
	 * "J Bloggs"	-> (&(givenName=J*)(|(givenName=*Bloggs*)(sn=*Bloggs*)(cn=*Bloggs*)))
	 *
	 * @param query space-delimited query string
	 * @param page 1-based index of page to return
	 * @param pageSize number of results per page to return
	 * @return a set of matching users from OID
	 */
	@Override
	public List<OIDUser> search(String query, int page, int pageSize)
	{
		Filter filter = Stream.of(query.split(" "))
				.map(token -> token.length() > 1?
						query().where("givenName").whitespaceWildcardsLike(token)
								.or("sn").whitespaceWildcardsLike(token)
								.or("cn").whitespaceWildcardsLike(token):
						query().where("givenName").like(token + "*"))
				.collect(AndFilter::new, (f, q) -> f.and(q.filter()), AndFilter::and);

		log.debug("Searching OID: {}", filter.encode());

		return stream(userRepository
				.findAll(query()
						.base(OIDUser.class.getAnnotation(Entry.class).base())
						.countLimit(pageSize * page)
						.filter(filter))
				.spliterator(), false)
				.skip(pageSize * (page-1))
				.collect(toList());
	}

	@Override
	public Optional<OIDUser> getUser(String username)
	{
		Optional<OIDUser> user = userRepository.findByUsername(username);
		return user.map(u -> {
			u.setTransactions(getUserTransactions(username));
			return u;
		});
	}

	@Override
	public String getUserHomeArea(String username)
	{
		return userRepository.getOIDUserHomeAreaByUsername(username).map(OIDUserHomeArea::getHomeArea).orElse(null);
	}

	@Override
	public void save(OIDUser user)
	{
		userRepository.save(user);

		// TODO for update - remove existing role aliases
//		roleAliasRepository.deleteAll(roleAliasRepository.findAll(query()
//				.searchScope(SearchScope.ONELEVEL)
//				.base(String.format("cn=%s,%s", user.getUsername(), OIDUser.class.getAnnotation(Entry.class).base()))
//				.where("objectclass").is("alias")));

		val roleAliases = user.getTransactions().stream()
				.map(OIDBusinessTransaction::getName)
				.map(name -> OIDBusinessTransactionAlias.builder()
						.name(name)
						.username(user.getUsername())
						.aliasedObjectName(String.format("cn=%s,%s,%s", name,
								OIDBusinessTransaction.class.getAnnotation(Entry.class).base(), oidBase))
						.build())
				.collect(toList());
		roleAliasRepository.saveAll(roleAliases);
	}
}
