# V0 changes
#  - use pacificsupply global ENGLISH catalog (.tsv)
#  - upload to gdrive, translate specific attribute values using formula: =GOOGLETRANSLATE
#  --- title, description, application, color, material, size, type, category_names (not catid)
#  --- since some attrib values are blank, GoogleTranslates those to #VALUE!. Those need to be manually replaced by <blank>
# pid 1141643 does not have valid image - therefore set 'availability = false'

import logging
import os
import copy
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV0 as rcv0

PIDS_WITH_BAD_IMAGE = [
    '1141643',
    '188531',
    '4805943',
    '4885217',
    '4885218',
    '4895078',
    '4845510',
    '4159188',
    '663835',
    '578176',
    '3082267',
    '4551993',
    '4816331',
    '4895068',
    '4895073',
    '4895072',
    '4281860',
    '4314661',
    '4487900',
    '4761743'
]

class RevisionV0 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v0')
        super().__init__ ()
        self._catid_name_map = None # {"catid", "catname_en", "catname_de"}
        self._attribute_translation_map = None # read translated de records (tsv)
        return

    def _initialize (self, source_records, inject_av_map):
        self._catid_name_map = []
        if os.path.exists (rcv0.FILENAME_CATID_NAME_MAP_TSV_IN):
            with open (rcv0.FILENAME_CATID_NAME_MAP_TSV_IN, 'r') as input:
                tsv_reader = csv.DictReader (input, delimiter = '\t')
                for row in tsv_reader:
                    self._catid_name_map.append (row)
                input.close ()
        else:
            logging.error ('Cannot read catid name map')
            return False

        if os.path.exists (rcv0.FILENAME_ATTRIBUTE_TRANSLATION_MAP_TSV_IN):
            self._attribute_translation_map = []
            with open (rcv0.FILENAME_ATTRIBUTE_TRANSLATION_MAP_TSV_IN, 'r') as input:
                tsv_reader = csv.DictReader (input, delimiter = '\t')
                for row in tsv_reader:
                    self._attribute_translation_map.append (row)
                input.close ()
        else:
            logging.error ('Cannot read attribute translation map')
            return False
        return True

    # override base class method
    # This update class does not do any update to previous records except url
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        # inject_av_record = super()._lookup_inject_av_record (pid)
        #if (inject_av_record == None):
        #    logging.debug ('No inject attrib_value record for pid: %s', pid)

        updated_record = self._perform_update_internal (record)
        return updated_record

    def _finalize (self, updated_products):
        return updated_products
 
    # INTERNAL METHODS
    def _perform_update_internal (self, record):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']

        # lookup translation map for this pid (contains translation of specific attributes)
        attrib_translation_map = None
        for a_map in self._attribute_translation_map:
            if a_map ['pid'] == str (pid):
                attrib_translation_map = a_map
                break

        if attrib_translation_map == None:
            logging.error ('Cannot find attribute translation map for pid: %s' % pid)
            return None

        # dup original record and replace specific attribute values
        translated_record = copy.deepcopy (record)
       
        # use translated value for select attributes
        for attrib in rcv0.TRANSLATED_ATTRIBUTES_LIST:
            translated_record ['value']['attributes'][attrib] = attrib_translation_map [attrib]

        # map each category name and update overall category_path
        category_paths_translated = self._translate_category_paths (record ['value']['attributes']['category_paths'])
        translated_record ['value']['attributes']['category_paths'] = category_paths_translated

        # adjust url
        translated_record ['value']['attributes']['url'] = '%s%s___%s' % (uc.PRODUCT_URL_PREFIX, pid, pid)

        # some pids does not have valid image - therefore set 'availability = false'
        if str (pid) in PIDS_WITH_BAD_IMAGE:
            translated_record ['value']['attributes']['availability'] = False

        return translated_record

    # go thru src catid english names in category_paths, lookup corresponding german name
    # build translated-category-paths
    def _translate_category_paths (self, src_category_paths):
        translated_category_paths = []
        for src_branch in src_category_paths:
            translated_branch = []
            for src_leaf_node in src_branch:
                src_leaf_id = src_leaf_node ['id']
                translated_leaf_name = self._lookup_translated_catname (src_leaf_id)
                translated_leaf_node = { 'id': src_leaf_id,
                                         'name': translated_leaf_name
                                       }
                translated_branch.append (translated_leaf_node)
            translated_category_paths.append (translated_branch)
        return translated_category_paths

    def _lookup_translated_catname (self, src_leaf_id):
        for a_catid_map in self._catid_name_map:
            if a_catid_map ['catid'] == src_leaf_id:
                return a_catid_map ['catname_de']
        logging.error ('Cannot find translated category name for catid: %s' % src_leaf_id)
        return None


if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV0 ()
    logging.info ('RevisionV0 Finish...')

