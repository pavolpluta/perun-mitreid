package cz.muni.ics.oidc.elixir;

import com.nimbusds.jwt.JWTClaimsSet;
import cz.muni.ics.oidc.PerunTokenEnhancer;
import net.minidev.json.JSONArray;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class DatasetPermissionsAccessTokenModifier implements PerunTokenEnhancer.AccessTokenClaimsModifier{
    @Override
    public void modifyClaims(JWTClaimsSet.Builder builder, OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        if(accessToken.getScope().contains("datasets_permissions")) {
            //get permissions data
            //TODO read real permissions data
            JSONArray perms = new JSONArray();
            Map<String, Object> p1 = new LinkedHashMap<>();
            perms.add(p1);
            p1.put("affiliation", null);
            List<String> ds = new ArrayList<>();
            p1.put("datasets", ds);
            ds.add("https://www.ebi.ac.uk/ega/datasets/EGAD00000000053");
            ds.add("https://www.ebi.ac.uk/ega/datasets/EGAD00000000054");

            builder.claim("permissions", perms);
        }
    }
}
