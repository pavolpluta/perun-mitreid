package cz.muni.ics.oidc;

import org.mitre.openid.connect.token.ConnectTokenEnhancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

/**
 * Just logs issued tokens.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunTokenEnhancer extends ConnectTokenEnhancer {

	private final static Logger log = LoggerFactory.getLogger(PerunTokenEnhancer.class);

	@Override
	public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
		log.trace("enhance(accessToken={},authentication={})", accessToken, authentication);
		Object principal = authentication.getPrincipal();
		String userId = principal instanceof User ? ((User) principal).getUsername() : principal.toString();
		OAuth2Request oAuth2Request = authentication.getOAuth2Request();
		log.info("userId: {}, clientId: {}, grantType: {}, redirectUri: {}",
				userId, oAuth2Request.getClientId(), oAuth2Request.getGrantType(), oAuth2Request.getRedirectUri());
		return super.enhance(accessToken, authentication);
	}

}
