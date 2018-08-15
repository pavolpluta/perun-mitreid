package cz.muni.ics.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.Group;
import cz.muni.ics.oidc.models.Member;
import cz.muni.ics.oidc.models.Resource;
import cz.muni.ics.oidc.models.RichUser;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is mapping JsonNodes to object models.
 *
 * @author Peter Jancus jancus@ics.muni.cz
 */
public class Mapper {

	/**
	 * Maps JsonNode to Facility model
	 * @param jsonNode facility in Json format to be mapped
	 * @return Facility mapped from JsonNode
	 */
	public static Facility mapFacility(JsonNode jsonNode) {
		Long id = jsonNode.get("id").asLong();
		String name = jsonNode.get("name").asText();
		String description = jsonNode.get("description").asText();

		return new Facility(id, name, description);
	}

	/**
	 * Maps JsonNode to Group model
	 * @param jsonNode group in Json format to be mapped
	 * @return Group mapped from JsonNode
	 */
	public static Group mapGroup(JsonNode jsonNode) {
		Long id = jsonNode.get("id").asLong();
		Long parentGroupId = jsonNode.get("parentGroupId").asLong();
		String name = jsonNode.get("name").asText();
		String description = jsonNode.get("description").asText();
		String shortName = jsonNode.get("shortName").asText();

		return new Group(id, parentGroupId, name, description, shortName);
	}

	/**
	 * Maps JsonNode to Member model
	 * @param jsonNode member in Json format to be mapped
	 * @return Member mapped from JsonNode
	 */
	public static Member mapMember(JsonNode jsonNode) {
		Long id = jsonNode.get("id").asLong();
		Long userId = jsonNode.get("userId").asLong();
		Long voId = jsonNode.get("voId").asLong();
		String status = jsonNode.get("status").asText();

		return new Member(id, userId, voId, status);
	}

	/**
	 * Maps JsonNode to Resource model
	 * @param jsonNode resource in Json format to be mapped
	 * @return Resource mapped from JsonNode
	 */
	public static Resource mapResource(JsonNode jsonNode) {
		Long id = jsonNode.get("id").asLong();
		Long voId = jsonNode.get("voId").asLong();
		String name = jsonNode.get("name").asText();
		String description = jsonNode.get("description").asText();

		return new Resource(id, voId, name, description);
	}

	/**
	 * Mapps JsonNode to RichUser model
	 * @param jsonNode rich user in Json format to be mapped
	 * @return RichUser mapped from JsonNode
	 */
	public static RichUser mapRichUser (JsonNode jsonNode) {
		Map<String, JsonNode> map = new HashMap<>();

		Long id = jsonNode.get("id").asLong();
		String firstName = jsonNode.get("firstName").asText();
		String lastName = jsonNode.get("lastName").asText();
		String middleName = jsonNode.get("middleName").asText();
		RichUser richUser = new RichUser(id, firstName, lastName, middleName);

		JsonNode attributesNode = jsonNode.get("userAttributes");

		for (int i = 0; i < attributesNode.size(); i++) {
			String friendlyName = attributesNode.get(i).get("friendlyName").asText();
			String namespace = attributesNode.get(i).get("namespace").asText();
			JsonNode valueNode = attributesNode.get(i).get("value");
			map.put(namespace + ":" + friendlyName, valueNode);
		}
		richUser.setAttributes(map);
		return richUser;
	}

}
