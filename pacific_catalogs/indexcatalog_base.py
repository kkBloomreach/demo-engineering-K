import logging
import polling
import requests

DC_ENDPOINT = 'https://discovery.bloomreach.com/dataconnect/api/v3'

class IndexCatalogBase ():

    def __init__ (self, realm, account_name, catalog_name, access_key):
        self._realm = realm
        self._account_name = account_name
        self._catalog_name = catalog_name
        self._access_key = access_key
        self._dcEndPoint = DC_ENDPOINT
        return

    def index (self):
        print ('in base index')
        if self._realm == None or  self._account_name == None or self._catalog_name == None or self._access_key == None == None:
            print ('base index error 0')
            logging.error ('One or more required parameters undefined')
            return

        logging.info ('Begin index, %s\n' % self._account_name)
        headers = {
            "Authorization": "Bearer " + self._access_key 
        }

        # bloomreach patch API url
        url = '%s/accounts/%s/catalogs/%s/environments/%s/indexes' % (self._dcEndPoint, self._account_name, self._catalog_name, self._realm)
        logging.debug ('url = ' + url)

        response = requests.post (url, headers=headers)
        response.raise_for_status()

        logging.info("Feed API: HTTP PUT: %s", response.url)
        logging.info("Feed Job response: %s", response.json())
        job_id = response.json()["data"]["job_id"]
        print ('job_id: %s' % job_id)

        logging.info ('Finish index, %s\n' % self._account_name)
        return

if __name__ == '__main__':
    logging.basicConfig (level=logging.DEBUG)
    logging.debug ('start')    
    indexcatalog ()
    logging.debug ('finish') 

