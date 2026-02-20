# V2 changes
# -- change pid values such as "KIT" -> C...
# -- remove 'test' attributes added earlier
# -- change variant-id same as pid
# -- remove one special product "P1-" 
# -- adjust image urls to refer to .../homesc/images/...

import logging
import os
import copy
import json
import csv
import random

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV2 as rcv2

class RevisionV2 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v2')
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
        return updated_products
 
    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']
        # @@@ logging.debug ('@@@ Processing pid %s' % pid)

        # pid = P1- was specially defined pid in home catalog; ignore it
        if (pid.find ('P1-') >= 0):
            return None

        updated_record = copy.deepcopy (record)

        # change variant-id if needed. Do this before changing pid if needed
        # In some cases, variant-id same as pid
        self._change_variantid_if_needed (updated_record)

        # change pid value if needed
        self._change_pid_if_needed (updated_record)

        # adjust image urls
        self._adjust_image_urls (updated_record)

        # remove 'test' attributes added earlier
        # @@@ "X" -- add two 'test' integer attributes to test V3-api-response handling in SPA
        if 'test_int1' in updated_record ['value']['attributes']:
            del updated_record ['value']['attributes']['test_int1']
        if 'test_int2' in updated_record ['value']['attributes']:
            del updated_record ['value']['attributes']['test_int2']

        return updated_record

    def _change_variantid_if_needed (self, updated_record):
        orig_pid = updated_record ['value']['attributes']['pid']
        change_variant_id = False

        # variant if any
        if ('variants' in updated_record ['value']) and (updated_record ['value']['variants']):
            variant_list = updated_record ['value']['variants']
            for variant_id, variant_obj in variant_list.items():
                if (variant_id == orig_pid):
                    change_variant_id = True
                    break

        if change_variant_id == False:
            return  # none of the variant-id's match orig-pid
    
        # dup original variants 
        dup_variants = copy.deepcopy (updated_record ['value']['variants'])

        count = 0
        variant_list = updated_record ['value']['variants']
        for variant_id, variant_obj in variant_list.items():
            if (variant_id == orig_pid):
                count = count + 1

                # see if 'orig-pid' itself needs to be changed (eg, 123214fam)
                pid_change_needed, pid = self._is_pid_change_needed (orig_pid)
                new_variant_id = '%s_%s' % (pid, count)
                dup_variants [new_variant_id] = variant_obj # add new-variant-id in dup'd variants-list
                del dup_variants [variant_id]   # delete original variant-id from dup'd variants-list

        # replace original variants in updated_product record
        updated_record ['value']['variants'] = dup_variants

        return

    def _change_pid_if_needed (self, updated_record):
        orig_pid = updated_record ['value']['attributes']['pid']
        change_needed, pid = self._is_pid_change_needed (orig_pid)

        if change_needed == False:
            return  # updated_record remains as-is

        # product attributes
        updated_record ['value']['attributes']['pid'] = pid
        updated_record ['path'] = '/products/%s' % pid
        if 'url' in updated_record ['value']['attributes']:
            value = updated_record ['value']['attributes']['url']
            value = value.replace (orig_pid, pid)
            updated_record ['value']['attributes']['url'] = value
        if 'thumb_image' in updated_record ['value']['attributes']:
            value = updated_record ['value']['attributes']['thumb_image']
            value = value.replace (orig_pid, pid)
            updated_record ['value']['attributes']['thumb_image'] = value
        if 'large_image' in updated_record ['value']['attributes']:
            value = updated_record ['value']['attributes']['large_image']
            value = value.replace (orig_pid, pid)
            updated_record ['value']['attributes']['large_image'] = value
        if 'collection' in updated_record ['value']['attributes']:
            value = updated_record ['value']['attributes']['collection']
            if (value != None):
                value= value.replace (orig_pid, pid)
                updated_record ['value']['attributes']['collection'] = value

        # variant attribs
        if ('variants' in updated_record ['value']) and (updated_record ['value']['variants']):
            variant_list = updated_record ['value']['variants']
            for variant_id, variant_obj in variant_list.items():
                variant_attribs = variant_obj ['attributes'].keys ()
                for attrib in variant_attribs:
                    value = variant_obj ['attributes'][attrib]
                    if isinstance (value, str):
                        if value.find (orig_pid) >= 0:
                            value = value.replace (orig_pid, pid)
                            variant_obj ['attributes'][attrib] = value
        return

    # returns true if change needed and also new-pid-value
    def _is_pid_change_needed (self, orig_pid):
        change_needed = False
        pid = orig_pid

        indx = orig_pid.find ('KIT')
        if indx >= 0:
            indx = indx + len ('KIT')
            tail = orig_pid [indx:]
            pid = 'C%s' % tail
            change_needed = True
        else:
            indx = orig_pid.find ('fam')
            if indx >= 0:
                head = orig_pid [:indx]
                pid = 'C%s' % head
                change_needed = True

        return (change_needed, pid)

    # image url = .../images/webp/pid_0_image.webp
    def _adjust_image_urls (self, updated_record):
        pid = updated_record ['value']['attributes']['pid']
        img_url = '%s%s%s' % (uc.IMAGE_URL_PREFIX, pid, '_0_image.webp')
        if 'thumb_image' in updated_record ['value']['attributes']:
            updated_record ['value']['attributes']['thumb_image'] = img_url
        if 'large_image' in updated_record ['value']['attributes']:
            updated_record ['value']['attributes']['large_image'] = img_url

        if ('variants' in updated_record ['value']) and (updated_record ['value']['variants']):
            variant_list = updated_record ['value']['variants']
            for variant_id, variant_obj in variant_list.items():
                if 'swatch_image' in variant_obj ['attributes']:
                    variant_obj ['attributes']['swatch_image'] = img_url

        return

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV2 ()
    logging.info ('RevisionV2 Finish...')


