package cz.muni.ics.oidc.claims;

import java.util.Properties;

/**
 * Context for initializing ClaimValueModifiers.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class ClaimValueModifierInitContext {

	private final String propertyPrefix;
	private final Properties properties;

	public ClaimValueModifierInitContext(String propertyPrefix, Properties properties) {
		this.propertyPrefix = propertyPrefix;
		this.properties = properties;
	}

	public String getPropertyPrefix() {
		return propertyPrefix;
	}

	public Properties getProperties() {
		return properties;
	}
}
