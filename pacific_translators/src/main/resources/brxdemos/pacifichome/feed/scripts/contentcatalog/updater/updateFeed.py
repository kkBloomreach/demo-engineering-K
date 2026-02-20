import logging
import csv
from random import random 
import os

import updaterConstants as uc

class UpdateFeed ():
    def __init__ (self):
        self._currentDiscoveryCatalog = []
        self._siteApiResponses = []
        self._updatedCatalog = [] 

    def setDiscoveryCatalog (self, discoveryCatalog):
        self._currentDiscoveryCatalog = discoveryCatalog 

    def setSiteApiResponse (self, siteApiResponses):
        self._siteApiResponses = siteApiResponses

    def performUpdates (self):
        logging.info ("update current Discovery content feed")

        for discoveryContentRecord in self._currentDiscoveryCatalog:
            # the last-element in apiResponse->path and discoveryCatalog->url
            # are used to lookup
            # example:
            # discovery url = "/site/pacifichome/articles/featured/bedroom-decorating-ideas"
            # apiResponse path = path: "/home/articles/featured/bedroom-decorating-ideas"
            articleUrl = discoveryContentRecord ['value']['attributes']['url']
            siteContentEntry = self._lookupSiteContentEntry (articleUrl)

            if (siteContentEntry == None):
                logging.warn ('Site does not have content with title: %s', articleUrl)
                continue
            else:
                updatedDiscoveryRecord = self._updateOneDiscoveryRecord (discoveryContentRecord, siteContentEntry)
                self._updatedCatalog.append (updatedDiscoveryRecord)

        # sort updatedCatalog so that subset generation is deterministic in subsequent runs
        # sort key is the 'url' value because its leaf value (eg, 'home-decor-trends-2020')
        # is also the leaf in CMS's path and therefore 'deterministic'
        self._updatedCatalog.sort (key = lambda record: record ['value']['attributes']['url'])

        return self._updatedCatalog

    def _updateOneDiscoveryRecord (self, srcDiscoveryRecord, siteContentEntry):
        updatedDiscoveryRecord = srcDiscoveryRecord.copy ()

        siteContentTitle = None
        siteContentTags = None
        siteContentIntroduction = None
        aggregatedDescription  = None

        if ('title' in siteContentEntry ['fields']) and (siteContentEntry ['fields']['title']):
            siteContentTitle = siteContentEntry ['fields']['title']

        if ('tags' in siteContentEntry ['fields']) and (siteContentEntry ['fields']['tags']):
            siteContentTags = siteContentEntry ['fields']['tags']

        if ('introduction' in siteContentEntry ['fields']) and (siteContentEntry ['fields']['introduction']):
            siteContentIntroduction = siteContentEntry ['fields']['introduction']['value']

        # site description in turn has multiple entry blocks
        if ('entries' in siteContentEntry ['fields']) and (siteContentEntry ['fields']['entries']):
            descriptionEntries = siteContentEntry ['fields']['entries']
            aggregatedDescription = "<br/>"
            for descriptionEntry in siteContentEntry ['fields']['entries']:
                if 'title' in descriptionEntry:
                    if (descriptionEntry ['title'] != ''):
                        aggregatedDescription = '%s<h2>%s</h2><br/>' % (aggregatedDescription, descriptionEntry ['title'])
                if 'description' in descriptionEntry:
                    if (descriptionEntry ['description'] != ''):
                        aggregatedDescription = '%s<p>%s</p><br/>' % (aggregatedDescription, descriptionEntry ['description']['value'])
                aggregatedDescription = '%s<br/>' % (aggregatedDescription)

            # update discoveryRecord
            updatedDiscoveryRecord ['value']['attributes']['title'] = siteContentTitle
            updatedDiscoveryRecord ['value']['attributes']['tags'] = siteContentTags
            updatedDiscoveryRecord ['value']['attributes']['introduction'] = siteContentIntroduction
            updatedDiscoveryRecord ['value']['attributes']['xm_aggregated_descendants_text'] = aggregatedDescription

        return updatedDiscoveryRecord


        # sort the updated records by pid. This is mainly to ensure the 'subset'
        # creation will generate a 'subset' in a deterministic way (ie, same subset generated
        # in each new run)
        self.updated_products.sort (key=lambda record: record ['value']['attributes']['pid'])

        logging.info ("product + variants in updated feed count: %s", len (self.updated_products))
        return

    # use url and path 'leaves' to match articles in discovery with those on site
    def _lookupSiteContentEntry (self, articleUrl):
        rIndx = articleUrl.rindex ('/')
        urlLeaf = articleUrl [rIndx+1:]

        for entry in self._siteApiResponses ['documents']:
            path = entry ['path']
            rIndx = path.rindex ('/')
            pathLeaf = path [rIndx+1:]
            if (urlLeaf == pathLeaf):
                return entry
        return None


if __name__ == '__main__':
    uf = UpdateFeed ()
    uf.performUpdates ()

