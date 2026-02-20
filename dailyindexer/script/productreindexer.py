# NOTE: PYTHONPATH (aka sys.path) is set in __init__.py

import os
import json
import logging
import copy

# **** NOTE: Cannot use jsonlines package because it is not available in Debian unix.
# The google cloud account (GCP) has Debian OS

from utils import credentials as cr
from utils import jsonlFeedReaderLocal as jfrl
from utils import jsonlWriterLocal as jwl
from dataconnect import ingestAndIndex as dc_igx
from datahub import ingestAndIndex as dh_igx

TEMP_ADJUSTED_FEED_FILENAME = 'temp_adjusted_feed.jsonl'

class ProductReindexer ():

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

            # adjust product url as per account config
            logging.info ('Catalog file path: %s' % feed_source_path)
            mode = cr.Credentials.getBloomreachIndexMode (self._account_config)   # dataconnect or datahub
            adjusted_feed_source_path = self._adjust_product_urls (feed_source_path, data_dir, self._account_config)
            if (adjusted_feed_source_path != None):
                if mode == 'dataconnect':
                    op_stat = self._perform_discovery_dataconnect_reindex (adjusted_feed_source_path, self._account_config)
                elif mode == 'datahub':
                    op_stat = self._perform_datahub_reindex (adjusted_feed_source_path, self._account_config)
                if (op_stat == False):
                    logging.error ('Ingest_and_Index operation failed for: %s' % \
                                    cr.Credentials.getBloomreachAccountName (self._account_config))
                    return False
                else:
                    os.remove (adjusted_feed_source_path)   ## remove temp feed file
            else:
                logging.error ('Product ingest_and_index adjust feed failed for: %s' \
                                    % cr.Credentials.getBloomreachAccountName (self._account_config))
                return False
        except Exception as e:
            logging.error ('Exception in ingest_and_index for: %s, message: %s' % \
                            (cr.Credentials.getBloomreachAccountName (self._account_config), str(e)))
            return False
        return True

    # read source products, change each product's url, save adjusted products to 'temp' path
    # and return that path
    def _adjust_product_urls (self, feed_source_path, data_dir, account_config):
        srcProducts = self._loadSourceFeed (feed_source_path)
        if (srcProducts == None):
            return None

        adjusted_products = []
        for product in srcProducts:
            adjusted_product = copy.deepcopy (product)
            # catalog has 'value->attributes' OR 'value->fields' (dataconnect OR datahub)
            if 'attributes' in adjusted_product ['value']:
                pid = adjusted_product ['value']['attributes']['pid']
                adjusted_url = '%s%s___%s' % (account_config ['PRODUCT_URL_PREAMBLE'], pid, pid)
                adjusted_product ['value']['attributes']['url'] = adjusted_url
                adjusted_products.append (adjusted_product)
            elif 'fields' in adjusted_product ['value']:
                pid = adjusted_product ['value']['fields']['pid']
                adjusted_url = '%s%s___%s' % (account_config ['PRODUCT_URL_PREAMBLE'], pid, pid)
                adjusted_product ['value']['fields']['url'] = adjusted_url
                adjusted_products.append (adjusted_product)
            else:
                logging.error ('Unknown catalog format')
                return None

        # save adjusted products to 'temp' path
        temp_adjusted_feed_path = '%s/output/%s' % (data_dir, TEMP_ADJUSTED_FEED_FILENAME)
        try:
            self._writeJsonlFeed (adjusted_products, temp_adjusted_feed_path)
        except Exception as e:
            logging.error ('Exception in saving adjusted feed for: %s, message: %s' % \
                                    (cr.Credentials.getBloomreachAccountName (account_config), str(e)))
            return None
 
        return temp_adjusted_feed_path

    def _loadSourceFeed (self, feed_source_path):
        if (os.path.exists (feed_source_path) == False):
            logging.error ('Source feed file does not exist: %s', feed_source_path)
            return None

        srcFeedHandler = jfrl.JsonlFeedReaderLocal ()
        srcProducts = srcFeedHandler.readSourceFeed (feed_source_path)
        return srcProducts

    def _perform_discovery_dataconnect_reindex (self, adjusted_feed_source_path, account_config):
        dc_ingest_and_indexer = dc_igx.IngestAndIndex (self._command_line)
        job_id = dc_ingest_and_indexer.ingest_and_index_catalog (adjusted_feed_source_path, account_config)
        logging.debug ('Index job id: %s' % job_id)
        if (job_id == -1):
            logging.error ('Dataconnect Ingest_and_Index operation failed for: %s' % cr.Credentials.getBloomreachAccountName (account_config))
            return False
        return True

    def _perform_datahub_reindex (self, adjusted_feed_source_path, account_config):
        dh_ingest_and_indexer = dh_igx.IngestAndIndex (self._command_line)
        job_id = dh_ingest_and_indexer.ingest_and_index_catalog (adjusted_feed_source_path, account_config)
        logging.debug ('Index job id: %s' % job_id)
        if (job_id == -1):
            logging.error ('Datahub Ingest_and_Index operation failed for: %s' % cr.Credentials.getBloomreachAccountName (account_config))
            return False
        return True

    def _writeJsonlFeed (self, updatedProducts, temp_adjusted_feed_path):
        if os.path.exists (temp_adjusted_feed_path):
            os.remove (temp_adjusted_feed_path)   ## remove temp feed file; happens if previous indexer failed

        # full feed
        feedWriter = jwl.JsonlWriterLocal ()
        feedWriter.setProducts (updatedProducts)
        feedWriter.write (temp_adjusted_feed_path)
        return

if __name__ == "__main__":
    product_reindexer = ProductReindexer (None, None)
    product_reindexer.reingest_and_index ()

'''
    # read source products, change each product's url, save adjusted products to 'temp' path
    # and return that path
    def _adjust_product_urls_PREV (self, feed_source_path, data_dir, account_config):
        srcProducts = self._loadSourceFeed (feed_source_path)
        if (srcProducts == None):
            return None

        adjusted_products = []
        for product in srcProducts:
            adjusted_product = copy.deepcopy (product)
            pid = adjusted_product ['value']['attributes']['pid']
            adjusted_url = '%s%s___%s' % (account_config ['PRODUCT_URL_PREAMBLE'], pid, pid)
            adjusted_product ['value']['attributes']['url'] = adjusted_url
            adjusted_products.append (adjusted_product)

        # save adjusted products to 'temp' path
        temp_adjusted_feed_path = '%s/output/%s' % (data_dir, TEMP_ADJUSTED_FEED_FILENAME)
        try:
            self._writeJsonlFeed (adjusted_products, temp_adjusted_feed_path)
        except Exception as e:
            logging.error ('Exception in saving adjusted feed for: %s, message: %s' % \
                                    (cr.Credentials.getBloomreachAccountName (account_config), str(e)))
            return None
        return temp_adjusted_feed_path


'''
