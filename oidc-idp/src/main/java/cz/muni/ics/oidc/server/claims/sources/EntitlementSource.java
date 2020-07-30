package cz.muni.ics.oidc.server.claims.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.net.UrlEscapers;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;
import cz.muni.ics.oidc.server.claims.ClaimUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * This source converts groupNames and resource capabilities to AARC format and joins them with eduPersonEntitlement.
 *
 * Configuration (replace [claimName] with the name of the claim):
 * <ul>
 *     <li>
 *         <b>custom.claim.[claimName].source.forwardedEntitlements</b> - forwardedEntitlements attribute name,
 *         if not specified, the forwarded entitlements will not be added to the list
 *     </li>
 *     <li><b>custom.claim.[claimName].source.resourceCapabilities</b> - resource capabilities attribute name for resources</li>
 *     <li><b>custom.claim.[claimName].source.facilityCapabilities</b> - resource capabilities attribute name for facility</li>
 *     <li><b>custom.claim.[claimName].source.prefix</b> - string to be prepended to the value,</li>
 *     <li>
 *         <b>custom.claim.[claimName].source.authority</b> - string to be appended to the value, represents authority
 *         who has released the value
 *     </li>
 * </ul>
 *
 * @author Dominik Baránek <baranek@ics.muni.cz>
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class EntitlementSource extends GroupNamesSource {

	public static final Logger log = LoggerFactory.getLogger(EntitlementSource.class);

	private static final String FORWARDED_ENTITLEMENTS = "forwardedEntitlements";
	private static final String RESOURCE_CAPABILITIES = "resourceCapabilities";
	private static final String FACILITY_CAPABILITIES = "facilityCapabilities";
	private static final String PREFIX = "prefix";
	private static final String AUTHORITY = "authority";
	private static final String MEMBERS = "members";

	private final String forwardedEntitlements;
	private final String resourceCapabilities;
	private final String facilityCapabilities;
	private final String prefix;
	private final String authority;

	public EntitlementSource(ClaimSourceInitContext ctx) {
		super(ctx);
		this.forwardedEntitlements = ClaimUtils.fillStringPropertyOrNoVal(FORWARDED_ENTITLEMENTS, ctx);
		this.resourceCapabilities = ClaimUtils.fillStringPropertyOrNoVal(RESOURCE_CAPABILITIES, ctx);
		this.facilityCapabilities = ClaimUtils.fillStringPropertyOrNoVal(FACILITY_CAPABILITIES, ctx);
		this.prefix = ClaimUtils.fillStringPropertyOrNoVal(PREFIX, ctx);
		if (!ClaimUtils.isPropSet(this.prefix)) {
			throw new IllegalArgumentException("Missing mandatory configuration option - prefix");
		}
		this.authority = ClaimUtils.fillStringPropertyOrNoVal(AUTHORITY, ctx);
		if (!ClaimUtils.isPropSet(this.authority)) {
			throw new IllegalArgumentException("Missing mandatory configuration option - authority");
		}
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {
		JsonNode groupNamesJson = super.produceValueWithoutReplacing(pctx);

		Set<String> entitlements = new TreeSet<>();

		Facility facility = null;
		if (pctx.getClient() != null) {
			facility = pctx.getPerunAdapter().getFacilityByClientId(pctx.getClient().getClientId());
		}

		if (groupNamesJson != null) {
			fillEntitlementsFromGroupNames(facility, pctx, groupNamesJson, entitlements);
		}

		if (facility != null && ClaimUtils.isPropSet(this.facilityCapabilities)) {
			fillFacilityCapabilities(facility, pctx, entitlements);
		}

		if (ClaimUtils.isPropSet(this.forwardedEntitlements)) {
			fillForwardedEntitlements(pctx, entitlements);
		}

		ArrayNode result = JsonNodeFactory.instance.arrayNode();
		for (String entitlement: entitlements) {
			result.add(entitlement);
		}

		return result;
	}

	private void fillForwardedEntitlements(ClaimSourceProduceContext pctx, Set<String> entitlements) {
		PerunAttributeValue forwardedEntitlementsVal = pctx.getPerunAdapter()
				.getUserAttributeValue(pctx.getPerunUserId(), this.forwardedEntitlements);
		if (forwardedEntitlementsVal != null && !PerunAttributeValue.NULL.equals(forwardedEntitlementsVal)) {
			JsonNode eduPersonEntitlementJson = forwardedEntitlementsVal.valueAsJson();
			for (int i = 0; i < eduPersonEntitlementJson.size(); i++) {
				log.debug("Added forwarded entitlement: {}", eduPersonEntitlementJson.get(i).asText());
				entitlements.add(eduPersonEntitlementJson.get(i).asText());
			}
		}
	}

	private void fillFacilityCapabilities(Facility facility, ClaimSourceProduceContext pctx, Set<String> entitlements) {
		Set<String> resultCapabilities = pctx.getPerunAdapter()
				.getFacilityCapabilities(facility, facilityCapabilities);
		for (String capability : resultCapabilities) {
			entitlements.add(wrapCapabilityToAARC(capability));
		}
	}

	private void fillEntitlementsFromGroupNames(Facility facility, ClaimSourceProduceContext pctx,
												JsonNode groupNamesJson, Set<String> entitlements) {
		ArrayNode groupNamesArrayNode = (ArrayNode) groupNamesJson;
		Set<String> groupNames = new HashSet<>();

		for (JsonNode arrItem: groupNamesArrayNode) {
			if (arrItem == null || arrItem.isNull()) {
				continue;
			}

			String value = arrItem.textValue();
			String[] parts = value.split(":", 2);
			if (parts.length == 2 && StringUtils.hasText(parts[1]) && MEMBERS.equals(parts[1])) {
				parts[1] = parts[1].replace(MEMBERS, "");
			}

			String gname = parts[0];
			if (StringUtils.hasText(parts[1])) {
				gname += (':' + parts[1]);
			}
			groupNames.add(value);
			entitlements.add(wrapGroupNameToAARC(gname));
		}

		if (facility != null && ClaimUtils.isPropSet(this.resourceCapabilities)) {
			Set<String> resultCapabilities = pctx.getPerunAdapter()
					.getResourceCapabilities(facility, groupNames, resourceCapabilities);

			for (String capability : resultCapabilities) {
				entitlements.add(wrapCapabilityToAARC(capability));
			}
		}
	}

	private String wrapGroupNameToAARC(String groupName) {
		return prefix + "group:" + UrlEscapers.urlPathSegmentEscaper().escape(groupName) + "#" + authority;
	}

	private String wrapCapabilityToAARC(String capability) {
		return prefix + UrlEscapers.urlPathSegmentEscaper().escape(capability) + "#" + authority;
	}
}
