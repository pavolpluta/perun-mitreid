package cz.muni.ics.oidc.server.filters;

import cz.muni.ics.oidc.BeanUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.GenericFilterBean;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * This filter calls other Perun filters saved in the PerunFiltersContext
 *
 * @author Dominik Baranek <0Baranek.dominik0@gmail.com>
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class CallPerunFiltersFilter extends GenericFilterBean {

    public static final Logger log = LoggerFactory.getLogger(CallPerunFiltersFilter.class);

    @Autowired
    private Properties coreProperties;

    @Autowired
    private BeanUtil beanUtil;

    private PerunFiltersContext perunFiltersContext;

    @PostConstruct
    public void postConstruct() {
        this.perunFiltersContext = new PerunFiltersContext(coreProperties, beanUtil);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        List<PerunRequestFilter> filters = perunFiltersContext.getFilters();
        for (PerunRequestFilter filter : filters) {
            log.trace("Calling filter: {}", filter.getClass().getName());
            if(!filter.doFilter(servletRequest, servletResponse)) {
                return;
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
