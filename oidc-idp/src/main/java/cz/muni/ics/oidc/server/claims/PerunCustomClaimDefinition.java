package cz.muni.ics.oidc.server.claims;

/**
 * Keeps definition of a custom user claim.
 * <ul>
 *     <li><b>scope</b> - which scope must be granted to include the claim</li>
 *     <li><b>claim</b> - name of the claim</li>
 *     <li><b>claimSource</b> - instance of a class implementing {@link ClaimSource}</li>
 *     <li><b>claimModifier</b> - instance of a class implementing {@link ClaimModifier}</li>
 * </ul>
 * @see ClaimModifier
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunCustomClaimDefinition {

	private String scope;
	private String claim;
	private ClaimSource claimSource;
	private ClaimModifier claimModifier;

	public PerunCustomClaimDefinition(String scope, String claim, ClaimSource claimSource, ClaimModifier claimModifier) {
		this.scope = scope;
		this.claim = claim;
		this.claimSource = claimSource;
		this.claimModifier = claimModifier;
	}

	public String getScope() {
		return scope;
	}

	public String getClaim() {
		return claim;
	}

	public ClaimSource getClaimSource() {
		return claimSource;
	}

	public ClaimModifier getClaimModifier() {
		return claimModifier;
	}

}
