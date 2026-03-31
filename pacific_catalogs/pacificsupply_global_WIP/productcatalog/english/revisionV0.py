# V0 changes
# -- VersionV0 of PacificSupply GLOBAL English catalog
#   - starting from full PS english, reduce number of products
#   - use a pre-defined set of categories to reduce catalog size

import logging
import os
import copy
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV0 as rcv0
import execAPICall as epc

class RevisionV0 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v0')
        super().__init__ ()
        self._selected_pid_list = None
        return

    def _initialize (self, source_records, inject_av_map):
        selected_catinfo_list = []
        # read selected catinfo list 
        if os.path.exists (rcv0.FILENAME_SELECTED_CATIDLIST_IN):
            with open (rcv0.FILENAME_SELECTED_CATIDLIST_IN, 'r') as input:
                tsv_reader = csv.DictReader (input, delimiter = '\t')
                for row in tsv_reader:
                    selected_catinfo_list.append (row) # {L1, L2}
                input.close ()
        else:
            logging.error ('Cannot read selected catinfo list')
            return False

        # next, use execApiCall to collect products from the selected catid's
        self._selected_pid_list = []
        api_executor = epc.ExecAPICall ()
        for catinfo in selected_catinfo_list:
            catid = catinfo ['L2 catId']
            qterm = catinfo ['L2 Category']
            pid_list_in_cat = api_executor.getPidList (qterm, catid)
            if pid_list_in_cat != None and len (pid_list_in_cat) > 0:
                for pid in pid_list_in_cat:
                    if pid not in self._selected_pid_list:
                        self._selected_pid_list.append (str (pid))
        logging.debug ('Selected pid count: %s' % len (self._selected_pid_list))
        return True

    # override base class method
    # This update class does not do any update to previous records except url
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        # inject_av_record = super()._lookup_inject_av_record (pid)
        #if (inject_av_record == None):
        #    logging.debug ('No inject attrib_value record for pid: %s', pid)

        updated_record = self._perform_update_internal (record)
        return updated_record

    def _finalize (self, updated_products):
        return updated_products
 
    # INTERNAL METHODS
    def _perform_update_internal (self, record):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']

        if str (pid) not in self._selected_pid_list:
            return None # exclude this product

        updated_record = copy.deepcopy (record)

        # url change
        pdp_url = '%s%s___%s' % (uc.PRODUCT_URL_PREFIX, pid, pid)
        updated_record ['value']['attributes']['url'] = pdp_url

        return updated_record

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV0 ()
    logging.info ('RevisionV0 Finish...')

'''
        default_price, default_sale_price = self._lookup_default_price_and_sale_price (record)
        updated_record ['value']['attributes']['price'] = default_price
        updated_record ['value']['attributes']['sale_price'] = default_sale_price
        # del 'views' attribute
        if 'views' in updated_record ['value']:
            del updated_record ['value']['views']

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

'''
