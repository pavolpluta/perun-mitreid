package cz.muni.ics.oidc.server.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.GenericFilterBean;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * This filter calls other Perun filters
 *
 * @author Dominik Baranek <0Baranek.dominik0@gmail.com>
 */
public class CallPerunFiltersFilter extends GenericFilterBean {

    public static final Logger log = LoggerFactory.getLogger(CallPerunFiltersFilter.class);
    private List<PerunRequestFilter> requestFilters;

    public List<PerunRequestFilter> getRequestFilters() {
        return requestFilters;
    }

    public void setRequestFilters(List<PerunRequestFilter> requestFilters) {
        this.requestFilters = requestFilters;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        for (PerunRequestFilter filter : this.requestFilters) {
            if(!filter.doFilter(servletRequest, servletResponse)) {
                return;
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
