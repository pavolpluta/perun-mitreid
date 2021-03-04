package cz.muni.ics.oidc.server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import net.minidev.json.JSONArray;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.model.OAuth2AccessTokenEntity;
import org.mitre.openid.connect.models.Acr;
import org.mitre.openid.connect.models.DeviceCodeAcr;
import org.mitre.openid.connect.service.ScopeClaimTranslationService;
import org.mitre.openid.connect.service.UserInfoService;
import org.mitre.openid.connect.service.impl.DefaultOIDCTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Request;

import java.text.ParseException;
import java.util.Map;
import java.util.Set;

/**
 * Modifies ID Token.
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class PerunOIDCTokenService extends DefaultOIDCTokenService {

	private static final Logger log = LoggerFactory.getLogger(PerunOIDCTokenService.class);

	private final UserInfoService userInfoService;
	private final ScopeClaimTranslationService translator;
	private final PerunOidcConfig perunOidcConfig;
	private final PerunAcrRepository acrRepository;
	private final PerunDeviceCodeAcrRepository deviceCodeAcrRepository;

	private final Gson gson = new Gson();

	@Autowired
	public PerunOIDCTokenService(UserInfoService userInfoService,
								 ScopeClaimTranslationService translator,
								 PerunOidcConfig perunOidcConfig,
								 PerunAcrRepository acrRepository,
								 PerunDeviceCodeAcrRepository deviceCodeAcrRepository)
	{
		this.userInfoService = userInfoService;
		this.translator = translator;
		this.perunOidcConfig = perunOidcConfig;
		this.acrRepository = acrRepository;
		this.deviceCodeAcrRepository = deviceCodeAcrRepository;
	}

	@Override
	protected void addCustomIdTokenClaims(JWTClaimsSet.Builder idClaims, ClientDetailsEntity client, OAuth2Request request,
										  String sub, OAuth2AccessTokenEntity accessToken)
	{
		log.debug("modifying ID token");
		String userId = accessToken.getAuthenticationHolder().getAuthentication().getName();
		String clientId = request.getClientId();
		log.debug("userId={},clientId={}", userId, clientId);

		Set<String> scopes = accessToken.getScope();
		Set<String> authorizedClaims = translator.getClaimsForScopeSet(scopes);
		Set<String> idTokenClaims = translator.getClaimsForScopeSet(perunOidcConfig.getIdTokenScopes());

		for (Map.Entry<String, JsonElement> claim : userInfoService.getByUsernameAndClientId(userId,
				clientId).toJson().entrySet()) {
			String claimKey = claim.getKey();
			JsonElement claimValue = claim.getValue();
			if (claimValue != null && !claimValue.isJsonNull() && authorizedClaims.contains(claimKey)
					&& idTokenClaims.contains(claimKey))
			{
				log.debug("adding to ID token claim {} with value {}", claimKey, claimValue);
				idClaims.claim(claimKey, gson2jsonsmart(claimValue));
			}
		}

		String acr = getAuthnContextClass(client.getClientId(), sub, request.getRequestParameters());
		if (acr != null) {
			log.debug("adding to ID token claim acr with value {}", acr);
			idClaims.claim("acr", acr);
		}
	}

	private String getAuthnContextClass(String clientId, String sub, Map<String, String> params) {
		log.debug("Fetching ACR");
		String authnContextClass = null;
		if (params.containsKey(Acr.PARAM_STATE)) {
			String state = params.get(Acr.PARAM_STATE);
			log.debug("Fetch ACR for sub '{}', clientId: '{}' and state '{}'", sub, clientId, state);
			Acr acr = acrRepository.getActive(sub, clientId, state);
			if (acr != null) {
				authnContextClass = acr.getShibAuthnContextClass();
			}
		} else if (params.containsKey(DeviceCodeAcr.PARAM_DEVICE_CODE)) {
			String deviceCode = params.get(DeviceCodeAcr.PARAM_DEVICE_CODE);
			log.debug("Fetch ACR for device code '{}'", deviceCode);
			DeviceCodeAcr deviceCodeAcr = deviceCodeAcrRepository.getActiveByDeviceCode(deviceCode);
			if (deviceCodeAcr != null) {
				authnContextClass = deviceCodeAcr.getShibAuthnContextClass();
			}
		}
		return authnContextClass;
	}

	/**
	 * Converts claim values from com.google.gson.JsonElement to net.minidev.json.JSONObject or primitive value
	 *
	 * @param jsonElement Gson representation
	 * @return json-smart representation
	 */
	private Object gson2jsonsmart(JsonElement jsonElement) {
		if (jsonElement.isJsonPrimitive()) {
			JsonPrimitive p = jsonElement.getAsJsonPrimitive();
			if (p.isString()) {
				return p.getAsString();
			} else if (p.isBoolean()) {
				return p.getAsBoolean();
			} else if (p.isNumber()) {
				return p.getAsNumber();
			} else {
				log.warn("unknown JsonPrimitive {}", p);
				return null;
			}
		} else if (jsonElement.isJsonObject()) {
			try {
				return JSONObjectUtils.parse(gson.toJson(jsonElement));
			} catch (ParseException e) {
				log.error("cannot convert Gson->smart-json.JSONObject", e);
				return null;
			}
		} else if (jsonElement.isJsonArray()) {
			JSONArray jsonArray = new JSONArray();
			jsonElement.getAsJsonArray().forEach(je -> jsonArray.appendElement(gson2jsonsmart(je)));
			return jsonArray;
		} else {
			return null;
		}
	}
}
