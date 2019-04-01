package cz.muni.ics.oidc.server.claims;

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

	public PerunCustomClaimDefinition(String scope, String claim, String perunAttributeName, ClaimValueModifier claimValueModifier) {
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

	public String getPerunAttributeName() {
		return perunAttributeName;
	}

	public ClaimValueModifier getClaimValueModifier() {
		return claimValueModifier;
	}

}
