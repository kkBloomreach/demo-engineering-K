# anonymizer
# v3.02 - changed product url pattern to "pacific-supply/products/pid___pid"
# v3.01 - changed product url pattern to ".../products/pid"
import sys
import getopt
import os.path
import csv
import sourcefeed
import string
import random
import cattransform as ct
import makeimagefilename
import logging

FILENAME_ANONYMIZED_FEED = 'output.tsv'
FILENAME_SOURCE_FEED_ACCEPTED = 'source_feed_accepted.tsv'
FILENAME_CAT_NAMECHANGES = 'cat_transformation.tsv'
FILENAME_SOURCE_FEED = 'source_feed_NOBOM.tsv'

CRUMBS_DELIMITER = '|'
CRUMBS_ID_DELIMITER = '|'
VIEW_ID_DELIMITER = '|'
MASTER_VIEW_ID = 'master'
SALE_PRICE_FACTOR = 0.63

PRODUCT_URL_PREFIX = "https://pacific-supply.bloomreach.com/products/"
IMAGE_URL_PREFIX = "https://pacific-demo-data.bloomreach.cloud/supply/images/"

# This offset must be the same as the one used in 'makeimagefilename' module
FIXED_OFFSET_FOR_PID = 0x1abc

# list of keys in source file to be deleted
# Reason: anonymization, avoid confusion, ...
SOURCE_KEYS_TO_DELETE = ['Product ID',
                         'View_Id',
                         'Web Full Image Path',
                         'PrimaryImage_Data_Sheets',
                         'PrimaryImage_Installation_Manuals',
                         'PrimaryImage_Category_Image',
                         'PrimaryImage_Branch_Images',
                         'PrimaryImage_180px',
                         'PrimaryImage_500px',
                         'PrimaryImage_120px',
                         'PrimaryImage_1500px',
                         'PrimaryImage_80px',
                         'PrimaryImage',
                         'PrimaryImage_PDF_With_ID',
                         'PrimaryImage_JPG_With_ID',
                         'Image_Installation_Manuals',
                         'Image_Category_Image',
                         'Image_Branch_Images',
                         'Image_180px',
                         'Image_500px',
                         'Image_120px',
                         'Image_1500px',
                         'Image_80px',
                         'Product ShortDescription',
                         'Web Short Desc',
                         'Brand Name',
                         'PDW ID',
                         'UNSPSC Code',
                         'Datasheet_Data_Sheets',
                         'Customer Part Number',
                         '-']

# Special words (eg, vendor names) to be removed from output
SPECIAL_STRINGS_TO_REPLACE = [{'original': 'OneSource', 'replaced': 'PacificSupply'}]

# ViewId map - view_ids in source are mapped to corresponding name in the output
# Note that, besides this map, ALL numeric view_id values (eg, 29366) are deleted as well 
VIEW_ID_NAME_MAP = [{
                        "origName": "CommercialContractor",
                        "changedName": "ContractorCommercial",
                        "include": True
                    },
                    {
                        "origName": "ElectricalMRO",
                        "changedName": "MaintenanceElectrical",
                        "include": True
                    },
                    {
                        "origName": "IndustrialContractor",
                        "changedName": "ContractorIndustrial",
                        "include": True
                    },
                    {
                        "origName": "IndustrialMRO",
                        "changedName": "MaintenanceIndustrial",
                        "include": True
                    },
                    {
                        "origName": "MachiningFabrication",
                        "changedName": "Fabrication",
                        "include": True
                    },
                    {
                        "origName": "OEM",
                        "changedName": "OEM",
                        "include": True
                    },
                    {
                        "origName": "ResidentialContractor",
                        "changedName": "ContractorResidential",
                        "include": True
                    },
                    {
                        "origName": "ResidentialContractor",
                        "changedName": "ContractorResidential",
                        "include": True
                    },
                    {
                        "origName": "SQDOEM",
                        "changedName": "-",
                        "include": False 
                    },
                    {
                        "origName": "SolarContractor",
                        "changedName": "-",
                        "include": False 
                    },
                    {
                        "origName": "Utility",
                        "changedName": "Utility",
                        "include": True 
                    },
                    {
                        "origName": "master",
                        "changedName": "master",
                        "include": True 
                    }
                   ]
 
# This is for analysis of which views are actually in the feed
VIEW_IDS_IN_OUTPUT = []

# This is to write 'accepted' source feed records. This data
# is then used to download only the necessary image files from original hosts
# list of dictionaries, each is 'row' in source feed
# NOTE that the entries in this array are ORIGINAL source records, not the ones meant for pacificSupply
source_products_accepted = []


# image url
# Only the host is changed. "path" remains same
def generateImageUrl (srcRow):

    targetImageFileName = makeimagefilename.makeTargetImageFileName (srcRow)
    newImageUrl = IMAGE_URL_PREFIX + targetImageFileName

    logging.debug ("DEBUG new image url: " + newImageUrl)
    return newImageUrl


# newPid is used to generate productURL
# On the pacific-supply site (https://pacific-supply.bloomreach.com), the PDP
# url is of the form: https://pacific-supply.bloomreach.com/products/14808___14808
def generateProductUrl (srcUrl, newPid):
    newUrl = PRODUCT_URL_PREFIX + newPid + "___" + newPid
    return newUrl


# given source crumb name, return a tuple 
# 0: changed_crumb_name, 1: crumbs_id
# "srcCrumb" is of the format: Control & Automation|Sensors & Switches
# Few notes re: current taxonomy
# - apparently a product belongs to only one category tree. (there is only value-seperator. NO parent-value-separator)
# - that is, there is no product that belongs to multiple SEPARATE categories
def transformCrumb (srcCrumb, srcCrumbId):
    logging.debug ("DEBUG transforming src crumb: " + srcCrumb)

    # If srcCrumb has moved, apply that 
    if (ct.isCrumbCopied (srcCrumb) == True):
        # "copy" operation -- return a tuple with incoming crumb and crumb_id
        # 0: crumb, 1: crumbs_id
        processedCrumbAndId = (srcCrumb, srcCrumbId)
    elif (ct.isCrumbMoved (srcCrumb) == True):
        processedCrumbAndId = ct.applyMove (srcCrumb, srcCrumbId)
        logging.debug ("DEBUG transforming src crumb after move: " + processedCrumbAndId[0] + ", crumbId: " + processedCrumbAndId[1])
    elif (ct.isCrumbRenamed (srcCrumb) == True):
        processedCrumbAndId = ct.applyRename (srcCrumb, srcCrumbId)
        logging.debug ("DEBUG transforming src crumb after rename: " + processedCrumbAndId[0] + ", crumbId: " + processedCrumbAndId[1])
    else:
        logging.error ("Error: Unknown operation for crumb: " + srcCrumb)
        processedCrumbAndId = (srcCrumb, srcCrumbId)

    # return a tuple
    return processedCrumbAndId

# Generate as many prices as the number of views
# returns a dictionary with two values 
#   - concatenated-string of all the price values
#   - concatenated-string of all the sale price values
def generateViewPrices (srcPrice, viewIdList):
    logging.debug ("DEBUG source price: " + srcPrice)
    priceList = []
    salePriceList = []
    for aView in viewIdList:
        # generate a randomFactor in range {0.5,  1.5}srcPrice
        randomFactor = random.random ()
        if (randomFactor < 0.5):
            randomFactor = 1 + randomFactor
        aPrice = (float(srcPrice) * randomFactor)
        aSalePrice = aPrice * SALE_PRICE_FACTOR
        priceList.append (f"{aPrice:.2f}")
        salePriceList.append (f"{aSalePrice:.2f}")

    generatedPriceList = VIEW_ID_DELIMITER.join (priceList)
    generatedSalePriceList = VIEW_ID_DELIMITER.join (salePriceList)
    logging.debug ("DEBUG generated prices: " + generatedPriceList)
    return ({'view_list_price': generatedPriceList, 
             'view_list_sale_price': generatedSalePriceList})

# src value is expected to be in the format: number|number|...
def updateCustomerPartNumber (srcPartNumber, viewIdList):
    customerPartsList = srcPartNumber.split (VIEW_ID_DELIMITER)
    updatedCustomerPartsList = []

    if (len (customerPartsList) < len (viewIdList)):
        logging.error ("Error: customer part numbers are too few")

    for count in range (len (viewIdList)):
        updatedCustomerPartsList.append (customerPartsList [count])
   
    updatedCustomerPartNumber = VIEW_ID_DELIMITER.join (updatedCustomerPartsList)
    return (updatedCustomerPartNumber) 

# look up 'changed' view_id name
# Returns None if specified view_id is to be deleted
def lookupChangedViewIdName (srcViewId):
    global VIEW_ID_NAME_MAP

    for aMap in VIEW_ID_NAME_MAP:
        if (aMap ['origName'] == srcViewId):
            if (aMap ['include'] == True):
                return aMap ['changedName']
            else:
                return None

    # if srcViewId not in the map, return it as-is
    # Actually that should not happen
    logging.error ("ERROR viewId: " + srcViewId + " not in view_id_name_map")
    return srcViewId


# splits srcView_id string and returns list of view_id values
# Removes numeric view_id's from source
# Also adds "master" in viewIds if it is not there already
# Incoming param is like: 'master|OEM|IndustrialContractor'
def cleanupViewIds (srcViewIds):
    srcViewList = srcViewIds.split (VIEW_ID_DELIMITER)
    cleanedUpViewList = []
    for aView in srcViewList:
        if (aView.isnumeric () == True):
            continue

        # Use view_id_map defined above to map original-name to new-name
        # Some original view_ids (besides numerical) are deleted as well
        changedName = lookupChangedViewIdName (aView)
        if (changedName is not None):
            cleanedUpViewList.append (changedName)

            # for analysis purpose, collect ALL views for all products in a combined list
            if ((changedName in VIEW_IDS_IN_OUTPUT) == False):
                VIEW_IDS_IN_OUTPUT.append (changedName)

    # if "master" is not in viewList, add it
    if ((MASTER_VIEW_ID in cleanedUpViewList) == False):
        cleanedUpViewList.append (MASTER_VIEW_ID)

    return (cleanedUpViewList)


def generateUniqPID (srcPid):
    newPid = int (srcPid) + FIXED_OFFSET_FOR_PID
    return (str (newPid))


# returns 'row_ext' (row-extended-with-additional-attribs)
def processValidRecord (srcRow):

    # duplicate original source feed row and update that
    row_ext = srcRow.copy ()

    # add brSM required fields in the row dictionary
    newPid = generateUniqPID (srcRow ['Product ID'])
    row_ext ['pid'] = newPid

    row_ext ['title'] = srcRow ['Product ShortDescription']
    row_ext ['description'] = srcRow ['Web Short Desc']
    row_ext ['brand'] = srcRow ['Brand Name']

    # lookup changed crumb_name and generate crumbs_id using the changed_name
    # "transform" method returns a tuple. 0: changed_crumb_name, 1: crumb_id
    # Values are full crumb and crumbId (eg, A|B|C and Aid|Bid|Cid)
    crumbTransform = transformCrumb (srcRow ['crumbs'], srcRow ['crumbs_id'])
    row_ext ['crumbs'] = crumbTransform [0]
    row_ext ['crumbs_id'] = crumbTransform [1]

    # generate product URL
    productUrl = generateProductUrl (srcRow ['url'], newPid)
    row_ext ['url'] = productUrl

    # generate image URL
    thumb_imageUrl = generateImageUrl (srcRow)
    row_ext ['thumb_image'] = thumb_imageUrl

    # view_id list
    cleanedUpViewList = cleanupViewIds (srcRow ['View_Id'])
    logging.debug ("DEBUG cleanedUpViewList: ")
    logging.debug (cleanedUpViewList)

    # make sure there is at least one view for each product
    if (len (cleanedUpViewList) == 0):
        cleanedUpViewList.append (MASTER_VIEW_ID)
        view_id_string = MASTER_VIEW_ID
    else:
        # concat all elements in view_list
        view_id_string = VIEW_ID_DELIMITER.join (cleanedUpViewList)

    logging.debug ("DEBUG view_id string: " + view_id_string)
    row_ext ['view_id'] = view_id_string
    # also remove original "View_Id" to avoid confusion

    #### Update view-specific attributes (price, customernumber, ...)
 
    # generate price values based on view_id
    priceAndSalePriceList= generateViewPrices (srcRow ['price'], cleanedUpViewList);
    row_ext ['price'] = priceAndSalePriceList ['view_list_price']
    row_ext ['sale_price'] = priceAndSalePriceList ['view_list_sale_price']

    # update customer part number
    srcCustomerPartNumber = srcRow ['Customer Part Number']
    if ((srcCustomerPartNumber is not None) and (len (srcCustomerPartNumber) > 0)):
        customer_part_number = updateCustomerPartNumber (srcRow ['Customer Part Number'], cleanedUpViewList)
    else:
        customer_part_number = ""
    row_ext ['customerpartnumber'] = customer_part_number

    return row_ext


def doAnonymize ():

    # We have to do a two-pass approach for anonymization
    # Since a crumb 'move' may possibly refer to a renamed-crumb
    # and the 'rename' occurs in the feed AFTER the move-record is encountered
    # In such case, we don't yet have corresponding 'move-to' crumbsId for that move
    # Therefore, first pass just updates the internal crumb<->crumbsId map, applies 
    # all operations except "move" then second pass does the move-to steps

    sourceProducts = sourcefeed.getSourceProducts ()
    outputProducts = []

    # first pass
    logging.info ("INFO start anonymization, first pass...")

    for row in sourceProducts:

        srcCrumb = row ['crumbs']
        # Ignore all products that have Crumb = <blank>
        # Also, if ANY element in srcCrumb is 'deleted' remove the entire
        # crumb. Consequently, since a product belongs to only one category,
        # that product itself can be deleted from the catalog
        if ((ct.isCrumbNameValid (srcCrumb) == True) and (ct.isCrumbDeleted (srcCrumb) == False)):

            # save this source record in 'accepted-source' list
            source_products_accepted.append (row)

            # in first pass, skip all "move" operations
            if (ct.isCrumbMoved (srcCrumb) == False):
                outputRecord = processValidRecord (row)
                outputProducts.append (outputRecord)
                continue
        else:
            logging.debug ("DEBUG Crumb is blank or deleted; skipping")
            continue

    # second pass
    logging.info ("INFO start anonymization, second pass...")
    for row in sourceProducts:

        srcCrumb = row ['crumbs']
        # Ignore all products that have Crumb = <blank>
        # Also, if ANY element in srcCrumb is 'deleted' remove the entire
        # crumb. Consequently, since a product belongs to only one category,
        # that product itself can be deleted from the catalog
        if ((ct.isCrumbNameValid (srcCrumb) == True) and (ct.isCrumbDeleted (srcCrumb) == False)):
            # in second pass, perform only "move" operations
            if (ct.isCrumbMoved (srcCrumb) == True):
                outputRecord = processValidRecord (row)
                outputProducts.append (outputRecord)
                continue
        else:
            continue

    logging.info ("INFO finish anonymization...")
    return outputProducts


# before writing output, remove keys that should not be in the output
def doFinalCleanup (outputProducts):
    for outputRec in outputProducts:
        for aKey in SOURCE_KEYS_TO_DELETE:
            outputRec.pop (aKey, None)

    # go thru each record and replace/remove 'special names'
    # this can be slow...
    outputKeys = outputProducts [0].keys ()
    for outputRec in outputProducts:
        for aKey in outputKeys:
            fieldValue = outputRec [aKey]
            if (len (fieldValue) > 0):
                fieldValueLower = fieldValue.lower ()
                for specialName in SPECIAL_STRINGS_TO_REPLACE:
                    if (fieldValueLower.find (specialName['original'].lower()) >= 0):
                        newFieldValue = fieldValue.replace (specialName['original'], specialName['replaced'])
                        outputRec [aKey] = newFieldValue


def writeOutput (outputProducts, outputDirPath):

    outputFilePath = os.path.join (outputDirPath, FILENAME_ANONYMIZED_FEED)
    logging.info ("INFO writing output to: " + outputFilePath)

    # write header line
    outputKeys = outputProducts [0].keys ()

    file_obj = open (outputFilePath, 'w')
    file_writer = csv.DictWriter (file_obj, fieldnames = outputKeys, delimiter='\t')

    file_writer.writeheader ()
    file_writer.writerows (outputProducts)
    file_obj.flush ()
    file_obj.close ()

def writeAcceptedSource (acceptedSourceProducts, outputDirPath):

    acceptedSourceFeedFilePath = os.path.join (outputDirPath, FILENAME_SOURCE_FEED_ACCEPTED)

    # write header line
    outputKeys = acceptedSourceProducts [0].keys ()

    file_obj = open (acceptedSourceFeedFilePath, 'w')
    file_writer = csv.DictWriter (file_obj, fieldnames = outputKeys, delimiter='\t')

    file_writer.writeheader ()
    file_writer.writerows (acceptedSourceProducts)
    file_obj.flush ()
    file_obj.close ()


def doInits ():
    # various inits
    logging.basicConfig (level=logging.INFO)
    return


def readInputs (sourceDirPath):

    # read cat transformation data
    catnameChangesFilePath = os.path.join (sourceDirPath, FILENAME_CAT_NAMECHANGES)
    ct.read_inputs (catnameChangesFilePath)

    # read source feed 
    sourceFeedFilePath = os.path.join (sourceDirPath, FILENAME_SOURCE_FEED)
    sourcefeed.read_inputs (sourceFeedFilePath)


# expected args: -d <dataDirpath>
def readCommandArg (cmdArgs):

    dataDirPath = None

    try:
        opts, extraArgs = getopt.getopt (cmdArgs, "d:")
    except getopt.GetoptError:
        return None

    for oneOpt, oneOptVal in opts:
        if (oneOpt == '-d'):
            dataDirPath = oneOptVal

    return dataDirPath 


def main ():
    doInits ()

    # read dataDir path from command line
    dataDirPath = readCommandArg (sys.argv[1:])
    if (dataDirPath == None):
        logging.error ("ERROR. Specify source directory in command line: -d <dirpath>")
        sys.exit (2)

    # read source feed, cat transformation, ...
    sourceDirPath = dataDirPath + "/source"
    logging.debug ("DEBUG source dir path ..." + sourceDirPath)
    readInputs (sourceDirPath)

    # anonymize
    outputProducts = doAnonymize ()

    # during debugging, it is possible that outputProducts are zero
    if (len (outputProducts) > 0):
        logging.info ("INFO writing output...")

        # remove unwanted keys, replace special strings, ...
        doFinalCleanup (outputProducts)

        # write output
        outputDirPath = dataDirPath + "/output"
        writeOutput (outputProducts, outputDirPath) 

        # write accepted source feed. Needed to then list the
        # necessary image files from the original host
        writeAcceptedSource (source_products_accepted, outputDirPath)

        # for analysis, write list of ALL views in output
        logging.info ("INFO view ids in output: ")
        for aView in VIEW_IDS_IN_OUTPUT:
            logging.info ('\t' + aView)

    logging.info ("INFO translation complete...")

if __name__ == '__main__':
    main ()


