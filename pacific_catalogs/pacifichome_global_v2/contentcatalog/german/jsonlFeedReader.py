# jsonl feed reader

import jsonlines
import logging

class JsonlFeedReader ():

    def __init__ (self):
        self._source_product_records = []    # records from input

    def readSourceFeed (self, srcPath):
        logging.info ("Reading source feed (jsonl) ")
        # read source jsonl. Adds records to brsm_product_records list
        self._read_jsonl (srcPath)
        return (self._source_product_records)

    def _read_jsonl (self, filename):
        with open (filename, 'r') as feedJsonl:
            reader = jsonlines.Reader (feedJsonl)
            for aProduct in reader:
                self._source_product_records.append (aProduct)

            feedJsonl.close ()

        logging.info ('BRSM product record count: %s', len (self._source_product_records))
        return

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    logging.debug ('Finish...')

