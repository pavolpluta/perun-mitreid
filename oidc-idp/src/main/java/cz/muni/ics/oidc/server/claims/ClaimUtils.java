package cz.muni.ics.oidc.server.claims;

import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;
import org.springframework.util.StringUtils;

public class ClaimUtils {

    public static final String NO_VALUE = null;

    public static boolean isPropSetAndHasNonNullAttribute(String propertyName, ClaimSourceProduceContext ctx) {
        return isPropSetAndHasAttribute(propertyName, ctx)
                && ctx.getAttrValues().get(propertyName) != null;
    }

    public static boolean isPropSetAndHasAttribute(String propertyName, ClaimSourceProduceContext ctx) {
        return isPropSet(propertyName) && ctx.getAttrValues().containsKey(propertyName);
    }

    public static boolean isPropSet(String propertyName) {
        return StringUtils.hasText(propertyName);
    }

    public static String fillStringPropertyOrNoVal(String suffix, ClaimSourceInitContext ctx) {
        String prop = ctx.getProperty(suffix, NO_VALUE);
        if (!StringUtils.hasText(prop)) {
            return prop;
        } else {
            return NO_VALUE;
        }
    }
}
