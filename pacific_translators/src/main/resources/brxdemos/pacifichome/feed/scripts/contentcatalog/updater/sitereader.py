from bs4 import BeautifulSoup
import jsonlines
import json
import logging
import openai
import subprocess

OPENAI_KEY = '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'
DISCOVERY_CONTENT_CATALOG_PATH_CURRENT = './content_en_full_12232022.jsonl'
DISCOVERY_CONTENT_CATALOG_PATH_UPDATED = './content_en_full_10122023.jsonl'

FILENAME_APIRESPONSE = 'apiresp.json'
SITE_CONTENT_DOCUMENTS_API_URL = 'https://pacific-saas.bloomreach.io/delivery/site/v2/documents?folder=/home/articles&limit=55'

def readCurrentContentCatalog (jsonPath):
    catalog = []
    with open (jsonPath, 'r') as f:
        reader = jsonlines.Reader (f)
        for aContent in reader:
            catalog.append (aContent)

    return catalog

def readSiteContent ():
    documentsApiResp = None

    callstat = subprocess.call ([ "curl", "-o", FILENAME_APIRESPONSE, SITE_CONTENT_DOCUMENTS_API_URL])
    with open (FILENAME_APIRESPONSE, 'r') as f:
        documentsApiResp = json.loads (f.read ())
        f.close ()

    if documentsApiResp == None:
        logging.error ('cannot get contents from site')

    return documentsApiResp


def translate_text (text, source_language, target_language):
    return ('Translation of ' + text)

def translate_text_OPENAI (text, source_language, target_language):
    openai.api_key = OPENAI_KEY
    prompt = 'Translate the following %s text to %s: %s' % (source_language, target_language, text)
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

# parse the html and translate embedded text
def translateHtml (htmlText):
    try:
        soup = BeautifulSoup (htmlText, 'html.parser')
    except Exception as e:
        print ('BS exception: %s', str (e))
        return

    for htmlTag in soup.descendants:
        if htmlTag.name:
            print ('tag = ' + htmlTag.name)
            if (htmlTag.string):
                print ('tag value: ' + htmlTag.string)
                translated = translate_text (htmlTag.string, "English", "German")
                print ('translated: ' + translated)
                htmlTag.string.replace_with (translated)
                print ('translated htmlTag string: ' + htmlTag.string)
        #else:
        #    print ('no htmlTag name')

    return htmlText # translated

# currently update only title, tags, introduction, description
# In discoveryCatalog, 'description' is saved as 'xm_aggregated_descendants_text'
def updateOneDiscoveryContent (oneDiscoveryEntry, oneSiteContentEntry):

    siteContentTitle = None
    siteContentTags = None
    siteContentIntroduction = None
    aggregatedDescription  = None 

    if ('title' in oneSiteContentEntry ['fields']) and (oneSiteContentEntry ['fields']['title']):
        siteContentTitle = oneSiteContentEntry ['fields']['title']

    if ('tags' in oneSiteContentEntry ['fields']) and (oneSiteContentEntry ['fields']['tags']):
        siteContentTags = oneSiteContentEntry ['fields']['tags']

    if ('introduction' in oneSiteContentEntry ['fields']) and (oneSiteContentEntry ['fields']['introduction']):
        siteContentIntroduction = oneSiteContentEntry ['fields']['introduction']['value']

    # site description in turn has multiple entry blocks
    if ('entries' in oneSiteContentEntry ['fields']) and (oneSiteContentEntry ['fields']['entries']):
        descriptionEntries = oneSiteContentEntry ['fields']['entries']
        aggregatedDescription = "<br/>"
        for descriptionEntry in oneSiteContentEntry ['fields']['entries']:
            if 'title' in descriptionEntry:
                if (descriptionEntry ['title'] != ''):
                    aggregatedDescription = '%s<h2>%s</h2><br/>' % (aggregatedDescription, descriptionEntry ['title'])
            if 'description' in descriptionEntry:
                if (descriptionEntry ['description'] != ''):
                    aggregatedDescription = '%s<p>%s</p><br/>' % (aggregatedDescription, descriptionEntry ['description']['value'])
        aggregatedDescription = '%s<br/>' % (aggregatedDescription)

    # update discoveryEntry
    oneDiscoveryEntry ['value']['attributes']['title'] = siteContentTitle
    oneDiscoveryEntry ['value']['attributes']['tags'] = siteContentTags
    oneDiscoveryEntry ['value']['attributes']['introduction'] = siteContentIntroduction
    oneDiscoveryEntry ['value']['attributes']['xm_aggregated_descendants_text'] = aggregatedDescription

    return
 
def lookupSiteContentEntry (articleTitle, documentApiResponse):
    for entry in documentApiResponse ['entries']:
        if (entry ['name'] == articleTitle):
            return entry
    return None

def updateDiscoveryCatalog (currentDiscoveryCatalog, documentApiResponse):
    # dup current for updated
    updatedDiscoveryCatalog = currentDiscoveryCatalog.copy ()

    for discoveryEntry in updatedDiscoveryCatalog:
        articleTitle = discoveryEntry ['value']['attributes']['title']
        siteContentEntry = lookupSiteContentEntry (articleTitle, documentApiResponse)
        if (siteContentEntry == None):
            logging.warn ('No such entry in current site: %s', articleTitle)
        else:
            logging.debug ('Update discoveryContent, title = %s', articleTitle)
            updateOneDiscoveryContent (discoveryEntry, siteContentEntry)

    return updatedDiscoveryCatalog

def writeDiscoveryCatalog (updatedDiscoveryCatalog, targetPath):
    with open (targetPath, 'w') as outputFile:
        writer = jsonlines.Writer (outputFile)
        for aContent in updatedDiscoveryCatalog:
            writer.write (aContent)
        outputFile.close ()

    return

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)

    # read current content catalog .json
    currentDiscoveryCatalog = readCurrentContentCatalog (DISCOVERY_CONTENT_CATALOG_PATH_CURRENT)

    # read documents from site using documentApi call
    documentsApiResponse = readSiteContent ()

    # process documentApiResp to construct new 
    updatedDiscoveryCatalog = updateDiscoveryCatalog (currentDiscoveryCatalog, documentsApiResponse)

    # save updated DiscoveryContent catalog
    writeDiscoveryCatalog (updatedDiscoveryCatalog, DISCOVERY_CONTENT_CATALOG_PATH_UPDATED)

    logging.info ('Finish...')

