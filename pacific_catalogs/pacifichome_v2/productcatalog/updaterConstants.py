# module_name and class_name. Currently, both are same names except the first-letter-case
REVISION_UPDATER_MODULE = 'revisionV23'
REVISION_CLASS_NAME = 'RevisionV23'

## FEED --- FULL (and SMALL for debugging) -- expected to be already available
FILENAME_JSONL_SOURCE_FEED_IN = './data/input/ph2_product_en_full_01142026.jsonl'
#FILENAME_JSONL_SOURCE_FEED_IN = './data/input/ph2_product_en_full_01142026_10.jsonl'

FILENAME_UPDATED_JSONL_FEED_OUT = './data/output/ph2_product_en_full_03112026.jsonl'
FILENAME_UPDATED_DATAHUB_JSONL_FEED_OUT = './data/output/ph2_product_en_full_datahub_03112026.jsonl'
FILENAME_UPDATED_ENGAGEMENT_TSV_FEED_OUT = './data/output/ph2_product_en_full_engagement_03112026.tsv'
FILENAME_UPDATED_DATACONNECT_TSV_FEED_OUT = './data/output/ph2_product_en_full_dataconnect_03112026.tsv'
FILENAME_UPDATED_FEED_ATTRIBUTELIST_OUT = './data/output/ph2_product_en_full_03112026_attributes.txt'
FILENAME_UPDATED_FEED_ENGAGEMENT_ATTRIBUTELIST_OUT = './data/output/ph2_product_en_full_engagement_03112026.txt'
FILENAME_INJECTED_AVMAP_OUT = './data/output/injected_avmap_03112026.tsv'

# Injected av-map path (initially generated when different catalogs merged. later edited manually)
FILENAME_INJECTED_AV_MAP_IN = './data/input/injected_avmap_12062025.tsv'
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
PRODUCT_URL_PREFIX = 'https://pacific.bloomreach.com/home/products/'

