#!/bin/bash

BR_SRC=~/br/work/src
PROJECT_SRC=~/work/brxdemos_tmp/projects/demoEngineering/analyticssimulator/demo-engineering-analytics-simulator
WORK_DIR=~/tmp/brlab/test
GENERATOR_OUTPUT_DIR=$WORK_DIR/dataout/generator
# which account to process (used below in java command line)
ACCOUNT=pacifichome
#loglevel = debug/info/warn/error
LOGLEVEL=debug

# === NO CHANGES NEEDED BELOW THIS LINE ===
echo "Logfile: $GENERATOR_OUTPUT_DIR/output/$ACCOUNT/GeneratorMessages.log"

mkdir -p $WORK_DIR/data $WORK_DIR/data/source $WORK_DIR/data/source/generator

# copy source .json, templates, pre-processed feed, ...
cp -r $PROJECT_SRC/src/main/resources/sourcedata/generator $WORK_DIR/data/source

PROJECT_JAR=$PROJECT_SRC/target/analyticssimulator-1.0-SNAPSHOT.jar
BLOOMREACH_JARS=$BR_SRC/analytics/pipeline/target/analytics-pipeline-0.1-SNAPSHOT.jar:$BR_SRC/analytics/tools/target/bloomreach-analytics-tools-0.1-SNAPSHOT.jar:$BR_SRC/analytics/pipeline/target/lib/*
CP=$PROJECT_JAR:$BLOOMREACH_JARS:$CLASSPATH
export CLASSPATH=$CP

# generate
cd $WORK_DIR; time java com.bloomreach.analyticsdatagenerator.DataGenerator -a $ACCOUNT -d ./data -o $GENERATOR_OUTPUT_DIR -l $LOGLEVEL

#### Following are for internal testing ####
#java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:5005,server=y com.bloomreach.analyticsdatagenerator.DataGenerator -d ./data -o ~/tmp/gentest -a pacificsupply
#jdb -attach 127.0.0.1:5005 << goes with above java -agentilb... cmd

 
