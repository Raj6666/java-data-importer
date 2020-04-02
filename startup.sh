#!/bin/bash

# the home path
HOME_PATH=`dirname $(readlink -f $0)`
# the name of jar
JAR_NAME='data-importer.jar'
# the ips of elastic servers
CONFIG_LOCATION='conf/application-prod.properties'
# the name of pid file
PID_FILE='data-importer.pid'

if [ -f ${HOME_PATH}/${PID_FILE} ]; then
    PID=`cat ${HOME_PATH}/${PID_FILE}`
    if ps -p ${PID} > /dev/null; then
        echo 'The process exist!'
        exit;
    fi
fi

echo 'The program '${JAR_NAME}' starting...'
echo 'spring.config.location = '${CONFIG_LOCATION}
nohup java -jar ${HOME_PATH}/${JAR_NAME} --spring.config.location=${CONFIG_LOCATION} >/dev/null 2>&1 & PID=$!
echo 'PID['${PID}']'
echo ${PID} > ${HOME_PATH}/${PID_FILE}::