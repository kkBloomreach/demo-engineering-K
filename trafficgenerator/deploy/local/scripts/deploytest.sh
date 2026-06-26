#!/bin/bash

WORK_DIR=

# logger test
java com.bloomreach.trafficgenerator.test.TestLogger $HOME/tmp/brlab/test
#java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:5005,server=y com.bloomreach.trafficgenerator.test.TestLogger $HOME/tmp/brlab/test
#jdb -attach 127.0.0.1:5005 << goes with above java -agentilb... cmd

