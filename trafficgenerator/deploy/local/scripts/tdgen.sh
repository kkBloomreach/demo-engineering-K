#!/bin/bash

TRAFFICGENERATOR_SRC=$HOME/work/brx_demos/projects/demoEngineering/trafficgenerator/demo-engineering-traffic-generator
WORK_DIR=$HOME/tmp/brlab/test/trafficgenerator/
LOG4J_CORE_JAR=$HOME/.m2/repository/org/apache/logging/log4j/log4j-core/2.17.1/log4j-core-2.17.1.jar
LOG4J_API_JAR=$HOME/.m2/repository/org/apache/logging/log4j/log4j-api/2.17.1/log4j-api-2.17.1.jar
JSON_JAR=$HOME/.m2/repository/org/json/json/20220320/json-20220320.jar

# === NO CHANGES NEEDED BELOW THIS LINE ===

BRXDEMOS_JARS=$TRAFFICGENERATOR_SRC/target/trafficgenerator-1.0-SNAPSHOT.jar
CP=$BRXDEMOS_JARS:$LOG4J_CORE_JAR:$LOG4J_API_JAR:$CLASSPATH:$JSON_JAR
export CLASSPATH=$CP

# '-a' == account/site name
# '-d' == data dir path
# '-l' == message level (info/debug/warn/error)

# ACCOUNT
#account=pacifichome
#account=pacificsupply
#account=sandbox_product02
#account=pacific_supply_mindcurv
#account=sandbox_sales19
account=pacific_apparel

# NOTE: run log created in <-d> parameter/$account/output/generatorMessages*.log
# Look for 'warn'/'error' if any, in that log file
java com.bloomreach.trafficgenerator.DataGenerator -d ./data -a $account -l debug

# java debug
#java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:5005,server=y com.bloomreach.trafficgenerator.DataGenerator -d ./data  -a $account -l debug 
#jdb -attach 127.0.0.1:5005 << goes with above java -agentilb... cmd

 
