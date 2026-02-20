# For DataConnect, do not need to create any "sku_xxx" attributes
# See comments in PACIFICSUPPLYMINDCURV-101

import copy
import xml.etree.ElementTree as ET
import logging
import csv
from random import random 

import brsmFeedReader as bfr
import convertConstants as cc
import attributeMap as am

class BRSMFeed ():
    def __init__ (self):
        self._brsm_raw_feed = []    # records from input
        self._brsm_adjusted_feed = []   # records after adjusting attributes using ATTRIBUTE_MAP
        self._CRUMB_TO_ID_MAP = []
        # each elem is {product: <product record>, variants: List<product variant records>}
        self._brsmProductsAndVariantsList = []
        self._pid_brand_map = []
        self._pid_generated_attribute_value_map = []
        self._pid_to_delete_due_to_genimage = []

    def readBRSMFeed (self, srcPath):
        # read pacifichome actual catalog
        feedReader = bfr.BRSMFeedReader ()
        logging.info ('Read source product catalog...')
        self._brsm_raw_feed = feedReader.readBRSMFeed (cc.FILENAME_BRSM_FEED_IN)

        # read proxy products and add them to raw_feed
        logging.info ('Read proxy products...')
        feedReader = bfr.BRSMFeedReader ()
        proxy_products_raw_feed = feedReader.readBRSMFeed (cc.FILENAME_BRSM_PROXY_PRODUCTS_FEED_IN)
        if (proxy_products_raw_feed != None):
            for proxy in proxy_products_raw_feed:
                self._brsm_raw_feed.append (proxy)

        return

    # read pid-to-brand map (.tsv file)
    def readPidToBrandMap (self, srcPath):
        logging.info ("reading pid-to-brand map: " + srcPath)
        file_obj = open (srcPath, 'r')
        dict_reader = csv.DictReader (file_obj, delimiter='\t')

        for row in dict_reader:
            if (self._lookupPidBrand (row ['pid']) == None):    # enter only if not already in the list
                self._pid_brand_map.append (row)

        logging.info ("pid_to_brandmap count: %s", len (self._pid_brand_map))
        return

    # read pid-to-generated-attribute-values map (.tsv file)
    def readPidToGeneratedAttributeValueMap (self, srcPath):
        logging.info ("reading pid-to-generated-attribute-value map: " + srcPath)
        file_obj = open (srcPath, 'r')
        dict_reader = csv.DictReader (file_obj, delimiter='\t')

        for row in dict_reader:
            self._pid_generated_attribute_value_map.append (row)

        logging.info ("pid_to_generated_attribute_value_map count: %s", len (self._pid_generated_attribute_value_map))
        return

    # read list of pids to delete because we cannot generate their images via openAI
    def readPidToDeleteDueToGenimage (self, srcPath):
        logging.info ("reading pid-to-delete-due-to-genimage: " + srcPath)
        file_obj = open (srcPath, 'r')
        dict_reader = csv.DictReader (file_obj, delimiter='\t')

        for row in dict_reader:
            self._pid_to_delete_due_to_genimage.append (row)

        logging.info ("pid_to_delete_due_to_genimage count: %s", len (self._pid_to_delete_due_to_genimage))
        return

    def processBRSMFeed (self):
        logging.info ("process brsm feed")

        # process attribute map (remove some attribs, rename, ...)
        for record in self._brsm_raw_feed:
            # ignore (delete) product if it is in 'delete_due_to_genimage' list
            if self._isPidToDeleteDueToGenimage (record ['pid']) == True:
                continue

            # adjust specific attributes (eg, url, availability)
            # Note, source (original) record remains unchanged
            adjustedRecord = self._adjustAttributes (record)

            # add 'new'/'extended' attributes to adjustedRecord (eg, margin, brand)
            self._extendAdjustedRecord (adjustedRecord)
            self._brsm_adjusted_feed.append (adjustedRecord) 

        logging.info ("product + variants in brSM adjusted feed count: %s", len (self._brsm_adjusted_feed))

        # split colorFamily and sizeFamily attribs from multi-value to single-value. 
        # Do this before product / variant split and sorting
        # The color and size are expected to be single value attribs in each sku
        # However, in this feed, some are multi value (eg, Black|White). Split such records
        # into multiple records with single-value in each (aka 'extended' feed)
        self._splitColorFamilyAttribs ()
        self._splitSizeFamilyAttribs ()

        # xml feed is sku-level feed. Isolate products and their variants
        # since dataConnect needs it that way
        # Builds list of {product: <productRecord>, variants: <productVariantRecords>}
        self._splitProductsAndVariants ()

        logging.info ("product in brSM feed count: %s", len (self._brsmProductsAndVariantsList))
        return

    # returns iterator to productAndVariantsList
    def getProductIterator (self):
        return self._brsmProductsAndVariantsList.__iter__ ()

    def _lookupPidBrand (self, pid):
        for oneMap in self._pid_brand_map:
            if (oneMap ['pid'] == pid):
                return oneMap
        return None

    def _lookupPidToGeneratedAttributeValues (self, pid):
        for oneMap in self._pid_generated_attribute_value_map:
            if (oneMap ['pid'] == pid):
                return oneMap
        return None

    def _isPidToDeleteDueToGenimage (self, pid):
        for onePidMap in self._pid_to_delete_due_to_genimage:
            if (onePidMap ['pid_to_delete'] == pid):
                return True
        return False

    # remove attrib, rename as defined in ATTRIBUTE_MAP for given product record
    def _adjustAttributes (self, record):
        adjustedRecord = {}

        for key in record.keys ():
            attribMap = self._lookupAttribMapByOriginalName (key)
            if (attribMap != None):
                if (attribMap ['keep'] == False):
                    continue    # not included in output record
                else:
                    newName = attribMap ['newName']
                    adjustedRecord [newName] = record [key]
            else:
                adjustedRecord [key] = record [key]
        return adjustedRecord

    # add/update generated attribute values for given pid
    # Only specific attributes have 'generated' values (margin, condition, start_date, end_date, lowstock)
    def _extendAdjustedRecord (self, adjustedRecord):
        # force brand attrib
        if ('brand' not in adjustedRecord) or (adjustedRecord ['brand'] == None):
            pidToBrandMap = self._lookupPidBrand (adjustedRecord ['pid'])
            if (pidToBrandMap != None):
                adjustedRecord ['brand'] = pidToBrandMap ['brand']
                adjustedRecord ['product_brand'] = adjustedRecord ['brand']
            else:
                adjustedRecord ['brand'] = cc.PACIFIC_HOME_PRODUCT_BRAND_DEFAULT
                adjustedRecord ['product_brand'] = adjustedRecord ['brand']

        pidToGeneratedAttributeValues = self._lookupPidToGeneratedAttributeValues (adjustedRecord ['pid'])
        if (pidToGeneratedAttributeValues != None):
            adjustedRecord ['margin'] = pidToGeneratedAttributeValues ['margin']
            adjustedRecord ['condition'] = pidToGeneratedAttributeValues ['condition']
            adjustedRecord ['start_date'] = pidToGeneratedAttributeValues ['start_date']
            adjustedRecord ['end_date'] = pidToGeneratedAttributeValues ['end_date']
            adjustedRecord ['lowstock'] = pidToGeneratedAttributeValues ['lowstock']

        # Internally generated additional attributes
        if 'material' in adjustedRecord and adjustedRecord ['material']:
            adjustedRecord ['velo_material_lower'] = adjustedRecord ['material'].lower ()

    def _lookupAttribMapByOriginalName (self, key):
        for oneMap in am.ATTRIBUTE_MAP:
            if (oneMap ['name'] == key):
                return oneMap
        return None

    # originalName may have been renamed to "newName"
    # If attrib is deleted, its newName is blank in AttributeMap
    def _lookupAttribMapByNewName (self, key):
        for oneMap in am.ATTRIBUTE_MAP:
            if (oneMap ['newName'] == key):
                return oneMap
        return None

    def _splitColorFamilyAttribs (self):
        _extended_brsm_adjusted_feed = []

        try:
            for _i, _record in enumerate (self._brsm_adjusted_feed):
                if ('colorFamily' in _record) and (_record ['colorFamily']):
                    _value = _record ['colorFamily']
                    if (_value.find ('|') > 0):
    
                        # split values and add as many records as in the list-of-values, 
                        # each additional record with one of the values
                        _valueList = _value.split ('|')
    
                        for _j, _aValue in enumerate (_valueList):
                            _copyRecord = copy.copy (_record)
                            _newSkuId = _record ['skuid'] + '_' + str (_j)
                            _copyRecord ['skuid'] = _newSkuId
                            _copyRecord ['colorFamily'] = _aValue
                            _extended_brsm_adjusted_feed.append (_copyRecord)
                    else:
                        # colorFamily is single value - append as-is
                        _extended_brsm_adjusted_feed.append (_record)
                else:
                    # colorFamily not in the record - append as-is
                    _extended_brsm_adjusted_feed.append (_record)
        except Exception as e:
            logging.error ('Exception in splitColorFamilyAttribute %s', e)

        self._brsm_adjusted_feed = _extended_brsm_adjusted_feed
        return
 
    def _splitSizeFamilyAttribs (self):
        _extended_brsm_adjusted_feed = []

        try:
            for _i, _record in enumerate (self._brsm_adjusted_feed):
                if ('sizeFamily' in _record) and (_record ['sizeFamily']):
                    _value = _record ['sizeFamily']
                    if (_value.find ('|') > 0):
    
                        # split values and add as many records as in the list-of-values, 
                        # each additional record with one of the values
                        _valueList = _value.split ('|')
    
                        for _j, _aValue in enumerate (_valueList):
                            _copyRecord = copy.copy (_record)
                            _newSkuId = _record ['skuid'] + '_' + str (_j)
                            _copyRecord ['skuid'] = _newSkuId
                            _copyRecord ['sizeFamily'] = _aValue
                            _extended_brsm_adjusted_feed.append (_copyRecord)
                    else:
                        # sizeFamily is single value - append as-is
                        _extended_brsm_adjusted_feed.append (_record)
                else:
                    # sizeFamily not in the record - append as-is
                    _extended_brsm_adjusted_feed.append (_record)
        except Exception as e:
            logging.error ('Exception in splitSizeFamilyAttribute %s', e)

        # set self._brsm_adjusted_feed to _extended
        self._brsm_adjusted_feed = _extended_brsm_adjusted_feed

    def _splitProductsAndVariants (self): 
        # first sort the brsm_adjusted_feed list using pid for sort-key
        _sorted_brsm_adjusted_feed = sorted (self._brsm_adjusted_feed, key = lambda row: row ['pid'])

        _currentPid = None
        _productRecord = None
        _productVariants = []
        for _record in _sorted_brsm_adjusted_feed:
            if (_record ['pid'] != _currentPid):
                # new pid
                _currentPid = _record ['pid']

                # If we have processed a product and its variants, 
                # append it to productAndVariants list
                if (_productRecord != None):
                    _deDupedProductVariants = self._removeDuplicateVariants (_productVariants)
                    _productAndVariants = {
                                            'product': _productRecord,
                                            'variants': _deDupedProductVariants
                                          }
                    # append this to class-level list of productsAndVariants
                    self._brsmProductsAndVariantsList.append (_productAndVariants)

                # Append product record in variants list as well since this is sku-feed
                # IE, same source "_record" is added in as a 'product' record as well as a 'variant' record
                _productRecord = self._prepareProductRecord (_record)
                _productVariants = []
                _variantRecord = self._prepareVariantRecord (_record)

                # specifically for SKU_SELECT. Set 0th sku as the 'default_sku'
                _variantRecord ['default_sku'] = True
                _productVariants.append (_variantRecord)
            else:
                # another variant of previous product
                _variantRecord = self._prepareVariantRecord (_record)
                _productVariants.append (_variantRecord)

        # append the last product and its variants
        _deDupedProductVariants = self._removeDuplicateVariants (_productVariants)
        _productAndVariants = {
                                'product': _productRecord,
                                'variants': _deDupedProductVariants
                              }
        # append this to class-level list of productsAndVariants
        self._brsmProductsAndVariantsList.append (_productAndVariants)

        return

    # in this feed, there are duplicate variants; deDup them
    def _removeDuplicateVariants (self, productVariants):
        _deDupedVariants = []

        for aVariant in productVariants:
            aColorValue = None
            aSizeValue = None
            if ('color' in aVariant) and (aVariant ['color']):
                aColorValue = aVariant ['color']
            if ('size' in aVariant) and (aVariant ['size']):
                aSizeValue = aVariant ['size']

            # if another variant with same color AND size exists, exclude this variant
            # note, the values could be "None" as well
            if (self._isDuplicateVariant (aColorValue, aSizeValue, _deDupedVariants) == True):
                continue
            else:
                _deDupedVariants.append (aVariant)

        return _deDupedVariants

    def _isDuplicateVariant (self, currentColorValue, currentSizeValue, priorVariants):
        for aPriorVariant in priorVariants:
            aPriorColorValue = None
            aPriorSizeValue = None

            # aPrior variant's color, size values
            if ('color' in aPriorVariant) and (aPriorVariant ['color']):
                aPriorColorValue = aPriorVariant ['color']
            if ('size' in aPriorVariant) and (aPriorVariant ['size']):
                aPriorSizeValue = aPriorVariant ['size']

            # note, the values could be "None" as well
            if ((currentColorValue == aPriorColorValue) and (currentSizeValue == aPriorSizeValue)):
                return True

        return False


    # we include a subset of record attributes in a 'product' record
    # EG, 'pid' attribute is NOT included in variant record
    # Note, a new 'productRecord' is built, adjustedRecord in input remains unchanged
    def _prepareProductRecord (self, adjustedRecord):

        productRecord = {}
        for key in adjustedRecord.keys ():
            attribMap = self._lookupAttribMapByNewName (key)
            if (attribMap != None):
                if (attribMap ['isProductAttrib'] == False):    # not a product-level attribute
                    continue

            # skip the key 'product' - not needed
            if (key == 'product'):
                continue

            if (key == 'url'):
                url_orig = adjustedRecord ['url']
                rSlashIndx = url_orig.rindex ('/')
                pidValue = url_orig [rSlashIndx+1:]
                url_mod = cc.PRODUCT_URL_PREFIX + pidValue + '___' + pidValue
                productRecord ['url'] = url_mod
                continue

            # availability = "In stock" => True
            # Other possible values in this feed are "on backorder / Out of stock / Temporarily out of stock"
            if (key == 'availability') :
                if (adjustedRecord [key] == 'In stock'):
                    productRecord ['availability'] = True
                    productRecord ['inStock'] = True
                else:
                    productRecord ['availability'] = False
                    productRecord ['inStock'] = False
                continue

            # for attributes with true/false values, set them as bool(value)
            # Otherwise these values are saved as string(true) or string(false)
            if (key in am.BOOLEAN_ATTRIBUTES):
                if (adjustedRecord [key] == 'true'):
                    productRecord [key] = True
                else:
                    productRecord [key] = False
                continue
 
            if (key in am.FLOAT_ATTRIBUTES):
                productRecord [key] = float (adjustedRecord [key]) 
                continue
 
            # bestSeller - map 0 -> False, 1 -> True (per SC request)
            if (key == 'bestSeller'):
                if (adjustedRecord [key] == "0"):
                    productRecord [key] = False
                else:
                    productRecord [key] = True
                continue

            # shippingInfo and promotion values are sometimes lower-case, sometimes upper case
            if ((key == 'shipping_info') or (key == 'promotionNow')):
                if (adjustedRecord [key] != None):
                    if (adjustedRecord[key].lower () == "free shipping!"):
                        productRecord [key] = "Free Shipping!"
                    else:
                        productRecord [key] = adjustedRecord [key]  # as is (eg, "Free Christmas delivery")
                else:
                    productRecord [key] = adjustedRecord [key]
                continue

            # title, description, ... contain proper names; replace them
            # Note - some product descriptions are <blank> in source feed
            if (key in am.ATTRIBUTES_WITH_PROPERNAMES): 
                if (adjustedRecord [key] != None):
                    replaced_value = self._replaceProperName (adjustedRecord [key])
                    productRecord [key] = replaced_value
                else:
                    productRecord [key] = None
                continue

            # some keywords have 'world market' string. Replaced it with pacifichome
            if (key == 'keywords'):
                origValue = adjustedRecord [key]
                if (origValue != None):
                    if (origValue.find ('world market') >= 0):
                        replacedValue = origValue.replace ('world market', 'pacifichome')
                        productRecord [key] = replacedValue
                    else:
                        productRecord [key] = adjustedRecord [key]
                continue

            # all other keys, just transfer to output
            productRecord [key] = adjustedRecord [key]

        return productRecord


    # we include only a few record attributes in a 'variant' record
    # EG, 'pid' attribute is NOT included in variant record
    # Note: colorFamily, sizeFamily variant-level attributes are multi-value
    def _prepareVariantRecord (self, record):
        _variantRecord = {}

        for key in record.keys ():
            attribMap = self._lookupAttribMapByNewName (key)
            if (attribMap != None):
                if ((attribMap ['keep'] == True) and (attribMap ['isProductAttrib'] == False)):    # is variant-level attribute
                    _variantRecord [key] = record [key]
                    continue
            else:
                continue    # no attribMap found

        # specifically for SKU_SELECT feature -- 
        # -- actual attribute name in variants does not include 'sku_' prefix (PACIFICSUPPLYMINDCURV-101)
        # IE, variant attribName does NOT include 'sku_'; just the base attribute name (eg, 'size', not sku_size)

        # sku color
        if ('colorFamily' in record) and (record ['colorFamily']):
            # sku color: if 'Multi', replace it with one of the predecided colors
            if (record ['colorFamily'] == 'Multi'):
                randomValue = random ()
                colorIndx = (int) (randomValue * len (am.COLOR_MULTI_REPLACEMENTS))
                _variantRecord ['color'] = am.COLOR_MULTI_REPLACEMENTS [colorIndx]
            else:
                _variantRecord ['color'] = record ['colorFamily']

        # sku size 
        if 'sizeFamily' in record and record ['sizeFamily']:
            _variantRecord ['size'] = record ['sizeFamily']

        # sku price
        if 'price' in record and record ['price']:
            _variantRecord ['price'] = record ['price']

        # sku sale_price
        if 'sale_price' in record and record ['sale_price']:
            _variantRecord ['sale_price'] = record ['sale_price']

        # add BR-required attribs in output variant record 
        # swatch_image
        # NOTE: 'sku_thumb_image' is a field in the SOURCE feed record. 
        # When processing AttributeMap, it is already renamed to 'swatch_image'
        if (not ('swatch_image' in record)):
            # following assumes skuid is definitely available in each record
            _variantRecord ['swatch_image'] = record ['skuid']

        # velo_aaa because those are currently defined in devStudio
        _variantRecord ['velo_sku_price'] = record ['price']
        _variantRecord ['velo_sku_sale_price'] = record ['sale_price']

        return _variantRecord

    # Some description values have multiple proper names. Replace all of them
    # by randomly selected replacement name
    def _replaceProperName (self, origValue):
        for properName in am.EXCLUDE_PROPERNAMES:
            if (origValue.find (properName) >= 0):
                # find a random name to replace it by
                randomValue = random ()
                randomIndx = (int) (randomValue * len (am.CONVERTED_PROPERNAMES))
                replaced_value = am.CONVERTED_PROPERNAMES [randomIndx]
                origValue = origValue.replace (properName, replaced_value)
                # if we find even one properName exists, continue to check for more
                continue
        return origValue

if __name__ == '__main__':
    feedHandler = BRSMFeed ()
    feedHandler.readBRSMFeed (cc.FILENAME_BRSM_FEED_IN)
    feedHandler.readPidToBrandMap (cc.FILENAME_PID_BRANDMAP_IN)
    feedHandler.processBRSMFeed ()


''' MOVED to attributeMap.py
BOOLEAN_ATTRIBUTES = [
    'OnlineOnly',
    'inStoreOnly',
    'onSale',
    'pickUpInStore',
    'shipToStore',
    'lowstock'
]

FLOAT_ATTRIBUTES = [
    'price',
    'sale_price',
    'reviews',
    'rating',
    'sale_price_range_min',
    'sale_price_range_max',
    'velo_sku_price',
    'velo_sku_sale_price',
    'margin'
]
'''
