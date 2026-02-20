# V7 changes
# -- Change images of selected products

import logging
import os
import copy
import csv
import base64
import openai
from PIL import Image

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV7 as rcv7

PROMPT_HEADER = """
Generate a premium, high-resolution product image for a retail store. The product should be the sole focal point, presented against a clean, white backdrop that eliminates distractions and reinforces a modern, upscale aesthetic. Lighting should be soft. Do not include any text or labels in the image. The image must feel refined and editorial—ideal for showcasing the product on a high-end retail website so that shoppers will be enticed to purchase.
"""

class RevisionV7 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v7')
        super().__init__ ()
        self._selected_products = []
        self._aws_image_upload_records = []
        return

    def _initialize (self, source_records, inject_av_map):
        self._selected_products = self._read_selected_product_list (rcv7.FILENAME_SELECTED_PRODUCT_LIST)
        if (self._selected_products == None) or (len (self._selected_products) == 0):
            logging.error ('Cannot find selected product list to change images')
            return False
        return True
 
    # override base class method
    # category_manager not needed in this revision
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']

        if self._is_product_in_selected_list (pid) == True:
            logging.debug ('Changing image for pid: %s' % pid)
            inject_av_record = super()._lookup_inject_av_record (pid)
            if (inject_av_record == None):
                logging.debug ('No inject attrib_value record for pid: %s', pid)

            updated_record = self._perform_update_internal (record, inject_av_record)
            return updated_record
        else:
            return record # no change

    def _finalize (self, updated_products):
        if (len (self._aws_image_upload_records) > 0):
            self._prepare_upload_script ()
        else:
            logging.warning ('No images to upload to AWS')
        return True
 
    # INTERNAL METHODS
    def _read_selected_product_list (self, filepath) :
        logging.info ("reading tabular source: " + filepath)
        tabular_records = []
        if os.path.exists (filepath):
            with open (filepath, 'r') as file_obj:
                dict_reader = csv.DictReader (file_obj, delimiter='\t')
                for row in dict_reader:
                    # detect pid from 'image link' column and include it in row object
                    # link values are: .../<pid>
                    link_url = row ['Current image link']
                    rindx = link_url.rindex ('/')
                    pid = link_url [rindx+1:]
                    row ['pid'] = pid 
                    tabular_records.append (row)
                file_obj.close ()
            logging.info ("tabular record count: %s", len (tabular_records))
        else:
            logging.error ('cannot find tabular source: %s', filepath)
        return tabular_records

    def _is_product_in_selected_list (self, pid) :
        for selected_record in self._selected_products:
            if selected_record ['pid'] == pid:
                return True
            elif pid.find ('fam') > 0:
                # some pids in shopify catalog have 'fam' removed from the original pid value 
                # ie, original: 1111fam, shopify: 1111
                indx = pid.find ('fam')
                pid_sans_fam = pid [0:indx]
                if selected_record ['pid'] == pid_sans_fam:
                    return True
        return False

    def _perform_update_internal (self, record, inject_av_record):
        # check if product is to be deleted
        pid = record ['value']['attributes']['pid']

        updated_record = copy.deepcopy (record)

        # use ai image generator to generate new image. Actual url in the
        # record itself is not changed - only new image is uploaded to s3
        self._generate_thumb_image (updated_record)
        return updated_record

    def _generate_thumb_image (self, updated_record):
        img_bytes = self._generate_thumb_image_bytes (updated_record)
        if (img_bytes != None) and (len (img_bytes) > 0):
            file_base_name = '%s_image' % (updated_record ['value']['attributes']['pid'])
            local_image_store_path_png = '%s/%s.png' % (uc.THUMB_IMAGE_LOCAL_DIR, file_base_name)
            with open (local_image_store_path_png, 'wb') as local_image_file_png:
                local_image_file_png.write (img_bytes)

            # convert png-format to webp. Converted .webp stored in the same image folder as the .png images
            local_image_store_path_webp = self._convert_png_to_webp (local_image_store_path_png, file_base_name, uc.THUMB_IMAGE_LOCAL_DIR)

            # add record to aws_upload list. 
            img_upload_record = { 'pid': updated_record ['value']['attributes']['pid'],
                                  'local_image_path': local_image_store_path_webp
                                }
            # add img path to be uploaded to AWS
            self._aws_image_upload_records.append (img_upload_record)
        return 

    def _generate_thumb_image_bytes (self, updated_record):
        # 'color' is in variants for most products (some products don't have any color attrib)
        color = 'neutral'
        if 'variants' in updated_record ['value']:
            variant_list = updated_record ['value']['variants']
            for variant_id, variant_obj in variant_list.items():
                if 'color' in variant_obj ['attributes']:
                    color = variant_obj ['attributes']['color']
                    break

        product_details = """
                      Product Name: %s, \n
                      Product Brand: %s, \n
                      Product Description: %s, \n
                      Product Color: %s """  % (
                                                    updated_record ['value']['attributes']['title'],
                                                    updated_record ['value']['attributes']['brand'],
                                                    updated_record ['value']['attributes']['description'],
                                                    color
                                               )
        img_prompt = "%s\n%s" % (PROMPT_HEADER, product_details)
        openai.api_key = uc.OPENAI_KEY
        try:
            openai_response = openai.images.generate (
                 model = uc.OPENAI_MODEL_IMAGE_GENERATION,
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

    # openai generated images are in .png format. Convert to .webp
    def _convert_png_to_webp (self, local_image_path_png: str, base_name: str, local_converted_work_dir: str) -> str:
        local_converted_image_path = '%s/%s.webp' % (local_converted_work_dir, base_name)
        try:
            image = Image.open (local_image_path_png)
            image.save (local_converted_image_path, "webp")
        except Exception as e:
            logging.warning ('Exception in converting png to webp: %s' % base_name)
            local_converted_image_path = None
        return local_converted_image_path

    # return total count of new images uploaded
    def _prepare_upload_script (self):
        upload_count = 0
        upload_script_file = open (rcv7.FILENAME_AWS_UPLOAD_SCRIPT_OUT, 'w')

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
            upload_script_file.write ('%s\n' % aws_s3_cp_command)
            upload_count = upload_count + 1

        upload_script_file.write ('\ndate\n\n') # end date
        upload_script_file.flush ()
        upload_script_file.close ()
        logging.debug ('Total new images to upload to AWS: %s', upload_count)
        return upload_count

    # return string for s3 cp command
    # eg, aws --profile bloomreach-demo_main s3 cp --acl public-read  ./data/images/<filename> s3://pacific-demo-data.bloomreach.cloud/home/images/gen/webp/<pid>_0_image.webp
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
        s3_file_name = '%s_0_image.%s' % (pid, extension)  # .../pid_0_image.<extension>
        s3_path = '%s/%s' % (rcv7.AWS_S3_IMAGES_FOLDER, s3_file_name) 
        aws_cp_command = '%s %s %s' % (rcv7.AWS_CP_COMMAND_PREAMBLE, local_image_path, s3_path)
        logging.debug ('AWS s3 copy command: %s', aws_cp_command)
        return aws_cp_command

    # return string for s3 rm command
    # eg, aws --profile bloomreach-demo_main s3 rm s3://pacific-demo-data.bloomreach.cloud/apparel/images/hlthbeauty/<pid>_image.png
    # NOTE: dress images uploaded to a sub-dir 'hlthbeauty'
    def _construct_s3_rm_command (self, img_upload_record):
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
        s3_path = '%s/%s' % (rcv7.AWS_S3_IMAGES_FOLDER, s3_file_name)
        aws_rm_command = '%s %s' % (rcv7.AWS_RM_COMMAND_PREAMBLE, s3_path)
        logging.debug ('AWS s3 rm command: %s', aws_rm_command)
        return aws_rm_command

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV7 ()
    logging.info ('RevisionV7 Finish...')


