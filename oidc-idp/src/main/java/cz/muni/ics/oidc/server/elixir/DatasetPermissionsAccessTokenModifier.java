package cz.muni.ics.oidc.server.elixir;

import com.nimbusds.jwt.JWTClaimsSet;
import cz.muni.ics.oidc.server.PerunAccessTokenEnhancer;
import org.mitre.openid.connect.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * Implements adding EGA dataset permissions into signed JWT access tokens.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
@SuppressWarnings("unused")
public class DatasetPermissionsAccessTokenModifier implements PerunAccessTokenEnhancer.AccessTokenClaimsModifier {

	private final static Logger log = LoggerFactory.getLogger(DatasetPermissionsAccessTokenModifier.class);

	private static final String GA4GH = "ga4gh"; //Global Alliance for Genomics and Health
	private static final String GA4GH_USERINFO_CLAIMS = "ga4gh_userinfo_claims";

	public DatasetPermissionsAccessTokenModifier() {
	}

	@Override
	public void modifyClaims(String sub, JWTClaimsSet.Builder builder, OAuth2AccessToken accessToken, OAuth2Authentication authentication, UserInfo userInfo) {
		log.trace("modifyClaims(sub={})", sub);
		Set<String> scopes = accessToken.getScope();
		//GA4GH
		if (scopes.contains(GA4GH)) {
			log.debug("adding claims required by GA4GH to access token");
			builder.claim(GA4GH_USERINFO_CLAIMS, Arrays.asList("ga4gh.AffiliationAndRole", "ga4gh.ControlledAccessGrants", "ga4gh.AcceptedTermsAndPolicies", "ga4gh.ResearcherStatus"));
			builder.audience(Collections.singletonList(authentication.getOAuth2Request().getClientId()));
		}
	}

}
