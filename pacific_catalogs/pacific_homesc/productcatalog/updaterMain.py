# This 'updater' script expects its input to have been converted and 'massaged' for various earlier requests
# (eg, change product brands)

import logging
import json
import csv
import importlib
import os

import jsonlFeedReader as jfr
import jsonlWriter as jw
import tabularFeedReader as tfr
import tabularFeedBuilder as tfb
import tabularFeedWriter as tfw
import updaterConstants as uc
import categoryManager as cm

class UpdateMain ():
    def __init__ (self):
        return

    # actual feed updater object is instantiated for 'current-revision-updater'
    def instantiate_revision_handler (self):
        revision_handler = None
        # first load the revision_update_class and instantiate it
        try:
            revision_update_module = importlib.import_module (uc.REVISION_UPDATER_MODULE)
            try:
                update_task_class = getattr (revision_update_module, uc.REVISION_CLASS_NAME)
                revision_handler = update_task_class ()  # instantiate
            except AttributeError:
                logging.error ('Class does not exist: %s', uc.REVISION_CLASS_NAME)
                return None
            except Exception as e:
                logging.error ('Class instantiation failed: %s', uc.REVISION_CLASS_NAME)
                return None
        except ImportError:
            logging.error ('Module does not exist: %s', uc.REVISION_UPDATER_MODULE)
            return None
        return revision_handler

    def loadSourceFeed (self):
        srcFeedHandler = jfr.JsonlFeedReader ()
        srcProducts = srcFeedHandler.readSourceFeed (uc.FILENAME_JSONL_SOURCE_FEED_IN) 
        return srcProducts

    # returns 'category_manager' object. Internally it builds tree using source product records
    def build_category_tree (self, srcProducts):
        logging.debug ('Build category tree...')
        category_manager = cm.CategoryManager ()
        tree_root = category_manager.build_tree (srcProducts)
        logging.debug ('Build category tree... complete')
        return category_manager

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
    def perform_revision (self, revision_handler, src_products, category_manager, inject_av_map):
        try:
            revision_handler.set_source_records (src_products)
            revision_handler.set_category_manager (category_manager)
            revision_handler.set_inject_av_map (inject_av_map)
            updatedProducts, updatedAttributeList = revision_handler.perform_revision ()
            return updatedProducts, updatedAttributeList
        except Exception as e:
            logging.error ('Error in processing source feed: %s', e)
        return None, None

    def prepare_injected_av_map (self, updated_records, updated_attribute_list):
        injected_av_map = []
        for record in updated_records:
            injected_av_record = {}
            for attrib_name in updated_attribute_list:
                if (attrib_name in uc.INJECTED_AVMAP_EXCLUDED_ATTRIBUTES):
                    continue
                if attrib_name in record ['value']['attributes']:
                    injected_av_record [attrib_name] = record ['value']['attributes'][attrib_name]
                else:
                    injected_av_record [attrib_name] = '-'

            # NOTE: Of the attribs in inject-record, some of them cannot be edited
            injected_av_record_with_editable = injected_av_record.copy ()
            for attrib_name in injected_av_record:
                if (attrib_name == 'pid' or attrib_name == 'url' or attrib_name == 'skuid'):
                    continue
                edited_attrib_name = '%s_%s' % ('edited', attrib_name)
                injected_av_record_with_editable [edited_attrib_name] = injected_av_record [attrib_name]

            injected_av_map.append (injected_av_record_with_editable)
        return injected_av_map

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

    def writeInjectedAVMap (self, injected_av_map):
        output_path = uc.FILENAME_INJECTED_AVMAP_TSV_OUT
        with open (output_path, 'w') as file_output:
            tsvWriter = csv.writer (file_output, delimiter = '\t')

            headerLine = injected_av_map [0].keys ()
            tsvWriter.writerow (headerLine)

            for row in injected_av_map:
                tsvWriter.writerow (row.values())
            file_output.close ()
        return

    def writeCategoryTree (self, category_manager):
        output_path = uc.FILENAME_CATEGORY_TREE_TSV_OUT
        category_manager.write_tsv (output_path)
        return

if __name__ == '__main__':
    logging.basicConfig (level=logging.DEBUG)

    updateDriver = UpdateMain ()

    revision_handler = updateDriver.instantiate_revision_handler ()
    if (revision_handler == None):
        raise Exception ('Cannot instantiate revision handler')

    # read feed file (jsonl)
    sourceProducts = updateDriver.loadSourceFeed ()

    # read inject av map
    inject_av_map = updateDriver.read_inject_av_map ()

    # build category tree for this catalog
    category_manager = updateDriver.build_category_tree (sourceProducts)

    # process the feed (ie, 'update')
    updatedProducts, updatedAttributeList = updateDriver.perform_revision (revision_handler, sourceProducts, category_manager, inject_av_map)

    if (updatedProducts != None) and (len (updatedProducts) > 0):
        injected_av_map = updateDriver.prepare_injected_av_map (updatedProducts, updatedAttributeList)

        # build tabular feed, then save as .tsv and .csv    
        tabularRecords = updateDriver.generateTabulerFeed (updatedProducts, updatedAttributeList)

        updateDriver.writeJsonlFeed (updatedProducts)
        updateDriver.writeAttributesToFile (updatedAttributeList)

        # currently, tabular feed is created only for 'full' feed
        updateDriver.writeTabularFeed (tabularRecords)

        # write injected_av_map
        updateDriver.writeInjectedAVMap (injected_av_map)

        # write category tree
        updateDriver.writeCategoryTree (category_manager)


        logging.info ('Updated catalog has %s products' % len (updatedProducts))
    else:
        logging.warning ('Updated product count is zero')
 
    logging.info ("Finished ...")


