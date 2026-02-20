# V11 changes
# -- add variants to some products

import logging
import random
import os
import copy
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV11 as rcv11
from categorybuilder import CategoryBuilder

class RevisionV11 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v11')
        super().__init__ ()
        return

    def _initialize (self, source_records, inject_av_map):
        return True

    # override base class method
    # This update class does not do any update to previous records
    def _perform_record_update (self, record, gen_image_map):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)

        if (inject_av_record == None):
            logging.debug  ('No inject attrib_value record for pid: %s', pid)
            return record

        updated_record = self._perform_update_internal (record, inject_av_record)
        return updated_record

    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']

        updated_record = copy.deepcopy (record)
        # Add variant if product in specific category
        # Note: same product may be in multiple categories, however we need to check only
        # if at-least one of the categories is in 'selected_category_type'
        if 'category_paths' in record ['value']['attributes']:
            category_paths = record ['value']['attributes']['category_paths']
            selected_category_type = self._check_product_in_selected_category_type (category_paths)
            if (selected_category_type != None):
                if (selected_category_type ['type'] == 'SHOES'):
                    product_variants = self._build_variants_for_shoe (record)
                    updated_record ['value']['variants'] = product_variants 
                    # also remove product-level color/size/product_size attributes
                    if 'color' in updated_record ['value']['attributes']:
                        updated_record ['value']['attributes'].pop ('color')
                    if 'size' in updated_record ['value']['attributes']:
                        updated_record ['value']['attributes'].pop ('size')
                    if 'product_size' in updated_record ['value']['attributes']:
                        updated_record ['value']['attributes'].pop ('product_size')

                elif (selected_category_type ['type'] == 'JEANS'):
                    product_variants = self._build_variants_for_jean (record)
                    updated_record ['value']['variants'] = product_variants 
                    # also remove product-level color/size/product_size attributes
                    if 'color' in updated_record ['value']['attributes']:
                        updated_record ['value']['attributes'].pop ('color')
                    if 'size' in updated_record ['value']['attributes']:
                        updated_record ['value']['attributes'].pop ('size')
                    if 'product_size' in updated_record ['value']['attributes']:
                        updated_record ['value']['attributes'].pop ('product_size')
                elif (selected_category_type ['type'] == 'HANDBAGS'):
                    product_variants = self._build_variants_for_handbags (record)
                    updated_record ['value']['variants'] = product_variants 
                    # also remove product-level color/size/product_size attributes
                    if 'color' in updated_record ['value']['attributes']:
                        updated_record ['value']['attributes'].pop ('color')
                    if 'size' in updated_record ['value']['attributes']:
                        updated_record ['value']['attributes'].pop ('size')
                    if 'product_size' in updated_record ['value']['attributes']:
                        updated_record ['value']['attributes'].pop ('product_size')
                else:
                    logging.error ('Unknown category type to build variants: %s' % record ['value']['attributes']['pid'])

        return updated_record

    # only products in selected category have variants
    def _check_product_in_selected_category_type (self, category_paths):
        for branch in category_paths:
            branch_len = len (branch)
            leaf_id = branch [branch_len - 1]['id']
            for category_type_record in rcv11.CATEGORY_TYPES_FOR_PRODUCTS_WITH_VARIANTS:
                if category_type_record ['leafId'] == leaf_id:
                    return category_type_record
        return None 

    def _build_variants_for_shoe (self, record):
        pid = record ['value']['attributes']['pid']
        variants = {}

        num_variants = (int) (random.random () * rcv11.MAX_VARIANTS_PER_PRODUCT) + 1
        # string: color+size, ensure unique combo among all variants in this pid
        variant_color_and_size_combo = []  

        for variant_num in range (0, num_variants):
            variant_id = '%s_%s' % (pid, variant_num)
            variants [variant_id] = {}
            variant_attributes = {}
            variants [variant_id]['attributes'] = variant_attributes

            combo_available = False
            while (combo_available == False):
                color_indx = (int) (random.random () * len (rcv11.ATTRIBUTE_VALUES_COLOR_SHOES))
                color = rcv11.ATTRIBUTE_VALUES_COLOR_SHOES [color_indx]

                size_indx = (int) (random.random () * len (rcv11.ATTRIBUTE_VALUES_SIZE_SHOES))
                size = rcv11.ATTRIBUTE_VALUES_SIZE_SHOES [size_indx]

                combo = '%s+%s' % (color, size)
                if (combo in variant_color_and_size_combo) == False:
                    variant_color_and_size_combo.append (combo)
                    combo_available = True

            # yes, this color, size combo is unique in this product
            for attribute_name in rcv11.VARIANT_ATTRIBUTE_NAMES:
                if (attribute_name == 'color'):
                    variant_attributes['color'] = color
                elif (attribute_name == 'size'):
                    variant_attributes['size'] = size
                elif (attribute_name == 'skuid'):
                    variant_attributes['skuid'] = variant_id
                elif (attribute_name == 'default_sku'):
                    if variant_num == 0:
                        variant_attributes['default_sku'] = True   # first variant default_sku = True
                    else:
                        variant_attributes['default_sku'] = False
                elif (attribute_name == 'price'):
                    factor_indx = (int) (random.random () * len(rcv11.VARIANT_PRICE_FACTORS))
                    factor = rcv11.VARIANT_PRICE_FACTORS [factor_indx]
                    variant_price = record ['value']['attributes']['price'] * factor
                    variant_attributes['price'] = round (variant_price, 2)
                    # set sale_price = price
                    variant_attributes['sale_price'] = variant_attributes['price']
                elif (attribute_name == 'swatch_image'):
                    variant_attributes['swatch_image'] = record ['value']['attributes']['thumb_image']
                elif (attribute_name == 'sale_price'):
                    continue    # sale_price set along with price
                elif (attribute_name == 'availability'):
                    variant_attributes['availability'] = True

        return variants

    def _build_variants_for_jean (self, record):
        pid = record ['value']['attributes']['pid']
        variants = {}

        num_variants = (int) (random.random () * rcv11.MAX_VARIANTS_PER_PRODUCT) + 1
        # string: color+size, ensure unique combo among all variants in this pid
        variant_color_and_size_combo = []  

        for variant_num in range (0, num_variants):
            variant_id = '%s_%s' % (pid, variant_num)
            variants [variant_id] = {}
            variant_attributes = {}
            variants [variant_id]['attributes'] = variant_attributes

            combo_available = False
            while (combo_available == False):
                color_indx = (int) (random.random () * len (rcv11.ATTRIBUTE_VALUES_COLOR_JEANS))
                color = rcv11.ATTRIBUTE_VALUES_COLOR_JEANS [color_indx]

                size_indx = (int) (random.random () * len (rcv11.ATTRIBUTE_VALUES_SIZE_JEANS))
                size = rcv11.ATTRIBUTE_VALUES_SIZE_JEANS [size_indx]

                combo = '%s+%s' % (color, size)
                if (combo in variant_color_and_size_combo) == False:
                    variant_color_and_size_combo.append (combo)
                    combo_available = True

            # yes, this color, size combo is unique in this product
            for attribute_name in rcv11.VARIANT_ATTRIBUTE_NAMES:
                if (attribute_name == 'color'):
                    variant_attributes['color'] = color
                elif (attribute_name == 'size'):
                    variant_attributes['size'] = size
                elif (attribute_name == 'skuid'):
                    variant_attributes['skuid'] = variant_id
                elif (attribute_name == 'default_sku'):
                    if variant_num == 0:
                        variant_attributes['default_sku'] = True   # first variant default_sku = True
                    else:
                        variant_attributes['default_sku'] = False
                elif (attribute_name == 'price'):
                    factor_indx = (int) (random.random () * len(rcv11.VARIANT_PRICE_FACTORS))
                    factor = rcv11.VARIANT_PRICE_FACTORS [factor_indx]
                    variant_price = record ['value']['attributes']['price'] * factor
                    variant_attributes['price'] = round (variant_price, 2)
                    # set sale_price = price
                    variant_attributes['sale_price'] = variant_attributes['price']
                elif (attribute_name == 'swatch_image'):
                    variant_attributes['swatch_image'] = record ['value']['attributes']['thumb_image']
                elif (attribute_name == 'sale_price'):
                    continue    # sale_price set along with price
                elif (attribute_name == 'availability'):
                    variant_attributes['availability'] = True
        return variants

    def _build_variants_for_handbags (self, record):
        pid = record ['value']['attributes']['pid']
        variants = {}

        num_variants = (int) (random.random () * rcv11.MAX_VARIANTS_PER_PRODUCT) + 1
        # handbag variants - only color change, no size change
        variant_colors = []  

        for variant_num in range (0, num_variants):
            variant_id = '%s_%s' % (pid, variant_num)
            variants [variant_id] = {}
            variant_attributes = {}
            variants [variant_id]['attributes'] = variant_attributes

            color_available = False
            while (color_available == False):
                color_indx = (int) (random.random () * len (rcv11.ATTRIBUTE_VALUES_COLOR_HANDBAGS))
                color = rcv11.ATTRIBUTE_VALUES_COLOR_HANDBAGS [color_indx]

                if (color in variant_colors) == False:
                    variant_colors.append (color)
                    color_available = True

            # yes, this color is unique in this product
            for attribute_name in rcv11.VARIANT_ATTRIBUTE_NAMES:
                if (attribute_name == 'color'):
                    variant_attributes['color'] = color
                elif (attribute_name == 'skuid'):
                    variant_attributes['skuid'] = variant_id
                elif (attribute_name == 'default_sku'):
                    if variant_num == 0:
                        variant_attributes['default_sku'] = True   # first variant default_sku = True
                    else:
                        variant_attributes['default_sku'] = False
                elif (attribute_name == 'price'):
                    factor_indx = (int) (random.random () * len(rcv11.VARIANT_PRICE_FACTORS))
                    factor = rcv11.VARIANT_PRICE_FACTORS [factor_indx]
                    variant_price = record ['value']['attributes']['price'] * factor
                    variant_attributes['price'] = round (variant_price, 2)
                    # set sale_price = price
                    variant_attributes['sale_price'] = variant_attributes['price']
                elif (attribute_name == 'swatch_image'):
                    variant_attributes['swatch_image'] = record ['value']['attributes']['thumb_image']
                elif (attribute_name == 'sale_price'):
                    continue    # sale_price set along with price
                elif (attribute_name == 'availability'):
                    variant_attributes['availability'] = True
        return variants

if __name__ == '__main__':
    rv = RevisionV11 ()

