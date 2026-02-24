# module_name and class_name. Currently, both are same names except the first-letter-case
## FEED --- FULL (and SMALL for debugging) -- expected to be already converted

#CATALOG_TO_CONVERT = "PacificHome" # OR PacificApparel
CATALOG_TO_CONVERT = "PUMA"

# -------------------
if CATALOG_TO_CONVERT == "PacificHome":
    ''' 
    ######### PACIFICHOME #######
    '''

    FILENAME_JSONL_SOURCE_FEED_IN = './data/input/pacifichome/ph2_product_en_full_01142026.jsonl'
    #FILENAME_JSONL_SOURCE_FEED_IN = './data/input/pacifichome/ph2_product_en_full_01142026_10.jsonl'

    FILENAME_UPDATED_CSV_FEED_OUT = './data/output/pacifichome/ph2_product_en_full_engagement_01262026.csv'

    # JSONL created for debugging
    FILENAME_UPDATED_JSONL_FEED_OUT = './data/output/pacifichome/ph2_product_en_full_engagement_01262026.jsonl'

    PRODUCT_URL_PREFIX = 'https://pacific.bloomreach.com/home/products/'

elif CATALOG_TO_CONVERT == 'PacificApparel':
    # -------------------------------------

    '''
    ######### PACIFIC APPAREL #####
    '''

    FILENAME_JSONL_SOURCE_FEED_IN = './data/input/pacificapparel/pa_en_full_01162026.jsonl'
    #FILENAME_JSONL_SOURCE_FEED_IN = './data/input/pacificapparel/pa_en_full_01162026_10.jsonl'

    FILENAME_UPDATED_CSV_FEED_OUT = './data/output/pacificapparel/pa_en_full_engagement_01262026.csv'

    # JSONL created for debugging
    FILENAME_UPDATED_JSONL_FEED_OUT = './data/output/pacificapparel/pa_en_full_engagement_01262026.jsonl'

    PRODUCT_URL_PREFIX = 'https://pacific.bloomreach.com/apparel/products/'

elif CATALOG_TO_CONVERT == "PUMA":
    ''' 
    ######### PUMA #######
    '''

    FILENAME_JSONL_SOURCE_FEED_IN = './data/input/puma/puma_en.jsonl'
    #FILENAME_JSONL_SOURCE_FEED_IN = './data/input/puma/puma_en_10.jsonl'

    FILENAME_UPDATED_CSV_FEED_OUT = './data/output/puma/puma_en_02242026.csv'

    # JSONL created for debugging
    FILENAME_UPDATED_JSONL_FEED_OUT = './data/output/puma/puma_en_02242026.jsonl'

    PRODUCT_URL_PREFIX_UNUSED = 'https://eu.puma.com/de/en/pd/'
else:
    print ('@@@ UNKNOWN CATALOG TO CONVERT: %s\n' % CATALOG_TO_CONVERT)

# -----------------------------------------------
# COMMON FOR ALL CATALOGS

# CATEGORY LEVELS
MAX_CATEGORY_LEVELS = 4
PREAMBLE_ATTRIB_NAME_CATEGORY_LEVEL = 'category_level_'

# COMMA replaced with
COMMA_REPLACEMENT = ' '  # blank

# NOTE: default operation is 'copy as-is'
ATTRIB_SPECIAL_OPERATIONS = [
    { 'op': 'rename',   'src':  'availability',         'target': 'active' },   # 
    { 'op': 'add',      'src':  'pid',                  'target': 'item_id'},   # 
    { 'op': 'add',      'src':  'skuid',                'target': 'item_id'},   #
    { 'op': 'delete',   'src':  'category_paths',       'target': ''  },        # delete in output
    { 'op': 'delete',   'src':  'int_pid',              'target': ''  },        # delete in output
    { 'op': 'delete',   'src':  'mpn',                  'target': ''  },        # delete in output
    { 'op': 'delete',   'src':  'large_image',          'target': ''  },        # delete in output
    { 'op': 'delete',   'src':  'subject',              'target': ''  },        # delete in output
    { 'op': 'delete',   'src':  'bread_crumb',          'target': ''  },        # delete in output
    { 'op': 'delete',   'src':  'bread_crumb_id',       'target': ''  },        # delete in output
    { 'op': 'delete',   'src':  'shipping_info',        'target': ''  },        # delete in output
    { 'op': 'delete',   'src':  'product_brand',        'target': ''  },        # delete in output
    { 'op': 'delete',   'src':  'velo_material_lower',  'target': ''  },        # delete in output
    { 'op': 'delete',   'src':  'int_skuid',            'target': ''  },        # delete in output
    { 'op': 'delete',   'src':  'velo_sku_price',       'target': ''  },        # delete in output
    { 'op': 'delete',   'src':  'velo_sku_sale_price',  'target': ''  }         # delete in output
]


