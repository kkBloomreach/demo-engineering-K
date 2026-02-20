# revisionV12 specific constants
FILENAME_ADJUST_COLOR_NAMES_TSV_IN = './data/input/adjust_color_names_10282025.tsv'
FILENAME_ADJUST_MISSPELLED_TERMS_TSV_IN = './data/input/adjust_misspelled_terms_10282025.tsv'

COLOR_CHOICES = [
    'black',
    'blue',
    'gold',
    'green',
    'orange',
    'pink',
    'red',
    'silver',
    'white'
]

MIN_VARIANTS_PER_PRODUCT = 3
MAX_VARIANTS_PER_PRODUCT = 5

# OPENAI to genereate images
# used for downloading images
# image files are downloaded in LOCAL_DIR/data/images
OPENAI_KEY = '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'
OPENAI_MODEL = 'gpt-4o'
OPENAI_MODEL_IMAGE_GENERATION = 'gpt-image-1' # for image generation

THUMB_IMAGE_LOCAL_DIR = './data/images'
THUMB_IMAGE_URL_PROLOG = 'https://pacific-demo-data.bloomreach.cloud/home/images/webp/gen_gptimage_1'

HTTP_STATUS_OK = 200

# new images' aws store
FILENAME_AWS_UPLOAD_SCRIPT_OUT = './data/images/aws_image_upload.sh'
AWS_S3_IMAGES_FOLDER = 's3://pacific-demo-data.bloomreach.cloud/home/images/webp/gen_gptimage_1'
AWS_CP_COMMAND_PREAMBLE = 'aws --profile bloomreach-demo_main s3 cp --acl public-read '
AWS_RM_COMMAND_PREAMBLE = 'aws --profile bloomreach-demo_main s3 rm '


