import csv
import logging
import translateConsts as tc

class CategoryInfo ():

    def __init__ (self):
        self._categoriesInfo = []
        self.NEXT_UNDEFINED_CRUMB_ID = tc.DEFAULT_BREADCRUMBID
        return

    # Originally, categories and product info was provided separately 
    # to Bloomreach. Read the categories.tsv file and then use it to prepare
    # BR-style xml with crumb, crumbs_id values 
    # This method reads each-and-every field from the source feed
    # and saves each as a dictionary and appends to brsm_raw_feed dictionary list
    def readCategoriesInfo (self, filename):
        logging.info ("INFO reading source category info : " + filename)

        file_obj = open (filename, 'r')
        dict_reader = csv.DictReader (file_obj, delimiter='\t')

        # row = {'category_id', 'name', 'parent_category_id', 'breadcrumb', 'sequence', 'url', 'primary_category'
        # Of these, we use only category_id (aka leaf_id), 'name' (aka leaf_name), 'breadcrumb' (aka full_crumb_id)
        # full_crumb_id is of the form '111/222/333'
        # Internally we add another 'full_crumb' that has corresponding names (eg, 'L0/L1/L2')
        for row in dict_reader:
            infoDict = {
                'leaf_id': row ['category_id'],
                'leaf_name': row ['name'],
                'full_crumbIds_orig': row ['breadcrumb'],   # uses '/' as delimiter
                'full_crumbs': ''
            }
            self._categoriesInfo.append (infoDict);

        logging.info ("INFO total category record count = " + str (len (self._categoriesInfo)))

        ##### go thru all categoryInfo dicts and fill up full_crumbs
        self._buildFullCrumbs ()
        return

    # product record has fullCrumb (eg, 'L0>L1>...'). Return corresponding
    # fullCrumbIds (eg, '/111/122/...' as 111>122>...). 
    # Note that fullCrumb value, delimiter in categoryInfo is created to match what 
    # is expected in the product record
    def getFullCrumbIds (self, fullCrumbs):
        for oneInfo in self._categoriesInfo:
            if (oneInfo ['full_crumbs'] == fullCrumbs):
                fullCrumbIdsMod = oneInfo ['full_crumbIds_orig'][1:] # skip leading '/'
                fullCrumbIdsMod = fullCrumbIdsMod.replace (tc.CRUMBID_DELIMITER_IN_CATEGORYINFO, tc.BREADCRUMBID_VALUE_DELIMITER)
                return fullCrumbIdsMod    # returned value format: 100>200>300

        # coming here means a crumb in product record does not have corresponding
        # info in categoryInfo. Generate one
        # This method retuns crumbId in categoryInfo format (delimiter = /)
        fullCrumbIdsOrig = self._generateFakeFullCrumbsId (fullCrumbs)
        fullCrumbIdsMod = fullCrumbIdsOrig [1:] # skip leading '/'
        fullCrumbIdsMod = fullCrumbIdsMod.replace (tc.CRUMBID_DELIMITER_IN_CATEGORYINFO, tc.BREADCRUMBID_VALUE_DELIMITER)
        return fullCrumbIdsMod

    def _buildFullCrumbs (self):
        for oneInfo in self._categoriesInfo:
            fullCrumbIds = oneInfo ['full_crumbIds_orig']    # format: '/111/222/...'
            fullCrumbIds = fullCrumbIds [1:]    # skip leading '/'
            crumbIdList = fullCrumbIds.split (tc.CRUMBID_DELIMITER_IN_CATEGORYINFO) 

            fullCrumb = None
            for oneId in crumbIdList:
                leafCrumb = self._lookupLeafCrumb (oneId)
                # NOTE: fullCrumb string is created in same form as the one in product record (eg, L0>L1>L2) 
                if (fullCrumb == None):
                    fullCrumb = leafCrumb
                else:
                    fullCrumb = fullCrumb + tc.BREADCRUMB_VALUE_DELIMITER  + leafCrumb

            # update categoryInfo dict for this row
            oneInfo ['full_crumbs'] = fullCrumb
        return

    # given crumbId, return corresponding crumb
    def _lookupLeafCrumb (self, leafCrumbId):
        for oneInfo in self._categoriesInfo:
            if (oneInfo ['leaf_id'] == leafCrumbId):
                return oneInfo ['leaf_name']

        logging.error ('ERROR No crumb for crumbId: %s', leafCrumbId)
        return ('crumb_' + leafCrumbId)

    # fullCrumbs has format: 'L0>L1>L2...'
    # Creates a fakeDict entry for corresponding crumbs and
    # returns fakeFullCrumbsId, delimiter = '/'
    def _generateFakeFullCrumbsId (self, fullCrumbs):
        fakePartialCrumbsId_orig = None
        partialCrumbs = None    # Needed to create fakeDict in categories info list

        crumbLeaves = fullCrumbs.split (tc.BREADCRUMB_VALUE_DELIMITER)
        for aCrumb in crumbLeaves:
            if (partialCrumbs == None):
                partialCrumbs = aCrumb
            else:
                partialCrumbs = partialCrumbs + tc.BREADCRUMB_VALUE_DELIMITER + aCrumb

            partialCrumbsId_orig = self._lookupExistingFullCrumbId (partialCrumbs)

            if (partialCrumbsId_orig != None):
                fakePartialCrumbsId_orig = partialCrumbsId_orig # contains leading '/'
            else:
                # generate fake crumbId and also enter in categoryInfo list for future use
                aCrumbId = str (self.NEXT_UNDEFINED_CRUMB_ID)
                logging.warn ('WARNING Assigned crumbId %s for leaf crumb: %s', aCrumbId, aCrumb)
                self.NEXT_UNDEFINED_CRUMB_ID = self.NEXT_UNDEFINED_CRUMB_ID + 1
                if (fakePartialCrumbsId_orig == None):
                    fakePartialCrumbsId_orig = tc.CRUMBID_DELIMITER_IN_CATEGORYINFO + aCrumbId
                else:
                    fakePartialCrumbsId_orig = fakePartialCrumbsId_orig + tc.CRUMBID_DELIMITER_IN_CATEGORYINFO + aCrumbId

                # prepare corresponding crumbId in productrecord format (delimiter = >)
                fakeDict = {
                    'leaf_id': aCrumbId,
                    'leaf_name': aCrumb,
                    'full_crumbIds_orig': fakePartialCrumbsId_orig,
                    'full_crumbs': partialCrumbs 
                }
                # enter this fake row in categoryInfo for future similar leafName
                self._categoriesInfo.append (fakeDict);

        return fakePartialCrumbsId_orig  # format /1/2/3 (delimiter similar to categoryInfo entries)

    # given partialCrumbs (eg, 'L0>L1'), return corresponding crumbId (original delimiter, '/')
    def _lookupExistingFullCrumbId (self, partialCrumbs):
        for oneInfo in self._categoriesInfo:
            if (oneInfo ['full_crumbs'] == partialCrumbs):
                return oneInfo ['full_crumbIds_orig']

        return None # no such existing fullCrumbId

'''
##################

    # fullCrumbs has format: 'L0>L1>L2...'
    # Creates a fakeDict entry for corresponding crumbs and
    # returns fakeFullCrumbsId, delimiter = same as the one used in product record (ie, '>')
    def _generateFakeFullCrumbsId_OLD (self, fullCrumbs):
        fakeFullCrumbsId = None
        partialCrumbs = None    # Needed to create fakeDict in categories info list

        crumbLeaves = fullCrumbs.split (tc.BREADCRUMB_VALUE_DELIMITER)
        for aCrumb in crumbLeaves:
            aCrumbId = self._lookupExistingLeafCrumbId (aCrumb)

            if (aCrumbId == None):
                # generate fake crumbId and also enter in categoryInfo list for future use
                logging.error ('ERROR No crumbId for crumb: %s', aCrumb)
                aCrumbId = str (self.NEXT_UNDEFINED_CRUMB_ID)
                self.NEXT_UNDEFINED_CRUMB_ID = self.NEXT_UNDEFINED_CRUMB_ID + 1
                fakeDict = {
                    'leaf_id': aCrumbId,
                    'leaf_name': aCrumb,
                    'full_crumbIds_orig': '',
                    'full_crumbIds_mod': '', 
                    'full_crumbs': ''
                }
                # enter this fake row in categoryInfo for future similar leafName
                self._categoriesInfo.append (fakeDict);
            else:
                fakeDict = None

            if (fakeFullCrumbsId == None):
                fakeFullCrumbsId = '/' + aCrumbId
                partialCrumbs = aCrumb
            else:
                fakeFullCrumbsId = fakeFullCrumbsId + tc.CRUMBID_DELIMITER_IN_CATEGORYINFO + aCrumbId
                partialCrumbs = partialCrumbs + tc.BREADCRUMB_VALUE_DELIMITER + aCrumb
 
            # update fakeDict->full_xxx values
            if (fakeDict != None):
                fakeFullCrumbsIdMod = fakeFullCrumbsId [1:]
                fakeFullCrumbsIdMod = fakeFullCrumbsIdMod.replace ('/', '>')
                fakeDict ['full_crumbIds_orig'] = fakeFullCrumbsId
                fakeDict ['full_crumbIds_Mod'] = fakeFullCrumbsIdMod
                fakeDict ['full_crumb'] = partialCrumbs

        fakeFullCrumbsIdMod = fakeFullCrumbsId [1:]
        fakeFullCrumbsIdMod = fakeFullCrumbsIdMod.replace ('/', '>')
        return fakeFullCrumbsIdMod  # format 1>2>3 (delimiter similar to product breadcrumb)

    # given crumb, return corresponding crumbId
    def _lookupExistingLeafCrumbId (self, leafCrumb):
        for oneInfo in self._categoriesInfo:
            if (oneInfo ['leaf_name'] == leafCrumb):
                return oneInfo ['leaf_id']

        return None # no such existing leafCrumb

    def getFullCrumbIds_SAVE (self, fullCrumbs):
        fakeFullCrumbsId = None
        fullCrumbsBranchList = fullCrumbs.split (tc.BREADCRUMB_PARENTVALUE_DELIMITER_IN) # split per '|'
        for aCrumbsBranch in fullCrumbsBranchList:
            aCrumbsIdBranch = None
            branchLeaves = aCrumbsBranch.split (tc.BREADCRUMB_VALUE_DELIMITER) # split per '>'
            for aLeaf in branchLeaves:
                aLeafId = self._lookupLeafCrumbId (aLeaf)
                if (aCrumbsIdBranch == None):
                    aCrumbsIdBranch = aLeafId
                else:
                    aCrumbsIdBranch = aCrumbsIdBranch + tc.CRUMBID_DELIMITER_IN_CATEGORYINFO + aLeafId
            # add branch to fullCrumbsId
            if (fakeFullCrumbsId == None):
                fakeFullCrumbsId = aCrumbsIdBranch
            else:
                fakeFullCrumbsId = fakeFullCrumbsId + tc.BREADCRUMB_PARENTVALUE_DELIMITER_IN + aCrumbsIdBranch

        return fakeFullCrumbsId
'''

