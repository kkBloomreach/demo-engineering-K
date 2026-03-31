# module_name and class_name. Currently, both are same names except the first-letter-case
REVISION_UPDATER_MODULE = 'revisionV0'
REVISION_CLASS_NAME = 'RevisionV0'

## FEED --- FULL (and SMALL for debugging) -- expected to be already available
FILENAME_PS_ENGLISH_JSONL_SOURCE_FEED_IN = './data/input/ps_product_en_full_02122025.jsonl'
#FILENAME_PS_ENGLISH_JSONL_SOURCE_FEED_IN = './data/input/ps_product_en_full_02122025_10.jsonl'

FILENAME_PSG_ENGLISH_SUBSET_JSONL_FEED_OUT = './data/output/psg_product_en_subset_03312026.jsonl'
FILENAME_PSG_ENGLISH_SUBSET_DATAHUB_JSONL_FEED_OUT = './data/output/psg_product_en_subset_datahub_03312026.jsonl'
FILENAME_PSG_ENGLISH_SUBSET_TSV_FEED_OUT = './data/output/psg_product_en_subset_03312026.tsv'
FILENAME_PSG_ENGLISH_SUBSET_FEED_ATTRIBUTELIST_OUT = './data/output/psg_product_en_subset_03312026.txt'
FILENAME_PSG_ENGLISH_SUBSET_INJECTED_AVMAP_OUT = './data/output/psg_injected_avmap_03312026.tsv'

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
PRODUCT_URL_PREFIX = 'https://pacific.bloomreach.com/supplyglobal/products/'



