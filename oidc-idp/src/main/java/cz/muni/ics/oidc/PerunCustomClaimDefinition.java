package cz.muni.ics.oidc;

import java.util.regex.Pattern;

/**
 * Keeps definitionof a custom user claim.
 * <ul>
 *     <li><b>scope</b> - which scope must be granted to include the claim</li>
 *     <li><b>claim</b> - name of the claim</li>
 *     <li><b>perunAttributeName</b> - id of Perun user attribute to obtain values from</li>
 *     <li><b>regex</b> - if defined, matching parts of each value will be replaced with replacement</li>
 *     <li><b>replacement</b> - string with ${g} or $g to replace matching groups</li>
 * </ul>
 * @see java.util.regex.Matcher#replaceAll(String)
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunCustomClaimDefinition {

	private String scope;
	private String claim;
	private String perunAttributeName;
	private Pattern regex;
	private String replacement;

	public PerunCustomClaimDefinition(String scope, String claim, String perunAttributeName,Pattern regex,String replacement) {
		this.scope = scope;
		this.claim = claim;
		this.perunAttributeName = perunAttributeName;
		this.regex = regex;
		this.replacement = replacement;
	}

	public String getScope() {
		return scope;
	}

	public String getClaim() {
		return claim;
	}

	public String getPerunAttributeName() {
		return perunAttributeName;
	}

	public Pattern getRegex() {
		return regex;
	}

	public String getReplacement() {
		return replacement;
	}


}
