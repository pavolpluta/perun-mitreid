package cz.muni.ics.oidc.server.claims;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface for code that can produce claim values.
 *
 * @see cz.muni.ics.oidc.server.claims.sources for different implementations of claim sources
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public abstract class ClaimSource {

	public ClaimSource(ClaimSourceInitContext ctx) {
	}

	public abstract JsonNode produceValue(ClaimSourceProduceContext pctx);

	@Override
	public String toString() {
		return this.getClass().getName();
	}


}
