# current changes
# -- set availability=false for products wit broken/non-existant image

import logging
import os
import copy
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstants as rc

class Revision (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update')
        super().__init__ ()
        return

    def _initialize (self, source_records, inject_av_map):
        self._broken_image_products = self._read_broken_image_product_list ()
        return True

    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']

        updated_record = self._perform_update_internal (record)
        return updated_record

    # generate aws upload script (to be executed separately)
    def _finalize (self, updated_products):
        return True

    # INTERNAL METHODS
    def _perform_update_internal (self, record):
        pid = record ['value']['attributes']['pid']
        updated_record = copy.deepcopy (record)

        if self._pid_has_broken_image (pid):
            updated_record ['value']['attributes']['availability'] = False

        return updated_record

    def _read_broken_image_product_list (self):
        _broken_image_products = []

        with open (rc.FILENAME_BROKEN_IMAGES_LIST_TSV_IN, 'r') as list_file:
            tsv_reader = csv.DictReader (list_file, delimiter = '\t')
            for row in tsv_reader:
                _broken_image_products.append (row)
            list_file.close ()
        return _broken_image_products

    def _pid_has_broken_image (self, pid):
        for broken_image_record in self._broken_image_products:
            if broken_image_record ['pid'] == pid:
                return True
        return False


if __name__ == '__main__':
    rv = Revision ()

