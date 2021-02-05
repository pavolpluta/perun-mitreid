package cz.muni.ics.oidc.server.claims.modifiers;

import cz.muni.ics.oidc.server.claims.ClaimModifier;
import cz.muni.ics.oidc.server.claims.ClaimModifierInitContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger log = LoggerFactory.getLogger(AppendModifier.class);

	private static final String APPEND = "append";

	private final String appendText;
	private final String claimName;

	public AppendModifier(ClaimModifierInitContext ctx) {
		super(ctx);
		this.claimName = ctx.getClaimName();
		appendText = ctx.getProperty(APPEND, "");
		log.debug("{}(modifier) - appendText: '{}'", claimName, appendText);
	}

	@Override
	public String modify(String value) {
		String modified = value + appendText;
		log.trace("{} - modifying value '{}' by appending text '{}'", claimName, value, appendText);
		log.trace("{} - new value: '{}", claimName, modified);
		return modified;
	}

	@Override
	public String toString() {
		return "AppendModifier appending " + appendText;
	}

}
