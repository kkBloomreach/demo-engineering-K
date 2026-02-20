import logging
import csv
import random 
import time
import datetime

import convertConstants as cc
import brsmFeedReader as bfr

# assume month is 30 days
SECONDS_IN_MONTH = (30 * 24 * 60 * 60)

class GenerateAttributeValues ():

    def __init__ (self):
        self._brsm_raw_feed = [] 
        self._brsm_pid_records = []
        # generatedValues is list of dict of format:
        # we generate data ONLY for specific attributes. All such attributes are pid-level attributes
        # Some are existing attributes whose values are changed. Some are new attributes that did
        # not exist in original feed
        #{
        #   pid: 12321,
        #   margin: 0.0,
        #   condition: new/used/...
        #   start_date: date string
        #   end_date:   date string
        #   lowstock:   True/False
        #}
        self._attribValueMap = []
        return

    def _readBRSMFeed (self, srcPath):
        logging.info ("Reading brSM feed")
        feedReader = bfr.BRSMFeedReader ()
        self._brsm_raw_feed = feedReader.readBRSMFeed (cc.FILENAME_BRSM_FEED_IN)
        return

    # currently we change attribute values only at pid-level. Therefore,
    # extract product records (excluse variant records)
    def _extractProductRecords (self):
        for aRecord in self._brsm_raw_feed:
            aPid = aRecord ['pid']
            # see if we have already a generatedValue dict; if not, create one
            pidRecord = self._lookupPidRecord (aPid)
            if (pidRecord == None):
                self._brsm_pid_records.append (aRecord)
        logging.info ("Generating attribute values. Product count = %s", len (self._brsm_pid_records))
        return

    def _lookupPidRecord (self, aPid):
        for oneRecord in self._brsm_pid_records:
            if (oneRecord ['pid'] == aPid):
                return oneRecord
        return None

    # generate data and save to local .tsv file
    # It is then used to change / add to original brSM feed
    def _generateAttributeValues (self):
        logging.info ("Generate attribute values")
        for aPidRecord in self._brsm_pid_records:
            # returns attribValueMap record for given pid
            _pidAttribValueMap = self._generateAttributeValuesForPid (aPidRecord)
            self._attribValueMap.append (_pidAttribValueMap)
        return

    def _generateAttributeValuesForPid (self, aPidRecord):
        # margin, 1 decimal
        _margin = (random.random () + 1.0) * 10
        _margin = round (_margin, 1)

        # condition - 10% products are 'used', else 'new'
        # generate a random number. If value is 0 -> 0.1, set that pid to be 'used'
        # we assume radnom-number-generation is evenly spread between 0 to 1
        _bucket = random.random ()
        if (_bucket < 0.1):
            _condition = 'used'
        else:
            _condition = 'new'

        # start_date, set to be wihin the last 36 months
        # actual start_date calculated as: time_now - random * 36
        _launchDelta = random.random () * 36 * SECONDS_IN_MONTH # seconds in a month
        _actualLaunchTime = time.time () - _launchDelta

        # format as described in BR document: YYYYMMDDHHMM
        _gmTimeStruct = time.gmtime (_actualLaunchTime)
        _formatedStartDate = time.strftime ('%Y%m%d%H%M', _gmTimeStruct)

        # end_date, set to be wihin the next 60 months
        # actual end_date calculated as: time_now + 60month - (random * 24)
        _endDelta = random.random () * 24 * SECONDS_IN_MONTH # seconds in a month
        _actualEndTime = time.time () + (60 * SECONDS_IN_MONTH) - _endDelta

        # format as described in BR document: YYYYMMDDHHMM
        _gmTimeStruct = time.gmtime (_actualEndTime)
        _formatedEndDate = time.strftime ('%Y%m%d%H%M', _gmTimeStruct)

        # lowstock = True for ~15% products
        _bucket = random.random ()
        if (_bucket < 0.15):
            _lowstock = 'true'
        else:
            _lowstock = 'false'

        _pidAttribValueMap = {
                                'pid': aPidRecord ['pid'],
                                'margin': float (_margin),
                                'condition': _condition,
                                'start_date': _formatedStartDate,
                                'end_date': _formatedEndDate,
                                'lowstock': _lowstock
                             }
        return _pidAttribValueMap # only the generated attrib values

    def _writeAttributeValueMap (self, savePath):
        logging.info ("Write attribute values")
        with open (savePath, 'w') as file_output:
            csvWriter = csv.writer (file_output, delimiter='\t')

            headerLine = self._attribValueMap [0].keys ()
            csvWriter.writerow (headerLine)

            for row in self._attribValueMap:
                csvWriter.writerow (row.values())
        file_output.close ()


if __name__ == '__main__':
    logging.basicConfig (level=logging.INFO)
    attribValueGenerator = GenerateAttributeValues ()
    attribValueGenerator._readBRSMFeed (cc.FILENAME_BRSM_FEED_IN)
    attribValueGenerator._extractProductRecords ()
    attribValueGenerator._generateAttributeValues ()
    attribValueGenerator._writeAttributeValueMap (cc.FILENAME_PID_ATTRIBUTE_GENERATED_VALUES)


