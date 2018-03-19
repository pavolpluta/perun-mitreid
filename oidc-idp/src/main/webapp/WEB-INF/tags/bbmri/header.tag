<%@tag pageEncoding="UTF-8" %>
<%@attribute name="title" required="false"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ tag import="com.google.gson.Gson" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
<head>
    <base href="${config.issuer}">
    <title>${config.topbarTitle} - ${title}</title>
    <!-- meta -->
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="viewport" content="width=device-width, height=device-height, initial-scale=1.0" />
    <meta name="robots" content="noindex, nofollow" />
    <!-- link -->
    <link rel="icon" type="image/icon"
          href="https://login.bbmri-eric.eu/proxy/resources/icons/favicon.ico" />
    <link rel="stylesheet" type="text/css"
          href="https://login.bbmri-eric.eu/proxy/resources/default.css" />
    <link rel="stylesheet" type="text/css"
          href="https://login.bbmri-eric.eu/proxy/module.php/bbmri-eric/res/bootstrap/css/bootstrap.min.css" />
    <link rel="stylesheet" type="text/css"
          href="https://login.bbmri-eric.eu/proxy/module.php/bbmri-eric/res/css/bbmri-eric.css" />
    <link rel="stylesheet" media="screen" type="text/css"
          href="https://login.bbmri-eric.eu/proxy/module.php/consent/style.css" />
    <link rel="stylesheet" media="screen" type="text/css"
          href="https://login.bbmri-eric.eu/proxy/module.php/bbmri-eric/res/css/consent.css" />
</head>
<body>
    <div id="wrap">
        <div id="header">
            <img src="https://login.bbmri-eric.eu/proxy/module.php/bbmri-eric/res/img/BBMRI-ERIC-gateway-for-health_430.png" alt="BBMRI-ERIC logo">
            <h1 style="color: #222;">Consent about releasing personal information</h1>
        </div>