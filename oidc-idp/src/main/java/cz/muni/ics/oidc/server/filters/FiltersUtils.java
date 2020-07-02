package cz.muni.ics.oidc.server.filters;

import com.google.common.base.Strings;
import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.server.PerunPrincipal;
import cz.muni.ics.oidc.server.adapters.PerunAdapter;
import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import cz.muni.ics.oidc.web.controllers.ControllerUtils;
import cz.muni.ics.oidc.web.controllers.PerunUnapprovedController;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static cz.muni.ics.oidc.server.filters.PerunFilterConstants.PARAM_FORCE_AUTHN;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Utility class for filters. Contains common methods used by most of filter classes.
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class FiltersUtils {

	private static final Logger log = LoggerFactory.getLogger(FiltersUtils.class);

	/**
	 * Create map of request params in format key = name, value = paramValue.
	 *
	 * @param parameterMap Original map of parameters
	 * @return Map of parameters
	 */
	public static Map<String, String> createRequestMap(Map<String, String[]> parameterMap) {
		Map<String, String> requestMap = new HashMap<>();
		for (String key : parameterMap.keySet()) {
			String[] val = parameterMap.get(key);
			if (val != null && val.length > 0) {
				requestMap.put(key, val[0]); // add the first value only (which is what Spring seems to do)
			}
		}

		return requestMap;
	}

	/**
	 * Extract client from request
	 *
	 * @param requestMatcher matcher for matching the request
	 * @param request request to be matched and containing client
	 * @param authRequestFactory authorization request factory
	 * @param clientService service fetching client details
	 * @return extracted client, null if some error occurs
	 */
	@SuppressWarnings("unchecked")
	public static ClientDetailsEntity extractClient(RequestMatcher requestMatcher, HttpServletRequest request,
	                                         OAuth2RequestFactory authRequestFactory,
	                                         ClientDetailsEntityService clientService) {
		if (!requestMatcher.matches(request) || request.getParameter("response_type") == null) {
			return null;
		}

		AuthorizationRequest authRequest = authRequestFactory.createAuthorizationRequest(
				FiltersUtils.createRequestMap(request.getParameterMap()));

		ClientDetailsEntity client;
		if (Strings.isNullOrEmpty(authRequest.getClientId())) {
			log.warn("ClientID is null or empty, skip to next filter");
			return null;
		}

		client = clientService.loadClientByClientId(authRequest.getClientId());
		log.debug("Found client: {}", client.getClientId());

		if (Strings.isNullOrEmpty(client.getClientName())) {
			log.warn("ClientName is null or empty, skip to next filter");
			return null;
		}

		return client;
	}

	/**
	 * Get Perun user based on extSourceName and extLogin from request
	 * @param request Request object
	 * @param perunOidcConfig OIDC Configuration
	 * @param perunAdapter Adapter of Perun interface
	 * @return Found PerunUser
	 */
	public static PerunUser getPerunUser(HttpServletRequest request, PerunOidcConfig perunOidcConfig,
								  PerunAdapter perunAdapter) {
		Principal p = request.getUserPrincipal();

		String extSourceName = perunOidcConfig.getProxyExtSourceName();
		if (extSourceName == null) {
			extSourceName = (String) request.getAttribute(PerunFilterConstants.SHIB_IDENTITY_PROVIDER);
		}

		PerunPrincipal principal = new PerunPrincipal(p.getName(), extSourceName);
		return perunAdapter.getPreauthenticatedUserId(principal);
	}

	/**
	 * Extract PerunPrincipal from request
	 * @param req request object
	 * @param proxyExtSourceName name of proxy
	 * @return extracted principal or null if not present
	 */
	public static PerunPrincipal extractPerunPrincipal(HttpServletRequest req, String proxyExtSourceName) {
		String extLogin = null;
		String extSourceName = null;

		String shibIdentityProvider = proxyExtSourceName;
		if (shibIdentityProvider == null) {
			shibIdentityProvider = (String) req.getAttribute(PerunFilterConstants.SHIB_IDENTITY_PROVIDER);
		}
		String remoteUser = req.getRemoteUser();

		if (isNotEmpty(shibIdentityProvider)) {
			extSourceName = shibIdentityProvider;
		}

		if (isNotEmpty(remoteUser)) {
			extLogin = remoteUser;
		}

		if (extSourceName == null || extLogin == null) {
			return null;
		}

		return new PerunPrincipal(extLogin, extSourceName);
	}

	/**
	 * Check if given scope has been requested
	 * @param scopeParam Value of parameter "scope" from request
	 * @param scope Name of scope to be found.
	 * @return TRUE if present, false otherwise
	 */
	public static boolean isScopePresent(String scopeParam, String scope) {
		if (scopeParam == null || scopeParam.trim().isEmpty()) {
			return false;
		}

		String[] scopes = scopeParam.split(" ");
		for (String s : scopes) {
			if (s.equals(scope)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Build URL of original request, remove forceAuthn parameter.
	 * @param req request wrapper object
	 * @return Rebuilt URL.
	 */
	public static String buildRequestURL(HttpServletRequest req) {
		return buildRequestURL(req, null);
	}

	/**
	 * Build URL of original request, remove forceAuthn parameter, add new parameters if passed.
	 * @param req request wrapper object
	 * @param additionalParams parameters to be added
	 * @return Rebuilt URL.
	 */
	public static String buildRequestURL(HttpServletRequest req, Map<String, String> additionalParams) {
		log.trace("buildReturnUrl({})", req);

		String returnURL = req.getRequestURL().toString();

		if (req.getQueryString() != null) {
			if (req.getQueryString().contains(PARAM_FORCE_AUTHN)) {
				String queryStr = removeForceAuthParam(req.getQueryString());
				returnURL += ('?' + queryStr);
			} else {
				returnURL += ('?' + req.getQueryString());
			}

			if (additionalParams != null) {
				returnURL += ('&' + additionalParams.entrySet().stream()
						.map(pair -> pair.getKey() + '=' + pair.getValue())
						.collect(Collectors.joining("&")));
			}
		}

		log.trace("buildReturnUrl() returns: {}", returnURL);
		return returnURL;
	}

	/**
	 * Redirect user to the unapproved page.
	 * @param request original request object
	 * @param response response object
	 * @param clientId identifier of the service
	 */
	public static void redirectUnapproved(HttpServletRequest request, HttpServletResponse response, String clientId) {
		// cannot register, redirect to unapproved
		log.debug("redirect to unapproved");
		Map<String, String> params = new HashMap<>();
		if (clientId != null) {
			params.put("client_id", clientId);
		}

		String redirectUrl = ControllerUtils.createRedirectUrl(request, PerunFilterConstants.AUTHORIZE_REQ_PATTERN,
				PerunUnapprovedController.UNAPPROVED_MAPPING, params);
		response.reset();
		response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
		response.setHeader("Location", redirectUrl);
	}

	private static String removeForceAuthParam(String query) {
		return Arrays.stream(query.split("&"))
				.map(FiltersUtils::splitQueryParameter)
				.filter(pair -> !PARAM_FORCE_AUTHN.equals(pair.getKey()))
				.map(pair -> pair.getKey() + "=" + pair.getValue())
				.collect(Collectors.joining("&"));
	}

	private static Map.Entry<String, String> splitQueryParameter(String it) {
		final int idx = it.indexOf("=");
		final String key = (idx > 0) ? it.substring(0, idx) : it;
		final String value = (idx > 0 && it.length() > idx + 1) ? it.substring(idx + 1) : "";
		return new AbstractMap.SimpleImmutableEntry<>(key, value);
	}
}
