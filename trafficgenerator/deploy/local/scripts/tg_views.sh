#!/bin/bash

BR_SRC=/Users/kirankundargi/br/work/src
TRAFFICGENERATOR_SRC=/Users/kirankundargi/work/brxdemos_tmp/projects/demoEngineering/trafficgenerator/demo-engineering-traffic-generator
WORK_DIR=/Users/kirankundargi/tmp/brlab/test/trafficgenerator/
LOG4J_CORE_JAR=/Users/kirankundargi/.m2/repository/org/apache/logging/log4j/log4j-core/2.17.1/log4j-core-2.17.1.jar
LOG4J_API_JAR=/Users/kirankundargi/.m2/repository/org/apache/logging/log4j/log4j-api/2.17.1/log4j-api-2.17.1.jar
JSON_JAR=/Users/kirankundargi/.m2/repository/org/json/json/20220320/json-20220320.jar

# === NO CHANGES NEEDED BELOW THIS LINE ===

# mkdir -p $WORK_DIR/data $WORK_DIR/data/source $WORK_DIR/data/output
# mkdir -p $WORK_DIR/data/source/feed $WORK_DIR/data/source/feed/pacifichome

# The feed file must already have been translated since that is needed for pixel-log translation
# Copy that translated feed file to this WORK_DIR/data/source folder.
#cp $WORK_DIR_FEED/data/output/output.xml $WORK_DIR/data/source/translated_feed.xml

# cp $ANALYTICSSIMULATOR_SRC/src/main/resources/brxdemos/pacifichome/simulate/pixel/sourcedata/SimulatedUidToSegmentMap.tsv $WORK_DIR/data/source

# copy manually curated segment -> pid map. It contains a 'map' of pids that "should" be in specific segment
# cp $ANALYTICSSIMULATOR_SRC/src/main/resources/brxdemos/pacifichome/simulate/pixel/sourcedata/*.tsv $WORK_DIR/data/source

# copy templates for pixelLogs (search, product, atc, conversion). 
# cp $ANALYTICSSIMULATOR_SRC/src/main/resources/brxdemos/pacifichome/simulate/pixel/sourcedata/simtemplate*.txt $WORK_DIR/data/source


BRXDEMOS_JARS=$TRAFFICGENERATOR_SRC/target/trafficgenerator-1.0-SNAPSHOT.jar
#BLOOMREACH_JARS=$BR_SRC/analytics/pipeline/target/analytics-pipeline-0.1-SNAPSHOT.jar:$BR_SRC/analytics/tools/target/bloomreach-analytics-tools-0.1-SNAPSHOT.jar:$BR_SRC/analytics/pipeline/target/lib/*
CP=$BRXDEMOS_JARS:$LOG4J_CORE_JAR:$LOG4J_API_JAR:$CLASSPATH:$JSON_JAR
export CLASSPATH=$CP

# '-a' == account/site name
# '-d' == data dir path
# '-l' == message level (info/debug/warn/error)
# '-r' == realm (staging | prod ) (default staging)
# '-t' == testData (true | false) (for pixel) (default true)
# '-e' == env type ('dev' | 'qa' | 'release') (default dev)

# ACCOUNT
#account=pacifichome
#account=pacificsupply
#account=sandbox_product02
#account=pacific_supply_mindcurv
account=pacific_viewtest

# NOTE: simulation run log created in <-o> parameter/output/$account/*.log
# Look for 'warn'/'error' if any, in that log file
#time java com.bloomreach.analyticssimulator.Simulator -d ./data -s 0 -n 1 -u 100 -a $account -o ~/tmp/simtest -l debug
#time java com.bloomreach.analyticssimulator.Simulator -d ./data -a $account -o ~/tmp/simtest -l warn
#time java com.bloomreach.analyticssimulator.Simulator -d ./data -a $account -o ~/tmp/simtest -l debug -s 0 -n 23 
#### -t false => pixel test_data = false (ie, live pixel)
#java com.bloomreach.trafficgenerator.Generator -d ./data -a $account -l debug -t true -r staging -e dev

# java debug
java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:5005,server=y com.bloomreach.trafficgenerator.Generator -d ./data  -a $account -l debug -r staging -t true -e dev

#jdb -attach 127.0.0.1:5005 << goes with above java -agentilb... cmd

# === testing/debugging ===
# feed test - make sure feed filename is correct
#java com.bloomreach.trafficgenerator.test.TestProcessedFeed ./data/source/$account/feed/full_feed_preprocessed_03312021_02142023.jsonl
# feed indexer test
#java com.bloomreach.trafficgenerator.test.TestFeedIndexer 
#java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:5005,server=y com.bloomreach.trafficgenerator.test.TestFeedIndexer 

#java com.bloomreach.trafficgenerator.test.TestLogger
#java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:5005,server=y com.bloomreach.trafficgenerator.test.TestLogger

 
