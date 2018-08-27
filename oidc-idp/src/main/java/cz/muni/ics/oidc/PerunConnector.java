package cz.muni.ics.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.Group;
import cz.muni.ics.oidc.models.Member;
import cz.muni.ics.oidc.models.Resource;
import cz.muni.ics.oidc.models.RichUser;

import java.util.List;

/**
 * Connects to Perun and obtains information.
 *
 * @author Martin Kuba makub@ics.muni.czc
 * @author Dominik František Bučík bucik@ics.muni.cz
 * @author Peter Jancus jancus@ics.muni.cz
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
	 * @return RichUser with attributes of found user
	 */
	RichUser getUserAttributes(Long userId);

	/**
	 * Fetch OIDC facility from Perun
	 * @param clientId value for attribute OIDCClientID
	 * @return List of facilities if it has found at least one, empty list otherwise
	 */
	List<Facility> getFacilitiesByClientId(String clientId);

	/**
	 * Fetch resources assigned to facility specified by id
	 * @param facilityId id of facility
	 * @return List of assigned resources if it has found at least one, empty list otherwise
	 */
	List<Resource> getAssignedResourcesForFacility(Long facilityId);

	/**
	 * Fetch all groups associated with resource and member
	 * @param resourceId id of resource
	 * @param memberId id of member
	 * @return List of groups associated both with resource and member if it has found at least one, empty list otherwise
	 */
	List<Group> getAssignedGroups(Long resourceId, Long memberId);

	/**
	 * Fetch members of user specified by his/her id
	 * @param userId id of user
	 * @return List of user members if it has found at least one, empty list otherwise
	 */
	List<Member> getMembersByUser(String userId);

	/**
	 * Ask Perun if membership of user in groups allowed to access resource should be checked.
	 * @param facilityId id of facility to be accessed
	 * @return TRUE if check should be done, FALSE otherwise
	 */
	boolean isAllowedGroupCheckForFacility(Long facilityId);

}