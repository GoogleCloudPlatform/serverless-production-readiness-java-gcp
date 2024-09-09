# start this script then launch the local app with accurate.sh
# measures correctly the startup time
while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' localhost:8083/start)" != "200" ]]; do sleep .00001; done && date +"%T.%3N"