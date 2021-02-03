package cz.muni.ics.oidc.server.userInfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.models.PerunAttributeValueAwareModel;
import cz.muni.ics.oidc.server.adapters.PerunAdapter;
import cz.muni.ics.oidc.server.claims.ClaimModifier;
import cz.muni.ics.oidc.server.claims.ClaimModifierInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSource;
import cz.muni.ics.oidc.server.claims.ClaimSourceInitContext;
import cz.muni.ics.oidc.server.claims.ClaimSourceProduceContext;
import cz.muni.ics.oidc.server.claims.PerunCustomClaimDefinition;
import cz.muni.ics.oidc.server.claims.sources.PerunAttributeClaimSource;
import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import org.mitre.jwt.signer.service.JWTSigningAndValidationService;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.mitre.openid.connect.model.Address;
import org.mitre.openid.connect.model.DefaultAddress;
import org.mitre.openid.connect.model.UserInfo;
import org.mitre.openid.connect.service.UserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service called from UserInfoEndpoint and other places to get UserInfo.
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class PerunUserInfoService implements UserInfoService {

	private static final Logger log = LoggerFactory.getLogger(PerunUserInfoService.class);

	private static final String CUSTOM_CLAIM = "custom.claim.";
	private static final String SOURCE = ".source";
	private static final String CLASS = ".class";
	private static final String MODIFIER = ".modifier";

	@Autowired
	private ClientDetailsEntityService clientService;

	@Autowired
	private JWTSigningAndValidationService jwtService;

	@Autowired
	private PerunOidcConfig perunOidcConfig;

	private List<String> forceRegenerateUserinfoCustomClaims = new ArrayList<>();
	private List<String> forceRegenerateUserinfoStandardClaims = new ArrayList<>();

	private final LoadingCache<UserClientPair, UserInfo> cache;

	private PerunAdapter perunAdapter;
	private Properties properties;

	private String subAttribute;
	private ClaimModifier subModifier;
	private String preferredUsernameAttribute;
	private String givenNameAttribute;
	private String familyNameAttribute;
	private String middleNameAttribute;
	private String fullNameAttribute;
	private String emailAttribute;
	private String addressAttribute;
	private String phoneAttribute;
	private String zoneinfoAttribute;
	private String localeAttribute;
	private List<String> customClaimNames;
	private List<PerunCustomClaimDefinition> customClaims = new ArrayList<>();
	private UserInfoModifierContext userInfoModifierContext;

	private final Set<String> userAttrNames = new HashSet<>();

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public void setPerunAdapter(PerunAdapter perunAdapter) {
		this.perunAdapter = perunAdapter;
	}

	public void setSubAttribute(String subAttribute) {
		if (this.subAttribute != null) {
			userAttrNames.remove(this.subAttribute);
		}
		userAttrNames.add(subAttribute);
		this.subAttribute = subAttribute;
	}

	public void setPreferredUsernameAttribute(String preferredUsernameAttribute) {
		if (this.preferredUsernameAttribute != null) {
			userAttrNames.remove(this.preferredUsernameAttribute);
		}
		userAttrNames.add(preferredUsernameAttribute);
		this.preferredUsernameAttribute = preferredUsernameAttribute;
	}

	public void setGivenNameAttribute(String givenNameAttribute) {
		if (this.givenNameAttribute != null) {
			userAttrNames.remove(this.givenNameAttribute);
		}
		userAttrNames.add(givenNameAttribute);
		this.givenNameAttribute = givenNameAttribute;
	}

	public void setFamilyNameAttribute(String familyNameAttribute) {
		if (this.familyNameAttribute != null) {
			userAttrNames.remove(this.familyNameAttribute);
		}
		userAttrNames.add(familyNameAttribute);
		this.familyNameAttribute = familyNameAttribute;
	}

	public void setMiddleNameAttribute(String middleNameAttribute) {
		if (this.middleNameAttribute != null) {
			userAttrNames.remove(this.middleNameAttribute);
		}
		userAttrNames.add(middleNameAttribute);
		this.middleNameAttribute = middleNameAttribute;
	}

	public void setFullNameAttribute(String fullNameAttribute) {
		if (this.fullNameAttribute != null) {
			userAttrNames.remove(this.fullNameAttribute);
		}
		userAttrNames.add(fullNameAttribute);
		this.fullNameAttribute = fullNameAttribute;
	}

	public void setEmailAttribute(String emailAttribute) {
		if (this.emailAttribute != null) {
			userAttrNames.remove(this.emailAttribute);
		}
		userAttrNames.add(emailAttribute);
		this.emailAttribute = emailAttribute;
	}

	public void setAddressAttribute(String addressAttribute) {
		if (this.addressAttribute != null) {
			userAttrNames.remove(this.addressAttribute);
		}
		userAttrNames.add(addressAttribute);
		this.addressAttribute = addressAttribute;
	}

	public void setPhoneAttribute(String phoneAttribute) {
		if (this.phoneAttribute != null) {
			userAttrNames.remove(this.phoneAttribute);
		}
		userAttrNames.add(phoneAttribute);
		this.phoneAttribute = phoneAttribute;
	}

	public void setZoneinfoAttribute(String zoneinfoAttribute) {
		if (this.zoneinfoAttribute != null) {
			userAttrNames.remove(this.zoneinfoAttribute);
		}
		userAttrNames.add(zoneinfoAttribute);
		this.zoneinfoAttribute = zoneinfoAttribute;
	}

	public void setLocaleAttribute(String localeAttribute) {
		if (this.localeAttribute != null) {
			userAttrNames.remove(this.localeAttribute);
		}
		userAttrNames.add(localeAttribute);
		this.localeAttribute = localeAttribute;
	}

	public void setCustomClaimNames(List<String> customClaimNames) {
		if (this.customClaimNames != null) {
			userAttrNames.removeAll(this.customClaimNames);
		}
		userAttrNames.addAll(customClaimNames);
		this.customClaimNames = customClaimNames;
	}

	public void setForceRegenerateUserinfoCustomClaims(String[] claims) {
		this.forceRegenerateUserinfoCustomClaims = Arrays.asList(claims);
	}

	public void setForceRegenerateUserinfoStandardClaims(String[] claims) {
		this.forceRegenerateUserinfoStandardClaims = Arrays.asList(claims);
	}

	public List<PerunCustomClaimDefinition> getCustomClaims() {
		return customClaims;
	}

	@PostConstruct
	public void postInit() {
		log.debug("trying to load modifier for attribute.openid.sub");
		subModifier = loadClaimValueModifier("sub", "attribute.openid.sub.modifier");
		//custom claims
		this.customClaims = new ArrayList<>(customClaimNames.size());
		for (String claim : customClaimNames) {
			String propertyBase = CUSTOM_CLAIM + claim;
			//get scope
			String scopeProperty = propertyBase + ".scope";
			String scope = properties.getProperty(scopeProperty);
			if (scope == null) {
				log.error("property {} not found, skipping custom claim {}", scopeProperty, claim);
				continue;
			}
			//get ClaimSource
			ClaimSource claimSource = loadClaimSource(claim, propertyBase + SOURCE);
			//optional claim value modifier
			ClaimModifier claimModifier = loadClaimValueModifier(claim, propertyBase + MODIFIER);
			//add claim definition
			customClaims.add(new PerunCustomClaimDefinition(scope, claim, claimSource, claimModifier));
		}

		this.userInfoModifierContext = new UserInfoModifierContext(properties, perunAdapter);
	}

	@Override
	public UserInfo getByUsernameAndClientId(String username, String clientId) {
		ClientDetailsEntity client = clientService.loadClientByClientId(clientId);
		if (client == null) {
			log.warn("did not found client with id {}", clientId);
			return null;
		}

		PerunUserInfo userInfo;
		try {
			UserClientPair cacheKey = new UserClientPair(username, clientId, client);
			userInfo = (PerunUserInfo) cache.get(cacheKey);
			if (!checkStandardClaims(userInfo) || !checkCustomClaims(userInfo)) {
				log.info("Some required claim is null, regenerate userInfo");
				cache.invalidate(cacheKey);
				userInfo = (PerunUserInfo) cache.get(cacheKey);
			}
			log.debug("loaded UserInfo from cache for '{}'/'{}'", userInfo.getName(), client.getClientName());
			userInfo = userInfoModifierContext.modify(userInfo, clientId);
		} catch (ExecutionException e) {
			log.error("cannot get user from cache", e);
			return null;
		}

		return userInfo;
	}

	@Override
	public UserInfo getByUsername(String username) {
		PerunUserInfo userInfo;
		try {
			UserClientPair cacheKey = new UserClientPair(username);
			userInfo = (PerunUserInfo) cache.get(cacheKey);
			if (!checkStandardClaims(userInfo) || !checkCustomClaims(userInfo)) {
				log.info("Some required claim is null, regenerate userInfo");
				cache.invalidate(cacheKey);
				userInfo = (PerunUserInfo) cache.get(cacheKey);
			}
			log.debug("loaded UserInfo from cache for '{}'", userInfo.getName());
			userInfo = userInfoModifierContext.modify(userInfo, null);
		} catch (UncheckedExecutionException | ExecutionException e) {
			log.error("cannot get user from cache", e);
			return null;
		}

		return userInfo;
	}

	@Override
	public UserInfo getByEmailAddress(String email) {
		throw new RuntimeException("PerunUserInfoService.getByEmailAddress() not implemented");
	}

	public PerunUserInfoService() {
		this.cache = CacheBuilder.newBuilder()
				.maximumSize(100)
				.expireAfterAccess(60, TimeUnit.SECONDS)
				.build(cacheLoader);
	}

	private ClaimModifier loadClaimValueModifier(String claimName, String propertyPrefix) {
		String modifierClass = properties.getProperty(propertyPrefix + CLASS);
		if (!StringUtils.hasText(modifierClass)) {
			log.warn("{} - failed to initialized claim modifier: no class has ben configured", claimName);
			return null;
		}
		log.trace("{} - loading ClaimModifier class '{}'", claimName, modifierClass);

		try {
			Class<?> rawClazz = Class.forName(modifierClass);
			if (!ClaimModifier.class.isAssignableFrom(rawClazz)) {
				log.warn("{} - failed to initialized claim modifier: class '{}' does not extend ClaimModifier",
						claimName, modifierClass);
				return null;
			}
			@SuppressWarnings("unchecked") Class<ClaimModifier> clazz = (Class<ClaimModifier>) rawClazz;
			Constructor<ClaimModifier> constructor = clazz.getConstructor(ClaimModifierInitContext.class);
			ClaimModifierInitContext ctx = new ClaimModifierInitContext(propertyPrefix, properties, claimName);
			return constructor.newInstance(ctx);
		} catch (ClassNotFoundException e) {
			log.warn("{} - failed to initialize claim modifier: class '{}' was not found", claimName, modifierClass);
			log.trace("{} - details:", claimName, e);
			return null;
		} catch (NoSuchMethodException e) {
			log.warn("{} - failed to initialize claim modifier: class '{}' does not have proper constructor",
					claimName, modifierClass);
			log.trace("{} - details:", claimName, e);
			return null;
		} catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
			log.warn("{} - failed to initialize claim modifier: class '{}' cannot be instantiated", claimName, modifierClass);
			log.trace("{} - details:", claimName, e);
			return null;
		}
	}

	private ClaimSource loadClaimSource(String claimName, String propertyPrefix) {
		String sourceClass = properties.getProperty(propertyPrefix + CLASS, PerunAttributeClaimSource.class.getName());
		if (!StringUtils.hasText(sourceClass)) {
			log.warn("{} - failed to initialized claim source: no class has ben configured", claimName);
			return null;
		}

		log.trace("{} - loading ClaimSource class '{}'", claimName, sourceClass);

		try {
			Class<?> rawClazz = Class.forName(sourceClass);
			if (!ClaimModifier.class.isAssignableFrom(rawClazz)) {
				log.warn("{} - failed to initialized claim source: class '{}' does not extend ClaimSource",
						claimName, sourceClass);
				return null;
			}
			@SuppressWarnings("unchecked") Class<ClaimSource> clazz = (Class<ClaimSource>) rawClazz;
			Constructor<ClaimSource> constructor = clazz.getConstructor(ClaimSourceInitContext.class);
			ClaimSourceInitContext ctx = new ClaimSourceInitContext(perunOidcConfig, jwtService, propertyPrefix,
					properties, claimName);
			return constructor.newInstance(ctx);
		} catch (ClassNotFoundException e) {
			log.warn("{} - failed to initialize claim source: class '{}' was not found", claimName, sourceClass);
			log.trace("{} - details:", claimName, e);
			return null;
		} catch (NoSuchMethodException e) {
			log.warn("{} - failed to initialize claim source: class '{}' does not have proper constructor",
					claimName, sourceClass);
			log.trace("{} - details:", claimName, e);
			return null;
		} catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
			log.warn("{} - failed to initialize claim source: class '{}' cannot be instantiated", claimName, sourceClass);
			log.trace("{} - details:", claimName, e);
			return null;
		}
	}

	private static class UserClientPair {
		private final long userId;
		private String clientId;
		private ClientDetailsEntity client;

		UserClientPair(String userId) {
			this.userId = Long.parseLong(userId);
		}

		UserClientPair(String userId, String clientId, ClientDetailsEntity client) {
			this.userId = Long.parseLong(userId);
			this.clientId = clientId;
			this.client = client;
		}

		public long getUserId() {
			return userId;
		}

		public String getClientId() {
			return clientId;
		}

		public ClientDetailsEntity getClient() {
			return client;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			UserClientPair that = (UserClientPair) o;
			return userId == that.userId &&
					Objects.equals(clientId, that.clientId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, clientId);
		}

		@Override
		public String toString() {
			return "(" + "userId=" + userId + "," + (client == null ? "null" : "client=" + clientId + " '" + client.getClientName()) + "')";
		}
	}

	@SuppressWarnings("FieldCanBeLocal")
	private CacheLoader<UserClientPair, UserInfo> cacheLoader = new CacheLoader<UserClientPair, UserInfo>() {
		@Override
		public UserInfo load(UserClientPair pair) {
			log.debug("load({}) ... populating cache for the key", pair);
			PerunUserInfo ui = new PerunUserInfo();
			long perunUserId = pair.getUserId();

			Map<String, PerunAttributeValue> userAttributeValues = perunAdapter.getUserAttributeValues(perunUserId, userAttrNames);

			if (shouldFillAttrs(userAttributeValues)) {
				List<String> attrNames = userAttributeValues.entrySet()
						.stream()
						.filter(entry -> (null == entry.getValue() || entry.getValue().isNullValue()))
						.map(Map.Entry::getKey)
						.collect(Collectors.toList());
				Map<String, PerunAttributeValue> missingAttrs = perunAdapter.getAdapterFallback()
						.getUserAttributeValues(perunUserId, attrNames);
				for (Map.Entry<String, PerunAttributeValue> entry : missingAttrs.entrySet()) {
					userAttributeValues.put(entry.getKey(), entry.getValue());
				}
			}

			String sub = userAttributeValues.get(subAttribute).valueAsString();
			if (sub == null) {
				throw new RuntimeException("cannot get sub from attribute " + subAttribute + " for username " + perunUserId);
			}
			if (subModifier != null) {
				//transform sub value
				sub = subModifier.modify(sub);
			}

			ui.setId(perunUserId);
			ui.setSub(sub); // Subject - Identifier for the End-User at the Issuer.

			ui.setPreferredUsername(userAttributeValues.get(preferredUsernameAttribute).valueAsString()); // Shorthand name by which the End-User wishes to be referred to at the RP
			ui.setGivenName(userAttributeValues.get(givenNameAttribute).valueAsString()); //  Given name(s) or first name(s) of the End-User
			ui.setFamilyName(userAttributeValues.get(familyNameAttribute).valueAsString()); // Surname(s) or last name(s) of the End-User
			ui.setMiddleName(userAttributeValues.get(middleNameAttribute).valueAsString()); //  Middle name(s) of the End-User
			ui.setName(userAttributeValues.get(fullNameAttribute).valueAsString()); // End-User's full name
			//ui.setNickname(); // Casual name of the End-User
			//ui.setProfile(); //  URL of the End-User's profile page.
			//ui.setPicture(); // URL of the End-User's profile picture.
			//ui.setWebsite(); // URL of the End-User's Web page or blog.
			ui.setEmail(userAttributeValues.get(emailAttribute).valueAsString()); // End-User's preferred e-mail address.
			//ui.setEmailVerified(true); // True if the End-User's e-mail address has been verified
			//ui.setGender("male"); // End-User's gender. Values defined by this specification are female and male.
			//ui.setBirthdate("1975-01-01");//End-User's birthday, represented as an ISO 8601:2004 [ISO8601‑2004] YYYY-MM-DD format.
			ui.setZoneinfo(userAttributeValues.get(zoneinfoAttribute).valueAsString());//String from zoneinfo [zoneinfo] time zone database, For example, Europe/Paris
			ui.setLocale(userAttributeValues.get(localeAttribute).valueAsString()); //  For example, en-US or fr-CA.
			ui.setPhoneNumber(userAttributeValues.get(phoneAttribute).valueAsString()); //[E.164] is RECOMMENDED as the format, for example, +1 (425) 555-121
			//ui.setPhoneNumberVerified(true); // True if the End-User's phone number has been verified
			//ui.setUpdatedTime(Long.toString(System.currentTimeMillis()/1000L));// value is a JSON number representing the number of seconds from 1970-01-01T0:0:0Z as measured in UTC until the date/time
			Address address = null;
			if (StringUtils.hasText(userAttributeValues.get(addressAttribute).valueAsString())) {
				address = new DefaultAddress();
				address.setFormatted(userAttributeValues.get(addressAttribute).valueAsString());
				//address.setStreetAddress("Šumavská 15");
				//address.setLocality("Brno");
				//address.setPostalCode("61200");
				//address.setCountry("Czech Republic");
			}
			ui.setAddress(address);
			//custom claims
			ClaimSourceProduceContext pctx = new ClaimSourceProduceContext(perunUserId, sub, userAttributeValues, perunAdapter, pair.getClient());
			log.debug("processing custom claims");
			for (PerunCustomClaimDefinition pccd : customClaims) {
				log.debug("producing value for custom claim {}", pccd.getClaim());
				JsonNode claimInJson = pccd.getClaimSource().produceValue(pctx);
				log.debug("produced value {}={}", pccd.getClaim(), claimInJson);
				if (claimInJson == null || claimInJson.isNull()) {
					log.debug("claim {} is null", pccd.getClaim());
					continue;
				} else if (claimInJson.isTextual() && !StringUtils.hasText(claimInJson.asText())) {
					log.debug("claim {} is a string and it is empty or null", pccd.getClaim());
					continue;
				} else if ((claimInJson.isArray() || claimInJson.isObject()) && claimInJson.size() == 0) {
					log.debug("claim {} is an object or array and it is empty or null", pccd.getClaim());
					continue;
				}
				ClaimModifier claimModifier = pccd.getClaimModifier();
				if (claimModifier != null) {
					log.debug("modifying values of claim '{}' using {}", pccd.getClaim(), claimModifier);
					//transform values
					if (claimInJson.isTextual()) {
						//transform a simple string value
						claimInJson = TextNode.valueOf(claimModifier.modify(claimInJson.asText()));
					} else if (claimInJson.isArray()) {
						claimInJson = claimInJson.deepCopy();
						//transform all strings in an array
						ArrayNode arrayNode = (ArrayNode) claimInJson;
						for (int i = 0; i < arrayNode.size(); i++) {
							JsonNode item = arrayNode.get(i);
							if (item.isTextual()) {
								String original = item.asText();
								String modified = claimModifier.modify(original);
								arrayNode.set(i, TextNode.valueOf(modified));
							}
						}
					}
				}
				ui.getCustomClaims().put(pccd.getClaim(), claimInJson);
			}
			log.debug("UserInfo created");
			return ui;
		}
	};

	private boolean shouldFillAttrs(Map<String, PerunAttributeValue> userAttributeValues) {
		if (perunOidcConfig.isFillMissingUserAttrs()) {
			if (userAttributeValues.isEmpty()) {
				return true;
			} else if (userAttributeValues.containsValue(null)) {
				return true;
			} else {
				return !userAttributeValues.values().stream()
						.filter(PerunAttributeValueAwareModel::isNullValue)
						.collect(Collectors.toSet())
						.isEmpty();
			}
		}
		return false;
	}

	private boolean checkStandardClaims(PerunUserInfo userInfo) {
		for (String claim: forceRegenerateUserinfoStandardClaims) {
			switch (claim.toLowerCase()) {
				case "sub": {
					if (userInfo.getSub() == null) {
						return false;
					}
				} break;
				case "preferred_username": {
					if (userInfo.getPreferredUsername() == null) {
						return false;
					}
				} break;
				case "given_name": {
					if (userInfo.getGivenName() == null) {
						return false;
					}
				} break;
				case "family_name": {
					if (userInfo.getFamilyName() == null) {
						return false;
					}
				} break;
				case "middle_name": {
					if (userInfo.getMiddleName() == null) {
						return false;
					}
				} break;
				case "name": {
					if (userInfo.getName() == null) {
						return false;
					}
				} break;
				case "email": {
					if (userInfo.getEmail() == null) {
						return false;
					}
				} break;
				case "address_formatted": {
					if (userInfo.getAddress() == null
							|| userInfo.getAddress().getFormatted() == null)
					{
						return false;
					}
				} break;
				case "phone": {
					if (userInfo.getPhoneNumber() == null) {
						return false;
					}
				} break;
				case "zoneinfo": {
					if (userInfo.getZoneinfo() == null) {
						return false;
					}
				} break;
				case "locale": {
					if (userInfo.getLocale() == null) {
						return false;
					}
				} break;
			}
		}

		log.debug("All required standard claims are OK");
		return true;
	}

	private boolean checkCustomClaims(PerunUserInfo userInfo) {
		for (String claim: forceRegenerateUserinfoCustomClaims) {
			if (userInfo.getCustomClaims().get(claim) == null ||
					userInfo.getCustomClaims().get(claim).isNull()) {
				return false;
			}
		}

		log.debug("All required custom claims are OK");
		return true;
	}

}
