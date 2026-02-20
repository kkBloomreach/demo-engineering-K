import json
import logging
import time
import os

class Credentials ():

    INDEX_MODE_DATAHUB = 'datahub'
    INDEX_MODE_DATACONNECT = 'dataconnect'

    _accountConfigs = None

    def __init__ (self):
        logging.debug ("Credentials")
        return

    @staticmethod
    def loadAccountConfigs (config_path):
        with open (config_path) as f:
            Credentials._accountConfigs = json.load (f)
        return True
       
    # returns list of accountConfigs 
    @staticmethod
    def getAccountConfigList ():
       return (Credentials._accountConfigs ['accounts']) 

    @staticmethod
    # mode: dataconnect OR datahub. By default, dataconnect for backward compatibility
    def getBloomreachIndexMode (accountConfig):
        if 'INDEX_MODE' in accountConfig:
            return accountConfig ['INDEX_MODE']
        return 'dataconnect'

    @staticmethod
    def getBloomreachAccountName (accountConfig):
        return (accountConfig ['BR_ACCOUNT_NAME']);

    @staticmethod
    def getBloomreachAccountNumber (accountConfig):
        return (accountConfig ['BR_ACCOUNT_NUMBER']);

    @staticmethod
    def getBloomreachCatalogName (accountConfig):
        return (accountConfig ['BR_CATALOG_NAME']);

    @staticmethod
    def getBloomreachIngestRequestHeader (realm, accountConfig):
        if (realm == 'staging'):
            bearer = "Bearer %s" % (accountConfig ['BR_API_AUTHORIZATION_KEY_STAGING'])
        elif (realm == 'production'):
            bearer = "Bearer %s" % (accountConfig ['BR_API_AUTHORIZATION_KEY_PROD'])
        else:
            return None

        request_header = { 'Content-Type': 'application/json-patch+jsonlines',
                           'Authorization': bearer }
        return request_header

    @staticmethod
    def getBloomreachUploadRequestHeader_V3 (realm, accountConfig):
        if (realm == 'staging'):
            bearer = "Bearer %s" % (accountConfig ['BR_API_AUTHORIZATION_KEY_STAGING'])
        elif (realm == 'production'):
            bearer = "Bearer %s" % (accountConfig ['BR_API_AUTHORIZATION_KEY_PROD'])
        else:
            return None

        request_header = { 'Content-Type': 'application/json-patch+jsonlines',
                           'Authorization': bearer }
        return request_header
    @staticmethod
    def getBloomreachIngestApiEndpoint (realm, accountConfig):
        brIngestEndpoint = None
        catalogType = accountConfig ['CATALOG_TYPE']
        if (realm == 'staging'):
            if (catalogType == 'product'):
                brIngestEndpoint = Credentials._accountConfigs ['BR_INGEST_API_ENDPOINT_STAGING']
            elif (catalogType == 'content'):
                brIngestEndpoint = Credentials._accountConfigs ['BR_CONTENT_INGEST_API_ENDPOINT_STAGING']
        else:
            if (catalogType == 'product'):
                brIngestEndpoint = Credentials._accountConfigs ['BR_INGEST_API_ENDPOINT_PROD']
            elif (catalogType == 'content'):
                brIngestEndpoint = Credentials._accountConfigs ['BR_CONTENT_INGEST_API_ENDPOINT_PROD']

        if (brIngestEndpoint != None):
            brAccountNum = Credentials.getBloomreachAccountNumber (accountConfig)
            catalog = Credentials.getBloomreachCatalogName (accountConfig)
            brIngestEndpoint = brIngestEndpoint % (brAccountNum, catalog)

        return brIngestEndpoint

    @staticmethod
    def getBloomreachUploadApiEndpoint_V3 (realm, accountConfig):
        brUploadEndpoint = Credentials._accountConfigs ['BR_UPLOAD_API_ENDPOINT_V3']

        if (brUploadEndpoint != None):
            brAccountName = Credentials.getBloomreachAccountName (accountConfig)
            catalogName = Credentials.getBloomreachCatalogName (accountConfig)
            brUploadEndpoint = brUploadEndpoint % (brAccountName, catalogName, realm)

        return brUploadEndpoint

    @staticmethod
    def getBloomreachApiCheckStatusEndpoint (realm, jobId):
        brCheckStatusEndpoint = None
        if (realm == 'staging'):
            brCheckStatusEndpoint = Credentials._accountConfigs ['BR_CHECKSTATUS_API_ENDPOINT_STAGING']
        else:
            brCheckStatusEndpoint = Credentials._accountConfigs ['BR_CHECKSTATUS_API_ENDPOINT_PROD']

        if (brCheckStatusEndpoint != None):
            brCheckStatusEndpoint = brCheckStatusEndpoint % (jobId)

        return brCheckStatusEndpoint

    @staticmethod
    def getBloomreachApiCheckStatusEndpoint_V3 (realm, accountConfig, jobId):
        brCheckStatusEndpoint = Credentials._accountConfigs ['BR_CHECKSTATUS_API_ENDPOINT_V3']

        if (brCheckStatusEndpoint != None):
            brAccountName = Credentials.getBloomreachAccountName (accountConfig)
            catalogName = Credentials.getBloomreachCatalogName (accountConfig)
            brCheckStatusEndpoint = brCheckStatusEndpoint % (brAccountName, catalogName, realm, jobId)

        return brCheckStatusEndpoint

    @staticmethod
    def getBloomreachIndexRequestHeader (realm, accountConfig):
        if (realm == 'staging'):
            bearer = "Bearer %s" % (accountConfig ['BR_API_AUTHORIZATION_KEY_STAGING'])
        elif (realm == 'production'):
            bearer = "Bearer %s" % (accountConfig ['BR_API_AUTHORIZATION_KEY_PROD'])
        else:
            return None

        request_header = { 'Authorization': bearer }
        return request_header

    @staticmethod
    def getBloomreachIndexApiEndpoint (realm, accountConfig):
        brIndexEndpoint = None
        if (realm == 'staging'):
            brIndexEndpoint = Credentials._accountConfigs ['BR_INDEX_API_ENDPOINT_STAGING']
        elif (realm == 'production'):
            brIndexEndpoint = Credentials._accountConfigs ['BR_INDEX_API_ENDPOINT_PROD']
        if (brIndexEndpoint != None):
            brAccountNum = Credentials.getBloomreachAccountNumber (accountConfig)
            catalog = Credentials.getBloomreachCatalogName (accountConfig)
            brIndexEndpoint = brIndexEndpoint % (brAccountNum, catalog)

        return brIndexEndpoint 

    @staticmethod
    def getBloomreachIndexApiEndpoint_V3 (realm, accountConfig):
        brIndexEndpoint = Credentials._accountConfigs ['BR_INDEX_API_ENDPOINT_V3']
        if (brIndexEndpoint != None):
            brAccountName  = Credentials.getBloomreachAccountName (accountConfig)
            catalogName = Credentials.getBloomreachCatalogName (accountConfig)
            brIndexEndpoint = brIndexEndpoint % (brAccountName, catalogName, realm)

        return brIndexEndpoint 

    # ----- Following added to support datahub reindex process
    @staticmethod
    def getBloomreachDatahubWorkspaceId (accountConfig):
        return (accountConfig ['WORKSPACE_ID'])

    def getBloomreachDatahubCollectionName (accountConfig):
        return (accountConfig ['COLLECTION_NAME'])

    def getBloomreachDatahubApiTokenName (accountConfig):
        return (accountConfig ['API_TOKEN_NAME'])

    def getBloomreachDatahubApiTokenSecret (accountConfig):
        return (accountConfig ['API_TOKEN_SECRET'])

    @staticmethod
    # realm ignored here. It is configured via datahub dashboard as 'destination'
    def getBloomreachDatahubIndexApiEndpoint (accountConfig):
        brDatahubIndexEndpoint = Credentials._accountConfigs ['BR_DATAHUB_INDEX_API_ENDPOINT']
        if (brDatahubIndexEndpoint != None):
            brWorkspaceId = Credentials.getBloomreachDatahubWorkspaceId (accountConfig)
            collectionName = Credentials.getBloomreachDatahubCollectionName (accountConfig)
            brDatahubIndexEndpoint = brDatahubIndexEndpoint % (brWorkspaceId, collectionName)
        return brDatahubIndexEndpoint 

    @staticmethod
    def getBloomreachDatahubIndexRequestHeader (accountConfig):
        request_header = { "Content-Type": "application/json-patch+jsonlines"}
        return request_header

    @staticmethod
    # "auth" is parameter in python request call
    def getBloomreachDatahubAuth (accountConfig):
        auth = None
        apiTokenName = Credentials.getBloomreachDatahubApiTokenName (accountConfig)
        apiTokenSecret = Credentials.getBloomreachDatahubApiTokenSecret (accountConfig)
        if apiTokenName != None and apiTokenSecret != None:
            auth = (apiTokenName, apiTokenSecret)
        return auth

    @staticmethod
    # currently, 'params' is always the same = 'update: full'
    # also, always go ahead and do discovery-side re-index as well
    def getBloomreachDatahubIndexParams_UNUSED (accountConfig):
        params = {
                    "update_mode": "full",
                    "on_success_trigger": [ "update_items", "update_destination_items"]
                 }
        return params

    @staticmethod
    def getBloomreachDatahubApiCheckStatusEndpoint (accountConfig, jobId):
        brDatahubCheckStatusEndpoint = None
        brDatahubCheckStatusEndpoint = Credentials._accountConfigs ['BR_DATAHUB_CHECKSTATUS_API_ENDPOINT_PROD']
        if (brDatahubCheckStatusEndpoint != None):
            brWorkspaceId = Credentials.getBloomreachDatahubWorkspaceId (accountConfig)
            brDatahubCheckStatusEndpoint = brDatahubCheckStatusEndpoint % (brWorkspaceId, jobId)
        return brDatahubCheckStatusEndpoint

if __name__ == "__main__":
    logging.basicConfig (level=logging.DEBUG)
    Credentials.loadAccountConfigs ('../data/input/account_configs.json')
    accountConfigList = Credentials.getAccountConfigList ()
    accountConfig = accountConfigList [0]

    index_mode = Credentials.getBloomreachIndexMode (accountConfig)
    if index_mode == Credentials.INDEX_MODE_DATAHUB:
        logging.debug ('index mode: %s', 'datahub')
    elif index_mode == Credentials.INDEX_MODE_DATACONNECT:
        logging.debug ('index mode: %s', 'dataconnect')

    if index_mode == Credentials.INDEX_MODE_DATACONNECT:
        val = Credentials.getBloomreachRequestHeader (None, 'staging')
        logging.debug ("val: %s", val)

        val = Credentials.getBloomreachIngestApiEndpoint ('staging')
        logging.debug ("val: %s", val)

        val = Credentials.getBloomreachApiCheckStatusEndpoint ('staging', 'JJJJJJIDII')
        logging.debug ("val: %s", val)

        val = Credentials.getBloomreachIndexApiEndpoint ('staging')
        logging.debug ("val: %s", val)

    elif index_mode == Credentials.INDEX_MODE_DATAHUB:
        val = Credentials.getBloomreachDatahubWorkspaceId (accountConfig)
        logging.debug ("val: %s", val)

        val = Credentials.getBloomreachDatahubCollectionName (accountConfig)
        logging.debug ("val: %s", val)

        val = Credentials.getBloomreachDatahubApiTokenName (accountConfig)
        logging.debug ("val: %s", val)

        val = Credentials.getBloomreachDatahubApiTokenSecret (accountConfig)
        logging.debug ("val: %s", val)

        val = Credentials.getBloomreachDatahubIndexApiEndpoint (accountConfig)
        logging.debug ("val: %s", val)

        val = Credentials.getBloomreachDatahubIndexRequestHeader (accountConfig)
        logging.debug ("val: %s", val)

        val = Credentials.getBloomreachDatahubAuth (accountConfig)
        logging.debug ("val: %s", val)

        val = Credentials.getBloomreachDatahubIndexParams (accountConfig)
        logging.debug ("val: %s", val)
    else:
        logging.error ('Unknown index mode: %s', index_mode)


