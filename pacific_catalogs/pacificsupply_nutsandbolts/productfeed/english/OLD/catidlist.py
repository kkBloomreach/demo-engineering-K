import logging
import csv

# list of dictionary. keyNames same as in source file
# L1Cat, L2Cat, L1 catid, L2 catid 
SRC_CATID_LIST = []


# entire row is returned, not just the "l2 catid" values
def getCatIdList ():
    return SRC_CATID_LIST


def read (filepath):
    file_obj = open (filepath, 'r')
    dict_reader = csv.DictReader (file_obj, delimiter='\t')

    for row in dict_reader:
        catId = row ['L2 catId']
        if ((catId != "") and (catId != '?')):
            SRC_CATID_LIST.append (row)
            logging.debug ("DEBUG: srcCatId: " + catId)


def doTest ():
    read ("./data/catidlist.tsv")


if __name__ == "__main__":
    logging.basicConfig (level=logging.DEBUG)
    doTest ()

