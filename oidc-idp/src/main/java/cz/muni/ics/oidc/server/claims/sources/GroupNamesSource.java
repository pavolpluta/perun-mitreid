package cz.muni.ics.oidc.server.claims.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.Group;
import cz.muni.ics.oidc.server.adapters.PerunAdapter;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

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

	public static final Logger log = LoggerFactory.getLogger(GroupNamesSource.class);

	public GroupNamesSource(ClaimSourceInitContext ctx) {
		super(ctx);
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {
		return produceValue(pctx, true);
	}

	protected JsonNode produceValueWithoutReplacing(ClaimSourceProduceContext pctx) {
		log.debug("producing value without trimming 'members'");
		return produceValue(pctx, false);
	}

	private JsonNode produceValue(ClaimSourceProduceContext pctx, boolean trimMembers) {
		PerunAdapter perunConnector = pctx.getPerunAdapter();
		ClientDetailsEntity client = pctx.getClient();
		Facility facility = null;

		if (client != null) {
			String clientId = client.getClientId();
			facility = perunConnector.getFacilityByClientId(clientId);
			log.debug("found facility ({}) for client_id ({})", facility, clientId);
		}

		Set<Group> userGroups = new HashSet<>();
		if (facility != null) {
			userGroups = perunConnector.getGroupsWhereUserIsActiveWithUniqueNames(facility.getId(),
					pctx.getPerunUserId());
			log.debug("Found user groups: {}", userGroups);
		}

		Set<String> groups = new TreeSet<>();
		userGroups.forEach(g -> {
			String uniqueName = g.getUniqueGroupName();
			if (trimMembers && StringUtils.hasText(uniqueName) && "members".equals(g.getName())) {
				uniqueName = uniqueName.replace(":members", "");
				g.setUniqueGroupName(uniqueName);
			}

			groups.add(g.getUniqueGroupName());
		});

		ArrayNode result = JsonNodeFactory.instance.arrayNode();
		for (String groupName: groups) {
			result.add(groupName);
		}

		log.debug("produced groupNames: {}", result);
		return result;
	}


}
