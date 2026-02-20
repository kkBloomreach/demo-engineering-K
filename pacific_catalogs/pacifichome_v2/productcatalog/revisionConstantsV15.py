# revisionV15 related constants
FILENAME_VARIANT_COLOR_SWATCHURL_MAP_TSV_OUT = './data/images/variant_color_swatchurl_map_11252025.tsv'

# color-list size must be > (MAX_COLOR * MAX_SIZE) values defined below
COLOR_CHOICES = [
    'green',
    'white',
    'red',
    'yellow',
    'gray',
    'blue',
    'black',
    'purple',
    'brown',
    'berry',
    'bronze',
    'burgundy',
    'cream',
    'mauve',
    'navy',
    'peach',
    'rose',
    'taupe',
    'teal',
    'tan'
]

SIZE_CHOICES = [
    '7',
    '6',
    '8.5',
    '10',
    '9',
    '9.5',
    '12',
    '8'
]


MAX_COLORS_PER_PRODUCT = 4
MAX_SIZES_PER_PRODUCT = 4
MIN_VARIANTS_PER_PRODUCT = 4
MAX_VARIANTS_PER_PRODUCT = 6

# OPENAI to genereate images
# used for downloading images
# image files are downloaded in LOCAL_DIR/data/images
OPENAI_KEY = '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'
OPENAI_MODEL = 'gpt-4o'
OPENAI_MODEL_IMAGE_GENERATION = 'gpt-image-1' # for image generation

THUMB_IMAGE_LOCAL_DIR = './data/images'
THUMB_IMAGE_URL_PROLOG = 'https://pacific-demo-data.bloomreach.cloud/home/images/webp/gen_gptimage_1'

HTTP_STATUS_OK = 200

FILENAME_AWS_UPLOAD_SCRIPT_OUT = './data/images/aws_image_upload.sh'
# s3 folder for gen-gpt-images
AWS_S3_GEN_GPTIMAGE_1_IMAGES_FOLDER = 's3://pacific-demo-data.bloomreach.cloud/home/images/webp/gen_gptimage_1'
AWS_CP_COMMAND_PREAMBLE = 'aws --profile bloomreach-demo_main s3 cp --acl public-read '
AWS_RM_COMMAND_PREAMBLE = 'aws --profile bloomreach-demo_main s3 rm '

# search API to collect pids for specific search terms

MAX_START_ROW_ALLOWED = 10000; # Bloomreach does not allow "startRow > 10000" in api call.
MTB_API_CALL = 2 # N seconds just so that Discovery does not return status 429

SEARCH_APICALL_TEMPLATE="https://core.dxpapi.com/api/v1/core/?account_id=6413&auth_key=bcvpynhij980k0y1&domain_key=pacifichome&request_id=7057573203767&_br_uid_2=uid=3658782131650:v=15.0:ts=1763992653771:hc=13&url=www.bloomique.com&ref_url=www.bloomique.com&request_type=search&rows=100&start=$START&fl=pid&q=$QUERY&search_type=keyword"

# categories for variant-slice pids
SELECT_CATEGORY_IDS = [
    '116728>116885',  # Pillows
    '116728>116947',  # Baskets
    '116728>119563'   # Curtains
]

SELECT_SEARCH_TERMS_TEST = [
    'blankets'
]

SELECT_SEARCH_TERMS = [
    'dinnerware',
    'bowls',
    'bath towels',
    'throws',
    'blankets'
]


