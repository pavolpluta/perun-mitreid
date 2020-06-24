# OpenID Connect identity provider based on MitreID

Takes user info from Perun system

# Building

Project is built with 'mvn clean package' command. Following parameters can be passed to modify the final build:

### General properties
- location of the configuration files (path to the containing dir) : -Dconfig.location=location
  - default: /etc/perun
- final build name: -Dfinal.name=name
  - default: oidc

### Logging configuration
Following are the options for customization of logging when building

- logging style: -Dlog.to=FILE|SYSLOG|ROLLING_FILE
  - default: FILE
- logging level: -Dlog.level=level 
  - default: info
- logging to SYSLOG
  - logging contextName (program name in syslog): -Dlog.contextName=contextName
    - default: oidc
  - logging facility: -Dlog.facility=facility
    - default: LOCAL7
- logging to file
  - file path specification: -Dlog.file.path=/var/log/oidc/perun-mitreid.log
- logging to rolling-file
  - file path specification: -Dlog.rolling-file.path=${catalina.base}/logs/${CONTEXT_NAME}.log
