#!/usr/bin/env bash

# Will update/replace the existing attribute configuration for a content catalog
#   with supplied attributes json payload. Existing configuration will be wiped.
#
# ./modify_items.sh staging 6170 PATCH wc_content_en items.json

set -o errexit          # Exit on most errors (see the manual)
set -o errtrace         # Make sure any error trap is inherited
set -o nounset          # Disallow expansion of unset variables
set -o pipefail         # Use last non-zero exit code in a pipeline

REALM=$1 # prod or staging
ACCOUNT_ID=$2 # merchant account id, 4060
CATALOG_NAME=$3 # catalog name to ingest items into
ACCESS_KEY=$4 # API access key to use

# SAMPLE EXECUTION FOR PACIFIC_SUPPLY CONTENT_FEED (STAGING)
# sh ./indexer.sh staging 6370 PUT content_en UNUSED <access key>


[[ $REALM == "prod" ]] && DATACONNECT_HOST="api" || DATACONNECT_HOST="api-staging"

##### Execute INDEXER job
index_job=$(curl --location --request POST "http://$DATACONNECT_HOST.connect.bloomreach.com/dataconnect/api/v1/accounts/$ACCOUNT_ID/catalogs/$CATALOG_NAME/indexes" --header "Authorization: Bearer $ACCESS_KEY" | jq ' .jobId ' | tr -d '"')

echo $index_job

# get status of index job
curl --location --request GET "http://$DATACONNECT_HOST.connect.bloomreach.com/dataconnect/api/v1/jobs/$index_job" --header "Authorization: Bearer $ACCESS_KEY" | jq .

sleep 10

# get status of index job
curl --location --request GET "http://$DATACONNECT_HOST.connect.bloomreach.com/dataconnect/api/v1/jobs/$index_job" --header "Authorization: Bearer $ACCESS_KEY" | jq .

sleep 10

# get status of index job
curl --location --request GET "http://$DATACONNECT_HOST.connect.bloomreach.com/dataconnect/api/v1/jobs/$index_job" --header "Authorization: Bearer $ACCESS_KEY" | jq .


