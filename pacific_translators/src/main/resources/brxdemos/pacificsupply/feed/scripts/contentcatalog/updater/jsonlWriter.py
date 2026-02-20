import logging
import jsonlines

class JsonlWriter ():

    def __init__ (self):
        self._records = None
        return

    def setRecords (self, records):
        self._records = records

    # write to output 
    def write (self, targetPath):
        with open (targetPath, 'w') as outputFile:
            writer = jsonlines.Writer (outputFile)
            for aProduct in self._records:
                writer.write (aProduct)

            outputFile.close ()
        return
 
if __name__ == '__main__':
    _feedWriter = jsonlWriter ()
    _feedWriter.setRecords (None)
    _feedWriter.write()

