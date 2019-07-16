package cz.muni.ics.oidc.server.claims.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;

/**
 * This source extract attribute values for given scope
 *
 * @author Dominik Baránek 0Baranek.dominik0@gmail.com
 */
public class ExtractValuesByDomainSource extends ClaimSource {

	private String domain;
	private String attributeName;

	public ExtractValuesByDomainSource(ClaimSourceInitContext ctx) {
		super(ctx);
		domain = ctx.getProperty("extractByDomain", null);
		attributeName = ctx.getProperty("attributeName", null);
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {

		if (domain == null || domain.isEmpty()) {
			return null;
		}

		JsonNode attribute = pctx.getRichUser().getJson(attributeName);

		if (attribute != null) {
			if (attribute.isTextual() && hasDomain(attribute.textValue(), domain)) {
				return attribute;
			} else if (attribute.isArray()) {
				ArrayNode arrayNode = (ArrayNode) attribute;
				JsonNodeFactory factory = JsonNodeFactory.instance;
				ArrayNode result = new ArrayNode(factory);

				for (int i = 0; i < arrayNode.size(); i++) {
					String subValue = arrayNode.get(i).textValue();
					if (hasDomain(subValue, domain)) {
						result.add(subValue);
					}
				}
				return result;
			}
		}

		return null;
	}

	private boolean hasDomain(String value, String domain) {
		String[] parts = value.split("@");
		return parts[parts.length - 1].equals(domain);
	}
}
