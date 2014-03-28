package ca.corefacility.bioinformatics.irida.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.provider.BaseClientDetails;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.InMemoryClientDetailsService;
import org.springframework.security.oauth2.provider.client.ClientDetailsUserDetailsService;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.InMemoryTokenStore;
import org.springframework.security.oauth2.provider.token.TokenStore;

@Configuration
public class IridaOAuth2Config {

	@Autowired
	private DataSource dataSource;

	@Bean
	public DefaultTokenServices tokenServices() {
		DefaultTokenServices services = new DefaultTokenServices();
		services.setTokenStore(tokenStore());
		services.setSupportRefreshToken(true);
		services.setClientDetailsService(clientDetails());
		return services;
	}

	@Bean
	public TokenStore tokenStore() {
		TokenStore store = new InMemoryTokenStore();
		return store;
	}

	@Bean
	public ClientDetailsUserDetailsService clientDetailsUserDetailsService() {
		ClientDetailsUserDetailsService clientDetailsUserDetailsService = new ClientDetailsUserDetailsService(
				clientDetails());

		return clientDetailsUserDetailsService;
	}

	@Bean
	public ClientDetailsService clientDetails() {
		InMemoryClientDetailsService inMemoryClientDetailsService = new InMemoryClientDetailsService();

		inMemoryClientDetailsService.setClientDetailsStore(clientDetailsList());
		return inMemoryClientDetailsService;
	}

	private Map<String, ClientDetails> clientDetailsList() {
		Map<String, ClientDetails> clientStore = new HashMap<>();

		/*
		 * Add client details here: args:clientId,resourceId,scopes,grant
		 * types,authorities BaseClientDetails thing = new
		 * BaseClientDetails("clientId", "resourceId",
		 * "read,write","authorization_code,refresh_token", "ROLE_CLIENT");
		 * thing.setClientSecret("secret"); clientStore.put("clientId", thing);
		 */

		BaseClientDetails sequencerClient = new BaseClientDetails("sequencer", "NmlIrida", "read,write", "password","ROLE_CLIENT");
		sequencerClient.setClientSecret("sequencerSecret");

		clientStore.put("sequencer", sequencerClient);

		return clientStore;
	}
}
