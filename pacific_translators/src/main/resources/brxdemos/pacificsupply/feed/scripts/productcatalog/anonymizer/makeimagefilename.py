# this module is imported in anonymize as well as imagedownload 
# so that both of them finally land up using consistent naming convention
from urllib.parse import urlparse

PREFIX_LOCAL_IMAGE_FILE_NAME = "thumbimage_pid_"
# This offset MUST be the same as the one defined in anonymize module
FIXED_OFFSET_FOR_PID = 0x1abc

# given a record in source feed, use its values and
# return local image file path
def makeTargetImageFileName (srcRow):

    imageURL = srcRow ['Web Full Image Path']
    pid = srcRow ['Product ID']
    newPidValue = int (pid) + FIXED_OFFSET_FOR_PID
    ext = ''

    urlParseOutput = urlparse (imageURL)
    if (urlParseOutput is not None):
        urlPath = urlParseOutput.path
        # extract extenstion if any (eg, .jpg)
        extIndx = urlPath.rfind ('.')
        if (extIndx >= 0):
            ext = urlPath [extIndx:]

    targetFileName = PREFIX_LOCAL_IMAGE_FILE_NAME + str (newPidValue) + ext
    return targetFileName


def performTest ():
    dummyRow = ({'Web Full Image Path': 'http://www.legrand.us/mediaitem/Home/Wiremold/Raceway/Steel-Raceway/Multi-Channel-Raceway/4000-Large-Raceway/G4017TCA-Internal-Corner-Coupling/042630C35AB54892939CAD9D1F9E2FD3.jpg',
                 'Product ID': 1234
                })
    imageFileName = makeTargetImageFileName (dummyRow)
    print (imageFileName)


def main ():
    performTest ()

if __name__ == '__main__':
    main ()

