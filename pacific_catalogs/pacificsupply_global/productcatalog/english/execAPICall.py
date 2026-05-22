import logging
import csv
import json
import requests

import revisionConstants as rc

class ExecAPICall ():
    def __init__ (self):
        return

    # exec API call and return pidList
    # currently returns 16 pids max for given catId
    # Reason for chosing '16' is to have total pid's in sample feed ~300
    def getPidList (self, qTerm, catId):
        pidList = None
        responseJson = self._execCommand (qTerm, catId)
        if (responseJson != None):
            pidList = self._extractPidListFromResponse (responseJson)
        return (pidList)
 
    def _execCommand (self, qTerm, catId):
	    responseJson = None
	    try:
	        brsmAPIParams = rc.BRSM_CATAPI_PARAMS.copy ()
	
	        # add specific catId to get response for
	        brsmAPIParams ['q'] = qTerm 
	        brsmAPIParams ['fq'] = 'category:"' + catId + '"'
	
	        response = requests.get ( rc.BRSM_CATAPI_ENDPOINT,
	                                  params = brsmAPIParams)
	        logging.debug ("response status: " + str(response.status_code))
	        if (response.status_code == 200):
	            responseJson = response.json ()
	    except Exception as e:
	        logging.error ("Exception in API call: " + str(e))
	
	    if (responseJson != None):
	        logging.info ("response catId: " + catId + ", numFound = " + str (responseJson ['response']['numFound']))
	    return (responseJson)

    def _extractPidListFromResponse (self, responseJson):
        pidList = []
        responseDocs = responseJson ['response']['docs']
        for aDoc in responseDocs:
            #logging.debug ("pid = " + aDoc ['pid'])
            pidList.append (aDoc ['pid'])

        return pidList

if __name__ == "__main__":
    logging.basicConfig (level=logging.DEBUG)
    executor = ExecAPICall ()
    pidList = executor.getPidList ('NA_160402010000')
    logging.debug ("pidList length: " + str (len (pidList)))


'''
BRSM_CATAPI_ENDPOINT ='http://core.dxpapi.com/api/v1/core/' 
BRSM_CATAPI_PARAMS = {
                        'account_id': '6370',
                        'auth_key': '1vjobidilg5gcbpn',
                        'domain_key': 'pacific_supply',
                        'request_id': '427715043276',
                        'url': 'www.bloomique.com',
                        'ref_url': 'www.bloomique.com',
                        'request_type': 'search',
                        'rows': '16',
                        'start': '0',
                        'fl': 'pid',
                        'search_type': 'keyword'
                     }
'''
