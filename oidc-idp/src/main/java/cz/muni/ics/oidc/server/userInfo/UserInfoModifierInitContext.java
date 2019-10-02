package cz.muni.ics.oidc.server.userInfo;

import cz.muni.ics.oidc.server.connectors.PerunConnector;

import java.util.Properties;

/**
 * Context for initializing UserInfoModifiers.
 *
 * @author Dominik Baránek 0Baranek.dominik0@gmail.com
 */
public class UserInfoModifierInitContext {

	private final String propertyPrefix;
	private final Properties properties;
	private PerunConnector perunConnector;

	public UserInfoModifierInitContext(String propertyPrefix, Properties properties, PerunConnector perunConnector) {
		this.propertyPrefix = propertyPrefix;
		this.properties = properties;
		this.perunConnector = perunConnector;
	}

	public String getProperty(String suffix, String defaultValue) {
		return properties.getProperty(propertyPrefix + "." + suffix, defaultValue);
	}

	public PerunConnector getPerunConnector() {
		return perunConnector;
	}

	public void setPerunConnector(PerunConnector perunConnector) {
		this.perunConnector = perunConnector;
	}
}
