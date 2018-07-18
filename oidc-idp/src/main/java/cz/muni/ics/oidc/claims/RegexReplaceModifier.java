package cz.muni.ics.oidc.claims;

import java.util.regex.Pattern;

/**
 * Replaces parts matched by regex with string using backreferences to groups.
 *
 * @see java.util.regex.Matcher#replaceAll(String)
 * @author Martin Kuba makub@ics.muni.cz
 */
@SuppressWarnings("unused")
public class RegexReplaceModifier extends ClaimValueModifier {

	private Pattern regex;
	private String replacement;

	public RegexReplaceModifier(ClaimValueModifierInitContext ctx) {
		super(ctx);
		regex = Pattern.compile(ctx.getProperties().getProperty(ctx.getPropertyPrefix() + ".find", ""));
		replacement = ctx.getProperties().getProperty(ctx.getPropertyPrefix() + ".replace", "");
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
