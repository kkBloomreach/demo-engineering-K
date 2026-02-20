# V1 changes
# -- Delete list of pids (availability = false)

import logging
import random
import os
import copy
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV1 as rcv1

class RevisionV1 (RevisionBase) :
    # delete pid list {multiple columns}
    _pids_to_delete = []

    def __init__ (self):
        logging.info ('Perform update, version v1')
        super().__init__ ()
        return

    def _initialize (self, source_records, category_manager, inject_av_map):
        # read categorystatus map 
        self._pids_to_delete = self._read_pids_to_delete ()

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
    # read pid_to_delete list
    def _read_pids_to_delete (self):
        _pids_to_delete = []

        with open (rcv1.FILENAME_PIDS_TO_DELETE_TSV_IN, 'r') as pids_to_delete_file:
            tsv_reader = csv.DictReader (pids_to_delete_file, delimiter = '\t')
            for row in tsv_reader:
                # .tsv has multiple columns. Following columns have actual pids to be deleted
                cols_with_pids = ['pid1', 'pid2', 'pid3', 'pid4', 'pid5', 'pid6', 'pid7', 'pid8', 'pid9', 'pid10']
                for col in cols_with_pids:
                    pid = row [col]
                    if pid != None and pid != '':
                        if pid not in _pids_to_delete:  # de-dup
                            _pids_to_delete.append (pid)
            pids_to_delete_file.close ()
        logging.info ('Deleted pid count: %s' % len (_pids_to_delete))
        return _pids_to_delete

    def _perform_update_internal (self, record, inject_av_record):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']

        updated_record = copy.deepcopy (record)
        if pid in self._pids_to_delete:
            if updated_record ['value']['attributes']['availability'] == True:
                updated_record ['value']['attributes']['availability'] = False
                logging.debug ('setting pid unavailable: %s' % pid)

        return updated_record


if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV1 ()
    logging.info ('RevisionV1 Finish...')


