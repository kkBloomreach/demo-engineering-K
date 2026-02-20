# given a search term, return the list of all products resulting from that term
import logging
import requests
import urllib.parse
import time

import revisionConstantsV15 as rcv15

class SearchResponseCollector:
    def __init__ (self):
        logging.debug ('Search Response Collector')
        return

    def collect_search_response (self, search_term):
        response_pids = []
        num_found = 0
        start = 0
        total_collected = -1 

        # get num_found
        while total_collected < num_found:
            response_obj = self._get_search_response (search_term, start)
            if (response_obj == None):
                logging.error ('Null response for search term: %s' % search_term)
                break

            if start == 0:
                logging.debug ('num found for query = %s: %s' % (search_term, response_obj ['response']['numFound']))
                num_found = response_obj ['response']['numFound']
                total_collected = 0

            logging.debug ('start at: %s, num_found: %s, total_collected: %s' % (start, num_found, total_collected))
            response_docs = response_obj ['response']['docs']
            for doc in response_docs:
                response_pids.append (doc ['pid'])
            total_collected = total_collected + len (response_obj ['response']['docs'])
            start = start + len (response_obj ['response']['docs'])

            # wait to avoid status 429
            time.sleep (rcv15.MTB_API_CALL)
        return response_pids

    def _get_search_response (self, search_term, start) :
        api_url = self._construct_api_call (search_term, start)
        response = requests.get (api_url)
        if (response.status_code == 200):
            response_obj = response.json ()
        else:
            logging.error ('Response status not OK: %s' % response.status_code)
            response_obj = None
        return response_obj

    def _construct_api_call (self, search_term, start):
        apiCall = rcv15.SEARCH_APICALL_TEMPLATE
        apiCall = apiCall.replace ("$QUERY", str (urllib.parse.quote (search_term)))
        apiCall = apiCall.replace ("$START", str (start))

        logging.debug ("APICall: %s" % apiCall)
        return (apiCall)

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    logging.info ('Start eval...')
    total_pids = []

    collector = SearchResponseCollector ()
    terms_list = ['dinnerware', 'bowls', 'bath towels', 'throws', 'blankets']
    for term in terms_list:
        response_pids = collector.collect_search_response (term)
        logging.debug ('query: %s, pids count: %s' % (term, len (response_pids)))
        for pid in response_pids:
            if pid not in total_pids:
                total_pids.append (pid)

    logging.debug ('Total pid count for all search terms: %s' % len (total_pids))
    logging.info ('Finish eval...')


