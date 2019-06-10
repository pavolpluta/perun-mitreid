package cz.muni.ics.oidc.server.claims.modifiers;

import cz.muni.ics.oidc.server.claims.ClaimModifier;
import cz.muni.ics.oidc.server.claims.ClaimModifierInitContext;

import java.util.regex.Pattern;

/**
 * Replace regex modifier. Replaces parts matched by regex with string using backreferences to groups.
 *
 * @see java.util.regex.Matcher#replaceAll(String)
 * @author Martin Kuba makub@ics.muni.cz
 */
@SuppressWarnings("unused")
public class RegexReplaceModifier extends ClaimModifier {

	private Pattern regex;
	private String replacement;

	public RegexReplaceModifier(ClaimModifierInitContext ctx) {
		super(ctx);
		regex = Pattern.compile(ctx.getProperty("find", ""));
		replacement = ctx.getProperty("replace", "");
	}

	@Override
	public String modify(String value) {
		return regex.matcher(value).replaceAll(replacement);
	}

	@Override
	public String toString() {
		return "RegexReplaceModifier replacing" + regex.pattern() + " with " + replacement;
	}
}
