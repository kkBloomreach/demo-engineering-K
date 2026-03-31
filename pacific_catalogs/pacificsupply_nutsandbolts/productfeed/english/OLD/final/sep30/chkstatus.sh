# JOB_ID=711786db-1bdc-43ac-9355-ad76445c7ca7
JOB_ID=$1
curl --location --request GET "http://api-staging.connect.bloomreach.com/dataconnect/api/v1/jobs/$JOB_ID" --header "Authorization: Bearer pacific_supply_mindcurv-staging-814bfe11-bfcc-4138-b7ea-d01a81462fe2"  | jq .

#curl --location --request GET "http://api-staging.connect.bloomreach.com/dataconnect/api/v1/jobs/$JOB_ID" --header "Authorization: Bearer pacific_supply_mindcurv-staging-814bfe11-bfcc-4138-b7ea-d01a81462fe2" | jq .

# curl --location --request GET "http://api-staging.connect.bloomreach.com/dataconnect/api/v1/jobs/cfbbc1ba-1b60-40a9-bf5a-9e3559befc89" --header "Authorization: Bearer internal_feed_test-staging-c71ffb9b-6c54-46b8-983e-2138bc39d85c" | jq .

