package cz.muni.ics.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Connects to Perun. Should be using LDAP, but for now  RPC calls would do
 *
 * @author Martin Kuba makub@ics.muni.cz
 * @author Dominik František Bučík bucik@ics.muni.cz
 */
public class PerunConnectorRpc implements PerunConnector {

	private final static Logger log = LoggerFactory.getLogger(PerunConnectorRpc.class);

	private String perunUrl;
	private String perunUser;
	private String perunPassword;

	private String oidcClientIdAttr;

	public void setPerunUrl(String perunUrl) {
		log.trace("setting perunUrl to {}",perunUrl);
		this.perunUrl = perunUrl;
	}

	public void setPerunUser(String perunUser) {
		log.trace("setting perunUser to {}",perunUser);
		this.perunUser = perunUser;
	}

	public void setPerunPassword(String perunPassword) {
		log.trace("setting perunPassword");
		this.perunPassword = perunPassword;
	}

	public void setOidcClientIdAttr(String oidcClientIdAttr) {
		this.oidcClientIdAttr = oidcClientIdAttr;
	}

	@Override
	public JsonNode getPreauthenticatedUserId(PerunPrincipal perunPrincipal) {
		log.trace("getPreauthenticatedUserId({})", perunPrincipal);
		//make call
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("extLogin", perunPrincipal.getExtLogin());
		map.put("extSourceName", perunPrincipal.getExtSourceName());
		return makeRpcCall("/usersManager/getUserByExtSourceNameAndExtLogin", map);
	}

	@Override
	public JsonNode getUserAttributes(Long userId) {
		log.trace("getUserAttributes({})", userId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("user", userId);
		return makeRpcCall("/usersManager/getRichUserWithAttributes", map);
	}

	@Override
	public JsonNode getFacilitiesByClientId(String clientId) {
		log.trace("getFacilitiesByClientId({})", clientId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("attributeName", oidcClientIdAttr);
		map.put("attributeValue", clientId);
		return makeRpcCall("/facilitiesManager/getFacilitiesByAttribute", map);
	}

	@Override
	public JsonNode getAssignedResourcesForFacility(String facilityId) {
		log.trace("getAssignedResourcesForFacility({})", facilityId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facilityId);
		return makeRpcCall("/facilitiesManager/getAssignedResources", map);
	}

	@Override
	public JsonNode getAssignedGroups(String resourceId, String memberId) {
		log.trace("getAssignedGroups resource: ({}), member: ({})", resourceId, memberId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("resource", resourceId);
		map.put("member", memberId);
		return makeRpcCall("/resourcesManager/getAssignedGroups", map);
	}

	@Override
	public JsonNode getMembersByUser(String userId) {
		log.trace("getMembersByUser({})", userId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("user", userId);
		return makeRpcCall("/membersManager/getMembersByUser", map);
	}

	private JsonNode makeRpcCall(String urlPart, Map<String, Object> map) {
		//prepare basic auth
		RestTemplate restTemplate = new RestTemplate();
		List<ClientHttpRequestInterceptor> interceptors =
				Collections.singletonList(new BasicAuthorizationInterceptor(perunUser, perunPassword));
		restTemplate.setRequestFactory(new InterceptingClientHttpRequestFactory(restTemplate.getRequestFactory(), interceptors));
		String actionUrl = perunUrl + "/json" + urlPart;
		//make the call
		try {
			log.trace("calling {}",actionUrl);
			return restTemplate.postForObject(actionUrl, map, JsonNode.class);
		} catch (HttpClientErrorException ex) {
			MediaType contentType = ex.getResponseHeaders().getContentType();
			String body = ex.getResponseBodyAsString();
			log.error("HTTP ERROR " + ex.getRawStatusCode() + " URL " + actionUrl + " Content-Type: " + contentType);
			if ("json".equals(contentType.getSubtype())) {
				try {
					log.error(new ObjectMapper().readValue(body, JsonNode.class).path("message").asText());
				} catch (IOException e) {
					log.error("cannot parse error message from JSON",e);
				}
			} else {
				log.error(ex.getMessage());
			}
			throw new RuntimeException("cannot connect to Perun RPC",ex);
		}
	}

}
