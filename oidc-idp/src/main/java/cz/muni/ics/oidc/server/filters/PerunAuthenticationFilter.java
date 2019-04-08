package cz.muni.ics.oidc.server.filters;

import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.PerunAttribute;
import cz.muni.ics.oidc.server.PerunPrincipal;
import cz.muni.ics.oidc.server.configurations.FacilityAttrsConfig;
import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import cz.muni.ics.oidc.server.connectors.PerunConnector;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Extracts preauthenticated user id. The user must be already authenticated by Kerberos, Shibboleth, X509,
 * this class only gets extSourceName and extLogin from HTTP request.
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class PerunAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {

	private final static Logger log = LoggerFactory.getLogger(PerunAuthenticationFilter.class);

	private static final String SHIB_IDENTITY_PROVIDER = "Shib-Identity-Provider";
	private static final String WAYF_IDP = "wayf_idpentityid";
	private static final String WAYF_FILTER = "wayf_filter";
	private static final String WAYF_EFILTER = "wayf_efilter";
	private static final String CLIENT_ID = "client_id";
	private static final String AUTHN_CONTEXT_CLASS_REF = "authnContextClassRef";
	private static final String IDP_ENTITY_ID_PARAM = "=urn:cesnet:proxyidp:idpentityid:";
	private static final String FILTER_PARAM = "=urn:cesnet:proxyidp:filter:";
	private static final String EFILTER_PARAM = "=urn:cesnet:proxyidp:efilter:";

	@Autowired
	private PerunConnector perunConnector;

	@Autowired
	private FacilityAttrsConfig facilityAttrsConfig;

	@Autowired
	private PerunOidcConfig config;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		log.debug("doFilter({}, {}, {})", request, response, chain);
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		PerunPrincipal principal = extractPerunPrincipal(req);

		if (principal == null || principal.getExtLogin() == null || principal.getExtSourceName() == null) {
			log.debug("User not logged in, redirecting to login page");
			String clientId = null;
			String idpEntityId = null;

			if (req.getParameter(CLIENT_ID) != null) {
				clientId = req.getParameter(CLIENT_ID);
			}

			if (req.getParameter(WAYF_IDP) != null) {
				idpEntityId = req.getParameter(WAYF_IDP);
			}

			Map<String, PerunAttribute> filterAttributes = Collections.emptyMap();

			if (config.isAskPerunForIdpFiltersEnabled()) {
				Facility facility = null;
				if (clientId != null) {
					facility = perunConnector.getFacilityByClientId(clientId);
				}

				if (facility != null) {
					filterAttributes = getFacilityFilterAttributes(facility);
				}
			}

			String idpFilter = extractIdpFilter(req, filterAttributes);
			String idpEfilter = extractIdpEfilter(req, filterAttributes);

			String redirectURL = constructRedirectUrl(req, idpEntityId, idpFilter, idpEfilter);

			log.debug("Redirecting to URL: {}", redirectURL);
			res.sendRedirect(redirectURL);
		} else {
			log.debug("User is logged in");
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
			String shibIdentityProvider = (String) req.getAttribute(SHIB_IDENTITY_PROVIDER);
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

		String shibIdentityProvider = (String) req.getAttribute(SHIB_IDENTITY_PROVIDER);
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

	private String extractIdpEfilter(HttpServletRequest req, Map<String, PerunAttribute> filterAttributes) {
		log.debug("extractIdpEfilter");
		String result = null;
		if (req.getParameter(WAYF_EFILTER) != null) {
			result = req.getParameter(WAYF_EFILTER);
		} else if (filterAttributes.get(facilityAttrsConfig.getWayfEFilterAttr()) != null) {
			PerunAttribute filterAttribute = filterAttributes.get(facilityAttrsConfig.getWayfEFilterAttr());
			if (filterAttribute.getValue() != null) {
				result = filterAttribute.valueAsString();
			}
		}

		log.debug("extractIdpEfilter returns: {}", result);
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

	private String constructRedirectUrl(HttpServletRequest req, String idpEntityId, String idpFilter, String idpEfilter)
			throws UnsupportedEncodingException {
		log.debug("constructRedirectUrl(idpEntityId: {}, idpFilter: {}, idpEfilter: {})", idpEntityId,
				idpFilter, idpEfilter);

		String returnURL;

		if (req.getQueryString() == null) {
			returnURL = req.getRequestURL().toString();
		} else {
			returnURL = req.getRequestURL().toString() + '?' + req.getQueryString();
		}

		returnURL = URLEncoder.encode(returnURL, String.valueOf(StandardCharsets.UTF_8));

		StringBuilder builder = new StringBuilder();
		builder.append(config.getLoginUrl());
		builder.append("?target=").append(returnURL);

		if (idpEntityId != null) {
			builder.append('&').append(AUTHN_CONTEXT_CLASS_REF).append(IDP_ENTITY_ID_PARAM).append(idpEntityId);
		}

		if (idpFilter != null) {
			builder.append('&').append(AUTHN_CONTEXT_CLASS_REF).append(FILTER_PARAM).append(idpFilter);
		}

		if (idpEfilter != null) {
			builder.append('&').append(AUTHN_CONTEXT_CLASS_REF).append(EFILTER_PARAM).append(idpEfilter);
		}

		log.debug("constructRedirectUrl returns: '{}'", builder.toString());
		return builder.toString();
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
