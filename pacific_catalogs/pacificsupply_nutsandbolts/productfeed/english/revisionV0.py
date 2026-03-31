# V0 changes
# -- VersionV0 - start from PacificSupply GLOBAL English catalog
#   - remove views
#   - reduce original attributes to a small subset

import logging
import os
import copy
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV0 as rcv0

class RevisionV0 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v0')
        super().__init__ ()
        return

    def _initialize (self, source_records, inject_av_map):
        return True

    # override base class method
    # This update class does not do any update to previous records except url
    def _perform_record_update (self, record):
        updated_record = self._perform_update_internal (record)
        return updated_record

    def _finalize (self, updated_products):
        return updated_products
 
    # INTERNAL METHODS
    def _perform_update_internal (self, record):
        pid = record ['value']['attributes']['pid']

        updated_record = copy.deepcopy (record)

        # pick up price/sale_price from one of the views
        default_price, default_sale_price = self._lookup_default_price_and_sale_price (record)
        updated_record ['value']['attributes']['price'] = default_price
        updated_record ['value']['attributes']['sale_price'] = default_sale_price

        # url change
        pdp_url = '%s%s___%s' % (uc.PRODUCT_URL_PREFIX, pid, pid)
        updated_record ['value']['attributes']['url'] = pdp_url

        # del 'views' attribute
        if 'views' in updated_record ['value']:
            del updated_record ['value']['views']

        # reduce attributes to selected set
        for attrib in record ['value']['attributes']:
            if attrib in rcv0.SAMPLE_NAB_PRODUCT_ATTRIBUTES:
                continue
            else:
                del updated_record ['value']['attributes'][attrib]

        return updated_record

    # pick the first 'view' object and use that to return price, sale_price
    def _lookup_default_price_and_sale_price (self, record):
        default_price = 0.0
        default_sale_price = 0.0

        # if product has no associated views (ie, part of only 'master' view)
        if 'price' in record ['value']['attributes']:
            default_price = record ['value']['attributes']['price']
        if 'sale_price' in record ['value']['attributes']:
            default_sale_price =  record ['value']['attributes']['sale_price']

        if 'views' in record ['value']:
            for view_name, view_object in record ['value']['views'].items ():
                if 'price' in view_object ['attributes']:
                    default_price = view_object ['attributes']['price']
                if 'sale_price' in view_object ['attributes']:
                    default_sale_price = view_object ['attributes']['sale_price']
                if default_price != 0.0:
                    if default_sale_price == 0.0:
                        default_sale_price = default_price
                    return default_price, default_sale_price
        logging.warning ('Cannot find default price, sale_price for pid: %s' % record ['value']['attributes']['pid'])
        return (default_price, default_sale_price)

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV0 ()
    logging.info ('RevisionV0 Finish...')

