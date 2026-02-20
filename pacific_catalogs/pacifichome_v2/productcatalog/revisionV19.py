# V19 changes
# -- In a previous version, we had created variants (and their images) for livingroom ('119715>119754') based on SC request
# -- SCs now don't want those. Therefore, for products in '119715>119754', 
# -- Use an old .jsonl and extract variant info from that .jsonl for selected category

import logging
import copy
import os

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV19 as rcv19
import jsonlFeedReader as jfr

SELECT_CATEGORY_IDS = [
    '116715>116754'    # living room
]

class RevisionV19 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v19')
        super().__init__ ()
        self._original_variant_source_records = None
        return

    def _initialize (self, source_records, inject_av_map):
        original_variant_source_reader = jfr.JsonlFeedReader ()
        self._original_variant_source_records = original_variant_source_reader.readSourceFeed (rcv19.FILENAME_ORIGINAL_VARIANT_JSONL_SOURCE_FEED_IN)
        if self._original_variant_source_records == None:
            logging.error ('Could not read original variant source records')
            return False
        return True

    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)
        #if (inject_av_record == None):
        #    logging.debug ('No inject attrib_value record for pid: %s', pid)

        # change variant image only if product is in 'selected' categories
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

        logging.debug ('Reverting variant for pid: %s' % pid)
        updated_record = copy.deepcopy (record)

        # lookup original-0 variant and copy img, color from that object
        if ('variants' in record ['value']) and (record ['value']['variants']):
            variant_list = record ['value']['variants']
            for i in range (len (variant_list)):
                variant_id = '%s_%s' % (pid, i)
                if i == 0:
                    variant_obj = variant_list [variant_id]
                    variant_obj ['default_sku'] = True

                    # lookup original variant-0 for this pid
                    original_variant_0_obj = self._lookup_original_variant_0 (pid)
                    if original_variant_0_obj != None:
                        image_url = original_variant_0_obj ['attributes']['swatch_image']

                        updated_record ['value']['attributes']['thumb_image'] = image_url
                        updated_record ['value']['attributes']['large_image'] = image_url
                        updated_record ['value']['variants'][variant_id]['attributes']['swatch_image'] = image_url
                        if 'color' in original_variant_0_obj ['attributes']:
                            updated_record ['value']['variants'][variant_id]['attributes']['color'] = original_variant_0_obj ['attributes']['color']
                        else:
                            logging.debug ('No color attribute in original_variant_0 object: %s' % original_variant_0_obj ['attributes']['skuid'])
                            updated_record ['value']['variants'][variant_id]['attributes']['color'] = None
                    else:
                        logging.warning ('Could not find original variant-0 for pid: %s' % pid)
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

    def _lookup_original_variant_0 (self, pid):
        original_variant_0_obj = None

        for original_src_record in self._original_variant_source_records:
            if original_src_record ['value']['attributes']['pid'] == str (pid):
                if ('variants' in original_src_record ['value']) and (original_src_record ['value']['variants']):
                    original_variant_list = original_src_record ['value']['variants']
                    for original_variant_id in original_variant_list.keys ():
                        original_variant_0_obj = original_variant_list [original_variant_id]
                        # just to double check - 'default_sku = true'
                        if 'default_sku' in original_variant_0_obj ['attributes']:
                            if original_variant_0_obj ['attributes']['default_sku'] == True:
                                return original_variant_0_obj
                            else:
                                logging.warning ('Original variant-0 default-sku false, variantId: %s' % original_variant_id)
                        else:
                            logging.warning ('Original variant-0 does not have default-sku attribute, variantId: %s' % original_variant_id)
                else:
                    logging.warning ('Original record has no variants for pid: %s' % pid)
        return original_variant_0_obj


if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV19 ()
    logging.info ('RevisionV19 finish...')

