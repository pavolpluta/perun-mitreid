package cz.muni.ics.oidc.server.claims.modifiers;

import com.google.common.net.UrlEscapers;
import cz.muni.ics.oidc.server.claims.ClaimModifier;
import cz.muni.ics.oidc.server.claims.ClaimModifierInitContext;

/**
 * GroupName to AARC Format modifier. Converts groupName values to AARC format.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
@SuppressWarnings("unused")
public class GroupNamesAARCFormatModifier extends ClaimModifier {

	private String prefix;
	private String authority;

	public GroupNamesAARCFormatModifier(ClaimModifierInitContext ctx) {
		super(ctx);
		prefix = ctx.getProperty("prefix", "urn:geant:cesnet.cz:group:");
		authority = ctx.getProperty("authority", "perun.cesnet.cz");
	}

	@Override
	public String modify(String value) {
		return prefix + UrlEscapers.urlPathSegmentEscaper().escape(value) + "#" + authority;
	}


	@Override
	public String toString() {
		return "GroupNamesAARCFormatModifier to " + prefix + "<GROUP>#" + authority;
	}
}
