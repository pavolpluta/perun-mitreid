package cz.muni.ics.oidc.server.claims;

/**
 * Keeps definition of a custom user claim.
 *
 * Configuration declaring custom claims:
 * <ul>
 *     <li><b>custom.claims</b> - coma separated list of names of the claims</li>
 * </ul>
 *
 * Configuration for claim(replace [claimName] with name of the claim): *
 * <ul>
 *     <li><b>custom.claim.[claimName].claim</b> - name of the claim</li>
 *     <li><b>custom.claim.[claimName].scope</b> - scope that needs to be granted to include the claim</li>
 *     <li><b>custom.claim.[claimName].source.class</b> instance of a class implementing {@link ClaimSource}</li>
 *     <li><b>custom.claim.[claimName].modifier.class</b> instance of a class implementing {@link ClaimModifier}</li>
 * </ul>
 *
 *
 * @see ClaimSource
 * @see ClaimModifier
 * @author Martin Kuba <makub@ics.muni.cz>
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
