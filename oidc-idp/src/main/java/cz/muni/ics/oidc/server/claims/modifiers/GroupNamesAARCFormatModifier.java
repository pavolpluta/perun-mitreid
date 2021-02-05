package cz.muni.ics.oidc.server.claims.modifiers;

import com.google.common.net.UrlEscapers;
import cz.muni.ics.oidc.server.claims.ClaimModifier;
import cz.muni.ics.oidc.server.claims.ClaimModifierInitContext;
import cz.muni.ics.oidc.server.claims.ClaimUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger log = LoggerFactory.getLogger(GroupNamesAARCFormatModifier.class);

	public static final String PREFIX = "prefix";
	public static final String AUTHORITY = "authority";

	private final String prefix;
	private final String authority;
	private final String claimName;

	public GroupNamesAARCFormatModifier(ClaimModifierInitContext ctx) {
		super(ctx);
		this.claimName = ctx.getClaimName();
		this.prefix = ClaimUtils.fillStringPropertyOrNoVal(PREFIX, ctx);
		if (!ClaimUtils.isPropSet(this.prefix)) {
			throw new IllegalArgumentException(claimName + " - missing mandatory configuration option: " + PREFIX);
		}
		this.authority = ClaimUtils.fillStringPropertyOrNoVal(AUTHORITY, ctx);
		if (!ClaimUtils.isPropSet(this.authority)) {
			throw new IllegalArgumentException(claimName + " - missing mandatory configuration option: " + AUTHORITY);
		}
		log.debug("{}(modifier) - prefix: '{}', authority: '{}'", claimName, prefix, authority);
	}

	@Override
	public String modify(String value) {
		String modified = prefix + UrlEscapers.urlPathSegmentEscaper().escape(value) + "#" + authority;
		log.trace("{} - modifying value '{}' to AARC format", claimName, value);
		log.trace("{} - new value: '{}", claimName, modified);
		return modified;
	}

	@Override
	public String toString() {
		return "GroupNamesAARCFormatModifier to " + prefix + "<GROUP>#" + authority;
	}

}
