package cz.muni.ics.oidc.server.filters;

import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.PerunAttribute;
import cz.muni.ics.oidc.server.PerunAcrRepository;
import cz.muni.ics.oidc.server.PerunPrincipal;
import cz.muni.ics.oidc.server.configurations.FacilityAttrsConfig;
import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import cz.muni.ics.oidc.server.connectors.PerunConnector;
import org.mitre.openid.connect.models.Acr;
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
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

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
	private static final String FORCE_AUTHN = "forceAuthn";
	private static final String LOGIN_PARAM_TARGET = "target";
	private static final String PARAM_LOGOUT_PERFORMED = "loggedOut";

	@Autowired
	private PerunConnector perunConnector;

	@Autowired
	private FacilityAttrsConfig facilityAttrsConfig;

	@Autowired
	private PerunOidcConfig config;

	@Autowired
	private PerunAcrRepository acrRepository;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		PerunPrincipal principal = FiltersUtils.extractPerunPrincipal(req, config.getProxyExtSourceName());
		String clientId = null;

		if (req.getParameter(CLIENT_ID) != null) {
			clientId = req.getParameter(CLIENT_ID);
		}

		String redirectURL = null;
		if (mfaRequestedAndNotPerformedYet(req)) {
			// MFA - go to login with forceAuthn and also add loggedOut (as otherwise it would match the ACR again)
			log.debug("MFA requested, force login");
			redirectURL = buildLoginURL(req, clientId, true, true);
		} else if (req.getParameter(FORCE_AUTHN) != null) {
			// FORCE AUTHENTICATION - go to login with forceAuthn, it wil get removed and will not match when returned back
			log.debug("Force login");
			redirectURL = buildLoginURL(req, clientId, true, false);
		} else if (principal == null || principal.getExtLogin() == null || principal.getExtSourceName() == null) {
			// AUTHENTICATE
			log.debug("User not logged in, redirecting to login page");
			redirectURL = buildLoginURL(req, clientId, false, false);
		}

		if (redirectURL != null) {
			log.debug("Redirecting to URL: {}", redirectURL);
			res.sendRedirect(redirectURL);
		} else {
			log.debug("User is logged in, store ACR and log login");

			if (principal != null && req.getParameter(Acr.PARAM_ACR) != null) {
				storeAcr(principal, req);
			}

			super.doFilter(request, response, chain);
		}
	}

	private boolean mfaRequestedAndNotPerformedYet(HttpServletRequest req) {
		return req.getParameter(Acr.PARAM_ACR) != null
				&& req.getParameter(Acr.PARAM_ACR).contains(REFEDS_MFA)
				&& req.getParameter(PARAM_LOGOUT_PERFORMED) == null;
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

		PerunPrincipal perunPrincipal = FiltersUtils.extractPerunPrincipal(req, config.getProxyExtSourceName());
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

		long expiresAtEpoch = Instant.now().plusSeconds(600L).toEpochMilli();
		acr.setExpiresAt(expiresAtEpoch);

		log.debug("storing acr: {}", acr);
		acrRepository.store(acr);
	}

	private String buildLoginURL(HttpServletRequest req, String clientId, boolean forceAuthn, boolean addLoggedOut)
			throws UnsupportedEncodingException
	{
		log.debug("constructLoginRedirectUrl(req: {}, clientId: {})", req, clientId);

		String returnURL;
		if (addLoggedOut) {
			returnURL = buildRequestURL(req, Collections.singletonMap(PARAM_LOGOUT_PERFORMED, "true"));
		} else {
			returnURL = buildRequestURL(req);
		}
		String authnContextClassRef = buildAuthnContextClassRef(clientId, req);

		String base = config.getSamlLoginURL();
		Map<String, String> params = new HashMap<>();
		params.put(LOGIN_PARAM_TARGET, returnURL);

		if (authnContextClassRef != null && !authnContextClassRef.trim().isEmpty()) {
			params.put(AUTHN_CONTEXT_CLASS_REF, authnContextClassRef);
		}
		if (forceAuthn) {
			params.put(FORCE_AUTHN, "true");
		}

		String loginURL = buildStringURL(base, params);

		log.debug("constructLoginRedirectUrl returns: '{}'", loginURL);
		return loginURL;
	}

	private String buildRequestURL(HttpServletRequest req) {
		return buildRequestURL(req, null);
	}

	private String buildRequestURL(HttpServletRequest req, Map<String, String> additionalParams) {
		log.trace("buildReturnUrl({})", req);

		String returnURL = req.getRequestURL().toString();

		if (req.getQueryString() != null) {
			if (req.getQueryString().contains(FORCE_AUTHN)) {
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

	public String removeForceAuthParam(String query) {
		return Arrays.stream(query.split("&"))
				.map(this::splitQueryParameter)
				.filter(pair -> !FORCE_AUTHN.equals(pair.getKey()))
				.map(pair -> pair.getKey() + "=" + pair.getValue())
				.collect(Collectors.joining("&"));
	}

	public Map.Entry<String, String> splitQueryParameter(String it) {
		final int idx = it.indexOf("=");
		final String key = (idx > 0) ? it.substring(0, idx) : it;
		final String value = (idx > 0 && it.length() > idx + 1) ? it.substring(idx + 1) : "";
		return new AbstractMap.SimpleImmutableEntry<>(key, value);
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

}
