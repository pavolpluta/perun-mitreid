package cz.muni.ics.oidc;

import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.Group;
import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.models.RichUser;

import java.util.Set;

/**
 * Connects to Perun and obtains information.
 *
 * @author Martin Kuba makub@ics.muni.czc
 * @author Dominik František Bučík bucik@ics.muni.cz
 * @author Peter Jancus jancus@ics.muni.cz
 */
public interface PerunConnector {

	/**
	 * Fetch user based on his principal (extLogin and extSource) from Perun
	 *
	 * @param perunPrincipal principal of user
	 * @return PerunUser with id of found user
	 */
	PerunUser getPreauthenticatedUserId(PerunPrincipal perunPrincipal);

	/**
	 * Fetch user identified by userId from Perun with all attributes.
	 *
	 * @param userId identifier of the user
	 * @return RichUser with attributes of found user
	 */
	RichUser getUserAttributes(Long userId);

	/**
	 * Fetch facility registered in Perun for the given OIDC client.
	 *
	 * @param clientId value for attribute OIDCClientID
	 * @return facility if it was found or null
	 */
	Facility getFacilityByClientId(String clientId);

	/**
	 * Ask Perun if user's access to the facility should be checked.
	 *
	 * @param facility facility to be accessed
	 * @return TRUE if check should be done, FALSE otherwise
	 */
	boolean isMembershipCheckEnabledOnFacility(Facility facility);

	/**
	 * Decide whether the user is allowed to access the facility.
	 *
	 * @param facility facility to be accessed
	 * @param userId   id of user to check
	 * @return true if the user is member of any group assigned to a resource on the facility
	 */
	boolean isUserAllowedOnFacility(Facility facility, Long userId);

	/**
	 * Provides a list of groups which connect the user to the facility.
	 *
	 * @param facility acility to be accessed
	 * @param userId   id of user to check
	 * @return list of all groups such that the user is member of the group and the group is assigned to a resource of the facility
	 */
	Set<Group> getUserGroupsAllowedOnFacility(Facility facility, Long userId);
}
