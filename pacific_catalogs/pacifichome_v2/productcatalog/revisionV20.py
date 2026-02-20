# V20 changes
# -- adjust titles. 
# -- Some titles messed up after removing color-strings in them
import logging
import copy
import os
import csv
import random

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV20 as rcv20

class RevisionV20 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v20')
        super().__init__ ()

        # {"color, value}
        self._adjust_titles_list = []

        # list of product titles (used to remove duplicate products)
        self._product_titles = []
        return

    def _initialize (self, source_records, inject_av_map):
        # read adjust-titles list
        self._adjust_titles_list = self._read_adjust_titles_list ()
        if (self._adjust_titles_list == None):
            logging.debug ("No titles to adjust")
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
        pid = record ['value']['attributes']['pid']

        current_title = record ['value']['attributes']['title']

        # check if title needs to be changed
        new_title = self._lookup_fixed_title_if_any (pid)
        if new_title == None or new_title == '':
            new_title = current_title # no change
        else:
            logging.debug ('adjusting title for pid: %s' % (pid))

        # if duplicate title, exclude product from output catalog 
        if new_title.lower() in self._product_titles:
            logging.debug ('Excluding pid = %s due to duplicate title' % (pid))
            return None
        self._product_titles.append (new_title.lower ())

        updated_record = copy.deepcopy (record)
        updated_record ['value']['attributes']['title'] = new_title.strip ()
        return updated_record

    # list of color names (collected from titles). These are removed in the output
    def _read_adjust_titles_list (self):
        logging.info ("reading source: %s" % rcv20.FILENAME_ADJUST_TITLES_TSV_IN)
        adjust_titles = []
        if os.path.exists (rcv20.FILENAME_ADJUST_TITLES_TSV_IN):
            with open (rcv20.FILENAME_ADJUST_TITLES_TSV_IN, 'r') as file_obj:
                dict_reader = csv.DictReader (file_obj, delimiter='\t')
                for row in dict_reader:
                    adjust_titles.append (row)
                file_obj.close ()
            logging.info ('adjust record count: %s' % len (adjust_titles))
        else:
            logging.error ('cannot find source: %s' % rcv20.FILENAME_ADJUST_TITLES_TSV_IN)
            adjust_titles = None
        return adjust_titles

    def _lookup_fixed_title_if_any (self, pid):
        for adjust_record in self._adjust_titles_list:
            if adjust_record ['pid'] == str (pid):
                return adjust_record ['fixed_title']
        return None

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV20()
    logging.info ('RevisionV20finish...')


