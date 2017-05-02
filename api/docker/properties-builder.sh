#!/bin/bash

# if we are linked, use that info
if [ "$MONGO_PORT" != "" ]; then
  # Sample: MONGO_PORT=tcp://172.17.0.20:27017
  export SPRING_DATA_MONGODB_HOST=`echo $MONGO_PORT|sed 's;.*://\([^:]*\):\(.*\);\1;'`
  export SPRING_DATA_MONGODB_PORT=`echo $MONGO_PORT|sed 's;.*://\([^:]*\):\(.*\);\2;'`
fi

echo "SPRING_DATA_MONGODB_HOST: $SPRING_DATA_MONGODB_HOST"
echo "SPRING_DATA_MONGODB_PORT: $SPRING_DATA_MONGODB_PORT"


cat > dashboard.properties <<EOF
#Database Name - default is test
dbname=${SPRING_DATA_MONGODB_DATABASE:-dashboard}

#Database HostName - default is localhost
dbhost=${SPRING_DATA_MONGODB_HOST:-10.0.1.1}

#Database Port - default is 27017
dbport=${SPRING_DATA_MONGODB_PORT:-9999}

#Database Username - default is blank
dbusername=${SPRING_DATA_MONGODB_USERNAME:-db}

#Database Password - default is blank
dbpassword=${SPRING_DATA_MONGODB_PASSWORD:-dbpass}

corsEnabled=${CORS_ENABLED:-false}

corsWhitelist=${CORS_WHITELIST:-http://domain1.com:port,http://domain2.com:port}

feature.dynamicPipeline=${FEATURE_DYNAMIC_PIPELINE:-disabled}

#Authentication Settings
auth.expirationTime=${AUTH_EXPIRATION_TIME:-}
auth.secret=${AUTH_SECRET:-}
auth.ldapServerUrl=${AUTH_LDAP_SERVER_URL:-}
auth.ldapUserDnPattern=${AUTH_LDAP_USER_DN_PATTERN:-}
auth.ldapGroupSearchBase=${AUTH_LDAP_GROUP_SEARCH_BASE:-}
auth.ldapManagerDn=${AUTH_LDAP_MANAGER_DN:-}
auth.ldapManagerPassword=${AUTH_LDAP_MANAGER_PASSWORD:-}
auth.ldapAdminGroup=${AUTH_LDAP_ADMIN_GROUP:-}

#Monitor Widget proxy credentials
monitor.proxy.username=${MONITOR_PROXY_USERNAME:-}
monitor.proxy.password=${MONITOR_PROXY_PASSWORD:-}

#Monitor Widget proxy information
monitor.proxy.type=${MONITOR_PROXY_TYPE:-http}
monitor.proxy.host=${MONITOR_PROXY_HOST:-}
monitor.proxy.port=${MONITOR_PROXY_PORT:-80}

EOF
