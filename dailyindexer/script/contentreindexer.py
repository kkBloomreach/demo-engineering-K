# Main entry point for content feed reindexer
# NOTE: PYTHONPATH (aka sys.path) is set in __init__.py

import os
import json
import logging

# **** NOTE: Cannot use jsonlines package because it is not available in Debian unix.
# The google cloud account (GCP) has Debian OS

from utils import credentials as cr
from utils import jsonlFeedReaderLocal as jfrl
from utils import jsonlWriterLocal as jwl
from dataconnect import ingestAndIndex as dc_igx

class ContentReindexer ():

    def __init__ (self, account_config, command_line):
        self._account_config = account_config
        self._command_line = command_line
        return

    def reingest_and_index (self):
        # read source feed - expected to be in dataConnect JSONL format
        data_dir = self._command_line.getDataDir ()

        try:
            feed_file_name = self._account_config ["CATALOG_FILE_JSONL"]
            feed_source_path = '%s/input/%s' % (data_dir, feed_file_name)
            if (os.path.isfile (feed_source_path) == False):
                logging.error ('Cannot find source feed file: %s' % feed_source_path)
                return False

            logging.info ('Catalog file path: %s' % feed_source_path)
            ingester = dc_igx.IngestAndIndex (self._command_line)
            job_id = ingester.ingest_and_index_catalog (feed_source_path, self._account_config)
            logging.debug ('Ingest job id: %s' % job_id)
            if (job_id == -1):
                logging.error ('Content ingest_and_Index operation failed for: %s' % cr.Credentials.getBloomreachAccountName (self._account_config))
                return False

        except Exception as e:
            logging.error ('Exception in content feed ingest_and_index for: %s, message: %s' % \
                                    (cr.Credentials.getBloomreachAccountName (self._account_config), str(e)))
            return False

        return True

    def _loadSourceFeed (self, feed_source_path):
        if (os.path.exists (feed_source_path) == False):
            logging.error ('Source feed file does not exist: %s', feed_source_path)
            return None

        srcFeedHandler = jfrl.JsonlFeedReaderLocal ()
        srcProducts = srcFeedHandler.readSourceFeed (feed_source_path)
        return srcProducts

    def _writeJsonlFeed (self, updatedProducts, feed_path):
        if os.path.exists (feed_path):
            os.remove (feed_path)   ## remove temp feed file; happens if previous indexer failed

        # full feed
        feedWriter = jwl.JsonlWriterLocal ()
        feedWriter.setProducts (updatedProducts)
        feedWriter.write (feed_path)
        return

if __name__ == "__main__":
    # parse command line 
    content_reindexer = ContentReindexer (None, None)
    content_reindexer.reingest_and_index ()

