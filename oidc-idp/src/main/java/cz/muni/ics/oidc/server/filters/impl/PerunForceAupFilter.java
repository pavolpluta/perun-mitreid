package cz.muni.ics.oidc.server.filters.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.ics.oidc.BeanUtil;
import cz.muni.ics.oidc.models.Aup;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.PerunAttribute;
import cz.muni.ics.oidc.models.PerunUser;
import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import cz.muni.ics.oidc.server.connectors.PerunConnector;
import cz.muni.ics.oidc.server.filters.FiltersUtils;
import cz.muni.ics.oidc.server.filters.PerunFilterConstants;
import cz.muni.ics.oidc.server.filters.PerunRequestFilter;
import cz.muni.ics.oidc.server.filters.PerunRequestFilterParams;
import cz.muni.ics.oidc.web.controllers.AupController;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.provider.OAuth2RequestFactory;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

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

/**
 * AUP filter checks if there are new AUPs which user hasn't accepted yet and forces him to do that.
 *
 * Configuration (replace "name" part with name defined for filter):
 * - filter.name.orgAupsAttrName - Mapping to Perun entityless attribute containing organization AUPs
 * - filter.name.userAupsAttrName - Mapping to Perun user attribute containing list of AUPS approved by user
 * - filter.name.voAupAttrName - Mapping to Perun VO attribute containing AUP specific for VO
 * - filter.name.facilityRequestedAupsAttrName - Mapping to Perun facility attribute containing list of AUPs requested
 * by the service. Contains only keys for those AUPs
 * - filter.name.voShortNamesAttrName - Mapping to Perun facility attribute containing list of short names for VOs
 * that have a resource assigned to the facility
 *
 * @author Dominik Baranek <0Baranek.dominik0@gmail.com>
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

    private final RequestMatcher requestMatcher = new AntPathRequestMatcher(PerunFilterConstants.AUTHORIZE_REQ_PATTERN);
    private final ObjectMapper mapper = new ObjectMapper();

    private final OAuth2RequestFactory authRequestFactory;
    private final ClientDetailsEntityService clientService;
    private final PerunConnector perunConnector;
    private final PerunOidcConfig perunOidcConfig;

    public PerunForceAupFilter(PerunRequestFilterParams params) {
        super(params);

        BeanUtil beanUtil = params.getBeanUtil();

        this.authRequestFactory = beanUtil.getBean(OAuth2RequestFactory.class);
        this.clientService = beanUtil.getBean(ClientDetailsEntityService.class);
        this.perunConnector = beanUtil.getBean(PerunConnector.class);
        this.perunOidcConfig = beanUtil.getBean(PerunOidcConfig.class);

        this.perunOrgAupsAttrName = params.getProperty(ORG_AUPS_ATTR_NAME);
        this.perunUserAupsAttrName = params.getProperty(USER_AUPS_ATTR_NAME);
        this.perunVoAupAttrName = params.getProperty(VO_AUP_ATTR_NAME);
        this.perunFacilityRequestedAupsAttrName = params.getProperty(FACILITY_REQUESTED_AUPS_ATTR_NAME);
        this.perunFacilityVoShortNamesAttrName = params.getProperty(VO_SHORT_NAMES_ATTR_NAME);
    }

    @Override
    protected boolean process(ServletRequest req, ServletResponse res) throws IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        ClientDetailsEntity client = FiltersUtils.extractClient(requestMatcher, request, authRequestFactory, clientService);
        if (client == null) {
            log.warn("Could not extract client");
            log.debug("Skipping to next filter");
            return true;
        }

        String clientIdentifier = client.getClientId();

        Facility facility = perunConnector.getFacilityByClientId(clientIdentifier);
        if (facility == null) {
            log.warn("Could not find facility with clientID: {}", clientIdentifier);
            log.debug("Skipping to next filter");
            return true;
        }

        List<String> attrsToFetch = new ArrayList<>(Arrays.asList(perunFacilityRequestedAupsAttrName, perunFacilityVoShortNamesAttrName));
        Map<String, PerunAttribute> facilityAttributes = perunConnector.getFacilityAttributes(facility, attrsToFetch);
        PerunAttribute facilityRequestedAupsAttr;

        if (facilityAttributes == null) {
            log.warn("Could not fetch attributes {} for facility {}", attrsToFetch, facility);
            log.debug("Skipping to next filter");
            return true;
        } else if (!facilityAttributes.containsKey(perunFacilityRequestedAupsAttrName) ||
                (facilityRequestedAupsAttr = facilityAttributes.get(perunFacilityRequestedAupsAttrName)) == null) {
            log.warn("Attribute {} for facility {} not fetched / is null", perunFacilityRequestedAupsAttrName, facility);
            log.info("Skipping to next filter");
            return true;
        }

        // VARIABLE HAS BEEN ASSIGNED IN THE ELSE IF ABOVE
        List<String> requiredAups = facilityRequestedAupsAttr.valueAsList();
        if (requiredAups == null || requiredAups.isEmpty()) {
            log.info("No AUPs required by service, continue to next filter");
            log.debug("Continue to next filter");
            return true;
        }

        PerunUser user = FiltersUtils.getPerunUser(request, perunOidcConfig, perunConnector);

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

            log.debug("Redirecting to AUPs approval page");
            response.sendRedirect(request.getContextPath() + '/' + AupController.URL);
            return false;
        }

        log.debug("AUPs approved by user are actual for the AUPs requested by facility {}", facility);
        return true;
    }

    private Map<String, Aup> getAupsToApprove(PerunUser user, Map<String, PerunAttribute> facilityAttributes) throws ParseException, IOException {
        log.trace("getAupsToApprove({}, {})", user, facilityAttributes);

        Map<String, Aup> aupsToApprove= new LinkedHashMap<>();
        Map<String, List<Aup>> orgAups = new HashMap<>();

        PerunAttribute requestedAupsAttr = facilityAttributes.get(perunFacilityRequestedAupsAttrName);
        //we can do this as we have checked for null value in previous step
        List<String> requiredAups = requestedAupsAttr.valueAsList();

        PerunAttribute facilityVoShortNames = facilityAttributes.get(perunFacilityVoShortNamesAttrName);
        PerunAttribute userAupsAttr = perunConnector.getUserAttribute(user.getId(), perunUserAupsAttrName);
        Map<String, PerunAttribute> orgAupsAttr = perunConnector.getEntitylessAttributes(perunOrgAupsAttrName);

        Map<String, List<Aup>> voAups = getVoAups(facilityVoShortNames.valueAsList());

        if (orgAupsAttr != null) {
            for (Map.Entry<String, PerunAttribute> entry : orgAupsAttr.entrySet()) {
                List<Aup> aups = Arrays.asList(mapper.readValue(entry.getValue().valueAsString(), Aup[].class));
                orgAups.put(entry.getKey(), aups);
            }
        }

        Map<String, List<Aup>> userAups = convertToMapKeyToListOfAups(userAupsAttr.valueAsMap());

        if (!orgAups.isEmpty()) {
            for (String requiredOrgAupKey : requiredAups) {
                if (!orgAups.containsKey(requiredOrgAupKey) || orgAups.get(requiredOrgAupKey) == null) {
                    continue;
                }

                Aup orgLatestAup = getLatestAupFromList(orgAups.get(requiredOrgAupKey));

                if (userAups.containsKey(requiredOrgAupKey)) {
                    Aup userLatestAup = getLatestAupFromList(userAups.get(requiredOrgAupKey));

                    if (orgLatestAup.getDate().equals(userLatestAup.getDate())) {
                        continue;
                    }
                }

                aupsToApprove.put(requiredOrgAupKey, orgLatestAup);
            }
        }

        if (!voAups.isEmpty()) {
            for (Map.Entry<String, List<Aup>> keyToVoAup : voAups.entrySet()) {
                Aup voLatestAup = getLatestAupFromList(keyToVoAup.getValue());

                if (userAups.containsKey(keyToVoAup.getKey())) {
                    Aup userLatestAup = getLatestAupFromList(userAups.get(keyToVoAup.getKey()));

                    if (voLatestAup.getDate().equals(userLatestAup.getDate())) {
                        continue;
                    }
                }

                aupsToApprove.put(keyToVoAup.getKey(), voLatestAup);
            }
        }

        log.trace("getAupsToApprove({}, {}) returns: {}", user, facilityAttributes, aupsToApprove);
        return aupsToApprove;
    }

    private Map<String, List<Aup>> getVoAups(List<String> voShortNames) throws IOException {
        log.trace("getVoAups({})", voShortNames);
        Map<String, List<Aup>> voAups = new HashMap<>();

        if (voShortNames != null && !voShortNames.isEmpty()) {
            for (String voShortName : voShortNames) {
                Long voId = perunConnector.getVoByShortName(voShortName).getId();

                PerunAttribute voAupAttr = perunConnector.getVoAttribute(voId, perunVoAupAttrName);
                if (voAupAttr.getValue() == null) {
                    continue;
                }

                List<Aup> aups = Arrays.asList(mapper.readValue(voAupAttr.valueAsString(), Aup[].class));
                if (!aups.isEmpty()) {
                    voAups.put(voShortName, aups);
                }
            }
        }

        log.trace("getVoAups({}) returns: {}", voShortNames, voAups);
        return voAups;
    }

    private Map<String, List<Aup>> convertToMapKeyToListOfAups(Map<String, String> keyToListOfAupsString) throws IOException {
        log.trace("convertToMapKeyToListOfAups({})", keyToListOfAupsString);
        Map<String, List<Aup>> resultMap = new HashMap<>();

        if (keyToListOfAupsString != null && !keyToListOfAupsString.isEmpty()) {
            for (Map.Entry<String, String> entry : keyToListOfAupsString.entrySet()) {
                List<Aup> aups = Arrays.asList(mapper.readValue(entry.getValue(), Aup[].class));

                resultMap.put(entry.getKey(), aups);
            }
        }

        log.trace("convertToMapKEyToListOfAups({}) returns: {}", keyToListOfAupsString, resultMap);
        return resultMap;
    }

    private Aup getLatestAupFromList(List<Aup> aups) throws ParseException {
        log.trace("getLatesAup({})", aups);
        Aup latestAup = aups.get(0);

        for(Aup aup : aups) {
            Date latestAupDate = new SimpleDateFormat(DATE_FORMAT).parse(latestAup.getDate());
            Date aupDate = new SimpleDateFormat(DATE_FORMAT).parse(aup.getDate());

            if (latestAupDate.before(aupDate)) {
                latestAup = aup;
            }
        }

        log.trace("getLatestAup({}) returns: {}", aups, latestAup);
        return latestAup;
    }
}
