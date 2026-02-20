# module_name and class_name. Currently, both are same names except the first-letter-case
REVISION_UPDATER_MODULE = 'revisionV35'
REVISION_CLASS_NAME = 'RevisionV35'

## FEED --- FULL (and SMALL for debugging) -- expected to be already available
FILENAME_JSONL_SOURCE_FEED_IN = './data/input/pa_en_full_01152026.jsonl'
#FILENAME_JSONL_SOURCE_FEED_IN = './data/input/pa_en_full_01152026_10.jsonl'

FILENAME_UPDATED_JSONL_FEED_OUT = './data/output/pa_en_full_01162026.jsonl'
FILENAME_UPDATED_DATAHUB_JSONL_FEED_OUT = './data/output/pa_en_full_datahub_01162026.jsonl'
FILENAME_UPDATED_TSV_FEED_OUT = './data/output/pa_en_full_01162026.tsv'
FILENAME_UPDATED_FEED_ATTRIBUTELIST_OUT = './data/output/pa_en_full_01162026_attributes.txt'
FILENAME_INJECTED_AVMAP_OUT = './data/output/injected_avmap_01162026.tsv'

# Injected av-map path (initially generated when different catalogs merged. later edited manually)
FILENAME_INJECTED_AV_MAP_IN = './data/input/injected_avmap_01162026_edited.tsv'
INJECTED_AVMAP_ATTRIBUTES = [
    'pid',
    'url',
    'availability',
    'price',
    'gender',
    'color',
    'brand',
    'material',
    'collection',
    'style',
    'title',
    'description',
    'stock_level',  # added in V5
    'special_offer',
    'season'   
]

# category tree map
FILENAME_CATEGORY_MAP_IN = './data/input/categorymap_01152025.tsv'

# URL preambles
PRODUCT_URL_PREFIX = 'https://pacific.bloomreach.com/apparel/products/'
THUMB_IMAGE_URL_PROLOG = 'https://pacific-demo-data.bloomreach.cloud/apparel/'

# all prices must fall in this range
MIN_ALLOWED_PRICE = 19.12
MAX_ALLOWED_PRICE = 9872.59

# used for generating images
# image files are downloaded in LOCAL_DIR/data/images
OPENAI_KEY = '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'
OPENAI_MODEL = 'gpt-4o' # for description generation
OPENAI_MODEL_DALL_E = 'dall-e-3' # for image generation

THUMB_IMAGE_LOCAL_DIR = './data/images'
THUMB_IMAGE_URL_PROLOG = 'https://pacific-demo-data.bloomreach.cloud/apparel'

HTTP_STATUS_OK = 200
IMAGE_LOADER_STATUS_FAIL = -1
IMAGE_LOADER_STATUS_SUCCESS = 1

