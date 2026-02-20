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
        # jsonl english subset feed
        srcContents = srcFeedHandler.readSourceFeed (uc.FILENAME_JSONL_SOURCE_FEED_EN_IN) 
        return srcContents 

    def updateSourceFeed (self, srcContents):
        try:
            feedUpdater = uf.UpdateFeed ()
            feedUpdater.setSourceContents (srcContents)
            updatedContents, updatedAttributeList = feedUpdater.performUpdates ()
            return updatedContents, updatedAttributeList
        except Exception as e:
            logging.error ('Error in processing source feed: %s', e)
        return None, None

    def writeJsonlFeed (self, updatedContents):
        # full feed
        feedWriter = jw.JsonlWriter ()
        feedWriter.setContents (updatedContents)
        feedWriter.write (uc.FILENAME_UPDATED_JSONL_FEED_EN_OUT)


if __name__ == '__main__':
    logging.basicConfig (level=logging.INFO)

    updateDriver = UpdateMain ()

    # read feed file (jsonl)
    sourceContents = updateDriver.loadSourceFeed ()

    # for en-subset, there is no 'update' operation (copy-the-subset-as-is)
    updatedContents, updatedAttributeList = updateDriver.updateSourceFeed (sourceContents)

    updateDriver.writeJsonlFeed (updatedContents)

    logging.info ("Finished ...")

'''
    def writeAttributesToFile (self, attributeList):
        # sort the list
        attributeList.sort ()
        savePath = uc.FILENAME_UPDATED_FEED_ATTRIBUTELIST_OUT
        with open (savePath, 'w') as file_output:
            for attrib in attributeList:
                file_output.write ('%s\n' % attrib)
            file_output.close ()

        return

import tabularFeed as tf
import tabularFeedWriter as tfw
    def generateTabulerFeed (self, updatedContents, updatedAttributeList):
        tabularFeedBuilder = tf.TabularFeed ()
        tabularFeedBuilder.setUpdatedContents (updatedContents, updatedAttributeList)
        tabularRecords = tabularFeedBuilder.buildTabularFeed ()
        return tabularRecords

    def writeTabularFeed (self, tabularRecords):
        feedWriter = tfw.TabularFeedWriter ()
        feedWriter.setTabularRecords (tabularRecords)
        feedWriter.writeTSVFeed (uc.FILENAME_UPDATED_TSV_FEED_EN_OUT)
        feedWriter.writeCSVFeed (uc.FILENAME_UPDATED_CSV_FEED_EN_OUT)
        return
 
    # build tabular feed, then save as .tsv and .csv
    tabularRecords = updateDriver.generateTabulerFeed (updatedContents, updatedAttributeList)

    # currently, tabular feed is created only for 'full' feed
    updateDriver.writeTabularFeed (tabularRecords)

'''

