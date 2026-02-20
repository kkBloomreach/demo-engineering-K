# V6 changes
# -- Ensure each product has at least one variant
# -- Ensure 'price' and 'sale_price' are floats (not string)

import logging
import random
import os
import copy
import time

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV6 as rcv6

class RevisionV6 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v6')
        super().__init__ ()
        return

    def _initialize (self, source_records, inject_av_map):
        return True
 
    # override base class method
    # category_manager not needed in this revision
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)
        if (inject_av_record == None):
            logging.debug ('No inject attrib_value record for pid: %s', pid)

        updated_record = self._perform_update_internal (record, inject_av_record)
        return updated_record

    def _finalize (self, updated_products):
        return True # Place holder
 
    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']

        updated_record = copy.deepcopy (record)

        # ensure at least one variant exists in this record
        self._ensure_product_has_variant (updated_record)

        # ensure variant->price, sale_price are float values
        self._ensure_variant_price_type (updated_record)

        return updated_record

    def _ensure_product_has_variant (self, record):
        if 'variants' in record ['value']:
            if len (record ['value']['variants']) > 0:
                return record   # record has some variants
        # this record has no variants
        logging.debug ('Creating variant for pid: %s' % record ['value']['attributes']['pid'])
        variant_id = '%s_1' % record ['value']['attributes']['pid']
        record ['value']['variants'] = {}
        record ['value']['variants'][variant_id] = {}
        record ['value']['variants'][variant_id]['attributes'] = {}
        record ['value']['variants'][variant_id]['attributes']['skuid'] = variant_id
        record ['value']['variants'][variant_id]['attributes']['int_skuid'] = variant_id
        record ['value']['variants'][variant_id]['attributes']['price'] = record ['value']['attributes']['price']
        record ['value']['variants'][variant_id]['attributes']['sale_price'] = record ['value']['attributes']['sale_price']
        record ['value']['variants'][variant_id]['attributes']['velo_sku_price'] = record ['value']['attributes']['price']
        record ['value']['variants'][variant_id]['attributes']['velo_sku_sale_price'] = record ['value']['attributes']['sale_price']
        record ['value']['variants'][variant_id]['attributes']['default_sku'] = True
        record ['value']['variants'][variant_id]['attributes']['colorFamily'] = 'Multi'
        record ['value']['variants'][variant_id]['attributes']['sizeFamily'] = None
        record ['value']['variants'][variant_id]['attributes']['swatch_image'] = record ['value']['attributes']['thumb_image']

        return

    # earlier versions had variant->price,sale_price as 'string'
    def _ensure_variant_price_type (self, record):
        if ('variants' in record ['value']) and (record ['value']['variants']):
            variant_list = record ['value']['variants']
            for variant_id, variant_obj in variant_list.items():
                if 'price' in variant_obj ['attributes']:
                    if isinstance (variant_obj ['attributes']['price'], str):
                        variant_obj ['attributes']['price'] = round (float (variant_obj ['attributes']['price']), 2)
                if 'sale_price' in variant_obj ['attributes']:
                    if isinstance (variant_obj ['attributes']['sale_price'], str):
                        variant_obj ['attributes']['sale_price'] = round (float (variant_obj ['attributes']['sale_price']), 2)
                
                variant_obj ['attributes']['velo_sku_price'] = variant_obj ['attributes']['price']
                variant_obj ['attributes']['velo_sku_sale_price'] = variant_obj ['attributes']['sale_price']
        return record

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV6 ()
    logging.info ('RevisionV6 Finish...')


