package cz.muni.ics.oidc.server.filters;

import com.google.common.base.Strings;
import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.server.PerunPrincipal;
import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import cz.muni.ics.oidc.server.connectors.PerunConnector;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

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
	 * @param perunConnector Connector to Perun interface
	 * @return Found PerunUser
	 */
	public static PerunUser getPerunUser(HttpServletRequest request, PerunOidcConfig perunOidcConfig,
								  PerunConnector perunConnector) {
		Principal p = request.getUserPrincipal();

		String extSourceName = perunOidcConfig.getProxyExtSourceName();
		if (extSourceName == null) {
			extSourceName = (String) request.getAttribute(PerunFilterConstants.SHIB_IDENTITY_PROVIDER);
		}

		PerunPrincipal principal = new PerunPrincipal(p.getName(), extSourceName);
		return perunConnector.getPreauthenticatedUserId(principal);
	}
}
