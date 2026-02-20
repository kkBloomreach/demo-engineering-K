import logging
import getopt

class CommandLine ():

    def __init__ (self):
        self._logging_level = logging.INFO
        self._log_path = None
        self._remote_realm = 'staging'  #default
        self._data_dir = None
        self._acct_id = None
        self._catalog_name = None

    # -h -g -o <output> -l<logpath> -r<realm> -m<maxproducts>
    # realm: staging or prod
    def parseCommandLine (self, argv):
        try:
            opts, args = getopt.getopt (argv, 'hgl:r:d:a:c:')
        except getopt.GetOptError as err:
            print (err)
            return False

        for opt, arg in opts:
            if (opt == '-g'):
                self._logging_level = logging.DEBUG
            elif (opt == '-l'):
                self._log_path = arg
            elif (opt == '-r'):
                self._remote_realm = arg
            elif (opt == '-d'):
                self._data_dir = arg
            elif (opt == '-h'):
                print ('Usage: -d <data_dir> -s <sourcepath> [-h -l <logpath> -g]')
                break
            elif (opt == '-a'): # used to send acctId from driver to subprocess
                self._acct_id = arg
            elif (opt == '-c'): # used to send catalog name from driver to subprocess
                self._catalog_name = arg


        # check absolutely required commandline args
        if (self._data_dir == None):
            print ('FATAL: Required command line parameters not provided')
            print ('Usage: -d <data_dir> [-l <logpath> -g -h]')
            return False
        return True

    def getLogLevel (self):
        return self._logging_level

    # convenience method 
    def isDebugMode (self):
        if (self._logging_level == logging.DEBUG):
            return True
        return False

    def getLogPath (self):
        return self._log_path

    def getRemoteRealm (self):
        return self._remote_realm

    def getDataDir (self):
        return self._data_dir

    # method called from subprocess
    def getAccountId (self):
        return self._acct_id

    def getCatalogName (self):
        return self._catalog_name


