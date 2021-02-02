package cz.muni.ics.oidc.server.claims.modifiers;

import cz.muni.ics.oidc.server.claims.ClaimModifier;
import cz.muni.ics.oidc.server.claims.ClaimModifierInitContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Replace regex modifier. Replaces parts matched by regex with string using backreferences to groups.
 * <ul>
 *     <li><b>custom.claim.[claimName].modifier.find</b> - string to be replaced, can be a regex</li>
 *     <li><b>custom.claim.[claimName].modifier.append</b> - string to be used as replacement</li>
 * </ul>
 *
 * @see java.util.regex.Matcher#replaceAll(String)
 * @author Martin Kuba <makub@ics.muni.cz>
 */
@SuppressWarnings("unused")
public class RegexReplaceModifier extends ClaimModifier {

	private static final Logger log = LoggerFactory.getLogger(RegexReplaceModifier.class);

	private static final String FIND = "find";
	private static final String REPLACE = "replace";

	private final Pattern regex;
	private final String replacement;
	private final String claimName;

	public RegexReplaceModifier(ClaimModifierInitContext ctx) {
		super(ctx);
		this.claimName = ctx.getClaimName();
		regex = Pattern.compile(ctx.getProperty(FIND, ""));
		replacement = ctx.getProperty(REPLACE, "");
		log.debug("{}(modifier) - regex: '{}', replacement: '{}'", claimName, regex, replacement);
	}

	@Override
	public String modify(String value) {
		String modified = regex.matcher(value).replaceAll(replacement);
		log.trace("{} - modifying value '{}' by replacing matched part ('{}') with: '{}'", claimName, value, regex,
				replacement);
		log.trace("{} - new value: '{}", claimName, modified);
		return modified;
	}

	@Override
	public String toString() {
		return "RegexReplaceModifier replacing" + regex.pattern() + " with " + replacement;
	}
}
