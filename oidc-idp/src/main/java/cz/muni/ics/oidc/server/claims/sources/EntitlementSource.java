package cz.muni.ics.oidc.server.claims.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.net.UrlEscapers;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;
import java.util.HashSet;
import java.util.Set;

/**
 * This source converts groupNames and resource capabilities to AARC format and joins them with eduPersonEntitlement
 * Configuration (replace [claimName] with claimName defined for source):
 * - custom.claim.[claimName].groupNames - groupNames attribute name
 * - custom.claim.[claimName].eduPersonEntitlement - eduPersonEntitlement attribute name
 * - custom.claim.[claimName].capabilities - capabilities attribute name
 * - custom.claim.[claimName].prefix - prefix added before name of group
 * - custom.claim.[claimName].authority - source of claim
 *
 * @author Dominik Baránek baranek@ics.muni.cz
 */
public class EntitlementSource extends ClaimSource {

	private String groupNames;
	private String eduPersonEntitlement;
	private String capabilities;
	private String prefix;
	private String authority;

	public EntitlementSource(ClaimSourceInitContext ctx) {
		super(ctx);
		groupNames = ctx.getProperty("groupNames", null);
		eduPersonEntitlement = ctx.getProperty("eduPersonEntitlement", null);
		capabilities = ctx.getProperty("capabilities", null);
		prefix = ctx.getProperty("prefix", null);
		authority = ctx.getProperty("authority", null);
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {

		JsonNode groupNamesJson = pctx.getRichUser().getJson(groupNames);
		JsonNode eduPersonEntitlementJson = pctx.getRichUser().getJson(eduPersonEntitlement);

		JsonNodeFactory factory = JsonNodeFactory.instance;
		ArrayNode result = new ArrayNode(factory);

		if (groupNamesJson != null) {
			ArrayNode groupNamesArrayNode = (ArrayNode) groupNamesJson;
			Set<String> groupNames = new HashSet<>();

			for (int i = 0; i < groupNamesArrayNode.size(); i++) {
				String value = groupNamesArrayNode.get(i).textValue();
				groupNames.add(value);
				result.add(wrapGroupNameToAARC(value));
			}

			if (pctx.getClient() != null) {
				Set<String> resultCapabilities = pctx.getPerunConnector()
						.getResourceCapabilities(pctx.getClient().getClientId(), groupNames, capabilities);

				for (String capability : resultCapabilities) {
					result.add(wrapGroupNameToAARC(capability));
				}
			}
		}

		if (eduPersonEntitlementJson != null) {
			ArrayNode eduPersonEntitlementArrayNode = (ArrayNode) eduPersonEntitlementJson;
			result.addAll(eduPersonEntitlementArrayNode);
		}

		return result;
	}

	private String wrapGroupNameToAARC(String groupName) {
		return prefix + UrlEscapers.urlPathSegmentEscaper().escape(groupName) + "#" + authority;
	}

}
