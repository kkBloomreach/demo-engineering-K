## FEED --- FULL (and SMALL for debugging) -- expected to be already a subset
FILENAME_JSONL_SOURCE_FEED_IN = './data/input/ps_content_de_08302023.jsonl'
#FILENAME_JSONL_SOURCE_FEED_IN = './data/input/one.jsonl'
#FILENAME_JSONL_SOURCE_FEED_IN = './data/input/ps_content_de_08302023_5.jsonl'

FILENAME_UPDATED_JSONL_FEED_DE_OUT = './data/output/psg_content_de_08092024.jsonl'

# attributes in each content record as used in Discovery (besides "path", "op", ...}
CONTENT_ATTRIBUTES_TO_UPDATE = [
    'tags',
    'introduction',
    'title',
    'author',
    'url',  # same sourceurl, xm_urls [], 
    'image',
    'xm_aggregated_descendants_text'
]

SITE_CONTENT_DOCUMENTS_API_ENDPOINT = 'https://pacific-saas.bloomreach.io/delivery/site/v2/documents?folder=/versorgung/artikel&limit=60'

