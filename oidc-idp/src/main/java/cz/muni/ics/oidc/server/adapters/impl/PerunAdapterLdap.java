package cz.muni.ics.oidc.server.adapters.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import cz.muni.ics.oidc.exceptions.InconvertibleValueException;
import cz.muni.ics.oidc.models.AttributeMapping;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.Group;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.models.Resource;
import cz.muni.ics.oidc.models.Vo;
import cz.muni.ics.oidc.models.enums.PerunAttrValueType;
import cz.muni.ics.oidc.models.enums.PerunEntityType;
import cz.muni.ics.oidc.server.PerunPrincipal;
import cz.muni.ics.oidc.server.adapters.PerunAdapterMethods;
import cz.muni.ics.oidc.server.adapters.PerunAdapterMethodsLdap;
import cz.muni.ics.oidc.server.connectors.Affiliation;
import cz.muni.ics.oidc.server.connectors.PerunConnectorLdap;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.apache.directory.ldap.client.template.EntryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.ASSIGNED_GROUP_ID;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.CN;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.DESCRIPTION;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.EDU_PERSON_PRINCIPAL_NAMES;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.GIVEN_NAME;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.MEMBER_OF;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.O;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.OBJECT_CLASS;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.PERUN_FACILITY;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.PERUN_FACILITY_ID;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.PERUN_GROUP;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.PERUN_GROUP_ID;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.PERUN_PARENT_GROUP_ID;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.PERUN_RESOURCE;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.PERUN_RESOURCE_ID;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.PERUN_UNIQUE_GROUP_NAME;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.PERUN_USER;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.PERUN_USER_ID;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.PERUN_VO;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.PERUN_VO_ID;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.SN;
import static cz.muni.ics.oidc.server.adapters.impl.PerunAdapterLdapConstants.UNIQUE_MEMBER;
import static org.apache.directory.ldap.client.api.search.FilterBuilder.and;
import static org.apache.directory.ldap.client.api.search.FilterBuilder.equal;
import static org.apache.directory.ldap.client.api.search.FilterBuilder.or;

/**
 * Connects to Perun using LDAP.
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunAdapterLdap extends PerunAdapterWithMappingServices implements PerunAdapterMethods, PerunAdapterMethodsLdap {

	private final static Logger log = LoggerFactory.getLogger(PerunAdapterLdap.class);

	private PerunConnectorLdap connectorLdap;
	private String oidcClientIdAttr;
	private String oidcCheckMembershipAttr;
	private JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;

	public void setConnectorLdap(PerunConnectorLdap connectorLdap) {
		this.connectorLdap = connectorLdap;
	}

	public void setOidcClientIdAttr(String oidcClientIdAttr) {
		this.oidcClientIdAttr = oidcClientIdAttr;
	}

	public void setOidcCheckMembershipAttr(String oidcCheckMembershipAttr) {
		this.oidcCheckMembershipAttr = oidcCheckMembershipAttr;
	}

	/**
	 * Fetch user based on his principal (extLogin and extSource) from Perun
	 *
	 * @param perunPrincipal principal of user
	 * @return PerunUser with id of found user
	 */
	@Override
	public PerunUser getPreauthenticatedUserId(PerunPrincipal perunPrincipal) {
		log.trace("getPreauthenticatedUserId({})", perunPrincipal);

		String dnPrefix = "ou=People";
		FilterBuilder filter = and(equal(OBJECT_CLASS, PERUN_USER), equal(EDU_PERSON_PRINCIPAL_NAMES, perunPrincipal.getExtLogin()));
		SearchScope scope = SearchScope.ONELEVEL;
		String[] attributes = new String[]{PERUN_USER_ID, GIVEN_NAME, SN};
		EntryMapper<PerunUser> mapper = e -> {
			if (!checkHasAttributes(e, new String[] { PERUN_USER_ID, SN })) {
				return null;
			}

			long id = Long.parseLong(e.get(PERUN_USER_ID).getString());
			String firstName = (e.get(GIVEN_NAME) != null) ? e.get(GIVEN_NAME).getString() : null;
			String lastName = e.get(SN).getString();
			return new PerunUser(id, firstName, lastName);
		};

		PerunUser foundUser = connectorLdap.searchFirst(dnPrefix, filter, scope, attributes, mapper);

		log.trace("getPreauthenticatedUserId({} returns: {})", perunPrincipal, foundUser);
		return foundUser;
	}

	@Override
	public Facility getFacilityByClientId(String clientId) {
		log.trace("getFacilityByClientId({})", clientId);

		SearchScope scope = SearchScope.ONELEVEL;
		String[] attributes = new String[]{PERUN_FACILITY_ID, DESCRIPTION, CN};
		EntryMapper<Facility> mapper = e -> {
			if (!checkHasAttributes(e, attributes)) {
				return null;
			}

			long id = Long.parseLong(e.get(PERUN_FACILITY_ID).getString());
			String name = e.get(CN).getString();
			String description = e.get(DESCRIPTION).getString();

			return new Facility(id, name, description);
		};

		AttributeMapping mapping = this.getFacilityAttributesMappingService().getByName(oidcClientIdAttr);

		FilterBuilder filter = and(equal(OBJECT_CLASS, PERUN_FACILITY), equal(mapping.getLdapName(), clientId));
		Facility facility = connectorLdap.searchFirst(null, filter, scope, attributes, mapper);

		log.trace("getFacilitiesByClientId({}) returns {}", clientId, facility);
		return facility;
	}

	@Override
	public boolean isMembershipCheckEnabledOnFacility(Facility facility) {
		log.trace("isMembershipCheckEnabledOnFacility({})", facility);
		boolean res = false;

		PerunAttributeValue attrVal = getFacilityAttributeValue(facility, oidcCheckMembershipAttr);
		if (attrVal != null && !PerunAttributeValue.NULL.equals(attrVal)) {
			res = attrVal.valueAsBoolean();
		}

		log.trace("isMembershipCheckEnabledOnFacility({}) returns {}", facility, res);
		return res;
	}

	@Override
	public boolean canUserAccessBasedOnMembership(Facility facility, Long userId) {
		log.trace("canUserAccessBasedOnMembership({}, {})", facility, userId);

		Set<Long> groupsWithAccessIds = getGroupIdsWithAccessToFacility(facility.getId());
		if (groupsWithAccessIds == null || groupsWithAccessIds.isEmpty()) {
			log.trace("canUserAccessBasedOnMembership({}, {}) returns: {}", facility, userId, false);
			return false;
		}

		Set<Long> userGroupIds = getGroupIdsWhereUserIsMember(userId);
		if (userGroupIds == null || userGroupIds.isEmpty()) {
			log.trace("canUserAccessBasedOnMembership({}, {}) returns: {}", facility, userId, false);
			return false;
		}

		boolean canAccess = !Collections.disjoint(userGroupIds, groupsWithAccessIds);
		log.trace("canUserAccessBasedOnMembership({}, {}) returns: {}", facility, userId, canAccess);
		return canAccess;
	}

	@Override
	public boolean isUserInGroup(Long userId, Long groupId) {
		log.trace("isUserInGroup({}, {})", userId, groupId);
		String uniqueMemberValue = PERUN_USER_ID + '=' + userId + ",ou=People," + connectorLdap.getBaseDN();
		FilterBuilder filter = and(
				equal(OBJECT_CLASS, PERUN_GROUP),
				equal(PERUN_GROUP_ID, String.valueOf(groupId)),
				equal(UNIQUE_MEMBER, uniqueMemberValue)
		);

		EntryMapper<Boolean> mapper = e -> e.size() == 1;

		String[] attributes = new String[] { OBJECT_CLASS };

		boolean result = (null == connectorLdap.searchFirst(null, filter, SearchScope.SUBTREE, attributes, mapper));

		log.trace("isUserInGroup({}, {}) returns: {}", userId, groupId, result);
		return result;
	}

	@Override
	public List<Affiliation> getGroupAffiliations(Long userId, String groupAffiliationsAttr) {
		log.trace("getGroupAffiliations({}, {})", userId, groupAffiliationsAttr);
		Set<Long> userGroupIds = getGroupIdsWhereUserIsMember(userId);
		if (userGroupIds == null || userGroupIds.isEmpty()) {
			return new ArrayList<>();
		}

		FilterBuilder[] groupIdFilters = new FilterBuilder[userGroupIds.size()];
		int i = 0;
		for (Long id: userGroupIds) {
			groupIdFilters[i++] = equal(PERUN_GROUP_ID, String.valueOf(id));
		}

		AttributeMapping affiliationsMapping = getGroupAttributesMappingService().getByName(groupAffiliationsAttr);

		FilterBuilder filterBuilder = and(equal(OBJECT_CLASS, PERUN_GROUP), or(groupIdFilters));
		String[] attributes = new String[] { affiliationsMapping.getLdapName() };
		EntryMapper<Set<Affiliation>> mapper = e -> {
			Set<Affiliation> affiliations = new HashSet<>();
			if (!checkHasAttributes(e, attributes)) {
				return affiliations;
			}

			Attribute a = e.get(affiliationsMapping.getLdapName());
			long linuxTime = System.currentTimeMillis() / 1000L;
			a.iterator().forEachRemaining(v -> affiliations.add(new Affiliation(null, v.getString(), linuxTime)));

			return affiliations;
		};

		List<Set<Affiliation>> affiliationSets = connectorLdap.search(null, filterBuilder, SearchScope.SUBTREE, attributes, mapper);
		List<Affiliation> affiliations = affiliationSets.stream().flatMap(Set::stream).distinct().collect(Collectors.toList());

		log.trace("getGroupAffiliations({}, {}) returns: {}", userId, groupAffiliationsAttr, affiliations);
		return affiliations;
	}

	@Override
	public List<String> getGroupsAssignedToResourcesWithUniqueNames(Facility facility) {
		log.trace("getGroupsAssignedToResourcesWithUniqueNames({})", facility);
		List<String> res = new ArrayList<>();

		Set<Long> groupIds = getGroupIdsWithAccessToFacility(facility.getId());
		if (groupIds == null || groupIds.isEmpty()) {
			log.trace("getGroupsAssignedToResourcesWithUniqueNames({}) returns: {}", facility, res);
			return res;
		}

		FilterBuilder[] partialFilters = new FilterBuilder[groupIds.size()];
		int i = 0;
		for (Long id: groupIds) {
			partialFilters[i++] = equal(PERUN_GROUP_ID, String.valueOf(id));
		}

		FilterBuilder filter = and(equal(OBJECT_CLASS, PERUN_GROUP), or(partialFilters));
		String[] attributes = new String[] {PERUN_UNIQUE_GROUP_NAME};
		EntryMapper<String> mapper = e -> {
			if (!checkHasAttributes(e, attributes)) {
				return null;
			}

			return e.get(PERUN_UNIQUE_GROUP_NAME).getString();
		};

		List<String> uniqueGroupNames = connectorLdap.search(null, filter, SearchScope.SUBTREE, attributes, mapper);
		uniqueGroupNames = uniqueGroupNames.stream().filter(Objects::nonNull).collect(Collectors.toList());
		log.trace("getGroupsAssignedToResourcesWithUniqueNames({}) returns: {}", facility, uniqueGroupNames);
		return uniqueGroupNames;
	}

	@Override
	public Vo getVoByShortName(String shortName) {
		log.trace("getVoByShortName({})", shortName);

		FilterBuilder filter = and(equal(OBJECT_CLASS, PERUN_VO), equal(O, shortName));
		String[] attributes = new String[] { PERUN_VO_ID, O, DESCRIPTION };
		EntryMapper<Vo> mapper = e -> {
			if (!checkHasAttributes(e, attributes)) {
				return null;
			}

			Long id = Long.valueOf(e.get(PERUN_VO_ID).getString());
			String shortNameVo = e.get(O).getString();
			String name = e.get(DESCRIPTION).getString();

			return new Vo(id, name, shortNameVo);
		};

		Vo vo = connectorLdap.searchFirst(null, filter, SearchScope.ONELEVEL, attributes, mapper);
		log.trace("getVoByShortName({}) returns: {}", shortName, vo);
		return vo;
	}

	@Override
	public Map<String, PerunAttributeValue> getUserAttributeValues(PerunUser user, Collection<String> attrsToFetch) {
		log.trace("getUserAttributeValues({}, {})", user, attrsToFetch);
		return this.getUserAttributeValues(user.getId(), attrsToFetch);
	}

	@Override
	public Map<String, PerunAttributeValue> getUserAttributeValues(Long userId, Collection<String> attrsToFetch) {
		log.trace("getUserAttributeValues({}, {})", userId, attrsToFetch);

		String dnPrefix = PERUN_USER_ID + '=' + userId + ",ou=People";
		Map<String, PerunAttributeValue> attributeValueMap = getAttributeValues(dnPrefix, attrsToFetch, PerunEntityType.USER);
		log.trace("getUserAttributeValues({}, {}) returns: {}", userId, attrsToFetch, attributeValueMap);
		return attributeValueMap;
	}

	@Override
	public PerunAttributeValue getUserAttributeValue(PerunUser user, String attrToFetch) {
		log.trace("getUserAttributeValue({}, {})", user, attrToFetch);
		return this.getUserAttributeValue(user.getId(), attrToFetch);
	}

	@Override
	public PerunAttributeValue getUserAttributeValue(Long userId, String attrToFetch) {
		log.trace("getUserAttributeValue({}, {})", userId, attrToFetch);
		Map<String, PerunAttributeValue> map = this.getUserAttributeValues(
				userId, Collections.singletonList(attrToFetch));
		return map.getOrDefault(attrToFetch, PerunAttributeValue.NULL);
	}

	@Override
	public Map<String, PerunAttributeValue> getFacilityAttributeValues(Facility facility, Collection<String> attrsToFetch) {
		log.trace("getFacilityAttributeValues({}, {})", facility, attrsToFetch);
		return this.getFacilityAttributeValues(facility.getId(), attrsToFetch);
	}

	@Override
	public Map<String, PerunAttributeValue> getFacilityAttributeValues(Long facilityId, Collection<String> attrsToFetch) {
		log.trace("getFacilityAttributeValues({}, {})", facilityId, attrsToFetch);
		String dnPrefix = PERUN_FACILITY_ID + '=' + facilityId;
		Map<String, PerunAttributeValue> attributeValueMap = getAttributeValues(dnPrefix, attrsToFetch, PerunEntityType.FACILITY);
		log.trace("getFacilityAttributeValues({}, {}) returns: {}", facilityId, attrsToFetch, attributeValueMap);
		return attributeValueMap;
	}

	@Override
	public PerunAttributeValue getFacilityAttributeValue(Facility facility, String attrToFetch) {
		log.trace("getFacilityAttributeValue({}, {})", facility, attrToFetch);
		return this.getFacilityAttributeValue(facility.getId(), attrToFetch);
	}

	@Override
	public PerunAttributeValue getFacilityAttributeValue(Long facilityId, String attrToFetch) {
		log.trace("getFacilityAttributeValue({}, {})", facilityId, attrToFetch);
		Map<String, PerunAttributeValue> map = this.getFacilityAttributeValues(
				facilityId, Collections.singletonList(attrToFetch));
		return map.getOrDefault(attrToFetch, PerunAttributeValue.NULL);
	}

	@Override
	public Map<String, PerunAttributeValue> getVoAttributeValues(Vo vo, Collection<String> attrsToFetch) {
		log.trace("getVoAttributeValues({}, {})", vo, attrsToFetch);
		return this.getVoAttributeValues(vo.getId(), attrsToFetch);
	}

	@Override
	public Map<String, PerunAttributeValue> getVoAttributeValues(Long voId, Collection<String> attrsToFetch) {
		log.trace("getVoAttributeValues({}, {})", voId, attrsToFetch);

		String dnPrefix = PERUN_VO_ID + '=' + voId;
		Map<String, PerunAttributeValue> attributeValueMap = getAttributeValues(dnPrefix, attrsToFetch, PerunEntityType.VO);
		log.trace("getVoAttributeValues({}, {}) returns: {}", voId, attrsToFetch, attributeValueMap);
		return attributeValueMap;
	}

	@Override
	public PerunAttributeValue getVoAttributeValue(Vo vo, String attrToFetch) {
		log.trace("getVoAttributeValue({}, {})", vo, attrToFetch);
		return this.getVoAttributeValue(vo.getId(), attrToFetch);
	}

	@Override
	public PerunAttributeValue getVoAttributeValue(Long voId, String attrToFetch) {
		log.trace("getFacilityAttributeValue({}, {})", voId, attrToFetch);
		Map<String, PerunAttributeValue> map = this.getVoAttributeValues(
				voId, Collections.singletonList(attrToFetch));
		return map.getOrDefault(attrToFetch, PerunAttributeValue.NULL);
	}

	@Override
	public Map<String, PerunAttributeValue> getGroupAttributeValues(Group group, Collection<String> attrsToFetch) {
		log.trace("getGroupAttributeValues({}, {})", group, attrsToFetch);
		return this.getGroupAttributeValues(group.getVoId(), attrsToFetch);
	}

	@Override
	public Map<String, PerunAttributeValue> getGroupAttributeValues(Long groupId, Collection<String> attrsToFetch) {
		log.trace("getGroupAttributeValues({}, {})", groupId, attrsToFetch);

		String dnPrefix = PERUN_GROUP_ID + '=' + groupId;
		Map<String, PerunAttributeValue> attributeValueMap = getAttributeValues(dnPrefix, attrsToFetch, PerunEntityType.GROUP);
		log.trace("getResourceAttributeValues({}, {}) returns: {}", groupId, attrsToFetch, attributeValueMap);
		return attributeValueMap;
	}

	@Override
	public PerunAttributeValue getGroupAttributeValue(Group group, String attrToFetch) {
		log.trace("getGroupAttributeValue({}, {})", group, attrToFetch);
		return this.getGroupAttributeValue(group.getId(), attrToFetch);
	}

	@Override
	public PerunAttributeValue getGroupAttributeValue(Long groupId, String attrToFetch) {
		log.trace("getGroupAttributeValue({}, {})", groupId, attrToFetch);
		Map<String, PerunAttributeValue> map = this.getGroupAttributeValues(
				groupId, Collections.singletonList(attrToFetch));
		return map.getOrDefault(attrToFetch, PerunAttributeValue.NULL);
	}

	@Override
	public Map<String, PerunAttributeValue> getResourceAttributeValues(Resource resource, Collection<String> attrsToFetch) {
		log.trace("getResourceAttributeValues({}, {})", resource, attrsToFetch);
		return this.getResourceAttributeValues(resource.getVoId(), attrsToFetch);
	}

	@Override
	public Map<String, PerunAttributeValue> getResourceAttributeValues(Long resourceId, Collection<String> attrsToFetch) {
		log.trace("getResourceAttributeValues({}, {})", resourceId, attrsToFetch);

		String dnPrefix = PERUN_RESOURCE_ID + '=' + resourceId;
		Map<String, PerunAttributeValue> attributeValueMap = getAttributeValues(dnPrefix, attrsToFetch, PerunEntityType.RESOURCE);
		log.trace("getResourceAttributeValues({}, {}) returns: {}", resourceId, attrsToFetch, attributeValueMap);
		return attributeValueMap;
	}

	@Override
	public PerunAttributeValue getResourceAttributeValue(Resource resource, String attrToFetch) {
		log.trace("getResourceAttributeValue({}, {})", resource, attrToFetch);
		return this.getResourceAttributeValue(resource.getId(), attrToFetch);
	}

	@Override
	public PerunAttributeValue getResourceAttributeValue(Long resourceId, String attrToFetch) {
		log.trace("getResourceAttributeValue({}, {})", resourceId, attrToFetch);
		Map<String, PerunAttributeValue> map = this.getResourceAttributeValues(
				resourceId, Collections.singletonList(attrToFetch));
		return map.getOrDefault(attrToFetch, PerunAttributeValue.NULL);
	}

	@Override
	public Set<String> getResourceCapabilities(Facility facility, Set<String> groupNames, String capabilitiesAttrName) {
		log.trace("getResourceCapabilities({}, {}, {})", facility, groupNames, capabilitiesAttrName);
		Set<String> result = new HashSet<>();

		if (facility == null) {
			log.trace("getResourceCapabilities({}, {}, {}) returns: {}", facility, groupNames, capabilitiesAttrName, result);
			return result;
		}

		FilterBuilder filter = and(equal(OBJECT_CLASS, PERUN_RESOURCE),
				equal(PERUN_FACILITY_ID, String.valueOf(facility.getId())));

		AttributeMapping capabilitiesMapping = getResourceAttributesMappingService().getByName(capabilitiesAttrName);
		String[] attributes = new String[] {capabilitiesMapping.getLdapName(), ASSIGNED_GROUP_ID};

		EntryMapper<CapabilitiesAssignedGroupsPair> mapper = e -> {
			Set<String> capabilities = new HashSet<>();
			Set<Long> groupIds = new HashSet<>();

			if (!checkHasAttributes(e, attributes)) {
				return new CapabilitiesAssignedGroupsPair(capabilities, groupIds);
			}

			Attribute capabilitiesAttr = e.get(capabilitiesMapping.getLdapName());
			Attribute assignedGroupIds = e.get(ASSIGNED_GROUP_ID);

			if (capabilitiesAttr != null) {
				capabilitiesAttr.iterator().forEachRemaining(v -> capabilities.add(v.getString()));
			}

			if (assignedGroupIds != null) {
				assignedGroupIds.iterator().forEachRemaining(v -> groupIds.add(Long.parseLong(v.getString())));
			}

			return new CapabilitiesAssignedGroupsPair(capabilities, groupIds);
		};

		List<CapabilitiesAssignedGroupsPair> capabilitiesAssignedGroupsPairs = connectorLdap.search(null, filter,
				SearchScope.SUBTREE, attributes, mapper);
		log.debug("Found capabilities groups pairs: {}", capabilitiesAssignedGroupsPairs);

		capabilitiesAssignedGroupsPairs = capabilitiesAssignedGroupsPairs.stream()
				.filter(pair -> pair != null
						&& pair.assignedGroupIds != null
						&& !pair.assignedGroupIds.isEmpty()
						&& pair.capabilities != null
						&& !pair.capabilities.isEmpty())
				.collect(Collectors.toList());
		log.debug("Filtered capabilities groups pairs: {}", capabilitiesAssignedGroupsPairs);

		Set<Long> groupIdsFromGNames = getGroupsByUniqueGroupNames(groupNames).stream()
				.map(Group::getId).collect(Collectors.toSet());
		log.debug("Group IDs: {}", groupIdsFromGNames);

		for (CapabilitiesAssignedGroupsPair pair: capabilitiesAssignedGroupsPairs) {
			log.debug("Processing capability pair: {}", pair);
			Set<Long> ids = pair.assignedGroupIds;
			if (! Collections.disjoint(groupIdsFromGNames, ids)) {
				log.debug("Added all capabilities form pair: {}", pair);
				result.addAll(pair.capabilities);
			}
		}

		log.trace("getResourceCapabilities({}, {}, {}) returns: {}", facility, groupNames, capabilitiesAttrName, result);
		return result;
	}

	@Override
	public Set<String> getFacilityCapabilities(Facility facility, String capabilitiesAttrName) {
		log.trace("getFacilityCapabilities({}, {})", facility, capabilitiesAttrName);

		Set<String> result = new HashSet<>();
		PerunAttributeValue attrVal = getFacilityAttributeValue(facility, capabilitiesAttrName);
		if (PerunAttributeValue.NULL.equals(attrVal) && attrVal.valueAsList() != null) {
			result = new HashSet<>(attrVal.valueAsList());
		}

		log.trace("getFacilityCapabilities({}, {}) returns: {}", facility, capabilitiesAttrName, result);
		return result;
	}

	@Override
	public Set<Group> getGroupsWhereUserIsActiveWithUniqueNames(Long facilityId, Long userId) {
		log.trace("getGroupsWhereUserIsActiveWithUniqueNames({}, {})", facilityId, userId);
		Set<Long> userGroups = this.getGroupIdsWhereUserIsMember(userId);
		Set<Long> facilityGroups = this.getGroupIdsWithAccessToFacility(facilityId);
		Set<Long> groupIds = userGroups.stream()
				.filter(facilityGroups::contains)
				.collect(Collectors.toSet());
		log.debug("Intersection of userGroups and facilityGroups: {}", groupIds);
		Set<Group> groups = new HashSet<>();

		if (groupIds.isEmpty()) {
			log.trace("getGroupsWhereUserIsActiveWithUniqueNames({}, {}) returns: {}", facilityId, userId, groups);
			return groups;
		}

		List<Group> resGroups = getGroups(groupIds, PERUN_GROUP_ID);
		groups = new HashSet<>(resGroups);

		log.trace("getGroupsWhereUserIsActiveWithUniqueNames({}, {}) returns: {}", facilityId, userId, groups);
		return groups;
	}

	private List<Group> getGroups(Collection<?> objects, String objectAttribute) {
		log.trace("getGroups({})", objects);

		List<Group> result;
		if (objects == null || objects.size() <= 0) {
			result = new ArrayList<>();
		} else {
			FilterBuilder filter;
			if (objects.size() == 1) {
				Object first = objects.toArray()[0];
				filter = and(equal(OBJECT_CLASS, PERUN_GROUP), equal(objectAttribute, String.valueOf(first)));
			} else {
				FilterBuilder[] partialFilters = new FilterBuilder[objects.size()];
				int i = 0;
				for (Object obj: objects) {
					partialFilters[i++] = equal(objectAttribute, String.valueOf(obj));
				}
				filter = and(equal(OBJECT_CLASS, PERUN_GROUP), or(partialFilters));
			}

			String[] attributes = new String[]{PERUN_GROUP_ID, CN, DESCRIPTION, PERUN_UNIQUE_GROUP_NAME,
					PERUN_VO_ID, PERUN_PARENT_GROUP_ID};

			EntryMapper<Group> mapper = e -> {
				if (!checkHasAttributes(e, new String[]{
						PERUN_GROUP_ID, CN, DESCRIPTION, PERUN_UNIQUE_GROUP_NAME, PERUN_VO_ID }))
				{
					return null;
				}

				Long id = Long.valueOf(e.get(PERUN_GROUP_ID).getString());
				String name = e.get(CN).getString();
				String description = e.get(DESCRIPTION).getString();
				String uniqueName = e.get(PERUN_UNIQUE_GROUP_NAME).getString();
				Long voId = Long.valueOf(e.get(PERUN_VO_ID).getString());
				Long parentGroupId = null;
				if (e.get(PERUN_PARENT_GROUP_ID) != null) {
					parentGroupId = Long.valueOf(e.get(PERUN_PARENT_GROUP_ID).getString());
				}

				return new Group(id, parentGroupId, name, description, uniqueName, voId);
			};

			result = connectorLdap.search(null, filter, SearchScope.SUBTREE, attributes, mapper);
			result = result.stream().filter(Objects::nonNull).collect(Collectors.toList());
		}

		log.trace("getGroups({}) returns: {}", objects, result);
		return result;
	}

	private Set<Long> getGroupIdsWhereUserIsMember(Long userId) {
		log.trace("getGroupIdsWhereUserIsMember({})", userId);

		FilterBuilder filter = and(equal(OBJECT_CLASS, PERUN_USER),equal(PERUN_USER_ID, String.valueOf(userId)));
		String[] attributes = new String[] { MEMBER_OF };
		EntryMapper<Set<Long>> mapper = e -> {
			Set<Long> ids = new HashSet<>();
			if (checkHasAttributes(e, attributes)) {
				Attribute a = e.get(MEMBER_OF);
				a.iterator().forEachRemaining(id -> {
					String fullVal = id.getString();
					String val = fullVal.split(",")[0];
					val = val.replace(PERUN_GROUP_ID + '=', "");
					ids.add(Long.valueOf(val));
				});
			}

			return ids;
		};

		List<Set<Long>> memberGroupIdsAll = connectorLdap.search(null, filter, SearchScope.SUBTREE, attributes, mapper);
		Set<Long> memberGroupIds = memberGroupIdsAll.stream().flatMap(Set::stream).collect(Collectors.toSet());

		log.trace("getGroupIdsWhereUserIsMember({}) returns: {}", userId, memberGroupIds);
		return memberGroupIds;
	}

	private Set<Long> getGroupIdsWithAccessToFacility(Long facilityId) {
		log.trace("getGroupIdsWithAccessToFacility({})", facilityId);

		FilterBuilder filter = and(equal(OBJECT_CLASS, PERUN_RESOURCE), equal(PERUN_FACILITY_ID, String.valueOf(facilityId)));
		String[] attributes = new String[] { ASSIGNED_GROUP_ID };
		EntryMapper<Set<Long>> mapper = e -> {
			Set<Long> ids = new HashSet<>();
			if (checkHasAttributes(e, attributes)) {
				Attribute a = e.get(ASSIGNED_GROUP_ID);
				if (a != null) {
					a.iterator().forEachRemaining(id -> ids.add(Long.valueOf(id.getString())));
				}
			}

			return ids;
		};

		List<Set<Long>> assignedGroupIdsAll = connectorLdap.search(null, filter, SearchScope.SUBTREE, attributes, mapper);
		Set<Long> groupsWithAccessIds = assignedGroupIdsAll.stream()
				.flatMap(Set::stream)
				.collect(Collectors.toSet());

		log.trace("getGroupIdsWithAccessToFacility({}) returns: {}", facilityId, groupsWithAccessIds);
		return groupsWithAccessIds;
	}

	private Map<String, PerunAttributeValue> getAttributeValues(String dnPrefix, Collection<String> attrsToFetch,
																PerunEntityType entity) {
		log.trace("getAttributeValues({}, {}, {})", dnPrefix, attrsToFetch, entity);
		Set<AttributeMapping> mappings = getMappingsForAttrNames(entity, attrsToFetch);
		String[] attributes = getAttributesFromMappings(mappings);

		Map<String, PerunAttributeValue> res = new HashMap<>();
		if (attributes.length != 0) {
			EntryMapper<Map<String, PerunAttributeValue>> mapper = attrValueMapper(mappings);
			res = this.connectorLdap.lookup(dnPrefix, attributes, mapper);
		}
		log.trace("getAttributeValues({}, {}, {}) returns: {}", dnPrefix, attrsToFetch, entity, res);
		return res;
	}

	private List<Group> getGroupsByUniqueGroupNames(Set<String> groupNames) {
		log.trace("getGroupsByUniqueGroupNames({})", groupNames);
		List<Group> groups = getGroups(groupNames, PERUN_UNIQUE_GROUP_NAME);
		groups = groups.stream().filter(Objects::nonNull).collect(Collectors.toList());
		log.trace("getGroupsByUniqueGroupNames({}) returns {}", groupNames, groups);
		return groups;
	}

	private boolean checkHasAttributes(Entry e, String[] attributes) {
		if (e == null) {
			return false;
		} else if (attributes == null) {
			return true;
		}

		for (String attr: attributes) {
			if (e.get(attr) == null) {
				return false;
			}
		}

		return true;
	}

	private EntryMapper<Map<String, PerunAttributeValue>> attrValueMapper(Set<AttributeMapping> attrMappings) {
		return entry -> {
			Map<String, PerunAttributeValue> resultMap = new LinkedHashMap<>();
			Map<String, Attribute> attrNamesMap = new HashMap<>();

			for (Attribute attr : entry.getAttributes()) {
				if (attr.isHumanReadable()) {
					attrNamesMap.put(attr.getId(), attr);
				}
			}

			for (AttributeMapping mapping: attrMappings) {
				if (mapping.getLdapName() == null || mapping.getLdapName().isEmpty()) {
					continue;
				}
				String ldapAttrName = mapping.getLdapName();
				// the library always converts name of attribute to lowercase, therefore we need to convert it as well
				Attribute attribute = attrNamesMap.getOrDefault(ldapAttrName.toLowerCase(), null);
				PerunAttributeValue value = parseValue(attribute, mapping);
				resultMap.put(mapping.getIdentifier(), value);
			}

			return resultMap;
		};
	}

	private PerunAttributeValue parseValue(Attribute attr, AttributeMapping mapping) {
		PerunAttrValueType type = mapping.getAttrType();
		boolean isNull = (attr == null || attr.get() == null || attr.get().isNull());
		if (isNull && PerunAttrValueType.BOOLEAN.equals(type)) {
			return new PerunAttributeValue(PerunAttributeValue.BOOLEAN_TYPE, jsonNodeFactory.booleanNode(false));
		} else if (isNull && PerunAttrValueType.ARRAY.equals(type)) {
			return new PerunAttributeValue(PerunAttributeValue.ARRAY_TYPE, jsonNodeFactory.arrayNode());
		} else if (isNull && PerunAttrValueType.LARGE_ARRAY.equals(type)) {
			return new PerunAttributeValue(PerunAttributeValue.LARGE_ARRAY_LIST_TYPE, jsonNodeFactory.arrayNode());
		} else if (isNull && PerunAttrValueType.MAP_JSON.equals(type)) {
			return new PerunAttributeValue(PerunAttributeValue.MAP_TYPE, jsonNodeFactory.objectNode());
		} else if (isNull && PerunAttrValueType.MAP_KEY_VALUE.equals(type)) {
			return new PerunAttributeValue(PerunAttributeValue.MAP_TYPE, jsonNodeFactory.objectNode());
		} else if (isNull) {
			return PerunAttributeValue.NULL;
		}

		switch (type) {
			case STRING:
				return new PerunAttributeValue(PerunAttributeValue.STRING_TYPE,
						jsonNodeFactory.textNode(attr.get().getString()));
			case LARGE_STRING:
				return new PerunAttributeValue(PerunAttributeValue.LARGE_STRING_TYPE,
						jsonNodeFactory.textNode(attr.get().getString()));
			case INTEGER:
				return new PerunAttributeValue(PerunAttributeValue.INTEGER_TYPE,
						jsonNodeFactory.numberNode(Long.parseLong(attr.get().getString())));
			case BOOLEAN:
				return new PerunAttributeValue(PerunAttributeValue.BOOLEAN_TYPE,
						jsonNodeFactory.booleanNode(Boolean.parseBoolean(attr.get().getString())));
			case ARRAY:
				return new PerunAttributeValue(PerunAttributeValue.ARRAY_TYPE,
						getArrNode(attr));
			case LARGE_ARRAY:
				return new PerunAttributeValue(PerunAttributeValue.LARGE_ARRAY_LIST_TYPE,
						getArrNode(attr));
			case MAP_JSON:
				return new PerunAttributeValue(PerunAttributeValue.MAP_TYPE,
						getMapNodeJson(attr));
			case MAP_KEY_VALUE:
				return new PerunAttributeValue(PerunAttributeValue.MAP_TYPE,
						getMapNodeSeparator(attr, mapping.getSeparator()));
			default:
				throw new IllegalArgumentException("unrecognized type");
		}

	}

	private ObjectNode getMapNodeSeparator(Attribute attr, String separator) {
		ObjectNode objectNode = jsonNodeFactory.objectNode();
		for (Value value : attr) {
			if (value.getString() != null) {
				String[] parts = value.getString().split(separator, 2);
				objectNode.put(parts[0], parts[1]);
			}
		}
		return objectNode;
	}

	private ObjectNode getMapNodeJson(Attribute attr) {
		String jsonStr = attr.get().getString();
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.readValue(jsonStr, ObjectNode.class);
		} catch (IOException e) {
			throw new InconvertibleValueException("Could not parse value");
		}
	}

	private ArrayNode getArrNode(Attribute attr) {
		ArrayNode arrayNode = jsonNodeFactory.arrayNode(attr.size());
		for (Value value : attr) {
			arrayNode.add(value.getString());
		}
		return arrayNode;
	}

	private boolean isNumber(String s) {
		try {
			Long.parseLong(s);
			return true;
		} catch (NumberFormatException | NullPointerException e) {
			return false;
		}
	}

	private Set<AttributeMapping> getMappingsForAttrNames(PerunEntityType entity, Collection<String> attrsToFetch) {
		log.trace("getMappingsForAttrNames({}, {})", entity, attrsToFetch);
		Set<AttributeMapping> mappings;
		switch (entity) {
			case USER:
				mappings = this.getUserAttributesMappingService()
						.getMappingsForAttrNames(attrsToFetch);
				break;
			case FACILITY:
				mappings = this.getFacilityAttributesMappingService()
						.getMappingsForAttrNames(attrsToFetch);
				break;
			case VO:
				mappings = this.getVoAttributesMappingService()
						.getMappingsForAttrNames(attrsToFetch);
				break;
			case GROUP:
				mappings = this.getGroupAttributesMappingService()
						.getMappingsForAttrNames(attrsToFetch);
				break;
			case RESOURCE:
				mappings = this.getResourceAttributesMappingService()
						.getMappingsForAttrNames(attrsToFetch);
				break;
			default:
				log.error("Unknown ENTITY {}", entity);
				mappings = new HashSet<>();
				break;
		}

		log.trace("getMappingsForAttrNames({}, {}) returns: {}", entity, attrsToFetch, mappings);
		return mappings;
	}

	private String[] getAttributesFromMappings(Set<AttributeMapping> mappings) {
		return mappings
				.stream()
				.map(AttributeMapping::getLdapName)
				.distinct()
				.filter(e -> !Strings.isNullOrEmpty(e))
				.collect(Collectors.toList())
				.toArray(new String[]{});
	}

	private static class CapabilitiesAssignedGroupsPair {
		private Set<String> capabilities;
		private Set<Long> assignedGroupIds;

		public CapabilitiesAssignedGroupsPair(Set<String> capabilities, Set<Long> groupIds) {
			this.capabilities = capabilities;
			this.assignedGroupIds = groupIds;
		}

		@Override
		public String toString() {
			return "CapabilitiesAssignedGroupsPair{" +
					"capabilities='" + capabilities + '\'' +
					", assignedGroupIds='" + assignedGroupIds + "'}";
		}
	}

}
