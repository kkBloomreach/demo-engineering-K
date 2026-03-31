# revisionV35 specific constants

# OPENAI to genereate images
OPENAI_KEY = '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'
OPENAI_MODEL = 'gpt-4o'
#OPENAI_MODEL_IMAGE_GENERATION = 'gpt-image-1' # for image generation
OPENAI_MODEL_IMAGE_GENERATION = 'gpt-image-1.5' # for image generation - latest version

THUMB_IMAGE_LOCAL_DIR = './data/images'
THUMB_IMAGE_URL_PROLOG = 'https://pacific-demo-data.bloomreach.cloud/apparel/images/webp/gen_gptimage_1_5'

HTTP_STATUS_OK = 200

FILENAME_AWS_UPLOAD_SCRIPT_OUT = './data/images/aws_image_upload.sh'
# s3 folder for gen-gpt-images
AWS_S3_GEN_GPTIMAGE_1_IMAGES_FOLDER = 's3://pacific-demo-data.bloomreach.cloud/apparel/images/webp/gen_gptimage_1_5'
AWS_CP_COMMAND_PREAMBLE = 'aws --profile bloomreach-demo_main s3 cp --acl public-read '
AWS_RM_COMMAND_PREAMBLE = 'aws --profile bloomreach-demo_main s3 rm '


