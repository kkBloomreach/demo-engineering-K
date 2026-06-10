# current changes
# -- Change thumb and swatch image urls to match corresponding in Ph2 full english catalog
# -- Note: the 'original' german catalog (as of 2023) has a slightly different
# -- set of products (as compared to current ph2 english catalog). Therefore, to keep
# -- things simple, keep the 'original' german products (with translations etc)
# -- and change only the availability and image values 

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

    def _initialize (self, english_source_records, german_source_records, inject_av_map):
        return True
 
    # override base class method
    def _perform_record_update (self, german_record):
        pid = german_record ['value']['attributes']['pid']

        inject_av_record = super()._lookup_inject_av_record (pid)
        #if (inject_av_record == None):
        #    logging.debug ('No inject attrib_value record for pid: %s', pid)

        # lookup corresponding english record
        english_record = super()._lookup_english_source_record (pid)
        if (english_record == None):
            logging.warning ('No english record for pid: %s' % pid)
            return None

        updated_record = self._perform_update_internal (german_record, english_record, inject_av_record)
        return updated_record

    def _finalize (self, updated_products):
        return True
 
    # INTERNAL METHODS
    def _perform_update_internal (self, updated_record, english_record, inject_av_record):
        # check if product is to be deleted
        pid = updated_record ['value']['attributes']['pid']

        # update 'availability', 'thumb_image', 'swatch_image'
        updated_record ['value']['attributes']['availability'] =    \
                                    english_record ['value']['attributes']['availability']
        updated_record ['value']['attributes']['thumb_image'] = \
                                    english_record ['value']['attributes']['thumb_image']

        if ('variants' in updated_record ['value']) and (updated_record ['value']['variants']):
            updated_variant_list = updated_record ['value']['variants']
            english_variant_list = english_record ['value']['variants']
            for variant_id, updated_variant_obj in updated_variant_list.items():
                english_variant_obj = english_variant_list [variant_id]
                if (english_variant_obj != None):
                    updated_variant_obj ['attributes']['swatch_image'] =    \
                                        english_variant_obj ['attributes']['swatch_image']
                # else original swatch-img remains as-is -- this case should not occur
        return updated_record   # no change

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = Revision ()
    logging.info ('Revision Finish...')


