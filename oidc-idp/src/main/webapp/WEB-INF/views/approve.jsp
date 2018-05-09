<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="org.springframework.security.core.AuthenticationException"%>
<%@ page import="org.springframework.security.oauth2.common.exceptions.UnapprovedClientAuthenticationException"%>
<%@ page import="org.springframework.security.web.WebAttributes"%>
<%@ taglib prefix="authz" uri="http://www.springframework.org/security/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="o" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="elixir" tagdir="/WEB-INF/tags/elixir" %>
<%@ taglib prefix="cesnet" tagdir="/WEB-INF/tags/cesnet" %>
<%@ taglib prefix="bbmri" tagdir="/WEB-INF/tags/bbmri" %>
<%@ taglib prefix="ceitec" tagdir="/WEB-INF/tags/ceitec" %>

<spring:message code="approve.title" var="title"/>
<c:choose>
	<c:when test="${theme eq 'elixir'}">
		<elixir:header title="${title}"/>
	</c:when>
	<c:when test="${theme eq 'cesnet'}">
		<cesnet:header title="${title}"/>
	</c:when>
	<c:when test="${theme eq 'bbmri'}">
		<bbmri:header title="${title}"/>
	</c:when>
	<c:when test="${theme eq 'ceitec'}">
		<ceitec:header title="${title}"/>
	</c:when>
	<c:otherwise>
		<o:header title="${title}"/>
	</c:otherwise>
</c:choose>
<div id="content">
	<c:remove scope="session" var="SPRING_SECURITY_LAST_EXCEPTION" />
	<div class="row">
		<form name="confirmationForm"
			  action="${pageContext.request.contextPath.endsWith('/') ? pageContext.request.contextPath : pageContext.request.contextPath.concat('/') }authorize" method="post">
			<input id="user_oauth_approval" name="user_oauth_approval" value="true" type="hidden" />
			<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
			<div class="row">
				<div class="col-xs-6">
					<input name="authorize" value="Yes, continue" type="submit"
						   onclick="$('#user_oauth_approval').attr('value',true)" class="btn btn-success btn-lg btn-block" />
				</div>
				<div class="col-xs-6">
					<input name="deny" value="No, cancel" type="submit"
						   onclick="$('#user_oauth_approval').attr('value',false)" class="btn btn-light btn-lg btn-block" />
				</div>
			</div>
			<p>Privacy policy for the service <a target='_blank' href='<c:out value="${client.policyUri}" />'>
				<c:choose>
					<c:when test="${empty client.clientName}">
						<em><c:out value="${client.clientId}" /></em>
					</c:when>
					<c:otherwise>
						<em><c:out value="${client.clientName}" /></em>
					</c:otherwise>
				</c:choose>
			</a></p>
			<h3 id="attributeheader">Information that will be sent to
				<c:choose>
					<c:when test="${empty client.clientName}">
						<em><c:out value="${client.clientId}" /></em>
					</c:when>
					<c:otherwise>
						<em><c:out value="${client.clientName}" /></em>
					</c:otherwise>
				</c:choose>
			</h3>
			<table id="table_with_attributes" class="table attributes" summary="List the information about you that is about to be transmitted to the service you are going to login to">
				<caption>User information</caption>
				<c:forEach var="scope" items="${scopes}">
					<c:if test="${not empty claims[scope.value]}">
						<tr>
							<td>
								<input type="checkbox" name="scope_${ fn:escapeXml(scope.value) }" checked="checked"
									   id="scope_${ fn:escapeXml(scope.value) }" value="${ fn:escapeXml(scope.value) }">
								<label class="form-check-label" for="scope_${ fn:escapeXml(scope.value) }">
									<span class="attrname" style="text-transform: capitalize;">
										<c:out value="${ fn:toLowerCase(fn:escapeXml(scope.value))}" />
									</span>
								</label>

								<div class="attrvalue">
									<c:forEach var="claim" items="${ claims[scope.value] }">
										<b><c:out value="${claim.key}" />:</b> <c:out value="${claim.value}" /><br />
									</c:forEach>
								</div>
							</td>
						</tr>
					</c:if>
				</c:forEach>
			</table>
			<div class="row" style="margin: .5em 0;">
				<h4><spring:message code="approve.remember.title"/>:</h4>
				<div class="col-12 form-check">
					<input class="form-check-input" type="radio" name="remember"
						   id="remember-forever" value="until-revoked/" ${ !consent ? 'checked="checked"' : '' }>
					<label class="form-check-label" for="remember-forever">
						<spring:message code="approve.remember.until_revoke"/>
					</label>
				</div>
				<div class="col-12 form-check">
					<input class="form-check-input" type="radio" name="remember" id="remember-hour" value="one-hour">
					<label class="form-check-label" for="remember-hour">
						<spring:message code="approve.remember.one_hour"/>
					</label>
				</div>
				<div class="col-12 form-check">
					<input class="form-check-input" type="radio" name="remember"
						   id="remember-not" value="none" ${ consent ? 'checked="checked"' : '' }>
					<label class="form-check-label" for="remember-not">
						<spring:message code="approve.remember.next_time"/>
					</label>
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
	<c:when test="${theme eq 'bbmri'}">
		<bbmri:footer />
	</c:when>
	<c:when test="${theme eq 'ceitec'}">
		<ceitec:footer />
	</c:when>
	<c:otherwise>
		<o:footer />
	</c:otherwise>
</c:choose>