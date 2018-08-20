package cz.muni.ics.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import cz.muni.ics.oidc.claims.ClaimValueModifier;
import cz.muni.ics.oidc.claims.ClaimValueModifierInitContext;
import org.mitre.openid.connect.model.Address;
import org.mitre.openid.connect.model.DefaultAddress;
import org.mitre.openid.connect.model.UserInfo;
import org.mitre.openid.connect.repository.UserInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Provides data about a user.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunUserInfoRepository implements UserInfoRepository {

	private final static Logger log = LoggerFactory.getLogger(PerunUserInfoRepository.class);

	private static final String MODIFIER_CLASS = ".modifierClass";

	private PerunConnector perunConnector;
	private Properties properties;

	private String subAttribute;
	private ClaimValueModifier subModifier;
	private String preferredUsernameAttribute;
	private String emailAttribute;
	private String addressAttribute;
	private String phoneAttribute;
	private String zoneinfoAttribute;
	private String localeAttribute;
	private List<String> customClaimNames;
	private List<PerunCustomClaimDefinition> customClaims = new ArrayList<>();

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public void setPerunConnector(PerunConnector perunConnector) {
		this.perunConnector = perunConnector;
	}

	public void setSubAttribute(String subAttribute) {
		this.subAttribute = subAttribute;
	}

	public void setPreferredUsernameAttribute(String preferredUsernameAttribute) {
		this.preferredUsernameAttribute = preferredUsernameAttribute;
	}

	public void setEmailAttribute(String emailAttribute) {
		this.emailAttribute = emailAttribute;
	}

	public void setAddressAttribute(String addressAttribute) {
		this.addressAttribute = addressAttribute;
	}

	public void setPhoneAttribute(String phoneAttribute) {
		this.phoneAttribute = phoneAttribute;
	}

	public void setZoneinfoAttribute(String zoneinfoAttribute) {
		this.zoneinfoAttribute = zoneinfoAttribute;
	}

	public void setLocaleAttribute(String localeAttribute) {
		this.localeAttribute = localeAttribute;
	}

	public void setCustomClaimNames(List<String> customClaimNames) {
		this.customClaimNames = customClaimNames;
	}

	@PostConstruct
	public void postInit() {
		log.debug("trying to load modifier for attribute.openid.sub");
		subModifier = loadClaimValueModifier("attribute.openid.sub");
		//custom claims
		this.customClaims = new ArrayList<>(customClaimNames.size());
		for (String claim : customClaimNames) {
			String propertyPrefix = "custom.claim." + claim;
			//get scope
			String scopeProperty = propertyPrefix + ".scope";
			String scope = properties.getProperty(scopeProperty);
			if (scope == null) {
				log.error("property {} not found, skipping custom claim {}", scopeProperty, claim);
				continue;
			}
			//get Perun user attribute from which to get value
			String attributeProperty = propertyPrefix + ".attribute";
			String perunAttribute = properties.getProperty(attributeProperty);
			if (perunAttribute == null) {
				log.error("property {} not found, skipping custom claim {}", attributeProperty, claim);
				continue;
			}
			//optional claim value modifier
			ClaimValueModifier claimValueModifier = loadClaimValueModifier(propertyPrefix);
			//add claim definition
			customClaims.add(new PerunCustomClaimDefinition(scope, claim, perunAttribute, claimValueModifier));
		}
	}

	private ClaimValueModifier loadClaimValueModifier(String propertyPrefix) {
		String modifierClass = properties.getProperty(propertyPrefix + MODIFIER_CLASS);
		if (modifierClass != null) {
			try {
				Class<?> rawClazz = Class.forName(modifierClass);
				if (!ClaimValueModifier.class.isAssignableFrom(rawClazz)) {
					log.error("modifier class {} does not extend ClaimValueModifier", modifierClass);
					return null;
				}
				@SuppressWarnings("unchecked") Class<ClaimValueModifier> clazz = (Class<ClaimValueModifier>) rawClazz;
				Constructor<ClaimValueModifier> constructor = clazz.getConstructor(ClaimValueModifierInitContext.class);
				ClaimValueModifierInitContext ctx = new ClaimValueModifierInitContext(propertyPrefix, properties);
				ClaimValueModifier claimValueModifier = constructor.newInstance(ctx);
				log.info("loaded modifier '{}' for {}", claimValueModifier, propertyPrefix);
				return claimValueModifier;
			} catch (ClassNotFoundException e) {
				log.error("modifier class {} not found", modifierClass);
				return null;
			} catch (NoSuchMethodException e) {
				log.error("modifier class {} does not have proper constructor", modifierClass);
				return null;
			} catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
				log.error("cannot instantiate " + modifierClass, e);
				log.error("modifier class {} cannot be instantiated", modifierClass);
				return null;
			}
		} else {
			log.debug("property {} not found, skipping", propertyPrefix + MODIFIER_CLASS);
			return null;
		}
	}

	List<PerunCustomClaimDefinition> getCustomClaims() {
		return customClaims;
	}

	@Override
	public UserInfo getByUsername(String username) {
		log.trace("getByUsername({})", username);
		try {
			return cache.get(username);
		} catch (UncheckedExecutionException | ExecutionException e) {
			log.error("cannot get user from cache", e);
			return null;
		}
	}

	@Override
	public UserInfo getByEmailAddress(String email) {
		log.trace("getByEmailAddress({})", email);
		throw new RuntimeException("PerunUserInfoRepository.getByEmailAddress() not implemented");
	}

	public PerunUserInfoRepository() {
		this.cache = CacheBuilder.newBuilder()
				.maximumSize(100)
				.expireAfterAccess(30, TimeUnit.SECONDS)
				.build(cacheLoader);
	}

	private LoadingCache<String, UserInfo> cache;

	@SuppressWarnings("FieldCanBeLocal")
	private CacheLoader<String, UserInfo> cacheLoader = new CacheLoader<String, UserInfo>() {
		@Override
		public UserInfo load(String username) {
			log.trace("load({})", username);
			PerunUserInfo ui = new PerunUserInfo();
			JsonNode result = perunConnector.getUserAttributes(Long.parseLong(username));
			//process
			RichUser richUser = new RichUser(result);

			String sub = richUser.get(subAttribute);
			if (sub == null) {
				throw new RuntimeException("cannot get sub from attribute " + subAttribute + " for username " + username);
			}
			if (subModifier != null) {
				//transform sub value
				sub = subModifier.modify(sub);
			}

			ui.setId(Long.parseLong(username));
			ui.setSub(sub); // Subject - Identifier for the End-User at the Issuer.

			ui.setPreferredUsername(richUser.get(preferredUsernameAttribute)); // Shorthand name by which the End-User wishes to be referred to at the RP
			ui.setGivenName(richUser.get("urn:perun:user:attribute-def:core:firstName")); //  Given name(s) or first name(s) of the End-User
			ui.setFamilyName(richUser.get("urn:perun:user:attribute-def:core:lastName")); // Surname(s) or last name(s) of the End-User
			ui.setMiddleName(richUser.get("urn:perun:user:attribute-def:core:middleName")); //  Middle name(s) of the End-User
			ui.setName(richUser.get("urn:perun:user:attribute-def:core:displayName")); // End-User's full name
			//ui.setNickname(); // Casual name of the End-User
			//ui.setProfile(); //  URL of the End-User's profile page.
			//ui.setPicture(); // URL of the End-User's profile picture.
			//ui.setWebsite(); // URL of the End-User's Web page or blog.
			ui.setEmail(richUser.get(emailAttribute)); // End-User's preferred e-mail address.
			//ui.setEmailVerified(true); // True if the End-User's e-mail address has been verified
			//ui.setGender("male"); // End-User's gender. Values defined by this specification are female and male.
			//ui.setBirthdate("1975-01-01");//End-User's birthday, represented as an ISO 8601:2004 [ISO8601‑2004] YYYY-MM-DD format.
			ui.setZoneinfo(richUser.get(zoneinfoAttribute));//String from zoneinfo [zoneinfo] time zone database, For example, Europe/Paris
			ui.setLocale(richUser.get(localeAttribute)); //  For example, en-US or fr-CA.
			ui.setPhoneNumber(richUser.get(phoneAttribute)); //[E.164] is RECOMMENDED as the format, for example, +1 (425) 555-121
			//ui.setPhoneNumberVerified(true); // True if the End-User's phone number has been verified
			//ui.setUpdatedTime(Long.toString(System.currentTimeMillis()/1000L));// value is a JSON number representing the number of seconds from 1970-01-01T0:0:0Z as measured in UTC until the date/time
			Address address = new DefaultAddress();
			address.setFormatted(richUser.get(addressAttribute));
			//address.setStreetAddress("Šumavská 15");
			//address.setLocality("Brno");
			//address.setPostalCode("61200");
			//address.setCountry("Czech Republic");
			ui.setAddress(address);
			//custom claims
			for (PerunCustomClaimDefinition pccd : customClaims) {
				JsonNode claimInJson = richUser.getJson(pccd.getPerunAttributeName());
				ClaimValueModifier claimValueModifier = pccd.getClaimValueModifier();
				if (claimValueModifier != null) {
					log.debug("modifying values of claim '{}' using {}", pccd.getClaim(), claimValueModifier);
					//transform values
					if (claimInJson.isTextual()) {
						//transform a simple string value
						claimInJson = TextNode.valueOf(claimValueModifier.modify(claimInJson.asText()));
					} else if (claimInJson.isArray()) {
					    claimInJson = claimInJson.deepCopy();
						//transform all strings in an array
						ArrayNode arrayNode = (ArrayNode) claimInJson;
						for (int i = 0; i < arrayNode.size(); i++) {
							JsonNode item = arrayNode.get(i);
							if (item.isTextual()) {
								String original = item.asText();
								String modified = claimValueModifier.modify(original);
								log.debug("transforming value '{}' to '{}'", original, modified);
								arrayNode.set(i, TextNode.valueOf(modified));
							}
						}
					}
				}
				ui.getCustomClaims().put(pccd.getClaim(), claimInJson);
			}
			log.trace("user loaded");
			return ui;
		}
	};



	private static class RichUser {
		Map<String, JsonNode> map = new HashMap<>();

		RichUser(JsonNode jsonNode) {
			log.trace("parsing user attributes");
			for (JsonNode ua : jsonNode.path("userAttributes")) {
				String attributeName = ua.path("namespace").asText() + ":" + ua.path("friendlyName").asText();
				JsonNode value = ua.path("value");
				log.trace("got user attribute {} = {}", attributeName, value);
				map.put(attributeName, value);
			}
		}

		JsonNode getJson(String s) {
			return map.get(s);
		}

		String get(String s) {
			JsonNode jsonNode = map.get(s);
			if (jsonNode == null) return null;
			if (jsonNode.isNull()) return null;
			return jsonNode.asText();
		}
	}
}
