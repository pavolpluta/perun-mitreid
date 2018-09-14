package cz.muni.ics.oidc.models;

/**
 * Resource object model.
 *
 * @author Peter Jancus jancus@ics.muni.cz
 */
public class Resource extends Model {

	private Long voId;
	private String name;
	private String description;

	public Resource(Long id, Long voId, String name, String description) {
		super(id);
		this.voId = voId;
		this.name = name;
		this.description = description;
	}

	public Long getVoId() {
		return voId;
	}

	public void setVoId(Long voId) {
		this.voId = voId;
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
		return "Resource{" +
				"id=" + getId() +
				", voId=" + voId +
				", name='" + name + '\'' +
				", description='" + description + '\'' +
				'}';
	}
}
