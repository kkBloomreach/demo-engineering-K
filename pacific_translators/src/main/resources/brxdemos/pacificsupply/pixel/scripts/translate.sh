#!/bin/bash

BR_SRC=/Users/kirankundargi/br/work/src
BRXDEMOS_SRC=/Users/kirankundargi/work/brxdemos_tmp/pacific/dev
WORK_DIR=/Users/kirankundargi/tmp/brlab/test/pixel
WORK_DIR_FEED=/Users/kirankundargi/tmp/brlab/test/feed
WORK_DIR_API=/Users/kirankundargi/tmp/brlab/test/api

# === NO CHANGES NEEDED BELOW THIS LINE ===

# Copy source pixellogs zip and expand it in WORK dir
mkdir -p $WORK_DIR/data $WORK_DIR/data/source $WORK_DIR/data/output
cp $BRXDEMOS_SRC/src/main/resources/brxdemos/pacificsupply/translate/pixel/sourcedata/sourcepixellogs.zip $WORK_DIR/data/source
unzip $WORK_DIR/data/source/sourcepixellogs.zip -d $WORK_DIR/data/source

# The feed file must already have been translated since that is needed for pixel-log translation
# Copy that translated feed file to this WORK_DIR/data/source folder.
cp $WORK_DIR_FEED/data/output/output.tsv $WORK_DIR/data/source/translated_feed.tsv

# copy UID->ViewId map generated earlier (generated during API-translation)
cp $WORK_DIR_API/data/output/UidToViewIdMap.tsv $WORK_DIR/data/source/UidToViewIdMap.tsv


BRXDEMOS_JARS=$BRXDEMOS_SRC/target/brxdemos-1.0-SNAPSHOT.jar
BLOOMREACH_JARS=$BR_SRC/analytics/pipeline/target/analytics-pipeline-0.1-SNAPSHOT.jar:$BR_SRC/analytics/tools/target/bloomreach-analytics-tools-0.1-SNAPSHOT.jar:$BR_SRC/analytics/pipeline/target/lib/*
CP=$BRXDEMOS_JARS:$BLOOMREACH_JARS:$CLASSPATH
export CLASSPATH=$CP


java com.bloomreach.brxdemos.pacificsupply.translate.pixel.CloneAllPixelLogFiles ./data
 
