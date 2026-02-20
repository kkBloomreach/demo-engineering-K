import json
import logging

class JsonlFeedReaderLocal ():

    def __init__ (self):
        return

    def readSourceFeed (self, source_path):
        source_products = []
        with open (source_path, 'r') as source_file:
            for line in source_file:
                product = json.loads (line)
                source_products.append (product)
        return source_products

if __name__ == '__main__':
    jfrl = JsonFeedReaderLocal ()
    products = jfrl.readSourceFeed ('../data/input/ps_product_de_subset_11012023_me_processed.jsonl')
    print (len (products))
    print ('Finish...')

