# category .tsv format changed in V12
import logging
import csv

import updaterConstants as uc

class CategoryBuilder ():

    BREAD_CRUMB_WOMEN_DRESSES = 'women>dresses'
    BREAD_CRUMB_GENERAL_PRODUCTS = 'general>products'
    BREAD_CRUMB_HANDBAGS_PURSES = 'handbags>purses'
    BREAD_CRUMB_ID_GENERAL_PRODUCTS = '999999>99999901'
    BREAD_CRUMB_WOMEN_TOPS = 'women>tops'

    _category_map = None

    # internally built list 
    # {leafId, full_crumb_id, full_crumb}
    # value_delimiter: '|'
    # parent_delimiter: '>'
    # example: Men>Shirt|Sale>Apparel
    # list of {leafId, leafName, fullCrumbId, fullCrumb}
    _category_tree_internal = None

    def __init__ (self):
        return

    def load_category_map (self, path_name):
        self._category_map = []
        with open (path_name, 'r') as categorymap_file:
            dict_reader = csv.DictReader (categorymap_file, delimiter='\t')
            for row in dict_reader:
                if (row ['Parent'] == None) or (row ['Parent'] == ''):  # blank src line
                    continue

                self._category_map.append (row)
            categorymap_file.close ()

        # after reading .tsv, build internal category_tree
        self._build_internal_category_tree ()

        #logging.debug ('\nInternal category tree')
        #for row in self._category_tree_internal:
        #    logging.debug ('%s, %s, %s, %s' % (row ['leaf_id'], row ['leaf_name'], row ['full_crumb_id'], row ['full_crumb'])) 
        #logging.debug ('---\n')

        return 

    # product_record has 'category_paths'. In order to compare that with value in inject_record, 
    # we build a crumb string (eg, 'A>B|C>D') from category_path and compare that
    def construct_crumbs_and_crumbIds_from_category_path (self, product_record):
        category_paths = product_record ['value']['attributes']['category_paths']
        full_crumb = None
        full_crumb_id = None

        # since multiple leaves may have same name, first build a 'branch-crumb'
        # then use it to get corresponding branch-crumb-id. 
        for branch in category_paths:
            branch_crumb = None
            branch_crumb_id = None
            for leafNode in branch:
                if branch_crumb == None:
                    branch_crumb = leafNode ['name']
                    branch_crumb_id = leafNode ['id']
                else:
                    branch_crumb = '%s>%s' % (branch_crumb, leafNode ['name'])
                    branch_crumb_id = '%s>%s' % (branch_crumb_id, leafNode ['id'])

            # collect all branch_crumbs into full crumb (delimiter = '|')
            if full_crumb == None:
                full_crumb = branch_crumb
                full_crumb_id = branch_crumb_id
            else:
                full_crumb = '%s|%s' % (full_crumb, branch_crumb)
                full_crumb_id = '%s|%s' % (full_crumb_id, branch_crumb_id)

        # if all previously used crumbs in categoryPath are now 'removed', set entire path to 'general>products'
        if (full_crumb == None):
            full_crumb = self.BREAD_CRUMB_GENERAL_PRODUCTS
            full_crumb_id = self.BREAD_CRUMB_ID_GENERAL_PRODUCTS
        return (full_crumb, full_crumb_id)


    # This method is reverse of above 'construct' method. This method takes
    # input param: 'A>B|C>D|...'
    #   input may contain duplicate crumbs (eg, A>B|C>D|A>B|...)
    # returns category_paths
    # IMPORTANT - multiple leaves in category may have same name (but not id)
    # ALSO IMPORTANT - in case of crumb name or id change, the param uses 'newName' (and/or newId)
    def construct_category_path_from_crumbs_and_crumbIds (self, all_edited_full_crumbs):
        edited_category_paths = []

        all_edited_branch_crumbs_list = all_edited_full_crumbs.split ('|')
        for a_edited_branch_crumb in all_edited_branch_crumbs_list:   # A>B
            edited_category_branch_path = []
            a_edited_branch_crumb_id = self._lookup_branch_crumb_ids (a_edited_branch_crumb)    # corresponding id's from internal tree
            if (a_edited_branch_crumb_id == None):
                logging.error ('Incorrect bread crumb, replacing : %s to general>products', a_edited_branch_crumb)
                edited_leaf_node = { 'name': self.BREAD_CRUMB_GENERAL_PRODUCTS,
                                     'id': self.BREAD_CRUMB_ID_GENERAL_PRODUCTS
                            }
                edited_category_branch_path.append (edited_leaf_node)
                continue

            edited_leaf_crumbs = a_edited_branch_crumb.split ('>')
            edited_leaf_crumb_ids = a_edited_branch_crumb_id.split ('>')
            for i in range (0, len (edited_leaf_crumbs)):
                edited_leaf_node = { 'name': edited_leaf_crumbs [i],
                                     'id': edited_leaf_crumb_ids [i]
                                   }
                edited_category_branch_path.append (edited_leaf_node)

            # append branch_path to entire category_path
            edited_category_paths.append (edited_category_branch_path)
        return edited_category_paths

    # following method is used to get product-sizes/fits associated with a leaf_id
    # returns record as-is from the category.tsv file
    def lookup_category_map_record (self, leaf_id):
        for map_record in self._category_map:
            if (map_record ['LeafId'] == leaf_id):
                return map_record
        return None

    # following method is used to get "category_paths" for a given 'leaf_name'
    # this assumes given 'leaf_name' is unique across all categories (eg, 'cleanser')
    # otherwise return value can be wrong (ie, incorrect category_path)
    def lookup_category_path_for_leaf_name (self, leaf_name):
        category_path = None

        # lookup internal category_tree for given leaf_name, pick the first entry found 
        for internal_tree_record in self._category_tree_internal:
            if internal_tree_record ['leaf_name'] == leaf_name:
                full_crumb = internal_tree_record ['full_crumb']
                # use this full_crumb to build category_path
                category_path = self.construct_category_path_from_crumbs_and_crumbIds (full_crumb)
                break
        return category_path

    def lookup_category_id_for_leaf_name (self, leaf_name):
        leaf_id = None
        # lookup internal category_tree for given leaf_name, pick the first entry found 
        for internal_tree_record in self._category_tree_internal:
            if internal_tree_record ['leaf_name'] == leaf_name:
                full_crumb = internal_tree_record ['full_crumb']
                # use this full_crumb to build category_path
                leaf_id = internal_tree_record ['leaf_id']
                break
        return leaf_id
 


    # INTERNAL METHODS
    # .tsv columns are:
    # Parent, L0Name, L1Name, LeafId. Remaining columns ignored (eg, product_size)
    def _build_internal_category_tree (self):
        self._category_tree_internal = []
        for row in self._category_map:
            parent_id = row ['Parent']
            l0_name = row ['L0Name']
            l1_name = row ['L1Name']
            l2_name = row ['L2Name']
            leaf_id = str (row ['LeafId'])

            leaf_name = None
            full_crumb = None
            full_crumb_id = None
                
            if l0_name != '':
                leaf_name = l0_name.lower ()
                full_crumb_id = leaf_id
                full_crumb = l0_name.lower ()
            elif l1_name != '':
                parent_full_crumb, parent_full_crumb_id = self._lookup_parent_crumb_and_crumbid (parent_id)
                if (parent_full_crumb == None) or (parent_full_crumb_id == None):
                    logging.error ('Cannot find parent id %s' % parent_id)
                    continue
                leaf_name = l1_name.lower ()
                full_crumb_id = '%s>%s' % (parent_full_crumb_id, leaf_id)
                full_crumb = '%s>%s' % (parent_full_crumb, l1_name.lower())
            elif l2_name != '':
                parent_full_crumb, parent_full_crumb_id = self._lookup_parent_crumb_and_crumbid (parent_id)
                if (parent_full_crumb == None) or (parent_full_crumb_id == None):
                    logging.error ('Cannot find parent id %s' % parent_id)
                    continue
                leaf_name = l2_name.lower ()
                full_crumb_id = '%s>%s' % (parent_full_crumb_id, leaf_id)
                full_crumb = '%s>%s' % (parent_full_crumb, l2_name.lower())

            tree_node = { 'leaf_id' : leaf_id,
                          'leaf_name': leaf_name,
                          'full_crumb_id': full_crumb_id,
                          'full_crumb': full_crumb
                        }

            self._category_tree_internal.append (tree_node)
        return

    def _lookup_parent_crumb_and_crumbid (self, parent_id):
        for row in self._category_tree_internal:
            if row ['leaf_id'] == parent_id:
                return row ['full_crumb'], row ['full_crumb_id']    # parent's full_crumb, full_crumb_id
        return None, None

    # for a full branch_crumb (eg, A>B), return corresponding crumbIds (eg, 100>200)
    # NOTE: multiple leaves in a category may have the same name (but not id)
    def _lookup_branch_crumb_ids (self, branch_crumbs):
        for tree_row in self._category_tree_internal:
            if (tree_row ['full_crumb'] == branch_crumbs):
                return tree_row ['full_crumb_id']
        return None

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    c = CategoryBuilder ()
    c.load_category_map (uc.FILENAME_CATEGORY_MAP)

    m = c.lookup_category_map_record ('30400')
    if m != None:
        logging.info ('leafName=%s, leafId=%s' % (m ['L1Name'], m ['LeafId']))

    m = c.lookup_category_map_record ('1002B0')
    if m != None:
        logging.info ('leafName=%s, leafId=%s' % (m ['L2Name'], m ['LeafId']))


    catpath = c.construct_category_path_from_crumbs_and_crumbIds ("kids>accessories|jewellery>rings|handbags>backpacks&totes|men>suits")
    logging.info ('catPath: %s' % catpath)

    catpath = c.construct_category_path_from_crumbs_and_crumbIds ("handbags>backpacks&totes|men>suits|health & beauty>skincare>serum")
    logging.info ('catPath: %s' % catpath)

    catpath = c.lookup_category_path_for_leaf_name ('foundation')
    logging.info ('catPath: %s' % catpath)

    logging.info ('Finish...')

