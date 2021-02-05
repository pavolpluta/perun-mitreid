package cz.muni.ics.oidc.server.claims;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface for all code that needs to modify claim values.
 *
 * @see cz.muni.ics.oidc.server.claims.modifiers for different implementations of claim value modifiers
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public abstract class ClaimModifier {

	private static final Logger log = LoggerFactory.getLogger(ClaimModifier.class);

	public ClaimModifier(ClaimModifierInitContext ctx) {
		log.debug("{} - claim modifier initialized", ctx.getClaimName());
	}

	public abstract String modify(String value);

	@Override
	public String toString() {
		return this.getClass().getName();
	}
}
