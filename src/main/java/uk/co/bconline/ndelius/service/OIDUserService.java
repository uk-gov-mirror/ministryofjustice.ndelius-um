package uk.co.bconline.ndelius.service;

import java.util.List;
import java.util.Optional;

import uk.co.bconline.ndelius.model.OIDUser;

public interface OIDUserService
{
	List<String> getUserRoles(String username);
	List<OIDUser> search(String query, int page, int pageSize);
	Optional<OIDUser> getOIDUser(String username);
}