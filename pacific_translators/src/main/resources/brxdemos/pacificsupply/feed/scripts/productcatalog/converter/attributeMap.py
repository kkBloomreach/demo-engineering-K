# list of { origAttribName, keep/remove, newName, product-level/variant-level }
# names are case sensitive. 
# if 'remove', remaining fields are unused/undefined
# pacific_supply catalog does not have SKUs (aka, variants)
# Some source attributes are lower-cased just to keep them consistent with the names used in pacific_supply/GLOBAL catalog
# Some attributes are lower-cased because BR requires static attribs in lowercase (eg, color)
# "catalog_number" value is split specifically to support catalog_number_partialvalue. It is not view_specific
# "customerpartnumber" is view_specific (but not split as partial-value)
# Catalog has 400+ attributes, many of which have blank values for all products. Those should be excluded -- TO BE DONE
ATTRIBUTE_MAP = [
    { 'name': 'Color',                  'keep': True,  'newName': 'color',            'isProductAttrib': True },
    { 'name': 'Type',                   'keep': True,  'newName': 'type',             'isProductAttrib': True },
    { 'name': 'rating',                 'keep': True,  'newName': 'rating',           'isProductAttrib': True },
    { 'name': 'Mfr Name',               'keep': True,  'newName': 'manufacturer',     'isProductAttrib': True },
    { 'name': 'Catalog Number',         'keep': True,  'newName': 'catalog_number',   'isProductAttrib': True },
    { 'name': 'Master UPC',             'keep': True,  'newName': 'master_upc',       'isProductAttrib': True },
    { 'name': 'Application',            'keep': True,  'newName': 'application',      'isProductAttrib': True },
    { 'name': 'Height',                 'keep': True,  'newName': 'height',           'isProductAttrib': True },
    { 'name': 'Length',                 'keep': True,  'newName': 'length',           'isProductAttrib': True },
    { 'name': 'Material',               'keep': True,  'newName': 'material',         'isProductAttrib': True },
    { 'name': 'Size',                   'keep': True,  'newName': 'size',             'isProductAttrib': True },
    { 'name': 'Voltage Rating',         'keep': True,  'newName': 'voltage_rating',   'isProductAttrib': True },
    { 'name': 'Series/Model',           'keep': True,  'newName': 'model',            'isProductAttrib': True },
    { 'name': 'Standards',              'keep': True,  'newName': 'standards',        'isProductAttrib': True },
    { 'name': 'Weight',                 'keep': True,  'newName': 'weight',           'isProductAttrib': True },
    { 'name': 'Absorption Capacity',    'keep': False, 'newName': '',                 'isProductAttrib': True },
    { 'name': 'Adhesive Strength',      'keep': False, 'newName': '',                 'isProductAttrib': True }
]

# attribute name used to define views
VIEWID_ATTRIBUTE_NAME = 'view_id'

# all products are in 'master'. The view-specific-values in 'master' are
# treated as 'base' values. These are then overridden by individual view_specific_values
# as needed
VIEWID_NAME_MASTER = 'master'

# attributes that have view-specific values
VIEW_SPECIFIC_ATTRIBUTE_NAMES = [ \
        'price', \
        'sale_price', \
        'customerpartnumber', \
]

VIEWID_DELIMITER = '|'  # id1|id2|...
VIEW_VALUE_DELIMITER = '|' # val1|val2|...

CRUMB_VALUE_SEPARATOR = '|'
CRUMB_ID_VALUE_SEPARATOR = '|'

# Following values set to true / false
BOOLEAN_ATTRIBUTES = [
]

# Following values set to 'number'. Note that SOME are view_specific values (eg, price)
# Attrib names here are 'newName' mentioned in attributeMap list above (eg, "weight", not "Weight")
FLOAT_ATTRIBUTES = [
    'price',
    'sale_price',
    'weight'
]

# part-number search is supported for the following attributes
# Attrib names here are 'newName' mentioned in attributeMap list above (eg, "catalog_number", not "Catalog Number")
PARTIAL_VALUE_ATTRIBUTE_LIST = [ 
    'pid', 
    'catalog_number' 
]
MIN_PARTIALVALUE_LENGTH = 3
MAX_PARTIALVALUE_LENGTH = 30

# List of attributes used for Attribute query filtering. 
# For these attributes, the values have to be lower case
# NOTE: Currently view-specific attributes are not supported in PacificSupply
# Attrib names here are 'newName' mentioned in attributeMap list above (eg, "catalog_number", not "Catalog Number")
AQF_ATTRIBUTES = [
    'brand',
    'Series/Model',
    'Standards',
    'Mfr Name',
    'Material'
]
