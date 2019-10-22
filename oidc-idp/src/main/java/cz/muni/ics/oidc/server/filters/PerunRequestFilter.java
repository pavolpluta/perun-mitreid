package cz.muni.ics.oidc.server.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract class for Perun filters. All filters called in CallPerunFiltersFilter has to extend this.
 *
 * configuration:
 * filter.filterName.class=cz.muni.ics.oidc.server.filters.ClassName - Class the filter instantiates
 * filter.subs=sub1,sub2,... - execution of filter will be skipped if user SUB is in the list
 * filter.clientIds=cid1,cid2,... - execution of filter will be skipped if client_id is in the list
 *
 * @see cz.muni.ics.oidc.server.filters.impl package for specific filters and their configuration
 *
 * @author Dominik Baranek <0Baranek.dominik0@gmail.com>
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public abstract class PerunRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PerunRequestFilter.class);

    private static final String DELIMITER = ",";
    private static final String CLIENT_IDS = "clientIds";
    private static final String SUBS = "subs";

    private String filterName;
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
    protected abstract boolean process(ServletRequest request, ServletResponse response) throws IOException;

    public boolean doFilter(ServletRequest req, ServletResponse res) throws IOException {

        if (!skip((HttpServletRequest) req)) {
            log.debug("Executing filter: {}", filterName);
            return process(req, res);
        } else {
            log.debug("Filter: {} has been skipped", filterName);
            return true;
        }
    }

    private boolean skip(HttpServletRequest request) {
        String sub = request.getUserPrincipal().getName();
        String clientId = request.getParameter(PerunFilterConstants.CLIENT_ID);

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
