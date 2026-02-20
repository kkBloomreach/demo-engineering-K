# V10 changes
# -- remove 'shirt' term from specific product titles 

import logging
import random
import os
import copy
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV10 as rcv10
from categorybuilder import CategoryBuilder

class RevisionV10 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v10')
        super().__init__ ()
        return

    def _initialize (self, source_records, inject_av_map):
        return True

    # override base class method
    # This update class does not do any update to previous records
    def _perform_record_update (self, record, gen_image_map):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)

        if (inject_av_record == None):
            logging.warning ('No inject attrib_value record for pid: %s', pid)
            return record

        updated_record = self._perform_update_internal (record, inject_av_record)
        return updated_record

    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']

        updated_record = copy.deepcopy (record)

        # title for specific products
        title = updated_record ['value']['attributes']['title']
        if pid == '110722':
            title = 'Point Collar Denim Dress'
        elif pid == '110729':
            title = 'Mirror Embellished Long Sleeved Dress'
        elif pid == '110730':
            title = 'Printed Long Dress with Windy Flowers'
        elif pid == '110737':
            title = 'Short Sleeve Polo Tennis Dress'
        elif pid == '110748':
            title = 'Casual Ruffled Dress'

        updated_record ['value']['attributes']['title'] = title
        return updated_record

if __name__ == '__main__':
    rv = RevisionV10 ()

