package cz.muni.ics.oidc.server.claims.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.net.UrlEscapers;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;
import org.apache.directory.api.util.Strings;
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

	private final String forwardedEntitlements;
	private final String resourceCapabilities;
	private final String facilityCapabilities;
	private final String prefix;
	private final String authority;

	public EntitlementSource(ClaimSourceInitContext ctx) {
		super(ctx);
		forwardedEntitlements = ctx.getProperty("forwardedEntitlements", "eduPersonEntitlement");
		resourceCapabilities = ctx.getProperty("resourceCapabilities", "capabilities");
		facilityCapabilities = ctx.getProperty("facilityCapabilities", "capabilities");
		prefix = ctx.getProperty("prefix", null);
		authority = ctx.getProperty("authority", null);
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
			ArrayNode groupNamesArrayNode = (ArrayNode) groupNamesJson;
			Set<String> groupNames = new HashSet<>();

			for (int i = 0; i < groupNamesArrayNode.size(); i++) {
				String value = groupNamesArrayNode.get(i).textValue();
				String[] parts = value.split(":", 2);
				if (parts.length == 2 && !Strings.isEmpty(parts[1]) && "members".equals(parts[1])) {
					parts[1] = parts[1].replace("members", "");
				}

				String gname = parts[0];
				if (Strings.isNotEmpty(parts[1])) {
					gname += (':' + parts[1]);
				}
				entitlements.add(wrapGroupNameToAARC(gname));
			}

			if (facility != null && !StringUtils.isEmpty(this.resourceCapabilities)) {
				Set<String> resultCapabilities = pctx.getPerunAdapter()
						.getResourceCapabilities(facility, groupNames, resourceCapabilities);

				for (String capability : resultCapabilities) {
					entitlements.add(wrapCapabilityToAARC(capability));
				}
			}
		}

		if (facility != null && !StringUtils.isEmpty(this.facilityCapabilities)) {
			Set<String> resultCapabilities = pctx.getPerunAdapter()
					.getFacilityCapabilities(facility, facilityCapabilities);
			for (String capability : resultCapabilities) {
				entitlements.add(wrapCapabilityToAARC(capability));
			}
		}

		if (this.forwardedEntitlements != null && !this.forwardedEntitlements.trim().isEmpty()) {
			PerunAttributeValue forwardedEntitlementsVal = pctx.getAttrValues().get(forwardedEntitlements);
			if (forwardedEntitlementsVal != null && !PerunAttributeValue.NULL.equals(forwardedEntitlementsVal)) {
				JsonNode eduPersonEntitlementJson = forwardedEntitlementsVal.valueAsJson();
				for (int i = 0; i < eduPersonEntitlementJson.size(); i++) {
					entitlements.add(eduPersonEntitlementJson.get(i).asText());
				}
			}
		}

		ArrayNode result = JsonNodeFactory.instance.arrayNode();
		for (String entitlement: entitlements) {
			result.add(entitlement);
		}

		return result;
	}

	private String wrapGroupNameToAARC(String groupName) {
		return prefix + "group:" + UrlEscapers.urlPathSegmentEscaper().escape(groupName) + "#" + authority;
	}

	private String wrapCapabilityToAARC(String capability) {
		return prefix + UrlEscapers.urlPathSegmentEscaper().escape(capability) + "#" + authority;
	}
}
