import logging
import jsonlines
from bigtree import Node, find, preorder_iter, tree_to_dict, shift_nodes

# FILENAME_JSONL_SOURCE_FEED_IN = './data/input/ph2_product_en_full_12222025_10.jsonl'
# FILENAME_TSV_CATEGORYTREE_OUT = './data/output/ph2_product_en_category_tree_122222025.tsv'
FILENAME_JSONL_SOURCE_FEED_IN = './data/input/ps_product_en_full_02122025.jsonl'
FILENAME_TSV_CATEGORYTREE_OUT = './data/output/ps_product_en_category_tree_02122025.tsv'

CATEGORY_STATUS_KEEP = 0

class CategoryManager ():
    def __init__ (self):
        self._root = None
        return

    def read_catalog (self, catalog_file):
        source_catalog = []
        with jsonlines.open (catalog_file) as reader:
            for product in reader:
                source_catalog.append (product)
            reader.close ()
        return source_catalog
    
    # category tree built from the catalog
    def build_tree (self, source_catalog):
        self._root = Node ('Root', catid='0')
        self._root.set_attrs ({'tsv_line': ''})
    
        for record in source_catalog:
            # logging.debug ('Record pid = %s' % record ['value']['attributes']['pid'])
    
            category_paths = record ['value']['attributes']['category_paths']

            # full path -- needed to remove a node
            full_path = None
            for branch in category_paths:
                parent_node = self._root
                tab_count = 0
                for leaf in branch:
                    if (full_path == None):
                        full_path = leaf ['name']
                    else:
                        full_path = '%s/%s' % (full_path, leaf ['name'])

                    leaf_node = find (parent_node, lambda node: node.catid == leaf ['id'])
                    if (leaf_node == None):
                        # bigtree does not like multiple nodes with same name
                        node_name = '%s:%s' % (leaf ['name'], leaf ['id'])

                        # node status (remove/keep/)    
                        cat_status = CATEGORY_STATUS_KEEP

                        # tsv line
                        tsv_line = ''
                        for i in range (tab_count):
                            tsv_line = '%s%s' % (tsv_line, '	')
                        tsv_line = '%s  %s' % (tsv_line, leaf ['name'])

                        leaf_node = Node (node_name, catid = leaf ['id'], tsv_line = tsv_line, parent = parent_node, full_path = full_path, status = cat_status)
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
   
    def write_tsv (self, outputfile):
        tree_dict = tree_to_dict (self._root, all_attrs = True)
        with open (outputfile, 'w') as outputfile:
            header_line = '%s\t%s...\n' % ('CatId', 'category_levels L0, L1, ...')
            outputfile.write (header_line)
            for full_path_key, node_data in tree_dict.items ():
                if node_data ['catid'] == '0':
                    continue
                tsv_line = '%s\t%s\n' % (node_data ['catid'], node_data ['tsv_line'])
                outputfile.write (tsv_line)
            outputfile.flush ()
            outputfile.close ()
        return
 
if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    logging.info ('Start building category tree...')

    cm = CategoryManager ()
    source_catalog = cm.read_catalog (FILENAME_JSONL_SOURCE_FEED_IN)
    root = cm.build_tree (source_catalog)
    if root != None:
        cm.write_tsv (FILENAME_TSV_CATEGORYTREE_OUT)
    logging.info ('Finish...')


