package cz.muni.ics.oidc;

/**
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class PerunPrincipal {

	private String extSourceName;
	private String userExtSourceLogin;

	PerunPrincipal(String extSourceName, String userExtSourceLogin) {
		this.extSourceName = extSourceName;
		this.userExtSourceLogin = userExtSourceLogin;
	}

	String getExtSourceName() {
		return extSourceName;
	}

	String getUserExtSourceLogin() {
		return userExtSourceLogin;
	}

	@Override
	public String toString() {
		return "PerunPrincipal{" +
				"extSourceName='" + extSourceName + '\'' +
				", userExtSourceLogin='" + userExtSourceLogin + '\'' +
				'}';
	}
}
