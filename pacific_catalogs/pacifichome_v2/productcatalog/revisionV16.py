# V16 changes
# -- In a previous version, we had created variants (and their images) for livingroom ('116715>116754') based on SC request
# -- SCs now don't want those. Therefore, for products in '116715>116754', 
# -- 1. keep only one variant (as it was much earlier)
# -- 2. set that as default_sku
# -- 3. change swatch, thumb, large image url to "..../gen_gptimage_1/webp/<variant_id>_image.webp" (as it was much earlier)

import logging
import copy
import os

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV16 as rcv16

SELECT_CATEGORY_IDS = [
    '116715>116754'    # living room
]

class RevisionV16 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v16')
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

        # change image only if product is in 'selected' categories
        if self._is_select_category (record) == True:
            updated_record = self._perform_update_internal (record, inject_av_record)
            return updated_record
        else:
            return record
        return updated_record

    def _finalize (self, updated_products):
        return True

    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        pid = record ['value']['attributes']['pid']

        # This check is just to double-sure
        # Make sure the product for which we are reverting variants is not
        # one of those that SCs now want to have variants for... --:(
        current_img_url = record ['value']['attributes']['thumb_image']
        if current_img_url.find ('images/webp/gen_gptimage_1/variants') >= 0:
            logging.debug ('Not reverting variants for pid: %s' % pid)
            return record

        logging.debug ('Reverting variants for pid: %s' % pid)
        updated_record = copy.deepcopy (record)

        # keep only one variant
        if ('variants' in record ['value']) and (record ['value']['variants']):
            variant_list = record ['value']['variants']
            for i in range (len (variant_list)):
                variant_id = '%s_%s' % (pid, i)
                if i == 0:
                    variant_obj = variant_list [variant_id]
                    variant_obj ['default_sku'] = True
                    image_url = '%s/%s_image.webp' % (rcv16.THUMB_IMAGE_ORIGINAL_URL_PROLOG, variant_id)
                    updated_record ['value']['attributes']['thumb_image'] = image_url
                    updated_record ['value']['attributes']['large_image'] = image_url
                    updated_record ['value']['variants'][variant_id]['attributes']['swatch_image'] = image_url
                else:
                    del updated_record ['value']['variants'][variant_id]
        return updated_record

    def _is_select_category (self, record):
        category_paths = record ['value']['attributes']['category_paths']
        for branch in category_paths:
            full_path = None 
            for leaf_node in branch:
                if full_path == None:
                    full_path = leaf_node ['id']
                else:
                    full_path = '%s>%s' % (full_path, leaf_node ['id'])
            if full_path in SELECT_CATEGORY_IDS:
                return True
        return False

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV16 ()
    logging.info ('RevisionV16 finish...')

