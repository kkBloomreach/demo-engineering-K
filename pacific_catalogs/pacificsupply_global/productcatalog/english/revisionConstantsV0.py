# revisionV0 specific constants

# catid list
FILENAME_SELECTED_CATIDLIST_TSV_IN = './data/input/catinfolist_11202024.tsv'

# additional pid-list
FILENAME_ADDITIONAL_PIDLIST_TSV_IN = './data/input/additional_pidlist_11202024.tsv'

# catid-name map in output catalog. Needed for translation later
FILENAME_CATID_NAME_MAP_TSV_OUT = './data/output/catid_name_en_map_04292026.tsv'

BRSM_CATAPI_ENDPOINT ='http://core.dxpapi.com/api/v1/core/' 
BRSM_CATAPI_PARAMS = {
                        'account_id': '6370',
                        'auth_key': '1vjobidilg5gcbpn',
                        'domain_key': 'pacific_supply',
                        'request_id': '427715043276',
                        'url': 'www.bloomique.com',
                        'ref_url': 'www.bloomique.com',
                        'request_type': 'search',
                        'rows': '25',
                        'start': '0',
                        'fl': 'pid',
                        'search_type': 'keyword'
                     }

SELECTED_ATTRIBUTES_LIST = [
    'availability',
    'price',
    'sale_price',
    'url',
    'pid',
    'title',
    'description',
    'brand',
    'category_paths',
    'thumb_image',
    'manufacturer',
    'catalog_number',
    'catalog_number_partialvalue',
    'master_upc',
    'application',
    'color',
    'height',
    'length',
    'material',
    'size',
    'voltage_rating',
    'weight',
    'type'
]

