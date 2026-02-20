# This 'updater' script expects its input to have been processed for various earlier requests
import logging
import json
import csv
import importlib
import os

import jsonlFeedReader as jfr
import jsonlWriter as jw
import tabularFeedBuilder as tfb
import tabularFeedWriter as tfw
import updaterConstants as uc

class UpdateMain ():
    def __init__ (self):
        return

    # actual feed updater object is instantiated for 'current-updater-version'
    def instantiate_feed_updater (self):
        feedUpdater = None
        # first load the current_update_class and instantiate it
        try:
            current_update_module = importlib.import_module (uc.CURRENT_UPDATER_MODULE)
            try:
                update_task_class = getattr (current_update_module, uc.UPDATER_CLASS_NAME)
                feedUpdater = update_task_class ()  # instantiate
            except AttributeError:
                logging.error ('Class does not exist: %s', uc.UPDATER_CLASS_NAME)
                return None, None
        except ImportError:
            logging.error ('Module does not exist: %s', uc.CURRENT_UPDATER_MODULE)
            return None, None
        return feedUpdater

    def loadSourceFeed (self):
        if (os.path.exists (uc.FILENAME_JSONL_SOURCE_FEED_IN) == False):
            logging.error ('Source feed file does not exist: %s', uc.FILENAME_JSONL_SOURCE_FEED_IN)
            return None

        srcFeedHandler = jfr.JsonlFeedReader ()
        srcProducts = srcFeedHandler.readSourceFeed (uc.FILENAME_JSONL_SOURCE_FEED_IN) 
        return srcProducts

    def performUpdates (self, feedUpdater, src_products):
        try:
            feedUpdater.set_source_records (src_products)
            updatedProducts, updatedAttributeList = feedUpdater.perform_updates ()
            return updatedProducts, updatedAttributeList
        except Exception as e:
            logging.error ('Error in processing source feed: %s', e)
        return None, None

    def performAdditionsIfAny (self, feedUpdater, updatedProducts, updatedAttributeList):
        try:
            updatedProducts, updatedAttributeList = feedUpdater.perform_additions (updatedProducts, updatedAttributeList)
            return updatedProducts, updatedAttributeList
        except Exception as e:
            logging.error ('Error in processing additions to source feed: %s', e)
        return None, None

    def prepareSubsetIfAny (self, feedUpdater, updatedProducts):
        try:
            subsetProducts, subsetAttributeList = feedUpdater.prepare_subset (updatedProducts)
            return subsetProducts, subsetAttributeList
        except Exception as e:
            logging.error ('Error in prepare subset to source feed: %s', e)
        return None, None

    def generateTabulerFeed (self, updatedProducts, updatedAttributeList):
        tabularFeedBuilder = tfb.TabularFeedBuilder ()
        tabularFeedBuilder.setUpdatedProducts (updatedProducts, updatedAttributeList)
        tabularRecords = tabularFeedBuilder.buildTabularFeed ()
        return tabularRecords

    def writeJsonlFeed (self, updatedProducts):
        # full feed
        feedWriter = jw.JsonlWriter ()
        feedWriter.setProducts (updatedProducts)
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

    def writeSubsetJsonlFeed (self, subsetProducts):
        # full feed
        feedWriter = jw.JsonlWriter ()
        feedWriter.setProducts (subsetProducts)
        feedWriter.write (uc.FILENAME_SUBSET_JSONL_FEED_OUT)
        return

    def writeSubsetAttributesToFile (self, subsetAttributeList):
        # sort the list
        subsetAttributeList.sort ()
        savePath = uc.FILENAME_SUBSET_FEED_ATTRIBUTELIST_OUT
        with open (savePath, 'w') as file_output:
            for attrib in subsetAttributeList:
                file_output.write ('%s\n' % attrib)
            file_output.close ()
        return

if __name__ == '__main__':
    logging.basicConfig (level=logging.DEBUG)

    updateDriver = UpdateMain ()
    feedUpdater = updateDriver.instantiate_feed_updater ()
    if (feedUpdater == None):
        raise Exception ('Cannot instantiate feed updater')

    # read feed file (jsonl)
    sourceProducts = updateDriver.loadSourceFeed ()
    if (sourceProducts != None):
        # process the feed (ie, 'update')
        updatedProducts, updatedAttributeList = updateDriver.performUpdates (feedUpdater, sourceProducts)

        if (updatedProducts != None) and (len (updatedProducts) > 0):
            updateDriver.performAdditionsIfAny (feedUpdater, updatedProducts, updatedAttributeList)

            # build tabular feed, then save as .tsv and .csv    
            tabularRecords = updateDriver.generateTabulerFeed (updatedProducts, updatedAttributeList)

            updateDriver.writeJsonlFeed (updatedProducts)
            updateDriver.writeAttributesToFile (updatedAttributeList)

            # currently, tabular feed is created only for 'full' feed
            updateDriver.writeTabularFeed (tabularRecords)

            logging.info ('Updated catalog has %s products' % len (updatedProducts))

        # if necessary, generate a subset
        subsetProducts, subsetAttributes = updateDriver.prepareSubsetIfAny (feedUpdater, updatedProducts)
        if (subsetProducts != None):
            updateDriver.writeSubsetJsonlFeed (subsetProducts)
            updateDriver.writeSubsetAttributesToFile (subsetAttributes)


    logging.info ("Finished ...")

'''
#     def writeEngagementTabularFeed (self, tabularRecords):
#         engagement_records = []
#         for record in tabularRecords:
#             # dont include a product if it has variants (ie, product record's own skuid is blank)
#             if (record ['skuid'] == None) or (record ['skuid'] == ''):
#                 logging.debug ('Engagement catalog, skipping product record, pid = %s' % record ['pid'])
#                 continue
# 
#             engagement_record = {}
#             for attrib_map in uc.ENGAGEMENT_ATTRIB_NAMES:
#                 src_attrib_name = attrib_map ['src']
#                 target_attrib_name = attrib_map ['target']
#                 engagement_record [target_attrib_name] = record [src_attrib_name]
# 
#             engagement_records.append (engagement_record)
# 
#         # write engagement feed -> output .tsv
#         feedWriter = tfw.TabularFeedWriter ()
#         feedWriter.setTabularRecords (engagement_records)
#         feedWriter.writeTSVFeed (uc.FILENAME_UPDATED_ENGAGEMENT_TSV_FEED_OUT)
#         return
#########
#           # specifically for Engagement catalog, prepare a engagement-specific tabular feed
#           # ie, exclude category_paths, etc etc
#           updateDriver.writeEngagementTabularFeed (tabularRecords)

'''

