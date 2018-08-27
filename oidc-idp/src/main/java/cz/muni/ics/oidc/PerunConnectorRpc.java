package cz.muni.ics.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.Group;
import cz.muni.ics.oidc.models.Member;
import cz.muni.ics.oidc.models.Resource;
import cz.muni.ics.oidc.models.RichUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Connects to Perun. Should be using LDAP, but for now  RPC calls would do
 *
 * @author Martin Kuba makub@ics.muni.cz
 * @author Dominik František Bučík bucik@ics.muni.cz
 * @author Peter Jancus jancus@ics.muni.cz
 */
public class PerunConnectorRpc implements PerunConnector {

	private final static Logger log = LoggerFactory.getLogger(PerunConnectorRpc.class);

	private String perunUrl;
	private String perunUser;
	private String perunPassword;

	private String oidcClientIdAttr;
	private String oidcCheckMembershipAttr;

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
		log.trace("setting OIDCClientID attr name");
		this.oidcClientIdAttr = oidcClientIdAttr;
	}

	public void setOidcCheckMembershipAttr(String oidcCheckMembershipAttr) {
		log.trace("setting OIDCCheckGroupMembership attr name");
		this.oidcCheckMembershipAttr = oidcCheckMembershipAttr;
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
	public RichUser getUserAttributes(Long userId) {
		log.trace("getUserAttributes({})", userId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("user", userId);

		return Mapper.mapRichUser(makeRpcCall("/usersManager/getRichUserWithAttributes", map));
	}

	@Override
	public List<Facility> getFacilitiesByClientId(String clientId) {
		log.trace("getFacilitiesByClientId({})", clientId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("attributeName", oidcClientIdAttr);
		map.put("attributeValue", clientId);

		ArrayList<Facility> facilities = new ArrayList<>();
		JsonNode jsonNode = makeRpcCall("/facilitiesManager/getFacilitiesByAttribute", map);
		for (int i = 0; i < jsonNode.size(); i++) {
			facilities.add(Mapper.mapFacility(jsonNode.get(i)));
		}
		return facilities;
	}

	@Override
	public List<Resource> getAssignedResourcesForFacility(Long facilityId) {
		log.trace("getAssignedResourcesForFacility({})", facilityId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facilityId);

		ArrayList<Resource> resources = new ArrayList<>();
		JsonNode jsonNode = makeRpcCall("/facilitiesManager/getAssignedResources", map);
		for (int i = 0; i < jsonNode.size(); i++) {
			resources.add(Mapper.mapResource(jsonNode.get(i)));
		}
		return resources;
	}

	@Override
	public List<Group> getAssignedGroups(Long resourceId, Long memberId) {
		log.trace("getAssignedGroups resource: ({}), member: ({})", resourceId, memberId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("resource", resourceId);
		map.put("member", memberId);

		ArrayList<Group> groups = new ArrayList<>();
		JsonNode jsonNode = makeRpcCall("/resourcesManager/getAssignedGroups", map);
		for (int i = 0; i < jsonNode.size(); i++) {
			groups.add(Mapper.mapGroup(jsonNode.get(i)));
		}
		return groups;
	}

	@Override
	public List<Member> getMembersByUser(String userId) {
		log.trace("getMembersByUser({})", userId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("user", userId);

		ArrayList<Member> members = new ArrayList<>();
		JsonNode jsonNode = makeRpcCall("/membersManager/getMembersByUser", map);
		for (int i = 0; i < jsonNode.size(); i++) {
			members.add(Mapper.mapMember(jsonNode.get(i)));
		}
		return members;
	}

	@Override
	public boolean isAllowedGroupCheckForFacility(Long facilityId) {
		log.trace("isAllowedGroupCheckForFacility({})", facilityId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facilityId);
		map.put("attributeName", oidcCheckMembershipAttr);
		JsonNode res = makeRpcCall("/attributesManager/getAttribute", map);

		return res.get("value").asBoolean(false);
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
