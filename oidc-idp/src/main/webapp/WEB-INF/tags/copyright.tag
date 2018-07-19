<%@ tag import="org.springframework.web.context.support.WebApplicationContextUtils" %>
<%@ tag import="cz.muni.ics.oidc.PerunOidcConfig" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<c:if test="${ config.heartMode }"><span class="pull-left"><img src="resources/images/heart_mode.png" alt="HEART Mode" title="This server is running in HEART Compliance Mode" /></span> </c:if>
<%
    PerunOidcConfig perunOidcConfig = WebApplicationContextUtils.getWebApplicationContext(application).getBean("perunOidcConfig", PerunOidcConfig.class);
%>
Powered by
<a href="https://github.com/CESNET/perun-mitreid">Perun MITREid</a> <span class="label"><%=perunOidcConfig.getPerunOIDCVersion()%></span>
(modified <a href="https://github.com/mitreid-connect/">MITREid Connect <span class="label"><%=perunOidcConfig.getMitreidVersion()%></span></a>)
<span class="pull-right">&copy; 2017 The MIT Internet Trust Consortium.</span>.
