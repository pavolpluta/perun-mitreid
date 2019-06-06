package cz.muni.ics.oidc.server.elixir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.muni.ics.oidc.models.PerunAttribute;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;
import cz.muni.ics.oidc.server.connectors.Affiliation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.List;

@SuppressWarnings("unused")
public class GA4GHClaimSource extends ClaimSource {

	private static final Logger log = LoggerFactory.getLogger(GA4GHClaimSource.class);
	private static final long ONE_YEAR = 3600L * 24L * 365L;
	private static final String BONA_FIDE_URL = "https://doi.org/10.1038/s41431-018-0219-y";
	private static final String NO_ORG_URL = "https://ga4gh.org/duri/no_org";

	public GA4GHClaimSource(ClaimSourceInitContext ctx) {
		super(ctx);
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {
		log.trace("produceValue(user={})", pctx.getPerunUserId());
		ObjectNode ga4gh = JsonNodeFactory.instance.objectNode();

		List<Affiliation> affiliations = pctx.getPerunConnector().getUserExtSourcesAffiliations(pctx.getPerunUserId());

		ArrayNode affiliationAndRole = ga4gh.arrayNode();
		addAffiliationAndRoles(pctx, affiliationAndRole, affiliations);
		ga4gh.set("AffiliationAndRole", affiliationAndRole);

		ArrayNode controlledAccessGrants = ga4gh.arrayNode();
		addControlledAccessGrants(pctx, controlledAccessGrants);
		ga4gh.set("ControlledAccessGrants", controlledAccessGrants);

		ArrayNode acceptedTermsAndPolicies = ga4gh.arrayNode();
		addAcceptedTermsAndPolicies(pctx, acceptedTermsAndPolicies);
		ga4gh.set("AcceptedTermsAndPolicies", acceptedTermsAndPolicies);

		ArrayNode researcherStatus = ga4gh.arrayNode();
		addResearcherStatuses(pctx, researcherStatus, affiliations);
		ga4gh.set("ResearcherStatus", researcherStatus);
		return ga4gh;
	}

	private void addAffiliationAndRoles(ClaimSourceProduceContext pctx, ArrayNode affiliationAndRole, List<Affiliation> affiliations) {
		for (Affiliation affiliation : affiliations) {
			affiliationAndRole.add(createRIClaim(affiliation.getValue(), affiliation.getSource(), "system", affiliation.getAsserted(), affiliation.getAsserted() + ONE_YEAR));
		}
	}

	private void addControlledAccessGrants(ClaimSourceProduceContext pctx, ArrayNode controlledAccessGrants) {

	}

	private void addAcceptedTermsAndPolicies(ClaimSourceProduceContext pctx, ArrayNode acceptedTermsAndPolicies) {
		// group 10432  is "Bona Fide Researchers"
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
			long expires = asserted + (ONE_YEAR * 100L);
			acceptedTermsAndPolicies.add(createRIClaim(BONA_FIDE_URL, NO_ORG_URL, "self", asserted, expires));
		}
	}

	private void addResearcherStatuses(ClaimSourceProduceContext pctx, ArrayNode researcherStatus, List<Affiliation> affiliations) {
		//by=peer
		PerunAttribute elixirBonaFideStatusREMS = pctx.getPerunConnector().getUserAttribute(pctx.getPerunUserId(), "urn:perun:user:attribute-def:def:elixirBonaFideStatusREMS");
		String valueCreatedAt = elixirBonaFideStatusREMS.getValueCreatedAt();
		if (valueCreatedAt != null) {
			long asserted = Timestamp.valueOf(valueCreatedAt).getTime() / 1000L;
			long expires = asserted + ONE_YEAR;
			researcherStatus.add(createRIClaim(BONA_FIDE_URL, NO_ORG_URL, "peer", asserted, expires));
		}
		//by=system
		for (Affiliation affiliation : affiliations) {
			if (affiliation.getValue().startsWith("faculty@")) {
				researcherStatus.add(createRIClaim(BONA_FIDE_URL, affiliation.getValue()+" "+affiliation.getSource(), "system", affiliation.getAsserted(), affiliation.getAsserted() + ONE_YEAR));
			}
		}
		//by=so
		for (Affiliation affiliation : pctx.getPerunConnector().getGroupAffiliations(pctx.getPerunUserId())) {
			if (affiliation.getValue().startsWith("faculty@")) {
				researcherStatus.add(createRIClaim(BONA_FIDE_URL, affiliation.getValue(), "so", affiliation.getAsserted(), affiliation.getAsserted() + ONE_YEAR));
			}
		}
	}

	private ObjectNode createRIClaim(String value, String source, String by, long asserted, long expires) {
		ObjectNode n = JsonNodeFactory.instance.objectNode();
		n.put("value", value);
		n.put("source", source);
		n.put("by", by);
		n.put("asserted", asserted);
		n.put("expires", expires);
		return n;
	}
}
