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
        # 'update: full' always goes ahead and does discovery-side re-index as well
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
                     (got_status == rc.VALID_API_STATUS_FAILED) or \
                     (got_status == rc.VALID_API_STATUS_KILLED)):
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

