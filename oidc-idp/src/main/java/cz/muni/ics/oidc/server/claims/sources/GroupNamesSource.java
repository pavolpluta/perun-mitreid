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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Source fetches all unique group names in context of user and facility. If no facility exists for the client, empty
 * list is returned as result.
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class GroupNamesSource extends ClaimSource {

	public static final Logger log = LoggerFactory.getLogger(GroupNamesSource.class);

	private final String claimName;

	public GroupNamesSource(ClaimSourceInitContext ctx) {
		super(ctx);
		this.claimName = ctx.getClaimName();
		log.debug("{} - initialized", claimName);
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {
		Map<Long, String> idToNameMap = this.produceValue(pctx, true);
		ArrayNode arr = JsonNodeFactory.instance.arrayNode();
		new HashSet<>(idToNameMap.values()).forEach(arr::add);

		log.debug("{} - produced value for user({}): '{}'", claimName, pctx.getPerunUserId(), arr);
		return arr;
	}

	protected Map<Long, String> produceValueWithoutReplacing(ClaimSourceProduceContext pctx) {
		return produceValue(pctx, false);
	}

	private Map<Long, String> produceValue(ClaimSourceProduceContext pctx, boolean trimMembers) {
		log.trace("{} - produce value {} trimming 'members' part of the group names",
				claimName, (trimMembers ? "with": "without"));
		PerunAdapter perunConnector = pctx.getPerunAdapter();
		ClientDetailsEntity client = pctx.getClient();
		Facility facility = null;

		if (client != null) {
			String clientId = client.getClientId();
			facility = perunConnector.getFacilityByClientId(clientId);
		}

		Set<Group> userGroups = new HashSet<>();
		if (facility != null) {
			userGroups = perunConnector.getGroupsWhereUserIsActiveWithUniqueNames(facility.getId(),
					pctx.getPerunUserId());
			log.trace("{} - found user groups: '{}'", claimName, userGroups);
		}

		Map<Long, String> idToNameMap = new HashMap<>();
		userGroups.forEach(g -> {
			String uniqueName = g.getUniqueGroupName();
			if (trimMembers && StringUtils.hasText(uniqueName) && "members".equals(g.getName())) {
				uniqueName = uniqueName.replace(":members", "");
				g.setUniqueGroupName(uniqueName);
			}

			idToNameMap.put(g.getId(), g.getUniqueGroupName());
		});

		log.trace("{} - group ID to group name map: '{}'", claimName, idToNameMap);
		return idToNameMap;
	}

}
