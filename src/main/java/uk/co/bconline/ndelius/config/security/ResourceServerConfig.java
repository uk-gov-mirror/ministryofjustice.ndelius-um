package uk.co.bconline.ndelius.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.co.bconline.ndelius.util.LdapUtils;

import static org.springframework.web.cors.CorsConfiguration.ALL;

@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()
				.mvcMatchers("/api/**").authenticated()
				.and().cors()
				.and().headers().frameOptions().disable()
				.and().csrf().disable();
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
						.allowedMethods(ALL)
						.allowCredentials(true);
			}
		};
	}

	@Override
	public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
		resources.resourceId("NDelius");
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new LdapShaPasswordEncoder()
		{
			@Override
			public boolean matches(CharSequence rawPassword, String encodedPassword)
			{
				return encodedPassword != null && super.matches(rawPassword, LdapUtils.fixPassword(encodedPassword));
			}
		};
	}
}
