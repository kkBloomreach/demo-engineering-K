import logging
import openai
import time

import jsonlFeedReader as jfr
import updaterConstants as uc

MAX_ATTEMPTS_TO_TRANSLATE = 5
MTB_REATTEMPT = 5   # if openAI error, sleep N seconds then re-attempt

class UpdateFeed ():
    def __init__ (self):
        self.src_products_en = []
        self.updated_products_de = []
        self.updated_attributeList_de = []

    def setSourceProducts (self, srcProducts_en):
        self.src_products_en = srcProducts_en

    def performUpdates (self):
        self._updateFeed ()
        return self.updated_products_de, self.updated_attributeList_de

    def _updateFeed (self):
        logging.info ("update feed")

        for srcRecord in self.src_products_en:
            updatedRecord = srcRecord.copy ()

            pid = srcRecord ['value']['attributes']['pid']
            logging.debug ('process pid = %s', pid)
            productAttribs = srcRecord ['value']['attributes']
            for attrib, value in productAttribs.items ():
                logging.debug ('product attrib = %s', attrib)
                if attrib == 'category_paths':
                    self._translateCategoryPath (pid, value)
                elif attrib in uc.PRODUCT_ATTRIBUTES_TO_TRANSLATE:
                    if value != None:
                        translatedValue = self._translateText (pid, value)
                        updatedRecord ['value']['attributes'][attrib] = translatedValue

            if 'variants' in srcRecord ['value'] and srcRecord ['value']['variants']:
                for variantId, variantObj in srcRecord ['value']['variants'].items():
                    for attrib, value in variantObj ['attributes'].items ():
                        logging.debug ('variant attrib = %s', attrib)
                        if attrib in uc.VARIANT_ATTRIBUTES_TO_TRANSLATE:
                            if value != None:
                                translatedValue = self._translateText (pid, value)
                                variantObj ['attributes'] [attrib] = translatedValue

            # collect all updated products
            self.updated_products_de.append (updatedRecord)

            # accumulate attribute names across all products and their variants
            self._collectAttributeList (updatedRecord)

        logging.info ("product + variants in updated feed count: %s", len (self.updated_products_de))
        return

    # param 'value' is list-of-lists
    def _translateCategoryPath (self, pid, categoryPathList):
        for branch in categoryPathList:
            for leaf in branch:
                translatedLeafName = self._translateText (pid, leaf ['name'])
                leaf ['name'] = translatedLeafName

        return

    def _translateText_DEV (self, pid, value):
        return 'TR_' + str (value)

    def _translateText (self, pid, text):
        attempt = 0

        source_language = uc.SOURCE_CATALOG_LANGUAGE
        target_language = uc.TARGET_CATALOG_LANGUAGE

        openai.api_key = uc.OPENAI_KEY
        prompt = 'Translate the following %s text to %s: %s' % (source_language, target_language, text)
        logging.debug (prompt)

        while (attempt < MAX_ATTEMPTS_TO_TRANSLATE):
            try:
                response = openai.ChatCompletion.create (
                    model = 'gpt-3.5-turbo',
                    messages = [
                                 {'role': 'system', 'content': 'Your a helpful assistant' },
                                 { 'role': 'user', 'content': prompt }
                               ],
                    max_tokens = 150,
                    n = 1,
                    stop = None,
                    temperature = 0.5
                )
                translation = response.choices [0].message.content.strip ()
                return translation
            except Exception as e:
                logging.warn ('Translation openAI error. pid = %s, attempt = %s, error = %s' % (pid, attempt, e))
                attempt = attempt + 1
                time.sleep (MTB_REATTEMPT)
                continue    # try again

        # return text = TIMEOUT_ + text
        retText = '%s_%s' % ('TIMEOUT', text)
        return retText

    def _collectAttributeList (self, currentRecord):
        prodAttribs = currentRecord ['value']['attributes']
        for attrib in prodAttribs.keys():
            if attrib not in self.updated_attributeList_de:
                self.updated_attributeList_de.append (attrib)
        if 'variants' in currentRecord ['value'] and currentRecord ['value']['variants']:
            variantList = currentRecord ['value']['variants']
            for variantId, variantObj in variantList.items (): 
                for key in variantObj ['attributes'].keys ():
                    if key not in self.updated_attributeList_de:
                        self.updated_attributeList_de.append (key)

        # attribute list sorted just before writing it to attribList
        return

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    srcFeedReader = jfr.JsonlFeedReader ()
    srcProducts = srcFeedReader.readSourceFeed (uc.FILENAME_JSONL_SOURCE_FEED_EN_IN)

    feedUpdater = UpdateFeed ()
    feedUpdater.setSourceProducts (srcProducts) # english
    updatedProducts_de, updatedAttributeList_de = feedUpdater.performUpdates ()
    logging.debug ('Finish ...')


