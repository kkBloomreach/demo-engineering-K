import logging
import json
import csv

import brsmFeed as bf
import dcFeed as dcf
import tsvFeed as tsvf
import convertConstants as cc

def inits ():
    logging.basicConfig (level=logging.INFO)

def loadBRSMFeed ():
    brsmFeedHandler = bf.BRSMFeed ()
    brsmFeedHandler.readBRSMFeed (cc.FILENAME_BRSM_FEED_IN)
    brsmFeedHandler.readPidToBrandMap (cc.FILENAME_PID_BRANDMAP_IN)
    brsmFeedHandler.readPidToGeneratedAttributeValueMap (cc.FILENAME_PID_ATTRIBUTE_GENERATED_VALUES)
    brsmFeedHandler.readPidToDeleteDueToGenimage (cc.FILENAME_PID_TO_DELETE_DUE_TO_GENIMAGE)
    brsmFeedHandler.processBRSMFeed ()
    return brsmFeedHandler

def generateDCFeed ():
    dcFeedGenerator = dcf.DCFeed ()
    dcFeedGenerator.setBRSMFeed (brsmFeedHandler)
    dcFeed, attributeList = dcFeedGenerator.generateDCFeed ()
    return (dcFeed, attributeList)

def generateTSVFeed ():
    tsvFeedGenerator = tsvf.TSVFeed ()
    tsvFeedGenerator.setBRSMFeed (brsmFeedHandler)
    tsvFeed = tsvFeedGenerator.generateTSVFeed ()
    return tsvFeed

def prepareJsonLines (dcProducts):
    jsonLineList = []
    for aDCProduct in dcProducts:
        aProductJsonLine = json.dumps (aDCProduct, default=lambda o:o__dict__)
        logging.debug ('product JsonLine: %s', aProductJsonLine)
        jsonLineList.append (aProductJsonLine)

    return jsonLineList

def writeJsonLinesToFile (jsonLines):
    # jsonLines
    savePath = cc.FILENAME_DATACONNECT_OUT
    with open (savePath, 'w') as file_output:
        for aLine in jsonLines:
            file_output.write ('%s\n' % aLine)
        file_output.close ()

def writeAttributesToFile (attributeList):
    # attributeList
    savePath = cc.FILENAME_ATTRIBUTELIST_OUT
    with open (savePath, 'w') as file_output:
        for attrib in attributeList:
            file_output.write ('%s\n' % attrib)
        file_output.close ()

def writeTSVFeed (tsvRecords):
    savePath = cc.FILENAME_TSV_OUT
    with open (savePath, 'w') as file_output:
        tsvWriter = csv.writer (file_output, delimiter = '\t')

        headerLine = tsvRecords [0].keys ()
        tsvWriter.writerow (headerLine)

        for row in tsvRecords:
            tsvWriter.writerow (row.values())
        file_output.close ()

if __name__ == '__main__':
    inits ()
    brsmFeedHandler = loadBRSMFeed ()
    dcProducts, attributeList = generateDCFeed ()
    tsvRecords = generateTSVFeed ()

    # write dc feed and attributes
    jsonLines = prepareJsonLines (dcProducts)
    writeJsonLinesToFile (jsonLines)
    writeAttributesToFile (attributeList)

    # write tsvFeed
    writeTSVFeed (tsvRecords)

    logging.info ("Finished ...")

'''    
    # for debugging, beautified version
    writeFullJson (dcProducts)
def writeFullJson (dcProducts):
    fullJson = json.dumps (dcProducts, default = lambda o:o.__dict__)
    # jsonLines
    savePath = cc.FILENAME_DATACONNECT_OUT_BEAUTIFIED
    with open (savePath, 'w') as file_output:
        file_output.write ('%s\n' % fullJson)
        file_output.close ()

'''



