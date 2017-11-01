package cz.muni.ics.oidc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

import static org.mitre.openid.connect.web.AuthenticationTimeStamper.AUTH_TIMESTAMP;

/**
 * Must create timestamp and some logging.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final static Logger log = LoggerFactory.getLogger(PerunAuthenticationSuccessHandler.class);

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		//must create timestamp
		Date authTimestamp = new Date();
		request.getSession().setAttribute(AUTH_TIMESTAMP, authTimestamp);
		//just logging
		if(authentication instanceof PreAuthenticatedAuthenticationToken) {
			PreAuthenticatedAuthenticationToken token = (PreAuthenticatedAuthenticationToken) authentication;
			Object details = token.getDetails();
			if(details instanceof WebAuthenticationDetails) {
				WebAuthenticationDetails webDetails = (WebAuthenticationDetails) details;
				log.info("successful authentication, remote IP address {}",webDetails.getRemoteAddress());
			}
		}
	}
}
