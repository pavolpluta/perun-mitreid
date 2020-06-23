package cz.muni.ics.oidc.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.codehaus.jackson.JsonNode;

import java.util.Objects;

/**
 * Perun Attribute Definition model
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class PerunAttributeDefinition extends Model {

	private String friendlyName;
	private String namespace;
	private String description;
	private String type;
	private String displayName;
	private boolean writable;
	private boolean unique;
	private String entity;
	private final String beanName = "Attribute";
	private String baseFriendlyName;
	private String friendlyNameParameter;

	public PerunAttributeDefinition() { }

	public PerunAttributeDefinition(Long id, String friendlyName, String namespace, String description, String type,
									String displayName, boolean writable, boolean unique, String entity,
									String baseFriendlyName, String friendlyNameParameter) {
		super(id);
		this.setFriendlyName(friendlyName);
		this.setNamespace(namespace);
		this.setDescription(description);
		this.setType(type);
		this.setDisplayName(displayName);
		this.setWritable(writable);
		this.setUnique(unique);
		this.setEntity(entity);
		this.setBaseFriendlyName(baseFriendlyName);
		this.setFriendlyNameParameter(friendlyNameParameter);
	}

	public String getFriendlyName() {
		return friendlyName;
	}

	public void setFriendlyName(String friendlyName) {
		if (Strings.isNullOrEmpty(friendlyName)) {
			throw new IllegalArgumentException("friendlyName can't be null or empty");
		}

		this.friendlyName = friendlyName;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		if (Strings.isNullOrEmpty(namespace)) {
			throw new IllegalArgumentException("namespace can't be null or empty");
		}

		this.namespace = namespace;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		if (description == null) {
			throw new IllegalArgumentException("description can't be null");
		}

		this.description = description;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		if (Strings.isNullOrEmpty(type)) {
			throw new IllegalArgumentException("type can't be null or empty");
		}

		this.type = type;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		if (Strings.isNullOrEmpty(displayName)) {
			throw new IllegalArgumentException("displayName can't be null or empty");
		}

		this.displayName = displayName;
	}

	public boolean isWritable() {
		return writable;
	}

	public void setWritable(boolean writable) {
		this.writable = writable;
	}

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public String getEntity() {
		return entity;
	}

	public void setEntity(String entity) {
		if (Strings.isNullOrEmpty(entity)) {
			throw new IllegalArgumentException("entity can't be null or empty");
		}

		this.entity = entity;
	}

	public String getBeanName() {
		return beanName;
	}

	public String getBaseFriendlyName() {
		return baseFriendlyName;
	}

	public void setBaseFriendlyName(String baseFriendlyName) {
		if (Strings.isNullOrEmpty(baseFriendlyName)) {
			throw new IllegalArgumentException("baseFriendlyName can't be null or empty");
		}

		this.baseFriendlyName = baseFriendlyName;
	}

	public String getFriendlyNameParameter() {
		return friendlyNameParameter;
	}

	public void setFriendlyNameParameter(String friendlyNameParameter) {
		this.friendlyNameParameter = friendlyNameParameter;
	}

	@JsonIgnore
	public String getUrn() {
		return this.namespace + ':' + this.friendlyName;
	}

	@Override
	public String toString() {
		return "PerunAttributeDefinition{" +
				"id=" + this.getId() +
				", friendlyName='" + friendlyName + '\'' +
				", namespace='" + namespace + '\'' +
				", description='" + description + '\'' +
				", type='" + type + '\'' +
				", displayName='" + displayName + '\'' +
				", writable=" + writable +
				", unique=" + unique +
				", entity='" + entity + '\'' +
				", beanName='" + beanName + '\'' +
				", baseFriendlyName='" + baseFriendlyName + '\'' +
				", friendlyNameParameter='" + friendlyNameParameter + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		PerunAttributeDefinition that = (PerunAttributeDefinition) o;
		return writable == that.writable &&
				unique == that.unique &&
				super.equals(that) &&
				Objects.equals(friendlyName, that.friendlyName) &&
				Objects.equals(namespace, that.namespace) &&
				Objects.equals(description, that.description) &&
				Objects.equals(type, that.type) &&
				Objects.equals(displayName, that.displayName) &&
				Objects.equals(entity, that.entity) &&
				Objects.equals(baseFriendlyName, that.baseFriendlyName) &&
				Objects.equals(friendlyNameParameter, that.friendlyNameParameter);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), friendlyName, namespace, description, type, displayName, writable,
				unique, entity, baseFriendlyName, friendlyNameParameter);
	}

	protected ObjectNode toJson() {
		ObjectNode node = JsonNodeFactory.instance.objectNode();

		node.put("id", super.getId());
		node.put("friendlyName", friendlyName);
		node.put("namespace", namespace);
		node.put("type", type);
		node.put("displayName", displayName);
		node.put("writable", writable);
		node.put("unique", unique);
		node.put("entity", entity);
		node.put("beanName", beanName);
		node.put("baseFriendlyName", baseFriendlyName);
		node.put("friendlyName", friendlyName);
		node.put("friendlyNameParameter", friendlyNameParameter);

		return node;
	}
}
