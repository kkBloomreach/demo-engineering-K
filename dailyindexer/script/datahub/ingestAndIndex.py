# reference:
# https://documentation.bloomreach.com/data-hub/docs/how-to-use-the-api-to-update-product-source-data

import logging
import urllib.request
import requests
import time
import json

from utils import reindexconsts as rc
from utils import credentials as cr

class IngestAndIndex ():
    def __init__ (self, parsedCommandArgs):
        self._parsedCommandArgs = parsedCommandArgs
        return

    def ingest_and_index_catalog (self, feed_file_jsonl, account_config):
        job_id = self._indexcatalog (feed_file_jsonl, account_config)
        if job_id == -1:
            logging.error ('Datahub index operation failed for: %s' % cr.Credentials.getBloomreachAccountName (account_config))
        return job_id

    def _indexcatalog (self, feed_file_jsonl, account_config):
        endpoint = cr.Credentials.getBloomreachDatahubIndexApiEndpoint (account_config)
        headers = cr.Credentials.getBloomreachDatahubIndexRequestHeader (account_config)
        if (endpoint == None) or (headers == None):
            logging.error ('Could not get datahub apiendpoint for %s' % cr.Credentials.getBloomreachAccountName (account_config))
            return -1

        workspace_id = cr.Credentials.getBloomreachDatahubWorkspaceId (account_config)
        collection_name = cr.Credentials.getBloomreachDatahubCollectionName (account_config)
        api_token_name = cr.Credentials.getBloomreachDatahubApiTokenName (account_config)
        api_token_secret = cr.Credentials.getBloomreachDatahubApiTokenSecret (account_config)
        auth = cr.Credentials.getBloomreachDatahubAuth (account_config)
        # need to define this 'params' object in-line
        params = {
                    "update_mode": "full",
                    "on_success_trigger": ["update-items", "update-destination-items"]
        }

        with open(feed_file_jsonl, 'rb') as payload:
            response = requests.post (endpoint, data = payload, headers = headers, auth = auth, params = params)
            response.raise_for_status()

            response_data = response.json()['data']
            job_id = response_data['id']
            logging.debug ("Feed API: HTTP POST: %s", response.url)
            logging.debug ('Datahub index job_id: %s' % job_id)

            payload.close ()

        job_status = self._checkAPIStatus (job_id, account_config, -1)
        if (job_status != rc.VALID_API_STATUS_SUCCESS):
            logging.error ('Datahub index API failed,  status: %s' % job_status)
            return -1

        return job_id

    def _checkAPIStatus (self, job_id, account_config, max_tries = -1):
        count_tries = 0
        while True:
            if (max_tries > 0) and (count_tries > max_tries):
                logging.info ('\tDatahub stop check API status for job: %s' % job_id)
                break
            count_tries = count_tries + 1
            logging.debug ('\tDatahub checking API status for job: %s' % job_id)

            got_status = self._checkAPIStatusOnce (job_id, account_config)
            if got_status != None:
                if  ((got_status == rc.VALID_API_STATUS_SUCCESS) or  \
                     (job_status == rc.VALID_API_STATUS_FAILED) or \
                     (job_status == rc.VALID_API_STATUS_KILLED)):
                    break

            # wait a little and then check api status again
            time.sleep (rc.MIN_TIME_BEFORE_RECHECK_STATUS)

        return got_status # may be success or failed or killed

    # check status once - method called repeatedly
    def _checkAPIStatusOnce (self, job_id, account_config):
        job_status = None

        endPoint = cr.Credentials.getBloomreachDatahubApiCheckStatusEndpoint (account_config, job_id)
        auth = cr.Credentials.getBloomreachDatahubAuth (account_config)
        try:
            resp = requests.get (endPoint, auth = auth)
        except Exception as e:
            logging.error ('Error in Datahub check status API call: %s' % str (e))
            resp = None

        if resp != None:
            if resp.status_code != 200:
                logging.erro ('Datahub API response status: %s' % resp.status)
            else:
                response_content = resp.content
                payload = json.loads (response_content)
                api_status = payload ['success']
                logging.debug ('\tDatahub check APIStatus: %s' % api_status)
                if api_status == True:
                    job_status = rc.VALID_API_STATUS_SUCCESS
                else:
                    job_status = rc.VALID_API_STATUS_FAILED
        else:
            job_status = rc.VALID_API_STATUS_FAILED
        return job_status

if __name__ == '__main__':
    logging.basicConfig (level=logging.DEBUG)
    logging.debug ('start')    
    iai = IngestAndIndex ()
    iai.ingest_and_index_catalog (None, None)
    logging.debug ('finish') 

'''
#    DH_ENDPOINT_PROD = 'https://eu3-api.eng.bloomreach.com/api/cde/v1/workspaces/'
#    # DH_ENDPOINT_PROD = 'https://api.bloomreach.com/api/cde/v1/workspaces/'
#    
#    WORKSPACE_ID = 'ade99688-6e9d-11f0-beb9-024411eb8e7d'      # eg, "90097af2-2a2c-4dad-bf54-eec9c8634f89"
#    API_TOKEN_NAME =  'wgbl9jpup5eybxniyztw7quf5cjxma9v049p7ct0tqyu9p5w3hp73w4oi62mpoqo'  # 
#    API_TOKEN_SECRET =  'it4ha4eqm93atswudc7h6ymdbrctr5qctasthkp3psz4lp2d64clir8s1h74bmcm' 
#    COLLECTION_NAME =  'pacifichome'
#    
#    CATALOG_FILE_JSONL= './0814/ph2_product_en_full_08142025.jsonl'
#    
#    def ingestcatalog ():
#        dhEndPoint = DH_ENDPOINT_PROD
#        account_id = WORKSPACE_ID
#        workspace_id = WORKSPACE_ID
#        collection_name = COLLECTION_NAME
#        api_token_name = API_TOKEN_NAME
#        api_token_secret = API_TOKEN_SECRET
#        headers = {
#            "Content-Type": "application/json-patch+jsonlines"
#        }
#        auth = (API_TOKEN_NAME, API_TOKEN_SECRET)
#        params = {
#            "update_mode": "full",
#            "on_success_trigger": ["update-items", "update-destination-items"]
#        }
#    
#        # bloomreach patch API url
#        #url = '%s/accounts/%s/catalogs/%s/products' % (dhEndPoint, workspace_id, catalog_name)
#        url = '%s%s/item-collections/%s/item-types/product/records' % (dhEndPoint, workspace_id, collection_name)
#    
#        print ('url = ' + url)
#    
#        with open(CATALOG_FILE_JSONL, 'rb') as payload:
#            response = requests.post (url, data=payload, headers=headers, auth = auth, params = params)
#            response.raise_for_status()
#    
#            logging.info("Feed API: HTTP PUT: %s", response.url)
#            logging.info("Feed Job response: %s", response.json())
#            # job_id = response.json()[data['id']]
#            response_data = response.json()['data']
#            job_id = response_data['id']
#            print ('job_id: %s' % job_id)
#    
#            payload.close ()
#    
'''
