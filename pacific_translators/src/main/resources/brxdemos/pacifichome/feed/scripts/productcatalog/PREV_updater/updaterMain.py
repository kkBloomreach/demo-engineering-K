# This 'updater' script expects its input to have been converted and 'massaged' for various earlier requests
# (eg, change product brands)

import logging
import json
import csv

import jsonlFeedReader as jfr
import updateFeed as uf
import subsetFeedV1 as sfv1
import subsetFeedV2 as sfv2
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

    # in subset_v1, specific products selected based on categories.
    # this changed in subset_v2
    def prepareSubsetFeed_V1 (self, updatedProducts):
        try:
            subsetGenerator = sfv1.SubsetFeedV1 ()
            subsetGenerator.setUpdatedFeed (updatedProducts)
            subsetProducts, subsetAttributeList = subsetGenerator.prepareSubset ()
            return subsetProducts, subsetAttributeList
        except Exception as e:
            logging.error ('Error in generating subset feed: %s', e)
        return None, None

    # in subset_v2, the entire pacifichome catalog is copied to the subset
    def prepareSubsetFeed_V2 (self, updatedProducts):
        try:
            subsetGenerator = sfv2.SubsetFeedV2 ()
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
    logging.basicConfig (level=logging.INFO)

    updateDriver = UpdateMain ()

    # read feed file (jsonl)
    sourceProducts = updateDriver.loadSourceFeed ()

    # process the feed (ie, 'update')
    updatedProducts, updatedAttributeList = updateDriver.updateSourceFeed (sourceProducts)

    # generate subset from the full updatedFeed
    # In "v2", entire input catalog is copied to 'subset'
    subsetProducts, subsetAttributeList = updateDriver.prepareSubsetFeed_V2 (updatedProducts)

    # build tabular feed, then save as .tsv and .csv
    tabularRecords = updateDriver.generateTabulerFeed (updatedProducts, updatedAttributeList)

    updateDriver.writeJsonlFeed (updatedProducts, subsetProducts)
    updateDriver.writeAttributesToFile (updatedAttributeList, subsetAttributeList)

    # currently, tabular feed is created only for 'full' feed
    updateDriver.writeTabularFeed (tabularRecords)

    logging.info ("Finished ...")

'''
    # construct tabular feed
    outputProducts, attributeList = updateDriver.generateDCFeed ()
    tsvRecords = updateDriver.generateTSVFeed ()

    # write dc feed and attributes
    jsonLines = updateDriver.prepareJsonLines (dcProducts)
    updateDriver.writeJsonLinesToFile (jsonLines)
    updateDriver.writeAttributesToFile (attributeList)

    # write tsvFeed
    updateDriver.writeTSVFeed (tsvRecords)
    def generateUpdatedJsonl (self, updatedProducts):
        dcFeedGenerator = dcf.DCFeed ()
        dcFeedGenerator.setBRSMFeed (brsmFeedHandler)
        dcFeed, attributeList = dcFeedGenerator.generateDCFeed ()
        return (dcFeed, attributeList)

    def prepareJsonLines (self, dcProducts):
        jsonLineList = []
        for aDCProduct in dcProducts:
            aProductJsonLine = json.dumps (aDCProduct, default=lambda o:o__dict__)
            logging.debug ('product JsonLine: %s', aProductJsonLine)
            jsonLineList.append (aProductJsonLine)

        return jsonLineList
'''

