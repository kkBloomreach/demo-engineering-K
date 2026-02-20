# convert jsonl to csv/tsv
import logging
import os
import copy
import csv
import sys

import convertConstants as cc
import jsonlFeedReader as jfr
import tabularFeedBuilder as tfb
import tabularFeedWriter as tfw

class ConvertMain ():
    def __init__ (self):
        return

    def loadSourceFeed (self):
        if (os.path.exists (cc.FILENAME_JSONL_SOURCE_FEED_IN) == False):
            logging.error ('Source feed file does not exist: %s', cc.FILENAME_JSONL_SOURCE_FEED_IN)
            return None

        srcFeedHandler = jfr.JsonlFeedReader ()
        srcProducts = srcFeedHandler.readSourceFeed (cc.FILENAME_JSONL_SOURCE_FEED_IN)
        return srcProducts

    # prepare engagement records for discovery feed
    def prepare_engagement_records (self, source_discovery_products):
        engagement_product_records = []
        engagement_product_attributes = []

        for discovery_product in source_discovery_products:
            engagement_product_record = self._prepare_engagement_record (discovery_product)
            if (engagement_product_record != None):
                engagement_product_records.append (engagement_product_record)
                self._collect_attributes (engagement_product_record, engagement_product_attributes)
        return engagement_product_records, engagement_product_attributes

    # In engagement csv, 'item_id' for product-level record is item_id itself
    def prepare_tabular_feed_engagement (self, output_engagement_products, output_engagement_attributeList):
        tabularFeedBuilder = tfb.TabularFeedBuilder ()
        tabularFeedBuilder.setUpdatedProducts (output_engagement_products, output_engagement_attributeList)
        engagementTabularRecords = tabularFeedBuilder.buildTabularFeed ()
        return engagementTabularRecords

    def writeTabularFeed (self, tabularRecords):
        feedWriter = tfw.TabularFeedWriter ()
        feedWriter.setTabularRecords (tabularRecords)
        feedWriter.writeTSVFeed (cc.FILENAME_UPDATED_TSV_FEED_OUT)
        return 

    def writeTabularFeedCSV (self, tabularRecords):
        feedWriter = tfw.TabularFeedWriter ()
        feedWriter.setTabularRecords (tabularRecords)
        feedWriter.writeCSVFeed (cc.FILENAME_UPDATED_CSV_FEED_OUT)
        return 

    # method called within Base class - collect all attributes, across entire catalog
    def _collect_attributes (self, record, source_attributes):
        product_attribs = record ['value']['attributes']
        for attrib in product_attribs.keys():
            if attrib not in source_attributes:
                source_attributes.append (attrib)

        if ('variants' in record ['value']) and (record ['value']['variants']):
            variant_list = record ['value']['variants']
            for variant_id, variant_obj in variant_list.items():
                variant_attribs = variant_obj ['attributes'].keys ()
                for attrib in variant_attribs:
                    if attrib not in source_attributes:
                        source_attributes.append (attrib)
        return

    # 'pid' is not a required attribute in discovery catalog
    # However, it is used internally and required in engagement catalog
    def _prepare_engagement_record (self, discovery_record):
        if 'pid' in discovery_record ['value']['attributes']:
            pid = discovery_record ['value']['attributes']['pid']
        else:
            path = discovery_record ['path']
            indx = path.rindex ('/')
            if indx > 0:
                pid = path [indx+1:]
            else:
                pid = path # @@@ ???
            discovery_record ['value']['attributes']['pid'] = pid

        # duplicate original record
        engagement_record = copy.deepcopy (discovery_record)

        # product-level attributes
        output_attributes = self._convert_attributes (discovery_record ['value']['attributes'])
        engagement_record ['value']['attributes'] = output_attributes

        # variant-level attributes
        if ('variants' in discovery_record ['value']) and  (discovery_record ['value']['variants']):
            src_variant_list = discovery_record ['value']['variants']
            for src_variant_id, src_variant_obj in src_variant_list.items():
                output_variant_attributes = self._convert_attributes (src_variant_obj ['attributes'])
                engagement_record ['value']['variants'][src_variant_id]['attributes'] = output_variant_attributes

        # if engagement=specific category_level attribs don't exist, collect them
        if 'category_level_1' not in discovery_record ['value']['attributes']:
            # collect from discovery-record. The 'category_paths' from engagement_record may have been deleted already
            category_levels = self._collect_category_levels (discovery_record)
            for i in range (0, cc.MAX_CATEGORY_LEVELS):
                level_name = ''
                if (i < len (category_levels)) and (category_levels [i]):
                    level_name = category_levels [i]
                attrib_name = '%s%s' % (cc.PREAMBLE_ATTRIB_NAME_CATEGORY_LEVEL, i+1)
                engagement_record ['value']['attributes'][attrib_name] = level_name

        return engagement_record

    # given 'src' attrib name, see if it has 'special-op' associated
    def _lookup_special_op_if_any (self, attrib):
        for special_op in cc.ATTRIB_SPECIAL_OPERATIONS:
            if special_op ['src'] == attrib:
                return special_op
        return None

    # convert attributes - can be product-level or variant
    # param is entire 'attributes' object, with attrib_keys
    # returns new output_attributes
    def _convert_attributes (self, src_attributes):
        output_attributes = copy.deepcopy (src_attributes)

        for src_attrib_key in src_attributes.keys ():
            src_value = src_attributes [src_attrib_key]
            if isinstance (src_value, str): 
                src_value = src_value.replace (',', cc.COMMA_REPLACEMENT)

            special_op_rec = self._lookup_special_op_if_any (src_attrib_key)
            if special_op_rec:
                target_attrib_key = special_op_rec ['target']
                if special_op_rec ['op'] == 'rename':
                    # copy src_variant_attrib value as-is
                    output_attributes [target_attrib_key] = src_value
                    # remove src_variant_attribute from engagement-record
                    del output_attributes [src_attrib_key]
                elif special_op_rec ['op'] == 'delete':
                    del output_attributes [src_attrib_key]
                elif special_op_rec ['op'] == 'add':
                    # when adding a new attrib, we continue to keep the src_variant_attrib as-is
                    output_attributes [target_attrib_key] = src_value
            else:
                # no special-op for this attrib. It is copied as-is
                output_attributes [src_attrib_key] = src_value   # comma, if any, already replaced
        return output_attributes

    # use category_paths to return list of [l0, l1, ...] of first category
    def _collect_category_levels (self, record):
        category_levels = []

        category_paths = record ['value']['attributes']['category_paths']
        if (category_paths == None) or (len (category_paths) == 0):
            return category_levels # zero-length list

        branch_path_0 = category_paths [0]   # use first branch
        for branch_nodes in branch_path_0:
            category_levels.append (branch_nodes ['name'])
        return category_levels

if __name__ == '__main__':
    logging.basicConfig (level=logging.DEBUG)

    convertDriver = ConvertMain ()

    # read feed file (jsonl)
    sourceProducts = convertDriver.loadSourceFeed ()
    if (sourceProducts == None) or (len (sourceProducts) == 0):
        logging.error ('cannot load source jsonl feed')
        sys.exit (1)

    # convert
    output_engagement_products, output_engagement_product_attributes = convertDriver.prepare_engagement_records (sourceProducts)

    # CSV to be used to upload to engagement catalog
    engagement_tabular_records = convertDriver.prepare_tabular_feed_engagement (output_engagement_products, 
                                                                             output_engagement_product_attributes)
    if (len (engagement_tabular_records) > 0):
        convertDriver.writeTabularFeedCSV (engagement_tabular_records)
    else:
        logging.warning ('Engagement feed has no records')

    logging.info ('converted engagement catalog has %s records' % len (engagement_tabular_records))
    logging.info ("Finished ...")


