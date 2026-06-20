# sample aws cli cmd to upload image to aws (does require to install aws-cli on local m/c)
# the '--profile' is used for access control
# besides "s3 cp ...", you can also do
#   "s3 ls ..." (to list files)
#   "s3 rm ..." (to delete file)
#   <etc>

# source: local image file
# target: S3 bucket. In the below example, it is ..../home/images/....
# for SC's, you could create a bucket like .../br_sc/<specific_sc_name>/<some_sub_bucket>/... (just a thought). 
#   Just make sure not to upload files in to demo accounts' buckets
# Then the image can be accessed via https as:
#   https://pacific-demo-data.bloomreach.cloud/sc/<specific_sc_name>/<some_sub_bucket>/....

#aws --profile bloomreach-demo_main s3 cp --acl public-read  ./data/images/<local-file-name> s3://pacific-demo-data.bloomreach.cloud/home/images/....

