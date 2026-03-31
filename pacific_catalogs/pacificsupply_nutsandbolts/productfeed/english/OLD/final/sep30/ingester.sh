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
INGEST_MODE=$3 # PATCH or PUT
CATALOG_NAME=$4 # catalog name to ingest items into
JSON_SFTP_FILE_LIST=$5 # json patch file to spec. array of patch ops
ACCESS_KEY=$6 # API access key to use

# SAMPLE EXECUTION FOR PACIFIC_SUPPLY CONTENT_FEED (STAGING)
# sh ./indexer.sh staging 6370 PUT content_en sftpfiles.json <access key>

[[ $REALM == "prod" ]] && DATACONNECT_HOST="api" || DATACONNECT_HOST="api-staging"

# Execute ingest API
ingest_job=$(curl --location --request $INGEST_MODE "http://$DATACONNECT_HOST.connect.bloomreach.com/dataconnect/api/v1/accounts/$ACCOUNT_ID/catalogs/$CATALOG_NAME/items" --header "Authorization: Bearer $ACCESS_KEY" --header "Content-Type: application/json" --data-binary "@$JSON_SFTP_FILE_LIST" | jq ' .jobId ' | tr -d '"')

echo $ingest_job

# get status of ingest job execution
curl --location --request GET "http://$DATACONNECT_HOST.connect.bloomreach.com/dataconnect/api/v1/jobs/$ingest_job" --header "Authorization: Bearer $ACCESS_KEY" | jq .

sleep 4

# once again get status of ingest job execution
curl --location --request GET "http://$DATACONNECT_HOST.connect.bloomreach.com/dataconnect/api/v1/jobs/$ingest_job" --header "Authorization: Bearer $ACCESS_KEY" | jq .

