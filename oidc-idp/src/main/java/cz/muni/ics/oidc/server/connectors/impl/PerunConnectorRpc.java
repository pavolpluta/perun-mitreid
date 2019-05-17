package cz.muni.ics.oidc.server.connectors.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.Group;
import cz.muni.ics.oidc.models.Mapper;
import cz.muni.ics.oidc.models.Member;
import cz.muni.ics.oidc.models.PerunAttribute;
import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.models.RichUser;
import cz.muni.ics.oidc.models.Vo;
import cz.muni.ics.oidc.server.PerunPrincipal;
import cz.muni.ics.oidc.server.connectors.PerunConnector;
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
import java.util.stream.Collectors;

/**
 * Connects to Perun via RPC.
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
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("extLogin", perunPrincipal.getExtLogin());
		map.put("extSourceName", perunPrincipal.getExtSourceName());

		PerunUser res = Mapper.mapPerunUser(makeRpcCall("/usersManager/getUserByExtSourceNameAndExtLogin", map));
		log.trace("getPreauthenticatedUserId({}) returns: {}", perunPrincipal, res);
		return res;
	}

	@Override
	public RichUser getUserAttributes(Long userId) {
		log.trace("getUserAttributes({})", userId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("user", userId);

		RichUser res = Mapper.mapRichUser(makeRpcCall("/usersManager/getRichUserWithAttributes", map));
		log.trace("getUserAttributes({}) returns: {}", userId, res);
		return res;
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

	@Override
	public boolean isMembershipCheckEnabledOnFacility(Facility facility) {
		log.trace("isMembershipCheckEnabledOnFacility({})", facility);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facility.getId());
		map.put("attributeName", oidcCheckMembershipAttr);
		JsonNode res = makeRpcCall("/attributesManager/getAttribute", map);

		boolean result = res.get("value").asBoolean(false);
		log.trace("isMembershipCheckEnabledOnFacility({}) returns {}", facility, result);
		return result;
	}

	@Override
	public boolean canUserAccessBasedOnMembership(Facility facility, Long userId) {
		log.trace("canUserAccessBasedOnMembership({}, {})", facility, userId);
		List<Group> activeGroups = getGroupsWhereUserIsActive(facility, userId);

		boolean res = (activeGroups != null && !activeGroups.isEmpty());
		log.trace("canUserAccessBasedOnMembership({}, {}) returns: {}", facility, userId, res);
		return res;
	}

	@Override
	public Map<Vo, List<Group>> getGroupsForRegistration(Facility facility, Long userId, List<String> voShortNames) {
		log.trace("getGroupsForRegistration({}, {}, {})", facility, userId, voShortNames);
		List<Vo> vos = getVosByShortNames(voShortNames);
		Map<Long, Vo> vosMap = convertVoListToMap(vos);
		List<Member> userMembers = getMembersByUser(userId);
		userMembers = new ArrayList<>(new HashSet<>(userMembers));

		//Filter out vos where member is other than valid or expired. These vos cannot be used for registration
		Map<Long, String> memberVoStatuses = convertMembersListToStatusesMap(userMembers);
		Map<Long, Vo> vosForRegistration = new HashMap<>();
		for (Map.Entry<Long, Vo> entry: vosMap.entrySet()) {
			if (memberVoStatuses.containsKey(entry.getKey())) {
				String status = memberVoStatuses.get(entry.getKey());
				if (status.equalsIgnoreCase("VALID") || status.equalsIgnoreCase("EXPIRED")) {
					vosForRegistration.put(entry.getKey(), entry.getValue());
				}
			} else {
				vosForRegistration.put(entry.getKey(), entry.getValue());
			}
		}

		// filter groups only if their VO is in the allowed VOs and if they have registration form
		List<Group> allowedGroups = getAllowedGroups(facility);
		List<Group> groupsForRegistration = allowedGroups.stream()
				.filter(group -> vosForRegistration.containsKey(group.getVoId()) && getApplicationForm(group))
				.collect(Collectors.toList());

		// create map for processing
		Map<Vo, List<Group>> result = new HashMap<>();
		for (Group group: groupsForRegistration) {
			Vo vo = vosMap.get(group.getVoId());
			if (! result.containsKey(vo)) {
				result.put(vo, new ArrayList<>());
			}
			List<Group> list = result.get(vo);
			list.add(group);
		}

		log.trace("getGroupsForRegistration({}, {}, {}) returns: {}", facility, userId, voShortNames, result);
		return result;
	}

	private Map<Long, String> convertMembersListToStatusesMap(List<Member> userMembers) {
		Map<Long, String> res = new HashMap<>();
		for (Member m: userMembers) {
			res.put(m.getVoId(), m.getStatus());
		}

		return res;
	}

	@Override
	public boolean groupWhereCanRegisterExists(Facility facility) {
		log.trace("groupsWhereCanRegisterExists({})", facility);
		List<Group> allowedGroups = getAllowedGroups(facility);

		if (allowedGroups != null && !allowedGroups.isEmpty()) {
			for (Group group: allowedGroups) {
				if (getApplicationForm(group)) {
					log.trace("groupsWhereCanRegisterExists({}) returns: true", facility);
					return true;
				}
			}
		}

		log.trace("groupsWhereCanRegisterExists({}) returns: false", facility);
		return false;
	}

	@Override
	public Map<String, PerunAttribute> getFacilityAttributes(Facility facility, List<String> attributeNames) {
		log.trace("getFacilityAttributes({}, {})", facility, attributeNames);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facility.getId());
		map.put("attrNames", attributeNames);
		JsonNode res = makeRpcCall("/attributesManager/getAttributes", map);

		Map<String, PerunAttribute> attrs = Mapper.mapAttributes(res);
		log.trace("getFacilityAttributes({}, {}) returns: {}", facility, attributeNames, attrs);
		return attrs;
	}

	public PerunAttribute getFacilityAttribute(Facility facility, String attributeName) {
		log.trace("getFacilityAttribute({}, {})", facility, attributeName);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facility.getId());
		map.put("attributeName", attributeName);
		JsonNode res = makeRpcCall("/attributesManager/getAttribute", map);

		PerunAttribute attr = Mapper.mapAttribute(res);
		log.trace("getFacilityAttribute({}, {}) returns: {}", facility, attributeName, attr);
		return attr;
	}

	private List<Member> getMembersByUser(Long userId) {
		log.trace("getMemberByUser({})", userId);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("user", userId);
		JsonNode jsonNode = makeRpcCall("/membersManager/getMembersByUser", params);

		List<Member> userMembers = Mapper.mapMembers(jsonNode);
		log.trace("getMembersByUser({}) returns: {}", userId, userMembers);
		return userMembers;
	}

	private List<Vo> getVosByShortNames(List<String> voShortNames) {
		log.trace("getVosByShortNames({})", voShortNames);
		List<Vo> vos = new ArrayList<>();
		for (String shortName: voShortNames) {
			Vo vo = getVoByShortName(shortName);
			vos.add(vo);
		}

		log.trace("getVosByShortNames({}) returns: {}", voShortNames, vos);
		return vos;
	}

	private Vo getVoByShortName(String shortName) {
		log.trace("getVoByShortName({})", shortName);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("shortName", shortName);
		JsonNode jsonNode = makeRpcCall("/vosManager/getVoByShortName", params);

		Vo vo = Mapper.mapVo(jsonNode);
		log.trace("getVoByShortName({}) returns: {}", shortName, vo);
		return vo;
	}

	private List<Group> getAllowedGroups(Facility facility) {
		log.trace("getAllowedGroups({})", facility);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facility.getId());
		JsonNode jsonNode = makeRpcCall("/facilitiesManager/getAllowedGroups", map);
		List<Group> result = new ArrayList<>();
		for (int i = 0; i < jsonNode.size(); i++) {
			JsonNode groupNode = jsonNode.get(i);
			result.add(Mapper.mapGroup(groupNode));
		}

		log.trace("getAllowedGroups({}) returns: {}", facility, result);
		return result;
	}

	private boolean getApplicationForm(Group group) {
		log.trace("getApplicationForm({})", group);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("group", group.getId());
		try {
			if (group.getName().equalsIgnoreCase("members")) {
				log.trace("getApplicationForm({}) continues to call regForm for VO {}", group, group.getVoId());
				return getApplicationForm(group.getVoId());
			} else {
				makeRpcCall("/registrarManager/getApplicationForm", map);
			}
		} catch (Exception e) {
			// when group does not have form exception is thrown. Every error thus is supposed as group without form
			// this method will be used after calling other RPC methods - if RPC is not available other methods should discover it first
			log.trace("getApplicationForm({}) returns: false", group);
			return false;
		}

		log.trace("getApplicationForm({}) returns: true", group);
		return true;
	}

	private boolean getApplicationForm(Long voId) {
		log.trace("getApplicationForm({})", voId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("vo", voId);
		try {
			makeRpcCall("/registrarManager/getApplicationForm", map);
		} catch (Exception e) {
			// when vo does not have form exception is thrown. Every error thus is supposed as vo without form
			// this method will be used after calling other RPC methods - if RPC is not available other methods should discover it first
			log.trace("getApplicationForm({}) returns: false", voId);
			return false;
		}

		log.trace("getApplicationForm({}) returns: true", voId);
		return true;
	}

	private List<Group> getGroupsWhereUserIsActive(Facility facility, Long userId) {
		log.trace("getGroupsWhereUserIsActive({}, {})", facility, userId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facility.getId());
		map.put("user", userId);
		JsonNode jsonNode = makeRpcCall("/usersManager/getGroupsWhereUserIsActive", map);

		List<Group> res = Mapper.mapGroups(jsonNode);
		log.trace("getGroupsWhereUserIsActive({}, {}) returns: {}", facility, userId, res);
		return res;
	}

	private Map<Long, Vo> convertVoListToMap(List<Vo> vos) {
		Map<Long, Vo> map = new HashMap<>();
		for (Vo vo: vos) {
			map.put(vo.getId(), vo);
		}

		return map;
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
