package cz.muni.ics.oidc.web.controllers;

import cz.muni.ics.oidc.server.PerunScopeClaimTranslationService;
import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import cz.muni.ics.oidc.server.userInfo.PerunUserInfo;
import cz.muni.ics.oidc.web.WebHtmlClasses;
import cz.muni.ics.oidc.web.langs.Localization;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.model.DeviceCode;
import org.mitre.oauth2.service.SystemScopeService;
import org.mitre.oauth2.web.DeviceEndpoint;
import org.mitre.openid.connect.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.Principal;

@Controller
public class ApproveDeviceController {

    public static final String DEVICE = "device";
    public static final String APPROVE_DEVICE = "approveDevice";
    public static final String DEVICE_APPROVED = "deviceApproved";
    public static final String REQUEST_USER_CODE = "requestUserCode";
    public static final String USER_CODE = "user_code";
    public static final String USER_OAUTH_APPROVAL = "user_oauth_approval";

    private final SystemScopeService scopeService;
    private final DeviceEndpoint deviceEndpoint;
    private final PerunOidcConfig perunOidcConfig;
    private final Localization localization;
    private final WebHtmlClasses htmlClasses;
    private final PerunScopeClaimTranslationService scopeClaimTranslationService;
    private final UserInfoService userInfoService;

    @Autowired
    public ApproveDeviceController(SystemScopeService scopeService,
                                   DeviceEndpoint deviceEndpoint,
                                   PerunOidcConfig perunOidcConfig,
                                   Localization localization,
                                   WebHtmlClasses htmlClasses,
                                   PerunScopeClaimTranslationService scopeClaimTranslationService,
                                   UserInfoService userInfoService)
    {
        this.scopeService = scopeService;
        this.deviceEndpoint = deviceEndpoint;
        this.perunOidcConfig = perunOidcConfig;
        this.localization = localization;
        this.htmlClasses = htmlClasses;
        this.scopeClaimTranslationService = scopeClaimTranslationService;
        this.userInfoService = userInfoService;
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping(value = "/" + DEVICE,
            consumes = {"text/html", "application/xhtml+xml","application/xml;q=0.9","image/webp","*/*;q=0.8"})
    public String requestUserCode(@RequestParam(value = USER_CODE, required = false) String userCode,
                                  @ModelAttribute("authorizationRequest") AuthorizationRequest authRequest,
                                  Principal p,
                                  HttpServletRequest req,
                                  ModelMap model,
                                  HttpSession session)
    {
        String result = deviceEndpoint.requestUserCode(userCode, model, session);
        if (result.equals(REQUEST_USER_CODE) && !perunOidcConfig.getTheme().equalsIgnoreCase("default")) {
            ControllerUtils.setPageOptions(model, req, localization, htmlClasses, perunOidcConfig);
            model.put("page", REQUEST_USER_CODE);
            return "themedRequestUserCode";
        } else if (result.equals(APPROVE_DEVICE) && !perunOidcConfig.getTheme().equalsIgnoreCase("default")) {
            return themedApproveDevice(model, p, req);
        }
        return result;
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping(value = "/" + DEVICE + "/verify",
            consumes = {"text/html", "application/xhtml+xml","application/xml;q=0.9","image/webp","*/*;q=0.8"})
    public String readUserCode(@RequestParam(USER_CODE) String userCode,
                               @ModelAttribute("authorizationRequest") AuthorizationRequest authRequest,
                               Principal p,
                               HttpServletRequest req,
                               ModelMap model,
                               HttpSession session)
    {
        String result = deviceEndpoint.readUserCode(userCode, model, session);
        if (result.equals(APPROVE_DEVICE) && !perunOidcConfig.getTheme().equalsIgnoreCase("default")) {
            return themedApproveDevice(model, p, req);
        } else if (result.equals(REQUEST_USER_CODE) && !perunOidcConfig.getTheme().equalsIgnoreCase("default")) {
            ControllerUtils.setPageOptions(model, req, localization, htmlClasses, perunOidcConfig);
            model.put("page", REQUEST_USER_CODE);
            return "themedRequestUserCode";
        }

        return result;
    }

    private String themedApproveDevice(ModelMap model, Principal p, HttpServletRequest req) {
        model.remove("scopes");
        DeviceCode dc = (DeviceCode) model.get("dc");
        ClientDetailsEntity client = (ClientDetailsEntity) model.get("client");
        PerunUserInfo user = (PerunUserInfo) userInfoService.getByUsernameAndClientId(
                p.getName(), client.getClientId());
        ControllerUtils.setScopesAndClaims(scopeService, scopeClaimTranslationService, model, dc.getScope(), user);
        ControllerUtils.setPageOptions(model, req, localization, htmlClasses, perunOidcConfig);

        model.put("page", APPROVE_DEVICE);
        return "themedApproveDevice";
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping(value = "/" + DEVICE + "/approve", params = {USER_CODE, USER_OAUTH_APPROVAL})
    public String approveDevice(@RequestParam(USER_CODE) String userCode,
                                @RequestParam(USER_OAUTH_APPROVAL) Boolean approve,
                                @ModelAttribute(USER_OAUTH_APPROVAL) AuthorizationRequest authRequest,
                                Principal p,
                                HttpServletRequest req,
                                ModelMap model,
                                Authentication auth,
                                HttpSession session)
    {
        String result = deviceEndpoint.approveDevice(userCode, approve, model, auth, session);
        if (result.equals(DEVICE_APPROVED) && !perunOidcConfig.getTheme().equalsIgnoreCase("default")) {
            model.remove("scopes");

            DeviceCode dc = (DeviceCode)session.getAttribute("deviceCode");
            ClientDetailsEntity client = (ClientDetailsEntity) model.get("client");
            PerunUserInfo user = (PerunUserInfo) userInfoService.getByUsernameAndClientId(
                    p.getName(), client.getClientId());

            ControllerUtils.setScopesAndClaims(scopeService, scopeClaimTranslationService, model, dc.getScope(), user);
            ControllerUtils.setPageOptions(model, req, localization, htmlClasses, perunOidcConfig);

            model.put("page", DEVICE_APPROVED);
            return "themedDeviceApproved";
        }

        return result;
    }

}
