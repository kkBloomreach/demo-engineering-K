# V22 changes
# -- Some swatch images don't match the color assigned in variant->color

import logging
import copy
import os
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV22 as rcv22

# in this revision, products only in these categories are considered for generating variants
SELECT_CATEGORY_IDS = [
    '10000>10800',   # men > shoes
    '20000>20800',   # women > shoes
    '70000>70100',   # shoes > casual
    '70000>70200',   # shoes > dress
    '70000>70300'    # sneakers
]

class RevisionV22 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v22')
        super().__init__ ()

        # {"pid, title, images=[]}
        self._adjust_swatch_image_color_list = []
        return

    def _initialize (self, source_records, inject_av_map):
        # read adjust-color list
        self._adjust_swatch_image_color_list = self._read_adjust_swatch_image_color_list ()
        if (self._adjust_swatch_image_color_list == None):
            return False
        return True

    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)
        if (inject_av_record == None):
            logging.debug ('No inject attrib_value record for pid: %s', pid)

        # change image only if product is in 'jewellery' categories
        if self._is_select_category (record) == True:
            updated_record = self._perform_update_internal (record, inject_av_record)
            return updated_record
        else:
            return record
        return updated_record

    # 
    def _finalize (self, updated_products):
        return True

    # INTERNAL METHODS
    def _read_adjust_swatch_image_color_list (self):
        logging.info ("reading source: %s" % rcv22.FILENAME_ADJUST_SWATCH_IMAGE_COLOR_TSV_IN)
        adjust_colors = []
        if os.path.exists (rcv22.FILENAME_ADJUST_SWATCH_IMAGE_COLOR_TSV_IN):
            with open (rcv22.FILENAME_ADJUST_SWATCH_IMAGE_COLOR_TSV_IN, 'r') as file_obj:
                dict_reader = csv.DictReader (file_obj, delimiter='\t')
                for row in dict_reader:
                    adjust_colors.append (row)
                file_obj.close ()
            logging.info ('adjust record count: %s' % len (adjust_colors))
        else:
            logging.error ('cannot find source: %s' % rcv22.FILENAME_ADJUST_SWATCH_IMAGE_COLOR_TSV_IN)
            adjust_colors = None
        return adjust_colors

    def _perform_update_internal (self, record, inject_av_record):
        pid = record ['value']['attributes']['pid']
        updated_record = copy.deepcopy (record)

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

    def _lookup_adjust_swatch_image_color_record (self, swatch_image_url):
        for adjust_record in self._adjust_swatch_image_color_list:
            if adjust_record ['Swatch'] == swatch_image_url:
                return adjust_record
        return None # this case should not occur

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV22 ()
    logging.info ('RevisionV22 finish...')


