package cz.muni.ics.oidc.server.filters.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class MDCFilter extends GenericFilterBean {

    public static final Logger log = LoggerFactory.getLogger(MDCFilter.class);
    private static final int SIZE = 12;
    private static final String SESSION_ID = "sessionID";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            HttpServletRequest req = (HttpServletRequest) servletRequest;
            if (req.getSession() != null) {
                String id = req.getSession().getId();
                if (id != null && id.length() > SIZE) {
                    id = id.substring(0, SIZE);
                }
                log.debug("set sessionID {} for remote user {}", id, req.getRemoteUser());
                MDC.put(SESSION_ID, id);
            }
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            MDC.remove(SESSION_ID);
        }
    }

}
