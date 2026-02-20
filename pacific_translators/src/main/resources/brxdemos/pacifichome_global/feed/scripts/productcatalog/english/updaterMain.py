# This 'updater' script expects its input to have been converted and 'massaged' for various earlier requests
# (eg, change product brands)

import logging
import json
import csv

import jsonlFeedReader as jfr
import updateFeed as uf
import updaterConstants as uc
import jsonlWriter as jw

class UpdateMain ():
    def __inits__ (self):
        return

    def loadSourceFeed (self):
        srcFeedHandler = jfr.JsonlFeedReader ()
        # jsonl subset feed
        srcProducts = srcFeedHandler.readSourceFeed (uc.FILENAME_JSONL_SOURCE_FEED_EN_IN) 
        return srcProducts

    def updateSourceFeed (self, srcProducts):
        try:
            feedUpdater = uf.UpdateFeed ()
            feedUpdater.setSubsetProducts (srcProducts)
            updatedProducts, updatedAttributeList = feedUpdater.performUpdates ()
            return updatedProducts, updatedAttributeList
        except Exception as e:
            logging.error ('Error in processing source feed: %s', e)
        return None, None

    def writeJsonlFeed (self, updatedProducts):
        # full feed
        feedWriter = jw.JsonlWriter ()
        feedWriter.setProducts (updatedProducts)
        feedWriter.write (uc.FILENAME_UPDATED_JSONL_FEED_EN_OUT)
        return
 
    def writeAttributesToFile (self, attributeList):
        # sort the list
        attributeList.sort ()
        savePath = uc.FILENAME_UPDATED_FEED_ATTRIBUTELIST_EN_OUT
        with open (savePath, 'w') as file_output:
            for attrib in attributeList:
                file_output.write ('%s\n' % attrib)
            file_output.close ()

if __name__ == '__main__':
    logging.basicConfig (level=logging.INFO)

    updateDriver = UpdateMain ()

    # read feed file (jsonl)
    sourceProducts = updateDriver.loadSourceFeed ()

    # process the feed (ie, 'update')
    updatedProducts, updatedAttributeList = updateDriver.updateSourceFeed (sourceProducts)

    updateDriver.writeJsonlFeed (updatedProducts)
    updateDriver.writeAttributesToFile (updatedAttributeList)

    logging.info ("Finished ...")

'''
    # construct tabular feed
    outputProducts, attributeList = updateDriver.generateDCFeed ()
    tsvRecords = updateDriver.generateTSVFeed ()

    def generateTabulerFeed (self, updatedProducts, updatedAttributeList):
        tabularFeedBuilder = tf.TabularFeed ()
        tabularFeedBuilder.setUpdatedProducts (updatedProducts, updatedAttributeList)
        tabularRecords = tabularFeedBuilder.buildTabularFeed ()
        return tabularRecords

    def writeTabularFeed (self, tabularRecords):
        feedWriter = tfw.TabularFeedWriter ()
        feedWriter.setTabularRecords (tabularRecords)
        feedWriter.writeTSVFeed (uc.FILENAME_UPDATED_TSV_FEED_OUT)
        feedWriter.writeCSVFeed (uc.FILENAME_UPDATED_CSV_FEED_OUT)
        return

    # build tabular feed, then save as .tsv and .csv
    tabularRecords = updateDriver.generateTabulerFeed (updatedProducts, updatedAttributeList)
    # currently, tabular feed is created only for 'full' feed
    updateDriver.writeTabularFeed (tabularRecords)


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

