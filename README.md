# OpenID Connect identity provider based on MitreID

Takes user info from Perun system

# Building

Project is built with 'mvn clean package' command. Following parameters can be passed to modify the final build:

- location of the conguration file: -Dconfig.location=location [default: /etc/perun/perun-mitreid.properties]
- logging style: -Dlog.to=FILE|SYSLOG [default: FILE]
- logging contextName (program name in syslog): -Dlog.contextName=contextName [default: oidc]
- logging facility (syslog): -Dlog.facility=facility [default: LOCAL7]
- logging level: -Dlog.level=level [default: info]
- final build name: -Dfinal.name=name [default: oidc]
