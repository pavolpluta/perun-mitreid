<%@ tag pageEncoding="UTF-8" %>
<%@ attribute name="title" required="false"%>
<%@ attribute name="reqURL" required="true" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ tag import="com.google.gson.Gson" %>
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
          href="<c:out value='${baseUrl}'/>proxy/module.php/cesnet/res/img/icons/favicon.ico" />
    <link rel="stylesheet" type="text/css"
          href="<c:out value='${baseUrl}'/>proxy/resources/default.css" />
    <link rel="stylesheet" type="text/css"
          href="<c:out value='${baseUrl}'/>proxy/module.php/cesnet/res/bootstrap/css/bootstrap.min.css" />
    <link rel="stylesheet" type="text/css"
          href="<c:out value='${baseUrl}'/>proxy/module.php/cesnet/res/css/cesnet.css" />
    <link rel="stylesheet" media="screen" type="text/css"
          href="<c:out value='${baseUrl}'/>proxy/module.php/consent/style.css" />
    <link rel="stylesheet" media="screen" type="text/css"
          href="<c:out value='${baseUrl}'/>proxy/module.php/cesnet/res/css/consent.css" />
</head>
<body>
    <div id="wrap">
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
        <div id="header">
            <img src="<c:out value='${baseUrl}'/>proxy/module.php/cesnet/res/img/cesnet_RGB.png" alt="Cesnet logo">
            <h1 style="color: #222;"><c:out value="${langProps['consent_header']}"/></h1>
        </div>