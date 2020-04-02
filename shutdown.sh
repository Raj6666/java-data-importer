#!/bin/bash

# the home path
HOME_PATH=`dirname $(readlink -f $0)`
# the name of jar
JAR_NAME='data-importer.jar'
# the name of pid file
PID_FILE='data-importer.pid'

if [ ! -f ${HOME_PATH}/${PID_FILE} ]; then
	echo 'PID file not found!'
else
	PID=`cat ${HOME_PATH}/${PID_FILE}`
	echo 'The program '${JAR_NAME}' with PID['${PID}'] stopping...'
	kill -9 ${PID}
	rm ${HOME_PATH}/${PID_FILE}
	echo 'finish'
fi