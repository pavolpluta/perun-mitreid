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

	private static final String CONFIG_FILE = "config_file";
	private static final String KEY_SCOPE = "scope";
	private static final String KEY_VO_ID = "voId";
	private static final String KEY_AFFILIATIONS = "affiliations";
	private static final String KEY_VALUE = "value";
	private static final String KEY_GROUPS = "groups";

	private static final String DEFAULT_PATH = "/etc/perun/eduperson_scoped_affiliations_mu_source.yml";

	private final Map<List<Long>, String> affiliations = new HashMap<>();
	private Long voId = 363L;
	private String scope = "muni.cz";

	public EdupersonScopedAffiliationsMUSource(ClaimSourceInitContext ctx) {
		super(ctx);
		log.debug("Initializing '{}'", this.getClass().getSimpleName());
		parseConfigFile(ctx.getProperty(CONFIG_FILE, DEFAULT_PATH));
	}

	private void parseConfigFile(String file) {
		log.debug("Loading config file {}", file);
		YAMLMapper mapper = new YAMLMapper();
		try {
			JsonNode root = mapper.readValue(new File(file), JsonNode.class);
			scope = root.get(KEY_SCOPE).asText();
			voId = root.get(KEY_VO_ID).longValue();
			for (JsonNode affiliationMapping : root.path(KEY_AFFILIATIONS)) {
				String value = affiliationMapping.path(KEY_VALUE).asText();
				List<Long> gids = new ArrayList<>();
				for (JsonNode gid : affiliationMapping.path(KEY_GROUPS)) {
					gids.add(gid.asLong());
				}
				affiliations.put(gids, value);
			}
		} catch (IOException ex) {
			log.error("Cannot read '{}' config file", this.getClass().getSimpleName(), ex);
		}
	}

	@Override
	public JsonNode produceValue(ClaimSourceProduceContext pctx) {
		Long userId = pctx.getPerunUserId();
		log.debug("Producing values...");
		ArrayNode result = JsonNodeFactory.instance.arrayNode();
		Set<Long> groups = pctx.getPerunAdapter().getUserGroupsIds(userId, voId);
		for (Map.Entry<List<Long>, String> entry : affiliations.entrySet()) {
			for (Long id: entry.getKey()) {
				if (groups.contains(id)) {
					String affiliation = entry.getValue() + '@' + scope;
					log.trace("Added affiliation: {}", affiliation);
					result.add(affiliation);
					break;
				}
			}
		}

		if (result.size() == 0) {
			String affiliation = "affiliate@" + scope;
			log.trace("Added affiliation: {}", affiliation);
			result.add(affiliation);
		}

		log.debug("Value '{}' for user '{}' with sub '{}' produced", result, userId, pctx.getSub());
		return result;
	}
}
