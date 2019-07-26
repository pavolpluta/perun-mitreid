package cz.muni.ics.oidc.server.userInfo;

/**
 * Interface for all code that needs to modify user info.
 *
 * @author Dominik Baránek 0Baranek.dominik0@gmail.com
 */
public interface UserInfoModifier {

	/**
	 * Performs modification of UserInfo object. Modification depends on implementation.
	 * ATTENTION: param clientId can be NULL. In that case, implementation should not fail, modification should be
	 * rather skipped.
	 *
	 * @param perunUserInfo UserInfo to be modified
	 * @param clientId Id of client. Can be null.
	 */
	void modify(PerunUserInfo perunUserInfo, String clientId);

}
