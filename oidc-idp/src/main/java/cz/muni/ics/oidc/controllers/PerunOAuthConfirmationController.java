package cz.muni.ics.oidc.controllers;


import java.security.Principal;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.ics.oidc.*;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.mitre.oauth2.web.OAuthConfirmationController;
import org.mitre.openid.connect.service.UserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

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
    public String confirmAccess(Map<String, Object> model, @ModelAttribute("authorizationRequest") AuthorizationRequest authRequest,
                               Principal p) {
        // get the userinfo claims for each scope
        PerunUserInfo user = (PerunUserInfo) userInfoService.getByUsername(p.getName());

        // check if user is allowed to access depending on his membership in groups
        // we use perunUserInfoRepository which for sure returns PerunUserInfo object
        try {
            if (!isUserInApprovedGroups(authRequest.getClientId(), user.getId())) {
                model.put("title", "Unapproved access");
                return "unapproved";
            }
        } catch (IllegalArgumentException e) {
            model.put("title", "Error while processing");
            return "error";
        }

        model.put("theme", perunOidcConfig.getTheme());
        String result = oAuthConfirmationController.confimAccess(model, authRequest, p);
        if (result.equals("approve") && perunOidcConfig.getTheme().equals("default")) {
            return "defaultApprove";
        }

        return result;
    }

    private boolean isUserInApprovedGroups(String clientId, Long userId) throws IllegalArgumentException {

        if (clientId == null || userId == null) {
            log.error("isUserInApprovedGroups wrong parameters clientId({}), userId({})", clientId, userId);
            throw new IllegalArgumentException("isUserInApprovedGroups wrong parameters");
        }

        log.info("started approval check");
        JsonNode jn = perunConnector.getFacilitiesByClientId(clientId);
        Set<String> facilityIds = new HashSet<>(jn.findValuesAsText("id"));

        if (facilityIds.isEmpty()) {
            log.error("facility could not be found with clientId({})", clientId);
            throw new IllegalArgumentException("Facility could not be found");
        }

        Set<String> resIds = new HashSet<>();
        for (String facility : facilityIds) {
            JsonNode resources = perunConnector.getAssignedResourcesForFacility(facility);
            resIds.addAll(resources.findValuesAsText("id"));
        }

        if (resIds.isEmpty()) {
            log.error("resources could not be found for facilities({})", facilityIds.toString());
            throw new IllegalArgumentException("Resources could not be found");
        }

        jn = perunConnector.getMembersByUser(userId.toString());
        Set<String> members = filterActiveMembersIds(jn);

        if (members.isEmpty()) {
            log.error("members could not be found for user({})", userId.toString());
            throw new IllegalArgumentException("Members could not be found");
        }

        for (String resource : resIds) {
            for (String member : members) {
                jn = perunConnector.getAssignedGroups(resource, member);
                List<String> groups = jn.findValuesAsText("id");
                if(! groups.isEmpty()) {
                    log.trace("user ({}) has been approved to access service ({})", userId, clientId);
                    return true;
                }
            }
        }

        log.error("user ({}) is not in group allowed to access service ({})", userId, clientId);
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
