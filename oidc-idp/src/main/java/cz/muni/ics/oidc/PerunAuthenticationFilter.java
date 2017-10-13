package cz.muni.ics.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetailsSource;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Extracts preauthenticated user id. The user must be alredy authenticated by Kerberos, Shibboleth, X509.
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class PerunAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {

	private final static Logger log = LoggerFactory.getLogger(PerunAuthenticationFilter.class);

	private String perunUrl;
	private String perunUser;
	private String perunPassword;

	public void setPerunUrl(String perunUrl) {
		this.perunUrl = perunUrl;
	}

	public void setPerunUser(String perunUser) {
		this.perunUser = perunUser;
	}

	public void setPerunPassword(String perunPassword) {
		this.perunPassword = perunPassword;
	}

	/**
	 * Extracts extSourceName and userExtSourceLogin, and finds user id by calling Perun RPC.
	 * @param request HTTP request
	 * @return user id from Perun
	 */
	@Override
	protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
		log.trace("getPreAuthenticatedPrincipal()");
		PerunPrincipal perunPrincipal = parsePrincipal(request);
		String extSourceName = perunPrincipal.getExtSourceName();
		String userExtSourceLogin = perunPrincipal.getUserExtSourceLogin();
		//prepare basic auth
		RestTemplate restTemplate = new RestTemplate();
		List<ClientHttpRequestInterceptor> interceptors =
				Collections.singletonList(new BasicAuthorizationInterceptor(perunUser, perunPassword));
		restTemplate.setRequestFactory(new InterceptingClientHttpRequestFactory(restTemplate.getRequestFactory(), interceptors));
		//make call
		log.debug("calling Perun RPC usersManager/getUserByExtSourceNameAndExtLogin?extSourceName={}&userExtSourceLogin={}",extSourceName,userExtSourceLogin);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("extSourceName",extSourceName);
		map.put("userExtSourceLogin",userExtSourceLogin);
		JsonNode result = restTemplate.postForObject(perunUrl + "/json/usersManager/getUserByExtSourceNameAndExtLogin", map, JsonNode.class);
		Long id = result.path("id").asLong();
		log.debug("got user id={}",id);
		return id;
	}

	@Override
	protected Object getPreAuthenticatedCredentials(HttpServletRequest httpServletRequest) {
		return "dummy value";
	}

	public PerunAuthenticationFilter() {
		setAuthenticationDetailsSource(httpServletRequest -> "no details");
	}

	private static String getStringAttribute(HttpServletRequest req, String attributeName) {
		return (String) req.getAttribute(attributeName);
	}

	private static final String EXTSOURCE_NAME_LOCAL = "LOCAL";
	private static final String SHIB_IDENTITY_PROVIDER = "Shib-Identity-Provider";
	private static final String SSL_CLIENT_VERIFY = "SSL_CLIENT_VERIFY";
	private static final String SUCCESS = "SUCCESS";
	private static final String EXTSOURCE = "EXTSOURCE";
	private static final String ENV_REMOTE_USER = "ENV_REMOTE_USER";
	private static final String SSL_CLIENT_I_DN = "SSL_CLIENT_I_DN";
	private static final String SSL_CLIENT_S_DN = "SSL_CLIENT_S_DN";

	private static PerunPrincipal parsePrincipal(HttpServletRequest req) {

		String extLogin = null;
		String extSourceName = null;

		// If we have header Shib-Identity-Provider, then the user uses identity federation to authenticate
		String shibIdentityProvider = getStringAttribute(req,SHIB_IDENTITY_PROVIDER);
		String remoteUser = req.getRemoteUser();

		if (isNotEmpty(shibIdentityProvider)) {
			extSourceName =  shibIdentityProvider;
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
		else if (Objects.equals(getStringAttribute(req,SSL_CLIENT_VERIFY), SUCCESS)) {
			extSourceName = getStringAttribute(req, SSL_CLIENT_I_DN);
			extLogin = getStringAttribute(req, SSL_CLIENT_S_DN);
		}

		if (extLogin == null || extSourceName == null) {
			throw new IllegalStateException("ExtSource name or userExtSourceLogin is null. " +
					"extSourceName: " + extSourceName + ", " +
					"extLogin: " + extLogin + ", "
			);
		}

		return new PerunPrincipal(extSourceName, extLogin);
	}

}
