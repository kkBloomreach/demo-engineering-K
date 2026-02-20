# V0 changes
# -- Version2.0 of PacificHome GLOBAL German catalog
#   - Use Pacifichome global ENGLISH catalog for category-path info
#   - for each record in old pacifihome global german catalog
#       - lookup corresponding english product -> category paths
#       - use that category_path, change leaf names from english -> german
#   - Note:
#       - if there is new english product that is not in old german catalog, 
#         it won't be included in new german catalog
#       - if there WAS a old german catalog product but not in new english catalog,
#         it won't be included in new german catalog

import logging
import random
import os
import copy
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV0 as rcv0

class RevisionV0 (RevisionBase) :

    _category_name_map = None

    def __init__ (self):
        logging.info ('Perform update, version v0')
        super().__init__ ()
        return

    def _initialize (self, english_source_records, german_source_records, inject_av_map):
        # name_map = [ {id: , germanName: }, ...]
        self._construct_category_name_map (german_source_records)
        if (self._category_name_map == None) or (len (self._category_name_map) == 0):
            return False
        return True
 
    # override base class method
    # This update class does not do any update to previous records
    def _perform_record_update (self, german_record):
        pid = german_record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)
        if (inject_av_record == None):
            logging.debug ('No inject attrib_value record for pid: %s', pid)

        # lookup corresponding english record
        english_record = super()._lookup_english_source_record (pid)
        if (english_record == None):
            logging.warning ('No english record for pid: %s' % pid)
            return None

        updated_record = self._perform_update_internal (german_record, english_record, inject_av_record)
        return updated_record

    def _finalize (self):
        return True # Place holder
 
    # INTERNAL METHODS
    def _perform_update_internal (self, german_record, english_record, inject_av_record):
        # check if product is to be deleted
        pid = german_record ['value']['attributes']['pid']

        updated_record = copy.deepcopy (german_record)

        # update category names
        english_category_paths = english_record ['value']['attributes']['category_paths']
        german_category_paths = copy.deepcopy (english_category_paths)

        # use category_name_map to get german name for category ids
        for branch in german_category_paths:
            for leaf in branch:
                cat_name_record = self._lookup_cat_name_record (leaf ['id'])
                if (cat_name_record == None):
                    logging.error ('Cannot find german name for category id: %s' % lead ['id'])
                    german_name = leaf ['name'] # use english name itself @@@ 
                else:
                    german_name = cat_name_record ['german_name']
                leaf ['name'] = german_name
        updated_record ['value']['attributes']['category_paths'] = german_category_paths

        # url change
        pdp_url = '%s%s___%s' % (uc.PRODUCT_URL_PREFIX, pid, pid)
        updated_record ['value']['attributes']['url'] = pdp_url

        # availability
        updated_record ['value']['attributes']['availability'] = english_record ['value']['attributes']['availability'] 

        return updated_record

    def _construct_category_name_map (self, german_source_records):
        self._category_name_map = []
        for record in german_source_records:
            category_paths = record ['value']['attributes']['category_paths']
            for branch in category_paths:
                for leaf in branch:
                    if self._lookup_cat_name_record (leaf ['id']) == None:  #if not already mapped
                        self._category_name_map.append ({ 'id': leaf ['id'],
                                                          'german_name': leaf ['name'] })


    def _lookup_cat_name_record (self, catId):
        for name_record in self._category_name_map:
            if name_record ['id'] == catId:
                return name_record
        return None


if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV0 ()
    logging.info ('RevisionV0 Finish...')

