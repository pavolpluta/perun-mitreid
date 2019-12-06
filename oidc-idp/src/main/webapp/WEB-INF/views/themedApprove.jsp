<%@ page import="cz.muni.ics.oidc.server.elixir.GA4GHClaimSource" %>
<%@ page import="java.lang.String" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags/common" %>

<c:set var="baseURL" value="${baseURL}"/>
<c:set var="samlResourcesURL" value="${samlResourcesURL}"/>
<%

	String samlCssUrl = (String) pageContext.getAttribute("samlResourcesURL");
	List<String> cssLinks = new ArrayList<>();

	cssLinks.add(samlCssUrl + "/module.php/consent/assets/css/consent.css");
	cssLinks.add(samlCssUrl + "/module.php/perun/res/css/consent.css");

	pageContext.setAttribute("cssLinks", cssLinks);

%>

<t:header title="${langProps['consent_header']}" reqURL="${reqURL}" baseURL="${baseURL}" cssLinks="${cssLinks}" theme="${theme}"/>

<h1>${langProps['consent_header']}</h1>

</div> <%-- header --%>

<div id="content">

	<c:remove scope="session" var="SPRING_SECURITY_LAST_EXCEPTION" />
	<form name="confirmationForm"
		  action="${pageContext.request.contextPath.endsWith('/') ? pageContext.request.contextPath : pageContext.request.contextPath.concat('/')}authorize" method="post">
		<p>${langProps['consent_privacypolicy']}
			&#32;<a target='_blank' href='${fn:escapeXml(client.policyUri)}'><em>${fn:escapeXml(client.clientName)}</em></a>
		</p>
		<ul id="perun-table_with_attributes" class="perun-attributes">
			<c:forEach var="scope" items="${scopes}">
				<c:set var="scopeValue" value="${langProps[scope.value]}"/>
				<c:if test="${empty fn:trim(scopeValue)}">
					<c:set var="scopeValue" value="${scope.value}"/>
				</c:if>

				<c:set var="singleClaim" value="${fn:length(claims[scope.value]) eq 1}" />
				<li>
					<div class="row">
						<div class="col-sm-5">
							<div class="checkbox-wrapper">
								<input class="mt-0 mr-half" type="checkbox" name="scope_${ fn:escapeXml(scope.value) }" checked="checked"
									   id="scope_${fn:escapeXml(scope.value)}" value="${fn:escapeXml(scope.value)}">
							</div>
							<h2 class="perun-attrname h4">${scopeValue}</h2>
						</div>
						<div class="perun-attrcontainer col-sm-7">
							<span class="perun-attrvalue">
								<ul class="perun-attrlist">
									<c:forEach var="claim" items="${claims[scope.value]}">
										<c:choose>
											<c:when test="${not singleClaim}">
												<li>
													<c:set var="claimKey" value="${langProps[claim.key]}"/>
													<c:if test="${empty fn:trim(claimKey)}">
														<c:set var="claimKey" value="${claim.key}"/>
													</c:if>
													<strong>${claimKey}:</strong>
													<c:choose>
														<c:when test="${claim.value.getClass().name eq 'java.util.ArrayList'}">
															<br/>
															<ul>
																<c:forEach var="subValue" items="${claim.value}">
																	<li>${subValue}</li>
																</c:forEach>
															</ul>
														</c:when>
														<c:otherwise>
															${claim.value}
														</c:otherwise>
													</c:choose>
												</li>
											</c:when>
											<c:when test="${claim.value.getClass().name eq 'java.util.ArrayList'}">
												<c:forEach var="subValue" items="${claim.value}">
													<c:choose>
														<c:when test="${claim.key=='ga4gh_passport_v1'}">
															<li><%= GA4GHClaimSource.parseAndVerifyVisa((String)pageContext.findAttribute("subValue")).getPrettyString() %></li>
														</c:when>
														<c:otherwise>
															<li>${subValue}</li>
														</c:otherwise>
													</c:choose>
												</c:forEach>
											</c:when>
											<c:otherwise>
												<li>${claim.value}</li>
											</c:otherwise>
										</c:choose>
									</c:forEach>
								</ul>
							</span>
						</div>
					</div>
				</li>
			</c:forEach>
		</ul>
		<div class="row" id="saveconsentcontainer">
			<div class="col-xs-12">
				<div class="checkbox">
					<input type="checkbox" name="remember" id="remember-forever" value="until-revoked"/>
					<label for="remember-forever">${langProps['remember']}</label>
				</div>
			</div>
		</div>
		<input id="user_oauth_approval" name="user_oauth_approval" value="true" type="hidden" />
		<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
		<div class="row">
			<div class="col-sm-6">
				<div id="yesform">
					<button id="yesbutton" name="yes" type="submit" class="btn btn-success btn-lg btn-block btn-primary"
							onclick="$('#user_oauth_approval').attr('value', true);">
						<span>${langProps['yes']}</span>
					</button>
				</div>
			</div>
			<div class="col-sm-6">
				<div>
					<button id="nobutton" name="no" type="submit" class="btn btn-lg btn-default btn-block btn-no"
							onclick="$('#user_oauth_approval').attr('value', false);">
						<span>${langProps['no']}</span>
					</button>
				</div>
			</div>
		</div>
	</form>
</div>
</div><!-- wrap -->

<t:footer baseURL="${baseURL}" theme="${theme}"/>
