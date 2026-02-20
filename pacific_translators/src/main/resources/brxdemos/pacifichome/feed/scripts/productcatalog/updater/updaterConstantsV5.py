# updateV5 specific constants
BOOKS_PRODUCT_ID_LIST_TO_CHANGE = [
    '96571',
    '98641',
    '98037',
    '98098',
    '53814',
    '98095',
    '96539',
    '79800',
    '98094',
    '98084'
	'98094',
	'98084',
	'98099',
	'89511',
	'98085',
	'65641',
	'98638',
	'98097',
	'47490',
	'96548',
	'98681'
]

DIRNAME_BOOK_PRODUCT_IMAGES = './data/images/books'
HTTP_STATUS_OK = 200
IMAGE_LOADER_STATUS_FAIL = -1
IMAGE_LOADER_STATUS_SUCCESS = 1
THUMB_IMAGE_URL_PROLOG = 'https://pacific-demo-data.bloomreach.cloud/home/images/gen/books/'

# names in the original description etc -- anonymized
ORIGINAL_PROPER_NAMES = [
    'Gaby Chapman',
    'Jan Petrovic',
    'Coco Morante',
    'Erin Rhoads'
]

# AWS folder to upload images
FILENAME_AWS_UPLOAD_SCRIPT = './data/output/aws_image_upload.sh'
AWS_S3_IMAGES_FOLDER = 's3://pacific-demo-data.bloomreach.cloud/home/images/gen/books'
AWS_CP_COMMAND_PREAMBLE = 'aws --profile bloomreach-demo_main s3 cp --acl public-read '

# book authors
BOOK_AUTHORS = [
    'Kalel Bradford',
    #'Sage Whitney',
    'Vance Dennis',
    #'Walter Bishop',
    'Journey Swanson',
    'Amias Joseph',
    #'Nicholas Huff',
    'Fiona Liu',
    'Quentin Phan',
    'Travis Villareal',
    'Nii Kitahachi',
    'Osaki Matsuta',
    'Yukimori Hiro',
    'Xavier Monte',
    'Jose Manuel Ballesteros',
    'Youssef Serrano',
    'Paul Markussen',
    'Leo Boesen',
    'Aage Johansen'
]

BOOK_COLLECTIONS = [
    'travel',
    'cooking'
]

# concated leaf-names from category_path
ATTRIB_NAME_LEAFNAMES = 'leafNames'

'''
==========================
# subset
FILENAME_SUBSET_CATID_LIST_IN = './data/input/subsetcatidlistV4.tsv'

# value adjusted so as to have ~300 products in output
SUBSET_MAX_PRODUCTS_TO_USE_IN_CATEGORY = 20

# product or variant attribute to be removed in subset
ATTRIBS_TO_REMOVE_IN_SUBSET = [
    'bread_crumb',
    'bread_crumb_id',
    'category_level_1',
    'category_level_2',
    'category_level_3',
    'category_level_4',
    'int_pid',
    'large_image',
    'velo_material_lower',
    'int_skuid',
    'velo_sku_price',
    'velo_sku_sale_price',
    'wineAppellations',
    'subject',
    'colorFamily',
    'sizeFamily'

]
'''


