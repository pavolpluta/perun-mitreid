package cz.muni.ics.oidc;

import com.google.gson.JsonElement;
import org.mitre.oauth2.model.OAuth2AccessTokenEntity;
import org.mitre.oauth2.model.OAuth2RefreshTokenEntity;
import org.mitre.oauth2.service.impl.DefaultIntrospectionResultAssembler;
import org.mitre.openid.connect.config.ConfigurationPropertiesBean;
import org.mitre.openid.connect.model.UserInfo;
import org.mitre.openid.connect.service.ScopeClaimTranslationService;
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

	public PerunIntrospectionResultAssembler(ConfigurationPropertiesBean configBean, ScopeClaimTranslationService translator) {
		this.configBean = configBean;
		this.translator = translator;
	}

	private ConfigurationPropertiesBean configBean;
	private ScopeClaimTranslationService translator;

	@Override
	public Map<String, Object> assembleFrom(OAuth2AccessTokenEntity accessToken, UserInfo userInfo, Set<String> authScopes) {
		log.info("adding claims at Introspection endpoint for client {}({}) and user {}({})",
				accessToken.getClient().getClientId(), accessToken.getClient().getClientName(), userInfo.getSub(), userInfo.getName());
		Map<String, Object> map = super.assembleFrom(accessToken, userInfo, authScopes);
		addDataToResponse(map, userInfo, authScopes);
		return map;
	}

	@Override
	public Map<String, Object> assembleFrom(OAuth2RefreshTokenEntity refreshToken, UserInfo userInfo, Set<String> authScopes) {
		Map<String, Object> map = super.assembleFrom(refreshToken, userInfo, authScopes);
		addDataToResponse(map, userInfo, authScopes);
		return map;
	}

	private void addDataToResponse(Map<String, Object> map, UserInfo userInfo, Set<String> authScopes) {
		log.debug("adding iss to introspection response {}", map);
		map.put("iss", configBean.getIssuer());
		log.debug("adding user claims");
		Set<String> authorizedClaims = translator.getClaimsForScopeSet(authScopes);
		for (Map.Entry<String, JsonElement> claim : userInfo.toJson().entrySet()) {
			if (authorizedClaims.contains(claim.getKey()) && claim.getValue() != null && !claim.getValue().isJsonNull()) {
				log.debug("adding claim {} with value {}", claim.getKey(), claim.getValue());
				map.put(claim.getKey(), claim.getValue());
			}
		}
	}

}
