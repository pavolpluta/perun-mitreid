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

import java.util.Collection;
import java.util.Map;
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
		log.debug("Initializing '{}'", this.getClass().getSimpleName());
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
		Map<Long, String> idToGnameMap = super.produceValueWithoutReplacing(pctx);
		Set<String> entitlements = new TreeSet<>();

		Facility facility = null;
		if (pctx.getClient() != null) {
			facility = pctx.getPerunAdapter().getFacilityByClientId(pctx.getClient().getClientId());
		}

		if (idToGnameMap != null && !idToGnameMap.values().isEmpty()) {
			this.fillEntitlementsFromGroupNames(idToGnameMap.values(), entitlements);
			log.debug("Entitlements for group names added.");
		}

		if (facility != null) {
			this.fillCapabilities(facility, pctx, idToGnameMap, entitlements);
			log.debug("Capabilities added.");
		}

		if (ClaimUtils.isPropSet(this.forwardedEntitlements)) {
			this.fillForwardedEntitlements(pctx, entitlements);
			log.debug("Forwarded entitlements added.");
		}

		ArrayNode result = JsonNodeFactory.instance.arrayNode();
		for (String entitlement: entitlements) {
			result.add(entitlement);
		}

		return result;
	}

	private void fillCapabilities(Facility facility, ClaimSourceProduceContext pctx,
								  Map<Long, String> idToGnameMap, Set<String> entitlements) {
		Set<String> resultCapabilities = pctx.getPerunAdapter()
				.getCapabilities(facility, idToGnameMap,
						ClaimUtils.isPropSet(this.facilityCapabilities) ? facilityCapabilities : null,
						ClaimUtils.isPropSet(this.resourceCapabilities)? resourceCapabilities: null);

		for (String capability : resultCapabilities) {
			entitlements.add(wrapCapabilityToAARC(capability));
			log.trace("Added capability: {}", capability);
		}
	}

	private void fillForwardedEntitlements(ClaimSourceProduceContext pctx, Set<String> entitlements) {
		PerunAttributeValue forwardedEntitlementsVal = pctx.getPerunAdapter()
				.getUserAttributeValue(pctx.getPerunUserId(), this.forwardedEntitlements);
		if (forwardedEntitlementsVal != null && !forwardedEntitlementsVal.isNullValue()) {
			JsonNode eduPersonEntitlementJson = forwardedEntitlementsVal.valueAsJson();
			for (int i = 0; i < eduPersonEntitlementJson.size(); i++) {
				String entitlement = eduPersonEntitlementJson.get(i).asText();
				log.trace("Added forwarded entitlement: {}", entitlement);
				entitlements.add(entitlement);
			}
		}
	}

	private void fillEntitlementsFromGroupNames(Collection<String> groupNames, Set<String> entitlements) {
		for (String fullGname: groupNames) {
			if (fullGname == null || fullGname.trim().isEmpty()) {
				continue;
			}

			String[] parts = fullGname.split(":", 2);
			if (parts.length == 2 && StringUtils.hasText(parts[1]) && MEMBERS.equals(parts[1])) {
				parts[1] = parts[1].replace(MEMBERS, "");
			}

			String gname = parts[0];
			if (StringUtils.hasText(parts[1])) {
				gname += (':' + parts[1]);
			}
			String gNameEntitlement = wrapGroupNameToAARC(gname);
			log.trace("Added group name entitlement: {}", gNameEntitlement);
			entitlements.add(gNameEntitlement);
		}
	}

	private String wrapGroupNameToAARC(String groupName) {
		return prefix + "group:" + UrlEscapers.urlPathSegmentEscaper().escape(groupName) + "#" + authority;
	}

	private String wrapCapabilityToAARC(String capability) {
		return prefix + UrlEscapers.urlPathSegmentEscaper().escape(capability) + "#" + authority;
	}
}
