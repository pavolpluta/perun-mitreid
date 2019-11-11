package cz.muni.ics.oidc.server.filters;

import org.mitre.openid.connect.models.Acr;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.PerunAttribute;
import cz.muni.ics.oidc.server.PerunAcrRepository;
import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.server.PerunPrincipal;
import cz.muni.ics.oidc.server.configurations.FacilityAttrsConfig;
import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import cz.muni.ics.oidc.server.connectors.PerunConnector;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Extracts preauthenticated user id. The user must be already authenticated by Kerberos, Shibboleth, X509,
 * this class only gets extSourceName and extLogin from HTTP request.
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class PerunAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {

	private final static Logger log = LoggerFactory.getLogger(PerunAuthenticationFilter.class);

	private static final String WAYF_IDP = "wayf_idpentityid";
	private static final String WAYF_FILTER = "wayf_filter";
	private static final String WAYF_EFILTER = "wayf_efilter";
	private static final String CLIENT_ID = "client_id";
	private static final String IDP_ENTITY_ID_PREFIX = "urn:cesnet:proxyidp:idpentityid:";
	private static final String FILTER_PREFIX = "urn:cesnet:proxyidp:filter:";
	private static final String EFILTER_PREFIX = "urn:cesnet:proxyidp:efilter:";

	private static final String AUTHN_CONTEXT_CLASS_REF = "authnContextClassRef";
	private static final String REFEDS_MFA = "https://refeds.org/profile/mfa";
	private static final String LOGOUT_PARAM = "loggedOut";

	private static final long ONE_MINUTE_IN_MILLIS = 60000;

	@Autowired
	private PerunConnector perunConnector;

	@Autowired
	private FacilityAttrsConfig facilityAttrsConfig;

	@Autowired
	private PerunOidcConfig config;

	@Autowired
	private PerunAcrRepository acrRepository;

	@Autowired
	private ClientDetailsEntityService clientService;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		PerunPrincipal principal = extractPerunPrincipal(req);
		String clientId = null;

		if (req.getParameter(Acr.PARAM_CLIENT_ID) != null) {
			clientId = req.getParameter(Acr.PARAM_CLIENT_ID);
		}

		if (principal == null || principal.getExtLogin() == null || principal.getExtSourceName() == null) {
			log.debug("User not logged in, redirecting to login page");

			String redirectURL = buildLoginUrl(req, clientId, false);

			log.debug("Redirecting to URL: {}", redirectURL);
			res.sendRedirect(redirectURL);
		} else {
			log.debug("User is logged in");
			if (req.getParameter(Acr.PARAM_ACR) != null) {
				boolean end = handleACR(req, res, principal, clientId);
				if (end) {
					return;
				}
			}

			logUserLogin(req, principal);

			super.doFilter(request, response, chain);
		}
	}

	/**
	 * Extracts extSourceName and extLogin from HTTP request.
	 *
	 * @param req HTTP request
	 * @return extracted user info as instance of PerunPrincipal
	 */
	@Override
	protected Object getPreAuthenticatedPrincipal(HttpServletRequest req) {
		log.debug("getPreAuthenticatedPrincipal()");

		PerunPrincipal perunPrincipal = extractPerunPrincipal(req);
		if (perunPrincipal == null) {
			String shibIdentityProvider = config.getProxyExtSourceName();
			if (shibIdentityProvider == null) {
				shibIdentityProvider = (String) req.getAttribute(PerunFilterConstants.SHIB_IDENTITY_PROVIDER);
			}
			String remoteUser = req.getRemoteUser();
			throw new IllegalStateException("ExtSource name or userExtSourceLogin is null. " +
					"extSourceName: " + shibIdentityProvider + ", " +
					"extLogin: " + remoteUser + ", "
			);
		}
		log.debug("Extracted principal {}", perunPrincipal);
		return perunPrincipal;
	}

	@Override
	protected Object getPreAuthenticatedCredentials(HttpServletRequest httpServletRequest) {
		return "no credentials";
	}

	private PerunPrincipal extractPerunPrincipal(HttpServletRequest req) {
		String extLogin = null;
		String extSourceName = null;

		String shibIdentityProvider = config.getProxyExtSourceName();
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

	private boolean handleACR(HttpServletRequest req, HttpServletResponse res, PerunPrincipal principal, String clientId)
			throws IOException
	{
		log.trace("handleACR(req, res, principal: {}, clientId: {}", principal, clientId);
		boolean end;

		String acr = getAcrValues(req);
		if ((acr.contains(REFEDS_MFA) || acr.contains(urlEncode(REFEDS_MFA)))
				&& req.getParameter(LOGOUT_PARAM) == null) {
			String loginUrl = buildLoginUrl(req, clientId, true);
			String base = config.getSamlLogoutURL();
			Map<String, String> params = Collections.singletonMap("return", loginUrl);
			String redirect = buildStringURL(base, params);

			res.sendRedirect(redirect);
			end = true;
		} else {
			log.debug("storing acr");
			storeAcr(principal, req);
			end = false;
		}

		log.trace("handleACR() return: {}", end);
		return end;
	}

	private void storeAcr(PerunPrincipal principal, HttpServletRequest req) {
		String sub = principal.getExtLogin();
		String clientId = req.getParameter(Acr.PARAM_CLIENT_ID);
		String state = req.getParameter(Acr.PARAM_STATE);
		String acrValues = req.getParameter(Acr.PARAM_ACR);
		String shibAuthnContextClass = (String) req.getAttribute(PerunFilterConstants.SHIB_AUTHN_CONTEXT_CLASS);
		if (shibAuthnContextClass == null) {
			shibAuthnContextClass = (String) req.getAttribute(PerunFilterConstants.SHIB_AUTHN_CONTEXT_METHOD);
		}

		Acr acr = new Acr(sub, clientId, acrValues, state, shibAuthnContextClass);

		long t = Calendar.getInstance().getTimeInMillis();
		Date afterAddingTenMins = new Date(t + (10 * ONE_MINUTE_IN_MILLIS));
		acr.setExpiration(afterAddingTenMins);

		log.debug("storing acr: {}", acr);
		acrRepository.store(acr);
	}

	private String buildLoginUrl(HttpServletRequest req, String clientId, boolean loggedOut)
			throws UnsupportedEncodingException
	{
		log.debug("constructLoginRedirectUrl(req: {}, clientId: {})", req, clientId);

		String returnURL = buildReturnUrl(req, loggedOut);
		String authnContextClassRef = buildAuthnContextClassRef(clientId, req);

		String base = config.getSamlLoginURL();
		Map<String, String> params = new HashMap<>();
		params.put("target", returnURL);
		if (authnContextClassRef != null && !authnContextClassRef.trim().isEmpty()) {
			params.put(AUTHN_CONTEXT_CLASS_REF, authnContextClassRef);
		}

		String loginURL = buildStringURL(base, params);

		log.debug("constructLoginRedirectUrl returns: '{}'", loginURL);
		return loginURL;
	}

	private String buildReturnUrl(HttpServletRequest req, boolean loggedOut) {
		log.trace("buildReturnUrl({})", req);

		String returnURL;

		if (req.getQueryString() == null) {
			returnURL = req.getRequestURL().toString();
			if (loggedOut) {
				returnURL += ('?' + LOGOUT_PARAM + '=' + true);
			}
		} else {
			returnURL = req.getRequestURL().toString() + '?' + req.getQueryString();
			if (loggedOut) {
				returnURL += ('&' + LOGOUT_PARAM + '=' + true);
			}
		}

		log.trace("buildReturnUrl() returns: {}", returnURL);
		return returnURL;
	}

	private String buildAuthnContextClassRef(String clientId, HttpServletRequest req) {
		log.trace("buildAuthnContextClassRef(clientId: {}, req: {})", clientId, req);

		String filterParam = getFilterParam(clientId, req);
		String acrValues = getAcrValues(req);

		StringJoiner joiner = new StringJoiner(" ");
		if (filterParam != null) {
			joiner.add(filterParam);
		}

		if (acrValues != null) {
			String[] parts = acrValues.split(" ");
			if (parts.length > 0) {
				for (String part: parts) {
					joiner.add(part);
				}
			}
		}

		String authnContextClassRef = joiner.toString().trim().isEmpty() ? null : joiner.toString();
		log.trace("buildAuthnContextClassRef() returns: {}", authnContextClassRef);
		return authnContextClassRef;
	}

	private String buildStringURL(String base, Map<String, String> params) throws UnsupportedEncodingException {
		log.trace("buildStringURL(base: {}, params: {})", base, params);
		if (params == null || params.isEmpty()) {
			return base;
		}

		StringJoiner paramsJoiner = new StringJoiner("&");
		for (Map.Entry<String, String> param: params.entrySet()) {
			String paramName = param.getKey();
			String paramValue = urlEncode(param.getValue());
			paramsJoiner.add(paramName + '=' + paramValue);
		}

		String stringURL = base + '?' + paramsJoiner.toString();
		log.trace("buildStringURL returns: {}", stringURL);
		return stringURL;
	}

	private String urlEncode(String str) throws UnsupportedEncodingException {
		return URLEncoder.encode(str, String.valueOf(StandardCharsets.UTF_8));
	}

	private String getFilterParam(String clientId, HttpServletRequest req) {
		log.trace("getFilterParam(clientId: {}, req: {})", clientId, req);

		Map<String, PerunAttribute> filterAttributes = Collections.emptyMap();
		String filter = null;

		if (config.isAskPerunForIdpFiltersEnabled()) {
			Facility facility = null;
			if (clientId != null) {
				facility = perunConnector.getFacilityByClientId(clientId);
			}

			if (facility != null) {
				filterAttributes = getFacilityFilterAttributes(facility);
			}
		}

		String idpEntityId = null;
		String idpFilter = extractIdpFilter(req, filterAttributes);
		String idpEfilter = extractIdpEFilter(req, filterAttributes);

		if (req.getParameter(WAYF_IDP) != null) {
			idpEntityId = req.getParameter(WAYF_IDP);
		}

		if (idpEntityId != null) {
			filter = IDP_ENTITY_ID_PREFIX + idpEntityId;
		} else if (idpFilter != null) {
			filter = FILTER_PREFIX + idpFilter;
		} else if (idpEfilter != null) {
			filter = EFILTER_PREFIX + idpEfilter;
		}

		log.trace("getFilterParam() returns: {}", filter);
		return filter;
	}

	private String getAcrValues(HttpServletRequest req) {
		log.trace("getAcrValues({})", req);
		String acrValues = null;
		if (req.getParameter(Acr.PARAM_ACR) != null) {
			acrValues = req.getParameter(Acr.PARAM_ACR);
		}

		log.trace("getAcrValues() returns: {}", acrValues);
		return acrValues;
	}

	private String extractIdpEFilter(HttpServletRequest req, Map<String, PerunAttribute> filterAttributes) {
		log.debug("extractIdpEFilter");
		String result = null;
		if (req.getParameter(WAYF_EFILTER) != null) {
			result = req.getParameter(WAYF_EFILTER);
		} else if (filterAttributes.get(facilityAttrsConfig.getWayfEFilterAttr()) != null) {
			PerunAttribute filterAttribute = filterAttributes.get(facilityAttrsConfig.getWayfEFilterAttr());
			if (filterAttribute.getValue() != null) {
				result = filterAttribute.valueAsString();
			}
		}

		log.debug("extractIdpEFilter returns: {}", result);
		return result;
	}

	private String extractIdpFilter(HttpServletRequest req, Map<String, PerunAttribute> filterAttributes) {
		log.debug("extractIdpFilter");
		String result = null;
		if (req.getParameter(WAYF_FILTER) != null) {
			result = req.getParameter(WAYF_FILTER);
		} else if (filterAttributes.get(facilityAttrsConfig.getWayfFilterAttr()) != null) {
			PerunAttribute filterAttribute = filterAttributes.get(facilityAttrsConfig.getWayfFilterAttr());
			if (filterAttribute.getValue() != null) {
				result = filterAttribute.valueAsString();
			}
		}

		log.debug("extractIdpFilter returns: {}", result);
		return result;
	}

	private Map<String, PerunAttribute> getFacilityFilterAttributes(Facility facility) {
		log.debug("getFacilityFilterAttributes({})", facility);
		List<String> attrsToFetch = new ArrayList<>();
		attrsToFetch.add(facilityAttrsConfig.getWayfEFilterAttr());
		attrsToFetch.add(facilityAttrsConfig.getWayfFilterAttr());

		Map<String, PerunAttribute> result = perunConnector.getFacilityAttributes(facility, attrsToFetch);
		log.debug("getFacilityFilterAttributes returns: {}", result);
		return result;
	}

	private void logUserLogin(HttpServletRequest req, PerunPrincipal principal) {
		String clientId = req.getParameter(CLIENT_ID);

		if (clientId == null || clientId.isEmpty()) {
			return;
		}

		ClientDetailsEntity client = clientService.loadClientByClientId(clientId);
		if (client == null) {
			return;
		}

		PerunUser user = perunConnector.getPreauthenticatedUserId(principal);
		Long userId = null;
		if (user != null) {
			userId = user.getId();
		}

		log.info("UserId: {}, identity: {}, service: {}, serviceName: {}, via IdP: {}", userId,
				principal.getExtLogin(), client.getClientId(), client.getClientName(), principal.getExtSourceName() );
	}
}
