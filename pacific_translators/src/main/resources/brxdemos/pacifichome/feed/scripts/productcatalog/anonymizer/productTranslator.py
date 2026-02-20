import xml.etree.ElementTree as ET
import logging
from random import random
import re

import translateConsts as tc
import categoryInfo as ci

class ProductTranslator ():

    def __init__ (self):
        self._processedSkuIds = []
        return

    # params:
    # sourceProduct: xml node (element)
    # updates ouput dict with transformed values. Returns outputDict
    # If sku is duplicate (ie, included in multiple products), returns None 
    def translateOneProduct (self, sourceProduct, categoryInfo):
        # Some sku's are included in multiple products; exclude them
        thisSkuId = sourceProduct.find (tc.PRODUCT_SKU_FIELD_NAME).text
        if (thisSkuId in self._processedSkuIds):
            return None

        # we need to use 'pid' to generate imageURLs in BR output. Therefore
        # scan the entire product fields to get pid value
        thisPid = None
        thisPrice  = None
        for aField in sourceProduct.iter ():
            if (aField.tag == tc.KEY_NAME_PID):
                thisPid = aField.text
            elif (aField.tag == tc.KEY_NAME_PRICE):
                thisPrice = aField.text
            if (thisPid != None) and (thisPrice != None):
                break    

        if (thisPid == None or thisPrice == None):
            logging.error ("ERROR Source product does not have pid and/or price; setting it to 0000")
            thisPid = '000000'
            thisPrice = '000000'

        # transform price once. Same is then used to calculate sale_price 
        transformedPrice = self._transformPrice (thisPrice)

        outputDict = {}
        for aField in sourceProduct.iter ():
            #logging.debug ('DEBUG %s: %s' % (aField.tag, aField.text))

            if (aField.tag == tc.SOURCE_PRODUCT_NODE_NAME): # skip 'product'
                continue # skip 'product'
            elif (aField.tag == tc.KEY_NAME_PRICE):
                transformedText = transformedPrice
            elif (aField.tag == tc.KEY_NAME_SALE_PRICE):
                onSale, sale_price = self._transformSalePrice (transformedPrice)
                outputDict [tc.KEY_NAME_SALE_PRICE] = sale_price
                outputDict [tc.KEY_NAME_ONSALE] = onSale 
                continue 
            elif (aField.tag == tc.KEY_NAME_ONSALE):
                continue    # re-calculated onSale value above
            elif (aField.tag == tc.KEY_NAME_BREADCRUMB):
                # for given crumb, get corresponding crumb_id and enter into outputProduct
                transformedCrumbs, transformedCrumbsId = self._transformBreadcrumb (aField.text, categoryInfo)
                # add both crumb, crumbId in output and continue
                outputDict [tc.KEY_NAME_BREADCRUMB] = transformedCrumbs
                outputDict [tc.KEY_NAME_BREADCRUMB_ID] = transformedCrumbsId
                continue
            elif (aField.tag == tc.KEY_NAME_GOOG_CATEGORY):
                continue    # skip this in output
            elif (aField.tag == tc.KEY_NAME_LEAF_CATEGORIES):
                continue    # skip this in output
            elif (aField.tag == tc.KEY_NAME_STOCK_LEVEL):
                continue    # skip this in output
            elif (aField.tag == tc.KEY_NAME_URL):
                transformedText = self._transformUrl (aField.text, thisPid)
            elif (aField.tag == tc.KEY_NAME_THUMB_IMAGE):
                transformedText = self._transformThumbImageUrl (aField.text, thisPid)
            elif (aField.tag == tc.KEY_NAME_LARGE_IMAGE):
                transformedText = self._transformLargeImageUrl (aField.text, thisPid)
            elif (aField.tag == tc.KEY_NAME_SKU_THUMB_IMAGE):
                transformedText = self._transformLargeImageUrl (aField.text, thisPid)
            elif (aField.tag == tc.KEY_NAME_SKU_LARGE_IMAGE):
                transformedText = self._transformSkuImageUrl (aField.text, thisPid)
            elif (aField.tag == tc.KEY_NAME_TITLE):
                transformedText = self._transformTitle (aField.text)
            else:
                if (aField.text != None):
                    transformedText = self._cleanText (aField.text) 
                else:
                    transformedText = ''

            outputDict [aField.tag] = transformedText 
            self._processedSkuIds.append (thisSkuId)    # avoid duplicates
        return outputDict


    def _transformPrice (self, origPrice):
        transformed = float (origPrice)
        randomValue = random ()
        if (randomValue < 0.5):
            randomMultiplier = transformed * (1 + randomValue)
        transformed = '%.2f' % transformed
        return (str (transformed))

    # NOTE: param is transformed price (str format)
    def _transformSalePrice (self, transformedPrice):
        # reduce transformed price by a random factor for 15% of products
        # IE, 15% products are onSale
        randomValue = random ()
        if (randomValue < 0.15):
            transformedPriceFloat = float (transformedPrice)
            randomValue = random ()
            if (randomValue < 0.65) or (randomValue > 0.75):
                randomValue = 0.65
            transformedSalePriceFloat = transformedPriceFloat * randomValue
            transformedSalePrice = '%.2f' % transformedSalePriceFloat
            onSale = True
        else:
            onSale = False
            transformedSalePrice = transformedPrice # sale_price == price

        return (str(onSale).lower(), transformedSalePrice)

    # product record has breadCrumbs (eg, 'L0/L1/L2|L10>L11>L12|...')
    # Get corresponding crumbsId and return both
    def _transformBreadcrumb (self, fullSourceCrumbs, categoryInfo):
        fullCrumbsId = None 
        fullCrumbs = None 

        srcCrumbsBranchList = fullSourceCrumbs.split (tc.BREADCRUMB_PARENTVALUE_DELIMITER_IN)
        for aSrcCrumbsBranch in srcCrumbsBranchList:
            aCrumbsBranch = aSrcCrumbsBranch.replace (' > ', '>')
            aCrumbsBranch = self._cleanText (aCrumbsBranch)
            aCrumbsIdBranch = categoryInfo.getFullCrumbIds (aCrumbsBranch) # for 'L0>L1>L2' -> '111/222/333'
            # replace '/' -> '>' used in BR side
            aCrumbsIdBranch = aCrumbsIdBranch.replace ('/', tc.BREADCRUMBID_VALUE_DELIMITER)
            if (fullCrumbsId == None):
                fullCrumbs = aCrumbsBranch
                fullCrumbsId = aCrumbsIdBranch
            else:
                fullCrumbs = fullCrumbs + tc.BREADCRUMB_PARENTVALUE_DELIMITER_OUT + aCrumbsBranch
                fullCrumbsId = fullCrumbsId + tc.BREADCRUMB_PARENTVALUE_DELIMITER_OUT + aCrumbsIdBranch

        # return both crumbs and crumbsId        
        return (fullCrumbs, fullCrumbsId)

    def _transformUrl (self, origUrl, thisPid):
        anonymUrl = tc.DEFAULT_URL_PREFIX + thisPid + tc.DEFAULT_URL_POSTFIX
        return anonymUrl

    def _transformThumbImageUrl (self, origImgUrl, thisPid):
        anonymUrl = tc.DEFAULT_IMAGE_URL_PREFIX + thisPid + tc.DEFAULT_IMAGE_URL_POSTFIX
        return anonymUrl

    def _transformLargeImageUrl (self, origImgUrl, thisPid):
        anonymUrl = tc.DEFAULT_IMAGE_URL_PREFIX + thisPid + tc.DEFAULT_IMAGE_URL_POSTFIX
        return anonymUrl

    def _transformSkuImageUrl (self, origImgUrl, thisPid):
        anonymUrl = tc.DEFAULT_IMAGE_URL_PREFIX + thisPid + tc.DEFAULT_IMAGE_URL_POSTFIX
        return anonymUrl

    # title sometimes contains RegisteredTM and sometimes &reg; -- replace both
    def _transformTitle (self, origTitle):
        transformed = re.sub ('\\u00AE', '', origTitle)
        transformed = transformed.replace ('&reg;', '')
        transformed = self._cleanText (transformed)
        return transformed

    # some fields have 'world market' (and it variations) -- replace them
    def _cleanText (self, origText): 
        transformed = origText
        if (transformed.find (tc.SOURCE_COMPANY_NAME) >= 0):
            transformed = transformed.replace (tc.SOURCE_COMPANY_NAME, tc.REPLACED_COMPANY_NAME)
        if (origText.find (tc.RESERVED_TEXT_1) >= 0):
            transformed = transformed.replace (tc.RESERVED_TEXT_1, tc.REPLACED_RESERVED_TEXT_1)
        return transformed

