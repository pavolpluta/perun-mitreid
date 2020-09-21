package cz.muni.ics.oidc.server.filters.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.ics.oidc.BeanUtil;
import cz.muni.ics.oidc.models.Aup;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.PerunAttribute;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.server.adapters.PerunAdapter;
import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import cz.muni.ics.oidc.server.filters.FilterParams;
import cz.muni.ics.oidc.server.filters.FiltersUtils;
import cz.muni.ics.oidc.server.filters.PerunRequestFilter;
import cz.muni.ics.oidc.server.filters.PerunRequestFilterParams;
import cz.muni.ics.oidc.web.controllers.AupController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cz.muni.ics.oidc.web.controllers.AupController.APPROVED;

/**
 * AUP filter checks if there are new AUPs which user hasn't accepted yet and forces him to do that.
 *
 * Configuration (replace [name] part with the name defined for the filter):
 * <ul>
 *     <li><b>filter.[name].orgAupsAttrName</b> - Mapping to Perun entityless attribute containing organization AUPs</li>
 *     <li><b>filter.[name].userAupsAttrName</b> - Mapping to Perun user attribute containing list of AUPS approved by user</li>
 *     <li><b>filter.[name].voAupAttrName</b> - Mapping to Perun VO attribute containing AUP specific for VO</li>
 *     <li><b>filter.[name].facilityRequestedAupsAttrName</b> - Mapping to Perun facility attribute containing list of AUPs requested
 *         by the service. Contains only keys for those AUPs</li>
 *     <li><b>filter.[name].voShortNamesAttrName</b> - Mapping to Perun facility attribute containing list of short names for VOs
 *         that have a resource assigned to the facility</li>
 * </ul>
 *
 * @author Dominik Baranek <baranek@ics.muni.cz>
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class PerunForceAupFilter extends PerunRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PerunForceAupFilter.class);
    private static final String DATE_FORMAT = "dd-MM-yyyy";

    /* CONFIGURATION PROPERTIES */
    private static final String ORG_AUPS_ATTR_NAME = "orgAupsAttrName";
    private static final String USER_AUPS_ATTR_NAME = "userAupsAttrName";
    private static final String VO_AUP_ATTR_NAME = "voAupAttrName";
    private static final String FACILITY_REQUESTED_AUPS_ATTR_NAME = "facilityRequestedAupsAttrName";
    private static final String VO_SHORT_NAMES_ATTR_NAME = "voShortNamesAttrName";

    private final String perunOrgAupsAttrName;
    private final String perunUserAupsAttrName;
    private final String perunVoAupAttrName;
    private final String perunFacilityRequestedAupsAttrName;
    private final String perunFacilityVoShortNamesAttrName;
    /* END OF CONFIGURATION PROPERTIES */

    private final ObjectMapper mapper = new ObjectMapper();

    private final PerunAdapter perunAdapter;
    private final PerunOidcConfig perunOidcConfig;

    public PerunForceAupFilter(PerunRequestFilterParams params) {
        super(params);
        BeanUtil beanUtil = params.getBeanUtil();
        this.perunAdapter = beanUtil.getBean(PerunAdapter.class);
        this.perunOidcConfig = beanUtil.getBean(PerunOidcConfig.class);

        this.perunOrgAupsAttrName = params.getProperty(ORG_AUPS_ATTR_NAME);
        this.perunUserAupsAttrName = params.getProperty(USER_AUPS_ATTR_NAME);
        this.perunVoAupAttrName = params.getProperty(VO_AUP_ATTR_NAME);
        this.perunFacilityRequestedAupsAttrName = params.getProperty(FACILITY_REQUESTED_AUPS_ATTR_NAME);
        this.perunFacilityVoShortNamesAttrName = params.getProperty(VO_SHORT_NAMES_ATTR_NAME);
    }

    @Override
    protected boolean process(ServletRequest req, ServletResponse res, FilterParams params) throws IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (request.getSession() != null && request.getSession().getAttribute(APPROVED) != null) {
            request.getSession().removeAttribute(APPROVED);
            log.info("Aups already approved, check at next access to the service due to delayed propagation to LDAP");
            log.debug("Skipping to next filter");
            return true;
        }

        Facility facility = params.getFacility();
        if (facility == null) {
            log.debug("Skipping to next filter");
            return true;
        }

        List<String> attrsToFetch = new ArrayList<>(
                Arrays.asList(perunFacilityRequestedAupsAttrName, perunFacilityVoShortNamesAttrName));
        Map<String, PerunAttributeValue> facilityAttributes = perunAdapter.getFacilityAttributeValues(facility, attrsToFetch);

        if (facilityAttributes == null) {
            log.warn("Could not fetch attributes {} for facility {}", attrsToFetch, facility);
            log.debug("Skipping to next filter");
            return true;
        } else if (!facilityAttributes.containsKey(perunFacilityRequestedAupsAttrName) &&
                !facilityAttributes.containsKey(perunFacilityVoShortNamesAttrName)) {
            log.warn("Could not fetch attributes {}, {} for facility {}", perunFacilityRequestedAupsAttrName,
                    perunFacilityVoShortNamesAttrName, facility);
            log.debug("Skipping to next filter");
            return true;
        }

        PerunUser user = FiltersUtils.getPerunUser(request, perunOidcConfig, perunAdapter);

        Map<String, Aup> newAups;

        try {
            newAups = getAupsToApprove(user, facilityAttributes);
        } catch (ParseException | IOException e) {
            log.warn("Caught ParseException", e);
            log.debug("Skipping to next filter");
            return true;
        }

        if (!newAups.isEmpty()) {
            log.debug("User has to approve AUPs: {}", newAups.keySet());
            String newAupsString = mapper.writeValueAsString(newAups);

            request.getSession().setAttribute(AupController.RETURN_URL, request.getRequestURI().replace(request.getContextPath(), "") + '?' + request.getQueryString());
            request.getSession().setAttribute(AupController.NEW_AUPS, newAupsString);
            request.getSession().setAttribute(AupController.USER_ATTR, perunUserAupsAttrName);

            log.debug("Redirecting to AUPs approval page");
            response.sendRedirect(request.getContextPath() + '/' + AupController.URL);
            return false;
        }

        log.debug("AUPs approved by user are actual for the AUPs requested by facility {}", facility);
        return true;
    }

    private Map<String, Aup> getAupsToApprove(PerunUser user, Map<String, PerunAttributeValue> facilityAttributes) throws ParseException, IOException {
        Map<String, Aup> aupsToApprove= new LinkedHashMap<>();

        PerunAttributeValue userAupsAttr = perunAdapter.getUserAttributeValue(user.getId(), perunUserAupsAttrName);
        if (perunOidcConfig.isFillMissingUserAttrs() && (userAupsAttr == null || userAupsAttr.isNullValue())) {
            userAupsAttr = perunAdapter.getAdapterFallback().getUserAttributeValue(user.getId(), perunUserAupsAttrName);
        }
        Map<String, List<Aup>> userAups = convertToMapKeyToListOfAups(userAupsAttr.valueAsMap());

        PerunAttributeValue requestedAupsAttr = facilityAttributes.get(perunFacilityRequestedAupsAttrName);
        PerunAttributeValue facilityVoShortNamesAttr = facilityAttributes.get(perunFacilityVoShortNamesAttrName);

        if (requestedAupsAttr != null && !requestedAupsAttr.isNullValue() && requestedAupsAttr.valueAsList() != null
                && !requestedAupsAttr.valueAsList().isEmpty()) {
            Map<String, Aup> orgAupsToApprove = getOrgAupsToApprove(requestedAupsAttr.valueAsList(), userAups);
            mergeAupMaps(aupsToApprove, orgAupsToApprove);
        }

        if (facilityVoShortNamesAttr != null && !facilityVoShortNamesAttr.isNullValue()
                && facilityVoShortNamesAttr.valueAsList() != null && !facilityVoShortNamesAttr.valueAsList().isEmpty()) {
            Map<String, Aup> voAupsToApprove = getVoAupsToApprove(facilityVoShortNamesAttr.valueAsList(), userAups);
            mergeAupMaps(aupsToApprove, voAupsToApprove);
        }

        return aupsToApprove;
    }

    private void mergeAupMaps(Map<String, Aup> original, Map<String, Aup> updates) {
        for (Map.Entry<String, Aup> pair: updates.entrySet()) {
            if (original.containsKey(pair.getKey())) {
                Aup originalAup = original.get(pair.getKey());
                Aup updateAup = pair.getValue();
                if (updateAup.getDateAsLocalDate().isAfter(originalAup.getDateAsLocalDate())) {
                    original.replace(pair.getKey(), pair.getValue());
                }
            } else {
                original.put(pair.getKey(), pair.getValue());
            }
        }
    }

    private Map<String, Aup> getVoAupsToApprove(List<String> facilityVoShortNames, Map<String, List<Aup>> userAups)
            throws IOException, ParseException {
        Map<String, Aup> aupsToApprove = new LinkedHashMap<>();
        Map<String, List<Aup>> voAups = getVoAups(facilityVoShortNames);

        if (!voAups.isEmpty()) {
            for (Map.Entry<String, List<Aup>> keyToVoAup : voAups.entrySet()) {
                Aup voLatestAup = getLatestAupFromList(keyToVoAup.getValue());

                if (userAups.containsKey(keyToVoAup.getKey())) {
                    Aup userLatestAup = getLatestAupFromList(userAups.get(keyToVoAup.getKey()));

                    if (! (voLatestAup.getDateAsLocalDate().isAfter(userLatestAup.getDateAsLocalDate()))) {
                        continue;
                    }
                }

                aupsToApprove.put(keyToVoAup.getKey(), voLatestAup);
            }
        }

        return aupsToApprove;
    }

    private Map<String, Aup> getOrgAupsToApprove(List<String > requestedAups, Map<String, List<Aup>> userAups)
            throws ParseException, IOException {
        Map<String, Aup> aupsToApprove = new LinkedHashMap<>();
        Map<String, List<Aup>> orgAups = new HashMap<>();

        Map<String, PerunAttribute> orgAupsAttr = perunAdapter.getAdapterRpc().getEntitylessAttributes(perunOrgAupsAttrName);

        if (orgAupsAttr != null && !orgAupsAttr.isEmpty()) {
            for (Map.Entry<String, PerunAttribute> entry : orgAupsAttr.entrySet()) {
                if (entry.getValue() != null && entry.getValue().valueAsList() != null) {
                    List<Aup> aups = Arrays.asList(mapper.readValue(entry.getValue().valueAsString(), Aup[].class));
                    orgAups.put(entry.getKey(), aups);
                }
            }
        }

        if (!orgAups.isEmpty()) {
            for (String requiredOrgAupKey : requestedAups) {
                if (!orgAups.containsKey(requiredOrgAupKey) || orgAups.get(requiredOrgAupKey) == null) {
                    continue;
                }

                Aup orgLatestAup = getLatestAupFromList(orgAups.get(requiredOrgAupKey));

                if (userAups.containsKey(requiredOrgAupKey)) {
                    Aup userLatestAup = getLatestAupFromList(userAups.get(requiredOrgAupKey));

                    if (! (orgLatestAup.getDateAsLocalDate().isAfter(userLatestAup.getDateAsLocalDate()))) {
                        continue;
                    }
                }

                aupsToApprove.put(requiredOrgAupKey, orgLatestAup);
            }
        }

        return aupsToApprove;
    }

    private Map<String, List<Aup>> getVoAups(List<String> voShortNames) throws IOException {
        Map<String, List<Aup>> voAups = new HashMap<>();

        if (voShortNames != null && !voShortNames.isEmpty()) {
            for (String voShortName : voShortNames) {
                Long voId = perunAdapter.getVoByShortName(voShortName).getId();

                PerunAttributeValue voAupAttr = perunAdapter.getVoAttributeValue(voId, perunVoAupAttrName);
                if (voAupAttr == null || voAupAttr.valueAsString() == null) {
                    continue;
                }

                List<Aup> aups = Arrays.asList(mapper.readValue(voAupAttr.valueAsList().toString(), Aup[].class));
                if (!aups.isEmpty()) {
                    voAups.put(voShortName, aups);
                }
            }
        }

        return voAups;
    }

    private Map<String, List<Aup>> convertToMapKeyToListOfAups(Map<String, String> keyToListOfAupsString) throws IOException {
        Map<String, List<Aup>> resultMap = new HashMap<>();

        if (keyToListOfAupsString != null && !keyToListOfAupsString.isEmpty()) {
            for (Map.Entry<String, String> entry : keyToListOfAupsString.entrySet()) {
                List<Aup> aups = Arrays.asList(mapper.readValue(entry.getValue(), Aup[].class));

                resultMap.put(entry.getKey(), aups);
            }
        }

        return resultMap;
    }

    private Aup getLatestAupFromList(List<Aup> aups) throws ParseException {
        Aup latestAup = aups.get(0);

        for(Aup aup : aups) {
            Date latestAupDate = new SimpleDateFormat(DATE_FORMAT).parse(latestAup.getDate());
            Date aupDate = new SimpleDateFormat(DATE_FORMAT).parse(aup.getDate());

            if (latestAupDate.before(aupDate)) {
                latestAup = aup;
            }
        }

        return latestAup;
    }
}
