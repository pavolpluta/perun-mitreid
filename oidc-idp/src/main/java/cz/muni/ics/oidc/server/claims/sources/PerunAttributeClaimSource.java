package cz.muni.ics.oidc.server.claims.sources;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;

@SuppressWarnings("unused")
public class PerunAttributeClaimSource extends ClaimSource {

	private final String attributeName;

	public PerunAttributeClaimSource(ClaimSourceInitContext ctx) {
		super(ctx);
		attributeName = ctx.getProperty("attribute", "");
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {
		return pctx.getRichUser().getJson(attributeName);
	}

	@Override
	public String toString() {
		return "Perun attribute "+attributeName;
	}
}
