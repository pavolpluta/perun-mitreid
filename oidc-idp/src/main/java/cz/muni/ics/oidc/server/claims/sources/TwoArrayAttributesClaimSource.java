package cz.muni.ics.oidc.server.claims.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;
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
 */
@SuppressWarnings("unused")
public class TwoArrayAttributesClaimSource extends ClaimSource {

	private static final Logger log = LoggerFactory.getLogger(TwoArrayAttributesClaimSource.class);

	private final String attribute1Name;
	private final String attribute2Name;

	public TwoArrayAttributesClaimSource(ClaimSourceInitContext ctx) {
		super(ctx);
		attribute1Name = ctx.getProperty("attribute1", "");
		attribute2Name = ctx.getProperty("attribute2", "");
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {
		log.trace("produceValue(sub={})",pctx.getSub());
		JsonNode j1 = pctx.getRichUser().getJson(attribute1Name);
		log.trace("values for {}: {}",attribute1Name,j1);
		JsonNode j2 = pctx.getRichUser().getJson(attribute2Name);
		log.trace("values for {}: {}",attribute2Name,j2);
		if (j1 == null || !j1.isArray()) return j2;
		if (j2 == null || !j2.isArray()) return j1;
		ArrayNode a1 = (ArrayNode) j1;
		ArrayNode a2 = (ArrayNode) j2;
		ArrayNode result = a1.arrayNode(a1.size() + a2.size());
		result.addAll(a1);
		result.addAll(a2);
		return result;
	}
}
