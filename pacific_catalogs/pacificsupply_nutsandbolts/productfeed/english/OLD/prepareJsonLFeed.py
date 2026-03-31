# Convert .tsv feed file to jsonl and .json
import logging
import csv
import json

FILENAME_SRC_FEED ='./data/sample_nutsandbolt_feed_20210111.tsv'
# smaller feed for debugging
# FILENAME_SRC_FEED ='./data/sample_nutsandbolt_feed_20210111_10.tsv'
# output files
FILENAME_JSONL_FEED_OUTPUT = './data/sample_nutsandbold_feed_20210111.jsonl'
FILENAME_JSON_FEED_OUTPUT = './data/sample_nutsandbold_feed_20210111.json'

# Although we define these parent-delimiters, NutsAndBolt does not have
# products in multiple categories. Therefore there is no product that
# has such as parent-delimiter
CRUMBS_PARENTVALUE_DELIMITER = ';'
CRUMBSID_PARENTVALUE_DELIMITER = ';'
CRUMBS_VALUE_DELIMITER = '|'
CRUMBSID_VALUE_DELIMITER = '|'


# src crumb: A>B>C|X>Y, crumbsId: 10>20>30|50>60
# return category_paths as expected by dataConnect
def constructCategoryPaths (totalCrumbs, totalCrumbsId):
    # 'outer' list
    category_paths = []

    # get individual 'paths' within totalcrumb (eg, "A>B>C", "X>Y" and "10>20>30", "50>60")
    crumbPaths = totalCrumbs.split (CRUMBS_PARENTVALUE_DELIMITER)
    crumbIdPaths = totalCrumbsId.split (CRUMBSID_PARENTVALUE_DELIMITER)
    if (len (crumbPaths) == len (crumbIdPaths)):
        for i in range (0, len (crumbPaths)):
            crumbPathMembers = crumbPaths [i].split (CRUMBS_VALUE_DELIMITER) 
            crumbIdPathMembers = crumbIdPaths [i].split (CRUMBS_VALUE_DELIMITER) 
            # members: "A","B","C", "10","20","30"
            if (len (crumbPathMembers) == len (crumbIdPathMembers)):
                # 'inner' list, one for each path
                category_single_path = []
                for j in range (0, len (crumbPathMembers)):
                    # 'inner' list contains individual dict-objects {id, name}, one for each pathMember
                    category_single_path.append ({'id': crumbIdPathMembers [j],
                                                  'name': crumbPathMembers [j]})
                category_paths.append (category_single_path)

    return category_paths

# NOTE: No product in NutsAndBolt catalog has variants. Therefore
# 'variant' related code is not included in this script
def processSrcRecords (srcRecords):
    outputRecords = []
    for oneSrcRecord in srcRecords:
        oneOutputRecord = {}

        oneOutputRecord ['op'] = 'add'
        oneOutputRecord ['path'] = '/products/' + oneSrcRecord ['pid']
        oneOutputRecord ['value'] = {}

        # attributes
        oneOutputRecord ['value']['attributes'] = {}
        for key in oneSrcRecord.keys ():
            if (key == 'crumbs') or (key == 'crumbs_id'):
                continue
            oneOutputRecord ['value']['attributes'][key] = oneSrcRecord [key]
        category_paths = constructCategoryPaths (oneSrcRecord ['crumbs'], oneSrcRecord ['crumbs_id'])
        oneOutputRecord ['value']['attributes']['category_paths'] = category_paths

        # append individual outputRecord to list-of-output-records
        outputRecords.append (oneOutputRecord)

    return outputRecords

def writeJsonOutput (srcRecords):
    localSavePath = FILENAME_JSON_FEED_OUTPUT
    with open (localSavePath, 'w') as file_output:
        jsonString = json.dumps (srcRecords)
        file_output.write ('%s\n' % jsonString)


def writeJsonLOutput (srcRecords):
    localSavePath = FILENAME_JSONL_FEED_OUTPUT
    with open (localSavePath, 'w') as file_output:
        for i in range (0, len (srcRecords)):
            jsonLString = json.dumps (srcRecords [i])
            file_output.write ('%s\n' % jsonLString)


def readSourceFeed ():
    file_obj = open (FILENAME_SRC_FEED, 'r')
    dict_reader = csv.DictReader (file_obj, delimiter='\t')

    src_feed_records = []
    for row in dict_reader:
        src_feed_records.append (row)

    return src_feed_records


def doInits ():
    logging.basicConfig (level=logging.INFO)


if __name__ == "__main__":
    doInits ()

    # read source catIds
    src_feed_records = readSourceFeed () 

    # process all srcRecords
    outputRecords = processSrcRecords (src_feed_records)

    # write jsonl output
    writeJsonLOutput (outputRecords)

    # write json output
    writeJsonOutput (outputRecords)


