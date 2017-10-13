package cz.muni.ics.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.mitre.openid.connect.model.Address;
import org.mitre.openid.connect.model.DefaultAddress;
import org.mitre.openid.connect.model.UserInfo;
import org.mitre.openid.connect.repository.UserInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Provides data about a user.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunUserInfoRepository implements UserInfoRepository {

	private final static Logger log = LoggerFactory.getLogger(PerunUserInfoRepository.class);

	private String perunUrl;
	private String perunUser;
	private String perunPassword;
	private String subAttribute;
	private String preferredUsernameAttribute;
	private String emailAttribute;
	private String addressAttribute;
	private String phoneAttribute;

	public void setPerunUrl(String perunUrl) {
		this.perunUrl = perunUrl;
	}

	public void setPerunUser(String perunUser) {
		this.perunUser = perunUser;
	}

	public void setPerunPassword(String perunPassword) {
		this.perunPassword = perunPassword;
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

	private List<PerunCustomClaimDefinition> customClaims = new ArrayList<>();

	public void setCustomClaims(List<PerunCustomClaimDefinition> customClaims) {
		this.customClaims = customClaims;
	}

	public List<PerunCustomClaimDefinition> getCustomClaims() {
		return customClaims;
	}

	@Override
	public UserInfo getByUsername(String username) {
		log.trace("getByUsername({})", username);
		try {
			return cache.get(username);
		} catch (UncheckedExecutionException | ExecutionException e) {
			log.error("cannot get user from cache",e);
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
				.expireAfterAccess(14, TimeUnit.DAYS)
				.build(cacheLoader);
	}

	private LoadingCache<String, UserInfo> cache;

	@SuppressWarnings("FieldCanBeLocal")
	private CacheLoader<String, UserInfo> cacheLoader = new CacheLoader<String, UserInfo>() {
		@Override
		public UserInfo load(String username) throws Exception {
			log.trace("load({})",username);
			PerunUserInfo ui = new PerunUserInfo();
			//prepare basic auth
			RestTemplate restTemplate = new RestTemplate();
			List<ClientHttpRequestInterceptor> interceptors =
					Collections.singletonList(new BasicAuthorizationInterceptor(perunUser, perunPassword));
			restTemplate.setRequestFactory(new InterceptingClientHttpRequestFactory(restTemplate.getRequestFactory(), interceptors));
			//make call
			log.debug("calling Perun RPC usersManager/getRichUserWithAttributes?user={}",username);
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("user",Integer.parseInt(username));
			JsonNode result = restTemplate.postForObject(perunUrl + "/json/usersManager/getRichUserWithAttributes", map, JsonNode.class);
			//process
			RichUser richUser = new RichUser(result);

			ui.setSub(richUser.get(subAttribute)); // Subject - Identifier for the End-User at the Issuer.
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
			//ui.setZoneinfo(TimeZone.getDefault().getID());//String from zoneinfo [zoneinfo] time zone database, For example, Europe/Paris
			//ui.setLocale("cs-CZ"); //  For example, en-US or fr-CA.
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
			for(PerunCustomClaimDefinition pccd : customClaims) {
				ui.getCustomClaims().put(pccd.getClaim(),richUser.get(pccd.getPerunAttributeName()));
			}
			log.trace("user loaded");
			return ui;
		}
	};

	private static class RichUser {
		Map<String,JsonNode> map = new HashMap<>();
		RichUser(JsonNode jsonNode) {
			for(JsonNode ua: jsonNode.path("userAttributes")) {
				String attributeName = ua.path("namespace").asText() + ":" + ua.path("friendlyName").asText();
				JsonNode value = ua.path("value");
				map.put(attributeName,value);
			}
		}

		String get(String s) {
			JsonNode jsonNode = map.get(s);
			if(jsonNode==null) return null;
			if(jsonNode.isNull()) return null;
			return jsonNode.asText();
		}
	}


}
