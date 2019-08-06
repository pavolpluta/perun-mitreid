<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="o" tagdir="/WEB-INF/tags"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags/common"%>
<%@ taglib prefix="elixir" tagdir="/WEB-INF/tags/elixir" %>
<%@ taglib prefix="cesnet" tagdir="/WEB-INF/tags/cesnet" %>
<%@ taglib prefix="bbmri" tagdir="/WEB-INF/tags/bbmri" %>
<%@ taglib prefix="ceitec" tagdir="/WEB-INF/tags/ceitec" %>
<%@ taglib prefix="europdx" tagdir="/WEB-INF/tags/europdx" %>

<c:set var="title" value="${langProps['go_to_registration_title']}" />
<c:choose>
    <c:when test="${theme eq 'default'}">
        <o:header title="${title}"/>
    </c:when>
    <c:otherwise>
        <t:header title="${title}" reqURL="${reqURL}"/>
    </c:otherwise>
</c:choose>
<div id="content">
    <div id="head">
        <h1>${langProps['go_to_registration_header1']}
            <c:if test="${not empty client.clientName and not empty client.clientUri}">
                &#32;<a href="${fn:escapeXml(client.uri)}">${fn:escapeXml(client.clientName)}</a>
            </c:if>
            <c:if test="${not empty client.clientName}">
                &#32;${fn:escapeXml(client.clientName)}
            </c:if>
            &#32;${langProps['go_to_registration_header2']}
        </h1>
    </div>
    <form method="GET" action="${action}">
        <hr/>
        <br/>
        <input type="hidden" name="client_id" value="${client_id}" />
        <input type="hidden" name="facility_id" value="${facility_id}" />
        <input type="hidden" name="user_id" value="${user_id}" />
        <input type="submit" name="continueToRegistration" value="${langProps['go_to_registration_continue']}"
               class="btn btn-lg btn-primary btn-block">
    </form>
</div>
</div><!-- ENDWRAP -->
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
    <c:when test="${theme eq 'europdx'}">
        <europdx:footer />
    </c:when>
    <c:otherwise>
        <o:footer />
    </c:otherwise>
</c:choose>