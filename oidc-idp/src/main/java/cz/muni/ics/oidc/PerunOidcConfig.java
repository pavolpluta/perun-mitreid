package cz.muni.ics.oidc;

import org.mitre.openid.connect.config.ConfigurationPropertiesBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunOidcConfig {

	private final static Logger log = LoggerFactory.getLogger(PerunOidcConfig.class);

	private ConfigurationPropertiesBean configBean;
	private String rpcUrl;

	public void setRpcUrl(String rpcUrl) {
		this.rpcUrl = rpcUrl;
	}

	public void setConfigBean(ConfigurationPropertiesBean configBean) {
		this.configBean = configBean;
	}


	@PostConstruct
	public void postInit() {
		log.info("Perun OIDC initialized");
		log.info("Mitreid config URL: {}",configBean.getIssuer());
		log.info("RPC URL: {}",rpcUrl);
	}


}
