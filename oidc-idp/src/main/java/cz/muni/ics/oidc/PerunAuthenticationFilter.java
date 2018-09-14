package cz.muni.ics.oidc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.http.HttpServletRequest;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Extracts preauthenticated user id. The user must be alredy authenticated by Kerberos, Shibboleth, X509,
 * this class only gets extSourceName and extLogin from HTTP request.
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class PerunAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {

	private final static Logger log = LoggerFactory.getLogger(PerunAuthenticationFilter.class);

	private static final String SHIB_IDENTITY_PROVIDER = "Shib-Identity-Provider";

	/**
	 * Extracts extSourceName and extLogin from HTTP request.
	 *
	 * @param req HTTP request
	 * @return extracted user info as instance of PerunPrincipal
	 */
	@Override
	protected Object getPreAuthenticatedPrincipal(HttpServletRequest req) {
		log.trace("getPreAuthenticatedPrincipal()");

		String extLogin = null;
		String extSourceName = null;

		String shibIdentityProvider = (String) req.getAttribute(SHIB_IDENTITY_PROVIDER);
		String remoteUser = req.getRemoteUser();

		if (isNotEmpty(shibIdentityProvider)) {
			extSourceName = shibIdentityProvider;
		}
		if (isNotEmpty(remoteUser)) {
			extLogin = remoteUser;
		}

		if (extSourceName == null || extLogin == null) {
			throw new IllegalStateException("ExtSource name or userExtSourceLogin is null. " +
					"extSourceName: " + extSourceName + ", " +
					"extLogin: " + extLogin + ", "
			);
		}

		PerunPrincipal perunPrincipal = new PerunPrincipal(extLogin, extSourceName);
		log.trace("Extracted principal {}", perunPrincipal);
		return perunPrincipal;
	}

	@Override
	protected Object getPreAuthenticatedCredentials(HttpServletRequest httpServletRequest) {
		return "no credentials";
	}


}
