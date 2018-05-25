<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="org.springframework.security.core.AuthenticationException"%>
<%@ page import="org.springframework.security.oauth2.common.exceptions.UnapprovedClientAuthenticationException"%>
<%@ page import="org.springframework.security.web.WebAttributes"%>
<%@ taglib prefix="authz" uri="http://www.springframework.org/security/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="o" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags/common" %>
<%@ taglib prefix="elixir" tagdir="/WEB-INF/tags/elixir" %>
<%@ taglib prefix="cesnet" tagdir="/WEB-INF/tags/cesnet" %>
<%@ taglib prefix="bbmri" tagdir="/WEB-INF/tags/bbmri" %>
<%@ taglib prefix="ceitec" tagdir="/WEB-INF/tags/ceitec" %>

<c:if test="${empty title}">
	<c:set var="title" value="${langProps['consent_header']}"/>
</c:if>
<c:choose>
	<c:when test="${theme eq 'default'}">
		<o:header title="${title}"/>
	</c:when>
	<c:otherwise>
		<t:header title="${title}" reqURL="${reqURL}"/>
	</c:otherwise>
</c:choose>
<div id="content">
	<c:remove scope="session" var="SPRING_SECURITY_LAST_EXCEPTION" />
	<div class="row">
		<form name="confirmationForm"
			  action="${pageContext.request.contextPath.endsWith('/') ? pageContext.request.contextPath : pageContext.request.contextPath.concat('/') }authorize" method="post">
			<h3 id="attributeheader"><c:out value="${langProps['consent_attributes_header']}"/>
				<em> <c:out value="${client.clientName}" /></em>
			</h3>
			<p><c:out value="${langProps['consent_privacypolicy']}"/>
				<a target='_blank' href='<c:out value="${client.policyUri}" />'><em> <c:out value="${client.clientName}" /></em></a>
			</p>
			<table id="table_with_attributes" class="table attributes" summary="List the information about you that is about to be transmitted to the service you are going to login to">
				<c:forEach var="scope" items="${scopes}">
					<tr>
						<td>
							<div class="checkbox">
								<input type="checkbox" name="scope_${ fn:escapeXml(scope.value) }" checked="checked"
									   id="scope_${ fn:escapeXml(scope.value) }" value="${ fn:escapeXml(scope.value) }">
								<label class="form-check-label" for="scope_${ fn:escapeXml(scope.value) }">
									<span class="attrname">
										<c:out value="${langProps[scope.value]}" />
									</span>
								</label>
							</div>
							<c:if test="${ not empty claims[scope.value] }">
							<div class="attrvalue">
								<c:choose>
									<c:when test="${fn:length(claims[scope.value]) > 1}">
										<ul>
											<c:forEach var="claim" items="${ claims[scope.value] }">
												<li><strong><c:out value="${langProps[claim.key]}" />:</strong> <c:out value="${claim.value}" /></li>
											</c:forEach>
										</ul>
									</c:when>
									<c:otherwise>
										<c:forEach var="claim" items="${ claims[scope.value] }">
											<c:out value="${claim.value}" />
										</c:forEach>
									</c:otherwise>
								</c:choose>
							</div>
							</c:if>
						</td>
					</tr>
				</c:forEach>
			</table>
			<div class="row" style="margin: .5em 0;">
				<div class="col-12 checkbox-tight">
					<input class="form-check-input" type="checkbox" name="remember"
						   id="remember-forever" value="remember-forever">
					<label class="form-check-label" for="remember-forever"><c:out value="${langProps['remember']}"/></label>
				</div>
			</div>
			<input id="user_oauth_approval" name="user_oauth_approval" value="true" type="hidden" />
			<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
			<div class="row">
				<div class="col-xs-6">
					<input name="authorize" value="<c:out value="${langProps['yes']}"/>" type="submit"
						   onclick="$('#user_oauth_approval').attr('value',true)" class="btn btn-success btn-lg btn-block" />
				</div>
				<div class="col-xs-6">
					<input name="deny" value="<c:out value="${langProps['no']}"/>" type="submit"
						   onclick="$('#user_oauth_approval').attr('value',false)" class="btn btn-light btn-lg btn-block" />
				</div>
			</div>
		</form>
	</div>
</div>
</div><!-- wrap -->
<c:choose>
	<c:when test="${theme eq 'elixir'}">
		<elixir:footer />
	</c:when>
	<c:when test="${theme eq 'cesnet'}">
		<cesnet:footer />
	</c:when>
	<c:when test="${theme eq 'bbmri-eric'}">
		<bbmri:footer />
	</c:when>
	<c:when test="${theme eq 'ceitec'}">
		<ceitec:footer />
	</c:when>
	<c:otherwise>
		<o:footer />
	</c:otherwise>
</c:choose>
