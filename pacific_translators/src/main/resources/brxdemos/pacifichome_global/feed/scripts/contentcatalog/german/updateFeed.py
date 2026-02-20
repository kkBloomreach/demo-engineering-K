import logging
import time
import copy
import json

import updaterConstants as uc

class UpdateFeed ():
    def __init__ (self):
        self._currentDiscoveryCatalog = []
        self._siteApiResponses = []
        self.updated_contents_de = []

    def setDiscoveryCatalog (self, discoveryCatalog):
        self._currentDiscoveryCatalog = discoveryCatalog

    def setSiteApiResponse (self, siteApiResponses):
        self._siteApiResponses = siteApiResponses

    def performUpdates (self):
        self._updateFeed ()
        return self.updated_contents_de

    def _updateFeed (self):
        logging.info ("update feed")

        for srcRecord in self._currentDiscoveryCatalog:
            updatedRecord = copy.deepcopy (srcRecord)

            cmsDoc = self._lookupCMSDoc (srcRecord)
            if (cmsDoc == None):
                logging.info ('Cannot find CMS record %s, assuming it is pdf', srcRecord ['value']['attributes']['title'])
                self.updated_contents_de.append (updatedRecord)
                continue

            for attrib in uc.CONTENT_ATTRIBUTES_TO_UPDATE:
                cmsAttribValue = self._getCMSAttribValue (cmsDoc, attrib)
                if (cmsAttribValue != None):
                    updatedRecord ['value']['attributes'][attrib] = cmsAttribValue
                    if attrib == 'url': # same value -> sourceurl, xm_urls[0]
                        updatedRecord ['value']['attributes']['sourceurl'] = cmsAttribValue
                        updatedRecord ['value']['attributes']['xm_urls'] = []
                        updatedRecord ['value']['attributes']['xm_urls'].append (cmsAttribValue)
                else:
                    logging.warning ('Cannot find CMS attribute value for: %s' % attrib)
                    updatedRecord ['value']['attributes'][attrib] = ''

            # availability
            updatedRecord ['value']['attributes']['availability'] = True

            # collect all updated contents
            self.updated_contents_de.append (updatedRecord)

        logging.info ("content + variants in updated feed count: %s", len (self.updated_contents_de))
        return

    def _getCMSAttribValue (self, cmsDocument, attrib):
        logging.debug ('getCMSAttrib value: %s' % attrib)
        if (attrib == 'tags'):
            if ('tags' in cmsDocument ['fields']) and (cmsDocument ['fields']['tags']):
                return cmsDocument ['fields']['tags']
            else:
                return None

        elif (attrib == 'introduction'):
            if ('introduction' in cmsDocument ['fields']) and (cmsDocument ['fields']['introduction']):
                return cmsDocument ['fields']['introduction']['value']
            else:
                return None

        elif (attrib == 'title'):
            if ('title' in cmsDocument ['fields']) and (cmsDocument ['fields']['title']):
                return cmsDocument ['fields']['title']
            else:
                return None

        elif (attrib == 'author'):
            if ('authors' in cmsDocument ['fields']) and (cmsDocument ['fields']['authors']):
                author0 = cmsDocument ['fields']['authors'][0]
                if ('fields' in author0) and (author0 ['fields']):
                    if 'fullName' in author0 ['fields']:
                        return author0 ['fields']['fullName']
            else:
                return None

        elif (attrib == 'url'):
            if ('path' in cmsDocument) and cmsDocument ['path']:
                path = cmsDocument ['path']
                indx = path.find ('/articles')
                return path [indx:]
            else:
                return None

        # pacifichome_global images are embedded in unsplashImage (which is a json string)
        # actual value then is json.loads() -> x, x ['urls']['full']
        elif (attrib == 'image'):
            if ('images' in cmsDocument ['fields']) and (cmsDocument ['fields']['images']):
                image0 = cmsDocument ['fields']['images']['image'][0]
                unsplashImage = json.loads (image0 ['unsplashImage'])
                if ('urls' in unsplashImage) and (unsplashImage ['urls']):
                    if ('full' in unsplashImage ['urls']):
                        return unsplashImage ['urls']['full']
            return None

        # site description in turn has multiple entry blocks
        elif (attrib == 'xm_aggregated_descendants_text'):
            aggregatedDescription = None

            if ('entries' in cmsDocument ['fields']) and (cmsDocument ['fields']['entries']):
                aggregatedDescription = "<br/>"
                for descriptionEntry in cmsDocument ['fields']['entries']:
                    if 'title' in descriptionEntry and (descriptionEntry ['title'] != ''):
                        aggregatedDescription = '%s<h2>%s</h2><br/>' % (aggregatedDescription, descriptionEntry ['title'])
                    if 'description' in descriptionEntry and (descriptionEntry ['description'] != ''):
                        aggregatedDescription = '%s<p>%s</p><br/>' % (aggregatedDescription, descriptionEntry ['description']['value'])
                aggregatedDescription = '%s<br/>' % (aggregatedDescription)
            return aggregatedDescription

        else:
            logging.warning ('Unknown attribute for translation: %s', attrib)
        return None

    # 'title' is used to find matching cmsDoc
    # srcTitle = srcRecord ['value']['attributes']['title']
    # srcUrl = srcRecord ['value']['attributes']['url']
    # logging.debug ('processing record with title = %s', srcTitle)

    def _lookupCMSDoc  (self, srcRecord):
        srcUrl = srcRecord ['value']['attributes']['url']
        indx = srcUrl.rindex ('/')
        srcUrlTail = srcUrl [indx:]
        for cmsDoc in self._siteApiResponses ['documents']: 
            cmsPath = cmsDoc ['path']
            indx = cmsPath.rindex ('/')
            cmsPathTail = cmsPath [indx:]
            logging.debug ('path in cmsDoc: %s' % cmsPath)
            if (cmsPathTail.lower () == srcUrlTail.lower()):
                return cmsDoc
        return None

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)

    feedUpdater = UpdateFeed ()
    feedUpdater.setSourceContents (None) # english
    updatedContents_de = feedUpdater.performUpdates ()
    logging.debug ('Finish ...')

