import urllib.request
import logging
import requests
import json
import time

from utils import reindexconsts as rc
from utils import credentials as cr

class IngestAndIndex ():
    _parsedCommandArgs = None

    def __init__ (self, parsedCommandArgs):
        self._parsedCommandArgs = parsedCommandArgs
        return

    def ingest_and_index_catalog (self, feed_file_jsonl, account_config):
        # following if-then-else for debugging
        if (self._parsedCommandArgs != None):
            realm = self._parsedCommandArgs.getRemoteRealm ()
        else:
            realm = 'staging'

        job_id = self._ingestcatalog (realm, feed_file_jsonl, account_config)
        if (job_id == -1):
            logging.error ('Ingest operation failed for: %s' % cr.Credentials.getBloomreachAccountName (account_config))
            return job_id

        job_id = self._indexcatalog (realm, account_config)
        if (job_id == -1):
            logging.error ('Index operation failed for: %s' % cr.Credentials.getBloomreachAccountName (account_config))
            return job_id

        return job_id

    # feed source path is .jsonl file
    def _ingestcatalog (self, realm, feed_file_jsonl, account_config):
        # bloomreach patch API - depending on ingestion of product or content catalog
        #endpoint = cr.Credentials.getBloomreachIngestApiEndpoint (realm, account_config)
        endpoint = cr.Credentials.getBloomreachUploadApiEndpoint_V3 (realm, account_config)
        headers = cr.Credentials.getBloomreachUploadRequestHeader_V3 (realm, account_config) # previously 'upload' was called 'ingetst'
        if (endpoint == None) or (headers == None):
            logging.error ('Could not get upload apiendpoint for %s' % cr.Credentials.getBloomreachAccountName (account_config))
            return -1

        with open(feed_file_jsonl, 'rb') as payload:
            logging.debug ('ingest apiendpoint: %s' % endpoint)
            logging.debug ('ingest headers: %s' % headers)
            response = requests.put (url=endpoint, data=payload, headers=headers)
            response.raise_for_status()
            payload.close ()

        logging.info("Feed API: HTTP PUT: %s", response.url)
        logging.info("Feed Job response: %s", response.json())
        job_id = response.json()["data"]["job_id"]

        # wait for 'success'/'fail' status
        jobStatus = self._checkAPIStatus (job_id, account_config, -1)
        if ((jobStatus == rc.VALID_API_STATUS_FAILED) or (jobStatus == rc.VALID_API_STATUS_KILLED)):
            logging.error ("Ingest API status: %s", jobStatus)
            return -1   # invalid job_id
        return job_id

    def _indexcatalog (self, realm, account_config):
        # bloomreach index API url 
        endpoint = cr.Credentials.getBloomreachIndexApiEndpoint_V3 (realm, account_config)
        headers = cr.Credentials.getBloomreachIndexRequestHeader (realm, account_config)
        if (endpoint == None) or (headers == None):
            logging.error ('Could not get index apiendpoint for %s' % cr.Credentials.getBloomreachAccountName (account_config))
            return -1

        response = requests.post (url=endpoint, headers=headers)
        #response.raise_for_status()
        logging.debug ('index apiendpoint: %s' % endpoint)
        logging.debug ('index headers: %s' % headers)
        
        logging.info("Feed API: Index: %s", response.url)
        logging.info("Index Job response: %s", response.json())
        job_id = response.json()["data"]["job_id"]

        # check status until success OR failure
        jobStatus = self._checkAPIStatus (job_id, account_config, -1)
        if ((jobStatus == rc.VALID_API_STATUS_FAILED) or (jobStatus == rc.VALID_API_STATUS_KILLED)):
            logging.error ("Index API status: %s", jobStatus)
            return -1   # invalid job_id
        return job_id

    # For ingest, we have to wait till ingest is successful. However
    # for index, we check "MAX_TRIES" and break 
    def _checkAPIStatus (self, jobId, account_config, maxTries = -1):
        countTries = 0
        while (True):
            if ((maxTries > 0) and (countTries > maxTries)):
                logging.info ("\tStop checking API status for job: %s", jobId) 
                break
            countTries = countTries + 1
            logging.info ("\tChecking API status for job: %s", jobId) 
            # before making next status check, wait a few seconds
            time.sleep (rc.MIN_TIME_BEFORE_RECHECK_STATUS)
            gotStatus = self._checkAPIStatusOnce (jobId, account_config)
            logging.info ("\t\tAPI Status: %s", gotStatus)
            if (gotStatus != None):
                if ((gotStatus == rc.VALID_API_STATUS_SUCCESS) or (gotStatus == rc.VALID_API_STATUS_FAILED)
                                                                or (gotStatus == rc.VALID_API_STATUS_KILLED)):
                    break

        # value may be 'success' or 'fail'
        return gotStatus

    # check status once - this method is called repeatedly until we get SOME
    # operation status (including 'failed', ...)
    def _checkAPIStatusOnce (self, jobId, account_config):
        jobStatus = None 

        # following if-then-else for debugging
        if (self._parsedCommandArgs != None):
            realm = self._parsedCommandArgs.getRemoteRealm ()
        else:
            realm = 'staging'

        # make the API call
        endPoint = cr.Credentials.getBloomreachApiCheckStatusEndpoint_V3 (realm, account_config, jobId)
        try:
            request = urllib.request.Request (url=endPoint,
                                              headers=cr.Credentials.getBloomreachIndexRequestHeader (realm, account_config),
                                              method='GET')
            resp = urllib.request.urlopen (request)
        except Exception as e:
            logging.error ("Error in making API call: %s", e)
            resp = None

        # process API response
        if (resp != None):
            if (resp.status != 200):
                logging.error ("ERROR status = %s", resp.status)
            else:
                response_payload = resp.read ()
                payload = json.loads (response_payload)
                #jobStatus = payload ['status']
                jobStatus = payload ['data']['job']['status']   # response format change in V3
                logging.debug ("\tCheck APIStatus: " + jobStatus)
                if (jobStatus != rc.VALID_API_STATUS_SUCCESS):
                    # log all fields in API response
                    for key in payload.keys():
                        statusMsg = "status key: %s, value: %s" % (key, payload [key]) 
                        if ((jobStatus == rc.VALID_API_STATUS_FAILED) or (jobStatus == rc.VALID_API_STATUS_KILLED)):
                            # ERROR message
                            logging.error (statusMsg)
                        else:
                            # INFO message
                            logging.debug (statusMsg)
                    
        return jobStatus

if __name__ == '__main__':
    logging.basicConfig (level=logging.DEBUG)
    logging.debug ('start')    
    ingest_and_index = IngestAndIndex (None)
    ingest_and_index.ingest_and_index_catalog (None, None)
    logging.debug ('finish') 

