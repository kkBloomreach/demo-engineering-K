import json
import logging

class JsonlWriterLocal ():

    def __init__ (self):
        return

    def setProducts (self, products):
        self._products = products
        return

    def write (self, targetPath):
        with open (targetPath, 'w') as targetFile:
            for product in self._products:
                jsonline = json.dumps (product, default=lambda o:o.__dict__)
                #logging.debug (jsonline)
                targetFile.write (jsonline)
                targetFile.write ('\n')
            targetFile.flush ()
            targetFile.close ()

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    jwl = jsonlWriterLocal ()
    product = {
                "a": 123,
                "b": "sadada",
                "c": {
                        "r": True,
                        "u": 'uuuuu'
                     }
              }
    products = []
    products.append (product)
    jwl.setProducts (products)
    jwl.write ('./temp.jsonl')
    logging.info ('Finish...')

