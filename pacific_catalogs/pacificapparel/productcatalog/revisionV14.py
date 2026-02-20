# V14 changes
# -- add 'recency' attributes in the catalog -- TEMP NOT INCLUDED (04/25/2025)
# -- replace image extension to .webp
# -- ensure health&beauty products are 'not-available' at this time

import logging
import random
import os
import copy
import time

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV14 as rcv14
import categorybuilder as cb

class RevisionV14 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v14')
        super().__init__ ()
        return

    def _initialize (self, source_records, inject_av_map):
        return True

    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)

        if (inject_av_record == None):
            logging.debug  ('No inject attrib_value record for pid: %s', pid)

        updated_record = self._perform_update_internal (record, inject_av_record)
        return updated_record

    # generate aws upload script (to be executed separately)
    def _finalize (self, updated_products):
        return True

    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        pid = record ['value']['attributes']['pid']
        updated_record = copy.deepcopy (record)

        # recency attributes
        self._add_recency_attributes (updated_record)

        # replace image extensions to .webp
        self._change_image_url_to_webp (updated_record)

        # ensure health-and-beauty products availability = false
        self._ensure_health_and_beauty_unavailable (updated_record)

        return updated_record

    def _add_recency_attributes (self, record):
        # freshness - 10% week, 20% month, 30% quarter, remaining 'earlier'
        # Also, freshness < month => 'is_new = true'
        duration_1yr  = 1 * 360 # days
        is_new = "No" # default

        rand = random.random ()
        if (rand < 0.10):
            days_since_release = (int) (rand * rcv14.FRESHNESS_TIMEPERIOD_WEEK)
            released_since = 'New this week'
            is_new = "Yes"
        elif (rand < 0.30):
            days_since_release = (int) (rand * rcv14.FRESHNESS_TIMEPERIOD_MONTH) + rcv14.FRESHNESS_TIMEPERIOD_WEEK + 1
            released_since = '1-4 weeks'
            is_new = "Yes"
        elif (rand < 0.60):
            days_since_release = (int) (rand * rcv14.FRESHNESS_TIMEPERIOD_QUARTER) + rcv14.FRESHNESS_TIMEPERIOD_MONTH + 1
            released_since = '1-3 months'
        else:
            days_since_release = (int) (rand * duration_1yr) + rcv14.FRESHNESS_TIMEPERIOD_QUARTER + 1
            released_since = 'More than 1 quarter'

        time_now = int (time.time ())
        #logging.debug ('epoch today: %s' % time_now)
        start_time = time_now - (days_since_release * 24 * 60 * 60) # seconds
        days_till_end = (int) (random.random () * duration_1yr) + duration_1yr
        end_time = time_now + (days_till_end * 24 * 60 * 60) #seconds

        record ['value']['attributes']['start_date'] = start_time
        record ['value']['attributes']['end_date'] = end_time
        record ['value']['attributes']['is_new'] = is_new
        record ['value']['attributes']['released_since'] = released_since
        record ['value']['attributes']['days_since_release'] = days_since_release
        return

    def _change_image_url_to_webp (self, updated_record):
        product_attribs = updated_record ['value']['attributes']
        # product image
        if 'thumb_image' in product_attribs:
            img_url = product_attribs ['thumb_image']
            img_url = img_url.replace ('/images/', '/images/webp/')
            if '.jpg' in img_url:
                img_url = img_url.replace ('.jpg', '.webp')
            elif '.png' in img_url:
                img_url = img_url.replace ('.png', '.webp')
            else:
                logging.warning ('Unknown thumb_image extension for pid: %s' % product_attribs ['pid'])
            product_attribs ['thumb_image'] = img_url

        # variant images if any
        if 'variants' in updated_record ['value']:
            variant_list = updated_record ['value']['variants']
            for variant_id, variant_obj in variant_list.items():
                variant_attribs = variant_obj ['attributes']
                if 'swatch_image' in variant_attribs:
                    img_url = variant_attribs ['swatch_image']
                    img_url = img_url.replace ('/images/', '/images/webp/')
                    if '.jpg' in img_url:
                        img_url = img_url.replace ('.jpg', '.webp')
                    elif '.png' in img_url:
                        img_url = img_url.replace ('.png', '.webp')
                    else:
                        logging.warning ('Unknown thumb_image extension for pid: %s' % product_attribs ['pid'])
                variant_attribs ['swatch_image'] = img_url
        return 

    def _ensure_health_and_beauty_unavailable (self, updated_record):
        category_paths = updated_record ['value']['attributes']['category_paths']
        for branch in category_paths:
            for leaf in branch:
                if leaf ['id'] == rcv14.CAT_ID_HEALTH_AND_BEAUTY:
                    updated_record ['value']['attributes']['availability'] = False
                    return  # once availability = false, no need to look any other catId
        return


if __name__ == '__main__':
    rv = RevisionV14 ()


