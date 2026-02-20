# generate a subset from updated catalog. This subset is then used to index into
# pacific*global accounts (along with their translations)

import logging
import csv
import catidlistreader as cilr
import updaterConstants as uc

class SubsetFeedV2 ():
    def __init__ (self):
        self._updatedFullFeed = None 
        return

    def setUpdatedFeed (self, updatedFullFeed):
        self._updatedFullFeed = updatedFullFeed

    # In v2, entire input catalog is copied to the 'subset'
    def prepareSubset (self):
        subsetProductRecords = []

        for srcRecord in self._updatedFullFeed:
            subsetProductRecords.append (srcRecord.copy())
        subsetAttributeList = self._collectSubsetAttributeList (subsetProductRecords)
        if (len (subsetProductRecords) == 0):
            logging.warning ('Subset product feed size is 0')

        return subsetProductRecords, subsetAttributeList

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
    subsetFeed = SubsetFeedV2 ()
    subsetFeed.setUpdatedFeed (None)
    subsetFeed.prepareSubset ()
    logging.info ('Finish...')

    
