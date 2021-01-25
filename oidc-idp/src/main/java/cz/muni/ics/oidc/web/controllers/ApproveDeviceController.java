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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private static final Logger log = LoggerFactory.getLogger(ApproveDeviceController.class);

    public static final String USER_URL = "device";
    public static final String APPROVE_DEVICE = "approveDevice";
    public static final String USER_CODE = "user_code";

    private final SystemScopeService scopeService;
    private final DeviceEndpoint deviceEndpoint;
    private final PerunOidcConfig perunOidcConfig;
    private final Localization localization;
    private final WebHtmlClasses htmlClasses;
    private final PerunScopeClaimTranslationService scopeClaimTranslationService;
    private final UserInfoService userInfoService;
    private final PerunOidcConfig config;

    @Autowired
    public ApproveDeviceController(SystemScopeService scopeService,
                                   DeviceEndpoint deviceEndpoint,
                                   PerunOidcConfig perunOidcConfig,
                                   Localization localization,
                                   WebHtmlClasses htmlClasses,
                                   PerunScopeClaimTranslationService scopeClaimTranslationService,
                                   UserInfoService userInfoService,
                                   PerunOidcConfig config)
    {
        this.scopeService = scopeService;
        this.deviceEndpoint = deviceEndpoint;
        this.perunOidcConfig = perunOidcConfig;
        this.localization = localization;
        this.htmlClasses = htmlClasses;
        this.scopeClaimTranslationService = scopeClaimTranslationService;
        this.userInfoService = userInfoService;
        this.config = config;
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping(value = "/" + USER_URL, params = USER_CODE)
    public String requestUserCode(@RequestParam(value = USER_CODE, required = false) String userCode,
                                  @ModelAttribute("authorizationRequest") AuthorizationRequest authRequest,
                                  Principal p,
                                  HttpServletRequest req,
                                  ModelMap model,
                                  HttpSession session)
    {

        if (!config.getConfigBean().isAllowCompleteDeviceCodeUri() || userCode == null) {
            // if we don't allow the complete URI or we didn't get a user code on the way in,
            // print out a page that asks the user to enter their user code
            // user must be logged in
            return "requestUserCode";
        } else {
            // complete verification uri was used, we received user code directly
            // skip requesting code page
            // user must be logged in
            return readUserCode(userCode, authRequest, p, req, model, session);
        }
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping(value = "/" + USER_URL + "/verify", params = {USER_CODE})
    public String readUserCode(@RequestParam(USER_CODE) String userCode,
                               @ModelAttribute("authorizationRequest") AuthorizationRequest authRequest,
                               Principal p,
                               HttpServletRequest req,
                               ModelMap model,
                               HttpSession session)
    {
        String result = deviceEndpoint.readUserCode(userCode, model, session);
        if (result.equals(APPROVE_DEVICE) && !perunOidcConfig.getTheme().equalsIgnoreCase("default")) {
            log.error("{}", model.get("scopes"));
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

        return result;
    }

}
