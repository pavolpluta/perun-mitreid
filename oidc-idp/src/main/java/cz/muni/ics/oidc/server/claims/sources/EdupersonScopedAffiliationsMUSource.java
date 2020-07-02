package cz.muni.ics.oidc.server.claims.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Claim source for eduperson_scoped_affiliations MUNI.
 *
 * Configuration (replace [claimName] with the name of the claim):
 * <ul>
 *     <li>
 *         <b>custom.claim.[claimName].source.config_file</b> - path to the YML config file, see
 *         'eduperson_scoped_affiliations_mu_source.yml' for example configuration
 *     </li>
 * </ul>
 *
 * @author Dominik Baránek <baranek@ics.muni.cz>
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class EdupersonScopedAffiliationsMUSource extends ClaimSource {

	private static final Logger log = LoggerFactory.getLogger(EdupersonScopedAffiliationsMUSource.class);

	private Map<List<Long>, String> affiliations = new HashMap<>();
	private Long voId = 363L;
	private String scope = "muni.cz";

	public EdupersonScopedAffiliationsMUSource(ClaimSourceInitContext ctx) {
		super(ctx);
		log.debug("initializing");
		parseConfigFile(ctx.getProperty("config_file", "/etc/perun/eduperson_scoped_affiliations_mu_source.yml"));
	}

	private void parseConfigFile(String file) {
		log.debug("loading config file {}", file);
		YAMLMapper mapper = new YAMLMapper();
		try {
			JsonNode root = mapper.readValue(new File(file), JsonNode.class);
			// prepare claim repositories
			scope = root.get("scope").asText();
			voId = root.get("voId").longValue();
			for (JsonNode affiliationMapping : root.path("affiliations")) {
				String value = affiliationMapping.path("value").asText();
				List<Long> gids = new ArrayList<>();
				for (JsonNode gid : affiliationMapping.path("groups")) {
					gids.add(gid.asLong());
				}
				affiliations.put(gids, value);
			}
		} catch (IOException ex) {
			log.error("cannot read EPSA_MU config file", ex);
		}
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {
		log.debug("producing value started for user with sub: {}", pctx.getSub());
		ArrayNode result = JsonNodeFactory.instance.arrayNode();
		Long userId = pctx.getPerunUserId();
		Set<Long> groups = pctx.getPerunAdapter().getUserGroupsIds(userId, voId);
		for (Map.Entry<List<Long>, String> entry : affiliations.entrySet()) {
			for (Long id: entry.getKey()) {
				if (groups.contains(id)) {
					String affiliation = entry.getValue() + '@' + scope;
					log.debug("added affiliation: {}", affiliation);
					result.add(affiliation);
					break;
				}
			}
		}

		if (result.size() == 0) {
			String affiliation = "affiliate@" + scope;
			log.debug("added affiliation: {}", affiliation);
			result.add(affiliation);
		}

		log.debug("produced value {} for user with sub {}", result, pctx.getSub());
		return result;
	}
}
