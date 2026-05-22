# current changes
# -- initial content catalog generation
#    -- adjust specific attribute values to match the ones already in Experience (aka Content) site (eg, author, data, url, ...)
# -- inject PDF content files into earlier content catalog

import logging
import random
import os
import copy
import csv
import json

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstants as rc
import jsonlFeedReader as jfr

class Revision (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update')
        super().__init__ ()
        self._pdf_source_records = None # content records for .pdf files
        return

    def _initialize (self, documents_api_response, inject_av_map):
        if os.path.exists (rc.FILE_PDF_CONTENTS_JSONL_IN):
            pdf_content_reader = jfr.JsonlFeedReader ()
            self._pdf_source_records = pdf_content_reader.readSourceFeed (rc.FILE_PDF_CONTENTS_JSONL_IN);
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

        # adjust specific document attributes (eg, author, date, ...)
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

            # availability -- always set to True
            discovery_record ['value']['attributes']['availability'] = True
        return discovery_record 

    # override additions method to include pdf records
    def _perform_additions (self, current_records):
        if (self._pdf_source_records != None) and (len (self._pdf_source_records) > 0):
            for record in self._pdf_source_records:
                dup_record = copy.deepcopy (record)
                # for some reason, 'availability' was not included in each record !!!
                dup_record ['value']['attributes']['availability'] = True
                current_records.append (dup_record)
        return current_records # default

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

        # pacifichome_global images are embedded in unsplashImage (which is a json string)
        # actual value then is json.loads() -> x, x ['urls']['full']
        elif (attrib == 'image'):
            if ('images' in cms_record ['fields']) and (cms_record ['fields']['images']):
                image0 = cms_record ['fields']['images']['image'][0]
                if 'links' in image0:
                    if 'resource' in image0 ['links']:
                        if 'href' in image0 ['links']['resource']:
                            return image0 ['links']['resource']['href']
            return None

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

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = Revision ()


