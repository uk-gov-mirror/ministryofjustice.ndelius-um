package uk.co.bconline.ndelius.model.ldap;

import static java.util.Collections.emptyList;

import java.util.Collection;

import javax.naming.Name;

import org.springframework.ldap.odm.annotations.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(exclude = "password")
@Entry(objectClasses = "user")
public final class ADUser implements UserDetails
{
	@Id
	private Name dn;

	@Setter
	@Attribute(name="sAMAccountName")
	private String username;

	@DnAttribute(value="cn", index=0)
	private String cn;

	@Attribute(name="userPrincipalName")
	private String userPrincipalName;

	@Attribute(name="givenName")
	private String forename;

	@Attribute(name="sn")
	private String surname;

	@Attribute(name="displayName")
	private String displayName;

	@Attribute(name="userAccountControl")
	private String userAccountControl;

	@Transient
	private String password;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities()
	{
		return emptyList();
	}

	@Override
	public boolean isAccountNonExpired()
	{
		return true;
	}

	@Override
	public boolean isAccountNonLocked()
	{
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired()
	{
		return true;
	}

	@Override
	public boolean isEnabled()
	{
		return true;
	}
}