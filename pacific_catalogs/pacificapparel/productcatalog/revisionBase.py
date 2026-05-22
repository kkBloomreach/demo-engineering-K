# base class for actual 'revision update' task to be performed
# Base class updated to support datahub
# -- generate dataHub formatted output (besides dataConnect format)
# -- Only two differences in the formats:
# --  in dataHub format 'path' is like '/<pid> (in dataConnect, path: '/product/<pid>'
# --  in datahub format 'fields' is used instead of 'attributes'
# -- Also generate engagement-compatible .tsv

import logging
import traceback
import copy
import os
from dotenv import dotenv_values

import engagementConstants as ec

class RevisionBase ():
    _source_records = None
    _category_builder = None
    _inject_av_map = None

    def __init__ (self):
        self._env_configs = None
        if os.path.exists (".env"):
            self._env_configs = dotenv_values (".env")
        if self._env_configs == None:
            logging.warning ('Cannot find environment configuration')
        return

    def set_source_records (self, source_records):
        self._source_records = source_records
        return

    def set_category_builder (self, category_builder):
        self._category_builder = category_builder
        return

    def set_inject_av_map (self, inject_av_map):
        self._inject_av_map = inject_av_map 
        return

    # perform actual update, additions, ... return updated
    def perform_revision (self):
        updated_products = None

        # initialize - once, if any
        try:
            if self._initialize (self._source_records, self._inject_av_map ) == False:
                logging.error ('Revision module initialization failed')
                return (None, None, None, None, None)
        except Exception as e:
            logging.error ('Exception in initialize')
            traceback.print_exc ()

        # perform updates to existing records, if any
        try:
            updated_products = self.__perform_updates ()
        except Exception as e:
            logging.error ('Exception in perform_updates')
            traceback.print_exc ()

        if (updated_products == None) or (len (updated_products) == 0):
            logging.error ('Revision module perform_update failed')
            return (None, None, None, None, None)

        try:
            updated_products = self._perform_additions (updated_products)
        except Exception as e:
            logging.error ('Exception in perform_additions')
            traceback.print_exc ()

        if (updated_products == None) or (len (updated_products) == 0):
            logging.error ('Revision module perform_additions failed')
            return (None, None, None, None, None)

        # finalize - once, if any
        # modified variation to support datahub
        try:
            updated_products = self._finalize (updated_products)
            if updated_products == None:
                logging.error ('Revision module finalization failed')
                return (None, None, None, None, None)
        except Exception as e:
            logging.error ('Exception in finalize')
            traceback.print_exc ()

        # collect all attributes in entire catalog
        updated_attributes = self._collect_attributes (updated_products)

        # after dataconnect-revision is complete, construct datahub formatted output
        updated_products_datahub = self._construct_datahub_format (updated_products)

        # from dataconnect catalog, construct engagement formatted output. 
        updated_products_engagement = self._construct_engagement_format (updated_products)
        updated_attributes_engagement = self._collect_attributes (updated_products_engagement)

        return updated_products, updated_products_datahub, updated_attributes, updated_products_engagement, updated_attributes_engagement

    # let derived class do any one-time initialization
    def _initialize (self, source_records, inject_av_map ):
        return True

    # perform additions and return total catalog
    # derived class MAY override this method to return total products including additionals
    def _perform_additions (self, current_products):
        return current_products # default

    # let derived class do any one-time finalization
    def _finalize (self, updated_products):
        return updated_products

    ### For derived class
    def _perform_record_update (self, record):
        raise Exception ('Record update method must be implemented in derived classs')
        return

    # methods called by derived class
    def _lookup_inject_av_record (self, pid):
        if self._inject_av_map == None:
            return None

        for inject_record in  self._inject_av_map:
            if inject_record ['pid'] == pid:
                return inject_record

        return None

    # ----------------
    # methods called within Base class
    def __perform_updates (self):
        updated_records = []
        for record in self._source_records:
            try:
                updated_record = self._perform_record_update (record)
                if (updated_record != None):
                    updated_records.append (updated_record)
            except Exception as e:
                logging.warning ('Record update failed for pid: %s, error = %s' % (record ['value']['attributes']['pid'], e))
                traceback.print_exc ()
                continue
        return updated_records

    # collect all attributes, across entire catalog
    # param may be 'dataconnect' catalog or 'engagement'. Latter has fewer/translated attribs
    def _collect_attributes (self, catalog_products):
        updated_attributes = []
        for record in catalog_products:
            product_attribs = record ['value']['attributes']
            for attrib in product_attribs.keys():
                if attrib not in updated_attributes:
                    updated_attributes.append (attrib)

            if ('variants' in record ['value']) and (record ['value']['variants']):
                variant_list = record ['value']['variants']
                for variant_id, variant_obj in variant_list.items():
                    variant_attribs = variant_obj ['attributes'].keys ()
                    for attrib in variant_attribs:
                        if attrib not in updated_attributes:
                            updated_attributes.append (attrib)
        return updated_attributes

    def _construct_datahub_format (self, updated_products):
        updated_products_datahub = []
        for dc_record in updated_products:  # dc == dataConnect
            datahub_record = copy.deepcopy (dc_record)
            pid = datahub_record ['value']['attributes']['pid']
            datahub_record ['path'] = '/%s' % pid

            # rename product level 'attributes' -> 'fields'
            datahub_value_obj = datahub_record ['value']
            datahub_value_obj ['fields'] = copy.deepcopy (datahub_value_obj ['attributes'])
            del (datahub_value_obj ['attributes'])

            # for each variant, rename its 'attributes' -> 'fields'
            if ('variants' in datahub_value_obj) and (datahub_value_obj ['variants']):
                variant_list = datahub_value_obj ['variants']
                for variant_id in variant_list.keys ():
                    datahub_variant_obj = datahub_value_obj ['variants'][variant_id]
                    datahub_variant_obj ['fields'] = copy.deepcopy (datahub_variant_obj ['attributes'])
                    del (datahub_variant_obj ['attributes'])

            updated_products_datahub.append (datahub_record)
        return updated_products_datahub

    def _construct_engagement_format (self, updated_products):
        engagement_products = []

        for dataconnect_product in updated_products:
            engagement_product_record = self._prepare_engagement_record (dataconnect_product)
            if (engagement_product_record != None):
                engagement_products.append (engagement_product_record)
        return engagement_products

    # 'pid' is not a required attribute in discovery catalog
    # However, it is used internally and required in engagement catalog
    def _prepare_engagement_record (self, dataconnect_record):
        if 'pid' in dataconnect_record ['value']['attributes']:
            pid = dataconnect_record ['value']['attributes']['pid']
        else:
            path = dataconnect_record ['path']
            indx = path.rindex ('/')
            if indx > 0:
                pid = path [indx+1:]
            else:
                pid = path # @@@ ???
            dataconnect_record ['value']['attributes']['pid'] = pid

        # duplicate original record
        engagement_record = copy.deepcopy (dataconnect_record)

        # product-level attributes
        output_attributes = self._translate_attributes (dataconnect_record ['value']['attributes'])
        engagement_record ['value']['attributes'] = output_attributes

        # variant-level attributes
        if ('variants' in dataconnect_record ['value']) and  (dataconnect_record ['value']['variants']):
            src_variant_list = dataconnect_record ['value']['variants']
            for src_variant_id, src_variant_obj in src_variant_list.items():
                output_variant_attributes = self._translate_attributes (src_variant_obj ['attributes'])
                engagement_record ['value']['variants'][src_variant_id]['attributes'] = output_variant_attributes

        # if engagement=specific category_level attribs don't exist, collect them
        if 'category_level_1' not in dataconnect_record ['value']['attributes']:
            # collect from discovery-record. The 'category_paths' from engagement_record may have been deleted already
            if 'category_paths' in dataconnect_record ['value']['attributes']:
                category_levels = self._collect_category_levels (dataconnect_record)
                for i in range (0, ec.MAX_CATEGORY_LEVELS):
                    level_name = ''
                    if (i < len (category_levels)) and (category_levels [i]):
                        level_name = category_levels [i]
                    attrib_name = '%s%s' % (ec.PREAMBLE_ATTRIB_NAME_CATEGORY_LEVEL, i+1)
                    engagement_record ['value']['attributes'][attrib_name] = level_name
            else:
                logging.warning ('No category_paths in source feed')
        return engagement_record

    # translate attributes - can be product-level or variant
    # param is entire 'attributes' object, with attrib_keys
    # returns new output_attributes
    def _translate_attributes (self, src_attributes):
        output_attributes = copy.deepcopy (src_attributes)  # copy 'attributes' object within the product record

        for src_attrib_key in src_attributes.keys ():
            src_value = src_attributes [src_attrib_key]
            if isinstance (src_value, str):
                src_value = src_value.replace (',', ec.COMMA_REPLACEMENT)

            special_op_rec = self._lookup_engagement_special_op_if_any (src_attrib_key)
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

    # given 'src' attrib name, see if it has 'special-op' associated
    def _lookup_engagement_special_op_if_any (self, attrib):
        for special_op in ec.ATTRIB_SPECIAL_OPERATIONS:
            if special_op ['src'] == attrib:
                return special_op
        return None

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

