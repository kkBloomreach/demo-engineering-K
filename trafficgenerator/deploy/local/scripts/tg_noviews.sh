#!/bin/bash

BR_SRC=/Users/kirankundargi/br/work/src
TRAFFICGENERATOR_SRC=/Users/kirankundargi/work/brx_demos/projects/demoEngineering/trafficgenerator/demo-engineering-traffic-generator
WORK_DIR=/Users/kirankundargi/tmp/brlab/test/trafficgenerator/
LOG4J_CORE_JAR=/Users/kirankundargi/.m2/repository/org/apache/logging/log4j/log4j-core/2.17.1/log4j-core-2.17.1.jar
LOG4J_API_JAR=/Users/kirankundargi/.m2/repository/org/apache/logging/log4j/log4j-api/2.17.1/log4j-api-2.17.1.jar
JSON_JAR=/Users/kirankundargi/.m2/repository/org/json/json/20220320/json-20220320.jar

BRXDEMOS_JARS=$TRAFFICGENERATOR_SRC/target/trafficgenerator-2.3.6.2-X-SNAPSHOT.jar

# === NO CHANGES NEEDED BELOW THIS LINE ===
CP=$BRXDEMOS_JARS:$LOG4J_CORE_JAR:$LOG4J_API_JAR:$CLASSPATH:$JSON_JAR
export CLASSPATH=$CP

# '-a' == account/site name
# '-d' == data dir path
# '-l' == message level (info/debug/warn/error)
# '-r' == realm (staging | prod ) (default staging)
# '-t' == testData (true | false) (for pixel) (default true)
# '-e' == env type ('dev' | 'qa' | 'release') (default dev)
# '-p' == pixelDebug (true | false) (for pixel debug via Event manager) (default false)

# ACCOUNT
#account=pacifichome
#account=pacificsupply
#account=sandbox_product02
account=pacific_supply_mindcurv
#account=sandbox_sales19
#account=pacific_apparel
#account=sandbox_sales07
#account=sandbox_sales12
#account=pacific_fashion
#account=pacifichome
#account=sandbox_sales09
#account=demo_shopify_home
#account=pacific_homesc

# NOTE: run log created in <-d> parameter/$account/output/generatorMessages*.log
# Look for 'warn'/'error' if any, in that log file
time java com.bloomreach.trafficgenerator.Generator -d ./data -a $account -r prod -e dev -t true -p false -l debug
#date; time java com.bloomreach.trafficgenerator.Generator -d ./data -a $account -r prod -e qa -t true -p false -l debug 

# java debug
#java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:5005,server=y com.bloomreach.trafficgenerator.Generator -d ./data  -a $account -r prod -e dev -t true -p false -l debug 
#jdb -attach 127.0.0.1:5005 << goes with above java -agentilb... cmd

# === testing/debugging ===
# feed test - make sure feed filename is correct
#java com.bloomreach.trafficgenerator.test.TestProcessedFeed ./data/source/$account/feed/full_feed_preprocessed_03312021_02142023.jsonl

# feed indexer test
#java com.bloomreach.trafficgenerator.test.TestFeedIndexer 
#java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:5005,server=y com.bloomreach.trafficgenerator.test.TestFeedIndexer 

# logger test
#java com.bloomreach.trafficgenerator.test.TestLogger /tmp
#java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:5005,server=y com.bloomreach.trafficgenerator.test.TestLogger

# config test
#java com.bloomreach.trafficgenerator.test.TestSiteConfig ./data/pacific_supply_mindcurv/input/config/site.json
#java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:5005,server=y com.bloomreach.trafficgenerator.test.TestSiteConfig ./data/pacific_supply_mindcurv/input/config/site.json


# ======================
# mkdir -p $WORK_DIR/data $WORK_DIR/data/source $WORK_DIR/data/output
# mkdir -p $WORK_DIR/data/source/feed $WORK_DIR/data/source/feed/pacifichome

# The feed file must already have been translated since that is needed for pixel-log translation
# Copy that translated feed file to this WORK_DIR/data/source folder.
#cp $WORK_DIR_FEED/data/output/output.xml $WORK_DIR/data/source/translated_feed.xml

#BLOOMREACH_JARS=$BR_SRC/analytics/pipeline/target/analytics-pipeline-0.1-SNAPSHOT.jar:$BR_SRC/analytics/tools/target/bloomreach-analytics-tools-0.1-SNAPSHOT.jar:$BR_SRC/analytics/pipeline/target/lib/*

 
