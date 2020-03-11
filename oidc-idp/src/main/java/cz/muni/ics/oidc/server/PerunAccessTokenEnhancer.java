package cz.muni.ics.oidc.server;

import com.google.common.base.Joiner;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;

import java.util.Date;
import java.util.UUID;

import static org.mitre.oauth2.service.IntrospectionResultAssembler.SCOPE;
import static org.mitre.oauth2.service.IntrospectionResultAssembler.SCOPE_SEPARATOR;

/**
 * Copy of ConnectTokenEnhancer.
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class PerunAccessTokenEnhancer implements TokenEnhancer {

    private final static Logger log = LoggerFactory.getLogger(PerunAccessTokenEnhancer.class);


    @Autowired
    private ConfigurationPropertiesBean configBean;

    @Autowired
    private JWTSigningAndValidationService jwtService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private ClientDetailsEntityService clientService;

    @Autowired
    private UserInfoService userInfoService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
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
        String sub = userInfo != null ? userInfo.getSub() : authentication.getName();
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .claim("azp", clientId)
                .issuer(configBean.getIssuer())
                .issueTime(iat)
                .expirationTime(token.getExpiration())
                .subject(sub)
                .claim(SCOPE, Joiner.on(SCOPE_SEPARATOR).join(accessToken.getScope()))
                .jwtID(UUID.randomUUID().toString()); // set a random NONCE in the middle of it
        accessTokenClaimsHook(sub, builder, accessToken, authentication, userInfo);

        String audience = (String) authentication.getOAuth2Request().getExtensions().get("aud");
        if (!Strings.isNullOrEmpty(audience)) {
            builder.audience(Lists.newArrayList(audience));
        }

        JWTClaimsSet claims = builder.build();

        JWSAlgorithm signingAlg = jwtService.getDefaultSigningAlgorithm();
        JWSHeader header = new JWSHeader(signingAlg, null, null, null, null, null, null, null, null, null,
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

    private void accessTokenClaimsHook(String sub, JWTClaimsSet.Builder builder, OAuth2AccessToken accessToken, OAuth2Authentication authentication, UserInfo userInfo) {
        if (accessTokenClaimsModifier != null) {
            log.trace("calling accessTokenClaimsHook() on {}",accessTokenClaimsModifier.getClass().getName());
            accessTokenClaimsModifier.modifyClaims(sub, builder, accessToken, authentication, userInfo);
        }
    }

    @FunctionalInterface
    public interface AccessTokenClaimsModifier {
        void modifyClaims(String sub, JWTClaimsSet.Builder builder, OAuth2AccessToken accessToken, OAuth2Authentication authentication, UserInfo userInfo);
    }

    public static class NoOpAccessTokenClaimsModifier implements AccessTokenClaimsModifier {

        @Override
        public void modifyClaims(String sub, JWTClaimsSet.Builder builder, OAuth2AccessToken accessToken, OAuth2Authentication authentication, UserInfo userInfo) {
            log.debug("no modification");
        }
    }
}
