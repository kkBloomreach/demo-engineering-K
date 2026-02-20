# xml feed reader

import xml.etree.ElementTree as ET
import logging
import csv

import convertConstants as cc

class BRSMFeedReader ():

    def __init__ (self):
        self._brsm_raw_feed = []    # records from input

    def readBRSMFeed (self, srcPath):
        logging.info ("Reading brSM feed")
        # read source xml. Adds records to brsm_raw_feed list
        self._read_xml (srcPath)
        return (self._brsm_raw_feed)


    # This method reads each-and-every field from the source feed
    # and saves each as a dictionary and appends to brsm_raw_feed dictionary list
    # read xml file (brSM feed is in xml format)
    # xml feed format:
    # <?xml...
    #  <feed>
    #   <products>
    #     <product>
    #        ...
    #     </product>
    #     ...
    #   </products>
    # </feed>
    def _read_xml (self, filename):
        feedXML = ET.parse (filename)
        feedRoot = feedXML.getroot ()
        productList = feedRoot.iter ('product')
        for aProduct in productList:
            aBRSMProduct = {}
            for aField in aProduct.iter ():
                aBRSMProduct [aField.tag] = aField.text
            self._brsm_raw_feed.append (aBRSMProduct)

        logging.info ('BRSM product + variants count: %s', len (self._brsm_raw_feed))
        return

