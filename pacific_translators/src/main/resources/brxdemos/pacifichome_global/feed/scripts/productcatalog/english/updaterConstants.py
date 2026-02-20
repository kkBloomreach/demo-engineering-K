## FEED --- FULL (and SMALL for debugging) -- expected to be already a subset
FILENAME_JSONL_SOURCE_FEED_EN_IN = './data/input/ph_product_en_subset_12202023.jsonl'
#FILENAME_JSONL_SOURCE_FEED_EN_IN = './data/input/one.jsonl'
#FILENAME_JSONL_SOURCE_FEED_EN_IN = './data/input/ph_product_en_subset_12202023_5.jsonl'

FILENAME_UPDATED_JSONL_FEED_EN_OUT = './data/output/ph_product_en_subset_12202023.jsonl'
FILENAME_UPDATED_TSV_FEED_EN_OUT = './data/output/ph_product_en_subset_12202023.tsv'
FILENAME_UPDATED_CSV_FEED_EN_OUT = './data/output/ph_product_en_subset_12202023.csv'
FILENAME_UPDATED_FEED_ATTRIBUTELIST_EN_OUT = './data/output/ph_product_en_subset_12202023_attributes.txt'

# Format of *_me.json" is expected to be .json 
FILENAME_UPDATED_JSONL_FEED_EN_MANUALLYEDITED_OUT = './data/output/ph_product_en_subset_12202023_me.json'
FILENAME_UPDATED_JSONL_FEED_EN_MANUALLYEDITED_PROCESSED_OUT = './data/output/ph_product_en_subset_10202023_me_processed.jsonl'

# review file - contains translated attrib, values
FILENAME_REVIEW_DOC_EN = './data/output/ph_product_en_review_12202023.txt'
FILENAME_REVIEW_DOC_DE = './data/output/ph_product_de_review_12202023.txt'

SOURCE_CATALOG_LANGUAGE = 'English'
TARGET_CATALOG_LANGUAGE = 'German'
OPENAI_KEY = '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'    # should be 'secret' --:)

PRODUCT_ATTRIBUTES_TO_TRANSLATE = [
    'title',
    'condition',
    'keywords',
    'description',
    'material',
    'product_type',
    # 'wineVarietals' ??
]

VARIANT_ATTRIBUTES_TO_TRANSLATE = [
    'colorFamily',
    'color'
]

PRODUCT_URL_PREFIX = 'https://pacific.bloomreach.com/home-global/products/'
