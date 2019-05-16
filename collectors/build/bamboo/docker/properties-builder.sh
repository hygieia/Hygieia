#!/bin/bash

if [ "$SKIP_PROPERTIES_BUILDER" = true ]; then
  echo "Skipping properties builder"
  exit 0
fi
  
if [ "$MONGODB_PORT" != "" ]; then
	# Sample: MONGO_PORT=tcp://172.17.0.20:27017
	MONGODB_HOST=`echo $MONGODB_HOST|sed 's;.*://\([^:]*\):\(.*\);\1;'`
	MONGODB_PORT=`echo $MONGODB_PORT|sed 's;.*://\([^:]*\):\(.*\);\2;'`
else
	env
	echo "ERROR: MONGODB_PORT not defined"
	exit 1
fi

echo "MONGODB_HOST: $MONGODB_HOST"
echo "MONGODB_PORT: $MONGODB_PORT"


#update local host to bridge ip if used for a URL
DOCKER_LOCALHOST=
echo $JENKINS_MASTER|egrep localhost >>/dev/null
if [ $? -ne 1 ]
then
	#this seems to give a access to the VM of the docker-machine
	#LOCALHOST=`ip route|egrep '^default via'|cut -f3 -d' '`
	#see http://superuser.com/questions/144453/virtualbox-guest-os-accessing-local-server-on-host-os
	DOCKER_LOCALHOST=10.0.2.2
	MAPPED_URL=`echo "$JENKINS_MASTER"|sed "s|localhost|$DOCKER_LOCALHOST|"`
	echo "Mapping localhost -> $MAPPED_URL"
	JENKINS_MASTER=$MAPPED_URL	
fi

echo $JENKINS_OP_CENTER|egrep localhost >>/dev/null
if [ $? -ne 1 ]
then
	#this seems to give a access to the VM of the docker-machine
	#LOCALHOST=`ip route|egrep '^default via'|cut -f3 -d' '`
	#see http://superuser.com/questions/144453/virtualbox-guest-os-accessing-local-server-on-host-os
	LOCALHOST=10.0.2.2
	MAPPED_URL=`echo "$JENKINS_OP_CENTER"|sed "s|localhost|$LOCALHOST|"`
	echo "Mapping localhost -> $MAPPED_URL"
	JENKINS_OP_CENTER=$MAPPED_URL	
fi

cat > $PROP_FILE <<EOF
#Database Name - default is test
dbname=${SPRING_DATA_MONGODB_DATABASE:-dashboarddb}

#Database HostName - default is localhost
dbhost=${SPRING_DATA_MONGODB_HOST:-db}

#Database Port - default is 27017
dbport=${SPRING_DATA_MONGODB_PORT:-27017}

#Database Username - default is blank
dbusername=${SPRING_DATA_MONGODB_USERNAME:-dashboarduser}

#Database Password - default is blank
dbpassword=${SPRING_DATA_MONGODB_PASSWORD:-dbpassword}

#This is ensure if you are keeping DB outside docker compose.
dbhostport=${SPRING_DATA_MONGODB_HOST}:${SPRING_DATA_MONGODB_PORT}

#Collector schedule (required)
bamboo.cron=${JENKINS_CRON:-0 0/5 * * * *}

#Jenkins server (required) - Can provide multiple
bamboo.servers[0]=${JENKINS_MASTER:-http://jenkins.company.com}

#Another option: If using same username/password Jenkins auth - set username/apiKey to use HTTP Basic Auth (blank=no auth)
bamboo.username=${JENKINS_USERNAME}
bamboo.apiKey=${JENKINS_API_KEY}

#Determines if build console log is collected - defaults to false
bamboo.saveLog=${JENKINS_SAVE_LOG:-false}

#Specifies the upper limit of plans to fetch. For really large orgs, this may be adjusted to the tens of thousands.
bamboo.maxPlans=${JENKINS_MAX_PLANS:-2000}

#map the entry localhost so URLS in jenkins resolve properly
# Docker NATs the real host localhost to 10.0.2.2 when running in docker
# as localhost is stored in the JSON payload from jenkins we need
# this hack to fix the addresses
bamboo.dockerLocalHostIP=${DOCKER_LOCALHOST}

EOF

if ( "$JENKINS_OP_CENTER" != "" )
then

	cat >> $PROP_FILE <<EOF
#If using username/token for api authentication (required for Cloudbees Jenkins Ops Center) see sample
#jenkins.servers[1]=${JENKINS_OP_CENTER:-http://username:token@jenkins.company.com}
bamboo.servers[1]=${JENKINS_OP_CENTER}
EOF

fi


echo "

===========================================
Properties file created `date`:  $PROP_FILE
Note: passwords & apiKey hidden
===========================================
`cat $PROP_FILE |egrep -vi 'password|apiKey'`
"

exit 0
