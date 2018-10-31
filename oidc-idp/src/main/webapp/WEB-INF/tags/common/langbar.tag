<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div id="languagebar_line">
    <c:choose>
        <c:when test="${empty param.lang}">
            <%-- Lang was empty, continue with EN, change to CS --%>
            <c:set var="link" value="${reqURL}&lang=cs" />
            <div id="languagebar">English | <a href="<c:out value='${link}'/>">Čeština</a></div>
        </c:when>
        <c:when test="${ param.lang eq 'en'}">
            <%-- Lang was EN, change to CS --%>
            <c:set var="link" value="${fn:replace(reqURL, '&lang=en', '&lang=cs')}" />
            <div id="languagebar">English | <a href="<c:out value='${link}'/>">Čeština</a></div>
        </c:when>
        <c:otherwise>
            <%-- Lang was CS, change to EN --%>
            <c:set var="link" value="${fn:replace(reqURL, '&lang=cs', '&lang=en')}" />
            <div id="languagebar"><a href="<c:out value='${link}'/>">English</a> | Čeština</div>
        </c:otherwise>
    </c:choose>
</div>