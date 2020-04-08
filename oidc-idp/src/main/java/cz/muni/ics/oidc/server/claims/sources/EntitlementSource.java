package cz.muni.ics.oidc.server.claims.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.net.UrlEscapers;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

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

	private final String eduPersonEntitlement;
	private final String resourceCapabilities;
	private final String facilityCapabilities;
	private final String prefix;
	private final String authority;

	public EntitlementSource(ClaimSourceInitContext ctx) {
		super(ctx);
		eduPersonEntitlement = ctx.getProperty("forwardedEntitlements", null);
		resourceCapabilities = ctx.getProperty("resourceCapabilities", null);
		facilityCapabilities = ctx.getProperty("facilityCapabilities", null);
		prefix = ctx.getProperty("prefix", null);
		authority = ctx.getProperty("authority", null);
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {
		JsonNode groupNamesJson = super.produceValue(pctx);

		JsonNodeFactory factory = JsonNodeFactory.instance;
		ArrayNode result = new ArrayNode(factory);

		Facility facility = null;
		if (pctx.getClient() != null) {
			facility = pctx.getPerunConnector().getFacilityByClientId(pctx.getClient().getClientId());
		}

		if (groupNamesJson != null) {
			ArrayNode groupNamesArrayNode = (ArrayNode) groupNamesJson;
			Set<String> groupNames = new HashSet<>();

			for (int i = 0; i < groupNamesArrayNode.size(); i++) {
				String value = groupNamesArrayNode.get(i).textValue();
				groupNames.add(value);
				result.add(wrapGroupNameToAARC(value));
			}

			if (facility != null && !StringUtils.isEmpty(this.resourceCapabilities)) {
				Set<String> resultCapabilities = pctx.getPerunConnector()
						.getResourceCapabilities(facility, groupNames, resourceCapabilities);

				for (String capability : resultCapabilities) {
					result.add(wrapCapabilityToAARC(capability));
				}
			}
		}

		if (facility != null && !StringUtils.isEmpty(this.facilityCapabilities)) {
			Set<String> resultCapabilities = pctx.getPerunConnector()
					.getFacilityCapabilities(facility, facilityCapabilities);
			for (String capability : resultCapabilities) {
				result.add(wrapCapabilityToAARC(capability));
			}
		}

		if (this.eduPersonEntitlement != null && !this.eduPersonEntitlement.trim().isEmpty()) {
			JsonNode eduPersonEntitlementJson = pctx.getRichUser().getJson(eduPersonEntitlement);

			if (eduPersonEntitlementJson != null) {
				ArrayNode eduPersonEntitlementArrayNode = (ArrayNode) eduPersonEntitlementJson;
				result.addAll(eduPersonEntitlementArrayNode);
			}
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
