import logging
import jsonlines

class JsonlWriter ():

    def __init__ (self):
        self._contents = None
        return

    def setContents (self, contents):
        self._contents = contents

    # write to output 
    def write (self, targetPath):
        with open (targetPath, 'w') as outputFile:
            writer = jsonlines.Writer (outputFile)
            for aContent in self._contents:
                writer.write (aContent)

            outputFile.close ()
        return
 
if __name__ == '__main__':
    _feedWriter = jsonlWriter ()
    _feedWriter.setContents (None)
    _feedWriter.write()

