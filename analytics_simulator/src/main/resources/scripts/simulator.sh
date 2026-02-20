#!/bin/bash

BR_SRC=~/br/work/src
PROJECT_SRC=~/work/brxdemos_tmp/projects/demoEngineering/analyticssimulator/demo-engineering-analytics-simulator
WORK_DIR=~/tmp/brlab/test
SIMULATOR_OUTPUT_DIR=$WORK_DIR/dataout/simulator
GENERATOR_OUTPUT_DIR=$WORK_DIR/dataout/generator

# which account to process (used below in java command line)
ACCOUNT=pacifichome
#loglevel = debug/info/warn/error
LOGLEVEL=debug

# === NO CHANGES NEEDED BELOW THIS LINE ===
echo "Logfile: $SIMULATOR_OUTPUT_DIR/output/$ACCOUNT/SimulatorMessages.log"

mkdir -p $WORK_DIR/data $WORK_DIR/data/source 
mkdir -p $WORK_DIR/data/source/simulator $WORK_DIR/data/source/simulator/simdata/$ACCOUNT

# copy source .json, templates, pre-processed feed, ...
cp -r $PROJECT_SRC/src/main/resources/sourcedata/simulator $WORK_DIR/data/source

# copy .tsv files created by generator
cp $GENERATOR_OUTPUT_DIR/output/$ACCOUNT/*.tsv $WORK_DIR/data/source/simulator/simdata/$ACCOUNT

# Classpath
PROJECT_JAR=$PROJECT_SRC/target/analyticssimulator-1.0-SNAPSHOT.jar
BLOOMREACH_JARS=$BR_SRC/analytics/pipeline/target/analytics-pipeline-0.1-SNAPSHOT.jar:$BR_SRC/analytics/tools/target/bloomreach-analytics-tools-0.1-SNAPSHOT.jar:$BR_SRC/analytics/pipeline/target/lib/*
CP=$PROJECT_JAR:$BLOOMREACH_JARS:$CLASSPATH
export CLASSPATH=$CP


# '-a' == acount to process
# '-d' == dir with source files (feed, templates, ...)
# '-s' == start-day (default 0)
# '-n' == number-of-days to simulate, starting from -s value (default 31)
# '-u' == max-users-to-use-in-simulation (default ALL defined in uid.tsv)
# '-l' == message level (info/debug/warn/error) (default 'fatal')
cd $WORK_DIR; time java com.bloomreach.analyticssimulator.Simulator -a $ACCOUNT -d ./data -o $SIMULATOR_OUTPUT_DIR -l $LOGLEVEL -s 3 -n 1 -u 100 
#time java com.bloomreach.analyticssimulator.Simulator -a $ACCOUNT -d ./data -o $SIMULATOR_OUTPUT_DIR -l $LOGLEVEL


#### Following for internal tests #####
# feed test
#java com.bloomreach.analyticssimulator.test.TestProcessedFeed ./data/source/feed/pacifichome/full_feed_preprocessed_03312021_02142023.jsonl

# java debug
#java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:5005,server=y com.bloomreach.analyticssimulator.Simulator -d ./data  -n 1 -s 0 -o ~/tmp/testout -u 10 -a pacifichome -l debug
#jdb -attach 127.0.0.1:5005 << goes with above java -agentilb... cmd

 
