package cz.muni.ics.oidc.server.userInfo.modifiers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.net.UrlEscapers;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.server.adapters.PerunAdapter;
import cz.muni.ics.oidc.server.userInfo.PerunUserInfo;
import cz.muni.ics.oidc.server.userInfo.UserInfoModifier;
import cz.muni.ics.oidc.server.userInfo.UserInfoModifierInitContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UserInfo modifier to filter out groups that are not assigned to facility.
 * Group names are in AARC format from EduPersonEntitlement attribute.
 * Do not use on other than EduPersonEntitlement attribute (scope).
 *
 * Configuration (replace [name] with the actual name of modifier specified in modifier list)
 * <ul>
 *     <li>userInfo.modifier.[name].scope - claim for which the modifier should be executed</li>
 *     <li>userInfo.modifier.[name].prefix - prefix of entitlement used for matching</li>
 *     <li>userInfo.modifier.[name].authority - suffix of entitlement (who has released it) used for matching</li>
 * </ul>
 *
 * @author Dominik Baránek <baranek@ics.muni.cz>
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class FilterEduPersonEntitlement implements UserInfoModifier {

	private static final Logger log = LoggerFactory.getLogger(FilterEduPersonEntitlement.class);

	private final String prefix;
	private final String authority;
	private final String scope;

	private PerunAdapter perunAdapter;

	public FilterEduPersonEntitlement(UserInfoModifierInitContext ctx) {
		scope = ctx.getProperty("scope", null);
		prefix = ctx.getProperty("prefix", "");
		authority = ctx.getProperty("authority", "");
		if (scope == null) {
			throw new IllegalArgumentException("Scope must be defined");
		}

		perunAdapter = ctx.getPerunAdapter();
		log.trace("Initialized UserInfo modifier FilterGroups: {}", this.toString());
	}

	@Override
	public void modify(PerunUserInfo perunUserInfo, String clientId) {
		List<String> facilityGroups = new ArrayList<>();

		if (clientId != null) {
			Facility facility = perunAdapter.getFacilityByClientId(clientId);
			if (facility != null) {
				facilityGroups = perunAdapter.getGroupsAssignedToResourcesWithUniqueNames(facility);
			}
		}

		Set<String> facilityGroupNamesInAarcFormat = facilityGroups.stream()
				.map(this::getGroupNameInAarcFormat)
				.collect(Collectors.toSet());

		JsonNode eduPersonEntitlementJson = perunUserInfo.getCustomClaims().get(scope);
		JsonNodeFactory factory = JsonNodeFactory.instance;
		ArrayNode result = new ArrayNode(factory);

		if (eduPersonEntitlementJson != null && !(eduPersonEntitlementJson instanceof NullNode)
				&& !eduPersonEntitlementJson.isNull()) {
			ArrayNode eduPersonEntitlementArrayNode = (ArrayNode) eduPersonEntitlementJson;
			for (JsonNode jsonNode : eduPersonEntitlementArrayNode) {
				String value = jsonNode.textValue();
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

	private String getGroupNameInAarcFormat(String uniqueGroupName) {
		if (uniqueGroupName.matches("^[^:]*:members$")) {
			uniqueGroupName = uniqueGroupName.replace(":members", "");
		}

		return prefix + "group:" + UrlEscapers.urlPathSegmentEscaper().escape(uniqueGroupName) + '#' + authority;
	}
}
