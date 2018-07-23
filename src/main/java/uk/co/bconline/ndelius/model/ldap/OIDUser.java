package uk.co.bconline.ndelius.model.ldap;

import lombok.*;
import org.springframework.ldap.odm.annotations.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import uk.co.bconline.ndelius.model.ldap.projections.OIDUserHomeArea;

import javax.naming.Name;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(exclude = "password")
@Entry(objectClasses = {"NDUser", "person", "top"}, base="cn=Users")
public final class OIDUser implements OIDUserHomeArea, UserDetails, Serializable
{
	@Id
	private Name dn;

	@Setter
	@Attribute(name="cn")
	@DnAttribute(value="cn", index=1)
	private String username;

	@Attribute(name="givenName")
	private String forenames;

	@Attribute(name="sn")
	private String surname;

	@Attribute(name="userHomeArea")
	private String homeArea;

	@Attribute(name="userPassword")
	private String password;

	@Setter
	@Transient
	private List<OIDBusinessTransaction> transactions;

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities()
	{
		return null;
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