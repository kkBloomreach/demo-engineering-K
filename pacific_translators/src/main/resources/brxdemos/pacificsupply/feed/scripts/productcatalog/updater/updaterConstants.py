## FEED --- FULL (and SMALL for debugging) -- expected to be already converted
FILENAME_JSONL_SOURCE_FEED_IN = './data/input/full_preprocessed_07072021_05092023.jsonl'
#FILENAME_JSONL_SOURCE_FEED_IN = './data/input/one.jsonl'

# NOTE: PacificSupply images not generated via openAI
# Imagemap generated via genimage script - 
# FILENAME_GENIMAGE_MAP = './data/genimage_map_09292023.tsv'

FILENAME_UPDATED_JSONL_FEED_OUT = './data/output/ps_product_en_full_11012023.jsonl'
FILENAME_UPDATED_TSV_FEED_OUT = './data/output/ps_product_en_full_11012023.tsv'
FILENAME_UPDATED_CSV_FEED_OUT = './data/output/ps_product_en_full_11012023.csv'
FILENAME_UPDATED_FEED_ATTRIBUTELIST_OUT = './data/output/ps_product_en_full_11012023_attributes.txt'

# subset
FILENAME_SUBSET_CATID_LIST_IN ='./data/input/subsetcatidlist.tsv'
FILENAME_SUBSET_JSONL_FEED_OUT = './data/output/ps_product_en_subset_11012023.jsonl'
FILENAME_SUBSET_FEED_ATTRIBUTELIST_OUT = './data/output/ps_product_en_subset_11012023_attributes.txt'

PRODUCT_URL_PREFIX = 'https://pacific.bloomreach.com/supply/products/'
SUBSET_MAX_PRODUCTS_TO_USE_IN_CATEGORY = 20 

