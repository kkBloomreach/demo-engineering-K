# V25 changes
# -- generate variant images for different colors using openAI's image.edit api

import logging
import copy
import os
import csv
import random
from PIL import Image
import openai
import base64

from revisionBase import RevisionBase
from imageloader import ImageLoader
import updaterConstants as uc
import revisionConstantsV25 as rcv25

# in this revision, products only in these categories are considered for generating variants
SELECT_CATEGORY_IDS = [
    '10000>10800',   # men > shoes
    '20000>20800',   # women > shoes
    '70000>70100',   # shoes > casual
    '70000>70200',   # shoes > dress
    '70000>70300'    # sneakers
]

IMAGE_EDIT_PROMPT = f"""
Change the primary color in this image to %s, keeping all other details the same.
"""

# sub-folder within 'variants'. 
# Note: for a given category, all variants of all its products are in one s3 bucket
# eg: AWS_S3_IMAGES_FOLDER/<shoes>/variants_new/<variant_image>
SHOES_VARIANTS_IMAGE_SUBFOLDER = 'shoes' 

class RevisionV25 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v25')
        super().__init__ ()
        self._image_loader = ImageLoader ()
        self._aws_image_upload_records = []
        return

    def _initialize (self, source_records, inject_av_map):
        # read adjust-color list
        return True

    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)
        #if (inject_av_record == None):
        #    logging.debug ('No inject attrib_value record for pid: %s', pid)

        # change image only if product is in 'selected' categories
        if self._is_select_category (record) == True:
            updated_record = self._perform_update_internal (record, inject_av_record)
            return updated_record
        else:
            return record
        return updated_record

    def _finalize (self, updated_products):
        if (len (self._aws_image_upload_records) > 0):
            self._prepare_upload_script ()
        else:
            logging.warning ('No images to upload to AWS')
        return True

    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        pid = record ['value']['attributes']['pid']

        default_variant_id = None
        default_swatch_image_url = None
        default_color = None

        # detect default_sku's image-url, color. Note: default-sku-image == product-image
        if ('variants' in record ['value']) and (record ['value']['variants']):
            variant_list = record ['value']['variants']
            for variant_id in variant_list.keys ():
                variant_obj = variant_list [variant_id]
                if variant_obj ['attributes']['default_sku'] == True:
                    default_variant_id = variant_id
                    default_swatch_image_url = variant_obj ['attributes']['swatch_image']
                    default_color = variant_obj ['attributes']['color']
                    break

        if default_variant_id == None or default_swatch_image_url == None:
            return record   # this product has no variants -- should not happen

        updated_record = copy.deepcopy (record)
        available_colors = rcv25.COLOR_CHOICES.copy ()

        # remove the default-color from 'available' list since it is already 
        # used for the default-sku
        try:
            indx = available_colors.index (default_color)
        except Exception as e:
            indx = -1
        if indx >= 0:
            available_colors [indx] = None

        # load the default-sku's webp image once, convert it to .png (because openAI uses that format)
        # then use that .png to generate other variants' images
        local_default_image_file_name_webp = '%s_image.webp' % (default_variant_id)
        local_default_image_path_webp = '%s/%s' % (rcv25.THUMB_IMAGE_LOCAL_DIR, local_default_image_file_name_webp)
        load_status = self._image_loader.load_image (default_swatch_image_url, local_default_image_path_webp)
        if load_status == uc.IMAGE_LOADER_STATUS_SUCCESS:
            # convert default webp to png because openAI api requires png
            local_default_image_path_png = '%s/%s.png' % (rcv25.THUMB_IMAGE_LOCAL_DIR, default_variant_id)
            if self._convert_webp_to_png (local_default_image_path_webp, local_default_image_path_png) == False:
                logging.error ('Could not convert default webp image to png: %s' % default_variant_id)
                return updated_record

        # go thru all variants for this product and re-generate swatch_image 
        # with different colors (except default-sku) using image.edit api
        variant_list = updated_record ['value']['variants']
        for variant_id in variant_list.keys():
            variant_obj = variant_list [variant_id]

            if variant_id == default_variant_id:
                # copy same webp to 'variants_new' s3 bucket
                # load original image to local disk
                img_upload_record = { 'variant_id': variant_id,
                                      'local_image_path': local_default_image_path_webp,
                                      's3_file_name': local_default_image_file_name_webp
                                    }
                self._aws_image_upload_records.append (img_upload_record)

                # new variant-image url to set in catalog
                swatch_image_url = '%s/%s/variants_new/%s' % (rcv25.THUMB_IMAGE_URL_PROLOG, 
                                                              SHOES_VARIANTS_IMAGE_SUBFOLDER,
                                                              local_default_image_file_name_webp)
                variant_obj ['attributes']['swatch_image'] = swatch_image_url

                # also set the same as product's own thumb_image == dafault-sku's swatch-image
                updated_record ['value']['attributes']['thumb_image'] = swatch_image_url
            else:
                # generates only if not already done...
                local_image_new_path_webp = self._generate_swatch_image_with_different_color (variant_id, local_default_image_path_png, available_colors)
                if local_image_new_path_webp != None:
                    # add record to aws_upload list. 
                    s3_file_name = '%s_image.webp' % (variant_id)  # .../variant_id_image.webp
                    img_upload_record = { 'variant_id': variant_id,
                                          'local_image_path': local_image_new_path_webp,
                                          's3_file_name': s3_file_name
                                        }
                    self._aws_image_upload_records.append (img_upload_record)

                    # new variant-image url to set in catalog
                    new_variant_swatch_image_url = '%s/%s/variants_new/%s' % (rcv25.THUMB_IMAGE_URL_PROLOG, 
                                                                              SHOES_VARIANTS_IMAGE_SUBFOLDER,
                                                                              s3_file_name)
                    variant_obj ['attributes']['swatch_image'] = new_variant_swatch_image_url
                else:
                    logging.warning ('Could not generate swatch image with different color: %s' % variant_id)
        # clean ups
        if local_default_image_path_png != None:
            os.remove (local_default_image_path_png)

        return updated_record

    def _is_select_category (self, record):
        category_paths = record ['value']['attributes']['category_paths']
        for branch in category_paths:
            full_path = None 
            for leaf_node in branch:
                if full_path == None:
                    full_path = leaf_node ['id']
                else:
                    full_path = '%s>%s' % (full_path, leaf_node ['id'])
            if full_path in SELECT_CATEGORY_IDS:
                return True
        return False

    # once a color is selected, set its value = None
    def _select_uniq_color (self, color_list):
        select_color = None
        while select_color == None:
            color_indx = int (random.random () * (len (color_list)))
            select_color = color_list [color_indx]
            if select_color != None:
                color_list [color_indx] = None
        return (select_color, color_list) 

    # returns local webp path for new image
    def _generate_swatch_image_with_different_color (self, variant_id, local_default_image_path_png, available_colors):
        local_image_new_path_webp = None

        # if we have not generated another image already, do so now 
        # (this check is mainly to avoid undue cost of openAI api call)
        local_image_file_name_new_webp = '%s_image_new.webp' % (variant_id)
        local_image_new_path_webp = '%s/%s' % (rcv25.THUMB_IMAGE_LOCAL_DIR, local_image_file_name_new_webp)
        if os.path.exists (local_image_new_path_webp) == False:
            # use openAPI api to generate another image with select_color
            # select a uniq color at random
            select_color, available_colors = self._select_uniq_color (available_colors)
            img_bytes = self._get_openAI_variant_image_bytes (local_default_image_path_png, select_color)
            if img_bytes != None and len (img_bytes) > 0:
                # save new image bytes locally to file
                local_image_new_path_png  = '%s/%s_new.png' % (rcv25.THUMB_IMAGE_LOCAL_DIR, variant_id)
                with open (local_image_new_path_png, 'wb') as local_image_file_new_png:
                    local_image_file_new_png.write (img_bytes)
                    local_image_file_new_png.flush ()
                    local_image_file_new_png.close ()

                if local_image_new_path_png != None:
                    # convert this new_png to webp
                    local_image_file_name_new_webp = '%s_image_new.webp' % (variant_id)
                    local_image_new_path_webp = '%s/%s' % (rcv25.THUMB_IMAGE_LOCAL_DIR, local_image_file_name_new_webp)
                    if self._convert_png_to_webp (local_image_new_path_png, local_image_new_path_webp) == False:
                        logging.error ('Could not convert .png to .webp: %s' % local_image_new_path_webp)
                        local_image_new_path_webp = None

                    # clean up
                    os.remove (local_image_new_path_png)
                else:
                    logging.error ('Could not save .png : %s' % local_image_new_path_png)

        return local_image_new_path_webp

    # generate new image bytes using openAI.image.edit, using select_color. 
    # response bytes are png format
    def _get_openAI_variant_image_bytes (self, product_image, select_color):
        openai.api_key = rcv25.OPENAI_KEY
        actual_prompt = IMAGE_EDIT_PROMPT % select_color
        try:
            openai_response = openai.images.edit (
                 model = rcv25.OPENAI_MODEL_IMAGE_GENERATION,
                 image = open (product_image, 'rb'),
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
        upload_script_file = open (rcv25.FILENAME_AWS_UPLOAD_SCRIPT_OUT, 'w')

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
        s3_file_name = img_upload_record ['s3_file_name']

        s3_path = '%s/%s/variants_new/%s' % (rcv25.AWS_S3_GEN_GPTIMAGE_1_IMAGES_FOLDER, 
                                             SHOES_VARIANTS_IMAGE_SUBFOLDER, 
                                             s3_file_name) 
        aws_cp_command = '%s %s %s' % (rcv25.AWS_CP_COMMAND_PREAMBLE, local_image_path, s3_path)
        logging.debug ('AWS s3 copy command: %s', aws_cp_command)
        return aws_cp_command

    # return string for s3 rm command
    # eg, aws --profile bloomreach-demo_main s3 rm s3://pacific-demo-data.bloomreach.cloud/apparel/images/webp/gen/<category>/variants/file_name
    # NOTE: images uploaded to a sub-dir 
    def _construct_s3_rm_command (self, img_upload_record):
        # using variant_id, first lookup associated src_image_id
        variant_id = img_upload_record ['variant_id']
        local_image_path = img_upload_record ['local_image_path']
        s3_file_name = img_upload_record ['s3_file_name']

        s3_path = '%s/%s/variants_new/%s' % (rcv25.AWS_S3_GEN_GPTIMAGE_1_IMAGES_FOLDER, 
                                             SHOES_VARIANTS_IMAGE_SUBFOLDER, 
                                             s3_file_name)
        aws_rm_command = '%s %s' % (rcv25.AWS_RM_COMMAND_PREAMBLE, s3_path)
        logging.debug ('AWS s3 rm command: %s', aws_rm_command)
        return aws_rm_command

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV25 ()
    logging.info ('RevisionV25 finish...')


