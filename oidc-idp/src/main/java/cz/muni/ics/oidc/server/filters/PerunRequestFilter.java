package cz.muni.ics.oidc.server.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static cz.muni.ics.oidc.server.filters.PerunFilterConstants.AUTHORIZE_REQ_PATTERN;

/**
 * Abstract class for Perun filters. All filters called in CallPerunFiltersFilter has to extend this.
 *
 * Configuration of filter names:
 * <ul>
 *     <li><b>filter.names</b> - comma separated list of names of the request filters</li>
 * </ul>
 *
 * Configuration of filter (replace [name] part with the name defined for the filter):
 * <ul>
 *     <li><b>filter.[name].class</b> - Class the filter instantiates</li>
 *     <li><b>filter.[name].subs</b> - comma separated list of sub values for which execution of filter will be skipped
 *         if user's SUB is in the list</li>
 *     <li><b>filter.[name].clientIds</b> - comma separated list of client_id values for which execution of filter
 *         will be skipped if client_id is in the list</li>
 * </ul>
 *
 * @see cz.muni.ics.oidc.server.filters.impl package for specific filters and their configuration
 *
 * @author Dominik Baranek <baranek@ics.muni.cz>
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public abstract class PerunRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PerunRequestFilter.class);

    private static final String DELIMITER = ",";
    private static final String CLIENT_IDS = "clientIds";
    private static final String SUBS = "subs";

    private static final RequestMatcher requestMatcher = new AntPathRequestMatcher(AUTHORIZE_REQ_PATTERN);

    private final String filterName;
    private Set<String> clientIds = new HashSet<>();
    private Set<String> subs = new HashSet<>();

    public PerunRequestFilter(PerunRequestFilterParams params) {
        filterName = params.getFilterName();

        if (params.hasProperty(CLIENT_IDS)) {
            this.clientIds = new HashSet<>(Arrays.asList(params.getProperty(CLIENT_IDS).split(DELIMITER)));
        }

        if (params.hasProperty(SUBS)) {
            this.subs = new HashSet<>(Arrays.asList(params.getProperty(SUBS).split(DELIMITER)));
        }

        log.debug("Filter name: {}", filterName);
        log.debug("Skip for SUBS: {}", subs);
        log.debug("Skip for CLIENT_IDS: {}", clientIds);
    }

    /**
     * In this method is done whole logic of filer
     *
     * @param request request
     * @param response response
     * @return boolean if filter was successfully done
     * @throws IOException this exception could be thrown because of failed or interrupted I/O operation
     */
    protected abstract boolean process(ServletRequest request, ServletResponse response, FilterParams params)
            throws IOException;

    public boolean doFilter(ServletRequest req, ServletResponse res, FilterParams params) throws IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        // skip everything that's not an authorize URL
        if (!requestMatcher.matches(request)) {
            log.debug("Filter: {} has been skipped, did not match /authorize", filterName);
            return true;
        }
        if (!skip(request)) {
            log.debug("Executing filter: {}", filterName);
            return this.process(req, res, params);
        } else {
            log.debug("Filter: {} has been skipped", filterName);
            return true;
        }
    }

    private boolean skip(HttpServletRequest request) {
        String sub = request.getUserPrincipal().getName();
        String clientId = request.getParameter(PerunFilterConstants.PARAM_CLIENT_ID);

        if (sub != null && subs.contains(sub)) {
            log.debug("Skipping filter {} because of sub {}", filterName, sub);
            return true;
        } else if (clientId != null && clientIds.contains(clientId)){
            log.debug("Skipping filter {} because of client_id {}", filterName, clientId);
            return true;
        }

        return false;
    }
}
