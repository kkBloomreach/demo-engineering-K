# specifically for non-english catalog, the openAI generated text
# may not be perfect. That output is therefore manually edited
# then than manually edited file is postprocessed to generate the 'final'
# jsonl, which is then indexed
import json
import jsonlines
import logging
from collections import OrderedDict

import updaterConstants as uc
import jsonlWriter as jw

def postProcess ():
    # NOTE: the 'src_in' is actually 'output' created by manually editing 
    src_file = uc.FILENAME_UPDATED_JSONL_FEED_EN_MANUALLYEDITED_OUT
    output_file = uc.FILENAME_UPDATED_JSONL_FEED_EN_MANUALLYEDITED_PROCESSED_OUT

    src_catalog = None
    with open (src_file, 'r') as inputFile:
        src_catalog = json.load (inputFile)
        inputFile.close ()

    if (src_catalog != None):
        feedWriter = jw.JsonlWriter ()
        feedWriter.setContents (src_catalog)
        feedWriter.write (output_file)

        _generateReviewFile (src_catalog)
    return

# generate a 'readable' text file that experts can review
# for translation correctness. Add 'path' to translated attributes
# mainly for reviewer's convenience
def _generateReviewFile (src_catalog):
    prod_attribs_to_review = uc.CONTENT_ATTRIBUTES_TO_TRANSLATE.copy ()

    lineNum = 1
    with open (uc.FILENAME_REVIEW_DOC_EN, 'w') as reviewFile:
        sorted_catalog = sorted (src_catalog, key = lambda record: record ['path'])
        tag_names = []
        for record in sorted_catalog:
            sorted_record = OrderedDict (sorted (record ['value']['attributes'].items ()))
            reviewFile.write ('%s\tpath: %s\n' % (lineNum, record ['path']))
            lineNum = lineNum + 1
            for attrib, value in sorted_record.items ():
                if attrib in prod_attribs_to_review:
                    reviewFile.write ('%s\t%s: %s\n' % (lineNum, attrib, value))
                    lineNum = lineNum + 1
                elif attrib == 'tags':
                    tagList = record ['value']['attributes']['tags']
                    for tag in tagList:
                        if tag not in tag_names:
                            tag_names.append (tag)

            reviewFile.write ('===========\n\n')

        # write all tag names once
        if len (tag_names) > 0:
            reviewFile.write ('%s\ttag names:\n' % (lineNum))
            lineNum = lineNum + 1
            tag_names.sort ()
            for name in tag_names:
                reviewFile.write ('%s\t\t%s\n' % (lineNum, name))
                lineNum = lineNum + 1

        reviewFile.write ('\n')
        reviewFile.close ()
    return

if __name__ == '__main__':
    logging.basicConfig (level = logging.INFO)
    postProcess ()
    logging.info ('Finish...')


