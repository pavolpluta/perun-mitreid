package cz.muni.ics.oidc.controllers;


import java.io.IOException;
import java.security.Principal;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.ics.oidc.*;
import cz.muni.ics.oidc.exceptions.LanguageFileException;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.mitre.oauth2.web.OAuthConfirmationController;
import org.mitre.openid.connect.service.UserInfoService;
import org.mitre.openid.connect.view.HttpCodeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller of the pages where user accepts that information
 * about him will be sent to the client.
 *
 * @author Dominik František Bučík bucik@ics.muni.cz
 *
 */

@Controller
@SessionAttributes("authorizationRequest")
public class PerunOAuthConfirmationController{

    private final static Logger log = LoggerFactory.getLogger(PerunOAuthConfirmationController.class);

    @Autowired
    private OAuthConfirmationController oAuthConfirmationController;

    @Autowired
    private ClientDetailsEntityService clientService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private PerunConnector perunConnector;

    @Autowired
    private PerunOidcConfig perunOidcConfig;

    @Autowired
    public PerunOAuthConfirmationController() {
        super();
    }

    @RequestMapping(value = "/oauth/confirm_access", params = { "client_id" })
    public String confirmAccess(Map<String, Object> model, HttpServletRequest req, @ModelAttribute("authorizationRequest") AuthorizationRequest authRequest,
                                Principal p) {
        ClientDetailsEntity client;

        try {
            client = clientService.loadClientByClientId(authRequest.getClientId());
        } catch (OAuth2Exception e) {
            log.error("confirmAccess: OAuth2Exception was thrown when attempting to load client", e);
            model.put(HttpCodeView.CODE, HttpStatus.NOT_FOUND);
            return HttpCodeView.VIEWNAME;
        } catch (IllegalArgumentException e) {
            log.error("confirmAccess: IllegalArgumentException was thrown when attempting to load client", e);
            model.put(HttpCodeView.CODE, HttpStatus.BAD_REQUEST);
            return HttpCodeView.VIEWNAME;
        }

        if (client == null) {
            log.error("confirmAccess: could not find client " + authRequest.getClientId());
            model.put(HttpCodeView.CODE, HttpStatus.NOT_FOUND);
            return HttpCodeView.VIEWNAME;
        }

        model.put("client", client);
        model.put("theme", perunOidcConfig.getTheme().toLowerCase());

        PerunUserInfo user = (PerunUserInfo) userInfoService.getByUsername(p.getName());
        setLanguageForPage(model, req);
        // check if user is allowed to access depending on his membership in groups
        // we use perunUserInfoRepository which for sure returns PerunUserInfo object

        if (!isUserInApprovedGroups(authRequest.getClientId(), user.getId())) {
            model.put("title", "Unapproved access");
            model.put("page", "unapproved");
            return "unapproved";
        }

        String result = oAuthConfirmationController.confimAccess(model, authRequest, p);
        if (result.equals("approve") && perunOidcConfig.getTheme().equalsIgnoreCase("default")) {
            return "approve";
        } else if (result.equals("approve")) {
            model.put("page", "consent");
            return "themedApprove";
        }

        return result;
    }

    private void setLanguageForPage(Map<String, Object> model, HttpServletRequest req) {
        String langFile = "en.properties";
        model.put("lang", "en");
        log.trace("Resolving URL for possible language bar");
        model.put("reqURL", req.getRequestURL().toString() + '?' + req.getQueryString());
        if (perunOidcConfig.getTheme().equalsIgnoreCase("CESNET")) {
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

    private boolean isUserInApprovedGroups(String clientId, Long userId) throws IllegalArgumentException {

        if (clientId == null || userId == null) {
            log.error("isUserInApprovedGroups wrong parameters clientId({}), userId({})", clientId, userId);
            throw new IllegalArgumentException("isUserInApprovedGroups wrong parameters");
        }


        JsonNode jn = perunConnector.getFacilitiesByClientId(clientId);
        Set<String> facilityIds = new HashSet<>(jn.findValuesAsText("id"));

        if (facilityIds.isEmpty()) {
            log.warn("Facility with OIDCClientID({}) could not be found", clientId);
            return true;
        }

        boolean cont = false;
        for (String facility: facilityIds) {
            cont = cont || perunConnector.isAllowedGroupCheckForFacility(facility);
        }

        if (!cont) {
            //checking membership is not required, returning true as user is allowed to access
            log.trace("Membership check not requested, skipping");
            return true;
        }

        log.info("Started group membership check");

        Set<String> resIds = new HashSet<>();
        for (String facility : facilityIds) {
            JsonNode resources = perunConnector.getAssignedResourcesForFacility(facility);
            resIds.addAll(resources.findValuesAsText("id"));
        }

        if (resIds.isEmpty()) {
            log.error("Resources could not be found for facilities({})", facilityIds.toString());
            throw new IllegalArgumentException("Resources could not be found");
        }

        jn = perunConnector.getMembersByUser(userId.toString());
        Set<String> members = filterActiveMembersIds(jn);

        if (members.isEmpty()) {
            log.error("Members could not be found for user({})", userId.toString());
            throw new IllegalArgumentException("Members could not be found");
        }

        for (String resource : resIds) {
            for (String member : members) {
                jn = perunConnector.getAssignedGroups(resource, member);
                List<String> groups = jn.findValuesAsText("id");
                if(! groups.isEmpty()) {
                    log.trace("User ({}) has been approved to access service ({})", userId, clientId);
                    return true;
                }
            }
        }

        log.error("User ({}) is not in group allowed to access service ({})", userId, clientId);
        return false;
    }

    private Set<String> filterActiveMembersIds(JsonNode members) {
        Set<String> active = new HashSet<>();
        for (JsonNode jn : members) {
            if (jn.has("status") && jn.get("status").asText().equals("VALID")) {
                active.add(jn.get("id").asText());
            }
        }

        return active;
    }
}
