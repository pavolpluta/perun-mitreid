package cz.muni.ics.oidc;

import org.mitre.oauth2.model.OAuth2AccessTokenEntity;
import org.mitre.oauth2.model.OAuth2RefreshTokenEntity;
import org.mitre.oauth2.service.impl.DefaultIntrospectionResultAssembler;
import org.mitre.openid.connect.config.ConfigurationPropertiesBean;
import org.mitre.openid.connect.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Adds "iss" to identify issuer in response from Introspection endpoint to Resource Server.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunIntrospectionResultAssembler extends DefaultIntrospectionResultAssembler {

	private final static Logger log = LoggerFactory.getLogger(PerunIntrospectionResultAssembler.class);

	private ConfigurationPropertiesBean configBean;

	@Override
	public Map<String, Object> assembleFrom(OAuth2AccessTokenEntity accessToken, UserInfo userInfo, Set<String> authScopes) {
		Map<String, Object> map = super.assembleFrom(accessToken, userInfo, authScopes);
		addDataToResponse(map);
		return map;
	}

	@Override
	public Map<String, Object> assembleFrom(OAuth2RefreshTokenEntity refreshToken, UserInfo userInfo, Set<String> authScopes) {
		Map<String, Object> map = super.assembleFrom(refreshToken, userInfo, authScopes);
		addDataToResponse(map);
		return map;
	}

	private void addDataToResponse(Map<String, Object> map) {
		log.debug("adding iss to introspection response {}", map);
		map.put("iss", configBean.getIssuer());
	}

	public void setConfigBean(ConfigurationPropertiesBean configBean) {
		this.configBean = configBean;
	}
}
