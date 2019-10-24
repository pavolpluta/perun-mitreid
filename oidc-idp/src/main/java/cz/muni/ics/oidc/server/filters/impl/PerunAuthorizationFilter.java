package cz.muni.ics.oidc.server.filters.impl;

import cz.muni.ics.oidc.BeanUtil;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.PerunAttribute;
import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.server.configurations.FacilityAttrsConfig;
import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import cz.muni.ics.oidc.server.connectors.PerunConnector;
import cz.muni.ics.oidc.server.filters.FiltersUtils;
import cz.muni.ics.oidc.server.filters.PerunFilterConstants;
import cz.muni.ics.oidc.server.filters.PerunRequestFilter;
import cz.muni.ics.oidc.server.filters.PerunRequestFilterParams;
import cz.muni.ics.oidc.web.controllers.ControllerUtils;
import cz.muni.ics.oidc.web.controllers.PerunUnapprovedController;
import cz.muni.ics.oidc.web.controllers.PerunUnapprovedRegistrationController;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Authorization filter. Decides if user can access the service based on his/hers
 * membership in the groups assigned to the Perun facility resources. Facility represents
 * client in this context.
 *
 * Configuration:
 * - based on the configuration of bean "facilityAttrsConfig"
 * @see cz.muni.ics.oidc.server.configurations.FacilityAttrsConfig
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class PerunAuthorizationFilter extends PerunRequestFilter {

	private final static Logger log = LoggerFactory.getLogger(PerunAuthorizationFilter.class);

	private final OAuth2RequestFactory authRequestFactory;
	private final ClientDetailsEntityService clientService;
	private final PerunConnector perunConnector;
	private final FacilityAttrsConfig facilityAttrsConfig;
	private final PerunOidcConfig perunOidcConfig;

	private RequestMatcher requestMatcher = new AntPathRequestMatcher(PerunFilterConstants.AUTHORIZE_REQ_PATTERN);

	public PerunAuthorizationFilter(PerunRequestFilterParams params) {
		super(params);

		BeanUtil beanUtil = params.getBeanUtil();

		this.authRequestFactory = beanUtil.getBean(OAuth2RequestFactory.class);
		this.clientService = beanUtil.getBean(ClientDetailsEntityService.class);
		this.perunConnector = beanUtil.getBean(PerunConnector.class);
		this.facilityAttrsConfig = beanUtil.getBean(FacilityAttrsConfig.class);
		this.perunOidcConfig = beanUtil.getBean(PerunOidcConfig.class);
	}

	@Override
	protected boolean process(ServletRequest req, ServletResponse res) {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		ClientDetailsEntity client = FiltersUtils.extractClient(requestMatcher, request, authRequestFactory, clientService);
		if (client == null) {
			log.debug("Could not fetch client, skip to next filter");
			return true;
		}

		String clientIdentifier = client.getClientId();

		Facility facility = perunConnector.getFacilityByClientId(clientIdentifier);
		if (facility == null) {
			log.error("Could not find facility with clientID: {}", clientIdentifier);
			log.info("Skipping filter because not able to find facility");
			return true;
		}

		PerunUser user = FiltersUtils.getPerunUser(request, perunOidcConfig, perunConnector);

		return decideAccess(facility, user, request, response, clientIdentifier);
	}

	private boolean decideAccess(Facility facility, PerunUser user, HttpServletRequest request,
							  HttpServletResponse response, String clientIdentifier) {
		Map<String, PerunAttribute> facilityAttributes = perunConnector.getFacilityAttributes(
				facility, facilityAttrsConfig.getMembershipAttrsAsList());

		if (! facilityAttributes.get(facilityAttrsConfig.getCheckGroupMembershipAttr()).valueAsBoolean()) {
			log.debug("Membership check not requested, skipping filter");
			return true;
		}
		boolean canAccess = perunConnector.canUserAccessBasedOnMembership(facility, user.getId());

		if (canAccess) {
			// allow access, continue with chain
			log.info("User allowed to access the service");
			return true;
		} else if (facilityAttributes.get(facilityAttrsConfig.getAllowRegistrationAttr()).valueAsBoolean()) {
			log.info("User not allowed to access the service");
			boolean canRegister = perunConnector.groupWhereCanRegisterExists(facility);
			if (canRegister) {
				PerunAttribute customRegUrlAttr = facilityAttributes.get(facilityAttrsConfig.getRegistrationURLAttr());
				if (customRegUrlAttr != null && customRegUrlAttr.getValue()!= null) {
					String customRegUrl = facilityAttributes.get(
							facilityAttrsConfig.getRegistrationURLAttr()).valueAsString();
					customRegUrl = validateUrl(customRegUrl);
					if (customRegUrl != null) {
						// redirect to custom registration URL
						log.debug("Redirect to custom registration URL: {}", customRegUrl);
						response.reset();
						response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
						response.setHeader("Location", customRegUrl);
						return false;
					}
				}

				if (facilityAttributes.get(facilityAttrsConfig.getDynamicRegistrationAttr()).valueAsBoolean()) {
					// redirect to registration form
					log.debug("Redirect to registration form");
					Map<String, String> params = new HashMap<>();
					params.put("client_id", clientIdentifier);
					params.put("facility_id", facility.getId().toString());
					params.put("user_id", String.valueOf(user.getId()));
					String redirectUrl = ControllerUtils.createRedirectUrl(request, PerunFilterConstants.AUTHORIZE_REQ_PATTERN,
							PerunUnapprovedRegistrationController.REGISTRATION_CONTINUE_MAPPING, params);
					response.reset();
					response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
					response.setHeader("Location", redirectUrl);
					return false;
				}
			}
		}

		// cannot register, redirect to unapproved
		log.debug("redirect to unapproved");
		String redirectUrl = ControllerUtils.createRedirectUrl(request, PerunFilterConstants.AUTHORIZE_REQ_PATTERN, PerunUnapprovedController.UNAPPROVED_MAPPING,
				Collections.singletonMap("client_id", clientIdentifier));
		response.reset();
		response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
		response.setHeader("Location", redirectUrl);

		return false;
	}

	private String validateUrl(String customRegUrl) {
		if (customRegUrl == null || customRegUrl.isEmpty()) {
			return null;
		}

		if (!customRegUrl.startsWith("http://")) {
			customRegUrl = "http://" + customRegUrl;
		}

		try {
			URL url = new URL(customRegUrl);
			URLConnection conn = url.openConnection();
			conn.connect();
			return customRegUrl;
		} catch (IOException e) {
			//this is ok, we can try add https:// to the url
		}

		customRegUrl = customRegUrl.replace("http://", "https://");

		try {
			URL url = new URL(customRegUrl);
			URLConnection conn = url.openConnection();
			conn.connect();
			return customRegUrl;
		} catch (IOException e) {
			return null;
		}
	}

}
