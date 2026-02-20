# V16 changes
# -- Ensure each product has at least one variant (since some products already have some variants)

import logging
import copy

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV16 as rcv16
import categorybuilder as cb

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

        if (inject_av_record == None):
            logging.debug ('No inject attrib_value record for pid: %s', pid)

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
        # ensure at least one variant exists in this record
        self._ensure_product_has_variant (updated_record)

        return updated_record

    def _ensure_product_has_variant (self, record):
        if 'variants' in record ['value']:
            if len (record ['value']['variants']) > 0:
                return record   # record has some variants

        # this record has no variants
        logging.debug ('Creating variant for pid: %s' % record ['value']['attributes']['pid'])
        variant_id = '%s_0' % record ['value']['attributes']['pid']
        record ['value']['variants'] = {}
        record ['value']['variants'][variant_id] = {}
        record ['value']['variants'][variant_id]['attributes'] = {}
        record ['value']['variants'][variant_id]['attributes']['skuid'] = variant_id
        record ['value']['variants'][variant_id]['attributes']['price'] = record ['value']['attributes']['price']
        record ['value']['variants'][variant_id]['attributes']['sale_price'] = record ['value']['attributes']['sale_price']
        record ['value']['variants'][variant_id]['attributes']['default_sku'] = True
        if 'color' in record ['value']['attributes']:
            record ['value']['variants'][variant_id]['attributes']['color'] = record ['value']['attributes']['color']
            # remove this attrib from product-level record
            del (record ['value']['attributes']['color'])
        if 'size' in record ['value']['attributes']:
            record ['value']['variants'][variant_id]['attributes']['size'] = record ['value']['attributes']['size']
            # remove this attrib from product-level record
            del (record ['value']['attributes']['size'])
        record ['value']['variants'][variant_id]['attributes']['swatch_image'] = record ['value']['attributes']['thumb_image']

        return

if __name__ == '__main__':
    rv = RevisionV16 ()


