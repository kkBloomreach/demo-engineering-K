# V11 changes
# -- Set some products 'unavailable' because images cannot be generated due to openAI's
# -- 'rejected by safety system' error

import logging
import os
import copy
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV11 as rcv11

class RevisionV11 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v11')
        super().__init__ ()
        self._imageerror_pid_list = []    # 'pid' to set unavailable
        return

    def _initialize (self, source_records, inject_av_map):
        self._imageerror_pid_list = self._read_imageerror_products_list ()
        return True
 
    # override base class method
    # category_manager not needed in this revision
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']

        inject_av_record = super()._lookup_inject_av_record (pid)
        if (inject_av_record == None):
            logging.debug ('No inject attrib_value record for pid: %s', pid)

        updated_record = self._perform_update_internal (record, inject_av_record)
        return updated_record

    def _finalize (self, updated_products):
        return True
 
    # INTERNAL METHODS
    def _read_imageerror_products_list (self):
        imageerror_pid_list = []
        with open (rcv11.FILENAME_IMAGEERROR_PRODUCTS_TSV_IN, 'r') as input_file:
            dict_reader = csv.DictReader (input_file, delimiter = '\t')
            for row in dict_reader:
                # each row has pid
                pid = row ['pid'] 
                imageerror_pid_list.append (pid)
        return imageerror_pid_list

    def _perform_update_internal (self, record, inject_av_record):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']
        if pid in self._imageerror_pid_list:
            logging.debug ('setting availability = False for pid: %s' % pid)
            updated_record = copy.deepcopy (record)
            updated_record ['value']['attributes']['availability'] = False
            return updated_record
        else:
            return record   # no change

    def _is_pid_to_set_unavailabe (self, pid):
        for imageerror_pid in self._imageerror_pid_list:
            if imageerror_pid == pid:
                return True
        return False

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV11 ()
    logging.info ('RevisionV11 Finish...')


