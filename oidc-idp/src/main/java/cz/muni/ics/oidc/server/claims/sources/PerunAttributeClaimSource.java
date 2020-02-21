package cz.muni.ics.oidc.server.claims.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;

/**
 * Source for claim which get value of attribute from Perun.
 *
 * Configuration (replace [claimName] with the name of the claim):
 * <ul>
 *     <li><b>custom.claim.[claimName].source.attribute</b> - name of the attribute in Perun</li>
 * </ul>
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
@SuppressWarnings("unused")
public class PerunAttributeClaimSource extends ClaimSource {

	private final String attributeName;

	public PerunAttributeClaimSource(ClaimSourceInitContext ctx) {
		super(ctx);
		attributeName = ctx.getProperty("attribute", "");
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {
		if (pctx.getAttrValues().containsKey(attributeName)) {
			return pctx.getAttrValues().get(attributeName).valueAsJson();
		} else {
			return NullNode.getInstance();
		}
	}

	@Override
	public String toString() {
		return "Perun attribute "+attributeName;
	}
}
