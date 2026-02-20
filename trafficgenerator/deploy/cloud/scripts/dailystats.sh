#!/bin/bash
# collect stats for last 24hours (find ... -mtime 0 ...)

WORK_DIR="/home/kirankundargi/projects/trafficgenerator"

# === NO CHANGES NEEDED BELOW THIS LINE ===

# check we have account/site name 
if [ "$#" -eq 0 ] 
then
    echo "Must specify account name"
    exit 1
fi

account=$1
echo "----"
echo "$account - last 24hr traffic summary"; \
echo -n "Visitors (debug mode): " ; find $WORK_DIR/data/$account/output -type f -name "g*log" -mtime 0 -exec grep "total visitors" {} \;	;\
echo -n "Predefined Sessions: " ; find $WORK_DIR/data/$account/output -type f -name "g*log" -mtime 0 -exec grep "BEFORE" {} \;	| wc -l ;\
echo -n "ERRORS: " ; find $WORK_DIR/data/$account/output -type f -name "g*log" -mtime 0 -exec grep "ERR" {} \; | wc -l ;\
echo "End traffic summary"
echo "----"

