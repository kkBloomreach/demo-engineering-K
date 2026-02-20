# V1 changes
# -- Change thumb and swatch image urls to match corresponding in Ph2 full english catalog
# -- for ph2-global-english catalog, merely copy full english catalog

import logging
import os
import copy
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV1 as rcv1

class RevisionV1 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v1')
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
        return True
 
    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']

        updated_record = copy.deepcopy (record)

        return record   # no change

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV1 ()
    logging.info ('RevisionV1 Finish...')


