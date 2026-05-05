# in order to compare a german translation versus english, we create
# a 'review' file for english version. The two review files are then 
# comparable side-by-side
 
import json
import jsonlines
import logging
from collections import OrderedDict

import updaterConstants as uc
import jsonlWriter as jw

def postProcess ():
    # NOTE: For english, there is really no manual-edit. We keep the
    # filename _me only for consistency 
    src_file = uc.FILENAME_UPDATED_JSONL_FEED_EN_MANUALLYEDITED_OUT
    output_file = uc.FILENAME_UPDATED_JSONL_FEED_EN_MANUALLYEDITED_PROCESSED_OUT

    src_catalog = None
    with open (src_file, 'r') as inputFile:
        src_catalog = json.load (inputFile)
        inputFile.close ()

    if (src_catalog != None):
        feedWriter = jw.JsonlWriter ()
        feedWriter.setProducts (src_catalog)
        feedWriter.write (output_file)

        _generateReviewFile (src_catalog)
    return

# generate a 'readable' text file that experts can review
# for translation correctness. Add 'pid' to translated attributes
# mainly for reviewer's convenience
# NOTE: currently pacificsupply products do not have variants. We keep that
# code here mainly for consistency with pacifichome
def _generateReviewFile (src_catalog):
    prod_attribs_to_review = uc.PRODUCT_ATTRIBUTES_TO_TRANSLATE.copy ()
    variant_attribs_to_review = uc.VARIANT_ATTRIBUTES_TO_TRANSLATE.copy ()

    lineNum = 1
    with open (uc.FILENAME_REVIEW_DOC_EN, 'w') as reviewFile:
        sorted_catalog = sorted (src_catalog, key = lambda record: record ['value']['attributes']['pid'])
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

'''
'''
