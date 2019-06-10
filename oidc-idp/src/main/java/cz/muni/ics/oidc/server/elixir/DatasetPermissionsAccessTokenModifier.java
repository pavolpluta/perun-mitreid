package cz.muni.ics.oidc.server.elixir;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import cz.muni.ics.oidc.server.PerunTokenEnhancer;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Implements adding EGA dataset permissions into signed JWT access tokens.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
@SuppressWarnings("unused")
public class DatasetPermissionsAccessTokenModifier implements PerunTokenEnhancer.AccessTokenClaimsModifier {

	private final static Logger log = LoggerFactory.getLogger(DatasetPermissionsAccessTokenModifier.class);

	private static final String DATASETS_PROPERTIES = "/etc/perun/datasets.properties";
	private static final String PERMISSIONS_EGA = "permissions_ega";
	private static final String PERMISSIONS_REMS = "permissions_rems";
	private static final String GA4GH = "ga4gh"; //Global Alliance for Genomics and Health

	public DatasetPermissionsAccessTokenModifier() {
	}

	@Override
	public void modifyClaims(String sub, JWTClaimsSet.Builder builder, OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
		log.trace("modifyClaims(sub={})",sub);
		Set<String> scopes = accessToken.getScope();
		if ((!scopes.contains(PERMISSIONS_EGA)) && (!scopes.contains(PERMISSIONS_REMS)) && (!scopes.contains(GA4GH))) return;

		//load file
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(DATASETS_PROPERTIES));
		} catch (IOException e) {
			log.error("cannot read file " + DATASETS_PROPERTIES + ", will not modify access tokens");
			return;
		}
		//EGA
		if (scopes.contains(PERMISSIONS_EGA)) {
			String egaUrl = properties.getProperty("ega.url");
			String egaUser = properties.getProperty("ega.user");
			String egaPassword = properties.getProperty("ega.password");
			if (egaUrl == null || egaUser == null || egaPassword == null) {
				log.error("not found ega.url, ega.user or ega.password in file {}, giving up", DATASETS_PROPERTIES);
				return;
			}
			String actionUrl = egaUrl + sub + "/";
			Object perms = getPermissions(actionUrl, new BasicAuthorizationInterceptor(egaUser, egaPassword));
			if (perms != null) {
				builder.claim(PERMISSIONS_EGA, perms);
			}
		}
		//REMS
		if (scopes.contains(PERMISSIONS_REMS)) {
			String remsUrl = properties.getProperty("rems.url");
			String remsHeader = properties.getProperty("rems.header");
			String remsHeaderValue = properties.getProperty("rems.key");
			if (remsUrl == null || remsHeader == null || remsHeaderValue == null) {
				log.error("not found rems.url, rems.header or rems.key in file {}, giving up", DATASETS_PROPERTIES);
				return;
			}
			String actionUrl = remsUrl + sub;
			Object perms = getPermissions(actionUrl, new AddHeaderInterceptor(remsHeader, remsHeaderValue));
			if (perms != null) {
				builder.claim(PERMISSIONS_REMS, perms);
			}
		}

		//GA4GH
		if(scopes.contains(GA4GH)) {
			log.debug("adding claims required by GA4GH to access token");
			builder.claim("ga4gh.userinfo_claims", Arrays.asList("AffiliationAndRole", "ControlledAccessGrants", "AcceptedTermsAndPolicies", "ResearcherStatus"));
			builder.audience(Collections.singletonList(authentication.getOAuth2Request().getClientId()));
			ArrayList<String> scopesList = new ArrayList<>(accessToken.getScope());
			scopesList.sort(String::compareTo);
			builder.claim("scope", scopesList);
		}
	}

	private static class AddHeaderInterceptor implements ClientHttpRequestInterceptor {

		private final String header;
		private final String value;

		AddHeaderInterceptor(String header, String value) {
			this.header = header;
			this.value = value;
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
			request.getHeaders().add(header, value);
			return execution.execute(request, body);
		}
	}

	private Object getPermissions(String actionUrl, ClientHttpRequestInterceptor authInterceptor) {
		//get permissions data
		try {
			JsonNode result;
			//prepare basic auth
			RestTemplate rt = new RestTemplate();
			rt.setRequestFactory(
					new InterceptingClientHttpRequestFactory(rt.getRequestFactory(), Collections.singletonList(authInterceptor))
			);
			//make the call
			try {
				log.debug("calling Permissions API at {}", actionUrl);
				result = rt.getForObject(actionUrl, JsonNode.class);
			} catch (HttpClientErrorException ex) {
				MediaType contentType = ex.getResponseHeaders().getContentType();
				String body = ex.getResponseBodyAsString();
				log.error("HTTP ERROR " + ex.getRawStatusCode() + " URL " + actionUrl + " Content-Type: " + contentType);
				if (ex.getRawStatusCode() == 404) {
					log.warn("Got status 404 from Permissions endpoint {}, ELIXIR AAI user is not linked to user at Permissions API");
					return null;
				}
				if ("json".equals(contentType.getSubtype())) {
					try {
						log.error(new ObjectMapper().readValue(body, JsonNode.class).path("message").asText());
					} catch (IOException e) {
						log.error("cannot parse error message from JSON", e);
					}
				} else {
					log.error("cannot make REST call", ex);
				}
				return null;
			}
			JsonNode jsonNode = result;
			String rawJson = new ObjectMapper().writeValueAsString(jsonNode.path("permissions"));
			log.debug("Permissions API response: {}", rawJson);
			//add data
			JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
			return parser.parse(rawJson);
		} catch (ParseException | JsonProcessingException e) {
			log.error("cannot parse Permissions API response", e);
		} catch (Exception ex) {
			log.error("Cannot get dataset permissions", ex);
		}
		return null;
	}


}
