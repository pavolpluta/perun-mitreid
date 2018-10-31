<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
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
										<c:set var="scopeValue" value="${langProps[scope.value]}"/>
										<c:if test="${empty fn:trim(scopeValue)}">
											<c:set var="scopeValue" value="${scope.value}"/>
										</c:if>
										<c:out value="${scopeValue}" />
									</span>
								</label>
							</div>
							<c:if test="${not empty claims[scope.value]}">
								<!-- PRINT OUT CLAIMS -->
								<div class="attrvalue">
									<ul>
										<c:set var="singleClaim" value="${fn:length(claims[scope.value]) eq 1}" />
										<c:forEach var="claim" items="${claims[scope.value]}">
											<c:choose>
												<c:when test="${not singleClaim}">
													<li>
														<c:set var="claimKey" value="${langProps[claim.key]}"/>
														<c:if test="${empty fn:trim(claimKey)}">
															<c:set var="claimKey" value="${claim.key}"/>
														</c:if>
														<strong><c:out value="${claimKey}" />:</strong>
														<c:choose>
															<c:when test="${claim.value.getClass().name eq 'java.util.ArrayList'}">
																<br/>
																<ul>
																	<c:forEach var="subValue" items="${claim.value}">
																		<li><c:out value="${subValue}"/></li>
																	</c:forEach>
																</ul>
															</c:when>
															<c:otherwise>
																<c:out value="${claim.value}" />
															</c:otherwise>
														</c:choose>
													</li>
												</c:when>
												<c:when test="${claim.value.getClass().name eq 'java.util.ArrayList'}">
													<c:forEach var="subValue" items="${claim.value}">
														<li><c:out value="${subValue}"/></li>
													</c:forEach>
												</c:when>
												<c:otherwise>
													<li><c:out value="${claim.value}" /></li>
												</c:otherwise>
											</c:choose>
										</c:forEach>
									</ul>
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