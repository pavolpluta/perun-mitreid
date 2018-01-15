package cz.muni.ics.oidc;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.mitre.jwt.signer.service.JWTSigningAndValidationService;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.model.OAuth2AccessTokenEntity;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.mitre.oauth2.service.SystemScopeService;
import org.mitre.openid.connect.config.ConfigurationPropertiesBean;
import org.mitre.openid.connect.model.UserInfo;
import org.mitre.openid.connect.service.OIDCTokenService;
import org.mitre.openid.connect.service.UserInfoService;
import org.mitre.openid.connect.web.JWKSetPublishingEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

/**
 * Copy of ConnectTokenEnhancer.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunTokenEnhancer implements TokenEnhancer {

    private final static Logger log = LoggerFactory.getLogger(PerunTokenEnhancer.class);


    @Autowired
    private ConfigurationPropertiesBean configBean;

    @Autowired
    private JWTSigningAndValidationService jwtService;

    @Autowired
    private ClientDetailsEntityService clientService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private OIDCTokenService connectTokenService;

    /**
     * Exact copy from ConnectTokenEnhancer with added hooks.
     */
    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        Date iat = new Date();
        OAuth2AccessTokenEntity token = (OAuth2AccessTokenEntity) accessToken;
        OAuth2Request originalAuthRequest = authentication.getOAuth2Request();

        String clientId = originalAuthRequest.getClientId();
        ClientDetailsEntity client = clientService.loadClientByClientId(clientId);

        UserInfo userInfo = null;
        if (originalAuthRequest.getScope().contains(SystemScopeService.OPENID_SCOPE)
                && !authentication.isClientOnly()) {
            userInfo = userInfoService.getByUsernameAndClientId(authentication.getName(), clientId);
        }

        // create signed access token
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .claim("azp", clientId)
                .issuer(configBean.getIssuer())
                .issueTime(iat)
                .expirationTime(token.getExpiration())
                .subject(userInfo != null ? userInfo.getSub() : authentication.getName())
                .jwtID(UUID.randomUUID().toString()); // set a random NONCE in the middle of it
        accessTokenClaimsHook(builder, accessToken, authentication);

        String audience = (String) authentication.getOAuth2Request().getExtensions().get("aud");
        if (!Strings.isNullOrEmpty(audience)) {
            builder.audience(Lists.newArrayList(audience));
        }

        JWTClaimsSet claims = builder.build();

        JWSAlgorithm signingAlg = jwtService.getDefaultSigningAlgorithm();
        URI jku = URI.create(configBean.getIssuer() + JWKSetPublishingEndpoint.URL);
        JWSHeader header = new JWSHeader(signingAlg, null, null, null, jku, null, null, null, null, null,
                jwtService.getDefaultSignerKeyId(),
                null, null);
        SignedJWT signed = new SignedJWT(header, claims);

        jwtService.signJwt(signed);
        token.setJwt(signed);

        if (userInfo != null) {
            //needs access token
            JWT idToken = connectTokenService.createIdToken(client, originalAuthRequest, iat, userInfo.getSub(), token);
            // attach the id token to the parent access token
            token.setIdToken(idToken);
            if (log.isDebugEnabled()) log.debug("idToken: {}", idToken.serialize());
        } else {
            // can't create an id token if we can't find the user
            log.warn("Request for ID token when no user is present.");
        }


        logHook(token, authentication);
        return token;
    }

    private void logHook(OAuth2AccessTokenEntity token, OAuth2Authentication authentication) {
        //log request info from authentication
        log.trace("enhance(accessToken={},authentication={})", token, authentication);
        Object principal = authentication.getPrincipal();
        String userId = principal instanceof User ? ((User) principal).getUsername() : principal.toString();
        OAuth2Request oAuth2Request = authentication.getOAuth2Request();
        log.info("userId: {}, clientId: {}, grantType: {}, redirectUri: {}, scopes: {}",
                userId, oAuth2Request.getClientId(), oAuth2Request.getGrantType(), oAuth2Request.getRedirectUri(), token.getScope());
        if (log.isDebugEnabled()) log.debug("access token: {}", token.getValue());
    }

    private AccessTokenClaimsModifier accessTokenClaimsModifier;

    public void setAccessTokenClaimsModifier(AccessTokenClaimsModifier accessTokenClaimsModifier) {
        this.accessTokenClaimsModifier = accessTokenClaimsModifier;
    }

    private void accessTokenClaimsHook(JWTClaimsSet.Builder builder, OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        if (accessTokenClaimsModifier != null) {
            accessTokenClaimsModifier.modifyClaims(builder, accessToken, authentication);
        }
    }

    @FunctionalInterface
    public interface AccessTokenClaimsModifier {
        void modifyClaims(JWTClaimsSet.Builder builder, OAuth2AccessToken accessToken, OAuth2Authentication authentication);
    }

    public static class NoOpAccessTokenClaimsModifier implements AccessTokenClaimsModifier {

        @Override
        public void modifyClaims(JWTClaimsSet.Builder builder, OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
            log.debug("no modification");
        }
    }
}
