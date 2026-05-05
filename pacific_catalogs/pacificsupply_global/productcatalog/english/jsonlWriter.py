import logging
import jsonlines

class JsonlWriter ():

    def __init__ (self):
        self._products = None
        return

    def setProducts (self, products):
        self._products = products

    # write to output 
    def write (self, targetPath):
        with open (targetPath, 'w') as outputFile:
            writer = jsonlines.Writer (outputFile)
            for aProduct in self._products:
                writer.write (aProduct)

            outputFile.close ()
        return
 
if __name__ == '__main__':
    _feedWriter = jsonlWriter ()
    _feedWriter.setUpdatedProducts (None)
    _feedWriter.write()

