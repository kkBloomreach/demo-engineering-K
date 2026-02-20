# V10 changes
# -- Change images of all products using the new openAI image generation model
# -- Code is same as V10, except it tracks if there were some errors in image generation
# -- and if so, shows error count. That way we can decide to re-run this script
# -- until there are no errors
# -- To upload to aws, do so ONLY if a new image is generated (ie, don't rm/cp existing images)

import logging
import os
import copy
import csv
import base64
import openai
from PIL import Image

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV10 as rcv10

PROMPT_HEADER = """
Generate a premium, high-resolution product image for a retail store. The product should be the sole focal point, presented against a clean, white backdrop that eliminates distractions and reinforces a modern, upscale aesthetic. Lighting should be soft. Do not include any text or labels in the image. The image must feel refined and editorial—ideal for showcasing the product on a high-end retail website so that shoppers will be enticed to purchase.
"""

class RevisionV10 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v10')
        super().__init__ ()
        self._aws_image_upload_records = []
        self._image_generation_error_count = 0
        return

    def _initialize (self, source_records, inject_av_map):
        return True
 
    # override base class method
    # category_manager not needed in this revision
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']

        inject_av_record = super()._lookup_inject_av_record (pid)
        if (inject_av_record == None):
            logging.debug ('No inject attrib_value record for pid: %s', pid)

        updated_record = self._perform_update_internal (record, inject_av_record)
        return updated_record

    def _finalize (self, updated_products):
        if (len (self._aws_image_upload_records) > 0):
            self._prepare_upload_script ()
        else:
            logging.warning ('No images to upload to AWS')

        if self._image_generation_error_count != 0:
            # will need to re-run this script
            logging.warning ('Errors occured during image generation. Count = %s' % self._image_generation_error_count) 
        return True
 
    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        updated_record = copy.deepcopy (record)

        # use ai image generator to generate new image. Actual url in the
        # record itself is not changed - only new image is uploaded to s3
        self._generate_thumb_image (updated_record)
        return updated_record

    def _generate_thumb_image (self, updated_record):
        # generate new image if previous does not exist
        pid = updated_record ['value']['attributes']['pid']
        file_base_name = '%s_0_image' % pid
        local_webp_image_path = '%s/%s.webp' % (rcv10.THUMB_IMAGE_LOCAL_DIR, file_base_name)

        if (os.path.exists (local_webp_image_path) == False):
            logging.debug ('Generating image for pid: %s' % pid)
            img_bytes = self._generate_thumb_image_bytes (updated_record)
            if (img_bytes != None) and (len (img_bytes) > 0):
                local_image_store_path_png = '%s/%s.png' % (rcv10.THUMB_IMAGE_LOCAL_DIR, file_base_name)
                with open (local_image_store_path_png, 'wb') as local_image_file_png:
                    local_image_file_png.write (img_bytes)
                    local_image_file_png.flush ()
                    local_image_file_png.close ()

                # convert png-format to webp. Converted .webp stored in the same image folder as the .png images
                self._convert_png_to_webp (local_image_store_path_png, local_webp_image_path)

                # delete .png (to save local disk space)
                os.remove (local_image_store_path_png)

                # add record to aws_upload list. (only if new image generated) 
                img_upload_record = { 'pid': updated_record ['value']['attributes']['pid'],
                                      'local_image_path': local_webp_image_path
                                    }
                # add img path to be uploaded to AWS
                self._aws_image_upload_records.append (img_upload_record)
            else:
                logging.warning ('Could not generate image for pid: %s' % updated_record ['value']['attributes']['pid'])
                local_webp_image_path = None

        # set thumb_image and swatch_image urls in product record and its variants
        if local_webp_image_path != None:
            thumb_img_url = '%s/%s_0_image.%s' % (rcv10.THUMB_IMAGE_URL_PROLOG, updated_record ['value']['attributes']['pid'], 'webp')
            updated_record ['value']['attributes']['large_image'] = thumb_img_url
            updated_record ['value']['attributes']['thumb_image'] = thumb_img_url
            if ('variants' in updated_record ['value']) and (updated_record ['value']['variants']):
                variant_list = updated_record ['value']['variants']
                for variant_id, variant_obj in variant_list.items():
                    variant_obj ['attributes']['swatch_image'] = thumb_img_url
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
        openai.api_key = rcv10.OPENAI_KEY
        try:
            openai_response = openai.images.generate (
                 model = rcv10.OPENAI_MODEL_IMAGE_GENERATION,
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
            self._image_generation_error_count = self._image_generation_error_count + 1
        return img_bytes

    # openai generated images are in .png format. Convert to .webp
    def _convert_png_to_webp (self, local_image_path_png: str, local_image_path_webp: str) -> str:
        try:
            image = Image.open (local_image_path_png)
            image.save (local_image_path_webp, "webp")
        except Exception as e:
            logging.warning ('Exception in converting png to webp: %s' % local_image_path_png)
        return 

    # return total count of new images uploaded
    def _prepare_upload_script (self):
        upload_count = 0
        upload_script_file = open (rcv10.FILENAME_AWS_UPLOAD_SCRIPT_OUT, 'w')

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
        s3_path = '%s/%s' % (rcv10.AWS_S3_IMAGES_FOLDER, s3_file_name) 
        aws_cp_command = '%s %s %s' % (rcv10.AWS_CP_COMMAND_PREAMBLE, local_image_path, s3_path)
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
        s3_path = '%s/%s' % (rcv10.AWS_S3_IMAGES_FOLDER, s3_file_name)
        aws_rm_command = '%s %s' % (rcv10.AWS_RM_COMMAND_PREAMBLE, s3_path)
        logging.debug ('AWS s3 rm command: %s', aws_rm_command)
        return aws_rm_command

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV10 ()
    logging.info ('RevisionV10 Finish...')


