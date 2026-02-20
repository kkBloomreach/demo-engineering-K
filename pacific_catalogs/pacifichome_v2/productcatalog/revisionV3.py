# V3 changes
# -- set product urls -> pacific.bloomreach.com/home

import logging
import random
import os
import copy
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV3 as rcv3

class RevisionV3 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v3')
        super().__init__ ()
        return

    def _initialize (self, source_records, category_manager, inject_av_map):
        return True
 
    # override base class method
    # category_manager not needed in this revision
    def _perform_record_update (self, record, category_manager):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)
        if (inject_av_record == None):
            logging.debug ('No inject attrib_value record for pid: %s', pid)

        updated_record = self._perform_update_internal (record, inject_av_record)
        return updated_record

    def _finalize (self):
        return True # Place holder
 
    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']

        updated_record = copy.deepcopy (record)

        # url
        pdp_url = '%s%s___%s' % (uc.PRODUCT_URL_PREFIX, pid, pid)
        updated_record ['value']['attributes']['url'] = pdp_url
        return updated_record


if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV3 ()
    logging.info ('RevisionV3 Finish...')


