# For DataConnect, do not need to create any "sku_xxx" attributes
# See comments in PACIFICSUPPLYMINDCURV-101

import logging
import csv
from random import random 
import os

import jsonlFeedReader as jfr
import updaterConstants as uc
import attributeMap as am

class UpdateFeed ():
    def __init__ (self):
        self.src_products = []
        self.updated_products = []
        self.updated_attributeList = []
        self._genimage_map = [] # pid -> generated-image URL map

    def setSourceProducts (self, brsmSrcProducts):
        self.src_products = brsmSrcProducts

    def performUpdates (self):
        self._readGenimageMap (uc.FILENAME_GENIMAGE_MAP)
        self._updateFeed ()
        return self.updated_products, self.updated_attributeList

    # returns iterator to updatedProducts 
    def getUpdatedProductIterator (self):
        return self.updated_products.__iter__ ()

    # read pid -> genimage urls
    # If imgUrl does not exist for a source pid, that pid is excluded from output
    def _readGenimageMap (self, srcPath):
        logging.info ("reading genimage map: " + srcPath)
        if os.path.exists (srcPath):
            with open (srcPath, 'r') as file_obj:
                dict_reader = csv.DictReader (file_obj, delimiter='\t')
                for row in dict_reader:
                    self._genimage_map.append (row)
                file_obj.close ()
            logging.info ("genimage_map count: %s", len (self._genimage_map))
        else:
            logging.error ('cannot find genimage map: %s', srcPath)
        return

    def _updateFeed (self):
        logging.info ("process brsm feed")

        # process attribute map (remove some attribs, rename, ...)
        for srcRecord in self.src_products:
            updatedRecord = srcRecord.copy ()

            # replace imageUrls using genimage map
            # if there is no genimage for this pid, exclude it from output
            if (self._updateImageUrls (updatedRecord) == False):
                continue # this record not appended in outputFeed

            # adjust specific attributes (eg, url, availability)
            # Note, source (original) record remains unchanged
            self._processAttributeMap (updatedRecord)

            # properNames replaced (title, description, ...)
            self._replaceProperNames (updatedRecord)

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

    # remove attrib, rename as defined in ATTRIBUTE_MAP for given product or variant record
    # Any attrib NOT in attrib_map is carried over as-is
    # NOTE: in attribMap, 'newName' may be same as 'name' (ie, name is not changed) -- typically used for testing purpose
    def _processAttributeMap (self, currentRecord):
        for attribMap in am.ATTRIBUTE_MAP:
            newAttribName = attribMap ['newName']
            currentAttribName = attribMap ['name']
            if (attribMap ['isProductAttrib'] == True):
                if currentAttribName in currentRecord ['value']['attributes'].keys():
                    if (attribMap ['keep'] == True):
                        currentRecord ['value']['attributes'][newAttribName] = currentRecord ['value']['attributes'][currentAttribName]
                        if (newAttribName != currentAttribName):
                            del currentRecord['value']['attributes'][currentAttribName]
                    else:
                        # if 'keep' == false
                        del currentRecord['value']['attributes'][currentAttribName]
            else:
                # variant attribute name
                if 'variants' in currentRecord and currentRecord ['variants']:
                    variantList = currentRecord ['variants']
                    for variantId, variantObj in variantList.items ():
                        if currentAttribName in variantObj ['attributes'].keys():
                            if (attribMap ['keep'] == True):
                                variantObj ['attributes'][newAttribName] = variantObj ['attributes'][currentAttribName]
                                if (newAttribName != currentAttribName):
                                    del variantObj ['attributes'][currentAttribName]
                            else:
                                # if 'keep' == false
                                del variantObj ['attributes'][currentAttribName]

        return 

    # currently, selectedAttribs are ONLY at product level, therefore this code checks only those (not variant)
    # Some description values have multiple proper names. Replace all of them
    # by randomly selected replacement name
    def _replaceProperNames (self, currentRecord):
        # title, description, ... contain proper names; replace them
        # Note - some product descriptions are <blank> in source feed
        for selectedAttrib in am.ATTRIBUTES_WITH_PROPERNAMES:
            if selectedAttrib in currentRecord ['value']['attributes']:
                origValue = currentRecord ['value']['attributes'][selectedAttrib]
                if (origValue != None) and (len (origValue) > 0):
                    for properName in am.EXCLUDE_PROPERNAMES:
                        if (origValue.find (properName) >= 0):
                            # find a random name to replace it by
                            randomValue = random ()
                            randomIndx = (int) (randomValue * len (am.CONVERTED_PROPERNAMES))
                            replaced_value = am.CONVERTED_PROPERNAMES [randomIndx]
                            origValue = origValue.replace (properName, replaced_value)
                            # if we find even one properName exists, continue to check for more
                            continue
                    currentRecord ['value']['attributes'][selectedAttrib] = origValue
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
                
    def _lookupGenimageUrl (self, pid):
        for aMap in self._genimage_map:
            if (aMap ['pid'] == pid):
                # currently, use the 0th image generated by openAI by default
                return aMap ['gen_image_0'] 
        return None

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


