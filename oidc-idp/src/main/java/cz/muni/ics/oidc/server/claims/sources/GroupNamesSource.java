package cz.muni.ics.oidc.server.claims.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.Group;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;
import cz.muni.ics.oidc.server.connectors.PerunConnector;
import org.mitre.oauth2.model.ClientDetailsEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Source fetches all unique group names in context of user and facility. If no facility exists for the client, empty
 * list is returned as result.
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class GroupNamesSource extends ClaimSource {

	public GroupNamesSource(ClaimSourceInitContext ctx) {
		super(ctx);
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {
		PerunConnector perunConnector = pctx.getPerunConnector();
		ClientDetailsEntity client = pctx.getClient();
		Facility facility = null;

		if (client != null) {
			String clientId = client.getClientId();
			facility = perunConnector.getFacilityByClientId(clientId);
		}

		Set<Group> userGroups = new HashSet<>();
		if (facility != null) {
			userGroups = perunConnector.getGroupsWhereUserIsActiveWithUniqueNames(facility.getId(), pctx.getRichUser().getId());
		}

		Set<String> groups = new TreeSet<>();
		userGroups.forEach(g -> {
			String uniqueName = g.getUniqueGroupName();
			if (uniqueName != null && !uniqueName.trim().isEmpty()
					&& "members".equals(g.getName())) {
				uniqueName = uniqueName.replace(":members", "");
				g.setUniqueGroupName(uniqueName);
			}

			groups.add(g.getUniqueGroupName());
		});

		ArrayNode result = JsonNodeFactory.instance.arrayNode();
		for (String entitlement: groups) {
			result.add(entitlement);
		}

		return result;
	}

}
