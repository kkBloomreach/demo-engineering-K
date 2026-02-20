# using jsonl feed as input, generate a tabular feed. It is then
# writen out as .tsv and .csv

import logging

class TabularFeed ():

    def __init__ (self):
        self._updatedProducts = None
        self._updatedAttributeList = None
        return

    # updatedProducts in jsonl format
    def setUpdatedProducts (self, updatedProducts, updatedAttributeList): 
        self._updatedProducts = updatedProducts
        self._updatedAttributeList = updatedAttributeList

    # generate tabular object comprising info for all brsm products
    # returns list of those objects.
    def buildTabularFeed (self):
        # collect list of ALL attributes from ALL products AND their variants
        # That will generate the 'header' line in tabular feed
        _tabularMasterDict = {}
        for attrib in self._updatedAttributeList:
            _tabularMasterDict [attrib] = ''

        # clone masterDict for each product and its variants
        _tabularRecords = self._generateTabularRecords (_tabularMasterDict)
        return _tabularRecords

    # Note that total dict-entries equal (product-rec + #variant-records) for each product
    def _generateTabularRecords (self, masterDict):
        # list of _masterDict objects, containins all products and their variants
        _allTabularRecords = []

        for _aUpdatedProduct in self._updatedProducts:
            _productTabularRecord = masterDict.copy ()

            # product's own record
            for aKey, value in _aUpdatedProduct ['value']['attributes'].items ():
                if (aKey == 'price') or (aKey == 'sale_price'):
                    value = float (value)
                _productTabularRecord [aKey] = value
            _allTabularRecords.append (_productTabularRecord)

            # next, for each of the variants, create its own record, copy that
            # product's attributes and then overwrite variant-specific values 
            if 'variants' in _aUpdatedProduct ['value'] and _aUpdatedProduct ['value']['variants']:
                _variantList = _aUpdatedProduct ['value']['variants']
                for variantId, variantObj in _variantList.items ():
                    _oneTabularRecord = _productTabularRecord.copy ()
                    for aKey, value in variantObj ['attributes'].items ():
                        if (value != None):
                            # following to avoid embedded comma or \t in attribute value
                            if (type (value) == 'str'):
                                if (value.find (',') > 0):
                                    value = value.replace (',', ' ')
                                if (value.find ('\t') > 0):
                                    value = value.replace ('\t', ' ')
                        if (aKey == 'price') or (aKey == 'sale_price'):
                            value = float (value)
                        _oneTabularRecord [aKey] = value

                    _allTabularRecords.append (_oneTabularRecord)  # variant record

        return _allTabularRecords

 
if __name__ == '__main__':
    _tabularFeed = TabularFeed ()
    _tabularFeed.setUpdatedProducts (None) 
    _tabularFeed.buildTabularFeed ()
    logging.debug ("finish")

