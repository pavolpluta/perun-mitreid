package cz.muni.ics.oidc.server.filters.impl;

import cz.muni.ics.oidc.BeanUtil;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.server.adapters.PerunAdapter;
import cz.muni.ics.oidc.server.filters.*;
import cz.muni.ics.oidc.web.controllers.ControllerUtils;
import cz.muni.ics.oidc.web.controllers.IsTestSpController;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static cz.muni.ics.oidc.server.filters.PerunFilterConstants.PARAM_TARGET;
import static cz.muni.ics.oidc.web.controllers.IsTestSpController.IS_TEST_SP_APPROVED_SESS;

public class PerunIsTestSpFilter extends PerunRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PerunIsTestSpFilter.class);

    private static final String IS_TEST_SP_ATTR_NAME = "isTestSpAttr";

    private final String isTestSpAttrName;
    private final PerunAdapter perunAdapter;
    private final String filterName;

    public PerunIsTestSpFilter(PerunRequestFilterParams params) {
        super(params);
        BeanUtil beanUtil = params.getBeanUtil();
        this.perunAdapter = beanUtil.getBean(PerunAdapter.class);
        this.isTestSpAttrName = params.getProperty(IS_TEST_SP_ATTR_NAME);
        this.filterName = params.getFilterName();
    }

    @Override
    protected boolean process(ServletRequest req, ServletResponse res, FilterParams params) throws IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        log.error("================================");
        Facility facility = params.getFacility();
        if (facility == null || facility.getId() == null) {
            log.debug("{} - skip execution: no facility provider", filterName);
            return true;
        }


        //boolean isTestSp = facilityAttributes.get(facilityAttrsConfig.getTestSpAttr()).valueAsBoolean();

        if (testSpWarningApproved(request)){
            log.debug("{} - Warning already approved, continue to the next filter", filterName);
            return true;
        }

        PerunAttributeValue attrValue = perunAdapter.getFacilityAttributeValue(facility.getId(), isTestSpAttrName);

        log.error("ATTR VALUE: {}", attrValue);
        log.error("==============================");
        if (attrValue != null && attrValue.valueAsBoolean()) {
            log.debug("Redirecting user to test SP warning page");
            this.redirect(request, response);
            return false;
        }

        return true;

    }


    private boolean testSpWarningApproved(HttpServletRequest req) {
        if (req.getSession() == null) {
            return false;
        }
        boolean approved = false;
        if (req.getSession().getAttribute(IS_TEST_SP_APPROVED_SESS) != null) {
            approved = (Boolean) req.getSession().getAttribute(IS_TEST_SP_APPROVED_SESS);
            req.getSession().removeAttribute(IS_TEST_SP_APPROVED_SESS);
        }
        return approved;
    }

    private void redirect(HttpServletRequest req, HttpServletResponse res) {
        String targetURL = FiltersUtils.buildRequestURL(req);

        Map<String, String> params = new HashMap<>();
        params.put(PARAM_TARGET, targetURL);
        String redirectUrl = ControllerUtils.createRedirectUrl(req, PerunFilterConstants.AUTHORIZE_REQ_PATTERN,
                IsTestSpController.WARNING_MAPPING, params);
        log.debug("{} - redirecting user to testSP warning page: {}", filterName, redirectUrl);
        res.reset();
        res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        res.setHeader(HttpHeaders.LOCATION, redirectUrl);
    }

}
