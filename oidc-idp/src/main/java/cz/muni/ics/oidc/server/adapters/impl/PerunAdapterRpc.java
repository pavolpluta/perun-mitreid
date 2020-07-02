package cz.muni.ics.oidc.server.adapters.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import cz.muni.ics.oidc.models.AttributeMapping;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.Group;
import cz.muni.ics.oidc.models.Member;
import cz.muni.ics.oidc.models.PerunAttribute;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.models.Resource;
import cz.muni.ics.oidc.models.UserExtSource;
import cz.muni.ics.oidc.models.Vo;
import cz.muni.ics.oidc.models.enums.MemberStatus;
import cz.muni.ics.oidc.models.enums.PerunEntityType;
import cz.muni.ics.oidc.models.mappers.RpcMapper;
import cz.muni.ics.oidc.server.PerunPrincipal;
import cz.muni.ics.oidc.server.adapters.PerunAdapterMethods;
import cz.muni.ics.oidc.server.adapters.PerunAdapterMethodsRpc;
import cz.muni.ics.oidc.server.connectors.Affiliation;
import cz.muni.ics.oidc.server.connectors.PerunConnectorRpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.muni.ics.oidc.models.PerunAttributeValue.STRING_TYPE;
import static cz.muni.ics.oidc.models.enums.MemberStatus.VALID;
import static cz.muni.ics.oidc.server.connectors.PerunConnectorRpc.ATTRIBUTES_MANAGER;
import static cz.muni.ics.oidc.server.connectors.PerunConnectorRpc.FACILITIES_MANAGER;
import static cz.muni.ics.oidc.server.connectors.PerunConnectorRpc.GROUPS_MANAGER;
import static cz.muni.ics.oidc.server.connectors.PerunConnectorRpc.MEMBERS_MANAGER;
import static cz.muni.ics.oidc.server.connectors.PerunConnectorRpc.REGISTRAR_MANAGER;
import static cz.muni.ics.oidc.server.connectors.PerunConnectorRpc.RESOURCES_MANAGER;
import static cz.muni.ics.oidc.server.connectors.PerunConnectorRpc.USERS_MANAGER;
import static cz.muni.ics.oidc.server.connectors.PerunConnectorRpc.VOS_MANAGER;

/**
 * Interface for fetching data from Perun via RPC
 *
 * @author Martin Kuba makub@ics.muni.cz
 * @author Dominik František Bučík bucik@ics.muni.cz
 * @author Peter Jancus jancus@ics.muni.cz
 */
public class PerunAdapterRpc extends PerunAdapterWithMappingServices implements PerunAdapterMethods, PerunAdapterMethodsRpc {

	private final static Logger log = LoggerFactory.getLogger(PerunAdapterRpc.class);

	private PerunConnectorRpc connectorRpc;

	private String oidcClientIdAttr;
	private String oidcCheckMembershipAttr;
	private String orgUrlAttr;
	private String affiliationsAttr;

	public void setConnectorRpc(PerunConnectorRpc connectorRpc) {
		this.connectorRpc = connectorRpc;
	}

	public void setOidcClientIdAttr(String oidcClientIdAttr) {
		this.oidcClientIdAttr = oidcClientIdAttr;
	}

	public void setOidcCheckMembershipAttr(String oidcCheckMembershipAttr) {
		this.oidcCheckMembershipAttr = oidcCheckMembershipAttr;
	}

	public void setOrgUrlAttr(String orgUrlAttr) {
		this.orgUrlAttr = orgUrlAttr;
	}

	public void setAffiliationsAttr(String affiliationsAttr) {
		this.affiliationsAttr = affiliationsAttr;
	}

	@Override
	public PerunUser getPreauthenticatedUserId(PerunPrincipal perunPrincipal) {
		log.trace("getPreauthenticatedUserId({})", perunPrincipal);
		if (!this.connectorRpc.isEnabled()) {
			return null;
		}
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("extLogin", perunPrincipal.getExtLogin());
		map.put("extSourceName", perunPrincipal.getExtSourceName());

		JsonNode response = connectorRpc.post(USERS_MANAGER, "getUserByExtSourceNameAndExtLogin", map);
		PerunUser res = RpcMapper.mapPerunUser(response);
		log.trace("getPreauthenticatedUserId({}) returns: {}", perunPrincipal, res);
		return res;
	}

	@Override
	public Facility getFacilityByClientId(String clientId) {
		log.trace("getFacilitiesByClientId({})", clientId);
		if (!this.connectorRpc.isEnabled()) {
			return null;
		}

		AttributeMapping mapping = this.getFacilityAttributesMappingService().getByName(oidcClientIdAttr);

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("attributeName", mapping.getRpcName());
		map.put("attributeValue", clientId);
		JsonNode jsonNode = connectorRpc.post(FACILITIES_MANAGER, "getFacilitiesByAttribute", map);

		Facility facility = (jsonNode.size() > 0) ? RpcMapper.mapFacility(jsonNode.get(0)) : null;
		log.trace("getFacilitiesByClientId({}) returns {}", clientId, facility);
		return facility;
	}

	@Override
	public boolean isMembershipCheckEnabledOnFacility(Facility facility) {
		if (!this.connectorRpc.isEnabled()) {
			return false;
		}

		log.trace("isMembershipCheckEnabledOnFacility({})", facility);
		AttributeMapping mapping = this.getFacilityAttributesMappingService().getByName(oidcCheckMembershipAttr);

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facility.getId());
		map.put("attributeName", mapping.getRpcName());
		JsonNode res = connectorRpc.post(ATTRIBUTES_MANAGER, "getAttribute", map);

		boolean result = res.get("value").asBoolean(false);
		log.trace("isMembershipCheckEnabledOnFacility({}) returns {}", facility, result);
		return result;
	}

	@Override
	public boolean canUserAccessBasedOnMembership(Facility facility, Long userId) {
		if (!this.connectorRpc.isEnabled()) {
			return true;
		}

		log.trace("canUserAccessBasedOnMembership({}, {})", facility, userId);
		List<Group> activeGroups = getGroupsWhereUserIsActive(facility, userId);

		boolean res = !activeGroups.isEmpty();
		log.trace("canUserAccessBasedOnMembership({}, {}) returns: {}", facility, userId, res);
		return res;
	}

	@Override
	public Map<Vo, List<Group>> getGroupsForRegistration(Facility facility, Long userId, List<String> voShortNames) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}

		log.trace("getGroupsForRegistration({}, {}, {})", facility, userId, voShortNames);
		List<Vo> vos = getVosByShortNames(voShortNames);
		Map<Long, Vo> vosMap = convertVoListToMap(vos);
		List<Member> userMembers = getMembersByUser(userId);
		userMembers = new ArrayList<>(new HashSet<>(userMembers));

		//Filter out vos where member is other than valid or expired. These vos cannot be used for registration
		Map<Long, MemberStatus> memberVoStatuses = convertMembersListToStatusesMap(userMembers);
		Map<Long, Vo> vosForRegistration = new HashMap<>();
		for (Map.Entry<Long, Vo> entry : vosMap.entrySet()) {
			if (memberVoStatuses.containsKey(entry.getKey())) {
				MemberStatus status = memberVoStatuses.get(entry.getKey());
				if (VALID.equals(status) || MemberStatus.EXPIRED.equals(status)) {
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
		for (Group group : groupsForRegistration) {
			Vo vo = vosMap.get(group.getVoId());
			if (!result.containsKey(vo)) {
				result.put(vo, new ArrayList<>());
			}
			List<Group> list = result.get(vo);
			list.add(group);
		}

		log.trace("getGroupsForRegistration({}, {}, {}) returns: {}", facility, userId, voShortNames, result);
		return result;
	}

	@Override
	public boolean groupWhereCanRegisterExists(Facility facility) {
		if (!this.connectorRpc.isEnabled()) {
			return false;
		}

		log.trace("groupsWhereCanRegisterExists({})", facility);
		List<Group> allowedGroups = getAllowedGroups(facility);

		if (!allowedGroups.isEmpty()) {
			for (Group group : allowedGroups) {
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
	public boolean isUserInGroup(Long userId, Long groupId) {
		if (!this.connectorRpc.isEnabled()) {
			return false;
		}

		Map<String, Object> groupParams = new LinkedHashMap<>();
		groupParams.put("id", groupId);
		JsonNode groupResponse = connectorRpc.post(GROUPS_MANAGER, "getGroupById", groupParams);
		Group group = RpcMapper.mapGroup(groupResponse);

		Map<String, Object> memberParams = new LinkedHashMap<>();
		memberParams.put("vo", group.getVoId());
		memberParams.put("user", userId);
		JsonNode memberResponse = connectorRpc.post(MEMBERS_MANAGER, "getMemberByUser", memberParams);
		Member member = RpcMapper.mapMember(memberResponse);

		Map<String, Object> isGroupMemberParams = new LinkedHashMap<>();
		isGroupMemberParams.put("group", groupId);
		isGroupMemberParams.put("member", member.getId());
		JsonNode res = connectorRpc.post(GROUPS_MANAGER, "isGroupMember", isGroupMemberParams);

		boolean result = res.asBoolean(false);

		log.trace("isUserInGroup(userId={},group={}) returns {}", userId, group.getName(), result);
		return result;
	}

	@Override
	public boolean setUserAttribute(Long userId, PerunAttribute attribute) {
		if (!this.connectorRpc.isEnabled()) {
			return true;
		}

		log.trace("setUserAttribute(user={}, attribute={})", userId, attribute);
		JsonNode attributeJson = attribute.toJson();

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("user", userId);
		map.put("attribute", attributeJson);

		JsonNode response = connectorRpc.post(ATTRIBUTES_MANAGER, "setAttribute", map);
		boolean successful = (response == null || response.isNull() || response instanceof NullNode);

		log.trace("setUserAttribute({}, {}) returns {}", userId, attribute, successful);
		return successful;
	}

	@Override
	public List<Affiliation> getUserExtSourcesAffiliations(Long userId) {
		if (!this.connectorRpc.isEnabled()) {
			return new ArrayList<>();
		}

		log.trace("getUserExtSourcesAffiliations({})", userId);

		List<UserExtSource> userExtSources = getUserExtSources(userId);
		List<Affiliation> affiliations = new ArrayList<>();

		AttributeMapping affMapping = new AttributeMapping("affMapping", affiliationsAttr, "", PerunAttributeValue.ARRAY_TYPE);
		AttributeMapping orgUrlMapping = new AttributeMapping("orgUrl", orgUrlAttr, "", STRING_TYPE);
		Set<AttributeMapping> attributeMappings = new HashSet<>(Arrays.asList(affMapping, orgUrlMapping));

		for (UserExtSource ues : userExtSources) {
			if ("cz.metacentrum.perun.core.impl.ExtSourceIdp".equals(ues.getExtSource().getType())) {
				Map<String, PerunAttributeValue> uesAttrValues = getUserExtSourceAttributeValues(ues.getId(), attributeMappings);

				long asserted = ues.getLastAccess().getTime() / 1000L;

				String orgUrl = uesAttrValues.get(orgUrlMapping.getIdentifier()).valueAsString();
				String affs = uesAttrValues.get(affMapping.getIdentifier()).valueAsString();
				if (affs != null) {
					for (String aff : affs.split(";")) {
						String source = ( (orgUrl != null) ? orgUrl : ues.getExtSource().getName() );
						Affiliation affiliation = new Affiliation(source, aff, asserted);
						log.debug("found {} from IdP {} with orgURL {} asserted at {}", aff, ues.getExtSource().getName(),
								orgUrl, asserted);
						affiliations.add(affiliation);
					}
				}
			}
		}

		log.trace("getUserExtSourcesAffiliations({}) returns: {}", userId, affiliations);
		return affiliations;
	}

	@Override
	public List<Affiliation> getGroupAffiliations(Long userId, String groupAffiliationsAttr) {
		if (!this.connectorRpc.isEnabled()) {
			return new ArrayList<>();
		}

		log.trace("getGroupAffiliations({})", userId);
		List<Affiliation> affiliations = new ArrayList<>();

		List<Member> userMembers = getMembersByUser(userId);
		for (Member member : userMembers) {
			if (VALID.equals(member.getStatus())) {
				List<Group> memberGroups = getMemberGroups(member.getId());
				for (Group group : memberGroups) {
					PerunAttributeValue attrValue = this.getGroupAttributeValue(group, groupAffiliationsAttr);
					if (attrValue.getValue() != null) {
						long linuxTime = System.currentTimeMillis() / 1000L;
						for (String value : attrValue.valueAsList()) {
							Affiliation affiliation = new Affiliation(null, value, linuxTime);
							log.debug("found {} on group {}", value, group.getName());
							affiliations.add(affiliation);
						}
					}
				}
			}
		}

		log.trace("getGroupAffiliations({}) returns: {}", userId, affiliations);
		return affiliations;
	}

	@Override
	public List<String> getGroupsAssignedToResourcesWithUniqueNames(Facility facility) {
		if (!this.connectorRpc.isEnabled()) {
			return new ArrayList<>();
		}

		log.trace("getGroupsAssignedToResourcesWithUniqueNames({})", facility);
		List<Resource> resources = getAssignedResources(facility);
		List<String> result = new ArrayList<>();

		String voShortName = "urn:perun:group:attribute-def:virt:voShortName";

		for (Resource res : resources) {
			List<Group> groups = getRichGroupsAssignedToResourceWithAttributesByNames(res, Collections.singletonList(voShortName));

			for (Group group : groups) {
				if (group.getAttributeByUrnName(voShortName) != null &&
						group.getAttributeByUrnName(voShortName).hasNonNull("value")) {
					String value = group.getAttributeByUrnName(voShortName).get("value").textValue();
					group.setUniqueGroupName(value + ":" + group.getName());
					result.add(group.getUniqueGroupName());
				}
			}
		}

		log.trace("getGroupsAssignedToResourcesWithUniqueNames({}) returns: {}", facility, result);
		return result;
	}

	@Override
	public Map<String, PerunAttribute> getEntitylessAttributes(String attributeName) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}

		log.trace("getEntitylessAttributes({})", attributeName);

		Map<String, Object> attrNameMap = new LinkedHashMap<>();
		attrNameMap.put("attrName", attributeName);
		JsonNode entitylessAttributesJson = connectorRpc.post(ATTRIBUTES_MANAGER, "getEntitylessAttributes", attrNameMap);

		Long attributeDefinitionId = RpcMapper.mapAttribute(entitylessAttributesJson.get(0)).getId();

		Map<String, Object> attributeDefinitionIdMap = new LinkedHashMap<>();
		attributeDefinitionIdMap.put("attributeDefinition", attributeDefinitionId);
		JsonNode entitylessKeysJson = connectorRpc.post(ATTRIBUTES_MANAGER, "getEntitylessKeys", attributeDefinitionIdMap);

		Map<String, PerunAttribute> result = new LinkedHashMap<>();

		for(int i = 0; i < entitylessKeysJson.size(); i++) {
			result.put(entitylessKeysJson.get(i).asText(), RpcMapper.mapAttribute(entitylessAttributesJson.get(i)));
		}

		if (result.size() == 0) {
			return null;
		}

		log.trace("getEntitylessAttributes({}) returns {}", attributeName, result);
		return result;
	}

	@Override
	public Vo getVoByShortName(String shortName) {
		if (!this.connectorRpc.isEnabled()) {
			return null;
		}

		log.trace("getVoByShortName({})", shortName);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("shortName", shortName);

		JsonNode jsonNode = connectorRpc.post(VOS_MANAGER, "getVoByShortName", params);
		Vo vo = RpcMapper.mapVo(jsonNode);

		log.trace("getVoByShortName({}) returns: {}", shortName, vo);
		return vo;
	}

	@Override
	public Map<String, PerunAttributeValue> getUserAttributeValues(PerunUser user, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}

		log.trace("getUserAttributeValues({}, {})", user, attrsToFetch);
		return this.getUserAttributeValues(user.getId(), attrsToFetch);
	}

	@Override
	public Map<String, PerunAttributeValue> getUserAttributeValues(Long userId, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}

		log.trace("getUserAttributeValues({}, {})", userId, attrsToFetch);
		Map<String, PerunAttribute> userAttributes = this.getUserAttributes(userId, attrsToFetch);
		Map<String, PerunAttributeValue> valueMap = extractValues(userAttributes);

		log.trace("getUserAttributeValues({}, {}) returns: {}", userId, attrsToFetch, valueMap);
		return valueMap;
	}

	@Override
	public PerunAttributeValue getUserAttributeValue(PerunUser user, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttributeValue.NULL;
		}

		log.trace("getUserAttributeValue({}, {})", user, attrToFetch);
		return this.getUserAttributeValue(user.getId(), attrToFetch);
	}

	@Override
	public PerunAttributeValue getUserAttributeValue(Long userId, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttributeValue.NULL;
		}

		log.trace("getUserAttributeValue({}, {})", userId, attrToFetch);
		PerunAttributeValue value = this.getUserAttribute(userId, attrToFetch).getValue();

		log.trace("getUserAttributeValue({}, {}) returns: {}", userId, attrToFetch, value);
		return value;
	}

	@Override
	public Map<String, PerunAttributeValue> getFacilityAttributeValues(Facility facility, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}

		log.trace("getFacilityAttributeValues({}, {})", facility, attrsToFetch);
		return this.getFacilityAttributeValues(facility.getId(), attrsToFetch);
	}

	@Override
	public Map<String, PerunAttributeValue> getFacilityAttributeValues(Long facilityId, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}

		log.trace("getFacilityAttributeValues({}, {})", facilityId, attrsToFetch);
		Map<String, PerunAttribute> facilityAttributes = this.getFacilityAttributes(facilityId, attrsToFetch);
		Map<String, PerunAttributeValue> valueMap = extractValues(facilityAttributes);

		log.trace("getFacilityAttributeValues({}, {}) returns: {}", facilityId, attrsToFetch, valueMap);
		return valueMap;
	}

	@Override
	public PerunAttributeValue getFacilityAttributeValue(Facility facility, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttributeValue.NULL;
		}

		log.trace("getFacilityAttributeValue({}, {})", facility, attrToFetch);
		return this.getGroupAttributeValue(facility.getId(), attrToFetch);
	}

	@Override
	public PerunAttributeValue getFacilityAttributeValue(Long facilityId, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttributeValue.NULL;
		}

		log.trace("getFacilityAttributeValue({}, {})", facilityId, attrToFetch);
		PerunAttributeValue value = this.getFacilityAttribute(facilityId, attrToFetch).getValue();

		log.trace("getFacilityAttributeValue({}, {}) returns: {}", facilityId, attrToFetch, value);
		return value;
	}

	@Override
	public Map<String, PerunAttributeValue> getVoAttributeValues(Vo vo, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}

		log.trace("getVoAttributeValues({}, {})", vo, attrsToFetch);
		return this.getVoAttributeValues(vo.getId(), attrsToFetch);
	}

	@Override
	public Map<String, PerunAttributeValue> getVoAttributeValues(Long voId, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}

		log.trace("getVoAttributeValues({}, {})", voId, attrsToFetch);
		Map<String, PerunAttribute> voAttributes = this.getVoAttributes(voId, attrsToFetch);
		Map<String, PerunAttributeValue> valueMap = extractValues(voAttributes);

		log.trace("getVoAttributeValues({}, {}) returns: {}", voId, attrsToFetch, valueMap);
		return valueMap;
	}

	@Override
	public PerunAttributeValue getVoAttributeValue(Vo vo, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttributeValue.NULL;
		}

		log.trace("getVoAttributeValue({}, {})", vo, attrToFetch);
		return this.getVoAttributeValue(vo.getId(), attrToFetch);
	}

	@Override
	public PerunAttributeValue getVoAttributeValue(Long voId, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttributeValue.NULL;
		}

		log.trace("getVoAttributeValue({}, {})", voId, attrToFetch);
		PerunAttributeValue value = this.getFacilityAttribute(voId, attrToFetch).getValue();

		log.trace("getVoAttributeValue({}, {}) returns: {}", voId, attrToFetch, value);
		return value;
	}

	@Override
	public Map<String, PerunAttributeValue> getGroupAttributeValues(Group group, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}

		log.trace("getGroupAttributeValues({}, {})", group, attrsToFetch);
		return this.getGroupAttributeValues(group.getId(), attrsToFetch);
	}

	@Override
	public Map<String, PerunAttributeValue> getGroupAttributeValues(Long groupId, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}

		log.trace("getGroupAttributeValues({}, {})", groupId, attrsToFetch);
		Map<String, PerunAttribute> groupAttributes = this.getGroupAttributes(groupId, attrsToFetch);
		Map<String, PerunAttributeValue> valueMap = extractValues(groupAttributes);

		log.trace("getGroupAttributeValues({}, {}) returns: {}", groupId, attrsToFetch, valueMap);
		return valueMap;
	}

	@Override
	public PerunAttributeValue getGroupAttributeValue(Group group, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttributeValue.NULL;
		}

		log.trace("getGroupAttributeValue({}, {})", group, attrToFetch);
		return this.getGroupAttributeValue(group.getId(), attrToFetch);
	}

	@Override
	public PerunAttributeValue getGroupAttributeValue(Long groupId, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttributeValue.NULL;
		}

		log.trace("getGroupAttributeValue({}, {})", groupId, attrToFetch);
		PerunAttributeValue value = this.getGroupAttribute(groupId, attrToFetch).getValue();

		log.trace("getGroupAttributeValue({}, {}) returns: {}", groupId, attrToFetch, value);
		return value;
	}

	@Override
	public Map<String, PerunAttributeValue> getResourceAttributeValues(Resource resource, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}

		log.trace("getResourceAttributeValues({}, {})", resource, attrsToFetch);
		return this.getResourceAttributeValues(resource.getId(), attrsToFetch);
	}

	@Override
	public Map<String, PerunAttributeValue> getResourceAttributeValues(Long resourceId, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}
		
		log.trace("getResourceAttributeValues({}, {})", resourceId, attrsToFetch);
		Map<String, PerunAttribute> resourceAttributes = this.getResourceAttributes(resourceId, attrsToFetch);
		Map<String, PerunAttributeValue> valueMap = extractValues(resourceAttributes);

		log.trace("getResourceAttributeValues({}, {}) returns: {}", resourceId, attrsToFetch, valueMap);
		return valueMap;
	}

	@Override
	public PerunAttributeValue getResourceAttributeValue(Resource resource, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttributeValue.NULL;
		}
		
		log.trace("getResourceAttributeValue({}, {})", resource, attrToFetch);
		return this.getResourceAttributeValue(resource.getId(), attrToFetch);
	}

	@Override
	public PerunAttributeValue getResourceAttributeValue(Long resourceId, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttributeValue.NULL;
		}
		
		log.trace("getResourceAttributeValue({}, {})", resourceId, attrToFetch);
		PerunAttributeValue value = this.getResourceAttribute(resourceId, attrToFetch).getValue();

		log.trace("getResourceAttributeValue({}, {}) returns: {}", resourceId, attrToFetch, value);
		return value;
	}

	@Override
	public Map<String, PerunAttribute> getFacilityAttributes(Facility facility, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}
		
		log.trace("getFacilityAttributes({}, {})", facility, attrsToFetch);
		return this.getFacilityAttributes(facility.getId(), attrsToFetch);
	}

	@Override
	public Map<String, PerunAttribute> getFacilityAttributes(Long facilityId, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}
		
		log.trace("getFacilityAttributes({}, {})", facilityId, attrsToFetch);

		Map<String, PerunAttribute> attributes = getAttributes(PerunEntityType.FACILITY, facilityId, attrsToFetch);

		log.trace("getFacilityAttributes({}, {}) returns: {}", facilityId, attrsToFetch, attributes);
		return attributes;
	}

	@Override
	public PerunAttribute getFacilityAttribute(Facility facility, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttribute.NULL;
		}
		
		log.trace("getFacilityAttribute({}, {})", facility, attrToFetch);
		return this.getFacilityAttribute(facility.getId(), attrToFetch);
	}

	@Override
	public PerunAttribute getFacilityAttribute(Long facilityId, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttribute.NULL;
		}
		
		log.trace("getFacilityAttribute({}, {})", facilityId, attrToFetch);

		PerunAttribute attribute = getAttribute(PerunEntityType.FACILITY, facilityId, attrToFetch);

		log.trace("getFacilityAttribute({}, {}) returns: {}", facilityId, attrToFetch, attribute);
		return attribute;
	}

	@Override
	public Map<String, PerunAttribute> getGroupAttributes(Group group, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}
		
		log.trace("getGroupAttributes({}, {})", group, attrsToFetch);
		return this.getGroupAttributes(group.getId(), attrsToFetch);
	}

	@Override
	public Map<String, PerunAttribute> getGroupAttributes(Long groupId, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}
		
		log.trace("getGroupAttributes({}, {})", groupId, attrsToFetch);

		Map<String, PerunAttribute> attributes = getAttributes(PerunEntityType.GROUP, groupId, attrsToFetch);

		log.trace("getGroupAttributes({}, {}) returns: {}", groupId, attrsToFetch, attributes);
		return attributes;
	}

	@Override
	public PerunAttribute getGroupAttribute(Group group, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttribute.NULL;
		}
		
		log.trace("getGroupAttribute({}, {})", group, attrToFetch);
		return this.getGroupAttribute(group.getId(), attrToFetch);
	}

	@Override
	public PerunAttribute getGroupAttribute(Long groupId, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttribute.NULL;
		}
		
		log.trace("getGroupAttribute({}, {})", groupId, attrToFetch);

		PerunAttribute attribute = getAttribute(PerunEntityType.GROUP, groupId, attrToFetch);

		log.trace("getGroupAttribute({}, {}) returns: {}", groupId, attrToFetch, attribute);
		return attribute;
	}

	@Override
	public Map<String, PerunAttribute> getUserAttributes(PerunUser user, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}
		
		log.trace("getUserAttributes({}, {})", user, attrsToFetch);
		return this.getUserAttributes(user.getId(), attrsToFetch);
	}

	@Override
	public Map<String, PerunAttribute> getUserAttributes(Long userId, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}
		
		log.trace("getUserAttributes({}, {})", userId, attrsToFetch);

		Map<String, PerunAttribute> attributes = getAttributes(PerunEntityType.USER, userId, attrsToFetch);

		log.trace("getUserAttributes({}, {}) returns: {}", userId, attrsToFetch, attributes);
		return attributes;
	}

	@Override
	public PerunAttribute getUserAttribute(PerunUser user, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttribute.NULL;
		}
		
		log.trace("getUserAttribute({}, {})", user, attrToFetch);
		return this.getUserAttribute(user.getId(), attrToFetch);
	}

	@Override
	public PerunAttribute getUserAttribute(Long userId, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttribute.NULL;
		}
		
		log.trace("getUserAttribute({}, {})", userId, attrToFetch);

		PerunAttribute attribute = getAttribute(PerunEntityType.USER, userId, attrToFetch);

		log.trace("getUserAttribute({}, {}) returns: {}", userId, attrToFetch, attribute);
		return attribute;
	}

	@Override
	public Map<String, PerunAttribute> getVoAttributes(Vo vo, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}
		
		log.trace("getVoAttributes({}, {})", vo, attrsToFetch);
		return this.getVoAttributes(vo.getId(), attrsToFetch);
	}

	@Override
	public Map<String, PerunAttribute> getVoAttributes(Long voId, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}
		
		log.trace("getVoAttributes({}, {})", voId, attrsToFetch);

		Map<String, PerunAttribute> attributes = getAttributes(PerunEntityType.VO, voId, attrsToFetch);

		log.trace("getVoAttributes({}, {}) returns: {}", voId, attrsToFetch, attributes);
		return attributes;
	}

	@Override
	public PerunAttribute getVoAttribute(Vo vo, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttribute.NULL;
		}
		
		log.trace("getvoAttribute({}, {})", vo, attrToFetch);
		return this.getVoAttribute(vo.getId(), attrToFetch);
	}

	@Override
	public PerunAttribute getVoAttribute(Long voId, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttribute.NULL;
		}
		
		log.trace("getVoAttribute({}, {})", voId, attrToFetch);

		PerunAttribute attribute = getAttribute(PerunEntityType.VO, voId, attrToFetch);

		log.trace("getVoAttribute({}, {}) returns: {}", voId, attrToFetch, attribute);
		return attribute;
	}

	@Override
	public Map<String, PerunAttribute> getResourceAttributes(Resource resource, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}
		
		log.trace("getResourceAttributes({}, {})", resource, attrsToFetch);
		return this.getResourceAttributes(resource.getId(), attrsToFetch);
	}

	@Override
	public Map<String, PerunAttribute> getResourceAttributes(Long resourceId, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}
		
		log.trace("getResourceAttributes({}, {})", resourceId, attrsToFetch);

		Map<String, PerunAttribute> attributes = getAttributes(PerunEntityType.RESOURCE, resourceId, attrsToFetch);

		log.trace("getResourceAttributes({}, {}) returns: {}", resourceId, attrsToFetch, attributes);
		return attributes;
	}

	@Override
	public PerunAttribute getResourceAttribute(Resource resource, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttribute.NULL;
		}
		
		log.trace("getResourceAttribute({}, {})", resource, attrToFetch);
		return this.getResourceAttribute(resource.getId(), attrToFetch);
	}

	@Override
	public PerunAttribute getResourceAttribute(Long resourceId, String attrToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttribute.NULL;
		}
		
		log.trace("getResourceAttribute({}, {})", resourceId, attrToFetch);

		PerunAttribute attribute = getAttribute(PerunEntityType.RESOURCE, resourceId, attrToFetch);

		log.trace("getResourceAttribute({}, {}) returns: {}", resourceId, attrToFetch, attribute);
		return attribute;
	}

	@Override
	public Set<String> getResourceCapabilities(Facility facility, Set<String> groupNames, String capabilitiesAttrName) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashSet<>();
		}

		log.trace("getResourceCapabilities({}, {}, {})", facility, groupNames, capabilitiesAttrName);

		if (facility == null) {
			return new LinkedHashSet<>();
		}

		List<Resource> resources = getAssignedResources(facility);
		Set<String> capabilities = new LinkedHashSet<>();

		for (Resource resource : resources) {
			PerunAttributeValue attrValue = getResourceAttributeValue(resource.getId(), capabilitiesAttrName);

			List<String> resourceCapabilities = attrValue.valueAsList();
			if (resourceCapabilities == null || resourceCapabilities.size() == 0) {
				continue;
			}
			List<Group> groups = getAssignedGroups(resource.getId());
			for (Group group : groups) {
				String groupName = group.getName();
				if ("members".equals(groupName)) {
					log.trace("Group is members, continue with special handling");
					groupName = "";
					if (resource.getVo() != null) {
						groupName = resource.getVo().getShortName();
					}
				} else if (resource.getVo() != null) {
					groupName = resource.getVo().getShortName() + ':' + groupName;
				}
				group.setUniqueGroupName(groupName);
				log.trace("Constructed unique groupName: {}", groupName);

				if (groupNames.contains(groupName)) {
					log.trace("Group found in user's group, add capabilities");
					capabilities.addAll(resourceCapabilities);
					break;
				}
				log.trace("Group not found, continue to the next one");
			}
		}

		log.trace("getResourceCapabilities({}, {}, {}) returns {})", facility, groupNames, capabilitiesAttrName, capabilities);
		return capabilities;
	}

	@Override
	public Set<String> getFacilityCapabilities(Facility facility, String capabilitiesAttrName) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashSet<>();
		}
		log.trace("getFacilityCapabilities({}, {})", facility, capabilitiesAttrName);

		Set<String> capabilities = new HashSet<>();
		if (facility != null) {
			PerunAttributeValue attr = getFacilityAttributeValue(facility, capabilitiesAttrName);
			if (attr != null && attr.valueAsList() != null) {
				capabilities = new HashSet<>(attr.valueAsList());
			}
		}

		log.trace("getFacilityCapabilities({}, {}) returns: {}", facility, capabilitiesAttrName, capabilities);
		return capabilities;
	}

	@Override
	public Set<Group> getGroupsWhereUserIsActiveWithUniqueNames(Long facilityId, Long userId) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashSet<>();
		}
		
		log.trace("getGroupsWhereUserIsActiveWithUniqueNames({}, {})", facilityId, userId);
		Set<Group> groups = this.getGroupsWhereUserIsActive(facilityId, userId);

		Map<Long, String> voIdToShortNameMap = new HashMap<>();
		groups.forEach(g -> {
			if (!voIdToShortNameMap.containsKey(g.getVoId())) {
				Vo vo = this.getVoById(g.getVoId());
				if (vo != null) {
					voIdToShortNameMap.put(vo.getId(), vo.getShortName());
				}
			}
			g.setUniqueGroupName(voIdToShortNameMap.get(g.getVoId()) + ':' + g.getName());
		});

		log.trace("getGroupsWhereUserIsActiveWithUniqueNames({}, {}) returns: {}", facilityId, userId, groups);
		return groups;
	}

	@Override
	public Set<Long> getUserGroupsIds(Long userId, Long voId) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashSet<>();
		}

		log.trace("getUserGroups({}, {})", userId, voId);
		Member member = getMemberByUser(userId, voId);
		Set<Long> groups = new HashSet<>();
		if (member != null) {
			groups = getMemberGroups(member.getId()).stream().map(Group::getId).collect(Collectors.toSet());
		}

		log.trace("getUserGroups({}, {}) returns: {}", userId, voId, groups);
		return groups;
	}

	private Member getMemberByUser(Long userId, Long voId) {
		if (!this.connectorRpc.isEnabled()) {
			return null;
		}

		log.trace("getMemberByUser({}, {})", userId, voId);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("user", userId);
		params.put("vo", voId);
		JsonNode jsonNode = connectorRpc.post(MEMBERS_MANAGER, "getMemberByUser", params);

		Member member = RpcMapper.mapMember(jsonNode);
		log.trace("getMemberByUser({}, {}) returns: {}", userId, voId, member);
		return member;
	}

	private Set<Group> getGroupsWhereUserIsActive(Long facilityId, Long userId) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashSet<>();
		}
		
		log.trace("getGroupsWhereUserIsActive({}, {})", facilityId, userId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facilityId);
		map.put("user", userId);
		JsonNode res = connectorRpc.post(USERS_MANAGER, "getGroupsWhereUserIsActive", map);

		Set<Group> groups = new HashSet<>(RpcMapper.mapGroups(res));
		log.trace("getGroupsWhereUserIsActive({}, {}) returns: {}", facilityId, userId, groups);
		return groups;
	}

	private Vo getVoById(Long voId) {
		if (!this.connectorRpc.isEnabled()) {
			return null;
		}
		
		log.trace("getVoById({})",voId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", voId);

		JsonNode res = connectorRpc.post(VOS_MANAGER, "getVoById", map);
		Vo vo = RpcMapper.mapVo(res);
		log.trace("getVoById({}) returns {}", voId, vo);
		return vo;
	}

	private List<Group> getAssignedGroups(Long resourceId) {
		if (!this.connectorRpc.isEnabled()) {
			return new ArrayList<>();
		}
		
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("resource", resourceId);

		JsonNode response = connectorRpc.post(RESOURCES_MANAGER, "getAssignedGroups", params);

		return RpcMapper.mapGroups(response);
	}

	private Map<String, PerunAttributeValue> extractValues(Map<String, PerunAttribute> attributeMap) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}
		
		Map<String, PerunAttributeValue> resultMap = new LinkedHashMap<>();
		for (Map.Entry<String, PerunAttribute> attrPair: attributeMap.entrySet()) {
			String attrName = attrPair.getKey();
			PerunAttribute attr = attrPair.getValue();
			resultMap.put(attrName, attr.getValue());
		}

		return resultMap;
	}

	private Map<String, PerunAttribute> getAttributes(PerunEntityType entity, Long entityId, Collection<String> attrsToFetch) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		} else if (attrsToFetch == null || attrsToFetch.isEmpty()) {
			return new HashMap<>();
		}

		Set<AttributeMapping> mappings;
		switch (entity) {
			case USER: mappings = this.getUserAttributesMappingService()
					.getMappingsForAttrNames(attrsToFetch);
				break;
			case FACILITY: mappings = this.getFacilityAttributesMappingService()
					.getMappingsForAttrNames(attrsToFetch);
				break;
			case VO: mappings = this.getVoAttributesMappingService()
					.getMappingsForAttrNames(attrsToFetch);
				break;
			case GROUP: mappings = this.getGroupAttributesMappingService()
					.getMappingsForAttrNames(attrsToFetch);
				break;
			case RESOURCE: mappings = this.getResourceAttributesMappingService()
					.getMappingsForAttrNames(attrsToFetch);
				break;
			default: mappings  = new HashSet<>();
				break;
		}

		List<String> rpcNames = mappings.stream().map(AttributeMapping::getRpcName).collect(Collectors.toList());

		Map<String, Object> map = new LinkedHashMap<>();
		map.put(entity.toString().toLowerCase(), entityId);
		map.put("attrNames", rpcNames);

		JsonNode res = connectorRpc.post(ATTRIBUTES_MANAGER, "getAttributes", map);
		return RpcMapper.mapAttributes(res, mappings);
	}

	private List<Group> getMemberGroups(Long memberId) {
		if (!this.connectorRpc.isEnabled()) {
			return new ArrayList<>();
		}
		
		log.trace("getMemberGroups({})", memberId);

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("member", memberId);

		JsonNode response = connectorRpc.post(GROUPS_MANAGER, "getMemberGroups", map);
		List<Group> groups = RpcMapper.mapGroups(response);

		log.trace("getMemberGroups({}) returns: {}", memberId, groups);
		return groups;
	}

	private List<Member> getMembersByUser(Long userId) {
		if (!this.connectorRpc.isEnabled()) {
			return new ArrayList<>();
		}
		
		log.trace("getMemberByUser({})", userId);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("user", userId);
		JsonNode jsonNode = connectorRpc.post(MEMBERS_MANAGER, "getMembersByUser", params);

		List<Member> userMembers = RpcMapper.mapMembers(jsonNode);
		log.trace("getMembersByUser({}) returns: {}", userId, userMembers);
		return userMembers;
	}

	private List<Vo> getVosByShortNames(List<String> voShortNames) {
		if (!this.connectorRpc.isEnabled()) {
			return new ArrayList<>();
		}
		
		log.trace("getVosByShortNames({})", voShortNames);
		List<Vo> vos = new ArrayList<>();
		for (String shortName : voShortNames) {
			Vo vo = getVoByShortName(shortName);
			vos.add(vo);
		}

		log.trace("getVosByShortNames({}) returns: {}", voShortNames, vos);
		return vos;
	}

	private Map<Long, MemberStatus> convertMembersListToStatusesMap(List<Member> userMembers) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}
		
		Map<Long, MemberStatus> res = new HashMap<>();
		for (Member m : userMembers) {
			res.put(m.getVoId(), m.getStatus());
		}

		return res;
	}

	private Map<String, PerunAttributeValue> getUserExtSourceAttributeValues(Long uesId, Set<AttributeMapping> attrMappings) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}
		
		log.trace("getUserExtSourceAttributeValues({}, {})", uesId, attrMappings);

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("userExtSource", uesId);
		map.put("attrNames", attrMappings.stream().map(AttributeMapping::getRpcName).collect(Collectors.toList()));

		JsonNode response = connectorRpc.post(ATTRIBUTES_MANAGER, "getAttributes", map);
		Map<String, PerunAttribute> attributeMap = RpcMapper.mapAttributes(response, attrMappings);
		Map<String, PerunAttributeValue> valueMap = extractValues(attributeMap);

		log.trace("getUserExtSourceAttributeValues({}, {}) returns: {}", uesId, attrMappings, valueMap);
		return valueMap;
	}

	private List<Group> getAllowedGroups(Facility facility) {
		if (!this.connectorRpc.isEnabled()) {
			return new ArrayList<>();
		}
		
		log.trace("getAllowedGroups({})", facility);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facility.getId());
		JsonNode jsonNode = connectorRpc.post(FACILITIES_MANAGER, "getAllowedGroups", map);
		List<Group> result = new ArrayList<>();
		for (int i = 0; i < jsonNode.size(); i++) {
			JsonNode groupNode = jsonNode.get(i);
			result.add(RpcMapper.mapGroup(groupNode));
		}

		log.trace("getAllowedGroups({}) returns: {}", facility, result);
		return result;
	}

	private boolean getApplicationForm(Group group) {
		if (!this.connectorRpc.isEnabled()) {
			return false;
		}
		
		log.trace("getApplicationForm({})", group);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("group", group.getId());
		try {
			if (group.getName().equalsIgnoreCase("members")) {
				log.trace("getApplicationForm({}) continues to call regForm for VO {}", group, group.getVoId());
				return getApplicationForm(group.getVoId());
			} else {
				connectorRpc.post(REGISTRAR_MANAGER, "getApplicationForm", map);
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
		if (!this.connectorRpc.isEnabled()) {
			return false;
		}
		
		log.trace("getApplicationForm({})", voId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("vo", voId);
		try {
			connectorRpc.post(REGISTRAR_MANAGER, "getApplicationForm", map);
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
		if (!this.connectorRpc.isEnabled()) {
			return new ArrayList<>();
		}
		
		log.trace("getGroupsWhereUserIsActive({}, {})", facility, userId);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facility.getId());
		map.put("user", userId);
		JsonNode jsonNode = connectorRpc.post(USERS_MANAGER, "getGroupsWhereUserIsActive", map);

		List<Group> res = RpcMapper.mapGroups(jsonNode);
		log.trace("getGroupsWhereUserIsActive({}, {}) returns: {}", facility, userId, res);
		return res;
	}

	private Map<Long, Vo> convertVoListToMap(List<Vo> vos) {
		if (!this.connectorRpc.isEnabled()) {
			return new HashMap<>();
		}
		
		Map<Long, Vo> map = new HashMap<>();
		for (Vo vo : vos) {
			map.put(vo.getId(), vo);
		}

		return map;
	}

	private List<Resource> getAssignedResources(Facility facility) {
		if (!this.connectorRpc.isEnabled()) {
			return new ArrayList<>();
		}
		
		log.trace("getAssignedResources({})", facility);

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("facility", facility.getId());

		JsonNode res = connectorRpc.post(FACILITIES_MANAGER, "getAssignedResources", map);
		List<Resource> resources = RpcMapper.mapResources(res);

		log.trace("getAssignedResources({}) returns: {}", facility, resources);
		return resources;
	}

	private List<Group> getRichGroupsAssignedToResourceWithAttributesByNames(Resource resource, List<String> attrNames) {
		if (!this.connectorRpc.isEnabled()) {
			return new ArrayList<>();
		}
		
		log.trace("getRichGroupsAssignedToResourceWithAttributesByNames({}, {})", resource, attrNames);
		Map<String, Object> map = new LinkedHashMap<>();
		Set<AttributeMapping> mappings = this.getGroupAttributesMappingService()
				.getMappingsForAttrNames(attrNames);
		List<String> rpcNames = mappings.stream().map(AttributeMapping::getRpcName).collect(Collectors.toList());
		map.put("resource", resource.getId());
		map.put("attrNames", rpcNames);

		JsonNode res = connectorRpc.post(GROUPS_MANAGER, "getRichGroupsAssignedToResourceWithAttributesByNames", map);
		List<Group> groups = new ArrayList<>();

		for (int i = 0; i < res.size(); i++) {
			JsonNode jsonNode = res.get(i);
			Group group = RpcMapper.mapGroup(jsonNode);

			JsonNode groupAttrs = jsonNode.get("attributes");
			Map<String, JsonNode> attrsMap = new HashMap<>();

			for (int j = 0; j < groupAttrs.size(); j++) {
				JsonNode attr = groupAttrs.get(j);

				String namespace = attr.get("namespace").textValue();
				String friendlyName = attr.get("friendlyName").textValue();

				attrsMap.put(namespace + ":" + friendlyName, attr);
			}

			group.setAttributes(attrsMap);
			groups.add(group);
		}

		log.trace("getRichGroupsAssignedToResourceWithAttributesByNames({}) returns: {}", resource, groups);
		return groups;
	}

	private PerunAttribute getAttribute(PerunEntityType entity, Long entityId, String attributeName) {
		if (!this.connectorRpc.isEnabled()) {
			return PerunAttribute.NULL;
		}
		
		log.trace("getAttribute({}, {}, {})", entity, entityId, attributeName);
		AttributeMapping mapping;
		switch (entity) {
			case USER: mapping = this.getUserAttributesMappingService()
					.getByName(attributeName);
				break;
			case FACILITY: mapping = this.getFacilityAttributesMappingService()
					.getByName(attributeName);
				break;
			case VO: mapping = this.getVoAttributesMappingService()
					.getByName(attributeName);
				break;
			case GROUP: mapping = this.getGroupAttributesMappingService()
					.getByName(attributeName);
				break;
			case RESOURCE: mapping = this.getResourceAttributesMappingService()
					.getByName(attributeName);
				break;
			default:
				throw new IllegalArgumentException("Unrecognized entity");
		}

		Map<String, Object> map = new LinkedHashMap<>();
		map.put(entity.toString().toLowerCase(), entityId);
		map.put("attributeName", mapping.getRpcName());

		JsonNode res = connectorRpc.post(ATTRIBUTES_MANAGER, "getAttribute", map);
		PerunAttribute attribute = RpcMapper.mapAttribute(res);

		log.trace("getAttribute({}, {}, {}) returns: {}", entity, entityId, attributeName, attribute);
		return attribute;
	}

	private List<UserExtSource> getUserExtSources(Long userId) {
		if (!this.connectorRpc.isEnabled()) {
			return new ArrayList<>();
		}
		
		log.trace("getUserExtSources({})", userId);

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("user", userId);

		JsonNode response = connectorRpc.post(USERS_MANAGER, "getUserExtSources", map);
		List<UserExtSource> userExtSources = RpcMapper.mapUserExtSources(response);

		log.trace("getUserExtSources({}) returns: {}", userExtSources, userExtSources);
		return userExtSources;
	}
}
