
import logging
import csv
import importlib
import os

import jsonlFeedReader as jfr
import jsonlWriter as jw
import tabularFeedReader as tfr
import tabularFeedBuilder as tfb
import tabularFeedWriter as tfw
import updaterConstants as uc
from categorybuilder import CategoryBuilder

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
                return None, None
        except ImportError:
            logging.error ('Module does not exist: %s', uc.REVISION_UPDATER_MODULE)
            return None, None
        return revision_handler

    def loadSourceFeed (self):
        srcFeedHandler = jfr.JsonlFeedReader ()
        srcProducts = srcFeedHandler.readSourceFeed (uc.FILENAME_JSONL_SOURCE_FEED_IN) 
        return srcProducts

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
    def perform_revision (self, revision_handler, src_products, category_builder, inject_av_map):
        try:
            revision_handler.set_source_records (src_products)
            revision_handler.set_category_builder (category_builder)
            revision_handler.set_inject_av_map (inject_av_map)
            #updatedProducts, updatedProducts_datahub, updatedAttributeList = revision_handler.perform_revision ()
            #return updatedProducts, updatedProducts_datahub, updatedAttributeList
            updatedProducts, updatedProducts_datahub, updatedAttributeList, updatedProducts_engagement, updatedAttributeList_engagement  = revision_handler.perform_revision ()
            return updatedProducts, updatedProducts_datahub, updatedAttributeList, updatedProducts_engagement, updatedAttributeList_engagement
        except Exception as e:
            logging.error ('Error in processing source feed: %s', e)
        return None, None, None, None, None

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

    # params may be dataconnect_catalog OR engagement_catalog
    def generateTabularFeed (self, updatedProducts, updatedAttributeList):
        tabularFeedBuilder = tfb.TabularFeedBuilder ()
        tabularFeedBuilder.setUpdatedProducts (updatedProducts, updatedAttributeList)
        tabularRecords = tabularFeedBuilder.buildTabularFeed ()
        return tabularRecords

    # product_catalog can be dataConnect format OR datahub
    def writeJsonlFeed (self, product_catalog, format = 'dataconnect'):
        # full feed
        feedWriter = jw.JsonlWriter ()
        feedWriter.setProducts (product_catalog)
        if format == 'dataconnect':
            feedWriter.write (uc.FILENAME_UPDATED_JSONL_FEED_OUT)
        elif format == 'datahub':
            feedWriter.write (uc.FILENAME_UPDATED_DATAHUB_JSONL_FEED_OUT)
        else:
            logging.error ('Unknown output format: %s' % format)
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

    # tabular records for engagement have slightly different set of attributes
    def writeTabularFeed (self, tabularRecords, target = "dataconnect"):
        feedWriter = tfw.TabularFeedWriter ()
        feedWriter.setTabularRecords (tabularRecords)
        if target == "dataconnect":
            feedWriter.writeTSVFeed (uc.FILENAME_UPDATED_DATACONNECT_TSV_FEED_OUT)
        else:
            feedWriter.writeTSVFeed (uc.FILENAME_UPDATED_ENGAGEMENT_TSV_FEED_OUT)
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

    revision_handler = updateDriver.instantiate_revision_handler ()
    if (revision_handler == None):
        raise Exception ('Cannot instantiate revision handler')

    # read feed file (jsonl)
    sourceProducts = updateDriver.loadSourceFeed ()

    # read inject av map
    inject_av_map = updateDriver.read_inject_av_map ()

    # prepare category builder
    category_builder = CategoryBuilder ()
    category_builder.load_category_map (uc.FILENAME_CATEGORY_MAP_IN)

    # process the feed (ie, 'update')
    updatedProducts, updatedProducts_datahub, updatedAttributeList, updatedProducts_engagement, updatedAttributeList_engagement = \
                    updateDriver.perform_revision (revision_handler, sourceProducts, category_builder, inject_av_map)

    if (updatedProducts != None) and (len (updatedProducts) > 0):
        injected_av_map = updateDriver.prepare_injected_av_map (updatedProducts, updatedAttributeList)

        # build tabular feed, then save as .tsv and .csv
        # Note: tsv, csv formatted output generated using dataConnect style catalog (not dataHub format) 
        tabularRecords = updateDriver.generateTabularFeed (updatedProducts, updatedAttributeList)

        # engagement tabular records, save as .csv
        tabularRecords_engagement = updateDriver.generateTabularFeed (updatedProducts_engagement, updatedAttributeList_engagement)

        updateDriver.writeJsonlFeed (updatedProducts)   # dataConnect format by default
        updateDriver.writeJsonlFeed (updatedProducts_datahub, format = 'datahub')

        # attributes in the catalog
        updateDriver.writeAttributesToFile (updatedAttributeList)

        # tabular feed (dataconnect, datahub)
        updateDriver.writeTabularFeed (tabularRecords)

        # tabular feed (engagement)
        updateDriver.writeTabularFeed (tabularRecords_engagement, target = "engagement")

        # write injected_av_map
        updateDriver.writeInjectedAVMap (injected_av_map)

        logging.info ('Updated catalog has %s products' % len (updatedProducts))
    else:
        logging.warning ('Updated product count is zero')
 
    logging.info ("Finished ...")

'''
    def loadGenImageMap (self):
        # @@ TEMP - depends if a specific revision needs new images
        gen_image_map = None
        return gen_image_map

    def performAdditionsIfAny (self, revision_handler, updatedProducts, updatedAttributeList):
        try:
            revision_handler.set_category_builder (category_builder)
            updatedProducts, updatedAttributeList = revision_handler.perform_additions (updatedProducts, updatedAttributeList)
            return updatedProducts, updatedAttributeList
        except Exception as e:
            logging.error ('Error in processing source feed: %s', e)
        return None, None

    # read generated image map file (tsv)
    gen_image_map = updateDriver.loadGenImageMap ()

        updatedProducts, updatedAttributeList = updateDriver.performAdditionsIfAny (revision_handler, updatedProducts, updatedAttributeList)

        # generate injected_av_map
        if (updatedProducts != None) and (len (updatedProducts) > 0):
-------
            injected_av_record ['edited_availability'] = injected_av_record ['availability']
            injected_av_record ['edited_price'] = injected_av_record ['price']
            injected_av_record ['edited_gender'] = injected_av_record ['gender']
            injected_av_record ['edited_color'] = injected_av_record ['color']
            injected_av_record ['edited_brand'] = injected_av_record ['brand']
            injected_av_record ['edited_material'] = injected_av_record ['material']
            injected_av_record ['edited_collection'] = injected_av_record ['collection']
            injected_av_record ['edited_style'] = injected_av_record ['style']
            #injected_av_record ['edited_bread_crumb'] = injected_av_record ['bread_crumb']
            injected_av_record ['edited_title'] = injected_av_record ['title']
            injected_av_record ['edited_stock_level'] = injected_av_record ['stock_level']
            injected_av_record ['edited_special_offer'] = injected_av_record ['special_offer']
            injected_av_record ['edited_season'] = injected_av_record ['season']
            if 'product_fit' in injected_av_record:
                injected_av_record ['edited_product_fit'] = injected_av_record ['product_fit']
            if 'feature' in injected_av_record:
                injected_av_record ['edited_feature'] = injected_av_record ['feature']
            injected_av_map.append (injected_av_record)
---------
    def prepare_injected_av_map_OLD (self, updatedProducts, category_builder):
        injected_av_map = []
        for record in updatedProducts:
            injected_av_record = {}

            for attrib in uc.INJECTED_AVMAP_ATTRIBUTES:
                record_attribs = record ['value']['attributes']
                if (attrib == 'pid'):
                    injected_av_record ['pid'] = record_attribs ['pid']
                elif (attrib == 'url'):
                    injected_av_record ['url'] = record_attribs ['url']
                elif (attrib == 'availability'):
                    injected_av_record ['availability'] = record_attribs ['availability']
                elif (attrib == 'price'):
                    injected_av_record ['price'] = record_attribs ['price']
                elif (attrib == 'gender'):
                    injected_av_record ['gender'] = record_attribs ['gender']
                elif (attrib == 'color'):
                    if 'color' in record_attribs:
                        injected_av_record ['color'] = record_attribs ['color']
                    else:
                        injected_av_record ['color'] = ''
                elif (attrib == 'brand'):
                    injected_av_record ['brand'] = record_attribs ['brand']
                elif (attrib == 'material'):
                    injected_av_record ['material'] = record_attribs ['material']
                elif (attrib == 'collection'):
                    injected_av_record ['collection'] = record_attribs ['collection']
                elif (attrib == 'style'):
                    injected_av_record ['style'] = record_attribs ['style']
                elif (attrib == 'bread_crumb'):
                    bread_crumb, bread_crumb_id = category_builder.construct_crumbs_and_crumbIds_from_category_path (record)
                    injected_av_record ['bread_crumb'] = bread_crumb
                elif (attrib == 'title'):
                    injected_av_record ['title'] = record_attribs ['title']
                elif (attrib == 'description'):
                    injected_av_record ['description'] = record_attribs ['description']
                elif (attrib == 'special_offer'):
                    injected_av_record ['special_offer'] = record_attribs ['special_offer']
                elif (attrib == 'season'):
                    injected_av_record ['season'] = record_attribs ['season']
                elif (attrib == 'stock_level'):
                    injected_av_record ['stock_level'] = record_attribs ['stock_level']
                elif (attrib == 'product_fit'):
                    if 'product_fit' in record_attribs:
                        injected_av_record ['product_fit'] = record_attribs ['product_fit']
                    else:
                        injected_av_record ['product_fit'] = ''
                elif (attrib == 'feature'):
                    if 'feature' in record_attribs:
                        injected_av_record ['feature'] = record_attribs ['feature']
                    else:
                        injected_av_record ['feature'] = ''
                else:
                    logging.warning ('Unexpected attribute in injected_av_map: %s' % attrib)
                    continue

            # Following can be edited manually
            for attrib in uc.INJECTED_AVMAP_ATTRIBUTES:
                if attrib in injected_av_record:
                    edited_attrib_name = 'edited_%s' % attrib
                    injected_av_record [edited_attrib_name] = injected_av_record [attrib]
            injected_av_map.append (injected_av_record)
        return injected_av_map
'''

