import logging
import sys
import time
import requests
import json
import urllib.parse

import evalConstants as ec

class EvalProductCounts ():
    def __init__ (self):
        return

    def set_training_config (self, training_config):
        self._training_config = training_config
        return

    def collect_query_response_count (self, query):
        pid_list = []
        response_numfound = 0

        response_obj = self._get_search_response (query)
        if (response_obj != None):
            logging.debug ('num found for query = %s: %s' % (query, response_obj ['response']['numFound']))
            response_numfound = response_obj ['response']['numFound']
            docs = response_obj ['response']['docs']
            pid_list = None
            for j in range (min (self._training_config ['training_parameters']['max_pids_per_query'], len (docs))):
                if pid_list == None:
                    pid_list = docs [j]['pid']
                else:
                    pid_list = '%s,%s' % (pid_list, docs [j]['pid'])    # delimiter = ',' to avoid tsv-conflict
        else:
            logging.debug ('responseObj is null')
        return (response_numfound, pid_list)

    def _get_search_response (self, search_term):
        api_url = self._construct_api_call (search_term)
        response = requests.get (api_url)
        if (response.status_code == 200):
            response_obj = response.json ()
        else:
            logging.error ('Response status not OK: %s' % response.status_code)
            response_obj = None
        return response_obj

    def _construct_api_call (self, search_term, start = 0):
        apiCall = ec.SEARCH_APICALL_TEMPLATE
        if self._training_config ['training_parameters']['realm'] == 'staging':
            apiCall = apiCall.replace ("$API_ENDPOINT", ec.API_ENDPOINT_STAGING)
        else:
            apiCall = apiCall.replace ("$API_ENDPOINT", ec.API_ENDPOINT_PROD)

        apiCall = apiCall.replace ("$ACCOUNTID", self._training_config ['training_parameters']['account_id'])
        apiCall = apiCall.replace ("$AUTH_KEY",  self._training_config ['training_parameters']['auth_key'])
        apiCall = apiCall.replace ("$DOMAIN_KEY", self._training_config ['training_parameters']['domain_key'])
        apiCall = apiCall.replace ("$QUERY", str (urllib.parse.quote (search_term)))
        apiCall = apiCall.replace ("$START", str (start))
        apiCall = apiCall.replace ("$MAX_ROWS", str (self._training_config ['training_parameters']['max_pids_per_query']))
       
        cookieStr = self._generate_cookie_string ()
        apiCall = apiCall.replace ("BR_UID_2", cookieStr)

        logging.debug ("QueryExecutor APICall: " + apiCall)
        return (apiCall)

    # for the purpose of generating campaign products, we don't currently use 'segment'
    # since that essentially changes ranking, but not recall set
    def _generate_cookie_string (self) :
        epoch_time = int (time.time () * 1000)
        cookieStr = "uid=" + ec.DEFAULT_UID + ":v=17.0:ts=" + str (epoch_time) + ":hc=1"
        return cookieStr

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    logging.info ('Start get numFound...')
    evaluator = EvalProductCounts ()
    logging.info ('Finish get numFound...')

'''
    def set_refined_queries (self, refined_queries):
        self._refined_queries = refined_queries
        return 
'''
