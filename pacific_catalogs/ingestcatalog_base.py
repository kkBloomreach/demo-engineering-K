import logging
import polling
import requests

DC_ENDPOINT = 'https://discovery.bloomreach.com/dataconnect/api/v3'

class IngestCatalogBase ():

    def __init__ (self, realm, account_name, catalog_name, access_key, catalog_filename):
        self._realm = realm
        self._account_name = account_name
        self._catalog_name = catalog_name
        self._access_key = access_key
        self._catalog_filename = catalog_filename
        self._dcEndPoint = DC_ENDPOINT
        return

    def ingest (self):
        print ('in base ingest')
        if self._realm == None or  self._account_name == None or self._catalog_name == None or self._access_key == None or self._catalog_filename == None:
            print ('base ingest error 0')
            logging.error ('One or more required parameters undefined')
            return

        logging.info ('Begin ingest, %s\n' % self._account_name)
        headers = {
            "Content-Type": "application/json-patch+jsonlines",
            "Authorization": "Bearer " + self._access_key 
        }

        # bloomreach patch API url
        url = '%s/accounts/%s/catalogs/%s/environments/%s/records' % (self._dcEndPoint, self._account_name, self._catalog_name, self._realm)
        logging.debug ('url = ' + url)

        print ('catalog file: %s' % self._catalog_filename)
        with open(self._catalog_filename, 'rb') as payload:
            response = requests.put (url, data=payload, headers=headers)
            response.raise_for_status()

            logging.info("Feed API: HTTP PUT: %s", response.url)
            logging.info("Feed Job response: %s", response.json())
            job_id = response.json()["data"]["job_id"]
            print ('job_id: %s' % job_id)
            payload.close ()

        logging.info ('Finish ingest, %s\n' % self._account_name)
        return

if __name__ == '__main__':
    logging.basicConfig (level=logging.DEBUG)
    logging.debug ('start')    
    ingestcatalog ()
    logging.debug ('finish') 

