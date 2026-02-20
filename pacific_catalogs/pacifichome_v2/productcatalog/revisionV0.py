# V0 changes
# -- Version2.0 of PacificHome catalog
#   - cleanup categories
#   - if product is no longer in any category, remove that product as well
#   - all pid, catids remain same as before

import logging
import random
import os
import copy
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV0 as rcv0
import categoryManager as cm

class RevisionV0 (RevisionBase) :
    # status map read from .tsv
    _category_status_map = []

    def __init__ (self):
        logging.info ('Perform update, version v0')
        super().__init__ ()
        return

    def _initialize (self, source_records, category_manager, inject_av_map):
        # read categorystatus map 
        self._category_status_map = self._read_status_map ()

        # delete categories as mentioned in the status_map
        for status_record in self._category_status_map:
            if (status_record ['Status'] == 'remove'):
                if (category_manager.find_node_by_id (status_record ['CatId']) != None):
                    category_manager.remove_node_by_id (status_record ['CatId'])

        # remove empty categories (categories with zero products)
        # Also removes parent(s) if all its leaves have zero products
        # step1: collect category -> product count
        for record in source_records:
            category_paths = record ['value']['attributes']['category_paths']
            for branch in category_paths:
                branch_leaf_id = branch [len (branch) - 1]['id']
                category_manager.update_product_count (branch_leaf_id)  # increase product_count by 1

        # step 2: remove empty categories
        category_manager.remove_empty_nodes ()

        # finally, remove 'Trends' category from the category tree
        category_manager.remove_node_by_id (rcv0.TRENDS_CAT_ID)

        # @@@ debug
        category_manager.write_tsv (rcv0.FILENAME_PRUNDED_CATEGORY_STATUS_MAP_TSV_OUT)
        return True
 
    # override base class method
    # This update class does not do any update to previous records
    def _perform_record_update (self, record, category_manager):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)
        if (inject_av_record == None):
            logging.info ('No inject attrib_value record for pid: %s', pid)

        updated_record = self._perform_update_internal (record, inject_av_record, category_manager)
        return updated_record

    def _finalize (self):
        return True # Place holder
 
    # INTERNAL METHODS
    # status as to which categories to be removed etc...
    def _read_status_map (self):
        _category_status_map = []

        with open (rcv0.FILENAME_CATEGORY_STATUS_MAP_TSV_IN, 'r') as status_map_file:
            tsv_reader = csv.DictReader (status_map_file, delimiter = '\t')
            for row in tsv_reader:
                _category_status_map.append (row)
            status_map_file.close ()
        return _category_status_map

    def _perform_update_internal (self, record, inject_av_record, category_manager):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']

        updated_record = copy.deepcopy (record)

        # for this product, exclude categories that have been removed
        category_paths = record ['value']['attributes']['category_paths']
        new_category_paths = []
        for branch in category_paths:
            exclude_branch = False
            for node in branch:
                if (category_manager.find_node_by_id (node ['id']) == None):
                    # this cat_id has been 'removed'
                    exclude_branch = True
                    break

            if (exclude_branch == False):
                new_category_paths.append (branch)

        # if after removing branches, the product is not in any branch (ie, has new_category_path is
        # empty), exclude the product from catalog
        if (len (new_category_paths) == 0):
            logging.warning ('#1 Product removed from catalog because it is not in any category: %s' % pid)
            return None
        else:
            updated_record ['value']['attributes']['category_paths'] = new_category_paths

        # replace "Trends" sub-catagory -> "collection" attribute
        # if product already has a 'collection' attribute, it remains unchanged
        collection = None
        if 'collection' in updated_record ['value']['attributes']:
            collection = updated_record ['value']['attributes']['collection']
        if collection == None:
            # if product is in Trends category, set collection = Trends->subcategory
            for branch in new_category_paths:
                top_node = branch [0]
                if (top_node ['id'] == rcv0.TRENDS_CAT_ID):
                    # get leaf node in this branch
                    leaf_node = branch [len (branch) - 1]
                    updated_record ['value']['attributes']['collection'] = leaf_node ['name']

        # whether collection is set-or-not, remove "Trends" category from category_paths
        new_category_paths_2 = []
        for branch in new_category_paths:
            top_node = branch [0]
            if (top_node ['id'] == rcv0.TRENDS_CAT_ID):
                continue    # dont include this branch in new_category_path_2
            new_category_paths_2.append (branch)

        if (len (new_category_paths_2) == 0):
            logging.warning ('#2 Product removed from catalog because it is not in any category: %s' % pid)
            return None

        updated_record ['value']['attributes']['category_paths'] = new_category_paths_2

        # re-calculate "category_level_1/2/3/4"
        # category levels
        category_levels = self._collect_category_levels (updated_record)
        for i in range (0, rcv0.MAX_CATEGORY_LEVELS):
            level_name = ''
            if (i < len (category_levels)) and (category_levels [i]):
                level_name = category_levels [i]
            attrib_name = '%s%s' % (rcv0.PREAMBLE_ATTRIB_NAME_CATEGORY_LEVEL, i+1)
            updated_record ['value']['attributes'][attrib_name] = level_name

        # description: use description-enh (enhanced)
        if 'description_enh' in updated_record ['value']['attributes']:
            updated_record ['value']['attributes']['description'] = updated_record ['value']['attributes']['description_enh']
            # pop the description_enh attribute
            updated_record ['value']['attributes'].pop ('description_enh')

        # new attribute: 'return_rate'
        return_rate = self._get_return_rate_value (updated_record)
        updated_record ['value']['attributes'][rcv0.ATTRIB_NAME_RETURN_RATE] = return_rate

        # make some non-available products 'available' (70% of unavailable -> available)
        if (updated_record ['value']['attributes']['availability'] == False):
            rand = random.random () * 10
            if rand < 7:
                updated_record ['value']['attributes']['availability'] = True

        return updated_record

    # use bread_crumb to return list of [l0, l1, ...] of first category
    def _collect_category_levels (self, record):
        category_levels = []

        category_paths = record ['value']['attributes']['category_paths']
        if (category_paths == None) or (len (category_paths) == 0):
            return category_levels # zero-length list

        branch_path_0 = category_paths [0]   # use first branch
        for branch_nodes in branch_path_0:
            category_levels.append (branch_nodes ['name'])
        return category_levels

    # returnable 'blocks':
    #   0: none - never returned
    #   1: low - returned 10 - 20 % (ie, 10 to 20% product is returned)
    #   2: medium - returned 20 - 40 %
    #   3: high - returned 40 - 80 %
    # catalog has
    #   none: 70%
    #   low: 15%
    #   medium: 10 %
    #   high: 5%
    # for example, 5% of products are returned 40 to 80%  
    def _get_return_rate_value (self, record):
        block = (random.random () * 10)
        if (block < 7):
            return_rate = 0
        elif (block >= 7 and block < 8.5):
            return_rate = (random.random () * (20 - 10)) + 10
        elif (block >= 8.5 and block < 9.5 ):
            return_rate = (random.random () * (40 - 20)) + 20
        elif (block >= 9.5 ):
            return_rate = (random.random () * (80 - 40)) + 40

        return_rate = int (return_rate)
        return return_rate

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV0 ()
    logging.info ('RevisionV0 Finish...')

'''
        # delete 'removed' category nodes from tree
        for record in source_records:
            category_paths = record ['value']['attributes']['category_paths']
            for branch in category_paths:
                for node in branch:
                    if (category_manager.find_node_by_id (node ['id']) != None):
                        category_manager.remove_node_by_id (node ['id']) != None):
                        logging.debug ('Cannot remove category node: %s', '12321')
'''

