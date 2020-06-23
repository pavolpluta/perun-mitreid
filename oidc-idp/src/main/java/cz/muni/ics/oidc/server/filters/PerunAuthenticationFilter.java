package cz.muni.ics.oidc.server.filters;

import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.server.PerunAcrRepository;
import cz.muni.ics.oidc.server.PerunPrincipal;
import cz.muni.ics.oidc.server.adapters.PerunAdapter;
import cz.muni.ics.oidc.server.configurations.FacilityAttrsConfig;
import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import cz.muni.ics.oidc.web.controllers.PerunUnapprovedController;
import org.mitre.openid.connect.models.Acr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static cz.muni.ics.oidc.server.filters.PerunFilterConstants.EFILTER_PREFIX;
import static cz.muni.ics.oidc.server.filters.PerunFilterConstants.FILTER_PREFIX;
import static cz.muni.ics.oidc.server.filters.PerunFilterConstants.IDP_ENTITY_ID_PREFIX;
import static cz.muni.ics.oidc.server.filters.PerunFilterConstants.PARAM_AUTHN_CONTEXT_CLASS_REF;
import static cz.muni.ics.oidc.server.filters.PerunFilterConstants.PARAM_FORCE_AUTHN;
import static cz.muni.ics.oidc.server.filters.PerunFilterConstants.PARAM_LOGGED_OUT;
import static cz.muni.ics.oidc.server.filters.PerunFilterConstants.PARAM_TARGET;
import static cz.muni.ics.oidc.server.filters.PerunFilterConstants.PARAM_WAYF_EFILTER;
import static cz.muni.ics.oidc.server.filters.PerunFilterConstants.PARAM_WAYF_FILTER;
import static cz.muni.ics.oidc.server.filters.PerunFilterConstants.PARAM_WAYF_IDP;
import static cz.muni.ics.oidc.server.filters.PerunFilterConstants.REFEDS_MFA;
import static org.mitre.oauth2.model.RegisteredClientFields.CLIENT_ID;

/**
 * Extracts preauthenticated user id. The user must be already authenticated by Kerberos, Shibboleth, X509,
 * this class only gets extSourceName and extLogin from HTTP request.
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class PerunAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {

	private final static Logger log = LoggerFactory.getLogger(PerunAuthenticationFilter.class);

	AntPathRequestMatcher matcher = new AntPathRequestMatcher(PerunUnapprovedController.UNAPPROVED_MAPPING);

	@Autowired
	private PerunAdapter perunAdapter;

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
		String clientId = req.getParameter(CLIENT_ID);
		String redirectURL = null;
		if (mfaRequestedAndNotPerformedYet(req)) {
			// MFA - go to login with forceAuthn and also add loggedOut (as otherwise it would match the ACR again)
			log.debug("MFA requested, force login");
			redirectURL = buildLoginURL(req, clientId, true, true);
		} else if (req.getParameter(PARAM_FORCE_AUTHN) != null) {
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
			log.debug("User is logged in");
			if (principal == null &&  !matcher.matches(req)) {
				//user is logged in, but we cannot find him in Perun
				log.debug("User logged in, no principal found");
				FiltersUtils.redirectUnapproved(req, res, clientId);
			} else {
				PerunUser user;
				try {
					user = perunAdapter.getPreauthenticatedUserId(principal);
				} catch (RuntimeException e) {
					//user is logged in, but we cannot find him in Perun
					user = null;
				}

				if (user == null && !matcher.matches(req)) {
					log.debug("User logged in, no user found in Perun for principal {}", principal);
					FiltersUtils.redirectUnapproved(req, res, clientId);
					return;
				}

				if (principal != null && req.getParameter(Acr.PARAM_ACR) != null) {
					storeAcr(principal, req);
				}
			}

			super.doFilter(request, response, chain);
		}
	}

	private boolean mfaRequestedAndNotPerformedYet(HttpServletRequest req) {
		return req.getParameter(Acr.PARAM_ACR) != null
				&& req.getParameter(Acr.PARAM_ACR).contains(REFEDS_MFA)
				&& req.getParameter(PARAM_LOGGED_OUT) == null;
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
			returnURL = FiltersUtils.buildRequestURL(req, Collections.singletonMap(PARAM_LOGGED_OUT, "true"));
		} else {
			returnURL = FiltersUtils.buildRequestURL(req);
		}
		String authnContextClassRef = buildAuthnContextClassRef(clientId, req);

		String base = config.getSamlLoginURL();
		Map<String, String> params = new HashMap<>();
		params.put(PARAM_TARGET, returnURL);

		if (authnContextClassRef != null && !authnContextClassRef.trim().isEmpty()) {
			params.put(PARAM_AUTHN_CONTEXT_CLASS_REF, authnContextClassRef);
		}
		if (forceAuthn) {
			params.put(PARAM_FORCE_AUTHN, "true");
		}

		String loginURL = buildStringURL(base, params);

		log.debug("constructLoginRedirectUrl returns: '{}'", loginURL);
		return loginURL;
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

		Map<String, PerunAttributeValue> filterAttributes = Collections.emptyMap();
		String filter = null;

		if (config.isAskPerunForIdpFiltersEnabled()) {
			Facility facility = null;
			if (clientId != null) {
				facility = perunAdapter.getFacilityByClientId(clientId);
			}

			if (facility != null) {
				filterAttributes = getFacilityFilterAttributes(facility);
			}
		}

		String idpEntityId = null;
		String idpFilter = extractIdpFilter(req, filterAttributes);
		String idpEfilter = extractIdpEFilter(req, filterAttributes);

		if (req.getParameter(PARAM_WAYF_IDP) != null) {
			idpEntityId = req.getParameter(PARAM_WAYF_IDP);
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

	private String extractIdpEFilter(HttpServletRequest req, Map<String, PerunAttributeValue> filterAttributes) {
		log.debug("extractIdpEFilter");
		String result = null;
		if (req.getParameter(PARAM_WAYF_EFILTER) != null) {
			result = req.getParameter(PARAM_WAYF_EFILTER);
		} else if (filterAttributes.get(facilityAttrsConfig.getWayfEFilterAttr()) != null) {
			PerunAttributeValue filterAttribute = filterAttributes.get(facilityAttrsConfig.getWayfEFilterAttr());
			if (filterAttribute.getValue() != null) {
				result = filterAttribute.valueAsString();
			}
		}

		log.debug("extractIdpEFilter returns: {}", result);
		return result;
	}

	private String extractIdpFilter(HttpServletRequest req, Map<String, PerunAttributeValue> filterAttributes) {
		log.debug("extractIdpFilter");
		String result = null;
		if (req.getParameter(PARAM_WAYF_FILTER) != null) {
			result = req.getParameter(PARAM_WAYF_FILTER);
		} else if (filterAttributes.get(facilityAttrsConfig.getWayfFilterAttr()) != null) {
			PerunAttributeValue filterAttribute = filterAttributes.get(facilityAttrsConfig.getWayfFilterAttr());
			if (filterAttribute.getValue() != null) {
				result = filterAttribute.valueAsString();
			}
		}

		log.debug("extractIdpFilter returns: {}", result);
		return result;
	}

	private Map<String, PerunAttributeValue> getFacilityFilterAttributes(Facility facility) {
		log.debug("getFacilityFilterAttributes({})", facility);
		List<String> attrsToFetch = new ArrayList<>();
		attrsToFetch.add(facilityAttrsConfig.getWayfEFilterAttr());
		attrsToFetch.add(facilityAttrsConfig.getWayfFilterAttr());

		Map<String, PerunAttributeValue> result = perunAdapter.getFacilityAttributeValues(facility, attrsToFetch);
		log.debug("getFacilityFilterAttributes returns: {}", result);
		return result;
	}

}
