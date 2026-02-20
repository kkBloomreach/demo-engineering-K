# utilities for sourceFeed files
import csv
import makeimagefilename
from os import path

# source feed 
# FILENAME_SOURCE_FEED_ACCEPTED = './data/source_feed_accepted_10.tsv'
FILENAME_SOURCE_FEED_ACCEPTED = './data/source_feed_accepted.tsv'

# read source .tsv containing orig -> changed names
# NOTE: We process ONLY the accepted source feed. Many records from the
# original feed are rejected due to missing crumb and/or deleted thereafter
def read_tsv (filename):
    file_obj = open (filename, 'r')
    dict_reader = csv.DictReader (file_obj, delimiter='\t')

    for row in dict_reader:
        targetImageFileName = makeimagefilename.makeTargetImageFileName (row)
        imageURL = row ['Web Full Image Path']

        targetPath = './images/' + targetImageFileName
        if (path.exists (targetPath) == False):
            # use -L to follow redirects if any
            curlCmd = "sleep 1; curl --max-time 120 -L -o './images/" + targetImageFileName + "' " + str (imageURL)
            print (curlCmd) 

def read_inputs ():
    read_tsv (FILENAME_SOURCE_FEED_ACCEPTED)

def main ():
    read_inputs ()

if __name__ == '__main__':
    main ()
