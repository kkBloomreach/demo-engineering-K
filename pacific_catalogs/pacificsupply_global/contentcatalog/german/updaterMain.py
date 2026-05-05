import logging
import json
import csv
import os
import subprocess 

import jsonlFeedReader as jfr
import updateFeed as uf
import updaterConstants as uc
import jsonlWriter as jw

class UpdateMain ():
    def __inits__ (self):
        return

    def loadCurrentCatalog (self, jsonlPath):
        catalogReader = jfr.JsonlFeedReader ()
        currentCatalog = catalogReader.readSourceFeed (jsonlPath)
        return currentCatalog

    # use Experience CMS document API
    def getSiteApiResponse (self, apiEndpoint):
        FILENAME_APIRESPONSE = './data/apiresponse_tmp.json' # used only within this method
        documentsApiResp = None

        callstat = subprocess.call ([ "curl", "-o", FILENAME_APIRESPONSE, apiEndpoint])
        if os.path.exists (FILENAME_APIRESPONSE):
            with open (FILENAME_APIRESPONSE, 'r') as f:
                documentsApiResp = json.loads (f.read ())
                f.close ()

            # TEMP commented out
            # os.remove (FILENAME_APIRESPONSE)

        if documentsApiResp == None:
            logging.error ('cannot get contents from site: %s', apiEndpoint)

        return documentsApiResp

    def updateDiscoveryCatalog (self, discoveryCatalog, apiResponse):
        try:
            catalogUpdater = uf.UpdateFeed ()
            catalogUpdater.setDiscoveryCatalog (discoveryCatalog)
            catalogUpdater.setSiteApiResponse (apiResponse)
            updatedCatalog = catalogUpdater.performUpdates ()
            return updatedCatalog
        except Exception as e:
            logging.error ('Catalog update exception %s: ', e)
        return None

    def writeJsonlFeed (self, updatedContents):
        # full feed
        feedWriter = jw.JsonlWriter ()
        feedWriter.setContents (updatedContents)
        feedWriter.write (uc.FILENAME_UPDATED_JSONL_FEED_DE_OUT)
        return

if __name__ == '__main__':
    logging.basicConfig (level=logging.DEBUG)

    updateDriver = UpdateMain ()

    # current content catalog indexed in Discovery
    currentDiscoveryCatalog = updateDriver.loadCurrentCatalog (uc.FILENAME_JSONL_SOURCE_FEED_IN)

    # current contents in CMS (Experience)
    apiResponse = updateDriver.getSiteApiResponse (uc.SITE_CONTENT_DOCUMENTS_API_ENDPOINT) 

    # process the feed (ie, 'update')
    updatedContents = updateDriver.updateDiscoveryCatalog (currentDiscoveryCatalog, apiResponse)

    # save to jsonl
    if (updatedContents != None) and (len (updatedContents) > 0):
        updateDriver.writeJsonlFeed (updatedContents)

    logging.info ("Finished ...")


'''
import tabularFeed as tf
import tabularFeedWriter as tfw
    def generateTabulerFeed_UNUSED (self, updatedContents):
        tabularFeedBuilder = tf.TabularFeed ()
        tabularFeedBuilder.setUpdatedContents (updatedContents)
        tabularRecords = tabularFeedBuilder.buildTabularFeed ()
        return tabularRecords

    def writeTabularFeed (self, tabularRecords):
        feedWriter = tfw.TabularFeedWriter ()
        feedWriter.setTabularRecords (tabularRecords)
        feedWriter.writeTSVFeed (uc.FILENAME_UPDATED_TSV_FEED_DE_OUT)
        feedWriter.writeCSVFeed (uc.FILENAME_UPDATED_CSV_FEED_DE_OUT)
        return
 
    # build tabular feed, then save as .tsv and .csv
    # tabular feed not generated for Content feed
    # tabularRecords = updateDriver.generateTabulerFeed (updatedContents)

    # currently, tabular feed is created only for 'full' feed
    # tabular feed not generated for Content feed
    # updateDriver.writeTabularFeed (tabularRecords)

'''



