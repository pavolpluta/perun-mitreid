package cz.muni.ics.oidc.models;

/**
 * Facility object model.
 *
 * @author Peter Jancus <jancus@ics.muni.cz>
 */
public class Facility extends Model {

	private String name;
	private String description;

	public Facility(Long id, String name, String description) {
		super(id);
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "Facility{" +
				"id=" + getId() +
				", name='" + name + '\'' +
				", description='" + description + '\'' +
				'}';
	}
}
