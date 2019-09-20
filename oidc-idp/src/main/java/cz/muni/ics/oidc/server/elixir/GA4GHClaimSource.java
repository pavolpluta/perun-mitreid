package cz.muni.ics.oidc.server.elixir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import cz.muni.ics.oidc.models.PerunAttribute;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;
import cz.muni.ics.oidc.server.connectors.Affiliation;
import org.mitre.jwt.signer.service.JWTSigningAndValidationService;
import org.mitre.openid.connect.web.JWKSetPublishingEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Class producing GA4GH claims. The claim is specified in
 * https://docs.google.com/document/d/11Wg-uL75ypU5eNu2p_xh9gspmbGtmLzmdq5VfPHBirE/edi
 */
@SuppressWarnings("unused")
public class GA4GHClaimSource extends ClaimSource {

	public static final String GA4GH_SCOPE = "ga4gh_passport_v1";

	private static final Logger log = LoggerFactory.getLogger(GA4GHClaimSource.class);

	private static final String BONA_FIDE_URL = "https://doi.org/10.1038/s41431-018-0219-y";
	private static final String ELIXIR_ORG_URL = "https://elixir-europe.org/";

	private RestTemplate remsRestTemplate;
	private String remsUrl;
	private RestTemplate egaRestTemplate;
	private String egaUrl;
	private final JWTSigningAndValidationService jwtService;
	private final URI jku;
	private final String issuer;

	public GA4GHClaimSource(ClaimSourceInitContext ctx) throws URISyntaxException {
		super(ctx);
		//remember context
		jwtService = ctx.getJwtService();
		issuer = ctx.getPerunOidcConfig().getConfigBean().getIssuer();
		jku = new URI(issuer + JWKSetPublishingEndpoint.URL);
		//prepare remsRestTemplate for calling REMS
		remsUrl = ctx.getProperty("rems.url", null);
		String remsHeader = ctx.getProperty("rems.header", null);
		String remsHeaderValue = ctx.getProperty("rems.key", null);
		if (remsUrl == null || remsHeader == null || remsHeaderValue == null) {
			log.warn("REMS not configured, will not read its permissions!");
		} else {
			remsRestTemplate = new RestTemplate();
			remsRestTemplate.setRequestFactory(
					new InterceptingClientHttpRequestFactory(remsRestTemplate.getRequestFactory(),
							Collections.singletonList(new AddHeaderInterceptor(remsHeader, remsHeaderValue)))
			);
			log.info("REMS Permissions API configured at {}", remsUrl);
		}
		//prepare egaRestTemplate for calling EGA
		egaUrl = ctx.getProperty("ega.url", null);
		String egaUser = ctx.getProperty("ega.user", null);
		String egaPassword = ctx.getProperty("ega.password", null);
		if (egaUrl == null || egaUser == null || egaPassword == null) {
			log.warn("EGA not configured, will not read its permissions!");
		} else {
			egaRestTemplate = new RestTemplate();
			egaRestTemplate.setRequestFactory(
					new InterceptingClientHttpRequestFactory(egaRestTemplate.getRequestFactory(),
							Collections.singletonList(new BasicAuthorizationInterceptor(egaUser, egaPassword)))
			);
			log.info("EGA Permissions API configured at {}", egaUrl);
		}
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {
		log.trace("produceValue(user={})", pctx.getPerunUserId());

		if (pctx.getClient() == null) {
			log.debug("client is not set");
			return JsonNodeFactory.instance.textNode("Global Alliance For Genomic Health structured claim");
		}
		if (!pctx.getClient().getScope().contains(GA4GH_SCOPE)) {
			log.debug("Client '{}' does not have scope ga4gh", pctx.getClient().getClientName());
			return null;
		}

		List<Affiliation> affiliations = pctx.getPerunConnector().getUserExtSourcesAffiliations(pctx.getPerunUserId());

		ArrayNode ga4gh_passport_v1 = JsonNodeFactory.instance.arrayNode();
		addAffiliationAndRoles(pctx, ga4gh_passport_v1, affiliations);
		addAcceptedTermsAndPolicies(pctx, ga4gh_passport_v1);
		addResearcherStatuses(pctx, ga4gh_passport_v1, affiliations);
		addControlledAccessGrants(pctx, ga4gh_passport_v1);
		addLinkedIdentities(pctx, ga4gh_passport_v1);
		return ga4gh_passport_v1;
	}


	private void addAffiliationAndRoles(ClaimSourceProduceContext pctx, ArrayNode passport, List<Affiliation> affiliations) {
		//by=system for users with affiliation asserted by their IdP (set in UserExtSource attribute "affiliation")
		for (Affiliation affiliation : affiliations) {
			//expires 1 year after the last login from the IdP asserting the affiliation
			long expires = Instant.ofEpochSecond(affiliation.getAsserted()).atZone(ZoneId.systemDefault()).plusYears(1L).toEpochSecond();
			passport.add(createPassportVisa("AffiliationAndRole", pctx, affiliation.getValue(), affiliation.getSource(), "system", affiliation.getAsserted(), expires, null));
		}
	}

	private void addAcceptedTermsAndPolicies(ClaimSourceProduceContext pctx, ArrayNode passport) {
		//by=self for members of the group 10432 "Bona Fide Researchers"
		boolean userInGroup = pctx.getPerunConnector().isUserInGroup(pctx.getPerunUserId(), 10432L);
		if (userInGroup) {
			PerunAttribute bonaFideStatus = pctx.getPerunConnector().getUserAttribute(pctx.getPerunUserId(), "urn:perun:user:attribute-def:def:bonaFideStatus");
			String valueCreatedAt = bonaFideStatus.getValueCreatedAt();
			long asserted;
			if (valueCreatedAt != null) {
				asserted = Timestamp.valueOf(valueCreatedAt).getTime() / 1000L;
			} else {
				asserted = System.currentTimeMillis() / 1000L;
			}
			long expires = Instant.ofEpochSecond(asserted).atZone(ZoneId.systemDefault()).plusYears(100L).toEpochSecond();
			passport.add(createPassportVisa("AcceptedTermsAndPolicies", pctx, BONA_FIDE_URL, ELIXIR_ORG_URL, "self", asserted, expires, null));
		}
	}

	private void addResearcherStatuses(ClaimSourceProduceContext pctx, ArrayNode passport, List<Affiliation> affiliations) {
		//by=peer for users with attribute elixirBonaFideStatusREMS
		PerunAttribute elixirBonaFideStatusREMS = pctx.getPerunConnector().getUserAttribute(pctx.getPerunUserId(), "urn:perun:user:attribute-def:def:elixirBonaFideStatusREMS");
		String valueCreatedAt = elixirBonaFideStatusREMS.getValueCreatedAt();
		if (valueCreatedAt != null) {
			long asserted = Timestamp.valueOf(valueCreatedAt).getTime() / 1000L;
			long expires = ZonedDateTime.now().plusYears(1L).toEpochSecond();
			passport.add(createPassportVisa("ResearcherStatus", pctx, BONA_FIDE_URL, ELIXIR_ORG_URL, "peer", asserted, expires, null));
		}
		//by=system for users with faculty affiliation asserted by their IdP (set in UserExtSource attribute "affiliation")
		for (Affiliation affiliation : affiliations) {
			if (affiliation.getValue().startsWith("faculty@")) {
				long expires = Instant.ofEpochSecond(affiliation.getAsserted()).atZone(ZoneId.systemDefault()).plusYears(1L).toEpochSecond();
				passport.add(createPassportVisa("ResearcherStatus", pctx, BONA_FIDE_URL, affiliation.getSource(), "system", affiliation.getAsserted(), expires, null));
			}
		}
		//by=so for users with faculty affiliation asserted by membership in a group with groupAffiliations attribute
		for (Affiliation affiliation : pctx.getPerunConnector().getGroupAffiliations(pctx.getPerunUserId())) {
			if (affiliation.getValue().startsWith("faculty@")) {
				long expires = ZonedDateTime.now().plusYears(1L).toEpochSecond();
				passport.add(createPassportVisa("ResearcherStatus", pctx, BONA_FIDE_URL, ELIXIR_ORG_URL, "so", affiliation.getAsserted(), expires, null));
			}
		}
	}

	private void addLinkedIdentities(ClaimSourceProduceContext pctx, ArrayNode passport) {
		long now = Instant.now().getEpochSecond();
		passport.add(createPassportVisa("LinkedIdentities", pctx, pctx.getSub() + "|" + issuer, issuer, "system", now, now + 3600L, null));
	}

	private String isoDate(long linuxTime) {
		return DateTimeFormatter.ISO_LOCAL_DATE.format(ZonedDateTime.ofInstant(Instant.ofEpochSecond(linuxTime), ZoneId.systemDefault()));
	}

	private JsonNode createPassportVisa(String type, ClaimSourceProduceContext pctx, String value, String source, String by, long asserted, long expires, JsonNode condition) {

		Map<String, Object> passportVisaObject = new HashMap<>();
		passportVisaObject.put("type", type);
		passportVisaObject.put("asserted", asserted);
		passportVisaObject.put("value", value);
		passportVisaObject.put("source", source);
		passportVisaObject.put("by", by);
		if (condition != null && !condition.isNull() && !condition.isMissingNode()) {
			passportVisaObject.put("condition", condition);
		}
		JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.parse(jwtService.getDefaultSigningAlgorithm().getName()))
				.keyID(jwtService.getDefaultSignerKeyId())
				.type(JOSEObjectType.JWT)
				.jwkURL(jku)
				.build();
		JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
				.issuer(issuer)
				.issueTime(new Date())
				.expirationTime(new Date(expires * 1000L))
				.subject(pctx.getSub())
				.jwtID(UUID.randomUUID().toString())
				.claim("ga4gh_visa_v1", passportVisaObject)
				.build();
		SignedJWT myToken = new SignedJWT(jwsHeader, jwtClaimsSet);
		jwtService.signJwt(myToken);
		return JsonNodeFactory.instance.textNode(myToken.serialize());
	}

	private void addControlledAccessGrants(ClaimSourceProduceContext pctx, ArrayNode passport) {
		if (remsRestTemplate != null) {
			callPermissionsAPI(remsRestTemplate, remsUrl + pctx.getSub(), pctx, passport);
		}
		if (egaRestTemplate != null) {
			callPermissionsAPI(egaRestTemplate, egaUrl + pctx.getSub() + "/", pctx, passport);
		}
	}

	private void callPermissionsAPI(RestTemplate restTemplate, String actionURL, ClaimSourceProduceContext pctx, ArrayNode passport) {
		JsonNode permissions = getPermissions(restTemplate, actionURL);
		if (permissions != null) {
			JsonNode grants = permissions.path("ga4gh").path("ControlledAccessGrants");
			if (grants.isArray()) {
				for (JsonNode grant : grants) {
					String value = grant.path("value").asText();
					String source = grant.path("source").asText();
					String by = grant.path("by").asText();
					long asserted = grant.path("asserted").asLong();
					long expires = grant.path("expires").asLong();
					JsonNode condition = grant.get("condition");
					passport.add(createPassportVisa("ControlledAccessGrants", pctx, value, source, by, asserted, expires, condition));
				}
			} else {
				log.warn("permissions is not an array in {}", permissions);
			}
		}
	}

	@SuppressWarnings("Duplicates")
	private static JsonNode getPermissions(RestTemplate rt, String actionUrl) {
		//get permissions data
		try {
			JsonNode result;
			//make the call
			try {
				log.debug("calling Permissions API at {}", actionUrl);
				result = rt.getForObject(actionUrl, JsonNode.class);
			} catch (HttpClientErrorException ex) {
				MediaType contentType = ex.getResponseHeaders().getContentType();
				String body = ex.getResponseBodyAsString();
				log.error("HTTP ERROR " + ex.getRawStatusCode() + " URL " + actionUrl + " Content-Type: " + contentType);
				if (ex.getRawStatusCode() == 404) {
					log.warn("Got status 404 from Permissions endpoint {}, ELIXIR AAI user is not linked to user at Permissions API", actionUrl);
					return null;
				}
				if ("json".equals(contentType.getSubtype())) {
					try {
						log.error(new ObjectMapper().readValue(body, JsonNode.class).path("message").asText());
					} catch (IOException e) {
						log.error("cannot parse error message from JSON", e);
					}
				} else {
					log.error("cannot make REST call, exception: {} message: {}", ex.getClass().getName(), ex.getMessage());
				}
				return null;
			}
			log.debug("Permissions API response: {}", result);
			return result;
		} catch (Exception ex) {
			log.error("Cannot get dataset permissions", ex);
		}
		return null;
	}


}
