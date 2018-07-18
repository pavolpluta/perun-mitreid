package cz.muni.ics.oidc.claims;

import com.google.common.net.UrlEscapers;

/**
 * Converts groupName values to AARC format.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
@SuppressWarnings("unused")
public class GroupNamesAARCFormatModifier extends ClaimValueModifier {

	private String prefix;
	private String authority;

	public GroupNamesAARCFormatModifier(ClaimValueModifierInitContext ctx) {
		super(ctx);
		prefix = ctx.getProperties().getProperty(ctx.getPropertyPrefix() + ".prefix", "urn:geant:cesnet.cz:group:");
		authority = ctx.getProperties().getProperty(ctx.getPropertyPrefix() + ".authority", "perun.cesnet.cz");
	}

	@Override
	public String modify(String value) {
		return prefix + UrlEscapers.urlPathSegmentEscaper().escape(value)+ "#" + authority;
	}


	@Override
	public String toString() {
		return "GroupNamesAARCFormatModifier to " + prefix + "<GROUP>#" + authority;
	}
}
