## FEED --- FULL (and SMALL for debugging) -- expected to be already a subset
FILENAME_JSONL_SOURCE_FEED_EN_IN = './data/input/ps_content_en_subset_11022023.jsonl'
#FILENAME_JSONL_SOURCE_FEED_EN_IN = './data/input/one.jsonl'
#FILENAME_JSONL_SOURCE_FEED_EN_IN = './data/input/ps_content_en_subset_11022023_5.jsonl'

# FILENAME_UPDATED_JSONL_FEED_EN_OUT = './data/output/ps_content_en_subset_11022023.jsonl'
# .tsv, .csv not generated for content catalog
#FILENAME_UPDATED_TSV_FEED_EN_OUT = './data/output/ps_content_en_subset_11022023.tsv'
#FILENAME_UPDATED_CSV_FEED_EN_OUT = './data/output/ps_content_en_subset_11022023.csv'

# Format of *_me.json" is expected to be .json 
FILENAME_UPDATED_JSONL_FEED_EN_MANUALLYEDITED_OUT = './data/output/ps_content_en_subset_11022023_me.json'
FILENAME_UPDATED_JSONL_FEED_EN_MANUALLYEDITED_PROCESSED_OUT = './data/output/ps_content_en_subset_11022023_me_processed.jsonl'

# review file - contains translated attrib, values
FILENAME_REVIEW_DOC_EN = './data/output/ps_product_en_review_11022023.txt'
FILENAME_REVIEW_DOC_DE = './data/output/ps_product_de_review_11022023.txt'

SOURCE_CATALOG_LANGUAGE = 'English'
TARGET_CATALOG_LANGUAGE = 'German'
OPENAI_KEY = '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'    # should be 'secret' --:)

CONTENT_ATTRIBUTES_TO_TRANSLATE = [
    'title',
    'introduction',
    'xm_aggregated_descendants_text',
]

