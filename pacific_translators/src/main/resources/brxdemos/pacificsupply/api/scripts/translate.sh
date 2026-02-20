#!/bin/bash

BR_SRC=/Users/kirankundargi/br/work/src
BRXDEMOS_SRC=/Users/kirankundargi/work/brxdemos_tmp/pacific/dev
WORK_DIR=/Users/kirankundargi/tmp/brlab/test/api
WORK_DIR_FEED=/Users/kirankundargi/tmp/brlab/test/feed

# === NO CHANGES NEEDED BELOW THIS LINE ===

# Copy source apilogs zip and expand it in WORK dir
mkdir -p $WORK_DIR/data $WORK_DIR/data/source $WORK_DIR/data/output
cp $BRXDEMOS_SRC/src/main/resources/brxdemos/pacificsupply/translate/api/sourcedata/sourceapilogs.zip $WORK_DIR/data/source
unzip $WORK_DIR/data/source/sourceapilogs.zip -d $WORK_DIR/data/source

# The feed file must already have been translated since that is needed for api-log translation
# Copy that translated feed file to this WORK_DIR/data/source folder.
cp $WORK_DIR_FEED/data/output/output.tsv $WORK_DIR/data/source/translated_feed.tsv


BRXDEMOS_JARS=$BRXDEMOS_SRC/target/brxdemos-1.0-SNAPSHOT.jar
BLOOMREACH_JARS=$BR_SRC/analytics/pipeline/target/analytics-pipeline-0.1-SNAPSHOT.jar:$BR_SRC/analytics/tools/target/bloomreach-analytics-tools-0.1-SNAPSHOT.jar:$BR_SRC/analytics/pipeline/target/lib/*
CP=$BRXDEMOS_JARS:$BLOOMREACH_JARS:$CLASSPATH
export CLASSPATH=$CP


java com.bloomreach.brxdemos.pacificsupply.translate.api.CloneAllApiLogFiles ./data
 
