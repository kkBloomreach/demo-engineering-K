# V31 changes
# -- replace image urls->variants_new -> url->variants
# -- assumes we have already copied actual images (in aws) from variants_new -> variants folder

import logging
import copy
import os
import csv
import random

from revisionBase import RevisionBase
from imageloader import ImageLoader
import updaterConstants as uc
import revisionConstantsV31 as rcv31

class RevisionV31 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v31')
        super().__init__ ()
        return

    def _initialize (self, source_records, inject_av_map):
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

        # need to generate image for default-variant and then other
        updated_record = copy.deepcopy (record)

        thumb_img_url = updated_record ['value']['attributes']['thumb_image']
        if 'variants_new' in thumb_img_url:
            upd_thumb_img_url = thumb_img_url.replace ('variants_new', 'variants')
            updated_record ['value']['attributes']['thumb_image'] = upd_thumb_img_url

            if ('variants' in record ['value']) and (record ['value']['variants']):
                variant_list = record ['value']['variants']
                for variant_id in variant_list.keys ():
                    variant_obj = variant_list [variant_id]
                    variant_img_url = variant_obj ['attributes']['swatch_image']
                    upd_variant_img_url = variant_img_url.replace ('variants_new', 'variants')
                    updated_record ['value']['variants'][variant_id]['attributes']['swatch_image'] = upd_variant_img_url
                    updated_record ['value']['variants'][variant_id]['attributes']['thumb_image'] = upd_variant_img_url
        # else - img urls remain unchanged
        return updated_record

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV31 ()
    logging.info ('RevisionV31 finish...')


