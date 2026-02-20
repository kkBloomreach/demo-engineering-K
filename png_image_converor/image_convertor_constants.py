
# S3 pacific_apparel
#S3_SRC_IMAGE_BUCKET = 's3://pacific-demo-data.bloomreach.cloud/apparel/images/hlthbeauty/'
#S3_TARGET_IMAGE_BUCKET = 's3://pacific-demo-data.bloomreach.cloud/apparel/images/webp/hlthbeauty/'

# S3 pacifichome
S3_SRC_IMAGE_BUCKET = 's3://pacific-demo-data.bloomreach.cloud/home/images/gen/mattresses/'
S3_TARGET_IMAGE_BUCKET = 's3://pacific-demo-data.bloomreach.cloud/home/images/gen/webp/mattresses/'

S3_DOWNLOAD_CMD_PREAMBLE = 'aws --profile bloomreach-demo_main s3 cp '
S3_UPLOAD_CMD_PREAMBLE = 'aws --profile bloomreach-demo_main s3 cp --acl public-read '

# local pacific_apparel
#LOCAL_DOWNLOAD_WORK_DIR = './data/pacific_apparel/input/hlthbeauty'
#LOCAL_CONVERTED_WORK_DIR = './data/pacific_apparel/output/hlthbeauty'
#FILENAME_IMAGE_LIST_TSV_IN = 'pa_hlthbeauty_imagelist.tsv'
#FILENAME_UPLOADED_IMAGE_LIST_TSV_OUT = 'pa_hlthbeauty_imagelist_uploaded.tsv'

# local pacifichome
LOCAL_DOWNLOAD_WORK_DIR = './data/pacifichome/input'
LOCAL_CONVERTED_WORK_DIR = './data/pacifichome/output'
FILENAME_IMAGE_LIST_TSV_IN = 'ph_imagelist.tsv'
FILENAME_UPLOADED_IMAGE_LIST_TSV_OUT = 'ph_imagelist_uploaded.tsv'

# factor used when converting jpg to webp
JPG_CONVERSION_QUALITY_FACTOR = 80

