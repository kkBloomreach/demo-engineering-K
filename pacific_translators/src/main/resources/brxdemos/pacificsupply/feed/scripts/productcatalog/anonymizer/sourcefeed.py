# utilities for sourceFeed files
import csv
import logging

# array of dictionaries
sourceFeed_products = []

def getSourceProducts ():
    return sourceFeed_products


def read_tsv (filename, dataArray):
    logging.info ("INFO reading source feed: " + filename)
    file_obj = open (filename, 'r')
    dict_reader = csv.DictReader (file_obj, delimiter='\t')

    for row in dict_reader:
       dataArray.append (row)
    logging.info ("INFO total product record count in source feed = " + str (len (dataArray)))


def read_inputs (sourceFeedFile):
    # products
    read_tsv (sourceFeedFile, sourceFeed_products)


if __name__ == '__main__':
    fname = './data/source_feed_NOBOM.tsv'
    read_inputs (fname)

