package cz.muni.ics.oidc;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunCustomClaimDefinition {

	private String scope;
	private String claim;
	private String perunAttributeName;

	public PerunCustomClaimDefinition(String scope, String claim, String perunAttributeName) {
		this.scope = scope;
		this.claim = claim;
		this.perunAttributeName = perunAttributeName;
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
}
