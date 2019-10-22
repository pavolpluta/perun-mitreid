package cz.muni.ics.oidc.server.filters;

/**
 * Class containing common constants used by Perun request filters.
 *
 * @author Dominik Baranek <0Baranek.dominik0@gmail.com>
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class PerunFilterConstants {

    public static final String AUTHORIZE_REQ_PATTERN = "/authorize";
    public static final String SHIB_IDENTITY_PROVIDER = "Shib-Identity-Provider";
    public static final String SHIB_AUTHN_CONTEXT_CLASS = "Shib-AuthnContext-Class";
    public static final String SHIB_AUTHN_CONTEXT_METHOD = "Shib-Authentication-Method";

    public static final String CLIENT_ID = "client_id";

}
