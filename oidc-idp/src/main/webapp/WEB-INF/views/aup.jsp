<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="o" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags/common" %>
<%@ taglib prefix="elixir" tagdir="/WEB-INF/tags/elixir" %>
<%@ taglib prefix="cesnet" tagdir="/WEB-INF/tags/cesnet" %>
<%@ taglib prefix="bbmri" tagdir="/WEB-INF/tags/bbmri" %>
<%@ taglib prefix="ceitec" tagdir="/WEB-INF/tags/ceitec" %>
<%@ taglib prefix="europdx" tagdir="/WEB-INF/tags/europdx" %>

<c:if test="${empty title}">
    <c:set var="title" value="${langProps['aup_header']}"/>
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
    <h3>${langProps['must_agree_aup']}</h3>
    <form method="POST" action="">
        <c:forEach var="aup" items="${newAups}">
            <div>
                <p style="font-size: 16px; padding: 0; margin: 0;">${langProps['org_vo']} ${" "}<strong><c:out value="${aup.key}"/></strong></p>
                <p>${langProps['see_aup']} ${aup.value.version} ${" "}<a href="<c:out value="${aup.value.link}"/>">${langProps['here']}</a></p>
            </div>
        </c:forEach>
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <div class="form-group">
            <input type="submit" value="${langProps['agree_aup']}" class="btn btn-lg btn-primary btn-block">
        </div>
    </form>
</div>
</div><!-- wrap -->
<c:choose>
    <c:when test="${theme eq 'elixir'}">
        <elixir:footer/>
    </c:when>
    <c:when test="${theme eq 'cesnet'}">
        <cesnet:footer/>
    </c:when>
    <c:when test="${theme eq 'bbmri'}">
        <bbmri:footer/>
    </c:when>
    <c:when test="${theme eq 'ceitec'}">
        <ceitec:footer/>
    </c:when>
    <c:when test="${theme eq 'europdx'}">
        <europdx:footer/>
    </c:when>
    <c:otherwise>
        <o:footer/>
    </c:otherwise>
</c:choose>
