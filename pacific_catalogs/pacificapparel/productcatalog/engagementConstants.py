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
    { 'op': 'delete',   'src':  'product_brand',        'target': ''  }         # delete in output
]


