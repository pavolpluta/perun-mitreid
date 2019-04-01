package cz.muni.ics.oidc.models;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.Group;
import cz.muni.ics.oidc.models.Member;
import cz.muni.ics.oidc.models.PerunAttribute;
import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.models.Resource;
import cz.muni.ics.oidc.models.RichUser;
import cz.muni.ics.oidc.models.Vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is mapping JsonNodes to object models.
 *
 * @author Peter Jancus jancus@ics.muni.cz
 */
public class Mapper {

	/**
	 * Maps JsonNode to Facility model
	 *
	 * @param jsonNode facility in Json format to be mapped
	 * @return Facility mapped from JsonNode
	 */
	public static Facility mapFacility(JsonNode jsonNode) {
		Long id = jsonNode.get("id").asLong();
		String name = jsonNode.get("name").asText();
		String description = jsonNode.get("description").asText();

		return new Facility(id, name, description);
	}

	public static PerunUser mapPerunUser(JsonNode jsonNode) {
		long userId = jsonNode.path("id").asLong();
		String firstName = jsonNode.path("firstName").asText();
		String lastName = jsonNode.path("lastName").asText();
		return new PerunUser(userId, firstName, lastName);
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
		Long voId = jsonNode.get("voId").asLong();
		return new Group(id, parentGroupId, name, description, voId);
	}

	/**
	 * Maps JsonNode to List of Groups
	 * @param jsonNode groups in Json format to be mapped
	 * @return List of groups mapped or empty list
	 */
	public static List<Group> mapGroups(JsonNode jsonNode) {
		List<Group> result = new ArrayList<>();
		for (int i = 0; i < jsonNode.size(); i++) {
			JsonNode groupNode = jsonNode.get(i);
			Group mappedGroup = mapGroup(groupNode);
			result.add(mappedGroup);
		}

		return result;
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
	 * Maps JsonNode to List of Member model
	 * @param jsonNode members in JSON format to be mapped
	 * @return Members mapped from JsonNode
	 */
	public static List<Member> mapMembers(JsonNode jsonNode) {
		List<Member> members = new ArrayList<>();
		for (int i = 0; i < jsonNode.size(); i++) {
			JsonNode memberNode = jsonNode.get(i);
			Member mappedMember = mapMember(memberNode);
			members.add(mappedMember);
		}

		return members;
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
	 *
	 * @param jsonNode rich user in Json format to be mapped
	 * @return RichUser mapped from JsonNode
	 */
	public static RichUser mapRichUser(JsonNode jsonNode) {
		Map<String, JsonNode> map = new HashMap<>();
		Long id = jsonNode.get("id").asLong();
		RichUser richUser = new RichUser(id);
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

	/**
	 * Map JsonNode to Perun attribute
	 * @param jsonNode attribute in JSON format to be mapped
	 * @return PerunAttribute mapped from JsonNode
	 */
	public static PerunAttribute mapAttribute(JsonNode jsonNode) {
		PerunAttribute attribute = new PerunAttribute();
		attribute.setId(jsonNode.get("id").asLong());
		attribute.setFriendlyName(jsonNode.get("friendlyName").asText());
		attribute.setNamespace(jsonNode.get("namespace").asText());
		attribute.setDescription(jsonNode.get("description").asText());
		attribute.setType(jsonNode.get("type").asText());
		attribute.setDisplayName(jsonNode.get("displayName").asText());
		attribute.setWritable(jsonNode.get("writable").asBoolean());
		attribute.setUnique(jsonNode.get("unique").asBoolean());
		attribute.setBaseFriendlyName(jsonNode.get("baseFriendlyName").asText());
		attribute.setFriendlyNameParameter(jsonNode.get("friendlyNameParameter").asText());
		attribute.setValue((jsonNode.get("value").isNull()) ? null : attribute.getType(), jsonNode.get("value"));

		return attribute;
	}

	/**
	 * Map JsonNode to Map of Perun attributes
	 * @param jsonNode attributes as array in JSON format to be mapped
	 * @return Map of PerunAttributes mapped from JsonNode, where key = URN, value = Attribute
	 */
	public static Map<String, PerunAttribute> mapAttributes(JsonNode jsonNode) {
		Map<String, PerunAttribute> res = new HashMap<>();
		for (int i = 0; i < jsonNode.size(); i++) {
			JsonNode attribute = jsonNode.get(i);
			PerunAttribute mappedAttribute = mapAttribute(attribute);
			res.put(mappedAttribute.getUrn(), mappedAttribute);
		}

		return res;
	}

	/**
	 * Map JsonNode to VO
	 * @param jsonNode vo in JSON format to e mapped
	 * @return Vo mapped from JsonNode
	 */
	public static Vo mapVo(JsonNode jsonNode) {
		Vo vo = new Vo();
		vo.setId(jsonNode.get("id").asLong());
		vo.setName(jsonNode.get("name").asText());
		vo.setShortName(jsonNode.get("shortName").asText());

		return vo;
	}


}
