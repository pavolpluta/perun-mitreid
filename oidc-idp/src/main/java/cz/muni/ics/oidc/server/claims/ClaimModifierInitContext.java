package cz.muni.ics.oidc.server.claims;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Context for initializing ClaimModifiers.
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class ClaimModifierInitContext {

	private static final Logger log = LoggerFactory.getLogger(ClaimModifierInitContext.class);

	private final String propertyPrefix;
	private final Properties properties;
	private final String claimName;

	public ClaimModifierInitContext(String propertyPrefix, Properties properties, String claimName) {
		this.propertyPrefix = propertyPrefix;
		this.properties = properties;
		this.claimName = claimName;
		log.debug("{} - context: property prefix for modifier configured to '{}'", claimName, propertyPrefix);
	}

	public String getClaimName() {
		return claimName;
	}

	public String getProperty(String suffix, String defaultValue) {
		return properties.getProperty(propertyPrefix + "." + suffix, defaultValue);
	}

}
