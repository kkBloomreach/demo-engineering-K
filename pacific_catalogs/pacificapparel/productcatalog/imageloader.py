import requests
import logging

import updaterConstants as uc

class ImageLoader ():

    def __init__ (self):
        return

    # load image from source catalog. This method is called if we have
    # not yet downloaded a required image to local path
    def load_image (self, imgUrl, localStorePath):
        try:
            response = requests.get (imgUrl, allow_redirects=True)
        except Exception as e:
            logging.error ('Exception in load image: %s' % (e))
            return uc.IMAGE_LOADER_STATUS_FAIL

        if (response.status_code != uc.HTTP_STATUS_OK):
            logging.warn ('Response status not OK: %s' % (response.status_code))
            return uc.IMAGE_LOADER_STATUS_FAIL

        with open (localStorePath, 'wb') as local_file:
            local_file.write (response.content)
            local_file.close ()

        return uc.IMAGE_LOADER_STATUS_SUCCESS


