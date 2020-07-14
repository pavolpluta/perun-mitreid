package cz.muni.ics.oidc.web.controllers;

import com.google.common.base.Strings;
import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import cz.muni.ics.oidc.web.WebHtmlClasses;
import cz.muni.ics.oidc.web.langs.Localization;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
	public static final String LANG_PROPS_KEY = "langProps";

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
		model.put(LANG_PROPS_KEY, langProperties);
	}

	/**
	 * Create redirect URL
	 * @param request Request object
	 * @param removedPart Part of URL to be removed
	 * @param pathPart What to include as Path
	 * @param params Map object of parameters
	 * @return
	 */
	public static String createRedirectUrl(HttpServletRequest request, String removedPart,
										   String pathPart, Map<String, String> params) {
		String baseUrl = request.getRequestURL().toString();
		int endIndex = baseUrl.indexOf(removedPart);
		if (endIndex > 1) {
			baseUrl = baseUrl.substring(0, endIndex);
		}

		StringBuilder builder = new StringBuilder();
		builder.append(baseUrl);
		builder.append(pathPart);
		if (!params.isEmpty()) {
			builder.append('?');
			for (Map.Entry<String, String> entry: params.entrySet()) {
				try {
					String encodedParamVal = URLEncoder.encode(entry.getValue(), String.valueOf(StandardCharsets.UTF_8));
					builder.append(entry.getKey());
					builder.append('=');
					builder.append(encodedParamVal);
					builder.append('&');
				} catch (UnsupportedEncodingException e) {
					log.warn("Failed to encode param: {}, {}", entry.getKey(), entry.getValue());
				}
			}
			builder.deleteCharAt(builder.length() - 1);
		}

		return builder.toString();
	}

	public static void setPageOptions(Map<String, Object> model, HttpServletRequest req, Localization localization,
									  WebHtmlClasses classes, PerunOidcConfig perunOidcConfig) {
		setLanguageForPage(model, req, localization);
		model.put("classes", classes.getWebHtmlClassesProperties());
		model.put("theme", perunOidcConfig.getTheme().toLowerCase());
		model.put("baseURL", perunOidcConfig.getBaseURL());
		model.put("samlResourcesURL", perunOidcConfig.getSamlResourcesURL());
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
