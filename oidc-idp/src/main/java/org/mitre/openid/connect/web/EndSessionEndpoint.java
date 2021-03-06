/*******************************************************************************
 * Copyright 2018 The MIT Internet Trust Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package org.mitre.openid.connect.web;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import cz.muni.ics.oidc.server.configurations.PerunOidcConfig;
import cz.muni.ics.oidc.web.WebHtmlClasses;
import cz.muni.ics.oidc.web.controllers.ControllerUtils;
import cz.muni.ics.oidc.web.langs.Localization;
import org.mitre.jwt.assertion.impl.SelfAssertionValidator;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.mitre.openid.connect.config.ConfigurationPropertiesBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

/**
 * End Session Endpoint from OIDC session management.
 * <p>
 * This is a copy of the original file with modification at the end of processLogout().
 * </p>
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
@Controller
public class EndSessionEndpoint {

	public static final String URL = "endsession";

	private static final String CLIENT_KEY = "client";
	private static final String STATE_KEY = "state";
	private static final String REDIRECT_URI_KEY = "redirectUri";
	private static final String DENY_REDIRECT_URL = "oldReferer";
	private static final String REFERER_HEADER = "Referer" ;

	private static Logger logger = LoggerFactory.getLogger(EndSessionEndpoint.class);

	@Autowired
	private SelfAssertionValidator validator;

	@Autowired
	private PerunOidcConfig perunOidcConfig;

	@Autowired
	private ConfigurationPropertiesBean configBean;

	@Autowired
	private ClientDetailsEntityService clientService;

	@Autowired
	private Localization localization;

	@Autowired
	private WebHtmlClasses htmlClasses;

	@RequestMapping(value = "/" + URL, method = RequestMethod.GET)
	public String endSession(@RequestParam(value = "id_token_hint", required = false) String idTokenHint,
	                         @RequestParam(value = "post_logout_redirect_uri", required = false) String postLogoutRedirectUri,
	                         @RequestParam(value = STATE_KEY, required = false) String state,
	                         HttpServletRequest request,
	                         HttpServletResponse response,
	                         HttpSession session,
	                         Authentication auth, Map<String, Object> model) throws IOException {

		String referer = request.getHeader(REFERER_HEADER);
		session.setAttribute(DENY_REDIRECT_URL, referer);

		// conditionally filled variables
		JWTClaimsSet idTokenClaims = null; // pulled from the parsed and validated ID token
		ClientDetailsEntity client = null; // pulled from ID token's audience field

		if (!Strings.isNullOrEmpty(postLogoutRedirectUri)) {
			session.setAttribute(REDIRECT_URI_KEY, postLogoutRedirectUri);
		}
		if (!Strings.isNullOrEmpty(state)) {
			session.setAttribute(STATE_KEY, state);
		}

		// parse the ID token hint to see if it's valid
		if (!Strings.isNullOrEmpty(idTokenHint)) {
			try {
				JWT idToken = JWTParser.parse(idTokenHint);

				if (validator.isValid(idToken)) {
					// we issued this ID token, figure out who it's for
					idTokenClaims = idToken.getJWTClaimsSet();

					String clientId = Iterables.getOnlyElement(idTokenClaims.getAudience());

					client = clientService.loadClientByClientId(clientId);

					// save a reference in the session for us to pick up later
					//session.setAttribute("endSession_idTokenHint_claims", idTokenClaims);
					session.setAttribute(CLIENT_KEY, client);
				}
			} catch (ParseException e) {
				// it's not a valid ID token, ignore it
				logger.debug("Invalid id token hint", e);
			} catch (InvalidClientException e) {
				// couldn't find the client, ignore it
				logger.debug("Invalid client", e);
			}
		}

		// are we logged in or not?
		if (auth == null || !request.isUserInRole("ROLE_USER")) {
			// we're not logged in anyway, process the final redirect bits if needed
			return processLogout(null, null, request, response, session, auth);
		} else {
			logger.info("Logout confirmating for user {} from client {}", auth.getName(), client != null ? client.getClientName() : "unknown");
			// we are logged in, need to prompt the user before we log out
			model.put("client", client);
			model.put("idToken", idTokenClaims);

			ControllerUtils.setPageOptions(model, request, localization, htmlClasses, perunOidcConfig);

			// display the log out confirmation page
			return "logout";
		}
	}

	@RequestMapping(value = "/" + URL, method = RequestMethod.POST)
	public String processLogout(@RequestParam(value = "approve", required = false) String approved,
	                            @RequestParam(value = "deny", required = false) String deny,
	                            HttpServletRequest request,
	                            HttpServletResponse response,
	                            HttpSession session,
	                            Authentication auth) throws IOException {

		if (! Strings.isNullOrEmpty(deny)) {
			String referer = request.getHeader(REFERER_HEADER);
			String denyRedirectUrl = (String) session.getAttribute(DENY_REDIRECT_URL);
			session.removeAttribute(DENY_REDIRECT_URL);


			if (Strings.isNullOrEmpty(denyRedirectUrl) || !referer.contains(configBean.getIssuer())) {
				denyRedirectUrl = referer;
			}

			logger.trace("User denied logout - redirecting to: {}", denyRedirectUrl);
			response.sendRedirect(denyRedirectUrl);
			return null;
		}

		String redirectUri = (String) session.getAttribute(REDIRECT_URI_KEY);
		String state = (String) session.getAttribute(STATE_KEY);
		ClientDetailsEntity client = (ClientDetailsEntity) session.getAttribute(CLIENT_KEY);

		if (!Strings.isNullOrEmpty(approved)) {
			// use approved, perform the logout
			if (auth != null) {
				new SecurityContextLogoutHandler().logout(request, response, auth);
			}
			SecurityContextHolder.getContext().setAuthentication(null);
		}

		String redirectURL = null;

		// if we have a client AND the client has post-logout redirect URIs
		// registered AND the URI given is in that list, then...
		if (!Strings.isNullOrEmpty(redirectUri) &&
				client != null && client.getPostLogoutRedirectUris() != null) {

			if (client.getPostLogoutRedirectUris().contains(redirectUri)) {
				UriComponents uri = UriComponentsBuilder.fromHttpUrl(redirectUri).queryParam("state", state).build();
				logger.trace("redirect URL: {}", uri);
				redirectURL = uri.toString();
			}
		}

		String samlLogoutURL = perunOidcConfig.getSamlLogoutURL();
		if (redirectURL != null) {
			logger.trace("redirecting to logout SAML and then {}", redirectURL);
			return "redirect:" + UriComponentsBuilder.fromHttpUrl(samlLogoutURL).queryParam("return", redirectURL).build();
		} else {
			logger.trace("redirecting to logout SAML only");
			return "redirect:" + samlLogoutURL;
		}
	}

}
