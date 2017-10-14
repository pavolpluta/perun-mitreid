package cz.muni.ics.oidc;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

import static org.mitre.openid.connect.web.AuthenticationTimeStamper.AUTH_TIMESTAMP;

/**
 * Must create timestamp.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		Date authTimestamp = new Date();
		request.getSession().setAttribute(AUTH_TIMESTAMP, authTimestamp);
	}
}
