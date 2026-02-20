# constants used for translation of original feed to index into BR (legacy connect)

SOURCE_FEED_FILE_NAME = './data/source/products.xml'
#SOURCE_FEED_FILE_NAME = './data/source/products_5.xml'
#SOURCE_FEED_FILE_NAME = './data/source/products_1_M.xml'
CATEGORY_INFO_FILE_NAME = './data/source/categories.tsv'
#OUTPUT_FEED_FILE_NAME = './data/output/output.xml'
OUTPUT_FEED_FILE_NAME = './data/output/full_feed_preprocessed_06212023.xml'

SOURCE_COMPANY_NAME = 'worldmarket.com'
REPLACED_COMPANY_NAME = 'pacifichome'
RESERVED_TEXT_1 = 'World Market'
REPLACED_RESERVED_TEXT_1 = 'PacificHome'

SOURCE_ROOT_NODE_NAME='products'
SOURCE_PRODUCT_NODE_NAME = 'product'
PRODUCT_SKU_FIELD_NAME = 'skuid'
OUTPUT_ROOT_NODE_NAME = 'feed'
OUTPUT_PRODUCT_NODE_NAME = 'product'
OUTPUT_PRODUCTS_NODE_NAME = 'products'

KEY_VALUE_DELIMITER = '!!' # delimiter used in key-value pairs from product.xml   
# DEFAULT_URL_PREFIX = 'https://cdn.brcdn.com/homeoasis.bloomreach.com_products/'
# DEFAULT_URL_POSTFIX = '.html'
DEFAULT_URL_PREFIX = 'https://pacifichome.bloomreach.com/products/'
DEFAULT_URL_POSTFIX = ''

#DEFAULT_IMAGE_URL_PREFIX= 'https://cdn.brcdn.com/homeoasis.bloomreach.com_products/'
DEFAULT_IMAGE_URL_PREFIX= 'https://pacific-demo-data.bloomreach.cloud/home/images/'
DEFAULT_IMAGE_URL_POSTFIX = '_XXX_v1.tif'

KEY_NAME_BREADCRUMB = 'bread_crumb' # used in current source xml
KEY_NAME_BREADCRUMB_ID = 'bread_crumb_id' # used in current output xml
KEY_NAME_GOOG_CATEGORY = 'google_category' # to be skipped if exists in src
KEY_NAME_LEAF_CATEGORIES = 'leaf_categories' # to be skipped if exists in src
KEY_NAME_PID = 'pid'
KEY_NAME_URL = 'url'
KEY_NAME_THUMB_IMAGE = 'thumb_image'
KEY_NAME_LARGE_IMAGE = 'large_image'
KEY_NAME_PRICE = 'price'
KEY_NAME_SALE_PRICE = 'sale_price'
KEY_NAME_SKU_THUMB_IMAGE = 'sku_thumb_image'
KEY_NAME_SKU_LARGE_IMAGE = 'sku_large_image'
KEY_NAME_TITLE = 'title'
KEY_NAME_STOCK_LEVEL = 'stock_level'
KEY_NAME_ONSALE = 'onSale'

BREADCRUMB_VALUE_DELIMITER = '>'   # as currently used in source xml
BREADCRUMBID_VALUE_DELIMITER = '>'   # set in outputProduct
BREADCRUMB_PARENTVALUE_DELIMITER_IN = '|' # as currently used in source xml
BREADCRUMB_PARENTVALUE_DELIMITER_OUT = '|' # used in generated output
CRUMBID_DELIMITER_IN_CATEGORYINFO = '/' # used in category.tsv
CRUMB_DELIMITER_IN_CATEGORYINFO = '/' # used internally to build fullCrumb in categoryInfo dict

DEFAULT_BREADCRUMBID = 123
 
REGEX_REGISTERED_TM = '\\u00AE' # 'Home(regTM)' --> Home

# PATTERN_RTM = Pattern.compile (REGEX_REGISTERED_TM)

NEXT_UNDEFINED_CRUMB_ID = DEFAULT_BREADCRUMBID


