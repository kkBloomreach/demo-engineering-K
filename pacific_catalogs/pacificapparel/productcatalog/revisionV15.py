# V15 changes
# -- Change title, description of specific products
# -- Use injected-av-map-edited to make those changes

import logging
import copy

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV15 as rcv15
import categorybuilder as cb

class RevisionV15 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v15')
        super().__init__ ()
        return

    def _initialize (self, source_records, inject_av_map):
        return True

    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)

        if (inject_av_record == None):
            logging.warning ('No inject attrib_value record for pid: %s', pid)
            return None

        updated_record = self._perform_update_internal (record, inject_av_record)
        return updated_record

    # generate aws upload script (to be executed separately)
    def _finalize (self, updated_products):
        return True

    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        pid = record ['value']['attributes']['pid']
        updated_record = copy.deepcopy (record)

        # update previous record - 
        # title 
        updated_record ['value']['attributes']['title'] = inject_av_record ['edited_title']

        # description
        updated_record ['value']['attributes']['description'] = inject_av_record ['edited_description']

        return updated_record

if __name__ == '__main__':
    rv = RevisionV15 ()


