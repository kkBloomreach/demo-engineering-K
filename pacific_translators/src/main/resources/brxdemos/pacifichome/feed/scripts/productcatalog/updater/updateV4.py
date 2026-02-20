# V4 changes
# -- there is "update" action in this updater. Therefore, full output = full input
# -- However, a subset is created using the catIds provided in subset_catids.tsv

import logging
import copy
import csv

from updateBase import UpdateBase
import updaterConstants as uc
import updaterConstantsV4 as ucv4
import subsetFeedV4 as ssv4

class UpdateV4 (UpdateBase) :
    def __init__ (self):
        logging.info ('Perform update + generate subset, version v4')
        super().__init__ ()

        return

    # override base class method
    def _perform_record_update (self, record):
        updated_record = self._perform_update_internal (record)
        return updated_record

    # override base class mathod
    def prepare_subset (self, current_products):
        subseter = ssv4.SubsetFeedV4 ()
        subseter.setUpdatedFeed (current_products)
        subset_products, subset_attributes = subseter.prepareSubset ()
        logging.info ('Subset product count: %s' % len (subset_products))
        return subset_products, subset_attributes   # return subset products AND attributes

    def _perform_update_internal (self, record):
        updated_record = copy.deepcopy  (record)
        return updated_record

if __name__ == '__main__':
    u = UpdateV4 ()


