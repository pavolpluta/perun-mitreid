package cz.muni.ics.oidc.server.claims;

import java.util.Properties;

/**
 * Context for initializing ClaimValueSources.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class ClaimSourceInitContext {

	private final String propertyPrefix;
	private final Properties properties;

	public ClaimSourceInitContext(String propertyPrefix, Properties properties) {
		this.propertyPrefix = propertyPrefix;
		this.properties = properties;
	}

	public String getProperty(String suffix, String defaultValue) {
		return properties.getProperty(propertyPrefix + "." + suffix, defaultValue);
	}
}
