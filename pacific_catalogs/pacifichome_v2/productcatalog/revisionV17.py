# V17 changes
# -- remove color names from title and then if duplicate-title, delete the duplicate-title product from catalog
# -- remove some unused old attributes 
# -- some products had null description (bug) - set title as description
# -- include sku_on_sale attribute
# -- correct previous implementation of sku_price/sku_sale_price
# -- add another attribute 'variant->thumb_image' (needed for dashboard to show correct image)
import logging
import copy
import os
import csv
import random

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV17 as rcv17

ALTERNATE_ADJECTIVES = [
    'popular',
    'beautiful',
    'trending',
    'artistic',
    'admired',
    'lovely',
    'graceful',
    'superb',
    'magnificent'
]

class RevisionV17 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v17')
        super().__init__ ()

        # {"color, value}
        self._adjust_color_names_list = []

        # list of product titles (used to remove duplicate products)
        self._product_titles = []
        return

    def _initialize (self, source_records, inject_av_map):
        # read adjust-color list
        self._adjust_color_name_list = self._read_adjust_color_name_list ()
        if (self._adjust_color_name_list == None):
            logging.debug ("No color names to adjust")
        return True

    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)
        #if (inject_av_record == None):
        #    logging.debug ('No inject attrib_value record for pid: %s', pid)

        updated_record = self._perform_update_internal (record, inject_av_record)
        return updated_record

    def _finalize (self, updated_products):
        return True

    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        # replace color name in title
        title = self._replace_color_in_title (record ['value']['attributes']['title'])

        # in case title text is updated, check if it is now a duplicate of another product
        # If so, exclude it from output catalog
        if title.lower() in self._product_titles:
            logging.debug ('Excluding pid = %s due to duplicate title' % (record ['value']['attributes']['pid']))
            return None
        self._product_titles.append (title.lower())

        updated_record = copy.deepcopy (record)

        # if not duplicate product, update title and also replace color in description by an adjective
        description = updated_record ['value']['attributes']['description']
        if description == None: # some description were null due to an earlier bug
            description = '%s %s' % ('Description for', updated_record ['value']['attributes']['title'])
            updated_record ['value']['attributes']['description'] = description
        description = self._replace_color_in_description (description)
        updated_record ['value']['attributes']['title'] = title
        updated_record ['value']['attributes']['description'] = description

        # adjust sku_price/sku_sale_price and include 'sku_on_sale' attribute
        updated_record = self._adjust_sku_price (updated_record)

        # add variant->thumb_image attribute
        updated_record = self._add_sku_thumb_image_attrib (updated_record)

        # delete some unused attributes
        self._delete_unused_attributes (updated_record)

        return updated_record

    def _adjust_sku_price (self, updated_record):
        pid = updated_record ['value']['attributes']['pid']
        if updated_record ['value']['attributes']['onSale'] == True:
            pid_on_sale = True
        else:
            pid_on_sale = False

        # no variant has its price > product_price
        variant_max_price = updated_record ['value']['attributes']['price']

        if ('variants' in updated_record ['value']) and (updated_record ['value']['variants']):
            variant_list = updated_record ['value']['variants']
            variant_count = len (variant_list)
        else:
            return updated_record   # product has no variants (should not happen)

        logging.debug ('----')
        logging.debug ('pid %s, onSale = %s, variant_count:%s' % (pid, pid_on_sale, variant_count))

        # first find and update 'default_sku' object, values same as product price/sale-price
        # Note: it may not be the 0th variant in the variant list
        for variant_id in variant_list.keys():
            variant_obj = variant_list [variant_id]
            # default_variant price/sale-price/onSale == product price/sale-price/onSale
            if variant_obj ['attributes']['default_sku'] == True:
                variant_obj ['attributes']['price'] = updated_record ['value']['attributes']['price']
                variant_obj ['attributes']['sale_price'] = updated_record ['value']['attributes']['sale_price']
                variant_obj ['attributes']['sku_on_sale'] = pid_on_sale
                logging.debug ('\tdefault variant_id: %s, variant_price: %s, variant_sale_price: %s, variant_on_sale: %s' % 
                               (variant_id, 
                                updated_record ['value']['attributes']['price'], 
                                updated_record ['value']['attributes']['sale_price'], 
                                pid_on_sale)) 

        # for some (40%) of products that have onSale = True, some of its variants can be further on sale
        # Note: multiple variants of a product can be on sale (not just one)
        # Note: if pid.onSale = False, NONE of its variants are on sale (just keeps the logic simple to explain)
        total_count_variants_on_sale = 0
        if pid_on_sale == True:
            rand = int (random.random () * 100)
            if rand < 40:   # this product has discounted variants
                total_count_variants_on_sale = int (random.random () * variant_count) # number of variants on sale, [0 -> N)

        # now, of the remaining variants (excluding default), zero or more can be further on-sale
        current_variant_num_on_sale = 0
        for variant_id in variant_list.keys():
            variant_obj = variant_list [variant_id]
            # default_variant already processed above
            if variant_obj ['attributes']['default_sku'] == True:
                continue 

            discount_factor = 1.0
            variant_on_sale = False
            if current_variant_num_on_sale < total_count_variants_on_sale:
                discount_factor = 1 - ((random.random () * 0.20) + 0.15) # at least 15% off
                variant_on_sale = True
                current_variant_num_on_sale = current_variant_num_on_sale + 1

            this_variant_price = round (variant_max_price, 2)
            this_variant_sale_price = round (this_variant_price * discount_factor, 2)

            logging.debug ('\ttotal_count_variants_on_sale: %s, current_variant_num_on_sale: %s, variant_id: %s, variant_price: %s, variant_sale_price: %s, variant_on_sale: %s' % 
                            (total_count_variants_on_sale, current_variant_num_on_sale, variant_id, this_variant_price, this_variant_sale_price, variant_on_sale)) 
            variant_obj ['attributes']['price'] = this_variant_price
            variant_obj ['attributes']['sale_price'] = this_variant_sale_price
            variant_obj ['attributes']['sku_on_sale'] = variant_on_sale

        return updated_record

    def _add_sku_thumb_image_attrib (self, updated_record):
        variant_list = updated_record ['value']['variants']
        for variant_id in variant_list.keys ():
            variant_obj = variant_list [variant_id]
            variant_obj ['attributes']['thumb_image'] = variant_obj ['attributes']['swatch_image']
        return updated_record

    def _delete_unused_attributes (self, updated_record):
        for attrib in (['bread_crumb', 'bread_crumb_id', 'int_pid']):
            if attrib in updated_record ['value']['attributes']:
                del updated_record ['value']['attributes'][attrib]

        if 'variants' in updated_record ['value']:
            for variant_id in updated_record ['value']['variants'].keys():
                variant_obj = updated_record ['value']['variants'][variant_id]
                for attrib in (['int_skuid']):
                    if attrib in variant_obj ['attributes']:
                        del variant_obj ['attributes'][attrib]
        return

    # list of color names (collected from titles). These are removed in the output
    def _read_adjust_color_name_list (self):
        logging.info ("reading source: %s" % rcv17.FILENAME_ADJUST_COLOR_NAMES_TSV_IN)
        adjust_color_names = []
        if os.path.exists (rcv17.FILENAME_ADJUST_COLOR_NAMES_TSV_IN):
            with open (rcv17.FILENAME_ADJUST_COLOR_NAMES_TSV_IN, 'r') as file_obj:
                dict_reader = csv.DictReader (file_obj, delimiter='\t')
                for row in dict_reader:
                    adjust_color_names.append (row)
                file_obj.close ()
            logging.info ('adjust record count: %s' % len (adjust_color_names))
        else:
            logging.error ('cannot find source: %s' % rcv17.FILENAME_ADJUST_COLOR_NAMES_TSV_IN)
            adjust_color_names = None
        return adjust_color_names

    def _replace_color_in_title (self, title):
        for adjust_record in self._adjust_color_name_list:
            if title.find (adjust_record ['color']) >= 0:
                title = title.replace (adjust_record ['color'], '')
                title = title.replace ('  ', ' ')   # multiple consecutive blank spaces -> single
        return title # if no color in title, return it as-is

    def _replace_color_in_description (self, description):
        for adjust_record in self._adjust_color_name_list:
            if description.find (adjust_record ['color']) >= 0:
                alternate = self._select_alternate_adjective (adjust_record ['color'])
                description = description.replace (adjust_record ['color'], alternate)
        return description # if no color in description, return it as-is

    # replace actual color value with some other adjective
    def _select_alternate_adjective (self, color_value):
        rindx = int (random.random () * len (ALTERNATE_ADJECTIVES)) 
        alternate = ALTERNATE_ADJECTIVES [rindx]
        return alternate

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV17()
    logging.info ('RevisionV17finish...')


