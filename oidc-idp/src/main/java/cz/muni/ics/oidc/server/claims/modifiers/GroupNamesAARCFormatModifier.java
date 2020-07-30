package cz.muni.ics.oidc.server.claims.modifiers;

import com.google.common.net.UrlEscapers;
import cz.muni.ics.oidc.server.claims.ClaimModifier;
import cz.muni.ics.oidc.server.claims.ClaimModifierInitContext;
import cz.muni.ics.oidc.server.claims.ClaimUtils;

/**
 * GroupName to AARC Format modifier. Converts groupName values to AARC format.
 * Construction: prefix:URL_ENCODED_VALUE#authority
 * Example: urn:geant:cesnet.cz:group:some%20value#perun.cesnet.cz
 *
 * Configuration (replace [claimName] with the name of the claim):
 * <ul>
 *     <li><b>custom.claim.[claimName].modifier.prefix</b> - string to be prepended to the value, defaults to
 *         <i>urn:geant:cesnet.cz:group:</i>
 *     </li>
 *     <li><b>custom.claim.[claimName].modifier.append</b> - string to be appended to the value, represents authority
 *         who has released the value, defaults to <i>perun.cesnet.cz</i>
 *     </li>
 * </ul>
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
@SuppressWarnings("unused")
public class GroupNamesAARCFormatModifier extends ClaimModifier {

	public static final String PREFIX = "prefix";
	public static final String AUTHORITY = "authority";

	private final String prefix;
	private final String authority;

	public GroupNamesAARCFormatModifier(ClaimModifierInitContext ctx) {
		super(ctx);
		this.prefix = ClaimUtils.fillStringPropertyOrNoVal(PREFIX, ctx);
		if (!ClaimUtils.isPropSet(this.prefix)) {
			throw new IllegalArgumentException("Missing mandatory configuration option - prefix");
		}
		this.authority = ClaimUtils.fillStringPropertyOrNoVal(AUTHORITY, ctx);
		if (!ClaimUtils.isPropSet(this.authority)) {
			throw new IllegalArgumentException("Missing mandatory configuration option - authority");
		}
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
