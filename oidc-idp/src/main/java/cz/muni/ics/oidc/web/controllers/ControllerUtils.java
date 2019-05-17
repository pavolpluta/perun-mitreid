package cz.muni.ics.oidc.web.controllers;

import cz.muni.ics.oidc.server.exceptions.LanguageFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * Utility class with common methods used for Controllers
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class ControllerUtils {

	private static final Logger log = LoggerFactory.getLogger(ControllerUtils.class);

	private static final String CESNET = "CESNET";
	private static final String CS = "CS";
	private static final String EN_KEY = "en";
	private static final String CS_KEY = "cs";
	private static final String LANG_KEY = "lang";
	private static final String REQ_URL_KEY = "reqURL";
	private static final String EN_PROPERTIES = "en.properties";
	private static final String CS_PROPERTIES = "cs.properties";

	public static void setLanguageForPage(Map<String, Object> model, HttpServletRequest req, String theme) {
		String langFile = EN_PROPERTIES;
		model.put(LANG_KEY, EN_KEY);
		log.trace("Resolving URL for possible language bar");
		model.put(REQ_URL_KEY, req.getRequestURL().toString() + '?' + req.getQueryString());
		if (CESNET.equalsIgnoreCase(theme)) {
			log.trace("Resolving Language for CESNET");
			if (req.getParameter(LANG_KEY) != null && req.getParameter(LANG_KEY).equalsIgnoreCase(CS)) {
				langFile = CS_PROPERTIES;
				model.put(LANG_KEY, CS_KEY);
			}
		}

		Properties langProperties = new Properties();
		try {
			log.trace("Loading properties file containing messages - filename: {}", langFile);
			langProperties.load(PerunOAuthConfirmationController.class.getResourceAsStream(langFile));
		} catch (IOException e) {
			log.error("Cannot load properties file '{}' with messages", langFile);
			throw new LanguageFileException("Cannot load file: " + langFile);
		}

		model.put("langProps", langProperties);
	}

	public static String createRedirectUrl(HttpServletRequest request, String removedPart,
									 String pathPart, Map<String, String> params) {
		log.trace("createRedirectUrl({}, {}, {}, {})", request, removedPart, pathPart, params);
		int endIndex = request.getRequestURL().toString().indexOf(removedPart);
		String baseUrl = request.getRequestURL().toString().substring(0, endIndex);

		StringBuilder builder = new StringBuilder();
		builder.append(baseUrl);
		builder.append(pathPart);
		if (! params.isEmpty()) {
			builder.append('?');
			for (Map.Entry<String, String> entry: params.entrySet()) {
				builder.append(entry.getKey());
				builder.append('=');
				builder.append(entry.getValue());
				builder.append('&');
			}
			builder.deleteCharAt(builder.length() - 1);
		}

		log.trace("createRedirectUrl returns: {}", builder.toString());
		return builder.toString();
	}
}
