## FEED --- FULL (and SMALL for debugging) -- expected to be already converted
FILENAME_JSONL_SOURCE_FEED_IN = './data/input/full_feed_converted_09212023.jsonl'
#FILENAME_JSONL_SOURCE_FEED_IN = './data/input/one.jsonl'
#FILENAME_JSONL_SOURCE_FEED_IN = './data/input/full_feed_converted_09212023_10.jsonl'

# Imagemap generated via genimage script
FILENAME_GENIMAGE_MAP = './data/input/genimage_map_09292023.tsv'

FILENAME_UPDATED_JSONL_FEED_OUT = './data/output/ph_product_en_full_10232023.jsonl'
FILENAME_UPDATED_TSV_FEED_OUT = './data/output/ph_product_en_full_10232023.tsv'
FILENAME_UPDATED_CSV_FEED_OUT = './data/output/ph_product_en_full_10232023.csv'
FILENAME_UPDATED_FEED_ATTRIBUTELIST_OUT = './data/output/ph_product_en_full_10232023_attributes.txt'

# subset
FILENAME_SUBSET_CATID_LIST_IN ='./data/input/subsetcatidlist.tsv'
FILENAME_SUBSET_JSONL_FEED_OUT = './data/output/ph_product_en_subset_12202023.jsonl'
FILENAME_SUBSET_FEED_ATTRIBUTELIST_OUT = './data/output/ph_product_en_subset_12202023_attributes.txt'

PRODUCT_URL_PREFIX = 'https://pacific.bloomreach.com/home/products/'
SUBSET_MAX_PRODUCTS_TO_USE_IN_CATEGORY = 15

# module_name and class_name. Currently, both are same names except the first-letter-case
CURRENT_UPDATER_MODULE = 'updateV1'
UPDATER_CLASS_NAME = 'UpdateV1'

