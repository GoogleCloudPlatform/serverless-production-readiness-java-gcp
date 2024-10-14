#!/bin/bash

CRAC_FILES_DIR=`eval echo ${CRAC_FILES_DIR}`
mkdir -p $CRAC_FILES_DIR

if [ -z "$(ls -A $CRAC_FILES_DIR)" ]; then
  if [ "$FLAG" = "-r" ]; then
    java -Dspring.context.checkpoint=onRefresh -Dmanagement.endpoint.health.probes.add-additional-paths="true" -Dmanagement.health.probes.enabled="true" -XX:CRaCMinPid=128 -XX:+ShowCPUFeatures -XX:CPUFeatures=generic -XX:CRaCCheckpointTo=$CRAC_FILES_DIR -jar /opt/app/quotes-crac-1.0.0.jar
  else
    java -Dmanagement.endpoint.health.probes.add-additional-paths="true" -Dmanagement.health.probes.enabled="true" -XX:CRaCMinPid=128 -XX:+ShowCPUFeatures -XX:CPUFeatures=generic -XX:CRaCCheckpointTo=$CRAC_FILES_DIR -jar /opt/app/quotes-crac-1.0.0.jar&
    sleep 5
    jcmd /opt/app/quotes-crac-1.0.0.jar JDK.checkpoint
  fi
  sleep infinity
else
  java -Dmanagement.endpoint.health.probes.add-additional-paths="true" -Dmanagement.health.probes.enabled="true" -XX:CRaCRestoreFrom=$CRAC_FILES_DIR&
  PID=$!
  trap "kill $PID" SIGINT SIGTERM
  wait $PID
fi