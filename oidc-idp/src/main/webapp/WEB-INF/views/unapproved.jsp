<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ page import="org.springframework.security.core.AuthenticationException"%>
<%@ page import="org.springframework.security.oauth2.common.exceptions.UnapprovedClientAuthenticationException"%>
<%@ page import="org.springframework.security.web.WebAttributes"%>
<%@ taglib prefix="authz" uri="http://www.springframework.org/security/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="o" tagdir="/WEB-INF/tags"%>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags/common"%>
<%@ taglib prefix="elixir" tagdir="/WEB-INF/tags/elixir" %>
<%@ taglib prefix="cesnet" tagdir="/WEB-INF/tags/cesnet" %>
<%@ taglib prefix="bbmri" tagdir="/WEB-INF/tags/bbmri" %>
<%@ taglib prefix="ceitec" tagdir="/WEB-INF/tags/ceitec" %>

<c:choose>
    <c:when test="${theme eq 'default'}">
        <o:header title="${title}"/>
    </c:when>
    <c:otherwise>
        <t:header title="${title}" reqURL="${reqURL}"/>
    </c:otherwise>
</c:choose>
<div id="content">
    <style>
        .error_message{
            word-wrap: break-word;
        }
    </style>
    <div class="error_message">
        <c:forEach var="contactIter" items="${client.contacts}" end="0">
            <c:set var="contact" value="${contactIter}" />
        </c:forEach>
        <c:if test="${empty contact}">
            <c:choose>
                <c:when test="${theme eq 'elixir'}">
                    <c:set var="contact" value="aai-contact@elixir-europe.org"/>
                </c:when>
                <c:when test="${theme eq 'cesnet'}">
                    <c:set var="contact" value="support@cesnet.cz"/>
                </c:when>
                <c:when test="${theme eq 'ceitec'}">
                    <c:set var="contact" value="idm@ics.muni.cz"/>
                </c:when>
                <c:when test="${theme eq 'bbmri-eric'}">
                    <c:set var="contact" value="aai-infrastructure@lists.bbmri-eric.eu"/>
                </c:when>
            </c:choose>
        </c:if>
        <h1><c:out value="${langProps['403_header']}"/></h1>
        <p><c:out value="${langProps['403_text']} ${client.clientName}"/><br>
            <c:if test="${not empty client.clientUri}">
                <c:out value="${langProps['403_informationPage'] }"/><a href="<c:out value='${client.clientUri}'/>"><c:out value='${client.clientUri}'/></a>
            </c:if>
        </p>

        <p><c:out value="${langProps['403_contactSupport'] }"/>
           <a href="mailto:<c:out value='${contact}?subject=${langProps["403_subject"]} ${client.clientName}'/>">
                <c:out value="${contact}"/>
           </a>
        </p>
    </div>
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
    <c:otherwise>
        <o:footer />
    </c:otherwise>
</c:choose>