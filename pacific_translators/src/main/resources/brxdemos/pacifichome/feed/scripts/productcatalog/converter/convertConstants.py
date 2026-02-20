## FEED --- FULL (and SMALL for debugging)
FILENAME_BRSM_FEED_IN = './data/input/full_feed_preprocessed_06212023_beautified.xml'
#FILENAME_BRSM_FEED_IN = './data/input/one.xml'
#FILENAME_BRSM_FEED_IN = './data/input/20792.xml'

## BRANDS --- FULL (and SMALL for debugging)
PACIFIC_HOME_PRODUCT_BRAND_DEFAULT= 'Pacific Goods'
FILENAME_PID_BRANDMAP_IN = './data/input/pacifichome_brands.tsv'
#FILENAME_PID_BRANDMAP_IN = './data/input/pacifichome_brands_1.tsv'

## PROXY PRODUCTS
FILENAME_BRSM_PROXY_PRODUCTS_FEED_IN = './data/input/proxy_products.xml'

# File containing generated attribute data
# These values are created only once (not each time feed is generated)
FILENAME_PID_ATTRIBUTE_GENERATED_VALUES = './data/input/pacifichome_generated_attribute_values.tsv'

# Due to openAI genimage process, we could not generate images for certain
# products. Therefore those products (and their associated variants) are deleted
# from the catalog. List of those pids is in this file (generated while creating openAI images)
FILENAME_PID_TO_DELETE_DUE_TO_GENIMAGE = './data/input/pid_to_delete_due_to_genimage.tsv'

FILENAME_DATACONNECT_OUT = './data/output/full_feed_preprocessed_06212023_09212023.jsonl'
FILENAME_DATACONNECT_OUT_BEAUTIFIED = './data/output/full_feed_preprocessed_06212023_09212023_beautified.json'
FILENAME_ATTRIBUTELIST_OUT = './data/output/full_feed_preprocessed_06212023_09212023_attributes.txt'
FILENAME_TSV_OUT = './data/output/full_feed_preprocessed_06212023_09212023.tsv'

CRUMB_VALUE_SEPARATOR = '>'
CRUMB_PARENT_VALUE_SEPARATOR = '|'
CRUMB_ID_VALUE_SEPARATOR = '>'
CRUMB_ID_PARENT_VALUE_SEPARATOR = '|'

PRODUCT_URL_PREFIX = 'https://pacific.bloomreach.com/home/products/'


