# generate a subset from updated catalog. This subset is then used to index into
# pacific*global accounts (along with their translations)

import logging
import csv
import updaterConstants as uc

class SubsetFeed ():
    def __init__ (self):
        self._updatedFullFeed = None 
        return

    def setUpdatedFeed (self, updatedFullFeed):
        self._updatedFullFeed = updatedFullFeed

    def prepareSubset (self):
        subsetContentRecords = self._collectContentRecordsFromFeed ()
        if (len (subsetContentRecords) == 0):
            logging.warning ('Subset content feed size is 0')
        return subsetContentRecords

    def _collectContentRecordsFromFeed (self):
        subsetContentRecords = []
        if (uc.SUBSET_MAX_CONTENTS_TO_USE > 0): # if -ve, subset == full set
            subsetMax = uc.SUBSET_MAX_CONTENT_TO_USE
        else:
            subsetMax = len (self._updatedFullFeed)
        for i in range (0, subsetMax):
            subsetRecord = self._updatedFullFeed [i].copy ()
            subsetContentRecords.append (subsetRecord)
        return subsetContentRecords


if __name__ == '__main__':
    logging.basicConfig (level=logging.DEBUG)
    subsetFeed = SubsetFeed ()
    subsetFeed.setUpdatedFeed (None)
    subsetFeed.prepareSubset ()
    logging.info ('Finish...')

    
