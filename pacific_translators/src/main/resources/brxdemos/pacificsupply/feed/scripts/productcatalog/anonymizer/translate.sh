#!/bin/bash

BRXDEMOS_SRC=/Users/kirankundargi/work/brxdemos_tmp/pacific/dev
WORK_DIR=/Users/kirankundargi/tmp/brlab/test/feed

# === NO CHANGES NEEDED BELOW THIS LINE ===

# Copy source feed zip file 
mkdir -p $WORK_DIR/data
mkdir -p $WORK_DIR/data/source
mkdir -p $WORK_DIR/data/output
cp $BRXDEMOS_SRC/src/main/resources/brxdemos/pacificsupply/translate/feed/sourcedata/source_feed.zip $WORK_DIR/data/source
unzip $WORK_DIR/data/source/source_feed.zip -d $WORK_DIR/data/source
cp $BRXDEMOS_SRC/src/main/resources/brxdemos/pacificsupply/translate/feed/sourcedata/cat_transformation.tsv $WORK_DIR/data/source

# copy python scripts
cp $BRXDEMOS_SRC/src/main/resources/brxdemos/pacificsupply/translate/feed/scripts/*.py $WORK_DIR

# run python script. Generated output is in the 'data' folder itself
cd $WORK_DIR

# command line arg does not have trailing "/"
python3 anonymize.py -d ./data

