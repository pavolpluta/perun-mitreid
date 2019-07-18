package cz.muni.ics.oidc.server.configurations;

import org.mitre.openid.connect.config.ConfigurationPropertiesBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

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
	private String samlLoginURL;
	private String samlLogoutURL;
	private boolean askPerunForIdpFiltersEnabled;
	private String perunOIDCVersion;
	private String mitreidVersion;
	private String proxyExtSourceName;
	private Set<String> idTokenScopes;
	private List<String> availableLangs;

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




	public void setIdTokenScopes(Set<String> idTokenScopes) {
		this.idTokenScopes = idTokenScopes;
	}


	public Set<String> getIdTokenScopes() {
		return idTokenScopes;
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

	public void setSamlLoginURL(String samlLoginURL) {
		this.samlLoginURL = samlLoginURL;
	}

	public String getSamlLoginURL() {
		return samlLoginURL;
	}

	public void setSamlLogoutURL(String samlLogoutURL) {
		this.samlLogoutURL = samlLogoutURL;
	}

	public String getSamlLogoutURL() {
		return samlLogoutURL;
	}

	public ConfigurationPropertiesBean getConfigBean() {
		return configBean;
	}

	public boolean isAskPerunForIdpFiltersEnabled() {
		return askPerunForIdpFiltersEnabled;
	}

	public void setAskPerunForIdpFiltersEnabled(boolean askPerunForIdpFiltersEnabled) {
		this.askPerunForIdpFiltersEnabled = askPerunForIdpFiltersEnabled;
	}

	public String getProxyExtSourceName() {
		return proxyExtSourceName;
	}

	public void setProxyExtSourceName(String proxyExtSourceName) {
		if (proxyExtSourceName == null || proxyExtSourceName.isEmpty()) {
			this.proxyExtSourceName = null;
		} else {
			this.proxyExtSourceName = proxyExtSourceName;
		}
	}

	public void setAvailableLangs(List<String> availableLangs) {
		this.availableLangs = availableLangs;
	}

	@PostConstruct
	public void postInit() {
		//load URLs from properties if available or construct them from issuer URL
		String loginURL = coreProperties.getProperty("proxy.login.url");
		if (loginURL != null && !loginURL.trim().isEmpty()) {
			samlLoginURL = loginURL.trim();
		} else {
			samlLoginURL = UriComponentsBuilder.fromHttpUrl(configBean.getIssuer()).replacePath("/Shibboleth.sso/Login").build().toString();
		}
		String logoutURL = coreProperties.getProperty("proxy.logout.url");
		if (logoutURL != null && !logoutURL.trim().isEmpty()) {
			samlLogoutURL = logoutURL.trim();
		} else {
			samlLogoutURL = UriComponentsBuilder.fromHttpUrl(configBean.getIssuer()).replacePath("/Shibboleth.sso/Logout").build().toString();
		}
	}

	//called when all beans are initialized, but twice, once for root context and once for spring-servlet
	@EventListener
	public void handleContextRefresh(ContextRefreshedEvent event) {
		if (event.getApplicationContext().getParent() == null) {
			//log info
			log.info("Perun OIDC initialized");
			log.info("Mitreid config URL: {}", configBean.getIssuer());
			log.info("RPC URL: {}", rpcUrl);
			log.info("JSON Web Keys: {}", jwk);
			log.info("JDBC URL: {}", jdbcUrl);
			log.info("LDAP: ldaps://{}/{}", coreProperties.getProperty("ldap.host"), coreProperties.getProperty("ldap.baseDN"));
			log.info("THEME: {}", theme);
			log.info("Registrar URL: {}", registrarUrl);
			log.info("LOGIN  URL: {}", samlLoginURL);
			log.info("LOGOUT URL: {}", samlLogoutURL);
			log.info("accessTokenClaimsModifier: {}", coreProperties.getProperty("accessTokenClaimsModifier"));
			log.info("Proxy EXT_SOURCE name: {}", proxyExtSourceName);
			log.info("Available languages: {}", availableLangs);
			log.info("MitreID version: {}", getMitreidVersion());
			log.info("Perun OIDC version: {}", getPerunOIDCVersion());
		}
	}

}
