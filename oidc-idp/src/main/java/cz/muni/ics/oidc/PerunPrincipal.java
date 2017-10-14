package cz.muni.ics.oidc;

/**
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class PerunPrincipal {

	private String extLogin;
	private String extSourceName;

	PerunPrincipal(String extLogin, String extSourceName) {
		this.extLogin = extLogin;
		this.extSourceName = extSourceName;
	}

	String getExtLogin() {
		return extLogin;
	}

	String getExtSourceName() {
		return extSourceName;
	}

	@Override
	public String toString() {
		return "PerunPrincipal{" +
				"extLogin='" + extLogin + '\'' +
				", extSourceName='" + extSourceName + '\'' +
				'}';
	}
}
