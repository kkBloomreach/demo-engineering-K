# V0 changes
# -- Primary purpose of 'global' catalog is the 'german' version. This 'english'
# -- version is intermediate step. This english catalog itself is never really used 

# -- VersionV0 of PacificSupply GLOBAL English catalog
#   - starting from full PS english, reduce number of products
#   - use a pre-defined set of categories to reduce catalog size
#   - add selected more pids so that 'numeric-precision' feature 'works'
#   - remove views, select one of the view's product-price as regular-price
#   - reduce number of attributes


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
        if os.path.exists (rcv0.FILENAME_SELECTED_CATIDLIST_TSV_IN):
            with open (rcv0.FILENAME_SELECTED_CATIDLIST_TSV_IN, 'r') as input:
                tsv_reader = csv.DictReader (input, delimiter = '\t')
                for row in tsv_reader:
                    selected_catinfo_list.append (row) # {L1, L2}
                input.close ()
        else:
            logging.error ('Cannot read selected catinfo list')
            return False

        # next, use execApiCall to collect products from the selected catid's
        # IMPORTANTG: running this exec call on different days may return 
        # different pids. Therefore the generated catalog and the catid-name-map
        # will be different
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

        # read additional-pid-list (~280)
        if os.path.exists (rcv0.FILENAME_ADDITIONAL_PIDLIST_TSV_IN):
            with open (rcv0.FILENAME_ADDITIONAL_PIDLIST_TSV_IN, 'r') as input:
                tsv_reader = csv.DictReader (input, delimiter = '\t')
                for row in tsv_reader:
                    if row ['pid'] not in self._selected_pid_list:
                        self._selected_pid_list.append (row ['pid'])
                input.close ()
        else:
            logging.error ('Cannot read additional pid list')
            return False
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
        catid_name_map = []
        # prepare catid-name map. Needed for german translation of cat names
        for product in updated_products:
            category_paths = product ['value']['attributes']['category_paths']
            for branch in category_paths:
                for leaf in branch:
                    cat_info = { 'catid': leaf ['id'],
                                 'catname_en': leaf ['name'],
                                 'catname_de': ''    # populated during german translation via google translate
                               }
                    catid_name_map.append (cat_info)
        # save to local output
        with open (rcv0.FILENAME_CATID_NAME_MAP_TSV_OUT, 'w') as file_output:
            tsvWriter = csv.writer (file_output, delimiter = '\t')
            headerLine = catid_name_map [0].keys ()
            tsvWriter.writerow (headerLine)
            for row in catid_name_map:
                tsvWriter.writerow (row.values())
            file_output.close ()

        return updated_products
 
    # INTERNAL METHODS
    def _perform_update_internal (self, record):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']

        if str (pid) not in self._selected_pid_list:
            return None # exclude this product

        simplified_record = copy.deepcopy (record)

        # pick price/sale-price from one of the views in original product record
        default_price, default_sale_price = self._lookup_default_price_and_sale_price (record)
        simplified_record ['value']['attributes']['price'] = default_price
        simplified_record ['value']['attributes']['sale_price'] = default_sale_price
        if default_price == 0.0:
            simplified_record ['value']['attributes']['availability'] = False

        # url change
        pdp_url = '%s%s___%s' % (uc.PRODUCT_URL_PREFIX, pid, pid)
        simplified_record ['value']['attributes']['url'] = pdp_url

        # del 'views' attribute
        if 'views' in simplified_record ['value']:
            del simplified_record ['value']['views']

        # include only the selected-attributes
        for attrib in record ['value']['attributes']:
            if attrib in rcv0.SELECTED_ATTRIBUTES_LIST:
                continue
            else:
                try:
                    del (simplified_record ['value']['attributes'][attrib])
                except Exception as e:
                    continue    # dup delattr will cause exception
        return simplified_record

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

