# Use CMS api calls to collect content-files already deployed in CMS.
# Then build discovery catalog using those records

import logging
import json
import csv
import importlib
import os
import subprocess

import jsonlFeedReader as jfr
import jsonlWriter as jw
import tabularFeedReader as tfr
import tabularFeedBuilder as tfb
import tabularFeedWriter as tfw
import updaterConstants as uc

class UpdateMain ():
    def __init__ (self):
        return

    # actual feed updater object is instantiated for 'current-revision-updater'
    def instantiate_revision_updater (self):
        feedUpdater = None
        # first load the revision_update_class and instantiate it
        try:
            revision_update_module = importlib.import_module (uc.REVISION_UPDATER_MODULE)
            try:
                update_task_class = getattr (revision_update_module, uc.REVISION_CLASS_NAME)
                feedUpdater = update_task_class ()  # instantiate
            except AttributeError:
                logging.error ('Class does not exist: %s', uc.REVISION_CLASS_NAME)
                return None, None
        except ImportError:
            logging.error ('Module does not exist: %s', uc.REVISION_UPDATER_MODULE)
            return None, None
        return feedUpdater

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

    # previously created inject_av_map and then potentially manually edited
    def read_inject_av_map (self):
        injected_av_map = []

        if (os.path.exists (uc.FILENAME_INJECTED_AV_MAP_IN) == True):
            with open (uc.FILENAME_INJECTED_AV_MAP_IN, 'r') as injected_av_map_file:
                dict_reader = csv.DictReader (injected_av_map_file, delimiter='\t')
                for row in dict_reader:
                    injected_av_map.append (row)
                injected_av_map_file.close ()
        return injected_av_map

    # perform revision-specific-updated using revision-specific updater
    def performUpdates (self, revision_updater, api_response, inject_av_map):
        try:
            revision_updater.set_site_api_response (api_response)
            revision_updater.set_inject_av_map (inject_av_map)
            updated_products, updated_attribute_list = revision_updater.perform_updates ()
            return updated_products, updated_attribute_list
        except Exception as e:
            logging.error ('Error in processing source feed: %s', e)
        return None, None

    def prepare_injected_av_map (self, discovery_records):
        injected_av_map = []
        for record in discovery_records:
            injected_av_record = {}

            for av_map_attrib in uc.INJECTED_AVMAP_ATTRIBUTES:
                record_attribs = record ['value']['attributes']
                if av_map_attrib in record_attribs:
                    if (av_map_attrib == 'author'):
                        injected_av_record [av_map_attrib] = record_attribs ['author']
                    elif (av_map_attrib == 'availability'):
                        injected_av_record [av_map_attrib] = record_attribs ['availability']
                    elif (av_map_attrib == 'url'):
                        injected_av_record [av_map_attrib] = record_attribs ['url']
                else:
                    logging.warning ('Attribute for injected_av_map does not exist: %s' % av_map_attrib)
                    continue

            # Following can be edited manually
            injected_av_record ['edited_availability'] = injected_av_record ['availability']
            injected_av_record ['edited_author'] = injected_av_record ['author']
            injected_av_map.append (injected_av_record)

        return injected_av_map

    def generateTabulerFeed (self, discovery_records, updatedAttributeList):
        tabularFeedBuilder = tfb.TabularFeedBuilder ()
        tabularFeedBuilder.setUpdatedRecords (discovery_records, updatedAttributeList)
        tabularRecords = tabularFeedBuilder.buildTabularFeed ()
        return tabularRecords

    def writeJsonlFeed (self, discovery_records):
        # full feed
        feedWriter = jw.JsonlWriter ()
        feedWriter.setRecords (discovery_records)
        feedWriter.write (uc.FILENAME_UPDATED_JSONL_FEED_OUT)
        return
 
    def writeAttributesToFile (self, attributeList):
        # sort the list
        attributeList.sort ()
        savePath = uc.FILENAME_UPDATED_FEED_ATTRIBUTELIST_OUT
        with open (savePath, 'w') as file_output:
            for attrib in attributeList:
                file_output.write ('%s\n' % attrib)
            file_output.close ()
        return

    def writeTabularFeed (self, tabularRecords):
        feedWriter = tfw.TabularFeedWriter ()
        feedWriter.setTabularRecords (tabularRecords)
        feedWriter.writeTSVFeed (uc.FILENAME_UPDATED_TSV_FEED_OUT)
        return

    def writeInjectedAVMap (self, injected_av_map):
        output_path = uc.FILENAME_INJECTED_AVMAP_OUT
        with open (output_path, 'w') as file_output:
            tsvWriter = csv.writer (file_output, delimiter = '\t')

            headerLine = injected_av_map [0].keys ()
            tsvWriter.writerow (headerLine)

            for row in injected_av_map:
                tsvWriter.writerow (row.values())
            file_output.close ()
        return


if __name__ == '__main__':
    logging.basicConfig (level=logging.DEBUG)

    updateDriver = UpdateMain ()

    revisionUpdater = updateDriver.instantiate_revision_updater ()
    if (revisionUpdater == None):
        raise Exception ('Cannot instantiate revision updater')

    # read feed file (jsonl)
    apiResponse = updateDriver.getSiteApiResponse (uc.SITE_CONTENT_DOCUMENTS_API_ENDPOINT)

    # read inject av map
    inject_av_map = updateDriver.read_inject_av_map ()

    # process the feed (ie, 'update')
    discovery_records, updatedAttributeList = updateDriver.performUpdates (revisionUpdater, apiResponse, inject_av_map)

    if (discovery_records != None) and (len (discovery_records) > 0):
        # generate injected_av_map
        injected_av_map = updateDriver.prepare_injected_av_map (discovery_records)

        # build tabular feed, then save as .tsv and .csv    
        tabularRecords = updateDriver.generateTabulerFeed (discovery_records, updatedAttributeList)

        updateDriver.writeJsonlFeed (discovery_records)
        updateDriver.writeAttributesToFile (updatedAttributeList)

        # currently, tabular feed is created only for 'full' feed
        updateDriver.writeTabularFeed (tabularRecords)

        # write injected_av_map
        updateDriver.writeInjectedAVMap (injected_av_map)

        logging.info ('Updated catalog has %s products' % len (discovery_records))

    logging.info ("Finished ...")


