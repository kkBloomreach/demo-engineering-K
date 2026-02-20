# V1 changes
# -- exact same code as V0 except the input (.jsonl, category-tree) are next version
# -- Use the manually curated target-category-map.
# -- It includes actions for each category
# -- 'X'
#    -- added two test attributes to test V3 API response

import logging
import os
import copy
import json
import csv
import random

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV1 as rcv1
from categoryManager import CategoryManager

class RevisionV1 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v1')
        super().__init__ ()
        self._target_category_map = None
        # _cat_merge_list === 
        #   list {src_cat_id, src_full_crumbid, src_full_crumb, 
        #         target_parent_cat_id, target_parent_full_crumbid, target_parent_full_crumb} 
        self._cat_merge_list = [] 

        # to ensure max-products-per-category
        # _pids_per_cat ===
        #   list { catid: pid[]}
        self._pids_per_cat = []

        return

    def _initialize (self, source_records, inject_av_map):
        if os.path.exists (rcv1.FILE_NAME_PH3_TARGET_CATEGORY_MAP_INPUT_TSV):
            target_category_map = self._read_target_category_map (rcv1.FILE_NAME_PH3_TARGET_CATEGORY_MAP_INPUT_TSV)
        else:
            logging.error ('Cannot find target PH3 category map: %s' % rcv1.FILE_NAME_PH3_TARGET_CATEGORY_MAP_INPUT_TSV)
            return False

        self._target_category_map = target_category_map

        # process target_category_map and update category tree
        self._process_target_category_map (target_category_map)
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
        post_count_control_records = []
        for record in updated_products:
            pid = record ['value']['attributes']['pid']
            category_paths = record ['value']['attributes']['category_paths']
            ok_to_include_product_in_catalog = True

            # NOTE, we can do MAX-pid-per-cat control only if the pid is in only one branch
            # If pid is in multiple branches, won't be able to exclude it from the entire catalog
            if (len (category_paths) == 1):
                branch = category_paths [0]
                branch_leaf = branch [len (branch) - 1]
                leaf_cat_id = branch_leaf ['id']
                cat_pid_count = self._category_manager.get_product_count (leaf_cat_id)
                if (cat_pid_count > rcv1.MAX_PRODUCTS_IN_EACH_CATEGORY):
                    # randomly decide if this pid should/should-not be in catalog
                    rand = random.random () * cat_pid_count
                    if rand > rcv1.MAX_PRODUCTS_IN_EACH_CATEGORY:
                        ok_to_include_product_in_catalog = False
                        # reduce product count in this category
                        self._category_manager.decrement_product_count (leaf_cat_id)

                        # @@@
                        logging.debug ('Excluding pid due to max-count-per-cat control: %s' % pid)

            if ok_to_include_product_in_catalog == True:
                post_count_control_records.append (record)
        return post_count_control_records
 
    # INTERNAL METHODS
    def _read_target_category_map (self, category_map_filename):
        map_records = []
        if os.path.exists (category_map_filename):
            with open (category_map_filename, 'r') as file_obj:
                dict_reader = csv.DictReader (file_obj, delimiter='\t')
                for row in dict_reader:
                    map_records.append (row)
                file_obj.close ()
            logging.info ("category map record count: %s", len (map_records))
        else:
            logging.error ('cannot find category map file: %s', category_map_filename)
            map_records = None
        return map_records

    # update category tree based on target_category_map
    def _process_target_category_map (self, target_category_map):
        for map_record in target_category_map:
            cat_id = map_record ['CatId']
            category_action = map_record ['action']
            if (category_action == ''):
                # no change - keep-as-is
                continue
            elif (category_action == 'rename'):
                cat_name = map_record ['New category name/mapping']
                self._category_manager.update_cat_name (cat_id, cat_name)
            elif (category_action == 'merge'):
                target_parent_cat_id = map_record ['merge_target']  # target 'parent' where src's children are merged into
                if self._category_manager.merge_node_into_target (cat_id, target_parent_cat_id) == False:
                    logging.error ('Category merge failed for src cat_id = %s' % cat_id)
                else:
                    self._category_manager.update_node_action (cat_id, CategoryManager.ACTION_MERGE)

                    # also keep a list of merge-cats. This is needed when processing each pid's categories
                    merge_record = { 'src_cat_id': cat_id,
                                     'src_full_crumbid': None,   # filled below
                                     'src_full_crumb': None,     # filled below
                                     'target_parent_cat_id': target_parent_cat_id,
                                     'target_parent_full_crumbid': None,    # filled below
                                     'target_parent_full_crumb': None    # filled below
                                 }
                    self._cat_merge_list.append (merge_record)
            elif (category_action == 'delete'):
                self._category_manager.remove_node_by_id (cat_id)
            else:
                logging.error ('Unknown category_action: %s' % category_action)
                category_action = ''

        # fill up local merge-list (full_crumbid, full_crumb of the target parent)
        # this is needed when processing category_paths of products in the merged categories
        for merge_record in self._cat_merge_list:
            src_full_crumbid, src_full_crumb = self._category_manager.get_node_full_breadcrumb_and_id (merge_record ['src_cat_id'])
            target_parent_full_crumbid, target_parent_full_crumb = self._category_manager.get_node_full_breadcrumb_and_id (merge_record ['target_parent_cat_id'])
            merge_record ['src_full_crumbid'] = src_full_crumbid
            merge_record ['src_full_crumb'] = src_full_crumb
            merge_record ['target_parent_full_crumbid'] = target_parent_full_crumbid
            merge_record ['target_parent_full_crumb'] = target_parent_full_crumb
        return

    def _perform_update_internal (self, record, inject_av_record):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']
        # @@@ 
        logging.debug ('@@@ Processing pid %s' % pid)

        # currently, if product-not-available, exclude it from target catalog
        # @@@ TO BE DECIDED
        if record ['value']['attributes']['availability'] == False:
            return None

        new_category_paths = self._build_new_category_paths (record)
        if (new_category_paths == None) or (len (new_category_paths) == 0):
            logging.warning ('New category paths none or zero-length for pid: %s' % pid)
            return None

        updated_record = copy.deepcopy (record)
        updated_record ['value']['attributes']['category_paths'] = new_category_paths

        # re-calculate "category_level_1/2/3/4"
        category_levels = self._collect_category_levels (updated_record)
        for i in range (0, rcv1.MAX_CATEGORY_LEVELS):
            level_name = ''
            if (i < len (category_levels)) and (category_levels [i]):
                level_name = category_levels [i]
            attrib_name = '%s%s' % (rcv1.PREAMBLE_ATTRIB_NAME_CATEGORY_LEVEL, i+1)
            updated_record ['value']['attributes'][attrib_name] = level_name

        # update product counts in each branch's leaf
        self._update_product_counts (updated_record)

        # @@@ "X" -- add two 'test' integer attributes to test V3-api-response handling in SPA
        updated_record ['value']['attributes']['test_int1'] = int (random.random () * 100)
        updated_record ['value']['attributes']['test_int2'] = int (random.random () * 500)

        # remove unwanted product attributes
        if 'bread_crumb' in updated_record ['value']['attributes']:
            del updated_record ['value']['attributes']['bread_crumb']
        if 'bread_crumb_id' in updated_record ['value']['attributes']:
            del updated_record ['value']['attributes']['bread_crumb_id']
        if 'int_pid' in updated_record ['value']['attributes']:
            del updated_record ['value']['attributes']['int_pid']
        if 'mpn' in updated_record ['value']['attributes']:
            del updated_record ['value']['attributes']['mpn']
        if 'flavor' in updated_record ['value']['attributes']:
            del updated_record ['value']['attributes']['flavor']
        if 'subject' in updated_record ['value']['attributes']:
            del updated_record ['value']['attributes']['subject']
        if 'wineAppellations' in updated_record ['value']['attributes']:
            del updated_record ['value']['attributes']['wineAppellations']
        if 'shipping_info' in updated_record ['value']['attributes']:
            del updated_record ['value']['attributes']['shipping_info']
        if 'shipping_weight' in updated_record ['value']['attributes']:
            del updated_record ['value']['attributes']['shipping_weight']
        if 'sale_price_range_min' in updated_record ['value']['attributes']:
            del updated_record ['value']['attributes']['sale_price_range_min']
        if 'sale_price_range_max' in updated_record ['value']['attributes']:
            del updated_record ['value']['attributes']['sale_price_range_max']

        # remove unwanted variante attributes
        if ('variants' in updated_record ['value']) and (updated_record ['value']['variants']):
            variant_list = updated_record ['value']['variants']
            for variant_id, variant_obj in variant_list.items():
                if 'int_skuid' in variant_obj ['attributes']:
                    del variant_obj ['attributes']['int_skuid']
                if 'colorFamily' in variant_obj ['attributes']:
                    del variant_obj ['attributes']['colorFamily']
                if 'sizeFamily' in variant_obj ['attributes']:
                    del variant_obj ['attributes']['sizeFamily']
                if 'velo_sku_price' in variant_obj ['attributes']:
                    del variant_obj ['attributes']['velo_sku_price']
                if 'velo_sku_sale_price' in variant_obj ['attributes']:
                    del variant_obj ['attributes']['velo_sku_sale_price']
                # earlier, price, sale_price were strings; change to float
                if 'price' in variant_obj ['attributes']:
                    if isinstance (variant_obj ['attributes']['price'], str):
                        price_float = float (variant_obj ['attributes']['price'])
                        variant_obj ['attributes']['price'] = round (price_float, 2)
                if 'sale_price' in variant_obj ['attributes']:
                    if isinstance (variant_obj ['attributes']['sale_price'], str):
                        sale_price_float = float (variant_obj ['attributes']['sale_price'])
                        variant_obj ['attributes']['sale_price'] = round (sale_price_float, 2)


        return updated_record

    # if any category_path for this product is 'ok-to-keep' then return list of all such 'ok-to-keep' branches
    def _build_new_category_paths (self, src_record):
        category_paths = src_record ['value']['attributes']['category_paths']
        new_category_paths = []
        for branch in category_paths:
            if self._is_ok_to_keep_branch (branch) == False:
                continue    # check next branch if any in this category_paths
            else:
                new_category_paths.append (branch)

        if (len (new_category_paths) == 0):
            return None

        # next, see if any category has been 'renamed'
        for branch in new_category_paths:
            for branch_elem in branch:
                elem_name = self._category_manager.get_cat_name (branch_elem ['id'])
                branch_elem ['name'] = elem_name    # may/may-not have been renamed from original name

        # next, see if any branch_elem is to be 'merged' into some other parent
        # In a given branch, if any branch_elem is to be merged, recalculate the entire branch
        # Since we use new_category_paths to iterate, we cannot change its contents while iterating
        # therefore create another list
        post_merge_category_paths = []
        for branch in new_category_paths:
            merge_record = self._is_branch_merge_candidate (branch)
            if merge_record != None:
                # @@@ logging.debug ('\t@@@ Need to merge branch due to cat_id: %s' % 
                #                     merge_record ['src_cat_id'])

                post_merge_branch = self._build_merged_branch (branch, merge_record)

                # because of merge, it is possible the post-merge-branch is already in this product's
                # category path. Those duplicates are handled below (ancestral checks)

                post_merge_category_paths.append (post_merge_branch)
            else:
                # no change for this branch
                post_merge_category_paths.append (branch)

        if (post_merge_category_paths == None) or (len (post_merge_category_paths) == 0):
            logging.warning ('Post merge paths none or zero-length: %s' % src_record ['value']['attributes']['pid'])
            return None

        new_category_paths = post_merge_category_paths  # rename back 

        # next, see if same pid is included in child-cat as well as any of its ancestors
        # if so, remove the ancestors. 
        post_ancestral_check_paths = self._curate_ancestral_paths (new_category_paths)
        if (post_ancestral_check_paths == None) or (len (post_ancestral_check_paths) == 0):
            logging.warning ('Post ancestral check paths none or zero-length: %s' % src_record ['value']['attributes']['pid'])
            return None
        new_category_paths = post_ancestral_check_paths  # rename back 

        return new_category_paths

    def _is_ok_to_keep_branch (self, branch):
        for branch_elem in branch:
            if (self._category_manager.find_node_by_id (branch_elem ['id']) == None):
                return False
        return True

    # if ANY element in branch is to be merged, need to recalculate entire branch
    def _is_branch_merge_candidate (self, branch):
        for branch_elem in branch:
            for merge_record in self._cat_merge_list:
                if merge_record ['src_cat_id'] == branch_elem ['id']:
                    return merge_record
        return None

    # this is slightly complicated
    # add 'tail' of src-branch and add it to 'head' of target-parent
    # eg, src_branch = 'A/B/C', B is merged in to 'X/Y'
    # new branch = 'X/Y/C'
    # returns branch as expected in BR's category_paths 
    def _build_merged_branch (self, orig_branch, merge_record):
        target_parent_full_crumbid = merge_record ['target_parent_full_crumbid']
        target_parent_full_crumb = merge_record ['target_parent_full_crumb']

        orig_full_crumbid_list = []
        orig_full_crumb_list = []
        for branch_elem in orig_branch:
            orig_full_crumbid_list.append (branch_elem ['id'])
            orig_full_crumb_list.append (branch_elem ['name'])

        # in the list, find indx of src_cat_id. If src_cat_id is a leaf, tail = None
        src_cat_id = merge_record ['src_cat_id']
        indx = orig_full_crumbid_list.index (src_cat_id)
        if indx < (len (orig_full_crumbid_list) - 1):
            orig_crumbid_tail = orig_full_crumbid_list [indx+1:]
            orig_crumb_tail = orig_full_crumb_list [indx+1:]
        else:
            orig_crumbid_tail = None
            orig_crumb_tail = None

        # similarly, convert target crumbid, crumb to lists
        target_parent_full_crumbid_list = target_parent_full_crumbid.split ('>')
        target_parent_full_crumb_list = target_parent_full_crumb.split ('>')

        # add the two: target + src_tail
        if (orig_crumbid_tail != None):
            post_merge_crumbid_list = target_parent_full_crumbid_list + orig_crumbid_tail
            post_merge_crumb_list = target_parent_full_crumb_list + orig_crumb_tail
        else:
            post_merge_crumbid_list = target_parent_full_crumbid_list
            post_merge_crumb_list = target_parent_full_crumb_list

        # build 'branch' as expected in BR catalog
        post_merge_branch = []
        for i in range (0, len (post_merge_crumbid_list)):
            branch_elem = {}
            branch_elem ['id'] = post_merge_crumbid_list [i]
            branch_elem ['name'] = self._category_manager.get_cat_name (branch_elem ['id'])
            post_merge_branch.append (branch_elem)

        return post_merge_branch

    # Algo: build full-crumb-string for each branch's last-elem. Then see if any of those strings is subset of any other
    # Note that a single branch may have many ancestral nodes
    def _curate_ancestral_paths (self, new_category_paths):
        post_ancestral_check_paths = []
        num_branches_in_category_paths = len (new_category_paths)   # all branches assigned for this pid
        full_crumb_ids_of_branch_leaf = []       # eg, '100>200>300'

        for i in range (0,num_branches_in_category_paths):    # go-thru all branchs in this category path
            branch = new_category_paths [i]               # build full-crumb of this branch's last elem
            branch_leaf_elem = branch [len (branch) - 1]
            src_full_crumbid, src_full_crumb = self._category_manager.get_node_full_breadcrumb_and_id (branch_leaf_elem ['id'])
            full_crumb_ids_of_branch_leaf.append (src_full_crumbid) # src_full_crumb not used; only the crumb_ids

        # go thru all such crumb_ids.
        # if ANY full_crumb_id is substring of any other full_crumb_id, remove the earlier
        # eg, if crumb_ids: '100>200', '500>600>700', '100>200>300', then exclude '100>200' since it is parent of '100>200>300'
        for i in range (0, len (full_crumb_ids_of_branch_leaf)):
            full_crumbid1 = full_crumb_ids_of_branch_leaf [i]    # '100>200'
            full_crumbid1_is_subset = False
            for j in range (0, len (full_crumb_ids_of_branch_leaf)):
                if i == j:
                    continue    # don't compare to itself

                full_crumbid2 = full_crumb_ids_of_branch_leaf [j]    # '500>600>700', ..., '100>200>300'

                # in some cases, exact same branch is in category paths (ie, full-crumbid same)
                if (full_crumbid1 == full_crumbid2):
                    # set one of them as '_is_duplicate' and then ignore it in further comparisons
                    full_crumb_ids_of_branch_leaf [j] = '_is_duplicate'
                    continue

                # in a subsequent loop, if crumbid == '_is_duplicate', continue
                if (full_crumb_ids_of_branch_leaf [i] == '_is_duplicate'):
                    full_crumbid1_is_subset = True
                    continue

                # if crumbid1 is subset (ie, it is parent-of crumbid2), 
                # set flag to ignore parent and break
                if (full_crumbid1 in full_crumbid2):
                    full_crumbid1_is_subset = True
                    break

            # finally, if full_crumbid1 is not subset of any other branch, add to final path
            if (full_crumbid1_is_subset == False):
                post_ancestral_check_paths.append (new_category_paths [i])
            else:
                logging.debug ('Excluding crumb due to ancestral duplication: %s' % full_crumbid1)
        return post_ancestral_check_paths

    # category_levels 1, 2, 3, 4
    # use bread_crumb to return list of [l0, l1, ...] of first category
    def _collect_category_levels (self, updated_record):
        category_levels = []

        category_paths = updated_record ['value']['attributes']['category_paths']
        if (category_paths == None) or (len (category_paths) == 0):
            return category_levels # zero-length list

        branch_path_0 = category_paths [0]   # use first branch
        for branch_nodes in branch_path_0:
            category_levels.append (branch_nodes ['name'])
        return category_levels

    def _update_product_counts (self, updated_record):
        category_paths = updated_record ['value']['attributes']['category_paths']
        for branch in category_paths:
            branch_len = len (branch)
            branch_leaf = branch [branch_len - 1]
            leaf_cat_id = branch_leaf ['id']
            self._category_manager.increment_product_count (leaf_cat_id)
        return

    # same method called twice...
    def _lookup_pids_per_cat_record (self, catid):
        pids_per_cat_record = None
        for pids_per_cat_record in self._pids_per_cat: 
            if pids_per_cat_record ['catid'] == catid:
                return pids_per_cat_record
        return None

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV1 ()
    logging.info ('RevisionV1 Finish...')


'''
        self._selected_pid_list = None
        # use the target_category_map and collect products to include in target catalog
        selected_pid_list = self._collect_selected_pid_list (target_category_map)
        if (selected_pid_list == None) or (len (selected_pid_list) == 0):
            logging.warning ('Selected product list is empty')
            return False

        self._selected_pid_list = selected_pid_list
        logging.info ('Target pid count = %s' % len (self._selected_pid_list))

    def _adjust_category_paths (self, updated_record):
        category_paths = updated_record ['value']['attributes']['category_paths']
        target_category_paths = []
        for branch in category_paths:
            target_category_paths.append (branch)

        if len (target_category_paths) > 0:
            updated_record ['value']['attributes']['category_paths'] = target_category_paths
            return updated_record
        else:
            logging.info ('Target category path is empty for pid: %s' % updated_record ['value']['attributes']['pid'])
            return None 

    # go thru merge-list and see if given branch's cat_id is in that list
    # returns the merge_record if cat_id is to be merged
    def _is_branch_elem_in_merge_list (self, branch_elem):
        for merge_record in self._cat_merge_list:
            if merge_record ['src_cat_id'] == branch_elem ['id']:
                return merge_record
        return None 

    def _build_merged_branch (self, orig_branch, merge_record):
        # for this cat_id_to_merge, get corresponding target full crumb, full crumb_id
        src_full_crumbid = merge_record ['src_full_crumbid']
        src_full_crumb = merge_record ['src_full_crumb']

        target_parent_full_crumbid = merge_record ['target_parent_full_crumbid']
        target_parent_full_crumb = merge_record ['target_parent_full_crumb']

        # in order to find src_tail, convert src crumb, crumbid strings to lists
        src_full_crumbid_list = src_full_crumbid.split ('>')    # '>' is used in category manager
        src_full_crumb_list = src_full_crumb.split ('>')    # '>' is used in category manager
        src_cat_id = merge_record ['src_cat_id']

        # in the list, find indx of src_cat_id. If src_cat_id is a leaf, tail = None
        indx = src_full_crumbid_list.index (src_cat_id)
        if indx < (len (src_full_crumbid_list) - 1):
            src_crumbid_tail = src_full_crumbid_list [indx+1:]
            src_crumb_tail = src_full_crumb_list [indx+1:]
        else:
            src_crumbid_tail = None
            src_crumb_tail = None

        # similarly, convert target crumbid, crumb to lists
        target_parent_full_crumbid_list =target_parent_full_crumbid.split ('>')
        target_parent_full_crumb_list = target_parent_full_crumb.split ('>')

        # add the two: target + src_tail
        if (src_crumbid_tail != None):
            post_merge_crumbid_list = target_parent_full_crumbid_list + src_crumbid_tail
            post_merge_crumb_list = target_parent_full_crumb_list + src_crumb_tail
        else:
            post_merge_crumbid_list = target_parent_full_crumbid_list
            post_merge_crumb_list = target_parent_full_crumb_list

        # build 'branch' as expected in BR catalog
        post_merge_branch = []
        for i in range (0, len (post_merge_crumbid_list)):
            branch_elem = {}
            branch_elem ['id'] = post_merge_crumbid_list [i]
            branch_elem ['name'] = post_merge_crumb_list [i]
            post_merge_branch.append (branch_elem)

        return post_merge_branch

                # @@@ REMOVE Add this post-branch only if it is not already in the category_path
                # if (self._is_branch_duplicate (post_merge_branch, category_paths) == False):
                #   post_merge_category_paths.append (post_merge_branch)
                #else:
                #    logging.debug ('Excluding branch due to post-merge duplication: %s' % 
                #                   src_record ['value']['attributes']['pid'])

    # check to see if 'branch-to-add' is already in category_paths
    def _is_branch_duplicate (self, branch_to_add, category_paths):
        branch_to_add_last_elem = branch_to_add [len (branch_to_add) - 1]
        full_branch_crumbid_to_add, unused = self._category_manager.get_node_full_breadcrumb_and_id (branch_to_add_last_elem ['id'])

        for current_branch in category_paths:
            current_branch_last_elem = current_branch [len (current_branch) - 1]
            full_branch_crumbid_existing, unused = self._category_manager.get_node_full_breadcrumb_and_id (current_branch_last_elem ['id'])
            if (full_branch_crumbid_existing == full_branch_crumbid_to_add):
                return True # yes, the branch-to-add already exists in this category path
        return False

    # check to see if 'branch-to-add' is already in category_paths
    def _is_branch_duplicate_PREV (self, branch_to_add, category_paths):
        full_branch_crumbid_to_add = ''
        for branch_to_add_elem in branch_to_add:
            full_branch_crumbid_to_add = '%s>%s' % (full_branch_crumbid_to_add, branch_to_add_elem ['id'])
            # @@@
            logging.debug ('branch-to-add crumbid: %s' % full_branch_crumbid_to_add)

        for current_branch in category_paths:
            full_branch_crumbid_existing = ''
            for current_branch_elem in current_branch:
                full_branch_crumbid_existing = '%s>%s' % (full_branch_crumbid_existing, current_branch_elem ['id'])
                if (full_branch_crumbid_existing == full_branch_crumbid_to_add):
                    # @@@
                    logging.debug ('branch-to-add is duplicate: %s' % full_branch_crumbid_existing)
                    return True # yes, the branch-to-add already exists in this category path
        return False

    # Algo: build full-crumb-string for each branch's last-elem. Then see if any of those strings is subset of any other
    def _curate_ancestral_paths (self, new_category_paths):
        post_ancestral_check_paths = []
        num_branches_in_category_paths = len (new_category_paths)   # all branches assigned for this pid
        full_crumb_ids_of_branch_leaf = []       # eg, '100>200>300'

        for i in range (0,num_branches_in_category_paths):    # go-thru all branchs in this category path
            branch = new_category_paths [i]               # build full-crumb of this branch's last elem
            branch_leaf_elem = branch [len (branch) - 1]
            src_full_crumbid, src_full_crumb = self._category_manager.get_node_full_breadcrumb_and_id (branch_leaf_elem ['id'])
            full_crumb_ids_of_branch_leaf.append (src_full_crumbid) # src_full_crumb not used; only the crumb_ids

        # go thru all such crumb_ids.
        # if ANY full_crumb_id is substring of any other full_crumb_id, remove the earlier
        # eg, if crumb_ids: '100>200', '500>600>700', '100>200>300', then exclude '100>200' since it is parent of '100>200>300'
        for i in range (0, len (full_crumb_ids_of_branch_leaf)):
            full_crumbid1 = full_crumb_ids_of_branch_leaf [i]    # '100>200'
            full_crumbid1_is_subset = False
            for j in range (0, len (full_crumb_ids_of_branch_leaf)):
                if i == j:
                    continue    # don't compare to itself

                full_crumbid2 = full_crumb_ids_of_branch_leaf [j]    # '500>600>700', ..., '100>200>300'

                # in some cases, exact same branch is in category paths (ie, full-crumbid same)
                if (full_crumbid1 == full_crumbid2):
                    # set one of them as '_is_duplicate' and then ignore it in further comparisons
                    full_crumb_ids_of_branch_leaf [j] = '_is_duplicate'
                    continue

                # in a subsequent loop, if crumbid == '_is_duplicate', continue
                if (full_crumb_ids_of_branch_leaf [i] == '_is_duplicate'):
                    full_crumbid1_is_subset = True
                    continue

                # if crumbid1 is subset (ie, it is parent-of crumbid2), 
                # set flag to ignore parent and break
                if (full_crumbid1 in full_crumbid2):
                    full_crumbid1_is_subset = True
                    break

            # finally, if full_crumbid1 is not subset of any other branch, add to final path
            if (full_crumbid1_is_subset == False):
                post_ancestral_check_paths.append (new_category_paths [i])
            else:
                logging.debug ('Excluding crumb due to ancestral duplication: %s' % full_crumbid1)
        return post_ancestral_check_paths

            # also collect in pid-per-cat list (used later to ensure max-pids-in-cat)
            self._add_pid_in_catlist_if_needed  (updated_record ['value']['attributes']['pid'], leaf_cat_id)
    # ensure pid is associated only once in a cat. 
    # Returns pids-per-cat-record (if any) for given catid
    def _add_pid_in_catlist_if_needed (self, new_pid, catid):
        # first get pids_per_cat_record for given catid
        pids_per_cat_record = self._lookup_pids_per_cat_record (catid)
        if pids_per_cat_record == None:
            pids_per_cat_record = {'catid': catid,
                                   'pids': []
                                  }
        else:            
            for pid in pids_per_cat_record ['pids']:
                if new_pid == pid:
                    return  # new_pid already in pids list

        # coming here means new_pid is not in this pids-per-cat-record
        pids_per_cat_record ['pids'].append (new_pid)
        return
'''

