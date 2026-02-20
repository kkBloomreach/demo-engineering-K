# V9 changes
# -- change brand name for some products
# -- replace "Hrithik" -> HRX (similar to brand name) in title, description
# -- Note - the new brand names are provided in brand-name-change.tsv

import logging
import random
import os
import copy
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV9 as rcv9
from categorybuilder import CategoryBuilder

class RevisionV9 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v9')
        super().__init__ ()
        return

    def _initialize (self, source_records, inject_av_map):
        # read categorystatus map
        self._brand_names_to_change = self._read_brand_names_to_change ()
        return True

    # override base class method
    # This update class does not do any update to previous records
    def _perform_record_update (self, record, gen_image_map):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)
        brand_name_to_change_record = self._lookup_brand_to_change_record (pid)

        if (inject_av_record == None):
            logging.warning ('No inject attrib_value record for pid: %s', pid)
            return record

        if (brand_name_to_change_record == None):
            logging.warning ('No brand_to_change record for pid: %s', pid)
            return record

        updated_record = self._perform_update_internal (record, inject_av_record, brand_name_to_change_record)
        return updated_record

    # INTERNAL METHODS
    def _read_brand_names_to_change (self):
        _brand_names_to_change = []

        with open (rcv9.FILENAME_BRAND_NAME_CHANGE_TSV_IN, 'r') as brand_name_change_file:
            tsv_reader = csv.DictReader (brand_name_change_file, delimiter = '\t')
            for row in tsv_reader:
                _brand_names_to_change.append (row)
            brand_name_change_file.close ()
        logging.info ('brand name change record count: %s' % len (_brand_names_to_change))
        return _brand_names_to_change

    def _lookup_brand_to_change_record (self, pid):
        for row in self._brand_names_to_change:
            if row ['pid'] == pid:
                return row
        return None

    def _perform_update_internal (self, record, inject_av_record, brand_name_to_change_record):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']

        updated_record = copy.deepcopy (record)

        # brand
        current_brand = record ['value']['attributes']['brand']
        edited_brand = brand_name_to_change_record ['edited_brand']
        if (edited_brand != current_brand):
            logging.debug ('Changed brand for pid %s, new brand %s' % (pid, edited_brand))
            updated_record ['value']['attributes']['brand'] = edited_brand
            updated_record ['value']['attributes']['product_brand'] = edited_brand

        # also some additional changes 'Purple State'
        if (current_brand.startswith ('Purple State') and edited_brand.startswith ('Pacific')):
            logging.debug ('Changed brand for pid %s, new brand %s' % (pid, edited_brand))
            updated_record ['value']['attributes']['brand'] = 'Purple State'
            updated_record ['value']['attributes']['product_brand'] = 'Purple State' 
 
        # also some additional changes 'Red Tape'
        if (current_brand.startswith ('Red Tape') and edited_brand.startswith ('Pacific')):
            logging.debug ('Changed brand for pid %s, new brand %s' % (pid, edited_brand))
            updated_record ['value']['attributes']['brand'] = 'Red Tape'
            updated_record ['value']['attributes']['product_brand'] = 'Red Tape'
 
        # also some additional changes 'Roadster'
        if (current_brand.startswith ('Roadster') and edited_brand.startswith ('Pacific')):
            logging.debug ('Changed brand for pid %s, new brand %s' % (pid, edited_brand))
            updated_record ['value']['attributes']['brand'] = 'Roadster'
            updated_record ['value']['attributes']['product_brand'] = 'Roadster'
 

        # title 
        title = updated_record ['value']['attributes']['title']
        if (title.find ('Hrithik') >= 0):
            title = title.replace ('Hrithik', '')
            logging.debug ('Changed title for pid %s, new title %s' % (pid, title))
            updated_record ['value']['attributes']['title'] = title

        # description 
        description = updated_record ['value']['attributes']['description']
        if (description.find ('Hrithik') >= 0):
            description = description.replace ('Hrithik', '')
            logging.debug ('Changed description for pid %s, new description %s' % (pid, description))
            updated_record ['value']['attributes']['description'] = description

        # category for specific products
        if (pid == '102620'):
            # by mistake, this was included in men->tops category as well; should be only in women->tops
            changed_category_path = self._category_builder.construct_category_path_from_crumbs_and_crumbIds (CategoryBuilder.BREAD_CRUMB_WOMEN_TOPS)
            updated_record ['value']['attributes']['category_paths'] = changed_category_path
            logging.debug ('Changed crumbs for pid %s; new crumb %s' % (pid, CategoryBuilder.BREAD_CRUMB_WOMEN_TOPS))

        return updated_record

if __name__ == '__main__':
    rv = RevisionV9 ()

