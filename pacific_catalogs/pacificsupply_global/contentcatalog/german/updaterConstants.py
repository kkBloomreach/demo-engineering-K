## FEED --- FULL (and SMALL for debugging) -- expected to be already a subset
FILENAME_JSONL_SOURCE_FEED_IN = './data/input/ps_content_de_08302023.jsonl'
#FILENAME_JSONL_SOURCE_FEED_IN = './data/input/one.jsonl'
#FILENAME_JSONL_SOURCE_FEED_IN = './data/input/ps_content_de_08302023_5.jsonl'

FILENAME_UPDATED_JSONL_FEED_DE_OUT = './data/output/psg_content_de_08092024.jsonl'

# attributes in each content record as used in Discovery (besides "path", "op", ...}
CONTENT_ATTRIBUTES_TO_UPDATE = [
    'tags',
    'introduction',
    'title',
    'author',
    'url',  # same sourceurl, xm_urls [], 
    'image',
    'xm_aggregated_descendants_text'
]

SITE_CONTENT_DOCUMENTS_API_ENDPOINT = 'https://pacific-saas.bloomreach.io/delivery/site/v2/documents?folder=/versorgung/artikel&limit=60'


''' 
##########
# SOURCE_CATALOG_LANGUAGE = 'English'
# TARGET_CATALOG_LANGUAGE = 'German'

# Currently, OpenAI NOT USED for translation
# OPENAI_KEY = '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'    # should be 'secret' --:)

# CONTENT_ATTRIBUTES_TO_TRANSLATE = [
#     'title',
#     'introduction',
#     'tags',
#     'xm_aggregated_descendants_text'
# ]

# tsv downloaded from google-doc after running google translator
# This tsv was provided by another engr (who has since left Bloomreach)
# FILENAME_TRANSLATED_TSV_FEED_IN = './data/output/ps_content_tr_merged_08082024.tsv'
# FILENAME_UPDATED_TSV_FEED_DE_OUT = './data/output/ps_content_de_subset_08082024.tsv'
# FILENAME_UPDATED_CSV_FEED_DE_OUT = './data/output/ps_content_de_subset_08082024.csv'

# Format of *_me.json" is expected to be .json 
# FILENAME_UPDATED_JSONL_FEED_DE_MANUALLYEDITED_OUT = './data/output/ps_content_de_subset_08082024.json'
# FILENAME_UPDATED_JSONL_FEED_DE_MANUALLYEDITED_PROCESSED_OUT = './data/output/ps_content_de_subset_08082024.jsonl'

# review file - contains translated attrib, values
# FILENAME_REVIEW_DOC_EN = './data/output/ps_product_en_review_08082024.txt'
# FILENAME_REVIEW_DOC_DE = './data/output/ps_product_de_review_08082024.txt'
'''

