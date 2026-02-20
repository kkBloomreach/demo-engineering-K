# V12 changes
# -- add health_and_beauty products

import logging
import random
import os
import copy
import csv
import openai

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV12 as rcv12
import imageloader as il

class RevisionV12 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v12')
        super().__init__ ()
        return

    def _initialize (self, source_records, inject_av_map):
        # read source records
        self._healthandbeauty_source_records = self._read_healthandbeauty_source_product_records ()
        if ((self._healthandbeauty_source_records == None) or (len (self._healthandbeauty_source_records) == 0)):
            return False

        # list of ['catname', 'prod_count']
        self._selected_health_and_beauty_categories = self._prepare_health_and_beauty_categories ()

        # image loader, construct once
        self._image_loader = il.ImageLoader ()

        # list of product descriptions generated earlier and saved locally. May be empty
        self._local_product_description_records = self._read_product_descriptions ()

        # list of images to upload to aws
        self._aws_image_upload_records = []

        return True

    # override base class method
    # This update class does not do any update to previous records
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)

        if (inject_av_record == None):
            logging.debug  ('No inject attrib_value record for pid: %s', pid)

        updated_record = self._perform_update_internal (record, inject_av_record)
        return updated_record

    # add health&beauty products
    def _perform_additions (self, current_products):
        new_products = copy.deepcopy (current_products)

        # identify pid to use for new products
        next_pid_int = -1
        for record in current_products:
            pid = record ['value']['attributes']['pid']
            pid_int = int (pid)
            if (pid_int > next_pid_int):
                next_pid_int = pid_int

        next_pid_int = next_pid_int + 1
        logging.debug ('Health&Beauty pid start: %s' % next_pid_int)

        for healthandbeauty_src_record in self._healthandbeauty_source_records:
            selected_cat_record = self._check_if_src_record_selected (healthandbeauty_src_record ['Category'])
            if (selected_cat_record != None) and (selected_cat_record ['product_count'] < rcv12.MAX_PRODUCTS_TO_USE_IN_CATEGORY):
                new_record = self._build_additional_product (healthandbeauty_src_record, next_pid_int)
                if (new_record != None):
                    new_products.append (new_record)
                    next_pid_int = next_pid_int + 1
                    selected_cat_record ['product_count'] = selected_cat_record ['product_count'] + 1

        return new_products

    # generate aws upload script (to be executed separately)
    def _finalize (self, updated_products):
        if (len (self._aws_image_upload_records) > 0):
            self._prepare_upload_script ()
        else:
            logging.warning ('No images to upload to AWS')

        if (len (self._local_product_description_records) > 0):
            self._save_product_descriptions ()
        else:
            logging.warning ('No product descriptions to save')
        return True

    # INTERNAL METHODS
    # read previously generated product descriptions if any
    def _read_product_descriptions (self):
        product_descriptions = []
        if (os.path.exists (rcv12.FILENAME_PRODUCT_DESCRIPTIONS_TSV_IN)):
            with open (rcv12.FILENAME_PRODUCT_DESCRIPTIONS_TSV_IN, 'r') as descriptions_file:
                dict_reader = csv.DictReader (descriptions_file, delimiter='\t')
                for row in dict_reader:
                    product_descriptions.append (row)
                descriptions_file.close ()
        return product_descriptions

    def _save_product_descriptions (self):
        with open (rcv12.FILENAME_PRODUCT_DESCRIPTIONS_TSV_OUT, 'w') as descriptions_file:
            tsv_writer = csv.writer (descriptions_file, delimiter = '\t')
            header_line = self._local_product_description_records[0].keys()
            tsv_writer.writerow (header_line)

            for row in self._local_product_description_records:
                tsv_writer.writerow (row.values ())
            descriptions_file.flush ()
            descriptions_file.close ()
        return

    # This revision does not change any existing product record
    def _perform_update_internal (self, record, inject_av_record):
        pid = record ['value']['attributes']['pid']
        updated_record = copy.deepcopy (record)
        return updated_record

    # read entire source health&beauty catalog
    def _read_healthandbeauty_source_product_records (self):
        healthandbeauty_source_records = []
        with open (rcv12.FILENAME_SOURCE_PRODUCTS_TSV_IN, 'r') as source_file:
            dict_reader = csv.DictReader (source_file)
            for row in dict_reader:
                healthandbeauty_source_records.append (row)
            source_file.close ()
        return healthandbeauty_source_records

    def _prepare_health_and_beauty_categories (self):
        selected_category_list = []
        for cat_name in rcv12.SELECTED_HEALTH_AND_BEAUTY_PRODUCT_CATEGORIES:
            selected_category_list.append ( {'cat_name': cat_name,
                                             'product_count': 0
                                            }
                                          )
        return selected_category_list

    # if given cat_name is one of the selected categories, return that record
    def _check_if_src_record_selected (self, src_cat_name):
        src_cat_name_lower = src_cat_name.lower ()
        for selected_cat_record in self._selected_health_and_beauty_categories:
            if selected_cat_record ['cat_name'] == src_cat_name_lower:
                return selected_cat_record
        return None

    def _build_additional_product (self, healthandbeauty_src_record, new_product_pid_int):
        logging.debug ('adding new product, pid = %s, title = %s' % (new_product_pid_int, healthandbeauty_src_record ['Product_Name']))
        new_product = copy.deepcopy (rcv12.PACIFICAPPAREL_PRODUCT_RECORD_TEMPLATE)
        new_product_attribs = new_product ['value']['attributes'].keys ()
        for attrib in new_product_attribs:
            match attrib:
                case 'availability': 
                    new_product ['value']['attributes']['availability'] = True
                case 'brand': 
                    new_product ['value']['attributes']['brand'] = healthandbeauty_src_record ['Brand']
                case 'color': 
                    product_color = self._get_product_color (healthandbeauty_src_record)
                    if (product_color != None):
                        new_product ['value']['attributes']['color'] = product_color
                case 'countryOfOrigin':
                        new_product ['value']['attributes']['countryOfOrigin'] = healthandbeauty_src_record ['Country_of_Origin']
                case 'description':
                        new_product ['value']['attributes']['description'] = self._prepare_product_description (healthandbeauty_src_record, new_product_pid_int)
                case 'end_date':
                        new_product ['value']['attributes']['end_date'] = '203012311159'    # same as other products in this catalog
                case 'gender':
                        src_gender = healthandbeauty_src_record ['Gender_Target']
                        if (src_gender == 'Female'):
                            new_product ['value']['attributes']['gender'] = 'female'
                        elif (src_gender == 'Male'):
                            new_product ['value']['attributes']['gender'] = 'male'
                        else:
                            new_product ['value']['attributes']['gender'] = 'unisex'
                case 'margin':
                        margin_range = rcv12.MAX_MARGIN - rcv12.MIN_MARGIN
                        product_margin = (random.random () * margin_range) + rcv12.MIN_MARGIN
                        product_margin = round (product_margin, 1)
                        new_product ['value']['attributes']['margin'] = product_margin
                case 'onSale':
                        pass
                case 'price':
                        # also set sale_price, onSale = True/False
                        new_product ['value']['attributes']['price'] = float (healthandbeauty_src_record ['Price_USD'])
                        # 30% products onSale
                        if (random.random () < 0.3):
                            sale_price = new_product ['value']['attributes']['price'] * rcv12.SALE_PRICE_FACTOR
                            sale_price = round (sale_price, 2)
                            new_product ['value']['attributes']['sale_price'] = sale_price
                            new_product ['value']['attributes']['onSale'] = True
                        else:
                            new_product ['value']['attributes']['sale_price'] = new_product ['value']['attributes']['price']
                            new_product ['value']['attributes']['onSale'] = False
                case 'pid':
                        new_product ['value']['attributes']['pid'] = str (new_product_pid_int)
                case 'product_brand':
                    new_product ['value']['attributes']['product_brand'] = healthandbeauty_src_record ['Brand'].lower ()
                case 'rating':
                    new_product ['value']['attributes']['rating'] = healthandbeauty_src_record ['Rating']
                case 'reviews':
                    new_product ['value']['attributes']['reviews'] = healthandbeauty_src_record ['Number_of_Reviews']
                case 'sale_price':
                    pass
                case 'start_date':
                    new_product ['value']['attributes']['start_date'] = '202401010001' # same as other products in this catalog
                case 'thumb_image':
                    pass
                case 'title':
                    new_product ['value']['attributes']['title'] = healthandbeauty_src_record ['Product_Name']
                case 'url':
                    new_product ['value']['attributes']['url'] = '%s%s___%s' % (uc.PRODUCT_URL_PREFIX, new_product_pid_int, new_product_pid_int)
                case 'material':
                    new_product ['value']['attributes']['material'] = healthandbeauty_src_record ['Main_Ingredient']
                case 'collection':
                    new_product ['value']['attributes']['collection'] = ''
                case 'style':
                    new_product ['value']['attributes']['style'] = ''
                case 'stock_level':
                    new_product ['value']['attributes']['stock_level'] = 'ok'
                case 'season':
                    new_product ['value']['attributes']['season'] = ''
                case 'special_offer':
                    new_product ['value']['attributes']['special_offer'] = 'none'
                case 'size':
                    new_product ['value']['attributes']['size'] = healthandbeauty_src_record ['Product_Size']
                case _: 
                    logging.warning ('Unknown attribute name: %s' % attrib)

        # path
        new_product ['path'] = '%s%s' % ('/products/', new_product_pid_int)

        # category
        cat_name = healthandbeauty_src_record ['Category'].lower ()
        category_paths = self._category_builder.lookup_category_path_for_leaf_name (cat_name)
        if (category_paths != None):
            new_product ['value']['attributes']['category_paths'] = category_paths

        # Unique attributes for health-beauty products
        for attrib in rcv12.HEALTH_AND_BEAUTY_PRODUCT_ATTRIBUTES:
            match attrib:
                case 'packaging':
                    new_product ['value']['attributes']['packaging'] = healthandbeauty_src_record ['Packaging_Type']
                case 'skin_type':
                    new_product ['value']['attributes']['skin_type'] = healthandbeauty_src_record ['Skin_Type']
                case 'usage_frequency':
                    new_product ['value']['attributes']['usage_frequency'] = healthandbeauty_src_record ['Usage_Frequency']
                case _: 
                    logging.warning ('Unknown unique attribute name: %s' % attrib)

        # thumb_image - use various product attributes (eg, description, ...) 
        thumb_image_url = self._prepare_thumb_image (new_product)
        if (thumb_image_url != None):
            new_product ['value']['attributes']['thumb_image'] = thumb_image_url

        return new_product 

    # only some categories have associated color selections
    def _get_product_color (self, healthandbeauty_src_record):
        selected_color = None

        cat_name = healthandbeauty_src_record ['Category'].lower ()
        leaf_id = self._category_builder.lookup_category_id_for_leaf_name (cat_name)
        if (leaf_id != None):
            # get entire cat record - it contains additional data such as product size/fit/color
            cat_map_record = self._category_builder.lookup_category_map_record (leaf_id)
            if (cat_map_record != None):
                available_colors = cat_map_record ['health_and_beauty_colors']
                if (available_colors != None) and (available_colors != ''):
                    color_list = available_colors.split (',')
                    indx = (int) (random.random () * len (color_list))
                    selected_color = color_list [indx].strip ()
        return selected_color

    # generate image, save locally + add aws_upload_record, then return thumb_image_url
    def _prepare_product_description (self, healthandbeauty_src_record, new_product_pid_int):
        # check if we have already generated description earlier
        for record in self._local_product_description_records:
            if (record ['pid'] == str (new_product_pid_int)):
                return record ['product_description']

        description_txt = self._generate_product_description (healthandbeauty_src_record)
        if description_txt != None:
            # add record to local descriptions list
            description_store_record = { 'pid': str (new_product_pid_int),
                                         'product_description': description_txt 
                                       }
            # add img path to be uploaded to AWS
            self._local_product_description_records.append (description_store_record)

        # description txt
        return description_txt 

    def _generate_product_description (self, healthandbeauty_src_record):
        product_brand = healthandbeauty_src_record ['Brand']
        product_title = healthandbeauty_src_record ['Product_Name']
        txt_prompt = 'Prepare description of a product with brand "%s" and title "%s". Keep description text limited to 5 lines.\nDescription should include information about who the product is for, product rating, matching products.' % (product_brand, product_title)

        client = openai.OpenAI (api_key=uc.OPENAI_KEY)
        try:
            openai_response = client.chat.completions.create (
                model = uc.OPENAI_MODEL,
                messages = [
                    {'role': 'system', 'content': 'You are a marketing content writer'},
                    {'role': 'user',   'content': txt_prompt }
                ],
                temperature = 0.3
            )
            resp_text = openai_response.choices [0].message.content
            logging.debug ('gen openai text: %s' % resp_text)
        except Exception as e:
            logging.error ('cannot generate openAI text for %s, error = %s' % (product_text, str(e)))
            resp_text = None
        return resp_text

    # generate image, save locally + add aws_upload_record, then return thumb_image_url
    def _prepare_thumb_image (self, new_product):
        thumb_img_url = None
        pid = new_product ['value']['attributes']['pid']

        # load image and save it locally if we haven't generated image yet
        local_image_store_path = '%s/%s.png' % (uc.THUMB_IMAGE_LOCAL_DIR, pid)
        if (os.path.exists (local_image_store_path) == False):
            gen_img_url = self._generate_thumb_image_url (new_product)
            if gen_img_url == None:
                logging.warning ('Could not generate product thumb_image, pid = %s' % pid)
                return None

            save_stat = self._image_loader.load_image (gen_img_url, local_image_store_path)
            if (save_stat != uc.IMAGE_LOADER_STATUS_SUCCESS):
                logging.warning ('Could not locally save product thumb_image, pid = %s' % pid)
                return None

        # add record to aws_upload list. Local image may have been generated earlier
        # OR newly generated
        img_upload_record = { 'pid': pid,
                              'local_image_path': local_image_store_path
                            }
        # add img path to be uploaded to AWS
        self._aws_image_upload_records.append (img_upload_record)

        # thumb_image url to include in product record
        thumb_img_url = '%s/images/%s/%s_image.%s' % (uc.THUMB_IMAGE_URL_PROLOG, 'hlthbeauty', pid, 'png')
        return thumb_img_url

    def _generate_thumb_image_url (self, product):
        #img_prompt = 'You are creating a product catalog. Create studio shot image of a product whose description is %s\n. Image must not include any text, must be child friendly, must not include any human body part' % (product_description)
        #img_prompt = 'Generate studio image of %s for a cosmetics product catalog. Image must not include any labels. It must be child friendly. It must not include any human body part' % (product_description)
        #img_prompt = """You are a professional photographer. Generate image for a product to be included in a cosmetics catalog. Product details are: \n
        img_prompt = """You are a professional photographer. Generate stock photo with plain white background for a product to be included in a cosmetics catalog. Product details are: \n
                      Product Name: %s, \n 
                      Product Brand: %s, \n
                      Product Packaging: %s, \n
                      Used by: %s, \n
                      Use frequency: %s """  % (product ['value']['attributes']['title'],
                                            product ['value']['attributes']['brand'],
                                            product ['value']['attributes']['packaging'],
                                            product ['value']['attributes']['gender'],
                                            product ['value']['attributes']['usage_frequency'])

        openai.api_key = uc.OPENAI_KEY
        try:
            openai_response = openai.images.generate (
                 model = uc.OPENAI_MODEL_DALL_E,
                 prompt = img_prompt,
                 n = 1,
                 size = '1024x1024',
                 quality = 'standard',
                 style = 'natural'
            )
            img_url = openai_response.data[0].url
            logging.debug ('gen image url: %s' % img_url)
        except Exception as e:
            logging.error ('cannot generate openAI image for %s, error = %s' % (product_text, str(e)))
            img_url = None
        return img_url

    # return total count of new images uploaded
    def _prepare_upload_script (self):
        upload_count = 0
        upload_script_file = open (rcv12.FILENAME_AWS_UPLOAD_SCRIPT_OUT, 'w')

        # initial commands
        upload_script_file.write ('\nset -e\n\n')

        # individual image cp commands
        upload_count = 0
        for img_upload_record in self._aws_image_upload_records:
            aws_s3_cp_command = self._construct_s3_cp_command (img_upload_record)
            if aws_s3_cp_command == None:
                continue    # warning already issued

            upload_script_file.write ('%s\n' % aws_s3_cp_command)
            upload_count = upload_count + 1

        upload_script_file.flush ()
        upload_script_file.close ()
        logging.debug ('Total new images to upload to AWS: %s', upload_count)
        return upload_count

    # return string for s3 cp command
    # eg, aws --profile bloomreach-demo_main s3 cp --acl public-read  ./data/images/<filename> s3://pacific-demo-data.bloomreach.cloud/apparel/images/hlthbeauty/<pid>_image.png
    # NOTE: dress images uploaded to a sub-dir 'hlthbeauty'
    def _construct_s3_cp_command (self, img_upload_record):
        # using pid, first lookup associated src_image_id
        pid = img_upload_record ['pid']
        local_image_path = img_upload_record ['local_image_path']

        # s3 path
        indx = local_image_path.rfind ('.')
        if (indx > 0):
            extension = local_image_path [indx+1:]
        else:
            extension = ''
        s3_file_name = '%s_image.%s' % (pid, extension)  # .../pid_image.<extension>
        s3_path = '%s/%s/%s' % (rcv12.AWS_S3_IMAGES_FOLDER, 'hlthbeauty', s3_file_name)  # sub-folder = hlthbeauty
        aws_cp_command = '%s %s %s' % (rcv12.AWS_CP_COMMAND_PREAMBLE, local_image_path, s3_path)
        logging.debug ('AWS s3 copy command: %s', aws_cp_command)
        return aws_cp_command


if __name__ == '__main__':
    rv = RevisionV12 ()

