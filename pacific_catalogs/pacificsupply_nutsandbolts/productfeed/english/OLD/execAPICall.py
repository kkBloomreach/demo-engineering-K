import logging
import csv
import json
import subprocess
import requests

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


# exec API call and return pidList
# currently returns 16 pids max for given catId
# Reason for chosing '16' is to have total pid's in sample feed ~300
def getPidList (qTerm, catId):
    pidList = None
    responseJson = _execCommand (qTerm, catId)
    if (responseJson != None):
        pidList = _extractPidListFromResponse (responseJson)

    return (pidList)


def _execCommand (qTerm, catId):

    responseJson = None
    try:
        brsmAPIParams = BRSM_CATAPI_PARAMS.copy ()

        # add specific catId to get response for
        brsmAPIParams ['q'] = qTerm 
        brsmAPIParams ['fq'] = 'category:"' + catId + '"'

        response = requests.get ( BRSM_CATAPI_ENDPOINT,
                                  params = brsmAPIParams)
        logging.debug ("response status: " + str(response.status_code))
        if (response.status_code == 200):
            responseJson = response.json ()
    except Exception as e:
        logging.error ("Exception in API call: " + str(e))

    if (responseJson != None):
        logging.info ("response catId: " + catId + ", numFound = " + str (responseJson ['response']['numFound']))

    return (responseJson)


def _extractPidListFromResponse (responseJson):
    pidList = []
    responseDocs = responseJson ['response']['docs']
    for aDoc in responseDocs:
        logging.debug ("pid = " + aDoc ['pid'])
        pidList.append (aDoc ['pid'])

    return pidList


if __name__ == "__main__":
    logging.basicConfig (level=logging.DEBUG)
    pidList = getPidList ('NA_160402010000')
    logging.debug ("pidList length: " + str (len (pidList)))



# ===========
# BRSM_CATAPI_PREAMBLE = \
# 'http://core.dxpapi.com/api/v1/core/?account_id=6370&auth_key=1vjobidilg5gcbpn&domain_key=pacific_supply&request_id=427715043276&_br_uid_2=uid%3D4202137991960%3Av%3D11.8%3Ats%3D1581627952476%3Ahc%3D1359&url=www.bloomique.com&ref_url=www.bloomique.com&request_type=search&rows=30&start=0&fl=pid&search_type=category'
# CURL_CMD = 'curl'
# CURL_OPTION = '-o'
# FILENAME_API_RESPONSE = './data/apiresponse.json'

# returns true if exec successful. JSON output in 'response' file
# def execCommand_curl (catId):
#     apiCallArgs = []
#     apiCallArgs.append (CURL_CMD)
#     apiCallArgs.append (CURL_OPTION + FILENAME_API_RESPONSE)
#     apiCallArgs.append (BRSM_CATAPI_PREAMBLE + '&q=' + catId)
# 
#     cmdStr = ' '.join (apiCallArgs)
#     logging.debug ("curl cmd: " + cmdStr)
# 
#     try:
#         subprocess.run (apiCallArgs)
#     except SubprocessError:
#         logging.error ("curl command exec failed")
#         return False
# 
#     return True

