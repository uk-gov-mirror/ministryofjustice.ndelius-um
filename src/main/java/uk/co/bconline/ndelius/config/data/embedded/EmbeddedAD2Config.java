package uk.co.bconline.ndelius.config.data.embedded;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.boot.autoconfigure.ldap.embedded.EmbeddedLdapProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.LDAPException;

@Configuration
@EnableConfigurationProperties(EmbeddedLdapProperties.class)
@AutoConfigureBefore(LdapAutoConfiguration.class)
@ConditionalOnClass(InMemoryDirectoryServer.class)
@ConditionalOnProperty("embedded.ad.secondary.base-dn")
public class EmbeddedAD2Config extends AliasDereferencingEmbeddedLdap
{
	@Autowired
	public EmbeddedAD2Config(
			@Qualifier("embeddedAd2Properties") EmbeddedLdapProperties embeddedProperties,
			ConfigurableApplicationContext applicationContext)
	{
		super(embeddedProperties, applicationContext);
	}

	@Bean(name = "ad2DirectoryServer")
	public InMemoryDirectoryServer ad1DirectoryServer() throws LDAPException, IOException
	{
		return super.directoryServer();
	}
}
