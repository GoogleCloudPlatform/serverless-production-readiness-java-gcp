while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' https://quotes-k6nu2xpn3a-ew.a.run.app/start)" != "200" ]]; do sleep .00001; done

date +"%T.%3N" && while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' https://quotes-k6nu2xpn3a-uc.a.run.app/start)" != "200" ]]; do sleep .00001; done && date +"%T.%3N"

date +"%T.%3N" && while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' https://quotes-k6nu2xpn3a-ew.a.run.app/start)" != "200" ]]; do sleep .00001; done && date +"%T.%3N"

https://quotes-4-4-k6nu2xpn3a-ew.a.run.app 
while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' https://quotes-4-4-k6nu2xpn3a-ew.a.run.app/start)" != "200" ]]; do sleep .00001; done && date +"%T.%3N"

while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' localhost:8083/start)" != "200" ]]; do sleep .00001; done && date +"%T.%3N"

