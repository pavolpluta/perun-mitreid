package cz.muni.ics.oidc;

import org.mitre.openid.connect.config.ConfigurationPropertiesBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.Properties;

/**
 * Logs some interesting facts.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
@SuppressWarnings("SpringAutowiredFieldsWarningInspection")
public class PerunOidcConfig {

	private final static Logger log = LoggerFactory.getLogger(PerunOidcConfig.class);
	private static final String OIDC_POM_FILE = "/META-INF/maven/cz.muni.ics/oidc-idp/pom.properties";
	private static final String MITREID_POM_FILE = "/META-INF/maven/org.mitre/openid-connect-server-webapp/pom.properties";

	private ConfigurationPropertiesBean configBean;
	private String rpcUrl;
	private String jwk;
	private String jdbcUrl;
	private String theme;

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

    @PostConstruct
	public void postInit() {
		log.info("Perun OIDC initialized");
		log.info("Mitreid config URL: {}", configBean.getIssuer());
		log.info("RPC URL: {}", rpcUrl);
		log.info("JSON Web Keys: {}", jwk);
		log.info("JDBC URL: {}",jdbcUrl);
		log.info("THEME: {}", theme);
		log.info("accessTokenClaimsModifier: {}", coreProperties.getProperty("accessTokenClaimsModifier"));
		if (servletContext != null) {
			log.info("contextPath: {}", servletContext.getContextPath());
			try {
				Properties p = new Properties();
				p.load(servletContext.getResourceAsStream(MITREID_POM_FILE));
				log.info("MitreID version: {}", p.getProperty("version"));
			} catch (IOException e) {
				log.error("cannot read file " + MITREID_POM_FILE, e);
			}
			try {
				Properties p = new Properties();
				p.load(servletContext.getResourceAsStream(OIDC_POM_FILE));
				log.info("Perun OIDC version: {}", p.getProperty("version"));
			} catch (IOException e) {
				log.error("cannot read file " + OIDC_POM_FILE, e);
			}
		}
	}


}
