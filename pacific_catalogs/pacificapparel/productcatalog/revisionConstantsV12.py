# revisionV12 specific constants
FILENAME_SOURCE_PRODUCTS_TSV_IN = './data/input/source_health_and_beauty_products_01132025.csv'
#FILENAME_SOURCE_PRODUCTS_TSV_IN = './data/input/source_health_and_beauty_products_01132025_2.csv'
#FILENAME_SOURCE_PRODUCTS_TSV_IN = './data/input/source_health_and_beauty_products_01132025_6.csv'

SELECTED_HEALTH_AND_BEAUTY_PRODUCT_CATEGORIES = [
    'cleanser',
    'exfoliator',
    'moisturizer',
    'serum',
    'blush',
    'bronzer',
    'concealer',
    'contour',
    'eye shadow',
    'eyeliner',
    'foundation',
    'highlighter',
    'lip gloss',
    'lip liner',
    'lipstick',
    'makeup remover',
    'mascara',
    'primer'
]

# in each selected category, use at the most MAX products
MAX_PRODUCTS_TO_USE_IN_CATEGORY = 20
MAX_PRODUCT_PRICE = 150.0
SALE_PRICE_FACTOR = 0.8 # same factor for all products-on-sale
MIN_MARGIN = 30.0 # percent
MAX_MARGIN = 70.0 # percent

# specific attribs, additional to default
HEALTH_AND_BEAUTY_PRODUCT_ATTRIBUTES = [
    'packaging',
    'skin_type',
    'usage_frequency'
]

ATTRIBUTE_VALUES_COLOR = [
    'white',
    'red',
    'blue',
    'yellow',
    'green',
    'black',
    'gray'
]

# PacificApparel product record template
PACIFICAPPAREL_PRODUCT_RECORD_TEMPLATE = \
{
  "op": "add",
  "path": "",
  "value": {
    "attributes": {
      "availability": True,
      "brand": "",
      "color": "",
      "countryOfOrigin": "",
      "description": "",
      "end_date": "203012311159",
      "gender": "",
      "margin": 0.0,
      "onSale": False,
      "pid": "",
      "price": 0.0,
      "product_brand": "",
      "rating": 0.0,
      "reviews": 0,
      "sale_price": 0.0,
      "size": '',
      "start_date": "202401010001",
      "thumb_image": "",
      "title": "",
      "url": "",
      "material": "",
      "collection": "",
      "style": "",
      "stock_level": "ok",
      "season": "",
      "special_offer": ""
    }
  }
}

# new images
FILENAME_AWS_UPLOAD_SCRIPT_OUT = './data/images/aws_image_upload.sh'
AWS_S3_IMAGES_FOLDER = 's3://pacific-demo-data.bloomreach.cloud/apparel/images'
AWS_CP_COMMAND_PREAMBLE = 'aws --profile bloomreach-demo_main s3 cp --acl public-read '

# descriptions are also generated using openai. Save them locally as .tsv
FILENAME_PRODUCT_DESCRIPTIONS_TSV_IN =  './data/descriptions/product_descriptions_01162025.tsv'
FILENAME_PRODUCT_DESCRIPTIONS_TSV_OUT = './data/descriptions/product_descriptions_01172025.tsv'

