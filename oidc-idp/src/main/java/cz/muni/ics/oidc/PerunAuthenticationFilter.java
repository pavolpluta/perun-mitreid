package cz.muni.ics.oidc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Extracts preauthenticated user id. The user must be alredy authenticated by Kerberos, Shibboleth, X509,
 * this class only gets extSourceName and extLogin from HTTP request.
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class PerunAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {

	private final static Logger log = LoggerFactory.getLogger(PerunAuthenticationFilter.class);

	private static final String EXTSOURCE_NAME_LOCAL = "LOCAL";
	private static final String SHIB_IDENTITY_PROVIDER = "Shib-Identity-Provider";
	private static final String SSL_CLIENT_VERIFY = "SSL_CLIENT_VERIFY";
	private static final String SUCCESS = "SUCCESS";
	private static final String EXTSOURCE = "EXTSOURCE";
	private static final String ENV_REMOTE_USER = "ENV_REMOTE_USER";
	private static final String SSL_CLIENT_I_DN = "SSL_CLIENT_I_DN";
	private static final String SSL_CLIENT_S_DN = "SSL_CLIENT_S_DN";

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

		// If we have header Shib-Identity-Provider, then the user uses identity federation to authenticate
		String shibIdentityProvider = getStringAttribute(req, SHIB_IDENTITY_PROVIDER);
		String remoteUser = req.getRemoteUser();

		if (isNotEmpty(shibIdentityProvider)) {
			extSourceName = shibIdentityProvider;
			if (isNotEmpty(remoteUser)) {
				extLogin = remoteUser;
			}
		}

		// EXT_SOURCE was defined in Apache configuration (e.g. Kerberos or Local)
		else if (req.getAttribute(EXTSOURCE) != null) {
			extSourceName = getStringAttribute(req, EXTSOURCE);
			if (isNotEmpty(remoteUser)) {
				extLogin = remoteUser;
			} else {
				String env_remote_user = getStringAttribute(req, ENV_REMOTE_USER);
				if (isNotEmpty(env_remote_user)) {
					extLogin = env_remote_user;
				} else if (extSourceName.equals(EXTSOURCE_NAME_LOCAL)) {
					/* LOCAL EXTSOURCE */
					// If ExtSource is LOCAL then generate REMOTE_USER name on the fly
					extLogin = Long.toString(System.currentTimeMillis());
				}
			}
		}

		// X509 cert was used
		// Cert must be last since Apache asks for certificate everytime and fills cert properties even when Kerberos is in place.
		else if (Objects.equals(getStringAttribute(req, SSL_CLIENT_VERIFY), SUCCESS)) {
			extSourceName = getStringAttribute(req, SSL_CLIENT_I_DN);
			extLogin = getStringAttribute(req, SSL_CLIENT_S_DN);
		}

		if (extLogin == null || extSourceName == null) {
			throw new IllegalStateException("ExtSource name or userExtSourceLogin is null. " +
					"extSourceName: " + extSourceName + ", " +
					"extLogin: " + extLogin + ", "
			);
		}

		PerunPrincipal perunPrincipal = new PerunPrincipal(extLogin, extSourceName);
		log.trace("Extracted principal {}",perunPrincipal);
		return perunPrincipal;
	}

	@Override
	protected Object getPreAuthenticatedCredentials(HttpServletRequest httpServletRequest) {
		return "no credentials";
	}


	private static String getStringAttribute(HttpServletRequest req, String attributeName) {
		return (String) req.getAttribute(attributeName);
	}

}
