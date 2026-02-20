# V5 changes
# -- add recency attributes

import logging
import random
import os
import copy
import time

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV5 as rcv5

class RevisionV5 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v5')
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

        # recency attributes
        self._add_recency_attributes (updated_record)

        return updated_record

    def _add_recency_attributes (self, record):
        # freshness - 10% week, 20% month, 30% quarter, remaining 'earlier'
        # Also, freshness < month => 'is_new = true'
        duration_1yr  = 1 * 360 # days
        is_new = "No" # default

        rand = random.random ()
        if (rand < 0.10):
            days_since_release = (int) (rand * rcv5.FRESHNESS_TIMEPERIOD_WEEK)
            released_since = 'New this week'
            is_new = "Yes"
        elif (rand < 0.30):
            days_since_release = (int) (rand * rcv5.FRESHNESS_TIMEPERIOD_MONTH) + rcv5.FRESHNESS_TIMEPERIOD_WEEK + 1
            released_since = '1-4 weeks'
            is_new = "Yes"
        elif (rand < 0.60):
            days_since_release = (int) (rand * rcv5.FRESHNESS_TIMEPERIOD_QUARTER) + rcv5.FRESHNESS_TIMEPERIOD_MONTH + 1
            released_since = '1-3 months'
        else:
            days_since_release = (int) (rand * duration_1yr) + rcv5.FRESHNESS_TIMEPERIOD_QUARTER + 1
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


if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV5 ()
    logging.info ('RevisionV5 Finish...')


