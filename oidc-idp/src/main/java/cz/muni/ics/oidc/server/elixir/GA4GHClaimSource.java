package cz.muni.ics.oidc.server.elixir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import cz.muni.ics.oidc.models.PerunAttribute;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;
import cz.muni.ics.oidc.server.connectors.Affiliation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * Class producing GA4GH claims. The claim is specified in
 * https://docs.google.com/document/d/11Wg-uL75ypU5eNu2p_xh9gspmbGtmLzmdq5VfPHBirE/edi
 */
@SuppressWarnings("unused")
public class GA4GHClaimSource extends ClaimSource {

	private static final Logger log = LoggerFactory.getLogger(GA4GHClaimSource.class);

	private static final String BONA_FIDE_URL = "https://doi.org/10.1038/s41431-018-0219-y";
	private static final String NO_ORG_URL = "https://ga4gh.org/duri/no_org";

	private RestTemplate remsRestTemplate;
	private String remsUrl;
	private RestTemplate egaRestTemplate;
	private String egaUrl;

	public GA4GHClaimSource(ClaimSourceInitContext ctx) {
		super(ctx);
		//REMS
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
		//EGA
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
		if (!pctx.getClient().getScope().contains("ga4gh")) {
			log.debug("Client '{}' does not have scope ga4gh", pctx.getClient().getClientName());
			return null;
		}

		ObjectNode ga4gh = JsonNodeFactory.instance.objectNode();

		List<Affiliation> affiliations = pctx.getPerunConnector().getUserExtSourcesAffiliations(pctx.getPerunUserId());

		ArrayNode affiliationAndRole = ga4gh.arrayNode();
		JsonNode affDesc = addAffiliationAndRoles(pctx, affiliationAndRole, affiliations);
//		ga4gh.set("AffiliationAndRole.description", affDesc);
		ga4gh.set("AffiliationAndRole", affiliationAndRole);

		ArrayNode acceptedTermsAndPolicies = ga4gh.arrayNode();
		TextNode termsDesc = addAcceptedTermsAndPolicies(pctx, acceptedTermsAndPolicies);
//		ga4gh.set("AcceptedTermsAndPolicies.description", termsDesc);
		ga4gh.set("AcceptedTermsAndPolicies", acceptedTermsAndPolicies);

		ArrayNode researcherStatus = ga4gh.arrayNode();
		TextNode resDesc = addResearcherStatuses(pctx, researcherStatus, affiliations);
//		ga4gh.set("ResearcherStatus.description", resDesc);
		ga4gh.set("ResearcherStatus", researcherStatus);

		ArrayNode controlledAccessGrants = ga4gh.arrayNode();
		TextNode ctrlDesc = addControlledAccessGrants(pctx, controlledAccessGrants);
//		ga4gh.set("ControlledAccessGrants.description", ctrlDesc);
		ga4gh.set("ControlledAccessGrants", controlledAccessGrants);

//		ga4gh.set("timestamp", ga4gh.textNode(ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)));
		return ga4gh;
	}

	private JsonNode addAffiliationAndRoles(ClaimSourceProduceContext pctx, ArrayNode affiliationAndRole, List<Affiliation> affiliations) {
		//by=system for users with affiliation asserted by their IdP (set in UserExtSource attribute "affiliation")
		StringBuilder sb = new StringBuilder("Affiliations: ");
		for (Affiliation affiliation : affiliations) {
			long expires = ZonedDateTime.now().plusYears(1L).toEpochSecond();//values are not updated, setting one year in the future
			sb.append(affiliation.getValue()).append(",");
			affiliationAndRole.add(createRIClaim(affiliation.getValue(), affiliation.getSource(), "system", affiliation.getAsserted(), expires, null));
		}
		sb.deleteCharAt(sb.length() - 1);
		return affiliationAndRole.textNode(sb.toString());
	}

	private TextNode addAcceptedTermsAndPolicies(ClaimSourceProduceContext pctx, ArrayNode acceptedTermsAndPolicies) {
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
			long expires = ZonedDateTime.ofInstant(Instant.ofEpochSecond(asserted), ZoneId.systemDefault()).plusYears(100L).toEpochSecond();
			acceptedTermsAndPolicies.add(createRIClaim(BONA_FIDE_URL, NO_ORG_URL, "self", asserted, expires, null));
			return acceptedTermsAndPolicies.textNode("terms accepted on " + isoDate(asserted));
		} else {
			return acceptedTermsAndPolicies.textNode("not accepted");
		}
	}

	private TextNode addResearcherStatuses(ClaimSourceProduceContext pctx, ArrayNode researcherStatus, List<Affiliation> affiliations) {
		StringBuilder sb = new StringBuilder("Researcher status asserted by ");
		//by=peer for users with attribute elixirBonaFideStatusREMS
		PerunAttribute elixirBonaFideStatusREMS = pctx.getPerunConnector().getUserAttribute(pctx.getPerunUserId(), "urn:perun:user:attribute-def:def:elixirBonaFideStatusREMS");
		String valueCreatedAt = elixirBonaFideStatusREMS.getValueCreatedAt();
		if (valueCreatedAt != null) {
			long asserted = Timestamp.valueOf(valueCreatedAt).getTime() / 1000L;
			long expires = ZonedDateTime.now().plusYears(1L).toEpochSecond();
			researcherStatus.add(createRIClaim(BONA_FIDE_URL, NO_ORG_URL, "peer", asserted, expires, null));
			sb.append("peer on ").append(isoDate(asserted));
		}
		//by=system for users with faculty affiliation asserted by their IdP (set in UserExtSource attribute "affiliation")
		for (Affiliation affiliation : affiliations) {
			if (affiliation.getValue().startsWith("faculty@")) {
				long expires = ZonedDateTime.now().plusYears(1L).toEpochSecond();
				researcherStatus.add(createRIClaim(BONA_FIDE_URL, affiliation.getValue() + " " + affiliation.getSource(), "system", affiliation.getAsserted(), expires, null));
				sb.append("system as affiliation ").append(affiliation.getValue()).append(" on ").append(isoDate(affiliation.getAsserted()));
			}
		}
		//by=so for users with faculty affiliation asserted by membership in a group with groupAffiliations attribute
		for (Affiliation affiliation : pctx.getPerunConnector().getGroupAffiliations(pctx.getPerunUserId())) {
			if (affiliation.getValue().startsWith("faculty@")) {
				long expires = ZonedDateTime.now().plusYears(1L).toEpochSecond();
				researcherStatus.add(createRIClaim(BONA_FIDE_URL, affiliation.getValue(), "so", affiliation.getAsserted(), expires, null));
				sb.append("signing official as affiliation ").append(affiliation.getValue()).append(" on ").append(isoDate(affiliation.getAsserted()));
			}
		}
		return researcherStatus.textNode(researcherStatus.size() == 0 ? "not researcher" : sb.toString());
	}

	private String isoDate(long linuxTime) {
		return DateTimeFormatter.ISO_LOCAL_DATE.format(ZonedDateTime.ofInstant(Instant.ofEpochSecond(linuxTime), ZoneId.systemDefault()));
	}

	private ObjectNode createRIClaim(String value, String source, String by, long asserted, long expires, JsonNode condition) {
		ObjectNode n = JsonNodeFactory.instance.objectNode();
		n.put("value", value);
		n.put("source", source);
		n.put("by", by);
		n.put("asserted", asserted);
		n.put("expires", expires);
		if (condition != null && !condition.isNull() && !condition.isMissingNode()) {
			n.set("condition", condition);
		}
		return n;
	}

	private TextNode addControlledAccessGrants(ClaimSourceProduceContext pctx, ArrayNode controlledAccessGrants) {
		StringBuilder sb = new StringBuilder();
		if (remsRestTemplate != null) {
			callPermissionsAPI(remsRestTemplate, remsUrl + pctx.getSub(), pctx, controlledAccessGrants, sb);
		}
		if (egaRestTemplate != null) {
			callPermissionsAPI(egaRestTemplate, egaUrl + pctx.getSub() + "/", pctx, controlledAccessGrants, sb);
		}
		if(sb.length()>1) {
			sb.deleteCharAt(0);
		}
		return controlledAccessGrants.textNode(sb.toString());
	}

	private void callPermissionsAPI(RestTemplate restTemplate, String actionURL, ClaimSourceProduceContext pctx, ArrayNode controlledAccessGrants, StringBuilder sb) {
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
					sb.append(",").append(value).append(" valid ").append(isoDate(asserted)).append(" - ").append(isoDate(expires));
					controlledAccessGrants.add(createRIClaim(value, source, by, asserted, expires, condition));
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
					log.error("cannot make REST call", ex);
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
