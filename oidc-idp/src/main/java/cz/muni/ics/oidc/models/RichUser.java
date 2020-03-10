package cz.muni.ics.oidc.models;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RichUser object model.
 *
 * @author Peter Jancus <jancus@ics.muni.cz>
 */
public class RichUser extends Model {

	private Map<String, JsonNode> attributes = new LinkedHashMap<>();

	public RichUser(Long id) {
		super(id);
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

	@Override
	public String toString() {
		return "RichUser{" +
				"id="+getId()+
				"attributes=" + attributes +
				'}';
	}
}
