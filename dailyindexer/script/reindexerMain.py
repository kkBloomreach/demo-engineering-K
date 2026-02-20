# Main entry point for sandbox feed reindexer
# NOTE: PYTHONPATH (aka sys.path) is set in __init__.py

import sys
import os
import json
import logging
import copy
import psutil

# **** NOTE: Cannot use jsonlines package because it is not available in Debian unix.
# The google cloud account (GCP) has Debian OS

from utils import commandLine as cl
from utils import credentials as cr
from utils import reindexconsts as rc
from productreindexer import ProductReindexer 
from contentreindexer import ContentReindexer

class ReindexerMain ():

    def __init__ (self, command_line):
        logging.info ('Reindexer V3.0') # changed to support dataconnect v3
        self._command_line = command_line
        return

    def initConfigs (self):
        data_dir = self._command_line.getDataDir ()
        config_path = '%s/%s' % (data_dir, rc.INPUT_ACCOUNT_CONFIGS_PATHNAME)
        if (os.path.isfile (config_path) == False):
            logging.error ('Cannot find account config file')
            return False

        initStat = cr.Credentials.loadAccountConfigs (config_path)
        if (initStat == False):
            logging.error ("Load account configuration failed")
        return initStat

    def reingest_and_index (self):
        account_config_list = cr.Credentials.getAccountConfigList ()
        for account_config in account_config_list:
            # debugging memory usage
            memory_used = psutil.virtual_memory()[3]
            logging.debug ('**** Memory used (GB): %s' % (memory_used/1000000000))

            if (account_config ['IGNORE'] == "YES"):
                logging.info ('Ignoring reindex for account: %s' % account_config ["BR_ACCOUNT_NAME"])
                continue

            if (account_config ['CATALOG_TYPE'] == 'product'):
                ingest_and_indexer = ProductReindexer (account_config, self._command_line)
                if (ingest_and_indexer.reingest_and_index () == False):
                    logging.info ('Product reindex for account failed: %s' % account_config ["BR_ACCOUNT_NAME"])
            elif (account_config ['CATALOG_TYPE'] == 'content'):
                ingest_and_indexer = ContentReindexer (account_config, self._command_line)
                if (ingest_and_indexer.reingest_and_index () == False):
                    logging.info ('Content reindex for account failed: %s' % account_config ["BR_ACCOUNT_NAME"])
            else:
                logging.error ('Unknown catalog_type for account: %s' % account_config ["BR_ACCOUNT_NAME"])
        return True

if __name__ == "__main__":
    # parse command line 
    command_line= cl.CommandLine ()
    if (command_line.parseCommandLine (sys.argv [1:]) == True):
        if (command_line.getLogPath () == None):
            logging.basicConfig (level = command_line.getLogLevel ())
        else:
            logging.basicConfig (level = command_line.getLogLevel (), filename = command_line.getLogPath(), encoding='utf-8')

        reindexer_main = ReindexerMain (command_line)
        if (reindexer_main.initConfigs () == True):
            reindexer_main.reingest_and_index ()

    logging.info ('ReindexerMain Finish...')

