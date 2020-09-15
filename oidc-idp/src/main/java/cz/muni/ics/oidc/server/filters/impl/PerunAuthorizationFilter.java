package cz.muni.ics.oidc.server.filters.impl;

import cz.muni.ics.oidc.BeanUtil;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.server.adapters.PerunAdapter;
import cz.muni.ics.oidc.server.configurations.FacilityAttrsConfig;
import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import cz.muni.ics.oidc.server.filters.FiltersUtils;
import cz.muni.ics.oidc.server.filters.PerunFilterConstants;
import cz.muni.ics.oidc.server.filters.PerunRequestFilter;
import cz.muni.ics.oidc.server.filters.PerunRequestFilterParams;
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
	private final PerunAdapter perunAdapter;
	private final FacilityAttrsConfig facilityAttrsConfig;
	private final PerunOidcConfig perunOidcConfig;

	private RequestMatcher requestMatcher = new AntPathRequestMatcher(PerunFilterConstants.AUTHORIZE_REQ_PATTERN);

	public PerunAuthorizationFilter(PerunRequestFilterParams params) {
		super(params);

		BeanUtil beanUtil = params.getBeanUtil();

		this.authRequestFactory = beanUtil.getBean(OAuth2RequestFactory.class);
		this.clientService = beanUtil.getBean(ClientDetailsEntityService.class);
		this.perunAdapter = beanUtil.getBean(PerunAdapter.class);
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
		if (clientIdentifier == null) {
			log.debug("Could not fetch client because we do not have the identifier... Skip to next filter");
			return true;
		}

		Facility facility = perunAdapter.getFacilityByClientId(clientIdentifier);
		if (facility == null) {
			log.error("Could not find facility with clientID: {}", clientIdentifier);
			log.info("Skipping filter because not able to find facility");
			return true;
		}

		PerunUser user = FiltersUtils.getPerunUser(request, perunOidcConfig, perunAdapter);
		if (user == null) {
			log.error("Could not find user in request");
			log.info("Skipping filter because not able to extract user");
			return true;
		}

		return FiltersUtils.decideAccess(facility, user, request, response, clientIdentifier, perunAdapter, facilityAttrsConfig);
	}

}
