#!/bin/bash

PID=$(ps aux | grep p2p2ch | grep -v grep | awk {'print $2'})
if [ "${PID}" != "" ]; then
  kill ${PID}
fi
