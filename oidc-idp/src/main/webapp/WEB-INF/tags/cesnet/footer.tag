<%@tag pageEncoding="UTF-8" %>
<%@ attribute name="js" required="false"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<jsp:useBean id="date" class="java.util.Date" />
<div id="footer">
    <footer>
        <div class="container">
            <div class="row">
                <div class="col-md-4 logo">
                    <a href="http://www.cesnet.cz/">
                        <img src="https://login.cesnet.cz/proxy/module.php/cesnet/res/img/logo-cesnet.png" style="width: 250px;">
                    </a>
                    <a href="https://www.cerit-sc.cz">
                        <img src="https://login.cesnet.cz/proxy/module.php/cesnet/res/img/logo-cerit.png">
                    </a>
                </div>
                <div class="col-md-8">
                    <div class="row">
                        <div class="col col-sm-6">
                            <h2>OTHER CESNET PROJECTS</h2>
                            <ul>
                                <li><a href="http://www.cesnet.cz/wp-content/uploads/2014/04/CzechLight-family_Posp%C3%ADchal.pdf">CzechLight</a></li>
                                <li><a href="http://www.ultragrid.cz/en">UltraGrid</a></li>
                                <li><a href="http://www.4kgateway.com/">4k Gateway</a></li>
                                <li><a href="http://shongo.cesnet.cz/">Shongo</a></li>
                                <li><a href="http://www.cesnet.cz/sluzby/sledovani-provozu-site/sledovani-infrastruktury/">FTAS a G3</a></li>
                                <li><a href="https://www.liberouter.org/">Librerouter</a></li>
                            </ul>
                        </div>
                        <div class="col col-sm-6">
                            <h2>HELPDESK</h2>
                            TEL: +420 224 352 994<br>
                            GSM: +420 602 252 531<br>
                            FAX: +420 224 313 211<br>
                            <a href="mailto:perun@cesnet.cz">perun@cesnet.cz</a>
                        </div>
                    </div>
                </div>
            </div>
            <div class="row">
                <div class="col col-sm-12 copyright">
                    &copy; 1991–<fmt:formatDate value="${date}" pattern="yyyy" /> | CESNET, z. s. p. o. &amp; CERIT-SC
                </div>
            </div>
        </div>
    </footer>
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
<script type="text/javascript" src="https://login.cesnet.cz/proxy/resources/script.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" crossorigin="anonymous"
        integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa"></script>
</body>
</html>
