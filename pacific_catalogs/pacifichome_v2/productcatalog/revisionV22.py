# V22 changes
# -- set all prices to xxx.00 (ie, zero-cents)

import logging
import copy
import os
import csv
import random

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV22 as rcv22

class RevisionV22 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v22')
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

        updated_record = self._perform_update_internal (record, inject_av_record)
        return updated_record

    def _finalize (self, updated_products):
        return updated_products

    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        pid = record ['value']['attributes']['pid']

        updated_record = copy.deepcopy (record)
        price = updated_record ['value']['attributes']['price']
        updated_record ['value']['attributes']['price'] = round (price, 0)

        if 'sale_price' in updated_record ['value']['attributes']:
            sale_price = updated_record ['value']['attributes']['sale_price']
            updated_record ['value']['attributes']['sale_price'] = round (sale_price, 0)

        if 'sale_price_range_min' in updated_record ['value']['attributes']:
            sale_price_range_min = updated_record ['value']['attributes']['sale_price_range_min']
            updated_record ['value']['attributes']['sale_price_range_min'] = round (sale_price_range_min, 0)

        if 'sale_price_range_max' in updated_record ['value']['attributes']:
            sale_price_range_max = updated_record ['value']['attributes']['sale_price_range_max']
            updated_record ['value']['attributes']['sale_price_range_max'] = round (sale_price_range_max, 0)

        if ('variants' in updated_record ['value']) and (updated_record ['value']['variants']):
            variant_list = updated_record ['value']['variants']
            for variant_id, variant_obj in variant_list.items():
                if 'price' in variant_obj ['attributes']:
                    price = variant_obj ['attributes']['price']
                    variant_obj ['attributes']['price'] = round (price, 0)

                if 'sale_price' in variant_obj ['attributes']:
                    sale_price = variant_obj ['attributes']['sale_price']
                    variant_obj ['attributes']['sale_price'] = round (sale_price, 0)

                if 'velo_sku_price' in variant_obj ['attributes']:
                    velo_sku_price = variant_obj ['attributes']['velo_sku_price']
                    variant_obj ['attributes']['velo_sku_price'] = round (velo_sku_price, 0)

                if 'velo_sku_sale_price' in variant_obj ['attributes']:
                    velo_sku_sale_price = variant_obj ['attributes']['velo_sku_sale_price']
                    variant_obj ['attributes']['velo_sku_sale_price'] = round (velo_sku_sale_price, 0)

        return updated_record

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV22()
    logging.info ('RevisionV22finish...')


