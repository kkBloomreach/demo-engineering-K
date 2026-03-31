# module_name and class_name. Currently, both are same names except the first-letter-case
REVISION_UPDATER_MODULE = 'revisionV0'
REVISION_CLASS_NAME = 'RevisionV0'

## FEED --- FULL (and SMALL for debugging) -- expected to be already available
FILENAME_PS_ENGLISH_JSONL_SOURCE_FEED_IN = './data/input/psg_product_en_subset_03312026.jsonl'
#FILENAME_PS_ENGLISH_JSONL_SOURCE_FEED_IN = './data/input/psg_product_en_subset_03312026_10.jsonl'

# NAB - nuts-and-bolt
FILENAME_SAMPLE_NAB_JSONL_FEED_OUT = './data/output/sample_nab_03312026.jsonl'
FILENAME_SAMPLE_NAB_DATAHUB_JSONL_FEED_OUT = './data/output/sample_nab_datahub_03312026.jsonl'
FILENAME_SAMPLE_NAB_TSV_FEED_OUT = './data/output/sample_nab_03312026.tsv'
FILENAME_SAMPLE_NAB_FEED_ATTRIBUTELIST_OUT = './data/output/sample_nab_03312026.txt'
FILENAME_SAMPLE_NAB_INJECTED_AVMAP_OUT = './data/output/sample_nab_injected_avmap_03312026.tsv'

# Injected av-map path (initially generated when different catalogs merged. later edited manually)
FILENAME_INJECTED_AV_MAP_IN = './data/NONE'
INJECTED_AVMAP_INCLUDED_ATTRIBUTES = [
    'pid',
    'url',
    'availability',
    'brand',
    'price',
    'sale_price',
    'title',
    'description'
]

# URL preambles 
# This URL by itself is never used. The nuts-and-bolt catalog is used to
# populate new accounts by acct-creation-team. The url is/should-be adjusted by that 
# team to appropriate value
PRODUCT_URL_PREFIX = 'https://pacific.bloomreach.com/samplenab/products/'



