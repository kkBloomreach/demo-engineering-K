# q0 -> {q1} -> {q2} -> ...
MAX_REFINED_QUERY_LEVELS = 2 # testing

# PacificApparel
TRAINING_CONFIG_JSON_IN = './data/input/pacific_apparel/training_config.json'
SOURCE_CATALOG_JSONL_IN = './data/input/pacific_apparel/pa_en_full_03252026.jsonl'
#SOURCE_CATALOG_JSONL_IN = './data/input/pacific_apparel/pa_en_full_03252026_2.jsonl'
#REFINED_QUERIES_OUTPUT_TSV_OUT = './data/output/pacific_apparel/pa_en_full_refined_queries_04302026_female.tsv'
REFINED_QUERIES_OUTPUT_TSV_OUT = './data/output/pacific_apparel/pa_en_full_refined_queries_04302026_male.tsv'

# PacificHome
#SOURCE_CATALOG_JSONL_IN = './data/input/pacifichome/ph2_product_en_full_03112026.jsonl'
#SOURCE_CATALOG_JSONL_IN = './data/input/pacifichome/ph2_product_en_full_03112026_2.jsonl'
#REFINED_QUERIES_OUTPUT_TSV_OUT = './data/output/pacifichome/ph2_en_full_refined_queries_04032026.tsv'

OPENAI_KEY = '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'
OPENAI_MODEL = 'gpt-5.1'
HTTP_STATUS_OK = 200

