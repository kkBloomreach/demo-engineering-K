# V21 changes
# -- No change in this revision itself. Base class modified to support datahub

import logging
import copy
import os
import csv
import random

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV21 as rcv21

class RevisionV21 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v21')
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

        # in this revision, no change to input record
        return record

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV21()
    logging.info ('RevisionV21finish...')


