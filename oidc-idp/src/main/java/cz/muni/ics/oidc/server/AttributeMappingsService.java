package cz.muni.ics.oidc.server;


import cz.muni.ics.oidc.models.AttributeMapping;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.models.enums.PerunAttrValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service providing methods to use AttributeMapping objects when fetching attributes.
 *
 * Names for the attribute are configured in configuration file in the following way:
 * (replace [entity] with one of user|vo|facility|resource|group)
 * <ul>
 *     <li><b>[entity].attribute_names.customList</b> - comma separated list of names for attributes</li>
 * </ul>
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class AttributeMappingsService {

	private static final Logger log = LoggerFactory.getLogger(AttributeMappingsService.class);

	private static final String LDAP_NAME = ".mapping.ldap";
	private static final String RPC_NAME = ".mapping.rpc";
	private static final String TYPE = ".type";
	private static final String SEPARATOR = ".separator";

	private final Map<String, AttributeMapping> attributeMap;

	public AttributeMappingsService(String[] attrIdentifiersFixed, String[] attrIdentifiersCustom,
									Properties attrMappingsProperties) {
		attributeMap = new HashMap<>();

		if (attrIdentifiersFixed != null) {
			initAttrMappings(attrIdentifiersFixed, attrMappingsProperties);
		}

		if (attrIdentifiersCustom != null) {
			initAttrMappings(attrIdentifiersCustom, attrMappingsProperties);
		}
	}

	public void add(AttributeMapping attribute) {
		attributeMap.put(attribute.getIdentifier(), attribute);
	}

	public AttributeMapping getByName(String name) {
		return attributeMap.getOrDefault(name, null);
	}

	public List<AttributeMapping> getMappingsForAttrNames(String... attrNames) {

		List<AttributeMapping> mappings = new ArrayList<>();
		String[] attrNamesArr;

		if (attrNames != null && (attrNamesArr = attrNames.clone()) != null) {
			mappings = new ArrayList<>();
			for (String attrName : attrNamesArr) {
				if (attributeMap.containsKey(attrName)) {
					mappings.add(attributeMap.get(attrName));
				}
			}
		}

		return mappings;
	}

	public Set<AttributeMapping> getMappingsForAttrNames(Collection<String> attrNames) {
		Set<AttributeMapping> mappings = new HashSet<>();

		if (attrNames != null) {
			for (String attrName : attrNames) {
				if (attributeMap.containsKey(attrName)) {
					mappings.add(attributeMap.get(attrName));
				}
			}
		}
		return mappings;
	}

	public List<AttributeMapping> getAttributeMappingsToFetch(Map<String, PerunAttributeValue> fetched, Collection<String> allToFetch) {
		if (allToFetch == null) {
			throw new IllegalArgumentException("AllToFetch cannot be null");
		}

		List<AttributeMapping> mappings;

		if (fetched == null || fetched.keySet().isEmpty()) {
			mappings = allToFetch.stream()
					.map(attributeMap::get)
					.collect(Collectors.toList());
		} else {
			mappings = allToFetch.stream()
					.filter(attrKey -> (!fetched.containsKey(attrKey) || fetched.get(attrKey) == null))
					.map(attributeMap::get)
					.collect(Collectors.toList());
		}

		return mappings;
	}

	private void initAttrMappings(String[] attributeIdentifiers, Properties attrProperties) {
		if (attributeIdentifiers == null || attributeIdentifiers.length <= 0) {
			return;
		}

		for (String identifier : attributeIdentifiers) {
			if (identifier == null || identifier.isEmpty()) {
				continue;
			}
			AttributeMapping am = initAttrMapping(identifier, attrProperties);
			log.trace("Initialized attributeMapping: {}", am);
			attributeMap.put(am.getIdentifier(), am);
		}
	}

	private AttributeMapping initAttrMapping(String attrIdentifier, Properties attrProperties) {
		String rpcIdentifier = attrProperties.getProperty(attrIdentifier + RPC_NAME);
		String ldapIdentifier = attrProperties.getProperty(attrIdentifier + LDAP_NAME);
		if (ldapIdentifier != null && ldapIdentifier.trim().isEmpty()) {
			ldapIdentifier = null;
		}

		String type = attrProperties.getProperty(attrIdentifier + TYPE);
		String separator = "";
		if (PerunAttrValueType.MAP_KEY_VALUE.equals(PerunAttrValueType.parse(type))) {
			separator = attrProperties.getProperty(attrIdentifier + SEPARATOR);
		}

		return new AttributeMapping(attrIdentifier, rpcIdentifier, ldapIdentifier, type, separator);
	}

}

