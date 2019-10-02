package cz.muni.ics.oidc.server.userInfo;

import cz.muni.ics.oidc.server.connectors.PerunConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Context for UserInfoModifiers.
 *
 * @author Dominik Baránek 0Baranek.dominik0@gmail.com
 */
public class UserInfoModifierContext {

	private static final Logger log = LoggerFactory.getLogger(PerunUserInfoService.class);

	private static final String MODIFIER_CLASS = ".modifierClass";

	private List<String> modifiers;
	private Properties properties;
	private PerunConnector perunConnector;

	public UserInfoModifierContext(Properties properties, PerunConnector perunConnector) {
		this.properties = properties;
		String property = properties.getProperty("userInfo.modifiers");
		modifiers = Arrays.asList(property.split(","));
		this.perunConnector = perunConnector;
	}

	public PerunUserInfo modify(PerunUserInfo perunUserInfo, String clientId) {
		log.trace("modify({}, {})", perunUserInfo, clientId);
		for (String m : modifiers) {
			UserInfoModifier modifier = loadModifier("userInfo.modifier." + m);
			if (modifier != null) {
				log.trace("Executing modifier {}", m);
				modifier.modify(perunUserInfo, clientId);
			}
		}

		return perunUserInfo;
	}

	private UserInfoModifier loadModifier(String propertyPrefix) {

		String modifierClass = properties.getProperty(propertyPrefix + MODIFIER_CLASS, null);
		if (modifierClass == null) {
			return null;
		}
		try {
			Class<?> rawClazz = Class.forName(modifierClass);
			if (!UserInfoModifier.class.isAssignableFrom(rawClazz)) {
				log.error("modifier class {} does not extend UserInfoModifier", modifierClass);
				return null;
			}
			@SuppressWarnings("unchecked") Class<UserInfoModifier> clazz = (Class<UserInfoModifier>) rawClazz;
			Constructor<UserInfoModifier> constructor = clazz.getConstructor(UserInfoModifierInitContext.class);
			UserInfoModifierInitContext ctx = new UserInfoModifierInitContext(propertyPrefix, properties, perunConnector);
			UserInfoModifier userInfoModifier = constructor.newInstance(ctx);
			log.info("loaded modifier '{}' for {}", userInfoModifier, propertyPrefix);
			return userInfoModifier;
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
	}
}
