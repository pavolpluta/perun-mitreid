package cz.muni.ics.oidc.server.claims.modifiers;

import cz.muni.ics.oidc.server.claims.ClaimModifier;
import cz.muni.ics.oidc.server.claims.ClaimModifierInitContext;

/**
 * Appending modifier. Appends the given text to the claim value.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
@SuppressWarnings("unused")
public class AppendModifier extends ClaimModifier {

	private String appendText;

	public AppendModifier(ClaimModifierInitContext ctx) {
		super(ctx);
		appendText = ctx.getProperty("append", "");
	}

	@Override
	public String modify(String value) {
		return value + appendText;
	}

	@Override
	public String toString() {
		return "AppendModifier appending "+appendText;
	}
}
