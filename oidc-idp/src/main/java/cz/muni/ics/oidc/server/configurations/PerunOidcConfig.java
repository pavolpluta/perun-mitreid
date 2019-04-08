package cz.muni.ics.oidc.server.configurations;

import org.mitre.openid.connect.config.ConfigurationPropertiesBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration of OIDC server in context of Perun.
 * Logs some interesting facts.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunOidcConfig {
	private final static Logger log = LoggerFactory.getLogger(PerunOidcConfig.class);
	private static final String OIDC_POM_FILE = "/META-INF/maven/cz.muni.ics/oidc-idp/pom.properties";
	private static final String MITREID_POM_FILE = "/META-INF/maven/org.mitre/openid-connect-server-webapp/pom.properties";

	private ConfigurationPropertiesBean configBean;
	private String rpcUrl;
	private String jwk;
	private String jdbcUrl;
	private String theme;
	private String registrarUrl;
	private String loginUrl;
	private boolean askPerunForIdpFiltersEnabled;
	private String perunOIDCVersion;
	private String mitreidVersion;

	@Autowired
	private ServletContext servletContext;

	@Autowired
	private Properties coreProperties;

	public void setRpcUrl(String rpcUrl) {
		this.rpcUrl = rpcUrl;
	}

	public void setConfigBean(ConfigurationPropertiesBean configBean) {
		this.configBean = configBean;
	}

	public void setJwk(String jwk) {
		this.jwk = jwk;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	public void setTheme(String theme) {
		this.theme = theme;
	}

	public String getTheme() {
		return theme;
	}

	public String getRegistrarUrl() {
		return registrarUrl;
	}

	public void setRegistrarUrl(String registrarUrl) {
		this.registrarUrl = registrarUrl;
	}

	public String getPerunOIDCVersion() {
		if (perunOIDCVersion == null) {
			perunOIDCVersion = readPomVersion(OIDC_POM_FILE);
		}
		return perunOIDCVersion;
	}

	public String getMitreidVersion() {
		if (mitreidVersion == null) {
			mitreidVersion = readPomVersion(MITREID_POM_FILE);
		}
		return mitreidVersion;
	}

	private String readPomVersion(String file) {
		try {
			Properties p = new Properties();
			p.load(servletContext.getResourceAsStream(file));
			return p.getProperty("version");
		} catch (IOException e) {
			log.error("cannot read file " + file, e);
			return "UNKNOWN";
		}
	}

	public void setLoginUrl(String loginUrl) {
		this.loginUrl = loginUrl;
	}

	public String getLoginUrl() {
		return loginUrl;
	}

	public boolean isAskPerunForIdpFiltersEnabled() {
		return askPerunForIdpFiltersEnabled;
	}

	public void setAskPerunForIdpFiltersEnabled(boolean askPerunForIdpFiltersEnabled) {
		this.askPerunForIdpFiltersEnabled = askPerunForIdpFiltersEnabled;
	}

	@PostConstruct
	public void postInit() {
		log.info("Perun OIDC initialized");
		log.info("Mitreid config URL: {}", configBean.getIssuer());
		log.info("RPC URL: {}", rpcUrl);
		log.info("JSON Web Keys: {}", jwk);
		log.info("JDBC URL: {}", jdbcUrl);
		log.info("LDAP: ldaps://{}/{}", coreProperties.getProperty("ldap.host"), coreProperties.getProperty("ldap.baseDN"));
		log.info("THEME: {}", theme);
		log.info("Registrar URL: {}", registrarUrl);
		log.info("LOGIN URL: {}", loginUrl);
		log.info("accessTokenClaimsModifier: {}", coreProperties.getProperty("accessTokenClaimsModifier"));
		log.info("MitreID version: {}", getMitreidVersion());
		log.info("Perun OIDC version: {}", getPerunOIDCVersion());
	}

}
