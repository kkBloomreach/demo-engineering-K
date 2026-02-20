# V24 changes
# -- replace 'size' attribute to 'length' for specific category

import logging
import copy
import os
import csv
import random

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV24 as rcv24

# in this revision, products only in these categories are considered 
SELECT_CATEGORY_IDS = [
    '90000>90200'    # necklace
]

class RevisionV24 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v24')
        super().__init__ ()
        return

    def _initialize (self, source_records, inject_av_map):
        return True

    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)
        #if (inject_av_record == None):
        #    logging.debug ('No inject attrib_value record for pid: %s', pid)

        # change color value only in select category
        if self._is_select_category (record) == True:
            updated_record = self._perform_update_internal (record, inject_av_record)
            return updated_record
        else:
            updated_record = record
        return updated_record

    def _finalize (self, updated_products):
        return True

    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        pid = record ['value']['attributes']['pid']
        updated_record = copy.deepcopy (record)

        product_length = []
        if ('variants' in updated_record ['value']) and (updated_record ['value']['variants']):
            variant_list = updated_record ['value']['variants']
            for variant_id in variant_list.keys():
                variant_obj = updated_record ['value']['variants'][variant_id]
                if 'size' in variant_obj ['attributes']:
                    size_value = variant_obj ['attributes']['size']
                    length = 18  #default
                    match size_value:
                        case 'short':  length = 12
                        case 'medium': length = 18
                        case 'long':   length = 24
                        case _:        length = 18
 
                    variant_obj ['attributes']['length'] = length
                    product_length.append (length)
                    del variant_obj ['attributes']['size']

        # update product-level attribute product_size -> product_length
        if 'product_size' in updated_record ['value']['attributes']:
            del updated_record ['value']['attributes']['product_size']
        updated_record ['value']['attributes']['product_length'] = product_length

        return updated_record

    def _is_select_category (self, record):
        category_paths = record ['value']['attributes']['category_paths']
        for branch in category_paths:
            full_path = None 
            for leaf_node in branch:
                if full_path == None:
                    full_path = leaf_node ['id']
                else:
                    full_path = '%s>%s' % (full_path, leaf_node ['id'])
            if full_path in SELECT_CATEGORY_IDS:
                return True
        return False

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV24 ()
    logging.info ('RevisionV24 finish...')


