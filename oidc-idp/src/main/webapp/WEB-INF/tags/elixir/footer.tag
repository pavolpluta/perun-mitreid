<%@tag pageEncoding="UTF-8" %>
<%@ attribute name="js" required="false"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<jsp:useBean id="date" class="java.util.Date" />
<div id="footer">
    <div style="margin: 0 auto; max-width: 1000px;">
        <div style="float: left;">
            <img src="https://login.elixir-czech.org/proxy/module.php/elixir/res/img/logo_64.png">
        </div>
        <div style="float: left;">
            <p>ELIXIR, Wellcome Trust Genome Campus, Hinxton, Cambridgeshire, CB10 1SD, UK&nbsp; &nbsp; +44&nbsp;(0)1223&nbsp;492-670&nbsp;&nbsp;
                <a href="mailto:info@elixir-europe.org">info@elixir-europe.org</a>
            </p>
            <p>Copyright &copy; ELIXIR <fmt:formatDate value="${date}" pattern="yyyy" /> |
                <a href="https://www.elixir-europe.org/legal/privacy">Privacy</a> |
                <a href="https://www.elixir-europe.org/legal/cookies">Cookies</a> |
                <a href="https://www.elixir-europe.org/legal/terms-of-use">Terms of use</a>
            </p>
        </div>
    </div>
</div>
<!-- javascript
================================================== -->
<!-- Placed at the end of the document so the pages load faster -->

<!-- Load jQuery up here so that we can use in-page functions -->
<script type="text/javascript" src="resources/js/lib/jquery.js"></script>
<script type="text/javascript" charset="UTF-8" src="resources/js/lib/moment-with-locales.js"></script>
<script type="text/javascript" src="resources/js/lib/i18next.js"></script>
<script type="text/javascript">
    $(document).ready(function() {
        $('.claim-tooltip').popover();
        $('.claim-tooltip').on('click', function(e) {
            e.preventDefault();
            $(this).popover('show');
        });

        $(document).on('click', '#toggleMoreInformation', function(event) {
            event.preventDefault();
            if ($('#moreInformation').is(':visible')) {
                // hide it
                $('.moreInformationContainer', this.el).removeClass('alert').removeClass('alert-info').addClass('muted');
                $('#moreInformation').hide('fast');
                $('#toggleMoreInformation i').attr('class', 'icon-chevron-right');
            } else {
                // show it
                $('.moreInformationContainer', this.el).addClass('alert').addClass('alert-info').removeClass('muted');
                $('#moreInformation').show('fast');
                $('#toggleMoreInformation i').attr('class', 'icon-chevron-down');
            }
        });

        var creationDate = "<c:out value="${ client.createdAt }" />";
        var displayCreationDate = $.t('approve.dynamically-registered-unkown');
        var hoverCreationDate = "";
        if (creationDate != null && moment(creationDate).isValid()) {
            creationDate = moment(creationDate);
            if (moment().diff(creationDate, 'months') < 6) {
                displayCreationDate = creationDate.fromNow();
            } else {
                displayCreationDate = "on " + creationDate.format("LL");
            }
            hoverCreationDate = creationDate.format("LLL");
        }

        $('#registrationTime').html(displayCreationDate);
        $('#registrationTime').attr('title', hoverCreationDate);
    });
</script>
<script type="text/javascript">
    $.i18n.init({
        fallbackLng: "en",
        lng: "${config.locale}",
        resGetPath: "resources/js/locale/__lng__/__ns__.json",
        ns: {
            namespaces: ${config.languageNamespacesString},
            defaultNs: '${config.defaultLanguageNamespace}'
        },
        fallbackNS: ${config.languageNamespacesString}
    });
    moment.locale("${config.locale}");
    // safely set the title of the application
    function setPageTitle(title) {
        document.title = "${config.topbarTitle} - " + title;
    }

    // get the info of the current user, if available (null otherwise)
    function getUserInfo() {

    }

    // get the authorities of the current user, if available (null otherwise)
    function getUserAuthorities() {

    }

    // is the current user an admin?
    // NOTE: this is just for
    function isAdmin() {
        var auth = getUserAuthorities();
        if (auth && _.contains(auth, "ROLE_ADMIN")) {
            return true;
        } else {
            return false;
        }
    }

    var heartMode = ${config.heartMode};
</script>
<script type="text/javascript" src="resources/bootstrap2/js/bootstrap.js"></script>
<script type="text/javascript" src="resources/js/lib/underscore.js"></script>
<script type="text/javascript" src="resources/js/lib/backbone.js"></script>
<script type="text/javascript" src="resources/js/lib/purl.js"></script>
<script type="text/javascript" src="resources/js/lib/bootstrapx-clickover.js"></script>
<script type="text/javascript" src="resources/js/lib/bootstrap-sheet.js"></script>
<script type="text/javascript" src="resources/js/lib/bootpag.js"></script>
<script type="text/javascript" src="resources/js/lib/retina.js"></script>
<c:if test="${js != null && js != ''}">
	<script type="text/javascript">
	
		// set up a global variable for UI components to hang extensions off of
		
		var ui = {
			templates: ["resources/template/admin.html"], // template files to load for UI
			routes: [], // routes to add to the UI {path: URI to map to, name: unique name for internal use, callback: function to call when route is activated}
			init: [] // functions to call after initialization is complete
		};
	
	</script>
	<c:forEach var="file" items="${ ui.jsFiles }">
		<script type="text/javascript" src="<c:out value="${ file }" />" ></script>
	</c:forEach>
	<script type="text/javascript" src="resources/js/admin.js"></script>
</c:if>
<div id="templates" class="hide"></div>
<script type="text/javascript" src="https://login.elixir-czech.org/proxy/resources/script.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" crossorigin="anonymous"
		integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa"></script>
</body>
</html>
