# revisionV8 specific constants
FILENAME_CATEGORY_STATUS_MAP_TSV_IN = './data/input/ph_category_status_map_sept2024.tsv'
FILENAME_PRUNDED_CATEGORY_STATUS_MAP_TSV_OUT = './data/output/ph_pruned_category_status_map_10242024.tsv'

# category tree map - contains info about which categories to remove etc
FILENAME_CATEGORY_STATUS_MAP = './data/input/category_status_map_09302024.tsv'

CATEGORY_STATUS_KEEP = 0
CATEGORY_STATUS_REMOVE = -1

# products in Trends category get 'collection' attribute
# with value = Trends->sub_category. The collection attribute is single-value
TRENDS_CAT_ID = '142'

# "category_levels" attrib
MAX_CATEGORY_LEVELS = 4
PREAMBLE_ATTRIB_NAME_CATEGORY_LEVEL = 'category_level_'

# attribute name
ATTRIB_NAME_RETURN_RATE = 'return_rate'

