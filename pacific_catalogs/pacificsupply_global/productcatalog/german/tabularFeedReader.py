import logging
import csv
import os

class TabularFeedReader () :

    _source_path = None

    def __init__ (self):
        return

    def set_source_path (self, source_path):
        self._source_path = source_path
        return

    def read_source (self):
        logging.info ("reading tabular source: " + self._source_path)
        tabular_records = []
        if os.path.exists (self._source_path):
            with open (self._source_path, 'r') as file_obj:
                dict_reader = csv.DictReader (file_obj, delimiter='\t')
                for row in dict_reader:
                    tabular_records.append (row)
                file_obj.close ()
            logging.info ("tabular record count: %s", len (tabular_records))
        else:
            logging.error ('cannot find tabular source: %s', self._source_path)
        return tabular_records


