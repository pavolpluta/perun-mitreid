package cz.muni.ics.oidc.web.controllers;

import com.google.common.base.Strings;
import cz.muni.ics.oidc.web.langs.Localization;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Utility class with common methods used for Controllers
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class ControllerUtils {

	private static final Logger log = LoggerFactory.getLogger(ControllerUtils.class);

	private static final String LANG_KEY = "lang";
	private static final String REQ_URL_KEY = "reqURL";
	private static final String LANGS_MAP_KEY = "langsMap";

	public static void setLanguageForPage(Map<String, Object> model, HttpServletRequest req, Localization localization) {
		String langFromParam = req.getParameter(LANG_KEY);
		String browserLang = req.getLocale().getLanguage();

		List<String> enabledLangs = localization.getEnabledLanguages();
		String langKey = "en";

		if (langFromParam != null && enabledLangs.stream().anyMatch(x -> x.equalsIgnoreCase(langFromParam))) {
			langKey = langFromParam;
		} else if (enabledLangs.stream().anyMatch(x -> x.equalsIgnoreCase(browserLang))) {
			langKey = browserLang;
		}

		String reqUrl = req.getRequestURL().toString();

		if (!Strings.isNullOrEmpty(req.getQueryString())) {
			reqUrl += ('?' + req.getQueryString());
		}

		try {
			reqUrl = removeQueryParameter(reqUrl, LANG_KEY);
		} catch (URISyntaxException e) {
			log.warn("Could not remove lang param");
		}

		Properties langProperties = localization.getLocalizationFiles().get(langKey);

		model.put(LANG_KEY, langKey);
		model.put(REQ_URL_KEY, reqUrl);
		model.put(LANGS_MAP_KEY, localization.getEntriesAvailable());
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

	private static String removeQueryParameter(String url, String parameterName) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder(url);
		List<NameValuePair> queryParameters = uriBuilder.getQueryParams()
				.stream()
				.filter(p -> !p.getName().equals(parameterName))
				.collect(Collectors.toList());
		if (queryParameters.isEmpty()) {
			uriBuilder.removeQuery();
		} else {
			uriBuilder.setParameters(queryParameters);
		}
		return uriBuilder.build().toString();
	}
}
