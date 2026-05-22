# module_name and class_name. Currently, both are same names except the first-letter-case
REVISION_UPDATER_MODULE = 'revision'
REVISION_CLASS_NAME = 'Revision'

## FEED --- FULL (and SMALL for debugging) -- expected to be already available
FILENAME_JSONL_SOURCE_FEED_IN = './data/input/phsc_product_en_full_07032025.jsonl'
#FILENAME_JSONL_SOURCE_FEED_IN = './data/input/phsc_product_en_full_07032025_1.jsonl'

FILENAME_UPDATED_JSONL_FEED_OUT = './data/output/phsc_product_en_full_10102025.jsonl'
FILENAME_UPDATED_TSV_FEED_OUT = './data/output/phsc_product_en_full_10102025.tsv'
FILENAME_UPDATED_FEED_ATTRIBUTELIST_OUT = './data/output/phsc_product_en_full_10102025.txt'
FILENAME_INJECTED_AVMAP_TSV_OUT = './data/output/injected_avmap_10102025.tsv'
FILENAME_CATEGORY_TREE_TSV_OUT = './data/output/category_tree_10102025.tsv'

# Injected av-map path (initially generated when different catalogs merged. later edited manually)
FILENAME_INJECTED_AV_MAP_IN = './data/input/NONE.tsv'
INJECTED_AVMAP_EXCLUDED_ATTRIBUTES = [
	'bread_crumb',
	'bread_crumb_id',
	'category_level_1',
	'category_level_2',
	'category_level_3',
	'category_level_4',
	'category_paths',
	'color',
	'colorFamily',
	'default_sku',
	'description',
	'entityType',
	'int_pid',
	'int_skuid',
	'large_image',
	'mpn',
	'product_brand',
	'product_type',
	'sale_price_range_max',
	'sale_price_range_min',
	'size',
	'sizeFamily',
	'skuid',
	'subject',
	'swatch_image',
	'thumb_image',
	'velo_material_lower',
	'velo_sku_price',
	'velo_sku_sale_price'
]

# URL preambles
PRODUCT_URL_PREFIX = 'https://pacific.bloomreach.com/homesc/products/'
IMAGE_URL_PREFIX = 'https://pacific-demo-data.bloomreach.cloud/homesc/images/'

