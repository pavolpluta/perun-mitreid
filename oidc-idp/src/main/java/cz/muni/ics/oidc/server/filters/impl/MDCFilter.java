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

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            HttpServletRequest req = (HttpServletRequest) servletRequest;
            if (req.getSession() != null) {
                MDC.put("sessionID", req.getSession().getId());
                log.debug("set sessionID {} for remote user {}", req.getSession().getId(), req.getRemoteUser());
            }
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            MDC.remove("sessionID");
        }
    }

}
