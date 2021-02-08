<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags/common" %>

<c:set var="baseURL" value="${baseURL}"/>
<c:set var="samlResourcesURL" value="${samlResourcesURL}"/>

<%

List<String> cssLinks = new ArrayList<>();
pageContext.setAttribute("cssLinks", cssLinks);

%>

<t:header title="${langProps['device_approved_title']}" reqURL="${reqURL}" baseURL="${baseURL}" cssLinks="${cssLinks}" theme="${theme}"/>

</div> <%-- header --%>

<div id="content" class="text-center">
    <div id="head">
        <h1>
            <c:choose>
                <c:when test="${ approved }">
                    <p>&#x2714; ${langProps['device_approved_approved']}</p>
                </c:when>
                <c:otherwise>
                    <p>&#x2717; ${langProps['device_approved_notApproved']}</p>
                </c:otherwise>
            </c:choose>
        </h1>
    </div>

    <h1>
        <c:choose>
            <c:when test="${empty client.clientName}">
                <em><c:out value="${client.clientId}" /></em>
            </c:when>
            <c:otherwise>
                <em><c:out value="${client.clientName}" /></em>
            </c:otherwise>
        </c:choose>
    </h1>
</div>

</div> <%-- wrap --%>

<t:footer baseURL="${baseURL}" theme="${theme}"/>
