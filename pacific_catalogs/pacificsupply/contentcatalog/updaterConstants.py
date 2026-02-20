# module_name and class_name. Currently, both are same names except the first-letter-case
REVISION_UPDATER_MODULE = 'revisionV0'
REVISION_CLASS_NAME = 'RevisionV0'

SITE_CONTENT_DOCUMENTS_API_ENDPOINT = 'https://pacific-saas.bloomreach.io/delivery/site/v2/documents?folder=/supply/articles&limit=60'

FILENAME_UPDATED_JSONL_FEED_OUT = './data/output/ps_content_en_full_03262025.jsonl'
FILENAME_UPDATED_TSV_FEED_OUT = './data/output/ps_content_en_full_03262025.tsv'
FILENAME_UPDATED_FEED_ATTRIBUTELIST_OUT = './data/output/ps_content_en_full_03262025_attributes.txt'
FILENAME_INJECTED_AVMAP_OUT = './data/output/injected_avmap_03262025.tsv'

# Injected av-map path (initially generated when different catalogs merged. later edited manually)
FILENAME_INJECTED_AV_MAP_IN = './data/NONE'
INJECTED_AVMAP_ATTRIBUTES = [
    'author',
    'availability'
]

# attributes in each content record as used in Discovery (besides "path", "op", ...}
CONTENT_ATTRIBUTES_TO_EXTRACT = [
    'author',
    'date',
    'image',
    'introduction',
    'tags',
    'title',
    'url',  # same sourceurl, xm_urls [],
    'xm_aggregated_descendants_text'
]

# Discovery catalog template
DISCOVERY_DEFAULT_CATALOG_TEMPLATE = \
{
  "op": "add",
  "path": "",
  "value": {
    "attributes": {
      "title": "",
      "date": "",
      "tags": [
        ""
      ],
      "introduction": "",
      "image": "",
      "sourceurl": "",
      "xm_primaryDocType": "starterstoreboot:article",
      "xm_type": [
        "CONTENTPAGE"
      ],
      "xm_aggregated_descendants_text": "",
      "xm_channelIds": [
        "pacificapparel"
      ],
      "url": "",
      "xm_urls": [
        "",
      ],
      "author": "",
      "availability": True
    }
  }
}

# NOTE: for pdf's we don't use API response because it does not contain information such as title...
# SITE_CONTENT_PDFS_API_ENDPOINT = 'https://pacific-saas.bloomreach.io/delivery/site/v2/assets?folder=/pacific-supply/pdfs&limit=10'


