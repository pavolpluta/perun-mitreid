package cz.muni.ics.oidc.web.controllers;


import cz.muni.ics.oidc.server.PerunScopeClaimTranslationService;
import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import cz.muni.ics.oidc.server.userInfo.PerunUserInfo;
import cz.muni.ics.oidc.web.WebHtmlClasses;
import cz.muni.ics.oidc.web.langs.Localization;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.service.SystemScopeService;
import org.mitre.oauth2.web.OAuthConfirmationController;
import org.mitre.openid.connect.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Map;

/**
 * Controller of the pages where user accepts that information
 * about him will be sent to the client.
 *
 * @author Dominik František Bučík <bucik@ics.muni.cz>
 * @author Peter Jancus <jancus@ics.muni.cz>
 */
@Controller
@SessionAttributes("authorizationRequest")
public class PerunOAuthConfirmationController{

    public static final String APPROVE = "approve";

    private final OAuthConfirmationController oAuthConfirmationController;
    private final UserInfoService userInfoService;
    private final PerunOidcConfig perunOidcConfig;
    private final SystemScopeService scopeService;
    private final PerunScopeClaimTranslationService scopeClaimTranslationService;
    private final Localization localization;
    private final WebHtmlClasses htmlClasses;

    @Autowired
    public PerunOAuthConfirmationController(OAuthConfirmationController oAuthConfirmationController,
                                            UserInfoService userInfoService,
                                            PerunOidcConfig perunOidcConfig,
                                            SystemScopeService scopeService,
                                            PerunScopeClaimTranslationService scopeClaimTranslationService,
                                            Localization localization,
                                            WebHtmlClasses htmlClasses)
    {
        this.oAuthConfirmationController = oAuthConfirmationController;
        this.userInfoService = userInfoService;
        this.perunOidcConfig = perunOidcConfig;
        this.scopeService = scopeService;
        this.scopeClaimTranslationService = scopeClaimTranslationService;
        this.localization = localization;
        this.htmlClasses = htmlClasses;
    }

    @RequestMapping(value = "/oauth/confirm_access", params = { "client_id" })
    public String confirmAccess(Map<String, Object> model, HttpServletRequest req, Principal p,
                                @ModelAttribute("authorizationRequest") AuthorizationRequest authRequest)
    {
        String result = oAuthConfirmationController.confimAccess(model, authRequest, p);
        if (result.equals(APPROVE) && !perunOidcConfig.getTheme().equalsIgnoreCase("default")) {
            model.remove("scopes");
            model.remove("claims");
            ClientDetailsEntity client = (ClientDetailsEntity) model.get("client");
            PerunUserInfo user = (PerunUserInfo) userInfoService.getByUsernameAndClientId(
                    p.getName(), client.getClientId());
            ControllerUtils.setScopesAndClaims(scopeService, scopeClaimTranslationService, model, authRequest.getScope(),
                    user);
            ControllerUtils.setPageOptions(model, req, localization, htmlClasses, perunOidcConfig);

            model.put("page", "consent");
            return "themedApprove";
        }

        return result;
    }

}
