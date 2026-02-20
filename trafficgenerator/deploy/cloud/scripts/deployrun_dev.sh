#!/bin/bash

WORK_DIR="/home/kirankundargi/projects/trafficgenerator"

# === NO CHANGES NEEDED BELOW THIS LINE ===

# check we have account/site name 
if [ "$#" -eq 0 ] 
then
    echo "Must specify account name"
    exit 1
fi

# ACCOUNT
account=$1
echo "Generating traffic for $account..."

CLSPATH=$WORK_DIR/lib/trafficgenerator-dev.jar
CLSPATH=$CLSPATH:$WORK_DIR/lib/log4j-core-2.17.1.jar
CLSPATH=$CLSPATH:$WORK_DIR/lib/log4j-api-2.17.1.jar
CLSPATH=$CLSPATH:$WORK_DIR/lib/json-20220320.jar
export CLASSPATH=$CLSPATH
echo $CLASSPATH

# '-a' == account/site name
# '-d' == data dir path
# '-l' == message level (info/debug/warn/error)
# '-r' == realm (staging | prod ) (default staging)
# '-t' == testData (true | false) (for pixel) (default true)
# '-e' == env type ('dev' | 'qa' | 'release') (default dev)
# '-p' == pixelDebug (true | false) (for pixel debug via Event manager) (default false)

# java com.bloomreach.trafficgenerator.Generator -d $WORK_DIR/data -a $account -l debug -r prod -t true -e dev -p false
java com.bloomreach.trafficgenerator.Generator -d $WORK_DIR/data -a $account -r prod -e dev -t true -p false -l debug;\
$WORK_DIR/dailystats.sh $account 1>$WORK_DIR/cronlog/dailystats_$account.txt 2>&1

# java debug
#java -agentlib:jdwp=transport=dt_socket,address=127.0.0.1:5005,server=y com.bloomreach.trafficgenerator.Generator -d ./data  -a $account -l debug -r staging -t true -e dev -p true
#jdb -attach 127.0.0.1:5005 << goes with above java -agentilb... cmd

 
