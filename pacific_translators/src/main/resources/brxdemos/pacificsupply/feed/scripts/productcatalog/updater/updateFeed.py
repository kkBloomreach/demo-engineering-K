# For DataConnect, do not need to create any "sku_xxx" attributes
# See comments in PACIFICSUPPLYMINDCURV-101

import logging
import csv
from random import random 
import os

import jsonlFeedReader as jfr
import updaterConstants as uc

class UpdateFeed ():
    def __init__ (self):
        self.src_products = []
        self.updated_products = []
        self.updated_attributeList = []

    def setSourceProducts (self, brsmSrcProducts):
        self.src_products = brsmSrcProducts

    def performUpdates (self):
        self._updateFeed ()
        return self.updated_products, self.updated_attributeList

    # returns iterator to updatedProducts 
    def getUpdatedProductIterator (self):
        return self.updated_products.__iter__ ()

    def _updateFeed (self):
        logging.info ("process brsm feed")

        # process attribute map (remove some attribs, rename, ...)
        for srcRecord in self.src_products:
            updatedRecord = srcRecord.copy ()

            # collect all updated products
            self.updated_products.append (updatedRecord)

            # accumulate attribute names across all products and their variants
            self._collectAttributeList (updatedRecord)

        # sort the updated records by pid. This is mainly to ensure the 'subset'
        # creation will generate a 'subset' in a deterministic way (ie, same subset generated
        # in each new run)
        self.updated_products.sort (key=lambda record: record ['value']['attributes']['pid'])

        logging.info ("product + variants in updated feed count: %s", len (self.updated_products))
        return

    def _collectAttributeList (self, currentRecord):
        prodAttribs = currentRecord ['value']['attributes']
        for attrib in prodAttribs.keys():
            if attrib not in self.updated_attributeList:
                self.updated_attributeList.append (attrib)
        if 'variants' in currentRecord ['value'] and currentRecord ['value']['variants']:
            variantList = currentRecord ['value']['variants']
            for variantId, variantObj in variantList.items (): 
                for key in variantObj ['attributes'].keys ():
                    if key not in self.updated_attributeList:
                        self.updated_attributeList.append (key)

        # attribute list sorted just before writing it to attribList
        return

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    srcFeedReader = jfr.JsonlFeedReader ()
    srcProducts = srcFeedReader.readSourceFeed (uc.FILENAME_JSONL_BRSM_FEED_IN)

    feedUpdater = UpdateFeed ()
    feedUpdater.setSourceProducts (srcProducts)
    updatedProducts, updatedAttributeList = feedUpdater.performUpdates ()
    prodIterator = feedUpdater.getUpdatedProductIterator ()
    logging.debug ('Finish ...')


