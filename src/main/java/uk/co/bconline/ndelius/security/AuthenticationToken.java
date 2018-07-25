package uk.co.bconline.ndelius.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class AuthenticationToken extends AbstractAuthenticationToken
{
	private final Object principal;
	private final Object credentials;

	public AuthenticationToken(UserDetails principal, String credentials)
	{
		super(principal.getAuthorities());
		this.principal = principal;
		this.credentials = credentials;
		setAuthenticated(true);
	}
}
