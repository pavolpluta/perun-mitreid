package cz.muni.ics.oidc;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Connects to Perun and obtains information.
 *
 * @author Martin Kuba makub@ics.muni.czc
 * @author Dominik František Bučík bucik@ics.muni.cz
 */
public interface PerunConnector {

	/**
	 * Fetch user based on his principal (extSource) from Perun
	 * @param perunPrincipal principal of user
	 * @return JsonNode with attributes of found user
	 */
	JsonNode getPreauthenticatedUserId(PerunPrincipal perunPrincipal);

	/**
	 * Fetch user identified by userId from Perun
	 * @param userId identifier of the user
	 * @return JsonNode with attributes of found user
	 */
	JsonNode getUserAttributes(Long userId);

	/**
	 * Fetch OIDC facility from Perun
	 * @param clientId value for attribute OIDCClientID
	 * @return JsonNode with attributes of found facility
	 */
	JsonNode getFacilitiesByClientId(String clientId);

	/**
	 * Fetch resources assigned to facility specified by id
	 * @param facilityId id of facility
	 * @return JsonNode with assigned resources
	 */
	JsonNode getAssignedResourcesForFacility(String facilityId);

	/**
	 * Fetch all groups associated with resource and member
	 * @param resourceId id of resource
	 * @param memberId id of member
	 * @return JsonNode with groups associated both with resource and member
	 */
	JsonNode getAssignedGroups(String resourceId, String memberId);

	/**
	 * Fetch members of user specified by his/her id
	 * @param userId id of user
	 * @return JsonNode with user members
	 */
	JsonNode getMembersByUser(String userId);

	/**
	 * Ask Perun if membership of user in groups allowed to access resource should be checked.
	 * @param facilityId id of facility to be accessed
	 * @return TRUE if check should be done, FALSE otherwise
	 */
	boolean isAllowedGroupCheckForFacility(String facilityId);

}