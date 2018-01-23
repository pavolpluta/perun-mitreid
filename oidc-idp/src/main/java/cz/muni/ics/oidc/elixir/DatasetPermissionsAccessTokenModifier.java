package cz.muni.ics.oidc.elixir;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import cz.muni.ics.oidc.PerunAuthenticationFilter;
import cz.muni.ics.oidc.PerunTokenEnhancer;
import net.minidev.json.JSONArray;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class DatasetPermissionsAccessTokenModifier implements PerunTokenEnhancer.AccessTokenClaimsModifier{

    private final static Logger log = LoggerFactory.getLogger(DatasetPermissionsAccessTokenModifier.class);

    @Override
    public void modifyClaims(JWTClaimsSet.Builder builder, OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        if(accessToken.getScope().contains("permissions_ega")) {
            //get permissions data

            try {
                JsonNode jsonNode = makeRestCall("?affiliation=member@csc.fi");
                String rawJson = new ObjectMapper().writeValueAsString(jsonNode.path("permissions"));
                log.debug("EGA response: {}",rawJson);
                //add data
                JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
                Object perms = parser.parse(rawJson);
                builder.claim("permissions_ega", perms);
            } catch (ParseException | JsonProcessingException e) {
                log.error("cannot parse Permissions API response",e);
            }

        }
    }

    private String egaUrl = "http://193.167.189.108:8080/user/jack/";
    private String egaUser = "user";
    private String egaPassword = "password";

    private JsonNode makeRestCall(String urlPart) {
        //prepare basic auth
        RestTemplate restTemplate = new RestTemplate();
//        List<ClientHttpRequestInterceptor> interceptors =
//                Collections.singletonList(new BasicAuthorizationInterceptor(egaUser, egaPassword));
//        restTemplate.setRequestFactory(new InterceptingClientHttpRequestFactory(restTemplate.getRequestFactory(), interceptors));
        String actionUrl = egaUrl + urlPart;
        //make the call
        try {
            log.trace("calling {}",actionUrl);
            return restTemplate.getForObject(actionUrl, JsonNode.class);
        } catch (HttpClientErrorException ex) {
            MediaType contentType = ex.getResponseHeaders().getContentType();
            String body = ex.getResponseBodyAsString();
            log.error("HTTP ERROR " + ex.getRawStatusCode() + " URL " + actionUrl + " Content-Type: " + contentType);
            if ("json".equals(contentType.getSubtype())) {
                try {
                    log.error(new ObjectMapper().readValue(body, JsonNode.class).path("message").asText());
                } catch (IOException e) {
                    log.error("cannot parse error message from JSON",e);
                }
            } else {
                log.error(ex.getMessage());
            }
            throw new RuntimeException("cannot make REST call",ex);
        }
    }
}
