package cz.muni.ics.oidc;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Connects to Perun and obtains information.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public interface PerunConnector {

	JsonNode getPreauthenticatedUserId(PerunPrincipal perunPrincipal);

	JsonNode getUserAttributes(Long userId);

}
