package cz.muni.ics.oidc.models;

/**
 * Group object model.
 *
 * @author Peter Jancus jancus@ics.muni.cz
 */
public class Group extends Model {

	private Long parentGroupId;
	private String name;
	private String description;
	private String uniqueGroupName; // voShortName + ":" + group name
	private Long voId;

	public Group(Long id, Long parentGroupId, String name, String description, String uniqueGroupName) {
		super(id);
		this.parentGroupId = parentGroupId;
		this.name = name;
		this.description = description;
		this.uniqueGroupName = uniqueGroupName;
	}

	public Group(Long id, Long parentGroupId, String name, String description, Long voId) {
		super(id);
		this.parentGroupId = parentGroupId;
		this.name = name;
		this.description = description;
		this.voId = voId;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public void setUniqueGroupName(String uniqueGroupName) {
		this.uniqueGroupName = uniqueGroupName;
	}

	public Long getVoId() {
		return voId;
	}

	public void setVoId(Long voId) {
		this.voId = voId;
	}

	/**
	 * Gets identifier voShortName:group.name usable for groupNames in AARC format.
	 */
	public String getUniqueGroupName() {
		return uniqueGroupName;
	}

	@Override
	public String toString() {
		return "Group{" +
				"id=" + getId() +
				", name='" + name + '\'' +
				", uniqueGroupName='" + uniqueGroupName + '\'' +
				", description='" + description + '\'' +
				", parentGroupId=" + parentGroupId +
				", voId=" + voId +
				'}';
	}
}
