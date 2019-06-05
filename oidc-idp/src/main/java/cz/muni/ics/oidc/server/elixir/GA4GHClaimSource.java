package cz.muni.ics.oidc.server.elixir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cz.muni.ics.oidc.models.PerunAttribute;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;

@SuppressWarnings("unused")
public class GA4GHClaimSource extends ClaimSource {

	private static final Logger log = LoggerFactory.getLogger(GA4GHClaimSource.class);

	public GA4GHClaimSource(ClaimSourceInitContext ctx) {
		super(ctx);
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {
		log.trace("produceValue(sub={})", pctx.getSub());
		ObjectNode ga4gh = JsonNodeFactory.instance.objectNode();

		ArrayNode affiliationAndRole = ga4gh.arrayNode();
		addAffiliationAndRoles(pctx, affiliationAndRole);
		ga4gh.set("AffiliationAndRole", affiliationAndRole);

		ArrayNode controlledAccessGrants = ga4gh.arrayNode();
		addControlledAccessGrants(pctx, controlledAccessGrants);
		ga4gh.set("ControlledAccessGrants", controlledAccessGrants);

		ArrayNode acceptedTermsAndPolicies = ga4gh.arrayNode();
		addAcceptedTermsAndPolicies(pctx, acceptedTermsAndPolicies);
		ga4gh.set("AcceptedTermsAndPolicies", acceptedTermsAndPolicies);

		ArrayNode researcherStatus = ga4gh.arrayNode();
		addResearcherStatuses(pctx, researcherStatus);
		ga4gh.set("ResearcherStatus", researcherStatus);
		return ga4gh;
	}

	private void addAffiliationAndRoles(ClaimSourceProduceContext pctx, ArrayNode affiliationAndRole) {

	}

	private void addControlledAccessGrants(ClaimSourceProduceContext pctx, ArrayNode controlledAccessGrants) {

	}

	private void addAcceptedTermsAndPolicies(ClaimSourceProduceContext pctx, ArrayNode acceptedTermsAndPolicies) {
		// group 10432  is "Bona Fide Researchers"
		boolean userInGroup = pctx.getPerunConnector().isUserInGroup(pctx.getPerunUserId(), 10432L);
		if (userInGroup) {
			ObjectNode n = JsonNodeFactory.instance.objectNode();
			n.put("value", "https://doi.org/10.1038/s41431-018-0219-y");
			n.put("source", "https://ga4gh.org/duri/no_org");
			n.put("by", "self");

			PerunAttribute userAttribute = pctx.getPerunConnector().getUserAttribute(pctx.getPerunUserId(), "urn:perun:user:attribute-def:def:bonaFideStatus");
			String valueCreatedAt = userAttribute.getValueCreatedAt();
			long asserted;
			if (valueCreatedAt != null) {
				asserted = Timestamp.valueOf(valueCreatedAt).getTime() / 1000L;
			} else {
				asserted = System.currentTimeMillis() / 1000L;
			}
			n.put("asserted", asserted);
			n.put("expires", asserted + (3600L * 24L * 365L * 100L));
			acceptedTermsAndPolicies.add(n);
		}
	}

	private void addResearcherStatuses(ClaimSourceProduceContext pctx, ArrayNode researcherStatus) {

	}


}
