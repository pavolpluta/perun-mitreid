package cz.muni.ics.oidc.models;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Group object model.
 *
 * @author Peter Jancus <jancus@ics.muni.cz>
 */
public class Group extends Model {

	public static final String GROUP_NAMESPACE_CORE = "urn:perun:group:attribute-def:core";
	public static final String GROUP_NAMESPACE_DEF = "urn:perun:group:attribute-def:def";
	public static final String GROUP_NAMESPACE_VIRT = "urn:perun:group:attribute-def:virt";
	public static final String GROUP_NAMESPACE_OPT = "urn:perun:group:attribute-def:opt";

	private Long parentGroupId;
	private String name;
	private String description;
	private String uniqueGroupName; // voShortName + ":" + group name
	private Long voId;
	private Map<String, JsonNode> attributes = new LinkedHashMap<>();

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

	public Map<String, JsonNode> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, JsonNode> attributes) {
		this.attributes = attributes;
	}

	/**
	 * Gets identifier voShortName:group.name usable for groupNames in AARC format.
	 */
	public String getUniqueGroupName() {
		return uniqueGroupName;
	}

	/**
	 * Gets attribute by urn name
	 *
	 * @param attributeName urn name of attribute
	 * @return attribute
	 */
	public JsonNode getAttributeByUrnName(String attributeName) {
		if (attributes == null || !attributes.containsKey(attributeName)) {
			return null;
		}

		return attributes.get(attributeName);
	}

	/**
	 * Gets attribute by friendly name
	 *
	 * @param attributeName attribute name
	 * @param attributeUrnPrefix urn prefix of attribute
	 * @return attribute
	 */
	public JsonNode getAttributeByFriendlyName(String attributeName, String attributeUrnPrefix) {
		String key = attributeUrnPrefix + ":" + attributeName;

		if (attributes == null || !attributes.containsKey(key)) {
			return null;
		}

		return attributes.get(key);
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
