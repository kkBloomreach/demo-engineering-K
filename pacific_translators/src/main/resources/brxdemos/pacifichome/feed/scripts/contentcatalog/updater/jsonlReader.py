# jsonl reader

import jsonlines
import logging

class JsonlReader ():

    def __init__ (self):
        self._source_product_records = []    # records from input

    def readSource (self, srcPath):
        logging.info ("Reading source (jsonl) ")
        # read source jsonl. Adds records to brsm_product_records list
        self._read_jsonl (srcPath)
        return (self._source_product_records)

    def _read_jsonl (self, filename):
        with open (filename, 'r') as fileJsonl:
            reader = jsonlines.Reader (fileJsonl)
            for aProduct in reader:
                self._source_product_records.append (aProduct)

            fileJsonl.close ()

        logging.info ('BRSM product record count: %s', len (self._source_product_records))
        return

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    testReader = JsonlReader ()
    records = testReader.readSource ('./data/full_feed_converted_09212023.jsonl')
    logging.debug ('Finish...')

