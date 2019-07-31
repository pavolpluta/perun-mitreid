package cz.muni.ics.oidc.web.langs;

import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Static utility class for Language Bar displayed on custom pages.
 *
 * It contains mapping with language keys to language displayed names.
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class Localization {

	private static final Logger log = LoggerFactory.getLogger(Localization.class);

	private Map<String, String> localizationEntries;
	private Map<String, Properties> localizationFiles;
	private String localizationFilesPath;
	private List<String> enabledLanguages;

	public Localization(PerunOidcConfig perunOidcConfig) {
		this.enabledLanguages = perunOidcConfig.getAvailableLangs();
		this.localizationFilesPath = perunOidcConfig.getLocalizationFilesPath();
		initEntriesAndFiles();
	}

	public Map<String, String> getLocalizationEntries() {
		return localizationEntries;
	}

	public Map<String, Properties> getLocalizationFiles() {
		return localizationFiles;
	}

	public List<String> getEnabledLanguages() {
		return enabledLanguages;
	}

	/**
	 * Get mapping for the languages available
	 * @return Map with key = language code, value = language displayed text
	 */
	public Map<String, String> getEntriesAvailable() {
		Map<String, String> result = new HashMap<>();

		for (String key: enabledLanguages) {
			String lower = key.toLowerCase();
			if (localizationEntries.containsKey(lower)) {
				result.put(lower, localizationEntries.get(lower));
			}
		}

		return result;
	}

	private void initEntriesAndFiles() {
		localizationEntries = new HashMap<>();
		localizationEntries.put("en", "English");
		localizationEntries.put("cs", "Čeština");
		localizationEntries.put("sk", "Slovenčina");

		localizationFiles = new HashMap<>();
		for (String lang: enabledLanguages) {
			lang = lang.toLowerCase();
			if (! localizationEntries.containsKey(lang)) {
				continue;
			}

			Properties langProps = new Properties();
			String resourceFileName = "localization/" + lang + ".properties";
			try (InputStream resourceIs = getClass().getClassLoader().getResourceAsStream(resourceFileName)) {
				if (resourceIs == null) {
					log.warn("could not load {}", resourceFileName);
					continue;
				}
				langProps.load(resourceIs);
				log.trace("Loaded localization file: {}", resourceFileName);
				localizationFiles.put(lang, langProps);
			} catch (IOException e) {
				log.warn("Exception caught when reading {}", resourceFileName, e);
			}

			String customFileName = localizationFilesPath + '/' +lang + ".properties";
			try (InputStream customIs = new FileInputStream(customFileName)) {
				langProps.load(customIs);
				log.trace("Loaded localization file: {}", customFileName);
			} catch (FileNotFoundException e) {
				log.warn("File: {} not found", customFileName);
				e.printStackTrace();
			} catch (IOException e) {
				log.warn("Exception caught when reading {}", customFileName, e);
			}
		}
	}
}
