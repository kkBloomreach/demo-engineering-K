import logging

class ProductListSelector ():
    def __init__ (self):
        self._training_config = None
        self._source_catalog = None
        return
    
    def set_training_config (self, training_config):
        self._training_config = training_config
        return
    
    def set_source_catalog (self, source_catalog):
        self._source_catalog = source_catalog
        return
    
    def build_selected_product_list (self):
        selected_product_list = None

        if 'attribute_constraint' in self._training_config ['catalog_constraints']:
            constrain_attrib = self._training_config ['catalog_constraints']['attribute_constraint']['attribute']
            constrain_value = self._training_config ['catalog_constraints']['attribute_constraint']['value']
            max_products_to_include = self._training_config ['catalog_constraints']['attribute_constraint']['max_products_to_include']
            selected_product_list = self._select_products_by_attribute_constraint (self._source_catalog, constrain_attrib, constrain_value, max_products_to_include)
        elif 'size_constraint' in self._training_config ['catalog_constraints']:
            start = self._training_config ['catalog_constraints']['size_constraint']['start']
            max_products = self._training_config ['catalog_constraints']['size_constraint']['max_products']
            selected_product_list = self._select_products_by_size_constraint (self._source_catalog, start, max_products)
        else:
            logging.error ("Unknown catalog constraint")
        return selected_product_list

    def _select_products_by_attribute_constraint (self, source_catalog, constrain_attrib, constrain_value, max_products_to_include):
        # extract specific fields from each product
        product_info_list = []

        for product in source_catalog:
            if constrain_attrib in product ['value']['attributes']:
                attrib_value = product ['value']['attributes'][constrain_attrib].lower ()
            else:
                attrib_value = None

            if attrib_value == None:
                continue    # exclude this product since it does not have constrain_attrib
            elif product ['value']['attributes'][constrain_attrib].lower() != constrain_value:
                continue # exclude products with value != constrain_value

            #@@@ Due to openAI token limit, include only N products
            if len (product_info_list) > max_products_to_include:
                break

            colors, categories = self._collect_colors_and_categories_for_product (product)
            product_info = {
                            'title' : product ['value']['attributes']['title'],
                            'description': product ['value']['attributes']['description'],
                            'brand': product ['value']['attributes']['brand'],
                            'colors': colors,
                            'categories': categories
                           }
            product_info_list.append (product_info)
        return product_info_list
    
    def _select_products_by_size_constraint (self, source_catalog, start = 0, max_products = 500):
        count = 0
        product_info_list = []

        # we go thru each product to make sure it is 'available'
        for product in source_catalog:
            if count < start:
                continue # skip products prior to 'start'
            if product ['value']['attributes']['availability'] == False:
                continue
            count = count + 1
            if len (product_info_list) > max_products:
                break # collect only 'max-products'

            colors, categories = self._collect_colors_and_categories_for_product (product)
            product_info = {
                            'title' : product ['value']['attributes']['title'],
                            'description': product ['value']['attributes']['description'],
                            'brand': product ['value']['attributes']['brand'],
                            'colors': colors,
                            'categories': categories
                           }
            product_info_list.append (product_info)
        return product_info_list
    
    def _collect_colors_and_categories_for_product (self, product):
        colors = None
        if 'variants' in product ['value']:
                product_variants = product ['value']['variants'].values()
                for variant in product_variants:
                    if 'color' in variant ['attributes']:
                        if colors == None:
                            colors = variant ['attributes']['color']
                        else:
                            colors = '%s,%s' % (colors, variant ['attributes']['color'])
        if 'colors' == None:
            colors = ''

        categories = None
        category_paths = product ['value']['attributes']['category_paths']
        for branch in category_paths:
            for leaf in branch:
                if categories == None:
                    categories = leaf ['name']
                else:
                    categories = '%s,%s' % (categories, leaf ['name'])
        return (colors, categories)
    
'''
colors = None
            if 'variants' in product ['value']:
                product_variants = product ['value']['variants'].values()
                for variant in product_variants:
                    if 'color' in variant ['attributes']:
                        if colors == None:
                            colors = variant ['attributes']['color']
                        else:
                            colors = '%s,%s' % (colors, variant ['attributes']['color'])
            if 'colors' == None:
                colors = ''

            categories = None
            category_paths = product ['value']['attributes']['category_paths']
            for branch in category_paths:
                for leaf in branch:
                    if categories == None:
                        categories = leaf ['name']
                    else:
                        categories = '%s,%s' % (categories, leaf ['name'])
'''