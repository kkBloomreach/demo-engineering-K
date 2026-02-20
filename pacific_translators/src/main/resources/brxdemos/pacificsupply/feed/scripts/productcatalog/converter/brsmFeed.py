# PacificSupply catalog products don't have SKU's. Therefore all sku_x related code
# is removed (as compared to PacificHome's code).
# On the other hand, pacificSupply has views (which Pacifichome does not)

# 05082023: changed 'Series/Model' attrib name to 'model' for NumericPrecision feature
# 04062023: added brandMap to make brand names consistent

import csv
import logging

import attributeMap as am
import convertConstants as cc

class BRSMFeed ():

    def __init__ (self):
        self._brsm_raw_feed = []
        self._CRUMB_TO_ID_MAP = []
        self._adjusted_brsm_feed = []
        # brand_map: map MILWAUKEE -> Milwaukee etc for consistency
        # each dictionary entry: {original, mapped}
        self._brand_map = []

    def readBRSMFeed (self, srcFeedPath, brandMapPath):
        logging.info ("Reading brSM feed")
        # read source .tsv. Adds records to brsm_feed list
        self._read_sourcefeed (srcFeedPath)

        # read brand map
        logging.info ("Reading brand map")
        self._read_brand_map (brandMapPath)

        # adjust specific attributes (eg, url, availability)
        for record in self._brsm_raw_feed:
            adjustedRecord = self._adjustAttributes (record)
            self._adjusted_brsm_feed.append (adjustedRecord)

        # Note - pacificSupply products don't have SKU's (currently)
        logging.info ("product count in brSM feed: %s", len (self._brsm_raw_feed))
        return

    # returns iterator to brsm_feed (list of source records)
    def getProductIterator (self):
        return self._adjusted_brsm_feed.__iter__ ()

    # This method reads each-and-every field from the source feed
    # and saves each as a dictionary and appends to brsm_raw_feed dictionary list
    def _read_sourcefeed (self, filename):
        logging.info ("INFO reading source feed: " + filename)
        file_obj = open (filename, 'r')
        dict_reader = csv.DictReader (file_obj, delimiter='\t')

        for row in dict_reader:
            self._brsm_raw_feed.append (row);
        logging.info ("INFO total product record count in source feed = " + str (len (self._brsm_raw_feed)))
        return

    # read brand-map which helps to make brand names consistent
    def _read_brand_map (self, filename):
        logging.info ("INFO reading brand map: " + filename)
        file_obj = open (filename, 'r')
        dict_reader = csv.DictReader (file_obj, delimiter='\t')

        for row in dict_reader:
            self._brand_map.append (row);
        return

    def _adjustAttributes (self, record):
        adjustedRecord = {}

        for key in record.keys ():
            # adjust attrib name if needed
            attribMap = self._lookupAttribMapByOriginalName (key) 
            if (attribMap != None):
                if (attribMap ['keep'] == False):
                    continue
                else:
                    newName = attribMap ['newName']
                    adjustedRecord [newName] = record [key]
            else:
                adjustedRecord [key] = record [key]

            # replace original url value to SPA convention
            if (key == 'url'):
                url_orig = record ['url']
                rSlashIndx = url_orig.rindex ('/')
                pidValue = url_orig [rSlashIndx + 1:]
                url_mod = cc.PRODUCT_URL_PREFIX + pidValue  # pidvalue is already num___num in input
                #url_mod = cc.PRODUCT_URL_PREFIX + pidValue + '___' + pidValue
                adjustedRecord ['url'] = url_mod

            # set 'availability' = True / False
            if (key == 'availability'):
                if (record ['availability'] == 'in stock'):
                    adjustedRecord ['availability'] = True
                else:
                    adjustedRecord ['availability'] = False

            # for brand, copy to product_brand for groupBy feature support
            if (key == 'brand'):
                _mapped_brandName = self._lookupBrandMap (record ['brand'])
                adjustedRecord ['brand'] = _mapped_brandName
                adjustedRecord ['product_brand'] = _mapped_brandName

            # Boolean attrib values -> True / False
            # Do this for non-view-specific attributes. View_specific values
            # are set after the values are split-by-view
            if (key in am.BOOLEAN_ATTRIBUTES):
                if ((key in am.VIEW_SPECIFIC_ATTRIBUTE_NAMES) == False):
                    if (record [key] == 'true'):
                        adjustedRecord [key] = True
                    else:
                        adjustedRecord [key] = False

            # Float (eg number) values
            # Do this for non-view-specific attributes. View_specific values
            # are set after the values are split-by-view
            if (key in am.FLOAT_ATTRIBUTES):
                if ((key in am.VIEW_SPECIFIC_ATTRIBUTE_NAMES) == False):
                    adjustedRecord [key] = float (record [key])

            # For AQF, create internal 'velo_lower' attribute names
            # also set the value in lower case (required by BR's AQF algo)
            if (key in am.AQF_ATTRIBUTES):
                mapped_dict = self._lookupAttribMapByOriginalName (key)
                if (mapped_dict != None):
                    mapped_name = mapped_dict ['newName']
                else:
                    mapped_name = key
                if (record[key] and record[key] != ''):
                    dup_key = "velo_" + mapped_name + "_lower";
                    adjustedRecord [dup_key] = record [key].lower()

        return adjustedRecord

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

    def _lookupBrandMap (self, origBrandName):
        for oneMap in self._brand_map:
            if (oneMap ['original'] == origBrandName):
                return oneMap ['mapped'];

        logging.err ("No brand map for: %s", origBrandName);
        return origBrandName  # actually internal error

if __name__ == '__main__':
    feedHandler = BRSMFeed ()
    feedHandler.readBRSMFeed (cc.FILENAME_BRSM_FEED_IN, cc.FILENAME_BRAND_MAP_IN)


