package cz.muni.ics.oidc.server.claims;

import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import org.mitre.jwt.signer.service.JWTSigningAndValidationService;

import java.util.Properties;

/**
 * Context for initializing ClaimValueSources.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class ClaimSourceInitContext {

	private final PerunOidcConfig perunOidcConfig;
	private final JWTSigningAndValidationService jwtService;
	private final String propertyPrefix;
	private final Properties properties;

	public ClaimSourceInitContext(PerunOidcConfig perunOidcConfig, JWTSigningAndValidationService jwtService, String propertyPrefix, Properties properties) {
		this.perunOidcConfig = perunOidcConfig;
		this.jwtService = jwtService;
		this.propertyPrefix = propertyPrefix;
		this.properties = properties;
	}

	public String getProperty(String suffix, String defaultValue) {
		return properties.getProperty(propertyPrefix + "." + suffix, defaultValue);
	}

	public JWTSigningAndValidationService getJwtService() {
		return jwtService;
	}

	public PerunOidcConfig getPerunOidcConfig() {
		return perunOidcConfig;
	}
}
