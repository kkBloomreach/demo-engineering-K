## FEED --- FULL (and SMALL for debugging) -- expected to be already a subset
FILENAME_JSONL_SOURCE_FEED_DE_IN = './data/input/ph_content_de_11272023.jsonl'
#FILENAME_JSONL_SOURCE_FEED_DE_IN = './data/input/one.jsonl'
#FILENAME_JSONL_SOURCE_FEED_DE_IN = './data/input/ph_content_en_subset_11272023_5.jsonl'

FILENAME_UPDATED_JSONL_FEED_DE_OUT = './data/output/ph_content_de_08092024.jsonl'

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

SITE_CONTENT_DOCUMENTS_API_ENDPOINT = 'https://pacific-saas.bloomreach.io/delivery/site/v2/documents?folder=/haus/articles&limit=60'

'''
# tsv generated via tsvFeedGenerator, to be uploaded to google-doc for translation
# FILENAME_UPDATED_TSV_FEED_EN_OUT = './data/output/ph_content_en_subset_11272023.tsv'
# FILENAME_UPDATED_CSV_FEED_EN_OUT = './data/output/ph_content_en_subset_11272023.csv'
# 
# # tsv feed downloaded from google-doc after running google-translator
# FILENAME_TRANSLATED_TSV_FEED_IN = './data/output/ph_content_tr_subset_11272023.tsv'
# 
# FILENAME_UPDATED_TSV_FEED_DE_OUT = './data/output/ph_content_de_subset_11272023.tsv'
# FILENAME_UPDATED_CSV_FEED_DE_OUT = './data/output/ph_content_de_subset_11272023.csv'
# 
# # Format of *_me.json" is expected to be .json 
# FILENAME_UPDATED_JSONL_FEED_DE_MANUALLYEDITED_OUT = './data/output/ph_content_de_subset_11272023_me.json'
# FILENAME_UPDATED_JSONL_FEED_DE_MANUALLYEDITED_PROCESSED_OUT = './data/output/ph_content_de_subset_11272023_me_processed.jsonl'
# 
# # review file - contains translated attrib, values
# FILENAME_REVIEW_DOC_EN = './data/output/ph_product_en_review_10272023.txt'
# FILENAME_REVIEW_DOC_DE = './data/output/ph_product_de_review_10272023.txt'
# 
# SOURCE_CATALOG_LANGUAGE = 'English'
# TARGET_CATALOG_LANGUAGE = 'German'
# OPENAI_KEY = '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'    # should be 'secret' --:)
# 
# CONTENT_ATTRIBUTES_TO_TRANSLATE = [
#     'title',
#     'introduction',
#     'tags',
#     'xm_aggregated_descendants_text',
# ]
# 
# CONTENT_ATTRIBUTES_TRANSLATED = [
#     'title_de',
#     'introduction_de',
#     'tags_de',
#     'xm_aggregated_descendants_text_de'
# ]
'''

