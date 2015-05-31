#!/bin/bash

curl -v -X POST                                                             \
        -H 'Content-Type: application/x-www-form-urlencoded; charset=utf-8' \
	--data-urlencode "bbs=test"				            \
	--data-urlencode "time="					    \
	--data-urlencode "submit="					    \
	--data-urlencode "FROM="					    \
	--data-urlencode "mail="					    \
	--data-urlencode "MESSAGE=test"		                            \
	--data-urlencode "subject=test"					    \
	http://localhost:9000/test/bbs.cgi | iconv -f CP932 -t UTF-8
