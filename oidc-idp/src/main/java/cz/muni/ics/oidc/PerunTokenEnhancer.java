package cz.muni.ics.oidc;

import org.mitre.openid.connect.token.ConnectTokenEnhancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunTokenEnhancer extends ConnectTokenEnhancer {

	private final static Logger log = LoggerFactory.getLogger(PerunTokenEnhancer.class);

	@Override
	public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
		log.info("enhance(accessToken={},authentication={})", accessToken, authentication);
		log.info("principal: {}",authentication.getPrincipal());
		OAuth2Request oAuth2Request = authentication.getOAuth2Request();
		log.info("grantType: {}", oAuth2Request.getGrantType());
		log.info("clientId: {}", oAuth2Request.getClientId());
		log.info("redirectUri: {}", oAuth2Request.getRedirectUri());
		return super.enhance(accessToken, authentication);
	}

}
