package cz.muni.ics.oidc.server.claims.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.net.UrlEscapers;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;

/**
 * This source converts groupNames to AARC format and joins them with eduPersonEntitlement
 *
 * @author Dominik Baránek 0Baranek.dominik0@gmail.com
 */
public class JoinGroupNamesAndEduPersonEntitlementSource extends ClaimSource {

	private String groupNames;
	private String eduPersonEntitlement;
	private String prefix;
	private String authority;

	public JoinGroupNamesAndEduPersonEntitlementSource(ClaimSourceInitContext ctx) {
		super(ctx);
		groupNames = ctx.getProperty("groupNames", null);
		eduPersonEntitlement = ctx.getProperty("eduPersonEntitlement", null);
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

			for (int i = 0; i < groupNamesArrayNode.size(); i++) {
				String value = groupNamesArrayNode.get(i).textValue();
				value = prefix + UrlEscapers.urlPathSegmentEscaper().escape(value) + "#" + authority;
				result.add(value);
			}
		}

		if (eduPersonEntitlementJson != null) {
			ArrayNode eduPersonEntitlementArrayNode = (ArrayNode) eduPersonEntitlementJson;
			result.addAll(eduPersonEntitlementArrayNode);
		}

		return result;
	}
}
