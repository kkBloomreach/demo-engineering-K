# write .tsv and .csv format

import logging
import csv

class TabularFeedWriter ():

    def __init__ (self):
        self._tabularRecords = None
        return

    def setTabularRecords (self, tabularRecords):
        self._tabularRecords = tabularRecords

    def writeTSVFeed (self, targetPath):
        with open (targetPath, 'w') as file_output:
            tsvWriter = csv.writer (file_output, delimiter = '\t')

            headerLine = self._tabularRecords[0].keys ()
            tsvWriter.writerow (headerLine)

            for row in self._tabularRecords:
                tsvWriter.writerow (row.values())
            file_output.close ()

    def writeCSVFeed (self, targetPath):
        with open (targetPath, 'w') as file_output:
            tsvWriter = csv.writer (file_output)

            headerLine = self._tabularRecords[0].keys ()
            tsvWriter.writerow (headerLine)

            for row in self._tabularRecords:
                tsvWriter.writerow (row.values())
            file_output.close ()

