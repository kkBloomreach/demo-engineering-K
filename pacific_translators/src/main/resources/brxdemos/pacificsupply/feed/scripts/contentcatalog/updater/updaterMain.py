import logging
import jsonlines
import os
import subprocess
import json

import sitereader as sr
import updaterConstants as uc
import jsonlWriter as jw
import jsonlReader as jr
import updateFeed as uf
import subsetFeed as sf

class UpdateMain ():
    def __init__ (self):
        return

    def loadCurrentCatalog (self, jsonlPath):
        catalogReader = jr.JsonlReader ()
        currentCatalog = catalogReader.readSource (jsonlPath)
        return currentCatalog

    # use Experience CMS document API
    def getSiteApiResponse (self, apiEndpoint):
        FILENAME_APIRESPONSE = 'apiresponse_tmp.json' # used only within this method
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

    def prepareSubsetCatalog (self, updatedDiscoveryCatalog):
        try:
            subsetGenerator = sf.SubsetFeed ()
            subsetGenerator.setUpdatedFeed (updatedDiscoveryCatalog)
            subsetCatalog = subsetGenerator.prepareSubset ()
            return subsetCatalog
        except Exception as e:
            logging.error ('Error in generating subset catalog: %s', e)

        return None

    def writeJsonlCatalog (self, updatedDiscoveryCatalog, subsetDiscoveryCatalog):
        catalogWriter = jw.JsonlWriter ()
        catalogWriter.setRecords (updatedDiscoveryCatalog)
        catalogWriter.write (uc.FILENAME_UPDATED_JSONL_FEED_OUT)

        catalogWriter = jw.JsonlWriter ()
        catalogWriter.setRecords (subsetDiscoveryCatalog)
        catalogWriter.write (uc.FILENAME_SUBSET_JSONL_FEED_OUT)

        return

if __name__ == '__main__':
    logging.basicConfig (level = logging.WARN)
    updateDriver = UpdateMain ()

    # current content catalog indexed in Discovery
    currentDiscoveryCatalog = updateDriver.loadCurrentCatalog (uc.FILENAME_JSONL_SOURCE_FEED_IN)

    # current contents in CMS (Experience) 
    apiResponse = updateDriver.getSiteApiResponse (uc.SITE_CONTENT_DOCUMENTS_API_ENDPOINT)

    # update current catalog using site apiResponse 
    updatedDiscoveryCatalog = updateDriver.updateDiscoveryCatalog (currentDiscoveryCatalog, apiResponse)

    # generate subset from full updatedCatalog
    subsetDiscoveryCatalog = updateDriver.prepareSubsetCatalog (updatedDiscoveryCatalog)

    # write both full and subset content catalogs (both are english)
    updateDriver.writeJsonlCatalog (updatedDiscoveryCatalog, subsetDiscoveryCatalog)

    # content catalog in tabular format is not needed for pacifichome
    # it is needed for *_global accounts to populate those Experience CMS's
    # (See README in those folders)

    logging.info ('Finish...')

