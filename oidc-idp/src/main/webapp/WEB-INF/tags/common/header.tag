<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="title" required="false" %>
<%@ attribute name="reqURL" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="o" tagdir="/WEB-INF/tags/common" %>
<c:choose>
    <c:when test="${theme eq 'cesnet'}">
        <c:set var="logo" value="cesnet_RGB.png"/>
    </c:when>
    <c:when test="${theme eq 'elixir'}">
        <c:set var="logo" value="logo_256.png"/>
    </c:when>
    <c:when test="${theme eq 'ceitec'}">
        <c:set var="logo" value="logo_512.png"/>
    </c:when>
    <c:when test="${theme eq 'bbmri'}">
        <c:set var="logo" value="BBMRI-ERIC-gateway-for-health_430.png"/>
    </c:when>
</c:choose>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="${lang}" xml:lang="${lang}">
<head>
    <c:set var="baseUrl" value="${fn:substringBefore(config.issuer, 'oidc')}" />
    <base href="${config.issuer}">
    <title>${config.topbarTitle} - ${title}</title>
    <!-- meta -->
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="viewport" content="width=device-width, height=device-height, initial-scale=1.0" />
    <meta name="robots" content="noindex, nofollow" />
    <!-- link -->
    <link rel="icon" type="image/icon"
          href="${baseUrl}proxy/module.php/${theme}/res/img/icons/favicon.ico" />
    <link rel="stylesheet" type="text/css"
          href="${baseUrl}proxy/resources/default.css" />
    <link rel="stylesheet" type="text/css"
          href="${baseUrl}proxy/module.php/${theme}/res/bootstrap/css/bootstrap.min.css" />
    <link rel="stylesheet" type="text/css"
          href="${baseUrl}proxy/module.php/${theme}/res/css/${theme}.css" />
    <c:if test="${page eq 'consent'}">
        <link rel="stylesheet" media="screen" type="text/css"
              href="${baseUrl}proxy/module.php/consent/style.css" />
        <link rel="stylesheet" media="screen" type="text/css"
              href="${baseUrl}proxy/module.php/${theme}/res/css/consent.css" />
    </c:if>
    <c:if test="${page eq 'regForm'}">
        <link rel="stylesheet" media="screen" type="text/css"
              href="${baseUrl}proxy/module.php/perun/res/css/perun_identity_choose_vo_and_group.css" />
    </c:if>
    <c:if test="${page eq 'regContinue'}">
        <link rel="stylesheet" media="screen" type="text/css"
              href="${baseUrl}proxy/module.php/perun/res/css/perun_identity_go_to_registration.css" />
    </c:if>
</head>
<body>
<div id="wrap">
    <c:if test="${theme eq 'cesnet'}">
        <o:langbar />
    </c:if>
    <div id="header">
        <img src="${baseUrl}proxy/module.php/${theme}/res/img/${logo}" alt="${theme} logo">
        <c:choose>
            <c:when test="${page eq 'consent'}">
                <h1 style="color: #222;">${langProps['consent_header']}</h1>
            </c:when>
            <c:when test="${page eq 'unapproved'}">
                <h1>
                    <a class="header-link" href="/proxy/">Proxy IdP</a>
                </h1>
            </c:when>
        </c:choose>
    </div>