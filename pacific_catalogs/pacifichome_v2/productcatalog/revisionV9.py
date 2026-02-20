# V9 changes
# -- Set some products 'unavailable' because images generated via openai are not "good"

import logging
import os
import copy
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV9 as rcv9

class RevisionV9 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v9')
        super().__init__ ()
        self._unavailable_pid_list = []    # 'pid' to set unavailable
        return

    def _initialize (self, source_records, inject_av_map):
        self._unavailable_pid_list = self._read_unavailable_products_list ()
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
    def _read_unavailable_products_list (self):
        unavailable_pid_list = []
        with open (rcv9.FILENAME_UNAVAILABLE_PRODUCTS_TSV_IN, 'r') as input_file:
            dict_reader = csv.DictReader (input_file, delimiter = '\t')
            for row in dict_reader:
                # each row has product url, extract pid
                url = row ['UnavailableProductUrl'] # format: .../pid_sku
                rindx = url.rindex ('/')
                if rindx > 0:
                    tail = url [rindx+1:]
                    indx = tail.index ('_')
                    pid = tail [:indx]
                    unavailable_pid_list.append (pid)
        return unavailable_pid_list

    def _perform_update_internal (self, record, inject_av_record):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']
        if pid in self._unavailable_pid_list:
            logging.debug ('setting availability = False for pid: %s' % pid)
            updated_record = copy.deepcopy (record)
            updated_record ['value']['attributes']['availability'] = False
            return updated_record
        else:
            return record   # no change

    def _is_pid_to_set_unavailabe (self, pid):
        for unavailable_pid in self._unavailable_pid_list:
            if unavailable_pid == pid:
                return True
        return False

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV9 ()
    logging.info ('RevisionV9 Finish...')


