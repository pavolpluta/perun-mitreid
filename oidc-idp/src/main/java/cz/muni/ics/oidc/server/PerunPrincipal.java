package cz.muni.ics.oidc.server;

/**
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class PerunPrincipal {

	private String extLogin;
	private String extSourceName;

	public PerunPrincipal(String extLogin, String extSourceName) {
		this.extLogin = extLogin;
		this.extSourceName = extSourceName;
	}

	public String getExtLogin() {
		return extLogin;
	}

	public String getExtSourceName() {
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
