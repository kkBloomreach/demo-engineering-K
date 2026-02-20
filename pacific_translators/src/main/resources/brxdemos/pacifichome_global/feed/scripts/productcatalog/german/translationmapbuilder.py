# For unknown reason, openAI translates the same en terms to different de terms. That is
# not acceptable to build a catalog (for example, a catid must have its name consistent across all
# category_path instances). Therefore we build a 'translationmap' and reuse it. 

import logging
import json
import csv

import jsonlFeedReader as jfr
import updaterConstants as uc

class BuildTranslationMap (): 

    def __init__ (self):
        self.enCatalogPath = None
        self.deCatalogPath = None
        return

    def setEnglishCatalog (self, enCatalogPath) :
        self.enCatalogPath = enCatalogPath

    def setGermanCatalog (self, deCatalogPath):
        self.deCatalogPath = deCatalogPath 

    def buildMaps (self):
        jsonlReader = jfr.JsonlFeedReader ()
        enCatalog = jsonlReader.readSourceFeed (self.enCatalogPath)

        jsonlReader = jfr.JsonlFeedReader ()
        deCatalog = jsonlReader.readSourceFeed (self.deCatalogPath)

        # list of {'termtype', 'enterm value', 'determ value'}
        # list of {'catId', 'catName_en', 'catName_de'}
        termMap, categoryMap = self._buildTermAndCategoryMaps (enCatalog, deCatalog)

        return termMap, categoryMap

    def _buildTermAndCategoryMaps (self, enCatalog, deCatalog):
        termMap = []
        categoryMap = []

        # first build english-terms list
        self._buildEnMaps (enCatalog, termMap, categoryMap)

        # update maps with corresponding german terms
        self._updateGermanTermsInMaps (deCatalog, termMap, categoryMap, enCatalog)

        return termMap, categoryMap

    def _buildEnMaps (self, enCatalog, termMap, categoryMap):
        for srcRecord in enCatalog:
            product_type_en = srcRecord ['value']['attributes']['product_type']
            material_en = srcRecord ['value']['attributes']['material']
            condition_en = srcRecord ['value']['attributes']['condition']
            if (product_type_en != None) and (product_type_en != ''):
                if self._lookupTerm ('product_type', product_type_en, termMap) == None:
                    termMap.append ({'termtype': 'product_type',
                                    'en': product_type_en,
                                    'de': None})

            if (material_en != None) and (material_en != ''):
                if self._lookupTerm ('material', material_en, termMap) == None:
                    termMap.append ({'termtype': 'material',
                                     'en': material_en,
                                     'de': None})

            if (condition_en != None) and (condition_en != ''):
                if self._lookupTerm ('condition', condition_en, termMap) == None:
                    termMap.append ({'termtype': 'condition',
                                     'en': condition_en,
                                     'de': None})

            categoryPathList = srcRecord ['value']['attributes']['category_paths']
            for branch in categoryPathList:
                for leaf in branch:
                    catId = leaf ['id']
                    catName_en = leaf ['name']
                    if self._lookupCategoryMap (catId, categoryMap) == None:
                        categoryMap.append ({'catId': catId,
                                             'catName_en': catName_en,
                                             'catName_de': None })
        return

    def _updateGermanTermsInMaps (self, deCatalog, termMap, categoryMap, enCatalog):
        for deRecord in deCatalog:
            pid = deRecord  ['value']['attributes']['pid']

            # get corresponding EN record
            enRecord = self._lookupEnProduct (pid, enCatalog)
            if enRecord == None:
                continue    # happens during debugging

            product_type_en = enRecord ['value']['attributes']['product_type']
            material_en = enRecord ['value']['attributes']['material']
            condition_en = enRecord ['value']['attributes']['condition']

            # get corresponding map entries
            if (product_type_en != None) and (product_type_en != ''):
                product_type_map = self._lookupTerm ('product_type', product_type_en, termMap)
                if product_type_map ['de'] == None:
                    product_type_map ['de'] = deRecord ['value']['attributes']['product_type']

            if (material_en != None) and (material_en != ''):
                material_map = self._lookupTerm ('material', material_en, termMap)
                if material_map ['de'] == None:
                    material_map ['de'] = deRecord ['value']['attributes']['material']

            if (condition_en != None) and (condition_en != ''):
                condition_map = self._lookupTerm ('condition', condition_en, termMap)
                if condition_map ['de'] == None:
                    condition_map ['de'] = deRecord ['value']['attributes']['condition']

            categoryPathList = deRecord ['value']['attributes']['category_paths']
            for branch in categoryPathList:
                for leaf in branch:
                    catId = leaf ['id']
                    catMap = self._lookupCategoryMap (catId, categoryMap)
                    if catMap ['catName_de'] == None:
                        catMap ['catName_de'] = leaf ['name']

        return

    def _lookupTerm (self, termtype, termvalue_en, termMap):
        for termMap in termMap:
            if (termMap ['termtype'] == termtype) and (termMap ['en'] == termvalue_en):
                return termMap

        return None

    def _lookupCategoryMap (self, catId, categoryMap):
        for catIdMap in categoryMap:
            if (catIdMap ['catId'] == catId):
                return catIdMap

        return None

    def _lookupEnProduct (self, pid, enCatalog):
        for enProduct in enCatalog:
            if enProduct ['value']['attributes']['pid'] == pid:
                return enProduct

        return None

    def writeMaps (self, termMap, categoryMap, termsTargetPath, categoryTargetPath):
        with open (termsTargetPath, 'w') as file_output:
            tsvWriter = csv.writer (file_output, delimiter = '\t')

            headerLine = termMap[0].keys ()
            tsvWriter.writerow (headerLine)

            for row in termMap:
                tsvWriter.writerow (row.values())
            file_output.close ()

        with open (categoryTargetPath, 'w') as file_output:
            tsvWriter = csv.writer (file_output, delimiter = '\t')

            headerLine = categoryMap[0].keys ()
            tsvWriter.writerow (headerLine)

            for row in categoryMap:
                tsvWriter.writerow (row.values())
            file_output.close ()

        return

if __name__ == '__main__':
    logging.basicConfig (level=logging.DEBUG)

    mapBuilder = BuildTranslationMap ()
    mapBuilder.setEnglishCatalog (uc.FILENAME_JSONL_SOURCE_FEED_EN_IN)
    mapBuilder.setGermanCatalog (uc.FILENAME_UPDATED_JSONL_FEED_DE_OUT)
    termMap, categoryMap = mapBuilder.buildMaps ()
    mapBuilder.writeMaps (termMap, categoryMap, \
                          uc.FILENAME_UPDATED_TRANSLATION_TERM_MAP_DE_OUT, \
                          uc.FILENAME_UPDATED_TRANSLATION_CATEGORY_MAP_DE_OUT)

    logging.debug ("Finish...")
 

