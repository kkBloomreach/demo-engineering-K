# revisionV30 specific constants
FILENAME_VARIANT_COLOR_SWATCHURL_MAP_TSV_OUT = './data/images/variant_color_swatchurl_map_11172025.tsv'

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

# OPENAI to genereate images
# used for downloading images
# image files are downloaded in LOCAL_DIR/data/images
OPENAI_KEY = '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'
OPENAI_MODEL = 'gpt-4o'
OPENAI_MODEL_IMAGE_GENERATION = 'gpt-image-1' # for image generation

THUMB_IMAGE_LOCAL_DIR = './data/images'
THUMB_IMAGE_URL_PROLOG = 'https://pacific-demo-data.bloomreach.cloud/apparel/images/webp/gen_gptimage_1'

HTTP_STATUS_OK = 200

FILENAME_AWS_UPLOAD_SCRIPT_OUT = './data/images/aws_image_upload.sh'
# s3 folder for gen-gpt-images
AWS_S3_GEN_GPTIMAGE_1_IMAGES_FOLDER = 's3://pacific-demo-data.bloomreach.cloud/apparel/images/webp/gen_gptimage_1'
AWS_CP_COMMAND_PREAMBLE = 'aws --profile bloomreach-demo_main s3 cp --acl public-read '
AWS_RM_COMMAND_PREAMBLE = 'aws --profile bloomreach-demo_main s3 rm '


