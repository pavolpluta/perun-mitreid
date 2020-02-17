############################################
## Template for application configuration ##
############################################

### APPLICATION PROPERTIES ###
#main.oidc.issuer.url=https://perun-dev.meta.zcu.cz/oidc/
#jwk=file:///etc/perun/perun-oidc-keystore.jwks                             # Path to JWKs
#admins=3197,59835                                                          # Comma separated list of IDs for admins
#accessTokenClaimsModifier=cz.muni.ics.***.NoOpAccessTokenClaimsModifier    # Fully qualified class name for Access token modifier
#idpFilters.askPerun.enabled=false                                          # Enable / disable filtering of IdPs on WAYF
#registrar.url=https://perun-dev.cesnet.cz/allfed/registrar/                # URL of Perun registrar module
#id_token.scopes=openid,profile,email,phone,address                         # Comma separated list of scopes included in ID_TOKEN


### PROXY ###
#proxy.extSource.name=      # Name of extSource to use for fetching user
#proxy.base.url=            # Base URL of proxy/idp
#proxy.login.url=           # URL to login on proxy/idp
#proxy.logout.url=          # URL to logout on proxy/idp


### PERUN RPC ###
#perun.rpc.url=https://perun.elixir-czech.cz/krb/rpc
#perun.rpc.user=xxxxx
#perun.rpc.password=yyyyy


### LDAP ###
#ldap.host=perun.cesnet.cz
#ldap.user=xxxxx
#ldap.password=yyyyyyy
#ldap.timeoutSecs=120
#ldap.baseDN=dc=perun,dc=cesnet,dc=cz


### JDBC ###
#jdbc.driver=org.mariadb.jdbc.Driver
#jdbc.url=jdbc:mariadb://localhost:3306/oidc
#jdbc.user=oidc
#jdbc.password=oidc
#jdbc.platform=org.eclipse.persistence.platform.database.MySQLPlatform


### STATISTICS JDBC ###
#stats.jdbc.url=jdbc:mariadb://localhost:3306/STATS
#stats.jdbc.user=user
#stats.jdbc.password=password


### WEB INTERFACE ###
#logo.image.url=resources/images/perun_24px.png                 # Logo displayed in top bar in GUI
#topbar.title=Perun OIDC                                        # Title displayed in top bar in GUI
#web.theme=default [muni|cesnet|elixir|europdx|bbmri|ceitec]    # theme for web interface
#web.langs=EN,CS,SK [EN|CS|SK]                                  # comma separated list of enabled languages
#web.langs.customfiles.path=/etc/perun                          # path to custom localization files
#web.baseURL=                                                   # base URL where the web sits


### FACILITY ATTRIBUTES MAPPING ###
#facility.attrs.checkGroupMembership=urn:perun:facility:attribute-def:def:OIDCCheckGroupMembership
#facility.attrs.allowRegistration=urn:perun:facility:attribute-def:def:allowRegistration
#facility.attrs.registrationURL=urn:perun:facility:attribute-def:def:registrationURL
#facility.attrs.dynamicRegistration=urn:perun:facility:attribute-def:def:dynamicRegistration
#facility.attrs.voShortNames=urn:perun:facility:attribute-def:virt:voShortNames
#facility.attrs.clientId=urn:perun:facility:attribute-def:def:OIDCClientID
#facility.attrs.wayfFilter=urn:perun:facility:attribute-def:def:wayfFilter
#facility.attrs.wayfEFilter=urn:perun:facility:attribute-def:def:wayfEFilter


#################################################################
### USER ATTRIBUTES MAPPING                                   ###
## attribute.[scope].[claim]=[mapping] - Template for mapping  ##
#################################################################

#attribute.openid.sub=urn:perun:user:attribute-def:core:id
#attribute.profile.preferred_username=urn:perun:user:attribute-def:def:login-namespace:einfra
#attribute.profile.given_name=urn:perun:user:attribute-def:core:firstName
#attribute.profile.middle_name=urn:perun:user:attribute-def:core:middleName
#attribute.profile.family_name=urn:perun:user:attribute-def:core:lastName
#attribute.profile.name=urn:perun:user:attribute-def:core:displayName
#attribute.profile.zoneinfo=urn:perun:user:attribute-def:def:timezone
#attribute.profile.locale=urn:perun:user:attribute-def:def:preferredLanguage
#attribute.email.email=urn:perun:user:attribute-def:def:preferredMail
#attribute.phone.phone=urn:perun:user:attribute-def:def:phone
#attribute.address.address.formatted=urn:perun:user:attribute-def:def:address


###############################################################################################################
### CUSTOM CLAIMS                                                                                           ###
## custom.claim.[claimName].[propertyName]=[val] - Template for options, see class documentation for options ##
###############################################################################################################

#custom.claims=c1,c2                            # Comma separated list of names for custom claims

# CUSTOM CLAIM C1
#custom.claim.c1.scope=                    # Name of scope for claim c1
#custom.claim.c1.source.class=             # Fully qualified class name for the claim source, defaults to PerunAttributeClaimSource
#custom.claim.c1.source.prop1=             # Value for custom claim source C1 property PROP1
#custom.claim.c1.modifier.class=           # Fully qualified class name for the claim modifier, modifier class is optional
#custom.claim.c1.modifier.prop1=           # Value for custom claim modifier C1 property PROP1


##################################################################################################################
### USERINFO MODIFIERS                                                                                         ###
## userInfo.modifier.[modName].[propertyName]=[val] - Template for options, see class documentation for options ##
##################################################################################################################

#userInfo.modifiers=mod1,mod2           # Comma separated list of names for UserInfo object modifiers

# USER INFO MODIFIER MOD1
#userInfo.modifier.mod1.class=           # Fully qualified class name the modifier instantiates
#userInfo.modifier.mod1.prop1=           # Value for modifier MOD1 and property PROP1
#userInfo.modifier.mod1.prop2=           # Value for modifier MOD1 and property PROP2


##########################################################################################################
### REQUEST FILTERS                                                                                    ###
## filter.[filterName].[propertyName]=[val] - Template for options, see class documentation for options ##
##########################################################################################################

#filter.names=f1,f2           # Comma separated list of names for UserInfo object modifiers

# REQUEST FILTER F1
#filter.f1.class=[fully qualified class name]  # Fully qualified class name the filter instantiates
#filter.f1.subs=[val1,val2]                    # Comma separated list of user SUB attribute values. If current is in the list, filter will be skipped
#filter.f1.clientIds=[val1,val2]               # Comma separated list of client_ids. If current is in the list, filter will be skipped
#filter.f1.prop1=[val]                         # Value for filter F1 and property PROP1
#filter.f1.prop2=[val]                         # Value for filter F1 and property PROP2