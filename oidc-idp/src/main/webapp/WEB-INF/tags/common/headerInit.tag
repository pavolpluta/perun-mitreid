<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="reqURL" required="true" %>
<%@ attribute name="baseURL" required="true" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="${lang}" xml:lang="${lang}">

<head>

    <base href="${config.issuer}">
    <title>${config.topbarTitle} - ${title}</title>

    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="viewport" content="width=device-width, height=device-height, initial-scale=1.0" />
    <meta name="robots" content="noindex, nofollow" />

    <link rel="stylesheet" type="text/css" href="${baseURL}proxy/resources/default.css" />

    <style type="text/css">
        .mt-0 {
            margin-top: 0 !important;
        }
        .checkbox-wrapper {
            float: left;
        }
        .attrname-formatter {
            display: block;
            margin-left: 2em !important;
        }
    </style>
