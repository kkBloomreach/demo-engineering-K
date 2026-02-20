# constants for revisionV7
FILENAME_SELECTED_PRODUCT_LIST = './data/input/selected_change_image_product_list.tsv'
#FILENAME_SELECTED_PRODUCT_LIST = './data/input/selected_change_image_product_list_2.tsv'

# new images
FILENAME_AWS_UPLOAD_SCRIPT_OUT = './data/images/aws_image_upload.sh'
AWS_S3_IMAGES_FOLDER = 's3://pacific-demo-data.bloomreach.cloud/home/images/gen/webp'
AWS_CP_COMMAND_PREAMBLE = 'aws --profile bloomreach-demo_main s3 cp --acl public-read '
AWS_RM_COMMAND_PREAMBLE = 'aws --profile bloomreach-demo_main s3 rm '

