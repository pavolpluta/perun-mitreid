package cz.muni.ics.oidc;

import cz.muni.ics.oidc.claims.ClaimValueModifier;

import java.util.regex.Pattern;

/**
 * Keeps definition of a custom user claim.
 * <ul>
 *     <li><b>scope</b> - which scope must be granted to include the claim</li>
 *     <li><b>claim</b> - name of the claim</li>
 *     <li><b>perunAttributeName</b> - id of Perun user attribute to obtain values from</li>
 *     <li><b>claimValueModifier</b> - instance of a class implementing {@link ClaimValueModifier}</li>
 * </ul>
 * @see ClaimValueModifier
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunCustomClaimDefinition {

	private String scope;
	private String claim;
	private String perunAttributeName;
	private ClaimValueModifier claimValueModifier;

	PerunCustomClaimDefinition(String scope, String claim, String perunAttributeName, ClaimValueModifier claimValueModifier) {
		this.scope = scope;
		this.claim = claim;
		this.perunAttributeName = perunAttributeName;
		this.claimValueModifier = claimValueModifier;
	}

	public String getScope() {
		return scope;
	}

	public String getClaim() {
		return claim;
	}

	String getPerunAttributeName() {
		return perunAttributeName;
	}

	public ClaimValueModifier getClaimValueModifier() {
		return claimValueModifier;
	}

}
