package cz.muni.ics.oidc.models;

/**
 * Represents user from Perun.
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class PerunUser {

	private long id;
	private String firstName;
	private String lastName;

	public PerunUser(long id, String firstName, String lastName) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public long getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}
}
