#!/bin/bash

WORK_DIR="$HOME/projects/trafficgenerator"

if [ "$#" -eq 0 ]
then
    echo "Must specify account name"
    exit 1
fi

# for curated traffic, append "-c" to default command line
dash_c=""
if [ "$#" -eq 2 ]
then
	dash_c=$2
fi

# ACCOUNT
account=$1
echo "Generating traffic for $account..."
docker run --mount type=bind,src=$WORK_DIR/data,dst=/trafficgenerator/data  --name $account --rm trafficgenerator-docker-amd64-2.7.0.0 -a $account $dash_c 

echo "Generate daily stats data for $account..."
$WORK_DIR/dailystats.sh $account 1>$WORK_DIR/cronlog/dailystats_$account.txt 2>&1


