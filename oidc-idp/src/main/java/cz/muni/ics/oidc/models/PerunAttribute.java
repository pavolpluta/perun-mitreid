package cz.muni.ics.oidc.models;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Perun Attribute model
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class PerunAttribute extends PerunAttributeDefinition {

	private final static String STRING_TYPE = "java.lang.String";
	private final static String INTEGER_TYPE = "java.lang.Long";
	private final static String BOOLEAN_TYPE = "java.lang.Boolean";
	private final static String ARRAY_TYPE = "java.util.ArrayList";
	private final static String MAP_TYPE = "java.util.LinkedHashMap";
	private final static String LARGE_STRING_TYPE = "java.lang.LargeString";
	private final static String LARGE_ARRAY_LIST_TYPE = "java.lang.LargeArrayList";

	private Object value;
	private String valueCreatedAt;
	private String valueModifiedAt;

	public PerunAttribute() { }

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

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public void setValue(String type, JsonNode value) {
		if (value.isNull() || "null".equalsIgnoreCase(value.asText())) {
			this.value = null;
			return;
		}

		switch (type) {
			case STRING_TYPE:
			case LARGE_STRING_TYPE: {
				this.value = value.asText();
			} break;
			case INTEGER_TYPE: {
				this.value = value.asLong();
			} break;
			case BOOLEAN_TYPE: {
				this.value = value.asBoolean();
			} break;
			case ARRAY_TYPE:
			case LARGE_ARRAY_LIST_TYPE: {
				List<String> arrValue = new ArrayList<>();
				for (int i = 0; i < value.size(); i++) {
					JsonNode subValue = value.get(i);
					arrValue.add(subValue.asText());
				}
				this.value = arrValue;
			} break;
			case MAP_TYPE: {
				Map<String, String> mapValue = new HashMap<>();
				Iterator<String> keysIt = value.fieldNames();
				while (keysIt.hasNext()) {
					String key = keysIt.next();
					String val = value.get(key).asText();
					mapValue.put(key, val);
				}
				this.value = mapValue;
			} break;
			default:
				this.value = null;
		}
	}

	public String valueAsString() {
		if ((STRING_TYPE.equals(super.getType()) || LARGE_STRING_TYPE.equals(super.getType()))
				&& value instanceof String) {
			return (String) value;
		}

		return null;
	}

	public Long valueAsLong() {
		if (INTEGER_TYPE.equals(super.getType()) && value instanceof Long) {
			return (Long) value;
		}

		return null;
	}

	public boolean valueAsBoolean() {
		if (BOOLEAN_TYPE.equals(super.getType()) && value instanceof Boolean) {
			return (Boolean) value;
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	public List<String> valueAsList() {
		if ((ARRAY_TYPE.equals(super.getType()) || LARGE_ARRAY_LIST_TYPE.equals(super.getType()))
			&& value instanceof List) {
			return (List<String>) value;
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> valueAsMap() {
		if (MAP_TYPE.equals(super.getType()) && value instanceof List) {
			return (Map<String, String>) value;
		}

		return null;
	}

	@Override
	public String toString() {
		return "PerunAttribute{" +
				super.toString() +
				", value=" + value +
				'}';
	}
}
