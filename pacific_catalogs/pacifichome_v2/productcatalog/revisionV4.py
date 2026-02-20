# V4 changes
# -- set image urls to use webp format

import logging
import random
import os
import copy
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV4 as rcv4

class RevisionV4 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v4')
        super().__init__ ()
        return

    def _initialize (self, source_records, inject_av_map):
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
        return True # Place holder
 
    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']

        updated_record = copy.deepcopy (record)

        # replace image extensions to .webp
        self._change_image_url_to_webp (updated_record)

        return updated_record

    def _change_image_url_to_webp (self, updated_record):
        product_attribs = updated_record ['value']['attributes']
        # product image
        if 'thumb_image' in product_attribs:
            img_url = product_attribs ['thumb_image']
            img_url = img_url.replace ('/images/gen/', '/images/gen/webp/')
            if '.jpg' in img_url:
                img_url = img_url.replace ('.jpg', '.webp')
            elif '.png' in img_url:
                img_url = img_url.replace ('.png', '.webp')
            else:
                logging.warning ('Unknown thumb_image extension for pid: %s' % product_attribs ['pid'])
            product_attribs ['thumb_image'] = img_url

        if 'large_image' in product_attribs:
            img_url = product_attribs ['large_image']
            img_url = img_url.replace ('/images/gen/', '/images/gen/webp/')
            if '.jpg' in img_url:
                img_url = img_url.replace ('.jpg', '.webp')
            elif '.png' in img_url:
                img_url = img_url.replace ('.png', '.webp')
            else:
                logging.warning ('Unknown large_image extension for pid: %s' % product_attribs ['pid'])
            product_attribs ['large_image'] = img_url

        # variant images if any
        if 'variants' in updated_record ['value']:
            variant_list = updated_record ['value']['variants']
            for variant_id, variant_obj in variant_list.items():
                variant_attribs = variant_obj ['attributes']
                if 'swatch_image' in variant_attribs:
                    img_url = variant_attribs ['swatch_image']
                    img_url = img_url.replace ('/images/gen/', '/images/gen/webp/')
                    if '.jpg' in img_url:
                        img_url = img_url.replace ('.jpg', '.webp')
                    elif '.png' in img_url:
                        img_url = img_url.replace ('.png', '.webp')
                    else:
                        logging.warning ('Unknown thumb_image extension for pid: %s' % product_attribs ['pid'])
                variant_attribs ['swatch_image'] = img_url
        return

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV4 ()
    logging.info ('RevisionV4 Finish...')


