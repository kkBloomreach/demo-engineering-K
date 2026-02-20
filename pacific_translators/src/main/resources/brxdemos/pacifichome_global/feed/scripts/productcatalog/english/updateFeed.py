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
        self.subset_products = []
        self.updated_products = []
        self.updated_attributeList = []

    def setSubsetProducts (self, subsetProducts):
        self.subset_products = subsetProducts

    def performUpdates (self):
        self._updateFeed ()
        return self.updated_products, self.updated_attributeList

    # returns iterator to updatedProducts 
    def getUpdatedProductIterator (self):
        return self.updated_products.__iter__ ()

    def _updateFeed (self):
        logging.info ("process subset products")

        # process attribute map (remove some attribs, rename, ...)
        for srcRecord in self.subset_products:
            updatedRecord = srcRecord.copy ()

            # replace productUrl
            url = srcRecord ['value']['attributes']['url']
            updatedUrl = url.replace ('/home/', '/home-global/');
            updatedRecord ['value']['attributes']['url'] = updatedUrl

            self.updated_products.append (updatedRecord)

            # accumulate attribute names across all products and their variants
            self._collectAttributeList (updatedRecord)

        logging.info ("product + variants in updated subset feed count: %s", len (self.updated_products))
        return

    # returns True if image exists
    def _updateImageUrls (self, currentRecord):
        pid = currentRecord ['value']['attributes']['pid']
        preferredGenImageUrl = self._lookupGenimageUrl (pid)
        if (preferredGenImageUrl != None):
            if 'thumb_image' in currentRecord ['value']['attributes']:
                currentRecord ['value']['attributes']['thumb_image'] = preferredGenImageUrl
            if 'large_image' in currentRecord ['value']['attributes']:
                currentRecord ['value']['attributes']['large_image'] = preferredGenImageUrl
            if 'variants' in currentRecord ['value'] and currentRecord ['value']['variants']:
                variantList = currentRecord ['value']['variants']
                for key, variantObj in variantList.items():
                    if 'swatch_image' in variantObj ['attributes']:
                        # currently same image is uses for all variants --
                        # COULD improve slightly by generating new image 
                        # with variant-specific color etc TO BE DONE
                        variantObj ['attributes']['swatch_image'] = preferredGenImageUrl
            return True
        else:
            logging.warn ('No generated image for pid: %s', pid)
            return False
                
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
    srcProducts = srcFeedReader.readSourceFeed (uc.FILENAME_JSONL_BRSM_FEED_IN)

    feedUpdater = UpdateFeed ()
    feedUpdater.setSourceProducts (srcProducts)
    updatedProducts, updatedAttributeList = feedUpdater.performUpdates ()
    prodIterator = feedUpdater.getUpdatedProductIterator ()
    logging.debug ('Finish ...')


