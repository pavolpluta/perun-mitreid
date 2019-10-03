package cz.muni.ics.oidc.server.filters;

/**
 * Class containing common constants used by Perun request filters.
 *
 * @author Dominik Baranek <0Baranek.dominik0@gmail.com>
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class PerunFilterConstants {

    static final String AUTHORIZE_REQ_PATTERN = "/authorize";
    static final String SHIB_IDENTITY_PROVIDER = "Shib-Identity-Provider";
    static final String SHIB_AUTHN_CONTEXT_CLASS = "Shib-AuthnContext-Class";
    static final String SHIB_AUTHN_CONTEXT_METHOD = "Shib-Authentication-Method";

}
