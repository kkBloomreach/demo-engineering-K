## FEED --- FULL (and SMALL for debugging) -- expected to be already a subset
#FILENAME_JSONL_SOURCE_FEED_EN_IN = './data/input/ps_product_en_subset_11012023.jsonl'
#FILENAME_JSONL_SOURCE_FEED_EN_IN = './data/input/one.jsonl'
FILENAME_JSONL_SOURCE_FEED_EN_IN = './data/input/ps_product_en_subset_11012023_5.jsonl'

FILENAME_UPDATED_JSONL_FEED_DE_OUT = './data/output/ps_product_de_subset_11012023.jsonl'
FILENAME_UPDATED_TSV_FEED_DE_OUT = './data/output/ps_product_de_subset_11012023.tsv'
FILENAME_UPDATED_CSV_FEED_DE_OUT = './data/output/ps_product_de_subset_11012023.csv'
FILENAME_UPDATED_FEED_ATTRIBUTELIST_DE_OUT = './data/output/ps_product_de_subset_11012023_attributes.txt'

# Format of *_me.json" is expected to be .json 
FILENAME_UPDATED_JSONL_FEED_DE_MANUALLYEDITED_OUT = './data/output/ps_product_de_subset_11012023_me.json'
FILENAME_UPDATED_JSONL_FEED_DE_MANUALLYEDITED_PROCESSED_OUT = './data/output/ps_product_de_subset_11012023_me_processed.jsonl'

# review file - contains translated attrib, values
FILENAME_REVIEW_DOC_EN = './data/output/ps_product_en_review_11012023.txt'
FILENAME_REVIEW_DOC_DE = './data/output/ps_product_de_review_11012023.txt'

SOURCE_CATALOG_LANGUAGE = 'English'
TARGET_CATALOG_LANGUAGE = 'German'
OPENAI_KEY = 'SHOULD_BE_IN_DOT_ENV'    # should be 'secret' --:)

PRODUCT_ATTRIBUTES_TO_TRANSLATE = [
    'title',
    'description',
    'material',
    'condition',
    'velo_material_lower',
    'Packaging Quantity',
    'type'
]

# Note - currently there are no variants in the PacificSupply catalog
VARIANT_ATTRIBUTES_TO_TRANSLATE = [
    'colorFamily',
    'color'
]

