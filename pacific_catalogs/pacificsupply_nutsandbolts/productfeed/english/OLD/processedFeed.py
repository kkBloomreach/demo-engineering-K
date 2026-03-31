# This python script uses the 'output.tsv' feed already generated via anonymize.py script
import csv
import logging

# array of dictionaries for products
sourceFeed_products = []

def getSourceProducts ():
    return sourceFeed_products

# returns the entire product record from processed feed
def getProductRecord (pid):
    for aRow in sourceFeed_products:
        if (aRow ['pid'] == pid):
            return (aRow)

    return None


def readProcessedFeed (filepath):
    file_obj = open (filepath, 'r')
    dict_reader = csv.DictReader (file_obj, delimiter='\t')

    for row in dict_reader:
        sourceFeed_products.append (row)

    file_obj.close ()
    logging.info ("total product record count = " + str (len (sourceFeed_products)))


def doTest ():
    # products
    logging.basicConfig (level=logging.DEBUG)
    readProcessedFeed ('./data/full_preprocessed_08032020_10.tsv')
    productRecord = getProductRecord ('1012686')
    if (productRecord != None):
        logging.debug ("pid in record: " + productRecord ['pid']) 
    else:
        logging.error ("pid not found in processed feed")


def main ():
    doTest ()


if __name__ == '__main__':
    main ()

