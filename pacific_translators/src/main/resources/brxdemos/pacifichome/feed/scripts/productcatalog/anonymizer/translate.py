import xml.etree.ElementTree as ET
import csv
import logging
from random import random

import translateConsts as tc
import productTranslator as pt
import categoryInfo as ci

def inits ():
    logging.basicConfig (level = logging.DEBUG)

def translateXML (sourceFileName, categoryInfo):
    sourceXML = ET.parse (sourceFileName)
    sourceRoot = sourceXML.getroot ()

    outputRoot = ET.Element (tc.OUTPUT_ROOT_NODE_NAME);
    outputProducts = ET.SubElement (outputRoot, tc.OUTPUT_PRODUCTS_NODE_NAME)

    productList = sourceRoot.iter (tc.SOURCE_PRODUCT_NODE_NAME)
    productTranslator = pt.ProductTranslator ()
    for sourceProduct in productList:
        outputDict = productTranslator.translateOneProduct (sourceProduct, categoryInfo)
        if (outputDict != None):
            outputProduct = ET.SubElement (outputProducts, tc.OUTPUT_PRODUCT_NODE_NAME)
            for key in outputDict.keys ():
                tag = ET.SubElement (outputProduct, key)
                tag.text = outputDict [key]
 
    outputTree = ET.ElementTree (outputRoot)
    return outputTree 

def writeOutput (outputTree, filename):
    outputTree.write (filename, encoding='UTF-8', xml_declaration=True)

if __name__ == '__main__':
    inits ();
    # read categories
    categoryInfo = ci.CategoryInfo ()
    categoryInfo.readCategoriesInfo (tc.CATEGORY_INFO_FILE_NAME)

    # process source xml
    outputTree = translateXML (tc.SOURCE_FEED_FILE_NAME, categoryInfo)

    # write output
    writeOutput (outputTree, tc.OUTPUT_FEED_FILE_NAME)

    # finish
    logging.info ('INFO Finish...')

