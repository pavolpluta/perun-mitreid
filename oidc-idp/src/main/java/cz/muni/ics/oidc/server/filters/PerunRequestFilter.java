package cz.muni.ics.oidc.server.filters;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * Abstract class for Perun filters. All filters called in CallPerunFiltersFilter has to extend this.
 *
 * @author Dominik Baranek <0Baranek.dominik0@gmail.com>
 */
public abstract class PerunRequestFilter {

    /**
     * In this method is done whole logic of filer
     *
     * @param req request
     * @param res response
     * @return boolean if filter was successfully done
     * @throws IOException this exception could be thrown because of failed or interrupted I/O operation
     */
    public abstract boolean doFilter(ServletRequest req, ServletResponse res) throws IOException;
}
