package cz.muni.ics.oidc.models;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RichUser object model.
 *
 * @author Peter Jancus jancus@ics.muni.cz
 */
public class RichUser extends Model {

	private String firstName;
	private String lastName;
	private String middleName;
	private Map<String, JsonNode> attributes = new LinkedHashMap<>();

	public RichUser(Long id, String firstName, String lastName, String middleName) {
		super(id);
		this.firstName = firstName;
		this.lastName = lastName;
		this.middleName = middleName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public Map<String, JsonNode> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, JsonNode> attributes) {
		this.attributes = attributes;
	}

	public String getAttributeValue(String attrName) {
		JsonNode jsonNode = attributes.get(attrName);
		return (jsonNode == null) || jsonNode.isNull() ? null : jsonNode.asText();
	}

	public JsonNode getJson(String attrName) {
		return attributes.get(attrName);
	}
}
