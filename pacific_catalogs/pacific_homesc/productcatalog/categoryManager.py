import logging
from bigtree import Node, find, preorder_iter, tree_to_dict, shift_nodes

class CategoryManager ():

    ACTION_KEEP = 'keep'
    ACTION_DELETE = 'delete'
    ACTION_MERGE = 'merge'

    def __init__ (self):
        self._root = None
        return

    # category tree built from the catalog
    def build_tree (self, source_catalog):
        self._root = Node ('Root', 
                           catid = '0', 
                           catname = '', 
                           product_count = 0, 
                           parent = None, 
                           action = CategoryManager.ACTION_KEEP)
    
        for record in source_catalog:
            # logging.debug ('Record pid = %s' % record ['value']['attributes']['pid'])
            category_paths = record ['value']['attributes']['category_paths']
            for branch in category_paths:
                parent_node = self._root
                tab_count = 0
                for leaf in branch:
                    leaf_node = self.find_node_by_id (leaf ['id'])
                    if (leaf_node == None):
                        # bigtree does not like multiple nodes with same name
                        node_name = '%s:%s' % (leaf ['name'], leaf ['id'])
                        leaf_node = Node (name = node_name, 
                                          catid = leaf ['id'], 
                                          catname = leaf ['name'], 
                                          product_count = 0, 
                                          parent = parent_node, 
                                          action = CategoryManager.ACTION_KEEP)
                    parent_node = leaf_node
                    tab_count = tab_count + 1
        # self._root.show (attr_list = ['catid'])
        return self._root

    def find_node_by_id (self, cat_id):
        # logging.debug ('find category node: %s', cat_id)
        node = find (self._root, lambda node: node.catid == cat_id)
        return node
    
    def remove_node_by_id (self, cat_id):
        # logging.debug ('Removing category node: %s', cat_id)
        node = self.find_node_by_id (cat_id)
        return (self.remove_node (node))

    # setting to paths = None => node removed from tree, including node children (ie, entire sub-tree)
    def remove_node (self, node):
        if (node != None):
            node_path = node.path_name
            shift_nodes (self._root, from_paths = [node_path], to_paths = [None])
        return True
 
    def increment_product_count (self, cat_id):
        node = self.find_node_by_id (cat_id)
        if (node != None):
            node.product_count = node.product_count + 1
        return

    def get_product_count (self, cat_id):
        node = self.find_node_by_id (cat_id)
        if (node != None):
            return (node.product_count)
        return None

    def decrement_product_count (self, cat_id):
        node = self.find_node_by_id (cat_id)
        if (node != None):
            node.product_count = node.product_count - 1
        return

    def update_node_action (self, cat_id, upd_action):
        node = self.find_node_by_id (cat_id)
        if (node != None):
            node.action = upd_action
        return

    def get_node_action (self, cat_id):
        node = self.find_node_by_id (cat_id)
        if (node != None):
            return (node.action)
        return None

    # note: cat_name is NOT node_name. The latter is for BigTree
    def update_cat_name (self, cat_id, upd_cat_name):
        node = self.find_node_by_id (cat_id)
        if (node != None):
            node.catname = upd_cat_name
        return

    # note: cat_name is NOT node_name. The latter is for BigTree
    def get_cat_name (self, cat_id):
        node = self.find_node_by_id (cat_id)
        if (node != None):
            return node.catname
        return None


    # move src node's children to target node
    # then remove src node
    def merge_node_into_target (self, src_cat_id, target_cat_id):
        src_node = self.find_node_by_id (src_cat_id)
        target_node = self.find_node_by_id (target_cat_id)
        if (src_node != None) and (target_node != None):
            src_node_children = src_node.children
            if len (src_node_children) > 0:
                src_children_list = list (src_node_children)
                target_node.extend (src_children_list)

            # do not remove src node yet. It needs to exist so that branch
            # containing it is considered 'valid'. Set the children to 0
            # self.remove_node (src_node)
            src_node.children = []
            return True
        return False

    # full-bread-crumb_id as used in BR (ie, '100>200>300' and 'A>B>C')
    # needed to get full target-path for merged categories
    def get_node_full_breadcrumb_and_id (self, cat_id):
        bread_crumb = None
        bread_crumb_id = None
        node = self.find_node_by_id (cat_id)
        if (node != None):
            node_path = node.path_name  # name:id/name:id/...
            path_split = node_path.split ('/')
            for elem in path_split:
                indx = elem.find (':')
                if (indx < 0):
                    continue    # 'blank', 'Root'

                cat_name = elem [:indx]
                cat_id = elem [indx+1:]
                if bread_crumb_id == None:
                    bread_crumb_id = '%s' % cat_id
                    bread_crumb = '%s' % cat_name
                else:
                    bread_crumb_id = '%s>%s' % (bread_crumb_id, cat_id)
                    bread_crumb = '%s>%s' % (bread_crumb, cat_name)
        return (bread_crumb_id, bread_crumb)    # tuple

    def remove_empty_nodes (self):
        self._remove_node_if_empty (self._root)
        return

    def is_ancestor_of (self, child_cat_id, target_cat_id):
        child_node = self.find_node_by_id (child_cat_id)
        if (child_node != None):
            parent_node = child_node.parent
            while parent_node.catid != '0':    # ROOT catid is set to 0
                if parent_node.catid == target_cat_id:
                    return True
                parent_node = parent_node.parent
        return False

    def write_tsv (self, outputfile_path):
        print_lines = []

        self._prepare_print_lines (self._root, -1, print_lines) # tabcount = -1 since root line is not printed
        with open (outputfile_path, 'w') as outputfile:
            header_line = '%s\t%s\t%s\t%s...\n' % ('CatId', 'ProductCount', 'Action', 'category_levels L0, L1, ...')
            outputfile.write (header_line)

            for line in print_lines:
                outputfile.write (line + '\n')
            outputfile.flush ()
            outputfile.close ()
        return

    # recursive method
    def _remove_node_if_empty (self, node):
        if node.is_leaf:
            if node.product_count == 0:
                logging.debug ('Removing empty leaf: %s' % node.catid)
                self.remove_node (node)
        else:
            children = node.children
            for child in children:
                self._remove_node_if_empty (child)  # recurse
            if len (node.children) == 0:
                logging.debug ('Removing empty parent: %s' % node.catid)
                self.remove_node (node)
        return

    # recursive method
    def _prepare_print_lines (self, node, tab_count, print_lines):
        if node.is_leaf:
            if (node.action != CategoryManager.ACTION_MERGE):
                print_line = self._prepare_line_for_node (node, tab_count)
                print_lines.append (print_line)
        else:
            # prepare own print_line (exclude 'root' and nodes that have been merged in to another node)
            if (node.catid != '0') and (node.action != CategoryManager.ACTION_MERGE):
                print_line = self._prepare_line_for_node (node, tab_count)
                print_lines.append (print_line)

            # go thru child-nodes
            for child in node.children:
                self._prepare_print_lines (child, tab_count+1, print_lines)
        return

    def _prepare_line_for_node (self, node, tab_count):
        leading_tabs = ''
        for i in range (0, tab_count):
            leading_tabs = leading_tabs + '\t'

        if node.product_count == 0:
            product_count_str = '-'
        else:
            product_count_str = '%s' % node.product_count 
        print_line = '%s\t%s\t%s\t%s%s' % (node.catid, 
                                           product_count_str, 
                                           node.action, 
                                           leading_tabs,
                                           node.catname)
        return print_line

if __name__ == '__main__':
    import jsonlFeedReader as jfr
    import updaterConstants as uc

    logging.basicConfig (level = logging.DEBUG)
    srcFeedHandler = jfr.JsonlFeedReader ()
    srcProducts = srcFeedHandler.readSourceFeed (uc.FILENAME_JSONL_SOURCE_FEED_IN) 

    cm = CategoryManager ()
    logging.debug ('Building category tree...')
    cm.build_tree (srcProducts)
    logging.debug ('Building category tree... done')

    # cm.update_node_action ('117079', CategoryManager.ACTION_MERGE)
    # print (cm.get_node_action ('117079'))
    # cm.remove_sub_tree ('116899')
    # cm.remove_node_by_id ('116900')
    # bread_crumb_id, bread_crumb = cm.get_node_full_breadcrumb_and_id ('116756')
    # cm.merge_node_into_target ('116926', '117079')
    # cm.write_tsv (uc.FILENAME_CATEGORY_TREE_TSV_OUT)
    op_stat = cm.is_ancestor_of ('116753', '116746')
    print ('is_ancestor: %s' % op_stat)
    op_stat = cm.is_ancestor_of ('116753', '116715')
    print ('is_ancestor: %s' % op_stat)
    op_stat = cm.is_ancestor_of ('116886', '116898')
    print ('is_ancestor: %s' % op_stat)


    logging.info ('Finish...')

'''
#     # remove subtree including node for 'cat_id'
#     # bigtree does not have a built-in method to do this
#     def remove_sub_tree (self, cat_id):
#         node = self.find_node_by_id (cat_id)
#         if (node != None):
#             siblings = node.siblings   # remaining child-nodes in parent
#             node_parent = node.parent
#             node_parent.children = siblings 
#         return
# 
#             src_node_path = src_node.path_name
#             target_node_path = target_node.path_name
#             shift_nodes (self._root,
#                          [src_node_path], 
#                          [target_node_path], 
#                          merge_children = True,  # merge src_node's children into target_node, not src_node itself
#                          overriding = False,     # keep original children in the target_node as-is
#                          sep = '/' 
#                         )
# 
#     def write_tsv_PREV (self, outputfile_path):
#         tree_dict = tree_to_dict (self._root, all_attrs = True)
#         with open (outputfile_path, 'w') as outputfile:
#             header_line = '%s\t%s\t%s\t%s...\n' % ('CatId', 'ProductCount', 'Action', 'category_levels L0, L1, ...')
#             outputfile.write (header_line)
#             for node_data in tree_dict.values ():
#                 if node_data ['catid'] == '0':
#                     continue
#                 if node_data ['product_count'] == 0:
#                     product_count_str = '-'
#                 else:
#                     product_count_str = '%s' % node_data ['product_count'] 
#                 tsv_line = '%s\t%s\t%s\t%s%s\n' % (node_data ['catid'], 
#                                                    product_count_str, 
#                                                    node_data ['action'], 
#                                                    node_data ['tsv_line'],
#                                                    node_data ['catname'])
#                 outputfile.write (tsv_line)
#             outputfile.flush ()
#             outputfile.close ()
#       return
'''
