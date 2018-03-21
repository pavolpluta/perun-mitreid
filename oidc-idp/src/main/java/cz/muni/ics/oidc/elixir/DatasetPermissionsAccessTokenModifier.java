package cz.muni.ics.oidc.elixir;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import cz.muni.ics.oidc.PerunTokenEnhancer;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

/**
 * Implements adding EGA dataset permissions into signed JWT access tokens.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
@SuppressWarnings("unused")
public class DatasetPermissionsAccessTokenModifier implements PerunTokenEnhancer.AccessTokenClaimsModifier {

    private final static Logger log = LoggerFactory.getLogger(DatasetPermissionsAccessTokenModifier.class);

    private static final String EGA_PROPERTIES = "/etc/perun/ega.properties";

    public DatasetPermissionsAccessTokenModifier() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(EGA_PROPERTIES));
            egaUrl = properties.getProperty("ega.url");
            egaUser = properties.getProperty("ega.user");
            egaPassword = properties.getProperty("ega.password");
            log.info("modifier initialized");
        } catch (IOException e) {
            log.error("cannot read file "+EGA_PROPERTIES+", will not modify access tokens");
        }

    }

    @Override
    public void modifyClaims(String sub, JWTClaimsSet.Builder builder, OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        if (accessToken.getScope().contains("permissions_ega")) {
            if(egaUrl==null) {
                log.error("not initialized properly for permissions_ega, giving up");
                return;
            }
            //get permissions data
            try {
                JsonNode result;
                //prepare basic auth
                RestTemplate rt = new RestTemplate();
                rt.setRequestFactory(
                        new InterceptingClientHttpRequestFactory( rt.getRequestFactory(),
                                Collections.singletonList(new BasicAuthorizationInterceptor(egaUser, egaPassword)))
                );
                String actionUrl = egaUrl + sub + "/";
                //make the call
                try {
                    log.trace("calling {}", actionUrl);
                    result = rt.getForObject(actionUrl, JsonNode.class);
                } catch (HttpClientErrorException ex) {
                    MediaType contentType = ex.getResponseHeaders().getContentType();
                    String body = ex.getResponseBodyAsString();
                    log.error("HTTP ERROR " + ex.getRawStatusCode() + " URL " + actionUrl + " Content-Type: " + contentType);
                    if(ex.getRawStatusCode()==404) {
                        log.warn("Got status 404 from EGA, ELIXIR AAI user is not linked to EGA user");
                        return;
                    }
                    if ("json".equals(contentType.getSubtype())) {
                        try {
                            log.error(new ObjectMapper().readValue(body, JsonNode.class).path("message").asText());
                        } catch (IOException e) {
                            log.error("cannot parse error message from JSON", e);
                        }
                    } else {
                        log.error("cannot make REST call", ex);
                    }
                    return;
                }
                JsonNode jsonNode = result;
                String rawJson = new ObjectMapper().writeValueAsString(jsonNode.path("permissions"));
                log.debug("EGA response: {}", rawJson);
                //add data
                JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
                Object perms = parser.parse(rawJson);
                builder.claim("permissions_ega", perms);
            } catch (ParseException | JsonProcessingException e) {
                log.error("cannot parse Permissions API response", e);
            } catch (Exception ex) {
                log.error("Cannot get EGA dataset permissions",ex);
            }
        }
    }

    private String egaUrl = null;
    private String egaUser = null;
    private String egaPassword = null;

}
