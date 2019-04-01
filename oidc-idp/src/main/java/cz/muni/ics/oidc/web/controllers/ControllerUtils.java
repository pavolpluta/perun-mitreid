package cz.muni.ics.oidc.web.controllers;

import cz.muni.ics.oidc.server.exceptions.LanguageFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class ControllerUtils {

	private static final Logger log = LoggerFactory.getLogger(ControllerUtils.class);

	public static void setLanguageForPage(Map<String, Object> model, HttpServletRequest req, String theme) {
		String langFile = "en.properties";
		model.put("lang", "en");
		log.trace("Resolving URL for possible language bar");
		model.put("reqURL", req.getRequestURL().toString() + '?' + req.getQueryString());
		if ("CESNET".equalsIgnoreCase(theme)) {
			log.trace("Resolving Language for CESNET ");
			if (req.getParameter("lang") != null && req.getParameter("lang").equalsIgnoreCase("CS")) {
				langFile = "cs.properties";
				model.put("lang", "cs");
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
		log.debug("Creating redirect URL");
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

		log.debug("createdUrl: {}", builder.toString());
		return builder.toString();
	}
}
