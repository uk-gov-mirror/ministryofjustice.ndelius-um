package uk.co.bconline.ndelius.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import uk.co.bconline.ndelius.model.ldap.OIDUser;

@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class OIDUserDetailsServiceTest
{
	@Autowired
	private OIDUserDetailsService service;

	@Test
	public void searchUsingInitial()
	{
		List<OIDUser> users = service.search("J Blog", 1, 10);

		assertFalse(users.isEmpty());
		users.forEach(user ->
				assertThat(user.getForenames(), startsWith("J"))
		);
	}

	@Test
	public void retrieveRoles()
	{
		List<String> roles = service.getUserInteractions("test.user");

		assertFalse(roles.isEmpty());
		assertThat(roles, hasItem("UMBI001"));
		assertThat(roles, not(hasItem("UMBI999")));
	}

	@Test
	public void retrieveOIDUser()
	{
		service.getUser("test.user").ifPresent(oidUser -> {
			assertEquals("Test", oidUser.getForenames());
			assertEquals("User", oidUser.getSurname());
		});
	}
}