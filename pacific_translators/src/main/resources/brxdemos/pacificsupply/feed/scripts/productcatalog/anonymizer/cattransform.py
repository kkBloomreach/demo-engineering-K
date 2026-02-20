# utilities for sourceFeed files
import csv
import os.path
import logging

# category name changes

INVALID_CRUMB_NAME = "Unclassified"
CRUMBS_DELIMITER = '|'
CRUMBS_ID_DELIMITER = '|'

# array of dictionaries. 
cat_name_copy = []
cat_name_rename = []
cat_name_delete = []
cat_name_move = []

# dictionary of "crumbs:crumbs_id" values
#   -- 'origCrumb': crumb, 'origCrumbId': crumbId
CRUMB_AND_ID_LIST = []


def checkIfCrumbAndIdMapExists (cumulativeName):
    global CRUMB_AND_ID_LIST 

    for aMap in CRUMB_AND_ID_LIST:
        if (aMap ['origCrumb'] == cumulativeName):
            return True
    return False

# this method has individual crumb and crumb_id in param
def addCrumbAndIdMap (aCrumb, aCrumbId):
    global CRUMB_AND_ID_LIST

    CRUMB_AND_ID_LIST.append ({'origCrumb': aCrumb.strip(),
                               'origCrumbId': aCrumbId.strip()
                              })

# this method has lists in the params
def updateCrumbAndIdMapUsingLists (crumbsList, crumbsIdList):
    global CRUMB_AND_ID_LIST 

    cumulativeName = ""
    if (len (crumbsList) != len (crumbsIdList)):
        logging.error ("ERROR crumbs and crumbs_id mismatch")
        return

    for count in range (len (crumbsList)):
        if (count == 0):
            cumulativeName = crumbsList [count]
        else:
            cumulativeName = cumulativeName + CRUMBS_DELIMITER + crumbsList [count]

        if (checkIfCrumbAndIdMapExists (cumulativeName) == False):
            logging.debug ("DEBUG adding to crumb-id-map: crumb: " + crumbsList [count] + ", id: " + crumbsIdList [count])
            addCrumbAndIdMap (cumulativeName, crumbsIdList[count])

    logging.debug (CRUMB_AND_ID_LIST)
    return
 
# using crumbs and crumbs_id in source feed, update
# crumbs->crumbs_id dictionary
# "crumbs" is cumulative. That is, for "A/B/C"
# entries are: A: , A|B: , A|B|C:
# this method has strings in the params
def updateCrumbAndIdMap (srcCrumb, srcCrumbsId):

    crumbsList = srcCrumb.split (CRUMBS_DELIMITER)
    crumbsIdList = srcCrumbsId.split (CRUMBS_ID_DELIMITER)

    updateCrumbAndIdMapUsingLists (crumbsList, crumbsIdList)
    return

# given a crumb of the form "A|B|C", return its ID from
# CRUMB_AND_ID_LIST
def lookupCrumbId (aCrumb):
    global CRUMB_AND_ID_LIST

    for aMap in CRUMB_AND_ID_LIST:
        if (aCrumb == aMap ['origCrumb']):
            return aMap ['origCrumbId']

    logging.error ("ERROR. CrumbId not found for: " + aCrumb)
    return None


# Given a list of crumbs, returns a list of corresponding crumb_ids
def populateCrumbIdList (crumbList):

    cumulativeName = ""
    crumbIdList = []

    for count in range (len (crumbList)):
        if (count == 0):
            cumulativeName = crumbList [count]
        else:
            cumulativeName = cumulativeName + CRUMBS_DELIMITER + crumbList [count]

        crumbId = lookupCrumbId (cumulativeName)
        crumbIdList.append (crumbId)

    return (crumbIdList)

# print current CRUMB_AND_ID_MAP - used for debugging etc
def printCrumbAndIdMap ():
    global CRUMB_AND_ID_LIST

    for aMap in CRUMB_AND_ID_LIST:
        logging.debug ("DEBUG: " + aMap ['origCrumb'] + "\t" + aMap ['origCrumbId'])

    return

# Many crumbs are label'd "Unclassified". Those need to be removed
# Returns True if crumbname is valid. Note that the parameter
# is name from the original source-feed (not 'changed' name)
def isCrumbNameValid (srcCrumb):
    if ((srcCrumb != "") and (srcCrumb.lower() != INVALID_CRUMB_NAME.lower())):
        return True

    return False

# since a map (from A -> X) may be followed by another map (A/B -> X/Y)
# we need to find the one with longest match. Returns index in
# 'mapToUse' if found. Else returns -1
def lookupLongestMapForSrcCrumb (mapToUse, fullSrcCrumb):

    finalMatchLength = -1 
    finalMatchIndex = -1

    for count in range (len (mapToUse)):
        aMap = mapToUse [count]
        if (fullSrcCrumb.find (aMap ['ORIGINAL_L0L1']) == 0):
            newMatchLength = len (aMap ['ORIGINAL_L0L1'])
            if (newMatchLength > finalMatchLength):
                finalMatchLength = newMatchLength
                finalMatchIndex = count

    return finalMatchIndex


# given a 'full' crumb, return true if that is just 'copied'
# That is, check if the 'COPY' entry matches with leading-part of actual full srcCrumb
# Since it has to be a 'leading' match, index must be 0 for a crumb to be deleted
def isCrumbCopied (fullSrcCrumb):

    if (lookupLongestMapForSrcCrumb (cat_name_copy, fullSrcCrumb) >=0):
        return True
    return False

# given a 'full' crumb, return true if that is 'moved'
# That is, check if the 'MOVED' entry matches with leading-part of actual full srcCrumb
# Since it has be a 'leading' match, index must be 0 for a crumb to be deleted
def isCrumbMoved (fullSrcCrumb):

    if (lookupLongestMapForSrcCrumb (cat_name_move, fullSrcCrumb) >=0):
        return True
    return False


# given a 'full' crumb, return true if that is 'renamed'
# That is, check if the 'RENAMED' entry matches with leading-part of actual full srcCrumb
# Since it has be a 'leading' match, index must be 0 for a crumb to be deleted
def isCrumbRenamed (fullSrcCrumb):

    if (lookupLongestMapForSrcCrumb (cat_name_rename, fullSrcCrumb) >=0):
        return True
    return False


# given a 'full' crumb, return true if that is 'deleted'
# That is, check if the 'DELETED' entry matches with leading-part of actual full srcCrumb
# Since it has be a 'leading' match, index must be 0 for a crumb to be deleted
def isCrumbDeleted (fullSrcCrumb):

    if (lookupLongestMapForSrcCrumb (cat_name_delete, fullSrcCrumb) >=0):
        return True
    return False


# given a crumb, return "moved-to" crumb. This method assumes
# a check has already been done that the srcCrumb has infact moved
# A move "A/B/C" -> "X/Y/C" means original "C" is now child of "X/Y"
# Note that the target "C" may be called something other than original "C"
# thus allowing a move + rename at the same time
def applyMove (fullSrcCrumb, fullSrcCrumbId):

    if (isCrumbMoved (fullSrcCrumb) == False):
        logging.error ("ERROR. ApplyMove called for a crumb that is not moved")
        return (fullSrcCrumb, fullSrcCrumbId)

    matchIndx = lookupLongestMapForSrcCrumb (cat_name_move, fullSrcCrumb)
    moveOrigL0L1 = cat_name_move [matchIndx]['ORIGINAL_L0L1']
    moveToDest = cat_name_move [matchIndx] ['MOVETO_L0L1']

    logging.debug ("DEBUG moving: orig: " + moveOrigL0L1 + ", to: " + moveToDest)

    # The 'leaf' in the 'original-move' is actually moved to the target
    # Find that leaf crumb
    leafIndx = moveOrigL0L1.rfind (CRUMBS_DELIMITER)
    if (leafIndx > 0):
        origLeaf = moveOrigL0L1 [leafIndx+1:]
    else:
        origLeaf = fullSrcCrumb

    # in order to generate new crumb_ids, split the fullSrcCrumb,
    # find the leaf node in it and then move it to 'move-to' parent
    origSrcCrumbList = fullSrcCrumb.split (CRUMBS_DELIMITER)
    origSrcCrumbIdList = fullSrcCrumbId.split (CRUMBS_ID_DELIMITER)

    # crumb-list for the 'move-to' crumb
    moveToParentIndx = moveToDest.rfind (CRUMBS_DELIMITER)
    if (moveToParentIndx > 0):
        moveToParent = moveToDest [:moveToParentIndx]
    else:
        moveToParent = moveToDest

    logging.debug ("DEBUG moveTo parent: " + moveToParent)
    moveToCrumbList = moveToParent.split (CRUMBS_DELIMITER)
    # Using the CRUMB_AND_ID_LIST global, populate crumbId's for move-to crumbs
    moveToCrumbIdList = populateCrumbIdList (moveToCrumbList)


    # starting with the 'leaf', append itself and all subsequent nodes to "moveTo" lists
    leafCrumbCount = origSrcCrumbList.index (origLeaf)
    for count in range (leafCrumbCount, len (origSrcCrumbList)):
        moveToCrumbList.append (origSrcCrumbList [count])
        moveToCrumbIdList.append (origSrcCrumbIdList [count])

    # add new entries to CRUMB_AND_ID_MAP for the 'moved-to' crumb
    updateCrumbAndIdMapUsingLists (moveToCrumbList, moveToCrumbIdList)

    # generate a tuple containing 'moved-to' crumbs, crumbs_id and return that
    movedToFullCrumb = CRUMBS_DELIMITER.join (moveToCrumbList)
    movedToFullCrumbId = CRUMBS_ID_DELIMITER.join (moveToCrumbIdList)

    return (movedToFullCrumb, movedToFullCrumbId)


# given a crumb, return "renamed" crumb. This method assumes
# a check has already been done that the srcCrumb is infact renamed
# Note that a 'rename' is 'rename-in-place'. There is no change
# in the category tree structure (unlike the Move operation)
def applyRename (fullSrcCrumb, fullSrcCrumbId):

    if (isCrumbRenamed (fullSrcCrumb) == False):
        logging.error ("ERROR. ApplyRenamed called for a crumb that is not renamed")
        return (fullSrcCrumb, fullSrcCrumbId)

    matchIndx = lookupLongestMapForSrcCrumb (cat_name_rename, fullSrcCrumb)
    renameOrigL0L1 = cat_name_rename [matchIndx]['ORIGINAL_L0L1']
    renameToL0L1 = cat_name_rename [matchIndx]['RENAMETO_L0L1']

    logging.debug ("DEBUG rename orig: " + renameOrigL0L1 + ", to: " + renameToL0L1)
    # in order to generate new crumb and crumb_ids, split the fullSrcCrumb,
    origSrcCrumbList = fullSrcCrumb.split (CRUMBS_DELIMITER)
    origSrcCrumbIdList = fullSrcCrumbId.split (CRUMBS_ID_DELIMITER)

    renameToCrumbList = renameToL0L1.split (CRUMBS_DELIMITER)

    # create "new" crumbList - origList with necessary names replaced by 'renameTo' names
    newCrumbList = origSrcCrumbList
    for count in range (len (renameToCrumbList)):
        newCrumbList [count] = renameToCrumbList [count]

    # add new entries to CRUMB_AND_ID_MAP for the 'new' crumb
    # Note that crumbIds remain unchanged - only crumbName are renamed 
    updateCrumbAndIdMapUsingLists (newCrumbList, fullSrcCrumbId.split (CRUMBS_ID_DELIMITER))

    # generate a tuple containing 'rename-to' crumbs, crumbs_id and return that
    renameToFullCrumb = CRUMBS_DELIMITER.join (newCrumbList)

    logging.debug ("DEBUG RENAME: src crumb to rename: " + fullSrcCrumb)
    logging.debug ("DEBUG RENAME: final renamed crumb: " + renameToFullCrumb)

    return (renameToFullCrumb, fullSrcCrumbId)


# read source .tsv containing orig -> changed names
def read_tsv (filename, copied, renamed, deleted, moved):
    file_obj = open (filename, 'r')
    dict_reader = csv.DictReader (file_obj, delimiter='\t')

    for row in dict_reader:
        # add crumbId to internal dictionary
        addCrumbAndIdMap (row ['ORIGINAL_L0L1'], row ['ORIGINAL_CRUMBID'])

        # based on 'operation', put each row in different buckets
        operation = row ['OPERATION']
        if (operation == 'Copy'):
            copied.append (row)
        elif (operation == 'Rename'):
            renamed.append (row)
        elif (operation == 'Move'):
            moved.append (row)
        elif (operation == 'Delete'):
            deleted.append (row)
        else:
            logging.error ("ERROR unknown operation: " + operation)
 
    logging.info ("INFO total category transform count = " + str ((len (copied) + len (renamed) + len (deleted) + len (moved))))
    # this method for debug-printing
    printCrumbAndIdMap ()



# param: list of category name+structure changes to be made in original source feed
def read_inputs (catnameChangesFilePath):

    # cat name changes 
    read_tsv (catnameChangesFilePath, cat_name_copy, cat_name_rename, cat_name_delete, cat_name_move)


if __name__ == '__main__':
    dataDirPath = './data/'
    FILENAME_CAT_NAMECHANGES = 'cat_transformation.tsv'
    catnameChangesFilePath = os.path.join (dataDirPath, FILENAME_CAT_NAMECHANGES)
    read_inputs (dataDirPath)

