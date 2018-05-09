<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ page import="org.springframework.security.core.AuthenticationException"%>
<%@ page import="org.springframework.security.oauth2.common.exceptions.UnapprovedClientAuthenticationException"%>
<%@ page import="org.springframework.security.web.WebAttributes"%>
<%@ taglib prefix="authz" uri="http://www.springframework.org/security/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="o" tagdir="/WEB-INF/tags"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>

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
    <h1>You have been unapproved to access this resource</h1>
</div>
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