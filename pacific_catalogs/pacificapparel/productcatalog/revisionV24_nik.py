# V24_nik changes
# -- add 'sku_on_sale' attribute if sku is on sale

import logging
import copy
import os
import csv
import random

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV24 as rcv24

class RevisionV24_Nik (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v24_nik')
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

        # add sku_on_sale attribute (true/false) 
        updated_record = self._perform_update_internal (record, inject_av_record)
        return updated_record

    def _finalize (self, updated_products):
        return True

    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        pid = record ['value']['attributes']['pid']
        updated_record = copy.deepcopy (record)

        if updated_record ['value']['attributes']['onSale'] == True:
            pid_on_sale = True
        else:
            pid_on_sale = False

        # no variant has its price > product_price
        variant_max_price = updated_record ['value']['attributes']['price']

        if ('variants' in updated_record ['value']) and (updated_record ['value']['variants']):
            variant_list = updated_record ['value']['variants']
            variant_count = len (variant_list)
        else:
            return updated_record   # product has no variants (should not happen)

        logging.debug ('----')
        logging.debug ('pid %s, onSale = %s, variant_count:%s' % (pid, pid_on_sale, variant_count))

        # first find and update 'default_sku' object, values same as product price/sale-price
        # Note: it may not be the 0th variant in the variant list
        for variant_id in variant_list.keys():
            variant_obj = variant_list [variant_id]
            # default_variant price/sale-price/onSale == product price/sale-price/onSale
            if variant_obj ['attributes']['default_sku'] == True:
                variant_obj ['attributes']['price'] = updated_record ['value']['attributes']['price']
                variant_obj ['attributes']['sale_price'] = updated_record ['value']['attributes']['sale_price']
                variant_obj ['attributes']['sku_on_sale'] = pid_on_sale
                logging.debug ('\tdefault variant_id: %s, variant_price: %s, variant_sale_price: %s, variant_on_sale: %s' % 
                               (variant_id, 
                                updated_record ['value']['attributes']['price'], 
                                updated_record ['value']['attributes']['sale_price'], 
                                pid_on_sale)) 

        # for some (40%) of products that have onSale = True, some of its variants can be further on sale
        # Note: multiple variants of a product can be on sale (not just one)
        # Note: if pid.onSale = False, NONE of its variants are on sale (just keeps the logic simple to explain)
        total_count_variants_on_sale = 0
        if pid_on_sale == True:
            rand = int (random.random () * 100)
            if rand < 40:   # this product has discounted variants
                total_count_variants_on_sale = int (random.random () * variant_count) # number of variants on sale, [0 -> N)

        # now, of the remaining variants (excluding default), zero or more can be further on-sale
        current_variant_num_on_sale = 0
        for variant_id in variant_list.keys():
            variant_obj = variant_list [variant_id]
            # default_variant already processed above
            if variant_obj ['attributes']['default_sku'] == True:
                continue 

            discount_factor = 1.0
            variant_on_sale = False
            if current_variant_num_on_sale < total_count_variants_on_sale:
                discount_factor = 1 - ((random.random () * 0.20) + 0.15) # at least 15% off
                variant_on_sale = True
                current_variant_num_on_sale = current_variant_num_on_sale + 1

            this_variant_price = round (variant_max_price, 2)
            this_variant_sale_price = round (this_variant_price * discount_factor, 2)

            logging.debug ('\ttotal_count_variants_on_sale: %s, current_variant_num_on_sale: %s, variant_id: %s, variant_price: %s, variant_sale_price: %s, variant_on_sale: %s' % 
                            (total_count_variants_on_sale, current_variant_num_on_sale, variant_id, this_variant_price, this_variant_sale_price, variant_on_sale)) 
            variant_obj ['attributes']['price'] = this_variant_price
            variant_obj ['attributes']['sale_price'] = this_variant_sale_price
            variant_obj ['attributes']['sku_on_sale'] = variant_on_sale

        return updated_record

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV24_Nik ()
    logging.info ('RevisionV24_Nik finish...')


