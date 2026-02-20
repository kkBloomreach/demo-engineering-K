# This 'updater' script expects its input to have been converted and 'massaged' for various earlier requests
# (eg, change product brands)

import logging
import json
import csv

import jsonlFeedReader as jfr
import updateFeed as uf
import subsetFeed as sf
import updaterConstants as uc
import tabularFeed as tf
import jsonlWriter as jw
import tabularFeedWriter as tfw

class UpdateMain ():
    def __inits__ (self):
        return

    def loadSourceFeed (self):
        srcFeedHandler = jfr.JsonlFeedReader ()
        # jsonl formated, converted from .xml and post-processed (see converter scripts)
        srcProducts = srcFeedHandler.readSourceFeed (uc.FILENAME_JSONL_SOURCE_FEED_IN) 
        return srcProducts

    def updateSourceFeed (self, srcProducts):
        try:
            feedUpdater = uf.UpdateFeed ()
            feedUpdater.setSourceProducts (srcProducts)
            updatedProducts, updatedAttributeList = feedUpdater.performUpdates ()
            return updatedProducts, updatedAttributeList
        except Exception as e:
            logging.error ('Error in processing source feed: %s', e)
        return None, None

    def prepareSubsetFeed (self, updatedProducts):
        try:
            subsetGenerator = sf.SubsetFeed ()
            subsetGenerator.setUpdatedFeed (updatedProducts)
            subsetProducts, subsetAttributeList = subsetGenerator.prepareSubset ()
            return subsetProducts, subsetAttributeList
        except Exception as e:
            logging.error ('Error in generating subset feed: %s', e)
        return None, None

    def generateTabulerFeed (self, updatedProducts, updatedAttributeList):
        tabularFeedBuilder = tf.TabularFeed ()
        tabularFeedBuilder.setUpdatedProducts (updatedProducts, updatedAttributeList)
        tabularRecords = tabularFeedBuilder.buildTabularFeed ()
        return tabularRecords

    def writeJsonlFeed (self, updatedProducts, subsetProducts):
        # full feed
        feedWriter = jw.JsonlWriter ()
        feedWriter.setProducts (updatedProducts)
        feedWriter.write (uc.FILENAME_UPDATED_JSONL_FEED_OUT)

        # subset
        feedWriter = jw.JsonlWriter ()
        feedWriter.setProducts (subsetProducts)
        feedWriter.write (uc.FILENAME_SUBSET_JSONL_FEED_OUT)
        return
 
    def writeAttributesToFile (self, attributeList, subsetAttributeList):
        # sort the list
        attributeList.sort ()
        savePath = uc.FILENAME_UPDATED_FEED_ATTRIBUTELIST_OUT
        with open (savePath, 'w') as file_output:
            for attrib in attributeList:
                file_output.write ('%s\n' % attrib)
            file_output.close ()

        # sort the list
        subsetAttributeList.sort ()
        savePath = uc.FILENAME_SUBSET_FEED_ATTRIBUTELIST_OUT
        with open (savePath, 'w') as file_output:
            for attrib in subsetAttributeList:
                file_output.write ('%s\n' % attrib)
            file_output.close ()

    def writeTabularFeed (self, tabularRecords):
        feedWriter = tfw.TabularFeedWriter ()
        feedWriter.setTabularRecords (tabularRecords)
        feedWriter.writeTSVFeed (uc.FILENAME_UPDATED_TSV_FEED_OUT)
        feedWriter.writeCSVFeed (uc.FILENAME_UPDATED_CSV_FEED_OUT)
        return
 

if __name__ == '__main__':
    logging.basicConfig (level=logging.DEBUG)

    updateDriver = UpdateMain ()

    # read feed file (jsonl)
    sourceProducts = updateDriver.loadSourceFeed ()

    # process the feed (ie, 'update')
    updatedProducts, updatedAttributeList = updateDriver.updateSourceFeed (sourceProducts)

    # generate subset from the full updatedFeed
    subsetProducts, subsetAttributeList = updateDriver.prepareSubsetFeed (updatedProducts)

    # build tabular feed, then save as .tsv and .csv
    tabularRecords = updateDriver.generateTabulerFeed (updatedProducts, updatedAttributeList)

    updateDriver.writeJsonlFeed (updatedProducts, subsetProducts)
    updateDriver.writeAttributesToFile (updatedAttributeList, subsetAttributeList)

    # currently, tabular feed is created only for 'full' feed
    updateDriver.writeTabularFeed (tabularRecords)

    logging.info ("Finished ...")





