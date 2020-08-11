package cz.muni.ics.oidc.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;

/**
 * Perun Attribute model
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class PerunAttribute extends PerunAttributeDefinition {

	private PerunAttributeValue value;
	private String valueCreatedAt;
	private String valueModifiedAt;

	public PerunAttributeValue getValue() {
		return value;
	}

	public void setValue(PerunAttributeValue value) {
		this.value = value;
	}

	public String getValueCreatedAt() {
		return valueCreatedAt;
	}

	public void setValueCreatedAt(String valueCreatedAt) {
		this.valueCreatedAt = valueCreatedAt;
	}

	public String getValueModifiedAt() {
		return valueModifiedAt;
	}

	public void setValueModifiedAt(String valueModifiedAt) {
		this.valueModifiedAt = valueModifiedAt;
	}

	@Override
	@JsonIgnore
	public String getUrn() {
		return super.getUrn();
	}

	@Override
	public String toString() {
		return "PerunAttribute{" +
				"value=" + value +
				", valueCreatedAt='" + valueCreatedAt + '\'' +
				", valueModifiedAt='" + valueModifiedAt + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		PerunAttribute attribute = (PerunAttribute) o;
		return Objects.equals(value, attribute.value) &&
				Objects.equals(valueCreatedAt, attribute.valueCreatedAt) &&
				Objects.equals(valueModifiedAt, attribute.valueModifiedAt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), value, valueCreatedAt, valueModifiedAt);
	}

	public ObjectNode toJson() {
		ObjectNode defInJson = super.toJson();
		defInJson.set("value", value.valueAsJson());

		return defInJson;
	}
}
