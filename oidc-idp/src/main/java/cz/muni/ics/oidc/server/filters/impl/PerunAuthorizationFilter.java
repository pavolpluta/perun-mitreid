package cz.muni.ics.oidc.server.filters.impl;

import cz.muni.ics.oidc.BeanUtil;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.server.adapters.PerunAdapter;
import cz.muni.ics.oidc.server.configurations.FacilityAttrsConfig;
import cz.muni.ics.oidc.server.filters.FilterParams;
import cz.muni.ics.oidc.server.filters.FiltersUtils;
import cz.muni.ics.oidc.server.filters.PerunRequestFilter;
import cz.muni.ics.oidc.server.filters.PerunRequestFilterParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

	private final PerunAdapter perunAdapter;
	private final FacilityAttrsConfig facilityAttrsConfig;

	public PerunAuthorizationFilter(PerunRequestFilterParams params) {
		super(params);
		BeanUtil beanUtil = params.getBeanUtil();
		this.perunAdapter = beanUtil.getBean(PerunAdapter.class);
		this.facilityAttrsConfig = beanUtil.getBean(FacilityAttrsConfig.class);
	}

	@Override
	protected boolean process(ServletRequest req, ServletResponse res, FilterParams params) {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		Facility facility = params.getFacility();
		if (facility == null) {
			log.info("Skipping filter because not able to find facility");
			return true;
		}

		PerunUser user = params.getUser();
		if (user == null) {
			log.info("Skipping filter because not able to extract user");
			return true;
		}

		return this.decideAccess(facility, user, request, response, params.getClientIdentifier(),
				perunAdapter, facilityAttrsConfig);
	}

	private boolean decideAccess(Facility facility, PerunUser user, HttpServletRequest request,
								 HttpServletResponse response, String clientIdentifier, PerunAdapter perunAdapter,
								 FacilityAttrsConfig facilityAttrsConfig) {
		Map<String, PerunAttributeValue> facilityAttributes = perunAdapter.getFacilityAttributeValues(
				facility, facilityAttrsConfig.getMembershipAttrNames());

		if (!facilityAttributes.get(facilityAttrsConfig.getCheckGroupMembershipAttr()).valueAsBoolean()) {
			log.debug("Membership check not requested, skipping filter");
			return true;
		}

		boolean canAccess = perunAdapter.canUserAccessBasedOnMembership(facility, user.getId());
		if (canAccess) {
			// allow access, continue with chain
			log.info("User allowed to access the service");
			return true;
		}
		FiltersUtils.redirectUserCannotAccess(request, response, facility, user, clientIdentifier, facilityAttrsConfig,
				facilityAttributes, perunAdapter);
		return false;
	}

}
