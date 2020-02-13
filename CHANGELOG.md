# Change Log
All notable changes to this project will be documented in this file.

## [Unreleased]
### Added
### Changed
- Changed property names for specifying custom claims (see configuration template for required format)
- UserInfo modifiers are now loaded only at the startup, previously were loaded for each modification separately
### Fixed
- Fixed possible null pointer exceptions and wrong behavior for FilterEduPersonEntitlement UserInfo modifier

## [v1.24.0]
### Added
- Added option to read classes for HTML pages from file
- Implemented request filter - isCesnetEligible (see class documentation for more info)
- Created an unapproved page that displays passed texts
### Changed
- changed the implementation of GA4GHClaimSource because the EGA Permissions service
  changed its API to be GA4GH-compatible
- Changed logging configuration - log all classes at the level specified while building
- Consent uses the loading of HTML classes from the file for some elements
- Updated templates for consent page
- Moved logs of user logins into the statistics filter, removed userID from it
- Changed handling of user logins
- Removed ProxyIDP header from unapproved page
- Modified ProxyStatisticsFilter to respond to new version of SimpleSAMLphp perun statistics
- Changed loading of JS files due to content-security policy
- Changed database table [acrs](https://github.com/CESNET/perun-mitreid/blob/release_1_24/oidc-idp/src/main/webapp/WEB-INF/classes/db/mysql/acrs.sql): `ALTER TABLE  acrs MODIFY COLUMN expiration BIGINT;`
### Fixed
- Fixed handling of group "members" in resource capabilities
- Fixed wrong behavior of MFA forced logout
- Fixed wrong loading of SAML logout URL when specified in properties
- Fixed wrong handling of date and time because of ignoring time zone (ACR expiration)
- Fixed bad rendering of registration pages (registration into groups when authorization has failed) - double service name in heading
- Fixed validation of custom registration URL in PerunAuthorizationFilter

## [v1.23.0]
### Changed
- Updated MUNI template
- Updated sample configuration template file
- Updated proxy.\* properties to full paths
- Refactored valuseAs...() methods in PerunAttribute to return empty collections rather than null objects
### Fixed
- Fixed another bug in resource capabilities - wrong prefixing - configuration changes needed!
### Added
- Added property to specify SAML URL - used as path to CSS files from Proxy

## [v1.22.2]
### Fixed
- Fixed bug in remember consent functionality - view did not send the parameter

## [v1.22.1]
### Fixed
- Fixed bug in recourceCapabilities - thrown NullPointerException when obtained attribute has been null

## [v1.22.0]
### Changed
- Localization files are now read as UTF-8 (no escaping of accented chars needed in files)
### Fixed
- Bugfixes in PerunAuthenticationFilter and PerunAUPFilter
- Fixes in frontend
### Added
- Added detailed statistics
- Added resource capabilities support
  - !! CLASS JoinGroupNamesAndEduPersonEntitlementSource.java has been renamed to EntitlementSource.java !!
  - scope, which uses above-mentioned source, requires new configuration - custom.claim.[claimName].capabilities

## [v1.21.0]
### Changed
- Updated implementation of the GA4GH claim
- Refactored calling of custom security filters
- Refactored templating in custom JSPs
### Fixed
- Fixed error in db_updated script for MySQL
- Fixed bug in the language bar on custom pages - bug caused generating addresses with weirdly formatted parameter
### Added
- Added PerunUserInfo modifiers - provides capabilities to modify UserInfo before it is released to the end service
- Added AUP security filter - will enforce user to agree with AUP
- Added template for Masaryk University
- Added configuration files templates

## [v1.20.0]
### Changed
- Refactored GA4GH claim source
- Refactored localization for custom pages
### Fixed
- Fixed exception in SQL for storing too long request parameter - need an update of the DB schemas!
- Fixed logout confirmation - clicking NO will redirect user back to the service
### Added
- Added custom template for EuroPDX
- Added option to specify claims included in id_token
- Added CODEOWNERS
- Implemented passing ACR from Shibboleth to OIDC
- Added support for processing ACR_VALUES param in authorization requests
- Added logging of user logins

## [v1.19.0]
### Changed
- GA4GH affiliations expiration is now based on lastAccess
### Fixed
- Fixed path to the CSS files in header.tag
### Added
- Added ClaimSources ExtractValuesByDomainSource and JoinGroupsAndEduPersonEntitlement

## [v1.18.0]
### Changed
- Replaced PerunUserInfoRepository with PerunUserInfoService, which can produce different UserInfo for the same user but different clients
- Removed calling of old ELIXIR Permissions API
- Renamed DatasetPermissionsAccessTokenModifier to ElixirAccessTokenModifier
### Added
- Added HTTP connection pooling to calling Perun RPC
- Added ClaimSource interface for classes that can produce arbitrary values for claims
- Added PerunAttributeClaimSource that gets claim value from selected user's attribute loaded from Perun
- Added GA4GHClaimSource producing the "ga4gh" claim, it calls the new ELIXIR Permissions API
- Added all claims to ID tokens (claims are now available in ID token, /userinfo and /introspection endpoints)
- Added SAML logout - after the end of OIDC session and before redirecting to a client-specified URL, SAML logout is done

## [v1.17.0]
### Changed
- Changed dependency on a fixed version of MITREid declaring release of refresh_token
- Refactored project structure
- Rebased on MITREid 1.3.3
- Updated consent page
### Fixed
- Minor bugfixes
### Added
- Added new access control filters
- Implemented forwarding Identity Provider filters

## [v1.16.1]
### Changed
- Changed dependency on MITREid version with declared release of refresh_token

## [v1.16.0]
### Changed
- Updated mapping of resources in consent template
- Updated proxyStatistics filter
- Updated project README
### Added
- Added support for build parameters

## [v1.15.1]
### Changed
- Scopes and claims at the consent page are rendered in sorted manner
### Fixed
- Swapped middle_name and family_name claims
### Added
- Added texts for consent page for scopes permissions_rems and eduPersonEntitlement

## [v1.15.0]
### Changed
- PerunConnector methods return enities in format of models
- enabled allowCompleteDeviceCodeUri property
### Fixed
- Fixed getAttributeValue to not return "null" as a String value
### Added
- Added configurable names for attributes with IdP name and id to statistics filter
- Added model items for entities
- Implemented LDAP connector
- Added REMS dataset permissions

## [v1.14.0]
### Changed
- Proxystatistics filter stores identifier of SP/IdP as addition to the name of SP/IdP
- Storing name of the SP/IdP into mapping table
### Fixed
- Fixed problem with getting attributeValue with UTF-8 characters from the request

## [v1.13.0]
### Changed
- Logging using syslog
### Fixed
- Fixed problem, when more attributes of user have been read from one source, value has been modified by CustomClaimModifier, change has propagated ino both attributes
### Added
- Added option to store statistics about each log in

## [v1.12.0]
### Added
- Added framework for claim value modifiers
- Added claim value modifier GroupNamesAARCFormatModifier for converting claim "groupNames" into AARC format
- Added claim value modifier AppendModifier for appending domain to claim "sub"
- Added claim value modifier RegexReplaceModifier just in case it might be useful for simple conversions

## [v1.11.0]
First release of modified MITREid server which uses Shibboleth for authentication and reads user data from Perun system. It also has support for new scopes and claims, for extension of released access tokens, and for releasing user claims from introspection endpoint.

[Unreleased]: https://github.com/CESNET/perun-mitreid/tree/master
[v1.24.0]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.24.0
[v1.23.0]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.23.0
[v1.22.2]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.22.2
[v1.22.1]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.22.1
[v1.22.0]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.22.0
[v1.21.0]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.21.0
[v1.20.0]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.20.0
[v1.19.0]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.19.0
[v1.18.0]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.18.0
[v1.17.0]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.17.0
[v1.16.1]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.16.1
[v1.16.0]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.16.0
[v1.15.1]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.15.1
[v1.15.0]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.15.0
[v1.14.0]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.14.0
[v1.13.0]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.13.0
[v1.12.0]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.12.0
[v1.11.0]: https://github.com/CESNET/perun-mitreid/releases/tag/v1.11.0
