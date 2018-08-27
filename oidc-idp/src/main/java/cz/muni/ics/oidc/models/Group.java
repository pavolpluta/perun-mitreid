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
	private String shortName;

	public Group(Long id, Long parentGroupId, String name, String description, String shortName) {
		super(id);
		this.parentGroupId = parentGroupId;
		this.name = name;
		this.description = description;
		this.shortName = shortName;
	}

	public Long getParentGroupId() {
		return parentGroupId;
	}

	public void setParentGroupId(Long parentGroupId) {
		this.parentGroupId = parentGroupId;
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

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}
}
