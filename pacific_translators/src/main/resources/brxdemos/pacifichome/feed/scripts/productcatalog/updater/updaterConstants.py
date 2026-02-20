## FEED --- FULL (and SMALL for debugging) -- expected to be already converted
FILENAME_JSONL_SOURCE_FEED_IN = './data/input/ph_product_en_full_07292024.jsonl'
#FILENAME_JSONL_SOURCE_FEED_IN = './data/input/ph_product_en_full_07292024_10.jsonl'
#FILENAME_JSONL_SOURCE_FEED_IN = './data/input/ph_product_en_full_07292024_1.jsonl'

FILENAME_UPDATED_JSONL_FEED_OUT = './data/output/ph_product_en_full_09102024.jsonl'
FILENAME_UPDATED_TSV_FEED_OUT = './data/output/ph_product_en_full_09102024.tsv'
FILENAME_UPDATED_FEED_ATTRIBUTELIST_OUT = './data/output/ph_product_en_full_09102024_attributes.txt'

# subsets are created only in some update versions; not always
FILENAME_SUBSET_JSONL_FEED_OUT = './data/output/ph_product_en_subset_09102024.jsonl'
FILENAME_SUBSET_FEED_ATTRIBUTELIST_OUT = './data/output/ph_product_en_subset_09102024_attributes.txt'

PRODUCT_URL_PREFIX = 'https://pacific.bloomreach.com/home/products/'

# module_name and class_name. Currently, both are same names except the first-letter-case
CURRENT_UPDATER_MODULE = 'updateV5'
UPDATER_CLASS_NAME = 'UpdateV5'

