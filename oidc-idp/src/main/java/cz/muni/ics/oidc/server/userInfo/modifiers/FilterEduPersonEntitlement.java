package cz.muni.ics.oidc.server.userInfo.modifiers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.net.UrlEscapers;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.server.connectors.PerunConnector;
import cz.muni.ics.oidc.server.userInfo.PerunUserInfo;
import cz.muni.ics.oidc.server.userInfo.UserInfoModifier;
import cz.muni.ics.oidc.server.userInfo.UserInfoModifierInitContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UserInfo modifier to filter out groups that are not assigned to facility.
 * Group names are in AARC format from EduPersonEntitlement attribute.
 * Do not use on other that EduPersonEntitlement attribute (scope).
 *
 * @author Dominik Baránek <0Baranek.dominik0@gmail.com>
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class FilterEduPersonEntitlement implements UserInfoModifier {

	private static final Logger log = LoggerFactory.getLogger(FilterEduPersonEntitlement.class);

	private final String prefix;
	private final String authority;
	private final String scope;

	private PerunConnector perunConnector;

	public FilterEduPersonEntitlement(UserInfoModifierInitContext ctx) {
		scope = ctx.getProperty("scope", null);
		prefix = ctx.getProperty("prefix", null);
		authority = ctx.getProperty("authority", null);
		if (scope == null) {
			throw new IllegalArgumentException("Scope must be defined");
		}

		perunConnector = ctx.getPerunConnector();
		log.trace("Initialized UserInfo modifier FilterGroups: {}", this.toString());
	}

	@Override
	public void modify(PerunUserInfo perunUserInfo, String clientId) {
		if (clientId == null) {
			return;
		}

		Facility facility = perunConnector.getFacilityByClientId(clientId);
		List<String> facilityGroups = perunConnector.getGroupsAssignedToResourcesWithUniqueNames(facility);

		if (facilityGroups == null || facilityGroups.isEmpty()) {
			return;
		}

		Set<String> facilityGroupNamesInAarcFormat = facilityGroups.stream()
				.map(g -> getGroupNameInAarcFormat(prefix, g, authority))
				.collect(Collectors.toSet());

		JsonNode eduPersonEntitlementJson = perunUserInfo.getCustomClaims().get(scope);
		JsonNodeFactory factory = JsonNodeFactory.instance;
		ArrayNode result = new ArrayNode(factory);

		if (eduPersonEntitlementJson != null) {
			ArrayNode eduPersonEntitlementArrayNode = (ArrayNode) eduPersonEntitlementJson;
			for (int i = 0; i < eduPersonEntitlementArrayNode.size(); i++) {
				String value = eduPersonEntitlementArrayNode.get(i).textValue();

				if (facilityGroupNamesInAarcFormat.contains(value)) {
					log.trace("Entitlement {} has been found, keep it", value);
					result.add(value);
				} else if (!(value.startsWith(prefix) && value.endsWith(authority))) {
					log.trace("Entitlement {} is not issued by our data repository, keep it", value);
					result.add(value);
				} else {
					log.trace("Entitlement {} filtered out", value);
				}
			}
		}

		perunUserInfo.getCustomClaims().put(scope, result);
	}

	@Override
	public String toString() {
		return "FilterGroups{" +
				"prefix='" + prefix + '\'' +
				", authority='" + authority + '\'' +
				", scope='" + scope + '\'' +
				'}';
	}

	private String getGroupNameInAarcFormat(String prefix, String uniqueGroupName, String authority) {
		if (uniqueGroupName.matches("^[^:]*:members$")) {
			uniqueGroupName = uniqueGroupName.replace(":members", "");
		}

		return prefix + UrlEscapers.urlPathSegmentEscaper().escape(uniqueGroupName) + '#' + authority;
	}
}
