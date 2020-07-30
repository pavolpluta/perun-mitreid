package cz.muni.ics.oidc.server.claims.modifiers;

import cz.muni.ics.oidc.server.claims.ClaimModifier;
import cz.muni.ics.oidc.server.claims.ClaimModifierInitContext;

/**
 * Appending modifier. Appends the given text to the claim value.
 *
 * Configuration (replace [claimName] with the name of the claim):
 * <ul>
 *     <li><b>custom.claim.[claimName].modifier.append</b> - string to be appended to the value</li>
 * </ul>
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
@SuppressWarnings("unused")
public class AppendModifier extends ClaimModifier {

	private static final String APPEND = "append";

	private final String appendText;

	public AppendModifier(ClaimModifierInitContext ctx) {
		super(ctx);
		appendText = ctx.getProperty(APPEND, "");
	}

	@Override
	public String modify(String value) {
		return value + appendText;
	}

	@Override
	public String toString() {
		return "AppendModifier appending " + appendText;
	}
}
