package cz.muni.ics.oidc.server.filters;

import cz.muni.ics.oidc.BeanUtil;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.server.adapters.PerunAdapter;
import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * This filter calls other Perun filters saved in the PerunFiltersContext
 *
 * @author Dominik Baranek <baranek@ics.muni.cz>
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class CallPerunFiltersFilter extends GenericFilterBean {

    public static final Logger log = LoggerFactory.getLogger(CallPerunFiltersFilter.class);

    @Autowired
    private Properties coreProperties;

    @Autowired
    private BeanUtil beanUtil;

    @Autowired
    private OAuth2RequestFactory authRequestFactory;

    @Autowired
    private ClientDetailsEntityService clientDetailsEntityService;

    @Autowired
    private PerunOidcConfig perunOidcConfig;

    @Autowired
    private PerunAdapter perunAdapter;

    private PerunFiltersContext perunFiltersContext;

    @PostConstruct
    public void postConstruct() {
        this.perunFiltersContext = new PerunFiltersContext(coreProperties, beanUtil);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException
    {
        List<PerunRequestFilter> filters = perunFiltersContext.getFilters();
        if (filters != null && !filters.isEmpty()) {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            ClientDetailsEntity client = FiltersUtils.extractClient(request, authRequestFactory,
                    clientDetailsEntityService);
            Facility facility = null;
            if (client != null && StringUtils.hasText(client.getClientId())) {
                try {
                    facility = perunAdapter.getFacilityByClientId(client.getClientId());
                } catch (Exception e) {
                    log.warn("Could not fetch facility for client_id {}", client.getClientId(), e);
                    facility = null;
                }
            }
            PerunUser user = FiltersUtils.getPerunUser(request, perunOidcConfig, perunAdapter);

            FilterParams params = new FilterParams(client, facility, user);
            for (PerunRequestFilter filter : filters) {
                log.debug("Calling filter: {}", filter.getClass().getName());
                if (!filter.doFilter(servletRequest, servletResponse, params)) {
                    return;
                }
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
