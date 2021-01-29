package cz.muni.ics.oidc.server.claims.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;
import cz.muni.ics.oidc.server.claims.ClaimUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Claim source which takes value from two attributes from Perun.
 *
 * Configuration (replace [claimName] with the name of the claim):
 * <ul>
 *     <li><b>custom.claim.[claimName].source.attribute1</b> - name of the first attribute in Perun</li>
 *     <li><b>custom.claim.[claimName].source.attribute2</b> - name of the second attribute in Perun</li>
 * </ul>
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
@SuppressWarnings("unused")
public class TwoArrayAttributesClaimSource extends ClaimSource {

	private static final Logger log = LoggerFactory.getLogger(TwoArrayAttributesClaimSource.class);

	public static final String ATTRIBUTE_1 = "attribute1";
	public static final String ATTRIBUTE_2 = "attribute2";

	private final String attribute1Name;
	private final String attribute2Name;

	public TwoArrayAttributesClaimSource(ClaimSourceInitContext ctx) {
		super(ctx);
		log.debug("Initializing '{}'", this.getClass().getSimpleName());
		this.attribute1Name = ClaimUtils.fillStringPropertyOrNoVal(ATTRIBUTE_1, ctx);
		if (!ClaimUtils.isPropSet(this.attribute1Name)) {
			throw new IllegalArgumentException("Missing mandatory configuration option - attribute1");
		}
		this.attribute2Name = ClaimUtils.fillStringPropertyOrNoVal(ATTRIBUTE_2, ctx);
		if (!ClaimUtils.isPropSet(this.attribute2Name)) {
			throw new IllegalArgumentException("Missing mandatory configuration option - attribute2");
		}
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {
		JsonNode j1 = new ArrayNode(JsonNodeFactory.instance);
		if (ClaimUtils.isPropSetAndHasAttribute(attribute1Name, pctx)) {
			j1 = pctx.getAttrValues().get(attribute1Name).valueAsJson();
		}
		log.debug("Found values for '{}': {}", attribute1Name, j1);

		JsonNode j2 = new ArrayNode(JsonNodeFactory.instance);
		if (ClaimUtils.isPropSetAndHasAttribute(attribute2Name, pctx)) {
			j2 = pctx.getAttrValues().get(attribute2Name).valueAsJson();
		}
		log.debug("Found values for '{}': {}", attribute2Name, j2);

		if (j1 == null || j1.isNull() || !j1.isArray()) return j2;
		if (j2 == null || j2.isNull() || !j2.isArray()) return j1;

		ArrayNode a1 = (ArrayNode) j1;
		ArrayNode a2 = (ArrayNode) j2;
		ArrayNode result = a1.arrayNode(a1.size() + a2.size());

		result.addAll(a1);
		result.addAll(a2);
		log.debug("Final values for attributes '{}' and '{}' are: {}", attribute1Name, attribute2Name, result);
		return result;
	}

}
