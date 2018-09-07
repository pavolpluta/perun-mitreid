package cz.muni.ics.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.Group;
import cz.muni.ics.oidc.models.Member;
import cz.muni.ics.oidc.models.PerunUser;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		log.trace("setting perunUrl to {}", perunUrl);
		this.perunUrl = perunUrl;
	}

	public void setPerunUser(String perunUser) {
		log.trace("setting perunUser to {}", perunUser);
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
	public PerunUser getPreauthenticatedUserId(PerunPrincipal perunPrincipal) {
		log.trace("getPreauthenticatedUserId({})", perunPrincipal);
		//make call
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("extLogin", perunPrincipal.getExtLogin());
		map.put("extSourceName", perunPrincipal.getExtSourceName());
		return Mapper.mapPerunUser(makeRpcCall("/usersManager/getUserByExtSourceNameAndExtLogin", map));
	}

	@Override
	public RichUser getUserAttributes(Long userId) {
		log.trace("getUserAttributes({})", userId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("user", userId);
		return Mapper.mapRichUser(makeRpcCall("/usersManager/getRichUserWithAttributes", map));
	}

	@Override
	public Facility getFacilityByClientId(String clientId) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("attributeName", oidcClientIdAttr);
		map.put("attributeValue", clientId);
		JsonNode jsonNode = makeRpcCall("/facilitiesManager/getFacilitiesByAttribute", map);
		Facility facility = (jsonNode.size() > 0) ? Mapper.mapFacility(jsonNode.get(0)) : null;
		log.trace("getFacilitiesByClientId({}) returns {}", clientId, facility);
		return facility;
	}

	private List<Resource> getAssignedResourcesForFacility(Facility facility) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facility.getId());
		ArrayList<Resource> resources = new ArrayList<>();
		JsonNode jsonNode = makeRpcCall("/facilitiesManager/getAssignedResources", map);
		for (int i = 0; i < jsonNode.size(); i++) {
			resources.add(Mapper.mapResource(jsonNode.get(i)));
		}
		log.trace("getAssignedResourcesForFacility({}) returns {}", facility, resources);
		return resources;
	}

	private List<Group> getAssignedGroups(Resource resource, Member member) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("resource", resource.getId());
		map.put("member", member.getId());
		ArrayList<Group> groups = new ArrayList<>();
		JsonNode jsonNode = makeRpcCall("/resourcesManager/getAssignedGroups", map);
		for (int i = 0; i < jsonNode.size(); i++) {
			groups.add(Mapper.mapGroup(jsonNode.get(i)));
		}
		//add vo short names
		Set<Long> voIds = new HashSet<>();
		for (Group group : groups) {
			voIds.add(group.getVoId());
		}
		Map<Long, String> voId2shortName = new HashMap<>();
		for (Long voId : voIds) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("id", voId);
			JsonNode r = makeRpcCall("/vosManager/getVoById", m);
			voId2shortName.put(voId, r.path("shortName").asText());
		}
		for (Group group : groups) {
			group.setUniqueGroupName(voId2shortName.get(group.getVoId()) + ":" + group.getName());
		}
		log.trace("getAssignedGroups(resource={},member={}) returns {}", resource, member, groups);
		return groups;
	}

	private List<Member> getValidMembersByUser(Long userId) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("user", userId);

		ArrayList<Member> members = new ArrayList<>();
		JsonNode jsonNode = makeRpcCall("/membersManager/getMembersByUser", map);
		for (int i = 0; i < jsonNode.size(); i++) {
			Member member = Mapper.mapMember(jsonNode.get(i));
			if ("VALID".equals(member.getStatus())) {
				members.add(member);
			}
		}
		log.trace("getMembersByUser({}) returns {}", userId, members);
		return members;
	}

	@Override
	public boolean isMembershipCheckEnabledOnFacility(Facility facility) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facility.getId());
		map.put("attributeName", oidcCheckMembershipAttr);
		JsonNode res = makeRpcCall("/attributesManager/getAttribute", map);
		boolean result = res.get("value").asBoolean(false);
		log.trace("isMembershipCheckEnabledOnFacility({}) returns {}", facility, result);
		return result;
	}

	/**
	 * Decide whether the user is in any group assigned to the facility.
	 *
	 * @param facility facility to be accessed
	 * @param userId   id of user to check
	 * @return true if the user is member of any group assigned to a resource of the facility
	 */
	@Override
	public boolean isUserAllowedOnFacility(Facility facility, Long userId) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facility.getId());
		JsonNode jsonNode = makeRpcCall("/facilitiesManager/getAllowedUsers", map);
		for (int i = 0; i < jsonNode.size(); i++) {
			JsonNode userJson = jsonNode.get(i);
			if (userId == userJson.get("id").asLong()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Provides a list of groups which connect the user to the facility.
	 *
	 * @param facility acility to be accessed
	 * @param userId   id of user to check
	 * @return collection of all groups such that the user is member of the group and the group is assigned to a resource of the facility
	 */
	@Override
	public Set<Group> getUserGroupsAllowedOnFacility(Facility facility, Long userId) {
		List<Resource> assignedResourcesForFacility = getAssignedResourcesForFacility(facility);
		List<Member> membersByUser = getValidMembersByUser(userId);
		Set<Group> groups = new HashSet<>();
		for (Member member : membersByUser) {
			for (Resource resource : assignedResourcesForFacility) {
				if (!resource.getVoId().equals(member.getVoId())) continue;
				groups.addAll(getAssignedGroups(resource, member));
			}
		}
		log.trace("getUserGroupsAllowedOnFacility(facility={},userId={}) returns {}", facility, userId, groups);
		return groups;
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
			log.trace("calling {} with {}", actionUrl, map);
			return restTemplate.postForObject(actionUrl, map, JsonNode.class);
		} catch (HttpClientErrorException ex) {
			MediaType contentType = ex.getResponseHeaders().getContentType();
			String body = ex.getResponseBodyAsString();
			log.error("HTTP ERROR " + ex.getRawStatusCode() + " URL " + actionUrl + " Content-Type: " + contentType);
			if ("json".equals(contentType.getSubtype())) {
				try {
					log.error(new ObjectMapper().readValue(body, JsonNode.class).path("message").asText());
				} catch (IOException e) {
					log.error("cannot parse error message from JSON", e);
				}
			} else {
				log.error(ex.getMessage());
			}
			throw new RuntimeException("cannot connect to Perun RPC", ex);
		}
	}

}
