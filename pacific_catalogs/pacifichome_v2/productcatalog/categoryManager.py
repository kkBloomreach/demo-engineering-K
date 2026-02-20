import logging
import jsonlines
from bigtree import Node, find, preorder_iter, tree_to_dict, shift_nodes

import revisionConstantsV0 as rcv0

class CategoryManager ():

    _root = None

    def __init__ (self):
        return

    # category tree built from the catalog
    def build_tree (self, source_catalog):
        self._root = Node ('Root', tsv_line = '', catid = '0', product_count = 0)
    
        for record in source_catalog:
            # logging.debug ('Record pid = %s' % record ['value']['attributes']['pid'])
    
            category_paths = record ['value']['attributes']['category_paths']

            # full path -- needed to remove a node
            for branch in category_paths:
                parent_node = self._root
                tab_count = 0
                for leaf in branch:
                    leaf_node = self.find_node_by_id (leaf ['id'])
                    if (leaf_node == None):
                        # bigtree does not like multiple nodes with same name
                        node_name = '%s:%s' % (leaf ['name'], leaf ['id'])

                        # product count in this node
                        product_count = 0 

                        # tsv line
                        tsv_line = ''
                        for i in range (tab_count):
                            tsv_line = '%s%s' % (tsv_line, '	')
                        tsv_line = '%s  %s' % (tsv_line, leaf ['name'])

                        leaf_node = Node (name = node_name, catid = leaf ['id'], tsv_line = tsv_line, product_count = product_count, parent = parent_node)
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

    def remove_node (self, node):
        if (node != None):
            node_path = node.path_name
            shift_nodes (self._root, from_paths = [node_path], to_paths = [None])
        return True

    def update_product_count (self, cat_id):
        node = self.find_node_by_id (cat_id)
        if (node != None) and (node.is_leaf):
            product_count = node.product_count
            node.product_count = product_count + 1
        return

    def remove_empty_nodes (self):
        self._remove_node_if_empty (self._root)
        return

    def write_tsv (self, outputfile):
        tree_dict = tree_to_dict (self._root, all_attrs = True)
        with open (outputfile, 'w') as outputfile:
            header_line = '%s\t%s\t%s...\n' % ('CatId', 'ProductCount', 'category_levels L0, L1, ...')
            outputfile.write (header_line)
            for node_data in tree_dict.values ():
                if node_data ['catid'] == '0':
                    continue
                if node_data ['product_count'] == 0:
                    product_count_str = '-'
                else:
                    product_count_str = '%s' % node_data ['product_count'] 
                tsv_line = '%s\t%s\t%s\n' % (node_data ['catid'], product_count_str, node_data ['tsv_line'])
                outputfile.write (tsv_line)
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

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    cm = CategoryManager ()
    cm.build_tree (None)

    logging.info ('Finish...')


