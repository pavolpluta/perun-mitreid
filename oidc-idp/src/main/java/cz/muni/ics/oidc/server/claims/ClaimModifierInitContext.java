package cz.muni.ics.oidc.server.claims;

import java.util.Properties;

/**
 * Context for initializing ClaimModifiers.
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class ClaimModifierInitContext {

	private final String propertyPrefix;
	private final Properties properties;

	public ClaimModifierInitContext(String propertyPrefix, Properties properties) {
		this.propertyPrefix = propertyPrefix;
		this.properties = properties;
	}

	public String getProperty(String suffix, String defaultValue) {
		return properties.getProperty(propertyPrefix + "." + suffix, defaultValue);
	}
}
