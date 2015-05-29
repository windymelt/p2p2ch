#!/bin/bash

./bin/p2p2ch -Dconfig.file=conf/p2p2ch.conf &
sleep 5
echo "*** First request for bootstrap !!!"
curl -v http://localhost:9000/bbs/ | iconv -f CP932 -t UTF-8
