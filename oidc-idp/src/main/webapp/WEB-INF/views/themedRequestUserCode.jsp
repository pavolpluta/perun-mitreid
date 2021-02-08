<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags/common" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="baseURL" value="${baseURL}"/>
<c:set var="samlResourcesURL" value="${samlResourcesURL}"/>

<%

    List<String> cssLinks = new ArrayList<>();
    pageContext.setAttribute("cssLinks", cssLinks);

%>

    <t:header title="${langProps['request_code_title']}" reqURL="${reqURL}" baseURL="${baseURL}"
              cssLinks="${cssLinks}" theme="${theme}"/>

</div> <%-- header --%>

<div id="content" class="text-center">
    <h1>${langProps['request_code_header']}</h1>
    <div class="error_message" style="word-wrap: break-word;">
        <c:if test="${ error != null }">
            <c:choose>
                <c:when test="${ error == 'noUserCode' }">
                    <h1>${langProps['noUserCode']}</h1>
                </c:when>
                <c:when test="${ error == 'expiredUserCode' }">
                    <h1>${langProps['expiredUserCode']}</h1>
                </c:when>
                <c:when test="${ error == 'userCodeAlreadyApproved' }">
                    <h1>${langProps['userCodeAlreadyApproved']}</h1>
                </c:when>
                <c:when test="${ error == 'userCodeMismatch' }">
                    <h1>${langProps['userCodeMismatch']}</h1>
                </c:when>
                <c:otherwise>
                    <h1>${langProps['userCodeError']}</h1>
                </c:otherwise>
            </c:choose>
        </c:if>
    </div>

    <form name="confirmationForm"
          action="${ config.issuer }${ config.issuer.endsWith('/') ? '' : '/' }device/verify" method="post">

        <div class="row-fluid">
            <div class="span12">
                <div>
                    <div class="input-block-level input-xlarge">
                        <input type="text" name="user_code" placeholder="code" autocorrect="off" autocapitalize="off" autocomplete="off" spellcheck="false" value="" />
                    </div>
                </div>
            </div>
        </div>
        <div class="row-fluid">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <input name="approve" value="${langProps['userCode.submit']}" type="submit" class="btn btn-info btn-large" />
        </div>

    </form>
</div>

</div><!-- ENDWRAP -->

<t:footer baseURL="${baseURL}" theme="${theme}"/>