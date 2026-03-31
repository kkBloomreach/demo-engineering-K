# Sample feed includes products from some specific catagories. Therefore,
# we first read the list of 'specific categories', then execute brSM category
# api call for each of those and collect products returned in that response.
# For each response we pick up max N products from that result.
# Then we collect all such pids that SHOULD be in sample feed and prepare
# sample feed containing only for those products

import logging
import csv
import catidlist
import execAPICall
import processedFeed

FILENAME_SRC_CATID_LIST='./data/catidlist.tsv'
# smaller feed for debugging
# FILENAME_PROCESSED_PRODUCTFEED = './data/full_preprocessed_08032020_10.tsv'
FILENAME_PROCESSED_PRODUCTFEED = './data/full_preprocessed_08032020.tsv'

FILENAME_SAMPLE_PRODUCTFEED_OUTPUT = './data/sample_product_feed.tsv'
VIEW_DELIMITER = '|'


# original record has view-based price/sale_price. Extract one each
def extractPrices (productRecord):
    origPrice = productRecord ['price']
    origSalePrice = productRecord ['sale_price']
    origPriceValues = origPrice.split (VIEW_DELIMITER)
    origSalePriceValues = origSalePrice.split (VIEW_DELIMITER)
    return (origPriceValues[0], origSalePriceValues [0])


def collectProductRecords (totalPidList):
    logging.debug ("collect product records")
    # list of 'dictionary' objects
    outputProductRecords = []

    for aPid in totalPidList:
        processedProductRecord = processedFeed.getProductRecord (aPid)
        logging.debug ("collect product record: " + aPid)
        if (processedProductRecord != None):
            samplePrice, sampleSalePrice = extractPrices (processedProductRecord)
            # replace all NA_ to "PNB" (for PacificNutsAndBolt)
            crumbs_id_anon = processedProductRecord ['crumbs_id'].replace ("NA_", "PNB")
            # extract required fields from original record
            outputRecord = {
                                'crumbs': processedProductRecord ['crumbs'],
                                'crumbs_id': crumbs_id_anon,
                                'availability': processedProductRecord ['availability'],
                                'price': samplePrice,
                                'url': processedProductRecord ['url'],
                                'pid': processedProductRecord ['pid'],
                                'title': processedProductRecord ['title'],
                                'description': processedProductRecord ['description'],
                                'brand': processedProductRecord ['brand'],
                                'thumb_image': processedProductRecord ['thumb_image'],
                                'sale_price': sampleSalePrice,
                                'manufacturer': processedProductRecord ['Mfr Name'],
                                'catalog_number': processedProductRecord ['Catalog Number'],
                                'master_upc': processedProductRecord ['Master UPC'],
                                'application': processedProductRecord ['Application'],
                                'color': processedProductRecord ['Color'],
                                'height': processedProductRecord ['Height'],
                                'length': processedProductRecord ['Length'],
                                'material': processedProductRecord ['Material'],
                                'size': processedProductRecord ['Size'],
                                'voltage_rating': processedProductRecord ['Voltage Rating'],
                                'weight': processedProductRecord ['Weight'],
                                'width': processedProductRecord ['Width'],
                                'type': processedProductRecord ['Type']
                            }

            outputProductRecords.append (outputRecord)
            

    return outputProductRecords


def collectPids (srcCatIdList):
    totalPidList = []

    for aRow in srcCatIdList:
        srcCatId = aRow ['L2 catId']
        # Note - the column "L2 Category" is actually supposed to be queryTerm
        srcQTerm = aRow ['L2 Category']
        pidListInCat = execAPICall.getPidList (srcQTerm, srcCatId)
        if (pidListInCat != None):
            for aPid in pidListInCat:
                if ((aPid in totalPidList) == False):
                    totalPidList.append (str (aPid))

    logging.info ("Total pid list size : %s", str (len (totalPidList)))
    return totalPidList

def writeOutput (outputRecords):
    outputKeys = outputRecords [0].keys ()
    file_obj = open (FILENAME_SAMPLE_PRODUCTFEED_OUTPUT, 'w')
    file_writer = csv.DictWriter (file_obj, fieldnames = outputKeys, delimiter='\t')

    file_writer.writeheader ()
    file_writer.writerows (outputRecords)
    file_obj.flush ()
    file_obj.close ()


def readCatIdList ():
    # read src catId list
    catidlist.read (FILENAME_SRC_CATID_LIST)
    srcCatIdList = catidlist.getCatIdList ()
    return srcCatIdList


def doInits ():
    logging.basicConfig (level=logging.INFO)


if __name__ == "__main__":
    doInits ()

    # read source catIds
    srcCatIdList = readCatIdList () 
    processedFeed.readProcessedFeed (FILENAME_PROCESSED_PRODUCTFEED)

    # execute API call for each catId
    # collected list of PIDs is saves to pidList file
    totalPidList = collectPids (srcCatIdList)

    if (len (totalPidList) > 0):
        outputRecords = collectProductRecords (totalPidList)
        if (len (outputRecords) > 0):
            writeOutput (outputRecords)
        else:
            logging.warning ("Total sample product feed size is 0")
    else:
        logging.warning ("Total pid list size is 0")

    
