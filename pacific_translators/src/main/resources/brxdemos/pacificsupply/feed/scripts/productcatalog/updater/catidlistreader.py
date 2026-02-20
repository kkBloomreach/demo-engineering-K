import logging
import csv

class CatIdListReader ():
    def __init__ (self):
        # list of dictionary. keyNames same as in source file
        # L1Cat, L2Cat, L2 catid 
        self._catId_list = []
        return

    def read (self, filepath):
        file_obj = open (filepath, 'r')
        dict_reader = csv.DictReader (file_obj, delimiter='\t')

        for row in dict_reader:
            catId = row ['L2 catId']
            if ((catId != "") and (catId != '?')):
                self._catId_list.append (row)
                logging.debug ("srcCatId: %s", catId)
        return self._catId_list


if __name__ == "__main__":
    logging.basicConfig (level=logging.DEBUG)
    cilr = CatIdListReader ()
    cilr.read ("./data/input/catidlist.tsv")

