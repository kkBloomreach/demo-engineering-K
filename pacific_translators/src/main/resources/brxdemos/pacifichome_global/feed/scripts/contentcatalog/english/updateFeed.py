# For 
# 

import logging
import csv
from random import random 
import os

import jsonlFeedReader as jfr
import updaterConstants as uc

class UpdateFeed ():
    def __init__ (self):
        self.src_contents_en = []
        self.updated_contents_en = []   # actually same as src_contents, no updates
        self.updated_attributeList = []

    def setSourceContents (self, srcContents_en):
        self.src_contents_en = srcContents_en 

    def performUpdates (self):
        self._updateFeed ()
        return self.updated_contents_en, self.updated_attributeList

    def _updateFeed (self):
        logging.info ("process feed")

        # process attribute map (remove some attribs, rename, ...)
        for srcRecord in self.src_contents_en:
            updatedRecord = srcRecord.copy ()

            # collect all updated products
            self.updated_contents_en.append (updatedRecord)

            # accumulate attribute names across all contents
            self._collectAttributeList (updatedRecord)

        logging.info ("content in updated feed count: %s", len (self.updated_contents_en))
        return

    def _collectAttributeList (self, currentRecord):
        prodAttribs = currentRecord ['value']['attributes']
        for attrib in prodAttribs.keys():
            if attrib not in self.updated_attributeList:
                self.updated_attributeList.append (attrib)

        # attribute list sorted just before writing it to attribList
        return

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    srcFeedReader = jfr.JsonlFeedReader ()
    srcContents = srcFeedReader.readSourceFeed (uc.FILENAME_JSONL_SOURCE_FEED_EN_IN)

    feedUpdater = UpdateFeed ()
    feedUpdater.setSourceContents (srcContents)
    updatedContents, updatedAttributeList = feedUpdater.performUpdates ()
    logging.debug ('Finish ...')


