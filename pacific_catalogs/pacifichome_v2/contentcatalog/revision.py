# current changes
# -- initial content catalog generation

import logging
import random
import os
import copy
import csv
import json

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstants as rc

class Revision (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update')
        super().__init__ ()
        return

    def _initialize (self, api_response, inject_av_map):
        self._read_imageurl_articleurl_map ()
        if (self._imageurl_articleurl_map == None) or (len (self._imageurl_articleurl_map) == 0):
            return False

        return True

    # override base class method
    # This update class does not do any update to previous records
    # record is from the CMS api response
    def _perform_record_update (self, cms_record):
        doc_id = cms_record ['id']
        discovery_record = copy.deepcopy (uc.DISCOVERY_DEFAULT_CATALOG_TEMPLATE)

        updated_discovery_record = self._perform_update_internal (cms_record, discovery_record)
        return updated_discovery_record

    # INTERNAL METHODS
    def _perform_update_internal (self, cms_record, discovery_record):
        # document-level labels "op", "path"
        discovery_record ["op"] = "add"
        discovery_record ["path"] = '%s/%s' % ('/items', cms_record ['id'])

        # document attributes
        for attrib in uc.CONTENT_ATTRIBUTES_TO_EXTRACT:
            cmsAttribValue = self._getCMSAttribValue (cms_record, attrib)
            if (cmsAttribValue != None):
                discovery_record ['value']['attributes'][attrib] = cmsAttribValue
                if attrib == 'url': # same value -> sourceurl, xm_urls[0]
                    discovery_record ['value']['attributes']['sourceurl'] = cmsAttribValue
                    discovery_record ['value']['attributes']['xm_urls'] = []
                    discovery_record ['value']['attributes']['xm_urls'].append (cmsAttribValue)
            else:
                logging.warning ('Cannot find CMS attribute value for: %s' % attrib)
                discovery_record ['value']['attributes'][attrib] = ''

            # availability
            discovery_record ['value']['attributes']['availability'] = True
        return discovery_record 

    def _getCMSAttribValue (self, cms_record, attrib):
        logging.debug ('getCMSAttrib value: %s' % attrib)
        if (attrib == 'tags'):
            if ('tags' in cms_record ['fields']) and (cms_record ['fields']['tags']):
                return cms_record ['fields']['tags']
            else:
                return None

        elif (attrib == 'introduction'):
            if ('introduction' in cms_record ['fields']) and (cms_record ['fields']['introduction']):
                return cms_record ['fields']['introduction']['value']
            else:
                return None

        elif (attrib == 'title'):
            if ('title' in cms_record ['fields']) and (cms_record ['fields']['title']):
                return cms_record ['fields']['title']
            else:
                return None

        elif (attrib == 'author'):
            if ('authors' in cms_record ['fields']) and (cms_record ['fields']['authors']):
                author0 = cms_record ['fields']['authors'][0]
                if ('fields' in author0) and (author0 ['fields']):
                    if 'fullName' in author0 ['fields']:
                        return author0 ['fields']['fullName']
            else:
                return None

        elif (attrib == 'url'):
            if ('path' in cms_record) and cms_record ['path']:
                path = cms_record ['path']
                indx = path.find ('/articles')
                return path [indx:]
            else:
                return None

        # pacifichome image urls received in API response refer to 'unsplach.com' whereas expected
        # urls are to be in pacific-saas.bloomreach.com. The imageurl<->articleurl map is used to
        # look up corresponding image url. The leaf-value in cms_record['path'] is used for this lookup
        # CHANGED. Apparently on CMS side, it is difficult/complicated to have locally-hosted-images
        # Therefore, no image mapping is needed in Discovery catalog either
        elif (attrib == 'image'):
            # image_url = self._lookup_image_url (cms_record ['path']) ---- NOT NEEDED
            image_url = self._extract_image_from_api_response (cms_record) 
            return image_url

        # site description in turn has multiple entry blocks
        elif (attrib == 'xm_aggregated_descendants_text'):
            aggregatedDescription = None

            if ('entries' in cms_record ['fields']) and (cms_record ['fields']['entries']):
                aggregatedDescription = "<br/>"
                for descriptionEntry in cms_record ['fields']['entries']:
                    if 'title' in descriptionEntry and (descriptionEntry ['title'] != ''):
                        aggregatedDescription = '%s<h2>%s</h2><br/>' % (aggregatedDescription, descriptionEntry ['title'])
                    if 'description' in descriptionEntry and (descriptionEntry ['description'] != ''):
                        aggregatedDescription = '%s<p>%s</p><br/>' % (aggregatedDescription, descriptionEntry ['description']['value'])
                aggregatedDescription = '%s<br/>' % (aggregatedDescription)
            return aggregatedDescription

        # document date
        elif (attrib == 'date'):
            if ('date' in cms_record ['fields']) and (cms_record ['fields']['date']):
                return cms_record ['fields']['date']

        else:
            logging.warning ('Unknown attribute for translation: %s', attrib)
        return None

    # read the old map of article-url -> image-url
    def _read_imageurl_articleurl_map (self):
        self._imageurl_articleurl_map = []
        with open (rc.FILENAME_IMAGEURL_ARTICLEURL_MAP_TSV_IN, 'r') as source_map_file:
            dict_reader = csv.DictReader (source_map_file, delimiter = '\t')
            for row in dict_reader:
                imageurl = row ['imageurl']
                articleurl = row ['articleurl']
                r_indx = articleurl.rindex ('/')
                article_url_leaf = articleurl [r_indx + 1:]
                self._imageurl_articleurl_map.append ({ 'article_url_leaf': article_url_leaf,
                                                        'image_url': imageurl
                                                      })
            source_map_file.close ()
        return

    def _lookup_image_url (self, cms_record_path):
        r_indx = cms_record_path .rindex ('/')
        cms_record_path_leaf = cms_record_path [r_indx+1:]

        for imageurl_articleurl_record in self._imageurl_articleurl_map:
            if imageurl_articleurl_record ['article_url_leaf'] == cms_record_path_leaf:
                return imageurl_articleurl_record ['image_url']
        return None

    # in case the map does not have appropriate record, extract image from the
    # api response itself
    def _extract_image_from_api_response (self, cms_record):
        if ('images' in cms_record ['fields']) and (cms_record ['fields']['images']):
            image0 = cms_record ['fields']['images']['image'][0]
            unsplashImage = json.loads (image0 ['unsplashImage'])
            if ('urls' in unsplashImage) and (unsplashImage ['urls']):
                if ('full' in unsplashImage ['urls']):
                    return unsplashImage ['urls']['full']
        return None


if __name__ == '__main__':
    rv = Revision ()


