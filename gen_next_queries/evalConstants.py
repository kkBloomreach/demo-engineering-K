DEFAULT_UID = "111222333444"
API_ENDPOINT_STAGING = "https://staging-core.dxpapi.com/api/v1/core/"
API_ENDPOINT_PROD = "https://core.dxpapi.com/api/v1/core/"
MAX_START_ROW_ALLOWED = 10000; # Bloomreach does not allow "startRow > 10000" in api call.
MTB_API_CALL = 100; # just so that Discovery does not return status 429
MAX_ROWS_IN_SEARCH_RESPONSE = 100 # num_rows in API call

# min ref count we must have for each pid
MIN_REQUIRED_PID_REF_COUNT = 2

SEARCH_APICALL_TEMPLATE="$API_ENDPOINT?account_id=$ACCOUNTID&auth_key=$AUTH_KEY&domain_key=$DOMAIN_KEY&request_id=7057573203767&_br_uid_2=$BR_UID_2&url=www.bloomique.com&ref_url=www.bloomique.com&request_type=search&rows=$MAX_ROWS&start=$START&fl=pid&q=$QUERY&search_type=keyword"

#ACCOUNT_ID = 7529
#AUTH_KEY =  '5lq6iakwrdluhpvq'
#DOMAIN_KEY = 'pacific_apparel'
#API_REALM = 'prod'
#MAX_PIDS_PER_QUERY = 20 # how many pids to collect for each query's response


