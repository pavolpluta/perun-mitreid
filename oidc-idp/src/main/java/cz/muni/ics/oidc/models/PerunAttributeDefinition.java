package cz.muni.ics.oidc.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
		this.friendlyName = friendlyName;
		this.namespace = namespace;
		this.description = description;
		this.type = type;
		this.displayName = displayName;
		this.writable = writable;
		this.unique = unique;
		this.entity = entity;
		this.baseFriendlyName = baseFriendlyName;
		this.friendlyNameParameter = friendlyNameParameter;
	}

	public String getFriendlyName() {
		return friendlyName;
	}

	public void setFriendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
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
		this.entity = entity;
	}

	public String getBeanName() {
		return beanName;
	}

	public String getBaseFriendlyName() {
		return baseFriendlyName;
	}

	public void setBaseFriendlyName(String baseFriendlyName) {
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
}
