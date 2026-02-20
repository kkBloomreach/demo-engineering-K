# revisionV11 specific constants
MAX_VARIANTS_PER_PRODUCT = 7
VARIANT_PRICE_FACTORS = [
    0.6,
    0.8,
    0.9,
    1.0
]

VARIANT_ATTRIBUTE_NAMES = [
    'skuid',
    'color',
    'size',
    'default_sku',
    'price',
    'sale_price',
    'swatch_image',
    'availability' 
]

ATTRIBUTE_VALUES_COLOR_SHOES = [
    'white',
    'red',
    'blue',
    'yellow',
    'green',
    'black',
    'gray'
]

ATTRIBUTE_VALUES_SIZE_SHOES = [
    '6',
    '7',
    '8',
    '8.5',
    '9',
    '9.5',
    '10',
    '12'
]

ATTRIBUTE_VALUES_COLOR_JEANS = [
    'brown',
    'blue',
    'black',
    'gray',
    'navy blue'
]

ATTRIBUTE_VALUES_SIZE_JEANS = [
    'XS',
    'XM',
    'MD',
    'LG',
    'XL',
    'XXL',
]

# list size must be greater than MAX_VARIANTS_PER_PRODUCT
ATTRIBUTE_VALUES_COLOR_HANDBAGS = [
    'brown',
    'blue',
    'black',
    'gray',
    'navy blue',
    'red',
    'pink',
    'white',
    'silver',
    'yellow'
]


# products only in the following categories have variants
CATEGORY_TYPES_FOR_PRODUCTS_WITH_VARIANTS = [
    {'leafName': 'men>shoes',       'leafId': '10800' , 'type': 'SHOES' },
    {'leafName': 'women>shoes',     'leafId': '20800' , 'type': 'SHOES' },
    {'leafName': 'shoes>casual',    'leafId': '70100' , 'type': 'SHOES' },
    {'leafName': 'shoes>dress',     'leafId': '70200' , 'type': 'SHOES' },
    {'leafName': 'shoes>sneakers',  'leafId': '70300' , 'type': 'SHOES' },
    {'leafName': 'men>jeans',       'leafId': '10900' , 'type': 'JEANS' },
    {'leafName': 'women>jeans',     'leafId': '20900' , 'type': 'JEANS' },
    {'leafName': 'handbags>purses', 'leafId': '80100' , 'type': 'HANDBAGS', },
    {'leafName': 'handbags>backpacks&totes', 'leafId': '80200' , 'type': 'HANDBAGS' }
]


