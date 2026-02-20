# V8 changes
# -- change crumbs for some 'bag' products (change to backpacks&totes)

import logging
import random
import os
import copy

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV8 as rcv8

class RevisionV8 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v8')
        super().__init__ ()
        return

    # override base class method
    # This update class does not do any update to previous records
    def _perform_record_update (self, record, gen_image_map):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)
        if (inject_av_record == None):
            logging.warning ('No inject attrib_value record for pid: %s', pid)
            return record

        updated_record = self._perform_update_internal (record, inject_av_record)
        return updated_record

    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']

        updated_record = copy.deepcopy (record)

        # change category if necessary. 
        # Using current 'category_path' value in jsonl, build crumb_and_crumbids in the form A>B, 100>200
        current_full_crumb, current_full_crumb_id = self._category_builder.construct_crumbs_and_crumbIds_from_category_path (record)
        edited_full_crumb = inject_av_record ['edited_bread_crumb'] # column expected in injected record

        # Category - check crumb change
        # NOTE: edited_full_crumb uses 'newCrumbName' (eg, totes -> backpack&totes)
        if (current_full_crumb != edited_full_crumb):
            changed_category_path = self._category_builder.construct_category_path_from_crumbs_and_crumbIds (edited_full_crumb)
            updated_record ['value']['attributes']['category_paths'] = changed_category_path
            logging.debug ('Changed crumbs for pid %s; new crumb %s' % (pid, edited_full_crumb))

        # availability
        edited_availability = inject_av_record ['edited_availability']  ## "TRUE" or "FALSE"
        edited_availability_bool = (edited_availability.lower () == 'true') # boolean true/false
        if (edited_availability_bool != record ['value']['attributes']['availability']):
            logging.debug ('Changed availability for pid %s, new availability %s' % (pid, edited_availability))
            updated_record ['value']['attributes']['availability'] = edited_availability_bool

        # gender
        edited_gender = inject_av_record ['edited_gender']
        if (edited_gender != record ['value']['attributes']['gender']):
            logging.debug ('Changed gender for pid %s, new gender %s' % (pid, edited_gender))
            updated_record ['value']['attributes']['gender'] = edited_gender

        # color
        edited_color = inject_av_record ['edited_color']
        if (edited_color != record ['value']['attributes']['color']):
            logging.debug ('Changed color for pid %s, new color %s' % (pid, edited_color))
            updated_record ['value']['attributes']['color'] = edited_color

        # brand
        edited_brand = inject_av_record ['edited_brand']
        if (edited_brand != record ['value']['attributes']['brand']):
            logging.debug ('Changed brand for pid %s, new brand %s' % (pid, edited_brand))
            updated_record ['value']['attributes']['brand'] = edited_brand
            updated_record ['value']['attributes']['product_brand'] = edited_brand

        # material
        edited_material = inject_av_record ['edited_material']
        if (edited_material != record ['value']['attributes']['material']):
            logging.debug ('Changed material for pid %s, new material %s' % (pid, edited_material))
            updated_record ['value']['attributes']['material'] = edited_material

        # title 
        edited_title = inject_av_record ['edited_title']
        if (edited_title != record ['value']['attributes']['title']):
            logging.debug ('Changed title for pid %s, new title %s' % (pid, edited_title))
            updated_record ['value']['attributes']['title'] = edited_title

        # price. Recalc sale_price, onSale ONLY IF price is changed
        if 'edited_price' in inject_av_record:
            edited_price = float (inject_av_record ['edited_price'])
            if (edited_price != record ['value']['attributes']['price']):
                logging.debug ('Changed price for pid %s, new price %s' % (pid, edited_price))
                updated_record ['value']['attributes']['price'] = edited_price 

                # onSale, about 20% of products
                price = updated_record ['value']['attributes']['price'] # type = float
                sale_price = price # default sale_price = price
                randInt = int (random.random () * 10)
                if (randInt <= 2):
                    # sale price is ~30%
                    sale_percent = (random.random () * 25) + 5 # 5 -> 30 sale factor
                    sale_price = price * (1 - (sale_percent / 100))
                    sale_price = round (sale_price, 2)
                updated_record ['value']['attributes']['sale_price'] = sale_price

                # onSale
                if sale_price < price:
                    updated_record ['value']['attributes']['onSale'] = True
                else:
                    updated_record ['value']['attributes']['onSale'] = False

        # collection
        edited_collection = inject_av_record ['edited_collection']
        if (edited_collection != record ['value']['attributes']['collection']):
            logging.debug ('Changed collection for pid %s, new collection %s' % (pid, edited_collection))
            updated_record ['value']['attributes']['collection'] = edited_collection

        # style
        edited_style = inject_av_record ['edited_style']
        if (edited_style != record ['value']['attributes']['style']):
            logging.debug ('Changed style for pid %s, new style %s' % (pid, edited_style))
            updated_record ['value']['attributes']['style'] = edited_style

        # stock-level 
        edited_stock_level = inject_av_record ['edited_stock_level']
        if (edited_stock_level != record ['value']['attributes']['stock_level']):
            logging.debug ('Changed stock_level for pid %s, new stock_level %s' % (pid, edited_stock_level))
            updated_record ['value']['attributes']['stock_level'] = edited_stock_level

        # season 
        edited_season = inject_av_record ['edited_season']
        if (edited_season != record ['value']['attributes']['season']):
            logging.debug ('Changed season for pid %s, new season %s' % (pid, edited_season))
            updated_record ['value']['attributes']['season'] = edited_season

        # special_offer 
        edited_special_offer = inject_av_record ['edited_special_offer']
        if (edited_special_offer != record ['value']['attributes']['special_offer']):
            logging.debug ('Changed special_offer for pid %s, new special_offer %s' % (pid, edited_special_offer))
            updated_record ['value']['attributes']['special_offer'] = edited_special_offer

        # collection
        edited_collection = inject_av_record ['edited_collection']
        if (edited_collection != record ['value']['attributes']['collection']):
            logging.debug ('Changed collection for pid %s, new collection %s' % (pid, edited_collection))
            updated_record ['value']['attributes']['collection'] = edited_collection

        # product_fit - Not in injected map in this version
        # if 'product_fit' in updated_record ['value']['attributes']:
        #     updated_record ['value']['attributes'].pop ('product_fit')

        return updated_record

if __name__ == '__main__':
    rv = RevisionV8 ()

