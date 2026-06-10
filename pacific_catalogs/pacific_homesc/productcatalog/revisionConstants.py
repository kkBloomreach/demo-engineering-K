# revisionV3 specific constants

# s3 bucket where ph2 images are stored
PH2_IMAGE_S3_BUCKET_PREAMBLE = 's3://pacific-demo-data.bloomreach.cloud/home/images/gen_gptimage_1/webp/'

# s3 bucket where phsc images are stored
PHSC_IMAGE_S3_BUCKET_PREAMBLE = 's3://pacific-demo-data.bloomreach.cloud/homesc/images/gen_gptimage_1/webp/'
THUMB_IMAGE_URL_PROLOG = 'https://pacific-demo-data.bloomreach.cloud/homesc/images/gen_gptimage_1/webp/'

AWS_CP_COMMAND_PREAMBLE = 'aws --profile bloomreach-demo_main s3 cp --acl public-read '
AWS_RM_COMMAND_PREAMBLE = 'aws --profile bloomreach-demo_main s3 rm '

# bash template to check if file exists; also append 'fi' to close the if-stmt
THUMB_IMAGE_LOCAL_DIR = './data/images'
FILENAME_AWS_DOWNLOAD_UPLOAD_SCRIPT_OUT = './data/images/aws_image_download_upload.sh'
BASH_TEMPLATE_IF_FILE_EXISTS = 'if [ -e "%s" ]; then '
