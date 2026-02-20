# V19 changes
# -- delete specific pids because their images are not looking good

import logging
import copy
import os
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV19 as rcv19
import categorybuilder as cb

class RevisionV19 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v19')
        super().__init__ ()
        return

    def _initialize (self, source_records, inject_av_map):
        self._pids_to_set_unavailable = self._read_pids_to_set_unavailable ()
        if ((self._pids_to_set_unavailable == None) or (len (self._pids_to_set_unavailable) == 0)):
            return False

        return True

    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)

        if (inject_av_record == None):
            logging.debug ('No inject attrib_value record for pid: %s', pid)

        updated_record = self._perform_update_internal (record, inject_av_record)
        return updated_record

    # generate aws upload script (to be executed separately)
    def _finalize (self, updated_products):
        return True

    # INTERNAL METHODS
    # read list of image urls where image needs to be replaced
    def _read_pids_to_set_unavailable (self):
        pids_to_set_unavailable = []
        if (os.path.exists (rcv19.FILENAME_PIDS_TO_SET_UNAVAILABLE_LIST_TSV_IN)):
            with open (rcv19.FILENAME_PIDS_TO_SET_UNAVAILABLE_LIST_TSV_IN, 'r') as input_file:
                dict_reader = csv.DictReader (input_file, delimiter='\t')
                for row in dict_reader:
                    url = row ['pdp_url']
                    if (len (url) == 0):
                        continue

                    if url.startswith ('#'):    # skip lines starting with #
                        continue

                    # line is url, extract pid from it: .../<pid>___<sku>_0
                    indx = url.rindex ('/')
                    tail = url [indx+1:]
                    indx = tail.index ('_')
                    pid = tail [:indx]
                    pids_to_set_unavailable.append (pid)
                input_file.close ()
        return pids_to_set_unavailable

    def _perform_update_internal (self, record, inject_av_record):
        pid = record ['value']['attributes']['pid']
        if pid in self._pids_to_set_unavailable:
            logging.debug ('Setting availability = false for pid: %s' % pid)
            updated_record = copy.deepcopy (record)
            updated_record ['value']['attributes']['availability'] = False
            return updated_record
        return record # no change

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV19 ()
    logging.info ('RevisionV19 finish...')


