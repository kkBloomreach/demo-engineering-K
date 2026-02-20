import logging
import re

class DCFeed ():

    def __init__ (self):
        return

    def setBRSMFeed (self, brsmFeed):
        self._brsmFeedHandler = brsmFeed

    # generate DC-style object comprising info for all brsm products
    # returns list of DC-style-product-objects
    def generateDCFeed (self):
        self._dcProducts = []
        self._attributeList = []

        _brsmProductIterator = self._brsmFeedHandler.getProductIterator ();
        for _aBrsmProductAndVariants in _brsmProductIterator: 
            _transProductRecord = self._transformProduct (_aBrsmProductAndVariants ['product'])
            _transProductVariants = self._transformVariants (_aBrsmProductAndVariants ['variants'])

            # construct DC-style object
            _aDCProduct = {}
            _aDCProduct ["op"] = "add"
            _aDCProduct ["path"] = "/products/"+str(_transProductRecord ['pid'])
            # product attributes
            _aDCProduct ["value"] = {}
            _aDCProduct ["value"]["attributes"] = _transProductRecord

            # product variants
            _aDCProductVariantsDict = {}
            for _aTransProductVariant in _transProductVariants:
                _variantSkuId = _aTransProductVariant ['skuid']
                _aDCProductVariantsDict [_variantSkuId] = {}
                _aDCProductVariantsDict [_variantSkuId]['attributes'] = _aTransProductVariant
            # add variants list to transDCProduct
            _aDCProduct ['value']['variants'] = _aDCProductVariantsDict
 
            # add this to class level dcFeed
            self._dcProducts.append (_aDCProduct) 

        logging.debug ("finish")

        # sort attributeList
        self._attributeList.append ('category_paths')
        self._attributeList.sort ();

        return (self._dcProducts, self._attributeList)

    # transfer all attributes from original brsmProduct attributes
    # Currently this feed has all the 'required' attributes names as expected (eg, 'price')
    def _transformProduct (self, brsmProduct):
        transRecord = {}
        for _key in brsmProduct.keys ():
            transRecord [_key] = brsmProduct [_key]

            # also add to attributeList if key is not there already
            if ((_key in self._attributeList) == False):
                self._attributeList.append (_key)
 
        totalCrumbs = brsmProduct ['bread_crumb']
        totalCrumbsId = brsmProduct ['bread_crumb_id']
        categoryPaths = self._constructCategoryPath (totalCrumbs, totalCrumbsId, brsmProduct ['pid'])
        transRecord["category_paths"] = categoryPaths
   
        return transRecord
 
    # totalCrumbs, totalCrumbsId -> DC style categoryPath 
    # parent value delimiter: '|'
    # value delimiter: '>'
    # 'pid' included to report error
    def _constructCategoryPath (self, totalCrumbs, totalCrumbsId, pid):
        categoryPaths = []
        if ((totalCrumbs != None) and (totalCrumbsId != None)):
            # if product is only in top-level category, there won't be a parent-value-delimiter
            if (totalCrumbs.find ('|') > 0):
                crumbsList = totalCrumbs.split ('|')
                crumbsIdList = totalCrumbsId.split ('|')
                if (len (crumbsList) == len (crumbsIdList)):
                    for i in range (0, len (crumbsList)):
                        aFullCrumbs = crumbsList [i]
                        aFullCrumbsId = crumbsIdList [i]
                        if (aFullCrumbs.find ('>') > 0):
                            crumbElements = aFullCrumbs.split ('>')
                            crumbIdElements = aFullCrumbsId.split ('>')
                            if (len (crumbElements) == len (crumbIdElements)):
                                aCategoryPath = []
                                for j in range (0, len (crumbElements)):
                                    aCategoryPath.append ({'id': crumbIdElements [j],
                                                           'name': self._cleanUpCrumbName (crumbElements [j]) } )
                                # bug #1 -- following line was indented; that is not correct 
                                categoryPaths.append (aCategoryPath)
                            else:
                                logging.error ('crumbs, crumbsId mismatch. Pid = %s', pid)
                        else:
                            aCategoryPath = []
                            aCategoryPath.append ({'id': aFullCrumbsId,
                                                   'name': self._cleanUpCrumbName (aFullCrumbs) } )
                            categoryPaths.append (aCategoryPath)
                else:
                    logging.error ('crumbs, crumbsId mismatch. Pid = %s', pid)
            else:
                # Product is in only one category, although it may have l0/l1/l2/... members
                aFullCrumbs = totalCrumbs
                aFullCrumbsId = totalCrumbsId
                if (aFullCrumbs.find ('>') > 0):
                    crumbElements = aFullCrumbs.split ('>')
                    crumbIdElements = aFullCrumbsId.split ('>')
                    if (len (crumbElements) == len (crumbIdElements)):
                        aCategoryPath = []
                        for j in range (0, len (crumbElements)):
                            aCategoryPath.append ({'id': crumbIdElements [j],
                                                   'name': self._cleanUpCrumbName (crumbElements [j]) } )
                        categoryPaths.append (aCategoryPath)
                else:
                    # single crumb,crumbid (eg, A, 10) in a list of multiple crumbs (eg, A|B, 10|20)
                    aCategoryPath = []
                    aCategoryPath.append ({'id': aFullCrumbsId,
                                           'name': self._cleanUpCrumbName (aFullCrumbs) } )
                    categoryPaths.append (aCategoryPath)

        return categoryPaths 

    # some original crumb names include the year 2018, 2017, ...
    # replace those with some meaningful 'new' values
    # 'original' values have multiple permutations of the year (2016->2019) in different 
    # string locations. For example:
    #   "2020 WK37 30% Furniture Sale"
    #   "2020 Greenpan"
    #   "... 2018"
    #   <etc>
    def _cleanUpCrumbName (self, origCrumbName):
        cleanedUpCrumbName = re.sub ("20[0-9]*\s[wWkK]*[\\s]*[0-9]*\s", "2023 ", origCrumbName)
        cleanedUpCrumbName = re.sub ("201[1-9]*[\\s$]*", "2023 ", cleanedUpCrumbName)
        cleanedUpCrumbName = cleanedUpCrumbName.replace ("2020", "2023")
        return (cleanedUpCrumbName)

    ###### variant transformation
    def _transformVariants (self, brsmProductVariants):
        _productVariantList = []
        for _aProductVariant in brsmProductVariants:
            _transVariant = {}
            for _key in _aProductVariant.keys ():
                _transVariant [_key] = _aProductVariant [_key]
                # also add to attributeList if key is not there already
                if ((_key in self._attributeList) == False):
                    self._attributeList.append (_key)

            _productVariantList.append (_transVariant)

        return _productVariantList

 
if __name__ == '__main__':
    _dcFeed = DCFeed ()
    _dcFeed.setBRSMFeed (None)
    _dcFeed.generateDCFeed ()

'''
        if (origCrumbName.find ('2016') >= 0):
            cleanedUpCrumbName = origCrumbName.replace ('2016', '2021')
        elif (origCrumbName.find ('2017') >= 0):
            cleanedUpCrumbName = origCrumbName.replace ('2017', '2021')
        elif (origCrumbName.find ('2018') >= 0):
            cleanedUpCrumbName = origCrumbName.replace ('2018', '2021')
        elif (origCrumbName.find ('2019') >= 0):
            cleanedUpCrumbName = origCrumbName.replace ('2019', '2022')
        elif (origCrumbName.find ('2020') >= 0):
            cleanedUpCrumbName = origCrumbName.replace ('2020', '2022')

''' 
