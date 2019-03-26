package cz.muni.ics.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.Group;
import cz.muni.ics.oidc.models.Member;
import cz.muni.ics.oidc.models.PerunAttribute;
import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.models.RichUser;
import cz.muni.ics.oidc.models.Vo;
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

	@Override
	public boolean canUserAccessBasedOnMembership(Facility facility, Long userId) {
		List<Group> activeGroups = getGroupsWhereUserIsActive(facility, userId);

		return (activeGroups != null && !activeGroups.isEmpty());
	}

	@Override
	public boolean groupWhereCanRegisterExists(Facility facility) {
		List<Group> allowedGroups = getAllowedGroups(facility);

		if (allowedGroups != null && !allowedGroups.isEmpty()) {
			for (Group group: allowedGroups) {
				if (getApplicationForm(group)) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public Map<String, PerunAttribute> getFacilityAttributes(Facility facility, List<String> attributes) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facility.getId());
		map.put("attrNames", attributes);
		JsonNode res = makeRpcCall("/attributesManager/getAttributes", map);

		Map<String, PerunAttribute> attrs = Mapper.mapAttributes(res);
		log.trace("getFacilityAttributes returns: {}", attrs);
		return attrs;
	}

	@Override
	public Map<Vo, List<Group>> getGroupsForRegistration(Facility facility, Long userId, List<String> voShortNames) {
		List<Vo> vos = getVosByShortNames(voShortNames);
		Map<Long, Vo> vosMap = convertVoListToMap(vos);
		List<Member> userMembers = getMembersByUser(userId);
		userMembers = new ArrayList<>(new HashSet<>(userMembers));

		List<Member> validAndExpiredMembers = userMembers.stream()
				.filter(member -> ("VALID".equalsIgnoreCase(member.getStatus()) || "EXPIRED".equalsIgnoreCase(member.getStatus())))
				.collect(Collectors.toList());
		Map<Long, Vo> vosForRegistration = new HashMap<>();
		for(Member m: validAndExpiredMembers) {
			if (vosMap.containsKey(m.getVoId())) {
				vosForRegistration.put(m.getVoId(), vosMap.get(m.getVoId()));
			}
		}

		List<Group> allowedGroups = getAllowedGroups(facility);
		List<Group> groupsWithForms = allowedGroups.stream()
				.filter(this::getApplicationForm)
				.collect(Collectors.toList());
		List<Group> groupsForRegistration = groupsWithForms.stream()
				.filter(group -> vosForRegistration.containsKey(group.getVoId()))
				.collect(Collectors.toList());
		Map<Vo, List<Group>> result = new HashMap<>();
		for (Group group: groupsForRegistration) {
			Vo vo = vosForRegistration.get(group.getVoId());
			if (! result.containsKey(vo)) {
				result.put(vo, new ArrayList<>());
			}
			List<Group> list = result.get(vo);
			list.add(group);
		}

		return result;
	}

	private List<Member> getMembersByUser(Long userId) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("user", userId);
		JsonNode jsonNode = makeRpcCall("/membersManager/getMembersByUser", params);

		List<Member> userMembers = Mapper.mapMembers(jsonNode);
		log.trace("getMembersByUser returns: {}", userMembers);
		return userMembers;
	}

	private Map<Long, Vo> convertVoListToMap(List<Vo> vos) {
		Map<Long, Vo> map = new HashMap<>();
		for (Vo vo: vos) {
			map.put(vo.getId(), vo);
		}

		return map;
	}

	private List<Vo> getVosByShortNames(List<String> voShortNames) {
		List<Vo> vos = new ArrayList<>();
		for (String shortName: voShortNames) {
			Vo vo = getVoByShortName(shortName);
			vos.add(vo);
		}

		log.trace("getVosByShortNames returns: {}", vos);
		return vos;
	}

	private Vo getVoByShortName(String shortName) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("shortName", shortName);
		JsonNode jsonNode = makeRpcCall("/vosManager/getVoByShortName", params);

		Vo vo = Mapper.mapVo(jsonNode);
		log.trace("getVoByShortName returns: {}", vo);
		return vo;
	}

	private List<Group> getAllowedGroups(Facility facility) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facility.getId());
		JsonNode jsonNode = makeRpcCall("/facilitiesManager/getAllowedGroups", map);
		List<Group> result = new ArrayList<>();
		for (int i = 0; i < jsonNode.size(); i++) {
			JsonNode groupNode = jsonNode.get(i);
			result.add(Mapper.mapGroup(groupNode));
		}

		return result;
	}

	private boolean getApplicationForm(Group group) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("group", group.getId());
		try {
			if (group.getName().equalsIgnoreCase("members")) {
				return getApplicationForm(group.getVoId());
			} else {
				makeRpcCall("/registrarManager/getApplicationForm", map);
			}
		} catch (Exception e) {
			// when group does not have form exception is thrown. Every error thus is supposed as group without form
			// this method will be used after calling other RPC methods - if RPC is not available other methods should discover it first
			return false;
		}

		return true;
	}

	private boolean getApplicationForm(Long voId) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("vo", voId);
		try {
			makeRpcCall("/registrarManager/getApplicationForm", map);
		} catch (Exception e) {
			// when vo does not have form exception is thrown. Every error thus is supposed as vo without form
			// this method will be used after calling other RPC methods - if RPC is not available other methods should discover it first
			return false;
		}

		return true;
	}

	private List<Group> getGroupsWhereUserIsActive(Facility facility, Long userId) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facility.getId());
		map.put("user", userId);
		JsonNode jsonNode = makeRpcCall("/usersManager/getGroupsWhereUserIsActive", map);

		return Mapper.mapGroups(jsonNode);
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
