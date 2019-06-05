package cz.muni.ics.oidc.server.elixir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;

@SuppressWarnings("unused")
public class GA4GHClaimSource extends ClaimSource {

	public GA4GHClaimSource(ClaimSourceInitContext ctx) {
		super(ctx);
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {
		return TextNode.valueOf("nothing so far");
	}
}
