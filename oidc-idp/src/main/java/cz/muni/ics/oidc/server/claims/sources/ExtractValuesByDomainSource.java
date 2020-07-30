package cz.muni.ics.oidc.server.claims.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;
import cz.muni.ics.oidc.server.claims.ClaimUtils;

/**
 * This source extract attribute values for given scope
 *
 * Configuration (replace [claimName] with the name of the claim):
 * <ul>
 *     <li><b>custom.claim.[claimName].source.extractByDomain</b> - domain which should be matched</li>
 *     <li><b>custom.claim.[claimName].source.attributeName</b> - attribute in which the lookup should be performed</li>
 * </ul>
 *
 * @author Dominik Baránek <baranek@ics.muni.cz>
 */
public class ExtractValuesByDomainSource extends ClaimSource {

	private static final String EXTRACT_BY_DOMAIN = "extractByDomain";
	private static final String ATTRIBUTE_NAME = "attributeName";

	private final String domain;
	private final String attributeName;

	public ExtractValuesByDomainSource(ClaimSourceInitContext ctx) {
		super(ctx);
		this.domain = ClaimUtils.fillStringPropertyOrNoVal(EXTRACT_BY_DOMAIN, ctx);
		if (!ClaimUtils.isPropSet(this.domain)) {
			throw new IllegalArgumentException("Missing mandatory configuration option - domain");
		}
		this.attributeName = ClaimUtils.fillStringPropertyOrNoVal(ATTRIBUTE_NAME, ctx);
		if (!ClaimUtils.isPropSet(this.attributeName)) {
			throw new IllegalArgumentException("Missing mandatory configuration option - attributeName");
		}
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {
		if (!ClaimUtils.isPropSet(domain)) {
			return NullNode.getInstance();
		} else if (!ClaimUtils.isPropSetAndHasAttribute(attributeName, pctx)) {
			return NullNode.getInstance();
		}

		PerunAttributeValue attributeValue = pctx.getAttrValues().get(attributeName);
		if (attributeValue != null) {
			JsonNode attributeValueJson = attributeValue.valueAsJson();
			if (attributeValueJson.isTextual() && hasDomain(attributeValueJson.textValue(), domain)) {
				return attributeValueJson;
			} else if (attributeValueJson.isArray()) {
				ArrayNode arrayNode = (ArrayNode) attributeValueJson;
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

		return NullNode.getInstance();
	}

	private boolean hasDomain(String value, String domain) {
		String[] parts = value.split("@");
		return parts[parts.length - 1].equals(domain);
	}

}
