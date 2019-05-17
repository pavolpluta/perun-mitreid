package cz.muni.ics.oidc.server.claims.modifiers;

/**
 * Appending modifier. Appends the given text to the claim value.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
@SuppressWarnings("unused")
public class AppendModifier extends ClaimValueModifier {

	private String appendText;

	public AppendModifier(ClaimValueModifierInitContext ctx) {
		super(ctx);
		appendText = ctx.getProperties().getProperty(ctx.getPropertyPrefix() + ".append", "");
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
