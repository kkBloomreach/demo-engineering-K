# specifically for non-english catalog, the openAI generated text
# may not be perfect. That output is therefore manually edited
# then that manually edited file is postprocessed to generate the 'final'
# jsonl, which is then indexed. 

# Besides 'cleaning' translation, we also make sure catName/material/product_type/condition
# values are 'consistent' across multiple products (ie, a catId has same name in all products' category_path)
# That consistency is done via a 'termMap' and 'categoryMap' created earlier (and cleaned by itself)

# During this process, we also create a 'review'
# file that experts can review and provide their feedback. That feedback has to
# be manually merged into openAI-generated file

import json
import jsonlines
import csv
import logging
from collections import OrderedDict

import updaterConstants as uc
import jsonlWriter as jw
import jsonlFeedReader as jfr

def postProcess ():
    # NOTE: the 'src_file_de' is actually 'output' created by manually editing 
    # src_file_de is manually edited .json format
    src_file_de = uc.FILENAME_UPDATED_JSONL_FEED_DE_MANUALLYEDITED_OUT 
    # map files are manually edited, .tsv format
    termMap_file = uc.FILENAME_UPDATED_TRANSLATION_TERM_MAP_DE_MANUALLYEDITED_OUT
    categoryMap_file = uc.FILENAME_UPDATED_TRANSLATION_CATEGORY_MAP_DE_MANUALLYEDITED_OUT

    src_file_en = uc.FILENAME_JSONL_SOURCE_FEED_EN_IN
    output_file_de = uc.FILENAME_UPDATED_JSONL_FEED_DE_MANUALLYEDITED_PROCESSED_OUT

    logging.info ('Begin read source files...')
    # de catalog
    src_catalog_de = None
    with open (src_file_de, 'r') as inputFile:
        src_catalog_de = json.load (inputFile)
        inputFile.close ()

    # en catalog - jsonl format
    jsonlReader = jfr.JsonlFeedReader ()
    src_catalog_en = jsonlReader.readSourceFeed (src_file_en)

    # term map - tsv format
    with open (termMap_file, 'r') as inputFile:
        termMap = []
        dict_reader = csv.DictReader (inputFile, delimiter='\t')
        for row in dict_reader:
            termMap.append (row);
        inputFile.close ()

    # category map - tsv format
    with open (categoryMap_file, 'r') as inputFile:
        categoryMap = []
        dict_reader = csv.DictReader (inputFile, delimiter='\t')
        for row in dict_reader:
            categoryMap.append (row);
        inputFile.close ()

    logging.info ('End read source files...')

    # use maps to ensure translation values are consistent across
    # multiple products
    updated_catalog_de = _ensureConsistency (src_catalog_de, src_catalog_en, termMap, categoryMap)

    # write updated catalog as 'jsonl'
    if (updated_catalog_de != None):
        logging.info ('Begin write updated de catalog')
        feedWriter = jw.JsonlWriter ()
        feedWriter.setProducts (updated_catalog_de)
        feedWriter.write (output_file_de)
        logging.info ('End write updated de catalog')

        logging.info ('Begin write review text file')
        _generateReviewFile (updated_catalog_de)
        logging.info ('End write review text file')
    return

def _ensureConsistency (src_catalog_de, src_catalog_en, termMap, categoryMap):
    logging.info ('Begin ensure...')
    updated_catalog_de = []

    for src_record_de in src_catalog_de:
        updated_record_de = src_record_de.copy ()

        pid = src_record_de ['value']['attributes']['pid']
        logging.debug ('\tpid = ' + pid)

        # corresponding en record and the specific values (eg, product_type)
        src_record_en = _lookupEnProduct (pid, src_catalog_en)
        product_type_en = src_record_en ['value']['attributes']['product_type']
        material_en = src_record_en ['value']['attributes']['material']
        condition_en = src_record_en ['value']['attributes']['condition']

        # use termMap to get corresponding de terms
        product_type_map_record = _lookupTerm ('product_type', product_type_en, termMap)
        material_map_record = _lookupTerm ('material', material_en, termMap)
        condition_map_record = _lookupTerm ('condition', condition_en, termMap)

        # set the map'd 'de' values in updated_de catalog. Not all products have all these
        # attributes, therefore the lookup may return "None"
        if (product_type_map_record != None):
            updated_record_de ['value']['attributes']['product_type'] = product_type_map_record ['de']
        if (material_map_record != None):
            material_de = material_map_record ['de']
            updated_record_de ['value']['attributes']['material'] = material_de
            updated_record_de ['value']['attributes']['velo_material_lower'] = material_de.lower()
        if (condition_map_record != None):
            updated_record_de ['value']['attributes']['condition'] = condition_map_record ['de']

        # go thru product's category_path and set catNames
        categoryPathList = updated_record_de ['value']['attributes']['category_paths']
        for branch in categoryPathList:
            for leaf in branch:
                catId = leaf ['id']
                cat_map_record = _lookupCategoryMap (catId, categoryMap)
                leaf ['name'] = cat_map_record ['catName_de']

        updated_catalog_de.append (updated_record_de)

    logging.info ('End ensure...')
    return updated_catalog_de

def _lookupTerm (termtype, termvalue_en, termMap):
    for termMap in termMap:
        if (termMap ['termtype'] == termtype) and (termMap ['en'] == termvalue_en):
            return termMap

    return None

def _lookupCategoryMap (catId, categoryMap):
    for catIdMap in categoryMap:
        if (catIdMap ['catId'] == catId):
            return catIdMap

    return None

def _lookupEnProduct (pid, enCatalog):
    for enProduct in enCatalog:
        if enProduct ['value']['attributes']['pid'] == pid:
            return enProduct

    return None


# generate a 'readable' text file that experts can review
# for translation correctness. Add 'pid' to translated attributes
# mainly for reviewer's convenience
def _generateReviewFile (src_catalog_de):
    prod_attribs_to_review = uc.PRODUCT_ATTRIBUTES_TO_TRANSLATE.copy ()
    variant_attribs_to_review = uc.VARIANT_ATTRIBUTES_TO_TRANSLATE.copy ()

    lineNum = 1
    with open (uc.FILENAME_REVIEW_DOC_DE_OUT, 'w') as reviewFile:
        sorted_catalog = sorted (src_catalog_de, key = lambda record: record ['value']['attributes']['pid'])
        category_names = [] # collect all category_names and write them all at once
        for record in sorted_catalog:
            sorted_record = OrderedDict (sorted (record ['value']['attributes'].items ()))
            reviewFile.write ('%s\tpid: %s\n' % (lineNum, record ['value']['attributes']['pid']))
            lineNum = lineNum + 1
            for attrib, value in sorted_record.items ():
                if attrib in prod_attribs_to_review:
                    reviewFile.write ('%s\t%s: %s\n' % (lineNum, attrib, value))
                    lineNum = lineNum + 1
                elif attrib == 'category_paths':
                    categoryPathList = record ['value']['attributes']['category_paths']
                    for branch in categoryPathList:
                        for leaf in branch:
                            if leaf ['name'] not in category_names:
                                category_names.append (leaf ['name'])

            if 'variants' in record ['value']:
                sorted_variants = OrderedDict (sorted (record ['value']['variants'].items ()))
                for variantId, variantObj in sorted_variants.items():
                    reviewFile.write ('\n%s\tvariantId: %s\n' % (lineNum, variantId))
                    lineNum = lineNum + 1
                    sorted_variant = OrderedDict (sorted (variantObj ['attributes'].items()))
                    for attrib, value in sorted_variant.items ():
                        if attrib in variant_attribs_to_review:
                            reviewFile.write ('%s\t%s: %s\n' % (lineNum, attrib, value))
                            lineNum = lineNum + 1
            reviewFile.write ('===========\n\n')

        # write all category names once
        if len (category_names) > 0:
            reviewFile.write ('%s\tcategory names:\n' % (lineNum))
            lineNum = lineNum + 1
            category_names.sort ()
            for name in category_names:
                reviewFile.write ('%s\t\t%s\n' % (lineNum, name))
                lineNum = lineNum + 1

        reviewFile.write ('\n')
        reviewFile.close ()
    return

if __name__ == '__main__':
    logging.basicConfig (level = logging.INFO)
    postProcess ()
    logging.info ('Finish...')

