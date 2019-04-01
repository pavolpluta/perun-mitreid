package cz.muni.ics.oidc.server.claims;

/**
 * Interface for all code that needs to modify claim values .
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public abstract class ClaimValueModifier {

	@SuppressWarnings("WeakerAccess")
	public ClaimValueModifier(ClaimValueModifierInitContext ctx) {
	}

	public abstract String modify(String value);

	@Override
	public String toString() {
		return this.getClass().getName();
	}
}
