# V15 changes
# -- collect pids for selected search terms and category ids
# -- generate variants for each pid if needed
# -- generate default variant images for each variant
# -- generate variant images for different colors using openAI's image.edit api

import logging
import copy
import os
import csv
import random
import re
from PIL import Image
import openai
import base64

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV15 as rcv15
from search_response_collector import SearchResponseCollector

# prompt used to generate default-variant-image
PROMPT_HEADER = """
Generate a premium, high-resolution product image for a retail store. The product should be the sole focal point, presented against a clean, white backdrop that eliminates distractions and reinforces a modern, upscale aesthetic. Lighting should be soft. Do not include any text or labels in the image. The image must feel refined and editorial—ideal for showcasing the product on a high-end retail website so that shoppers will be enticed to purchase.
"""

# image-edit prompt to generate variants with different color
IMAGE_EDIT_PROMPT = f"""
Change the primary color in this image to %s, keeping all other details the same. Keep the backdrop clean, white color that eliminates distractions and reinforces a modern, upscale aesthetic.
"""

class RevisionV15 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v15')
        super().__init__ ()
        self._aws_image_upload_records = []

        # each generated image's info saved so that we don't have
        # to regenerate 
        self._variant_color_swatchurl_map = [] # {variant_id, color, swatch_url}

        # selected pids for which variants are generated
        self._selected_pids = []
        return

    def _initialize (self, source_records, inject_av_map):
        # read previously created variant-color-swatchurl map, if any
        self._variant_color_swatchurl_map = self._read_variant_color_swatchurl_map ()

        # collect pids for which to generate variants
        # for selected search terms
        collector = SearchResponseCollector ()
        terms_list = ['dinnerware', 'bowls', 'bath towels', 'throws', 'blankets']
        for term in rcv15.SELECT_SEARCH_TERMS:
            response_pids = collector.collect_search_response (term)
            logging.debug ('query: %s, pids count: %s' % (term, len (response_pids)))
            for pid in response_pids:
                if pid not in self._selected_pids:
                    self._selected_pids.append (str (pid))

        # also from selected categories
        source_records = self._source_records
        for record in source_records:
            if self._is_select_category (record):
                pid = record ['value']['attributes']['pid']
                if pid not in self._selected_pids:
                    self._selected_pids.append (str (pid))

        logging.debug ('Total pid count to generate variants: %s' % len (self._selected_pids))
        return True

    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)
        #if (inject_av_record == None):
        #    logging.debug ('No inject attrib_value record for pid: %s', pid)

        # change image only if product is in 'selected' pid list
        if pid in self._selected_pids:
            updated_record = self._perform_update_internal (record, inject_av_record)
            return updated_record
        else:
            return record

    def _finalize (self, updated_products):
        if (len (self._aws_image_upload_records) > 0):
            self._prepare_upload_script ()
        else:
            logging.warning ('No images to upload to AWS')

        # locally save variant-color-url map
        if (len (self._variant_color_swatchurl_map) > 0):
            self._save_variant_color_swatchurl_map ()
        else:
            logging.warning ('No variant-color-url map')
        return True

    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        pid = record ['value']['attributes']['pid']

        updated_record = copy.deepcopy (record)

        # create variants if not already done
        if 'variants' in updated_record ['value']:
            variants_list = updated_record ['value']['variants'].keys ()
            if len (variants_list) <= 1:
                self._generate_variants (updated_record)

        # find default variant. 
        # earlier had a bug where some pids had variants but 'default' was not set
        default_variant_id = self._find_default_variant (updated_record)
        if default_variant_id == None:
            # this case is bug in prior version. Force one of the 
            # variants to be 'default' and continue
            variant_list = record ['value']['variants']
            indx = int (random.random() * len (variant_list))
            default_variant_id = '%s_%s' % (pid, indx)
            logging.warning ('product %s has no default-variant. Setting %s variant as default.' % (pid, default_variant_id))
            variant_list [default_variant_id]['attributes']['default_sku'] = True

        # if default-variant-image already exists, means all its other variant
        # images also exist. Add all those to aws_upload script and update record attributes
        local_default_image_file_name_webp = '%s_image.webp' % (default_variant_id)
        local_default_image_path_webp = '%s/%s' % (rcv15.THUMB_IMAGE_LOCAL_DIR, local_default_image_file_name_webp)
        if (os.path.exists (local_default_image_path_webp) == True):
            logging.debug ('add all existing variant images to aws_upload, pid: %s' % pid)
            self._handle_preexisting_images (updated_record)
            return updated_record

        # generate new images 
        self._generate_new_images (updated_record, default_variant_id)

        return updated_record

    def _generate_new_images (self, updated_record, default_variant_id):
        logging.debug ('Creating images for pid = %s and its variants.' % updated_record ['value']['attributes']['pid'])

        available_colors = rcv15.COLOR_CHOICES.copy ()

        # default variant's image, color
        local_default_image_path_png, selected_color, available_colors = self._generate_default_variant_image_png (updated_record, default_variant_id, available_colors)
        if (local_default_image_path_png == None):
            logging.error ('Could not generate image for default: %s' % default_variant_id)
            return updated_record

        local_default_image_file_name_webp = '%s_image.webp' % (default_variant_id)
        local_default_image_path_webp = '%s/%s' % (rcv15.THUMB_IMAGE_LOCAL_DIR, local_default_image_file_name_webp)
        logging.debug ('variant_id: %s, variant color: %s, local image path: %s' % (default_variant_id, selected_color, local_default_image_path_webp))
        if self._convert_png_to_webp (local_default_image_path_png, local_default_image_path_webp) == False:
            logging.error ('Could not convert default png image to webp: %s' % default_variant_id)
            return updated_record

        # add default-img-path to aws-upload list and set upload_record->urls, color
        s3_file_relative_path = self._construct_s3_file_relative_path (updated_record, local_default_image_file_name_webp)
        img_upload_record = { 'variant_id': default_variant_id,
                              'local_image_path': local_default_image_path_webp,
                              's3_file_relative_path': s3_file_relative_path
                            }
        self._aws_image_upload_records.append (img_upload_record)

        # default variant-image url to set in catalog
        swatch_image_url = '%s/%s' % (rcv15.THUMB_IMAGE_URL_PROLOG, s3_file_relative_path)
        updated_record ['value']['variants'][default_variant_id]['attributes']['swatch_image'] = swatch_image_url
        updated_record ['value']['variants'][default_variant_id]['attributes']['color'] = selected_color
        # also set the same as product's own thumb_image == dafault-sku's swatch-image
        updated_record ['value']['attributes']['thumb_image'] = swatch_image_url
        updated_record ['value']['attributes']['large_image'] = swatch_image_url

        variant_data_map = { 'variant_id': default_variant_id,
                             'color': selected_color,
                             'swatch_image': swatch_image_url
                           }
        self._variant_color_swatchurl_map.append (variant_data_map)

        # go thru all other variants for this product and re-generate swatch_image 
        # with different colors using image.edit api
        variant_list = updated_record ['value']['variants']
        for variant_id in variant_list.keys():
            if variant_id == default_variant_id:
                continue

            local_image_path_webp, selected_color, available_colors = self._generate_swatch_image_with_different_color (variant_id, local_default_image_path_png, available_colors)
            if local_image_path_webp != None:
                logging.debug ('variant_id: %s, variant color: %s, local image path: %s' % (variant_id, selected_color, local_image_path_webp))
                # add record to aws_upload list.
                file_base_name_webp = '%s_image.webp' % (variant_id)  # .../variant_id_image.webp
                s3_file_relative_path = self._construct_s3_file_relative_path (updated_record, file_base_name_webp)
                img_upload_record = { 'variant_id': variant_id,
                                      'local_image_path': local_image_path_webp,
                                      's3_file_relative_path': s3_file_relative_path
                                    }
                self._aws_image_upload_records.append (img_upload_record)

                # new variant-image url to set in catalog
                swatch_image_url = '%s/%s' % (rcv15.THUMB_IMAGE_URL_PROLOG, s3_file_relative_path)
                updated_record ['value']['variants'][variant_id]['attributes']['swatch_image'] = swatch_image_url
                updated_record ['value']['variants'][variant_id]['attributes']['color'] = selected_color

                variant_data_map = { 'variant_id': variant_id,
                                     'color': selected_color,
                                     'swatch_image': swatch_image_url
                                   }
                self._variant_color_swatchurl_map.append (variant_data_map)
            else:
                logging.warning ('Could not generate swatch image with different color: %s' % variant_id)

        # clean ups
        os.remove (local_default_image_path_png)
        return 

    def _is_select_category (self, record):
        category_paths = record ['value']['attributes']['category_paths']
        for branch in category_paths:
            full_path = None 
            for leaf_node in branch:
                if full_path == None:
                    full_path = leaf_node ['id']
                else:
                    full_path = '%s>%s' % (full_path, leaf_node ['id'])
            if full_path in rcv15.SELECT_CATEGORY_IDS:
                return True
        return False

    # if a selected pid does not have variants (>1), generate them now
    def _generate_variants (self, updated_record):
        updated_record ['value']['variants'] = {}

        count_variants_to_add = max (int (random.random () * (rcv15.MAX_VARIANTS_PER_PRODUCT)), rcv15.MIN_VARIANTS_PER_PRODUCT)
        available_colors = rcv15.COLOR_CHOICES.copy ()
        for count in range (0, count_variants_to_add):
            variant_id = '%s_%s' % (updated_record ['value']['attributes']['pid'], count)

            # add new variant
            logging.debug ('Creating variant for pid: %s' % updated_record ['value']['attributes']['pid'])
            # variant_id: <pid>_<count> (EG, 123321_1)
            updated_record ['value']['variants'][variant_id] = {}
            updated_record ['value']['variants'][variant_id]['attributes'] = {}
            updated_record ['value']['variants'][variant_id]['attributes']['skuid'] = variant_id
            updated_record ['value']['variants'][variant_id]['attributes']['color'] = '?'           # color and image set later
            updated_record ['value']['variants'][variant_id]['attributes']['swatch_image'] = '?'    # image generated later
            updated_record ['value']['variants'][variant_id]['attributes']['default_sku'] = False   # adjusted below
            updated_record ['value']['variants'][variant_id]['attributes']['availability'] = True   # all variants 'available'

        # having created multiple variants, pick one of those as 'default'
        default_variant_num = int (random.random () * count_variants_to_add)
        default_variant_id = '%s_%s' % (updated_record ['value']['attributes']['pid'], default_variant_num)
        updated_record ['value']['variants'][default_variant_id]['attributes']['default_sku'] = True 

        # set price, sale_price of default_variant = product price, sale_price
        default_variant_price = updated_record ['value']['attributes']['price'] # used below
        default_variant_sale_price = updated_record ['value']['attributes']['sale_price'] # used below
        updated_record ['value']['variants'][default_variant_id]['attributes']['price'] = default_variant_price
        updated_record ['value']['variants'][default_variant_id]['attributes']['sale_price'] = default_variant_sale_price

        # set product's thumb_image same as default_variant's swatch_image
        updated_record ['value']['attributes']['thumb_image'] = '?' # thumb_image set later
        updated_record ['value']['attributes']['large_image'] = '?'

        # for SOME (40%) of the products in catalog, non-default-variants can have different price/sale_price.
        # skip if product has only one variant
        if count_variants_to_add > 1:
            rand = int (random.random () * 100)
            if (rand < 40):
                product_has_discounted_variants = True
            else:
                product_has_discounted_variants = False

            print ('default variant num: %s' % default_variant_num)
            for this_variant_num in range (0, count_variants_to_add):
                if this_variant_num != default_variant_num:  # variant other-than-default
                    print ('\tthis variant num: %s' % this_variant_num)
                    this_variant_id = '%s_%s' % (updated_record ['value']['attributes']['pid'], this_variant_num)
                    if product_has_discounted_variants:
                        # this product's variants have different prices for non-default variants
                        discount_factor = 1 - (random.random () * 0.12)  # variant price = default_price - {0 -> 0.2}
                        print ('discount factor: %s' % discount_factor)
                    else:
                        discount_factor = 1.0

                    this_variant_price = default_variant_price * discount_factor
                    this_variant_sale_price = default_variant_sale_price * discount_factor
                    print ('price: %s, sale_price: %s' % (this_variant_price, this_variant_sale_price))
                    updated_record ['value']['variants'][this_variant_id]['attributes']['price'] = round (this_variant_price, 2)
                    updated_record ['value']['variants'][this_variant_id]['attributes']['sale_price'] = round (this_variant_sale_price, 2)
        return

    def _find_default_variant (self, record):
        default_variant_id = None

        # detect default_sku
        if ('variants' in record ['value']) and (record ['value']['variants']):
            variant_list = record ['value']['variants']
            for variant_id in variant_list.keys ():
                variant_obj = variant_list [variant_id]
                if variant_obj ['attributes']['default_sku'] == True:
                    default_variant_id = variant_id
                    break

        return default_variant_id

    # if default-variant image already exists, 
    # update catalog attributes and add variant paths to aws_upload list
    def _handle_preexisting_images (self, updated_record):
        variant_list = updated_record ['value']['variants']
        for variant_id in variant_list.keys():
            variant_color_swatchurl_record = self._lookup_variant_color_swatchurl_map (variant_id)
            if variant_color_swatchurl_record != None:
                file_base_name = '%s_image.webp' % (variant_id)  # .../variant_id_image.webp
                local_image_path_webp = '%s/%s' % (rcv15.THUMB_IMAGE_LOCAL_DIR, file_base_name)
                s3_file_relative_path = self._construct_s3_file_relative_path (updated_record, file_base_name)
                if os.path.exists (local_image_path_webp) == True:
                    img_upload_record = { 'variant_id': variant_id,
                                          'local_image_path': local_image_path_webp,
                                          's3_file_relative_path': s3_file_relative_path 
                                        }
                    self._aws_image_upload_records.append (img_upload_record)

                    # update record attribs
                    updated_record ['value']['variants'][variant_id]['attributes']['color'] = variant_color_swatchurl_record ['color']

                    # image url (previous url has changed in this revision)
                    swatch_image_url = '%s/%s' % (rcv15.THUMB_IMAGE_URL_PROLOG, s3_file_relative_path)
                    variant_color_swatchurl_record ['swatch_image'] = swatch_image_url

                    updated_record ['value']['variants'][variant_id]['attributes']['swatch_image'] = variant_color_swatchurl_record ['swatch_image']
                    # if this variant is 'default_sku', also update product->thumb_image
                    if updated_record ['value']['variants'][variant_id]['attributes']['default_sku'] == True:
                        updated_record ['value']['attributes']['thumb_image'] = variant_color_swatchurl_record ['swatch_image']
                        updated_record ['value']['attributes']['large_image'] = variant_color_swatchurl_record ['swatch_image']
                else:
                    logging.warning ('Image not available to upload for variant_id: %s' % variant_id)
            else:
                logging.warning ('cannot find variant_color_swatchurl map for variant_id: %s' % variant_id)
        return

    # use first category branch to construct s3 bucket relative_path
    def _construct_s3_file_relative_path (self, updated_record, file_base_name):
        category_paths = updated_record ['value']['attributes']['category_paths']
        branch_0 = category_paths [0]
        full_branch_path = None
        for leaf_node in branch_0:
            if full_branch_path == None:
                full_branch_path = '%s' % (leaf_node ['name'])
            else:
                full_branch_path = '%s/%s' % (full_branch_path, leaf_node ['name'])

        full_branch_path = re.sub (r"(\s&\s)", ' ', full_branch_path)    # replace <blank>&<blank> to blank
        full_branch_path = re.sub (r"[$,']", '', full_branch_path)    # remove special chars
        full_branch_path = full_branch_path.replace (' ', '_')        # replace blank -> '_'
        full_branch_path = full_branch_path.lower ()
        full_s3_relative_path = '%s/%s/%s' % ('variants', full_branch_path, file_base_name)
        return full_s3_relative_path

    # once a color is selected, set its value = None
    def _select_uniq_color (self, color_list):
        select_color = None
        while select_color == None:
            color_indx = int (random.random () * (len (color_list)))
            select_color = color_list [color_indx]
            if select_color != None:
                color_list [color_indx] = None
        return (select_color, color_list) 

    # variant_id = <pid>_<num> (EG, 123143_1)
    # Note: 'size' is not used to generate image
    # generates a new png 
    # returns png-path, associated image color value and available_colors list
    def _generate_default_variant_image_png (self, updated_record, default_variant_id, available_colors):
        selected_color, available_colors = self._select_uniq_color (available_colors)
        img_bytes = self._generate_default_variant_image_bytes (updated_record, selected_color)
        if (img_bytes != None) and (len (img_bytes) > 0):
            file_base_name_png = '%s_image.png' % (default_variant_id)  # .../variant_id_image.png
            local_image_store_path_png = '%s/%s' % (rcv15.THUMB_IMAGE_LOCAL_DIR, file_base_name_png)
            with open (local_image_store_path_png, 'wb') as local_image_file_png:
                local_image_file_png.write (img_bytes)
                local_image_file_png.flush ()
                local_image_file_png.close ()
        else:
            logging.warning ('Could not generate image for a pid: %s, variant: %s' % (updated_record ['value']['attributes']['pid'], default_variant_id))
            local_image_store_path_png = None
            selected_color = None
        return local_image_store_path_png, selected_color, available_colors

    # generate DEFAULT variant image bytes - it is then used to generate additional variants with color-change
    def _generate_default_variant_image_bytes (self, updated_record, select_color):
        # for purpose of generating image, add 'color' in title and description
        title_txt = '%s. Use %s color to generate image.' % (updated_record ['value']['attributes']['title'], select_color)
        description_txt = '%s. Use %s color to generate image.' % (updated_record ['value']['attributes']['description'], select_color)
        product_details = """
                      Product Name: %s, \n
                      Product Brand: %s, \n
                      Product Description: %s, \n
                      Product Color: %s """  % (
                                                    title_txt,
                                                    updated_record ['value']['attributes']['brand'],
                                                    description_txt,
                                                    select_color
                                               )
        img_prompt = "%s\n%s" % (PROMPT_HEADER, product_details)
        openai.api_key = rcv15.OPENAI_KEY
        try:
            openai_response = openai.images.generate (
                 model = rcv15.OPENAI_MODEL_IMAGE_GENERATION,
                 prompt = img_prompt,
                 n = 1,
                 size = '1024x1024',
                 quality = 'high',
                 #style = 'natural' -- not supported by this model
            )
            img_bytes = base64.b64decode (openai_response.data[0].b64_json)
            logging.debug ('gen image byte count: %s' % len (img_bytes))
        except Exception as e:
            logging.error ('cannot generate openAI image for %s, error = %s' % (updated_record ['value']['attributes']['pid'], str(e)))
            img_bytes  = None
        return img_bytes

    # given default-variant's png, generate another variant's image with different color
    # returns local webp path for new image, selected_color, available_colors
    def _generate_swatch_image_with_different_color (self, variant_id, local_default_image_path_png, available_colors):
        local_image_path_webp = None
        selected_color = None

        # use openAPI api to generate another image with select_color
        # select a uniq color at random
        selected_color, available_colors = self._select_uniq_color (available_colors)
        img_bytes = self._get_openAI_variant_image_bytes_with_different_color (local_default_image_path_png, selected_color)
        if img_bytes != None and len (img_bytes) > 0:
            # save new image bytes locally to file
            local_image_path_png  = '%s/%s.png' % (rcv15.THUMB_IMAGE_LOCAL_DIR, variant_id)
            with open (local_image_path_png, 'wb') as local_image_file_png:
                local_image_file_png.write (img_bytes)
                local_image_file_png.flush ()
                local_image_file_png.close ()

            # convert this new_png to webp
            local_image_file_name_webp = '%s_image.webp' % (variant_id)
            local_image_path_webp = '%s/%s' % (rcv15.THUMB_IMAGE_LOCAL_DIR, local_image_file_name_webp)
            if self._convert_png_to_webp (local_image_path_png, local_image_path_webp) == False:
                logging.error ('Could not convert .png to .webp: %s' % local_image_path_webp)
                local_image_path_webp = None

            # clean up
            os.remove (local_image_path_png)
        else:
            logging.error ('Could not generate .png for variant: %s' % variant_id)
        return local_image_path_webp, selected_color, available_colors

    # generate new image bytes using openAI.image.edit, using select_color. 
    # response bytes are png format
    def _get_openAI_variant_image_bytes_with_different_color (self, product_image_path_png, select_color):
        openai.api_key = rcv15.OPENAI_KEY
        actual_prompt = IMAGE_EDIT_PROMPT % select_color
        try:
            openai_response = openai.images.edit (
                 model = rcv15.OPENAI_MODEL_IMAGE_GENERATION,
                 image = open (product_image_path_png, 'rb'),
                 prompt = actual_prompt,
                 size = '1024x1024'
            )
            img_bytes = base64.b64decode (openai_response.data[0].b64_json)
            logging.debug ('gen image byte count: %s' % len (img_bytes))
        except Exception as e:
            logging.error ('cannot generate openAI variant image error %s' % str(e))
            img_bytes  = None
        return img_bytes

    def _convert_webp_to_png (self, local_image_path_webp: str, local_image_path_png: str) -> bool:
        try:
            image = Image.open (local_image_path_webp)
            image.save (local_image_path_png, "png")
            op_stat = True
        except Exception as e:
            logging.warning ('Exception in converting webp to png: %s' % local_image_path_webp)
            op_stat = False
        return op_stat

    def _convert_png_to_webp (self, local_image_path_png: str, local_image_path_webp: str) -> bool:
        try:
            image = Image.open (local_image_path_png)
            image.save (local_image_path_webp, "webp")
            op_stat = True
        except Exception as e:
            logging.warning ('Exception in converting png to webp: %s' % local_image_path_png)
            op_stat = False
        return op_stat

    def _prepare_upload_script (self):
        upload_count = 0
        upload_script_file = open (rcv15.FILENAME_AWS_UPLOAD_SCRIPT_OUT, 'w')

        # initial commands
        upload_script_file.write ('\nset -e\n\n')
        upload_script_file.write ('\ndate\n\n') # start-date

        # individual image cp commands
        upload_count = 0
        for img_upload_record in self._aws_image_upload_records:
            # rm previous image from s3
            aws_s3_rm_command = self._construct_s3_rm_command (img_upload_record)

            # upload new image to s3
            aws_s3_cp_command = self._construct_s3_cp_command (img_upload_record)
            if aws_s3_cp_command == None:
                continue    # warning already issued

            upload_script_file.write ('%s\n' % aws_s3_rm_command)
            upload_script_file.write ('%s\n\n' % aws_s3_cp_command)
            upload_count = upload_count + 1

        upload_script_file.write ('\ndate\n\n') # end date
        upload_script_file.flush ()
        upload_script_file.close ()
        logging.debug ('Total new images to upload to AWS: %s', upload_count)
        return upload_count

    # return string for s3 cp command
    # eg, aws --profile bloomreach-demo_main s3 cp --acl public-read \
    # ./data/images/<filename> 
    # s3://pacific-demo-data.bloomreach.cloud/home/images/webp/gen/<subfolder>/variants/variantid_image.webp
    def _construct_s3_cp_command (self, img_upload_record):
        # using variant_id, first lookup associated src_image_id
        variant_id = img_upload_record ['variant_id']
        local_image_path = img_upload_record ['local_image_path']
        s3_file_relative_path = img_upload_record ['s3_file_relative_path']

        s3_path = '%s/%s' % (rcv15.AWS_S3_GEN_GPTIMAGE_1_IMAGES_FOLDER, s3_file_relative_path) 
        aws_cp_command = '%s %s %s' % (rcv15.AWS_CP_COMMAND_PREAMBLE, local_image_path, s3_path)
        logging.debug ('AWS s3 copy command: %s', aws_cp_command)
        return aws_cp_command

    # return string for s3 rm command
    # eg, aws --profile bloomreach-demo_main s3 rm s3://pacific-demo-data.bloomreach.cloud/apparel/images/webp/gen/<category>/variants/file_name
    # NOTE: images uploaded to a sub-dir 
    def _construct_s3_rm_command (self, img_upload_record):
        # using variant_id, first lookup associated src_image_id
        variant_id = img_upload_record ['variant_id']
        local_image_path = img_upload_record ['local_image_path']
        s3_file_relative_path = img_upload_record ['s3_file_relative_path']

        s3_path = '%s/%s' % (rcv15.AWS_S3_GEN_GPTIMAGE_1_IMAGES_FOLDER, 
                                      s3_file_relative_path)
        aws_rm_command = '%s %s' % (rcv15.AWS_RM_COMMAND_PREAMBLE, s3_path)
        logging.debug ('AWS s3 rm command: %s', aws_rm_command)
        return aws_rm_command

    def _read_variant_color_swatchurl_map (self):
        variant_color_swatchurl_map = [] # blank list, not None

        if os.path.exists (rcv15.FILENAME_VARIANT_COLOR_SWATCHURL_MAP_TSV_OUT) == False:
            return variant_color_swatchurl_map
        with open (rcv15.FILENAME_VARIANT_COLOR_SWATCHURL_MAP_TSV_OUT, 'r') as input_file:
            tsv_reader = csv.DictReader (input_file, delimiter='\t')
            for row in tsv_reader:
                variant_color_swatchurl_map.append (row)
            input_file.close ()
        return variant_color_swatchurl_map

    def _lookup_variant_color_swatchurl_map (self, variant_id):
        if len (self._variant_color_swatchurl_map) == 0:
            return None

        for variant_color_swatchurl_record in self._variant_color_swatchurl_map:
            if variant_color_swatchurl_record ['variant_id'] == variant_id:
                return variant_color_swatchurl_record
        return None

    def _save_variant_color_swatchurl_map (self):
        with open (rcv15.FILENAME_VARIANT_COLOR_SWATCHURL_MAP_TSV_OUT, 'w') as output_file:
            tsv_writer = csv.writer (output_file, delimiter='\t')

            header_line = self._variant_color_swatchurl_map[0].keys()
            tsv_writer.writerow (header_line)
            for row in self._variant_color_swatchurl_map:
                tsv_writer.writerow (row.values())
            output_file.flush ()
            output_file.close ()
        return

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV15 ()
    logging.info ('RevisionV15 finish...')



