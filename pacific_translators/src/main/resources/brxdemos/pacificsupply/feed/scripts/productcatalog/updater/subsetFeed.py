# generate a subset from updated catalog. This subset is then used to index into
# pacific*global accounts (along with their translations)

import logging
import csv
import catidlistreader as cilr
import updaterConstants as uc

class SubsetFeed ():
    def __init__ (self):
        self._updatedFullFeed = None 
        return

    def setUpdatedFeed (self, updatedFullFeed):
        self._updatedFullFeed = updatedFullFeed

    def prepareSubset (self):
        # read selected catIds to generated subset feed
        srcCatIdList = self._readSubsetCatIdList () 

        subsetProductRecords = self._collectProductRecordsFromFeed (srcCatIdList)
        if (len (subsetProductRecords) == 0):
            logging.warning ('Subset product feed size is 0')
        subsetAttributeList = self._collectSubsetAttributeList (subsetProductRecords)

        return subsetProductRecords, subsetAttributeList

    def _readSubsetCatIdList (self):
        # read src catId list
        listReader = cilr.CatIdListReader ()
        srcCatIdList = listReader.read (uc.FILENAME_SUBSET_CATID_LIST_IN)
        return srcCatIdList

    def _collectProductRecordsFromFeed (self, srcCatIdList):
        subsetProductRecords = []
        collectedPids = [] # used to avoid duplicate pids in subset
        totalProductsInCat = [] # {catId, numProds}

        for i in range (0, len (srcCatIdList)):
            totalProductsInCat.append ({'catId': srcCatIdList [i]['L2 catId'], 'numProds': 0} )

        for catRecord in srcCatIdList:
            catId = catRecord ['L2 catId']
            for srcProduct in self._updatedFullFeed:            
                categoryPaths = srcProduct ['value']['attributes']['category_paths']
                # categoryPaths is list-of-lists
                for fullPath in categoryPaths:
                    for branch in fullPath:
                        if (branch ['id'] == catId):
                            if (srcProduct ['value']['attributes']['availability'] == True):
                                if srcProduct ['value']['attributes']['pid'] not in collectedPids:
                                    if (self._okToAddProductInCat (catId, totalProductsInCat) == True):
                                        subsetProductRecords.append (srcProduct.copy())
                                        collectedPids.append (srcProduct ['value']['attributes']['pid'])
                                        logging.debug ('catId = %s, pid = %s', catId, srcProduct ['value']['attributes']['pid'])

        return subsetProductRecords

    # we pick max "MAX" products in each subset category so that total# products in the
    # subset is ~300
    def _okToAddProductInCat (self, catId, totalProductsInCat):
        for i in range (0, len (totalProductsInCat)):
            if (totalProductsInCat [i]['catId'] == catId):
                if (totalProductsInCat [i]['numProds'] < uc.SUBSET_MAX_PRODUCTS_TO_USE_IN_CATEGORY):
                    totalProductsInCat [i]['numProds'] = totalProductsInCat [i]['numProds'] + 1
                    return True
                else:
                    return False
        return False    # should never reach here


    def _collectSubsetAttributeList (self, subsetProductRecords):
        subsetAttributeList = []

        for subsetProduct in subsetProductRecords:
            prodAttribs = subsetProduct ['value']['attributes']
            for attrib in prodAttribs.keys():
                if attrib not in subsetAttributeList:
                    subsetAttributeList.append (attrib)

            if 'variants' in subsetProduct ['value'] and subsetProduct ['value']['variants']:
                variantList = subsetProduct ['value']['variants']
                for variantId, variantObj in variantList.items ():
                    for key in variantObj ['attributes'].keys ():
                        if key not in subsetAttributeList:
                            subsetAttributeList.append (key)

        # attribute list sorted just before writing it to attribList
        return subsetAttributeList

if __name__ == '__main__':
    logging.basicConfig (level=logging.DEBUG)
    subsetFeed = SubsetFeed ()
    subsetFeed.setUpdatedFeed (None)
    subsetFeed.prepareSubset ()
    logging.info ('Finish...')

    
