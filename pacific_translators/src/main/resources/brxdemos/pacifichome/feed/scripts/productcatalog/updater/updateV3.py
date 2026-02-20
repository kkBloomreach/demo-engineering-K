# V3 changes
# -- remove all "food*" products (except the ones identified as 'to-keep'
# -- define new collection "Pacific Food" (not category, a collection)
# -- all food item brand => PacMart
import logging
import copy
import csv

from updateBase import UpdateBase
import updaterConstants as uc
import updaterConstantsV3 as ucv3

class UpdateV3 (UpdateBase) :
    def __init__ (self):
        logging.info ('Perform update, version v3')
        super().__init__ ()

        # read list of food products to keep
        self._food_products_to_keep = self._read_food_products_to_keep ()
        return

    # override base class method
    def _perform_record_update (self, record):
        updated_record = self._perform_update_internal (record)
        return updated_record

    def _perform_update_internal (self, record):
        pid = record ['value']['attributes']['pid']
        logging.debug ('process pid = %s', pid)

        updated_record = copy.deepcopy  (record)

        if (self._is_product_in_food_category (record) == False):
            return updated_record

        # if product in 'food' category and NOT in product-to-keep list, set availability = false 
        if pid in self._food_products_to_keep:
            updated_record ['value']['attributes']['collection'] = 'Pacific Food'
            updated_record ['value']['attributes']['availability'] = True
            return updated_record
        else:
            logging.debug ('Forcing pid to be unavailable, pid = %s' % pid)
            updated_record ['value']['attributes']['availability'] = False
        return updated_record

    def _read_food_products_to_keep (self):
        pids_to_keep_list = []
        with open (ucv3.FILENAME_FOOD_PRODUCTS_TO_KEEP_TSV_IN, 'r') as urllist_file:
            dict_reader = csv.DictReader (urllist_file, delimiter = '\t')
            for row in dict_reader:
                url = row ['product URL']
                # parse url to extract pid
                # url format: https://..../products/<pid>___<sku>
                rindx = url.rindex ('/')
                tail = url [rindx+1:]
                uscore_indx = tail.find ('_')
                pid = tail [:uscore_indx]
                logging.debug ('pid to keep: %s' % pid)

                pids_to_keep_list.append (pid)
            urllist_file.close ()
        return pids_to_keep_list

    def _is_product_in_food_category (self, record):
        category_paths = record ['value']['attributes']['category_paths']
        for branch in category_paths:
            for node in branch:
                if (node ['id'] == '162') or (node ['id'] == '116732'): # Food/Drink or Food & Drink
                    return True
        return False

if __name__ == '__main__':
    u = UpdateV3 ()



