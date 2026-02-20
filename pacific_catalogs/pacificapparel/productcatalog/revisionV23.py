# V23 changes
# -- replace 'color' values in title and description to avoid
# -- confusion between actual image and the text

import logging
import copy
import os
import csv
import random

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV23 as rcv23

ALTERNATE_ADJECTIVES = [
    'popular',
    'beautiful',
    'trending',
    'artistic',
    'admired',
    'lovely',
    'graceful',
    'superb',
    'magnificent' 
]

# in this revision, products only in these categories are considered 
SELECT_CATEGORY_IDS = [
    '80000>80100',   # handbags > purses
    '10000>10800',   # men > shoes
    '20000>20800',   # women > shoes
    '70000>70100',   # shoes > casual
    '70000>70200',   # shoes > dress
    '70000>70300'    # sneakers
]

class RevisionV23 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v23')
        super().__init__ ()

        # {"color, value}
        self._adjust_color_names_list = []

        # products with exact same title are removed from catalog
        self._product_titles = []
        return

    def _initialize (self, source_records, inject_av_map):
        # read adjust-color list
        self._adjust_color_name_list = self._read_adjust_color_name_list ()
        if (self._adjust_color_name_list == None):
            return False
        return True

    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)
        #if (inject_av_record == None):
        #    logging.debug ('No inject attrib_value record for pid: %s', pid)

        # change color value only in select category
        if self._is_select_category (record) == True:
            updated_record = self._perform_update_internal (record, inject_av_record)
            return updated_record
        else:
            updated_record = record
        return updated_record

    def _finalize (self, updated_products):
        return True

    # INTERNAL METHODS
    def _read_adjust_color_name_list (self):
        logging.info ("reading source: %s" % rcv23.FILENAME_ADJUST_COLOR_NAMES_TSV_IN)
        adjust_color_names = []
        if os.path.exists (rcv23.FILENAME_ADJUST_COLOR_NAMES_TSV_IN):
            with open (rcv23.FILENAME_ADJUST_COLOR_NAMES_TSV_IN, 'r') as file_obj:
                dict_reader = csv.DictReader (file_obj, delimiter='\t')
                for row in dict_reader:
                    adjust_color_names.append (row)
                file_obj.close ()
            logging.info ('adjust record count: %s' % len (adjust_color_names))
        else:
            logging.error ('cannot find source: %s' % rcv23.FILENAME_ADJUST_COLOR_NAMES_TSV_IN)
            adjust_color_names = None
        return adjust_color_names

    def _perform_update_internal (self, record, inject_av_record):
        pid = record ['value']['attributes']['pid']

        # check for color name in title
        title = self._replace_color_in_title (record ['value']['attributes']['title'])

        # in case title text is updated, check if it is now a duplicate of another product
        # If so, exclude it from output catalog
        if title in self._product_titles:
            logging.debug ('Excluding pid = %s due to duplicate title' % (record ['value']['attributes']['pid']))
            return None
        self._product_titles.append (title)

        updated_record = copy.deepcopy (record)

        # if not duplicate, update title and check color in description as well
        description = self._replace_color_in_description (record ['value']['attributes']['description'])

        updated_record ['value']['attributes']['title'] = title
        updated_record ['value']['attributes']['description'] = description
        return updated_record

    def _is_select_category (self, record):
        category_paths = record ['value']['attributes']['category_paths']
        for branch in category_paths:
            full_path = None 
            for leaf_node in branch:
                if full_path == None:
                    full_path = leaf_node ['id']
                else:
                    full_path = '%s>%s' % (full_path, leaf_node ['id'])
            if full_path in SELECT_CATEGORY_IDS:
                return True
        return False

    def _replace_color_in_title (self, title):
        for adjust_record in self._adjust_color_name_list:
            if title.find (adjust_record ['color']) >= 0:
                title = title.replace (adjust_record ['color'], '')
                title = title.replace ('  ', ' ')   # multiple consecutive blank spaces -> single
        return title # if no color in title, return it as-is

    def _replace_color_in_description (self, description):
        for adjust_record in self._adjust_color_name_list:
            if description.find (adjust_record ['color']) >= 0:
                alternate = self._select_alternate_adjective (adjust_record ['color'])
                description = description.replace (adjust_record ['color'], alternate)
        return description # if no color in description, return it as-is

    # replace actual color value with some other adjective
    def _select_alternate_adjective (self, color_value):
        rindx = int (random.random () * len (ALTERNATE_ADJECTIVES)) 
        alternate = ALTERNATE_ADJECTIVES [rindx]
        return alternate

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV23 ()
    logging.info ('RevisionV23 finish...')


'''
        updated_record = copy.deepcopy (record) # record only in select-category

        # if color is to be changed, do it; otherwise 'actual' value in adjust_color input is None
        if ('variants' in updated_record ['value']) and (updated_record ['value']['variants']):
            variant_list = updated_record ['value']['variants']
            for variant_id, variant_obj in variant_list.items():
                swatch_image_url = variant_obj ['attributes']['swatch_image']
                adjust_color_record = self._lookup_adjust_swatch_image_color_record (swatch_image_url)
                if adjust_color_record != None:
                    if (adjust_color_record ['Actual'] != ''):
                        logging.debug ('Adjusting color for variant_id: %s to color = %s' % (variant_id, adjust_color_record ['Actual']))
                        variant_obj ['attributes']['color'] = adjust_color_record ['Actual'].lower ()
                else:
                    logging.error ('Cannot find adjust-color-record for swatch_image: %s' % swatch_image_url)
                    updated_record = None
        return updated_record
'''

