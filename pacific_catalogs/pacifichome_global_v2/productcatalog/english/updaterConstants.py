# module_name and class_name. Currently, both are same names except the first-letter-case
REVISION_UPDATER_MODULE = 'revisionV1'
REVISION_CLASS_NAME = 'RevisionV1'

## FEED --- FULL (and SMALL for debugging) -- expected to be already available
FILENAME_PH2_ENGLISH_JSONL_SOURCE_FEED_IN = './data/input/ph2_product_en_full_10092025.jsonl'
#FILENAME_PH2_ENGLISH_JSONL_SOURCE_FEED_IN = './data/input/ph2_product_en_full_10092025_10.jsonl'

FILENAME_PHG2_ENGLISH_UPDATED_JSONL_FEED_OUT = './data/output/phg2_product_en_full_10102025.jsonl'
FILENAME_PHG2_ENGLISH_UPDATED_TSV_FEED_OUT = './data/output/phg2_product_en_full_10102025.tsv'
FILENAME_PHG2_ENGLISH_UPDATED_FEED_ATTRIBUTELIST_OUT = './data/output/phg2_product_en_full_10102025.txt'
FILENAME_PHG2_ENGLISH_INJECTED_AVMAP_OUT = './data/output/phg2_injected_avmap_10102025.tsv'

# Injected av-map path (initially generated when different catalogs merged. later edited manually)
FILENAME_INJECTED_AV_MAP_IN = './data/NONE'
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
PRODUCT_URL_PREFIX = 'https://pacific.bloomreach.com/homeglobal/products/'



