# V36 changes
# -- Update availability, style, brand, title, description based on edited injected_av_map

import logging
import copy
import os

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV36 as rcv36
import categorybuilder as cb

class RevisionV36 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v36')
        super().__init__ ()
        return

    def _initialize (self, source_records, inject_av_map):
        return True

    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)
        if (inject_av_record == None):
            logging.debug ('No inject attrib_value record for pid: %s', pid)

        updated_record = self._perform_update_internal (record, inject_av_record)
        return updated_record

    # 
    def _finalize (self, updated_products):
        return updated_products

    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        pid = record ['value']['attributes']['pid']
        updated_record = copy.deepcopy (record)

        # update availability as per inject_av_record
        if str (inject_av_record ['edited_availability_march2026']) == str ('False'):
            inject_availability = False
        else:
            inject_availability = True
        if str (inject_availability) != str (updated_record ['value']['attributes']['availability']):
            logging.debug ('Availability changed for pid: %s' % pid)
        updated_record ['value']['attributes']['availability'] = inject_availability

        # update style as per inject_av_record
        inject_style = inject_av_record ['edited_style_march2026']
        if str (inject_style) != str (updated_record ['value']['attributes']['style']):
            logging.debug ('Style changed for pid: %s' % pid)
        updated_record ['value']['attributes']['style'] = inject_style

        # update brand as per inject_av_record
        inject_brand = inject_av_record ['edited_brand_march2026']
        if str (inject_brand) != str (updated_record ['value']['attributes']['brand']):
            logging.debug ('Brand changed for pid: %s' % pid)
        updated_record ['value']['attributes']['brand'] = inject_brand 

        # update title as per inject_av_record
        inject_title = inject_av_record ['edited_title_march2026']
        if str (inject_title) != str (updated_record ['value']['attributes']['title']):
            logging.debug ('Title changed for pid: %s' % pid)
        updated_record ['value']['attributes']['title'] = inject_title

        # update description as per inject_av_record
        inject_description = inject_av_record ['edited_description_march2026']
        if str (inject_description) != str (updated_record ['value']['attributes']['description']):
            logging.debug ('Description changed for pid: %s' % pid)
        updated_record ['value']['attributes']['description'] = inject_description

        return updated_record

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV36 ()
    logging.info ('RevisionV36 finish...')



