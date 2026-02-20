# V33 changes
# -- change images of 'dresses' products using new openai model and prompt
# -- Also use new prompt header
# -- Also use openAI new image generation API version

import logging
import copy
import os
import openai
import base64
from PIL import Image

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV33 as rcv33
import categorybuilder as cb

PROMPT_HEADER_ORIG = """
Create a premium, high‑resolution product image for a luxury retail clothing website. The image should showcase a single apparel item on a fashion model with crisp detail, refined lighting, and a clean, modern presentation suitable for high‑end ecommerce brands like Neiman Marcus or Nordstrom. Use a neutral, elegant background that enhances the product without distraction. The style should feel sophisticated, upscale, and consistent with luxury fashion photography. No text, logos, or additional props should appear in the image—only the model wearing the item displayed clearly and attractively. 
"""

PROMPT_HEADER = """ Create a premium, high‑resolution product image for a luxury retail clothing website. The image should showcase a single apparel item on a fashion model. 

emotion: 
  joyful 
  enthusiastic 
  carefree
face: 
  skin_tone: smooth porcelain
structure:
  shape: oval
  cheekbones: high
  jawline: gently rounded
eyes:
  shape: bright almond-shaped
  expression: smiling intensely
  crease: subtle double eyelid
  eyebrows: thin, arched, slightly raised
nose: 
  small button
lips: 
  full, wide smile, soft pink glossy lipstick, teeth visible
hair:
  type: straight, silky
  style: loose around shoulders
lighting:
  type: bright natural daylight
effect: 
  emphasizing fresh youthful appearance
style: 
  photorealistic
  sophisticated
  upscale
  luxurious

Use refined lighting and show full face portrait. Image must have crisp details, be clean, modern presentation suitable for high‑end ecommerce brand. Use a neutral, elegant background that enhances the product without distraction. The style should feel sophisticated, upscale, and consistent with luxury fashion photography. No text, logos, or additional props should appear in the image—only the model wearing the item displayed clearly and attractively. 
"""
# as per current category map
WOMEN_DRESSES_CATEGORY_IDS = [
    "20000>21200"  # women > dresses
]

WOMEN_DRESSES_IMAGE_SUBFOLDER = 'dresses' # sub-folder/bucket in s3

class RevisionV33 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v33')
        super().__init__ ()
        self._aws_image_upload_records = []
        return

    def _initialize (self, source_records, inject_av_map):
        return True

    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)
        #if (inject_av_record == None):
        #    logging.debug ('No inject attrib_value record for pid: %s', pid)

        # change image only if product is in 'jewellery' categories
        if self._is_select_category (record) == True:
            logging.debug ('Changing image for pid: %s' % pid)
            updated_record = self._perform_update_internal (record, inject_av_record)
            return updated_record
        else:
            return record

    # generate aws upload script (to be executed separately)
    def _finalize (self, updated_products):
        if (len (self._aws_image_upload_records) > 0):
            self._prepare_upload_script ()
        else:
            logging.warning ('No images to upload to AWS')
        return updated_products

    # INTERNAL METHODS
    def _is_select_category (self, record):
        category_paths = record ['value']['attributes']['category_paths']
        for branch in category_paths:
            full_path = None 
            for leaf_node in branch:
                if full_path == None:
                    full_path = leaf_node ['id']
                else:
                    full_path = '%s>%s' % (full_path, leaf_node ['id'])
            if full_path in WOMEN_DRESSES_CATEGORY_IDS:
                return True
        return False

    def _perform_update_internal (self, record, inject_av_record):
        pid = record ['value']['attributes']['pid']
        updated_record = copy.deepcopy (record)

        # update previous record - 
        # use ai image generator to generate new image. Actual url in the
        # record itself is not changed - only new image is uploaded to s3
        self._generate_thumb_image (updated_record)

        return updated_record

    def _generate_thumb_image (self, updated_record):
        # generate new image if previous does not exist
        file_base_name = '%s_image' % (updated_record ['value']['attributes']['pid'])
        local_webp_image_path = '%s/%s.webp' % (rcv33.THUMB_IMAGE_LOCAL_DIR, file_base_name)

        if (os.path.exists (local_webp_image_path) == False):
            img_bytes = self._generate_thumb_image_bytes (updated_record)
            if (img_bytes != None) and (len (img_bytes) > 0):
                local_image_store_path_png = '%s/%s.png' % (rcv33.THUMB_IMAGE_LOCAL_DIR, file_base_name)
                with open (local_image_store_path_png, 'wb') as local_image_file_png:
                    local_image_file_png.write (img_bytes)
                    local_image_file_png.flush ()
                    local_image_file_png.close ()

                # convert png-format to webp. Converted .webp stored in the same image folder as the .png images
                self._convert_png_to_webp (local_image_store_path_png, local_webp_image_path)

                # delete .png (to save local disk space)
                os.remove (local_image_store_path_png)
            else:
                logging.warning ('Could not generate image for pid: %s' % updated_record ['value']['attributes']['pid'])
                local_webp_image_path = None

        # set thumb_image and swatch_image urls in product record and its variants
        if local_webp_image_path != None:
            thumb_img_url = '%s/%s/%s_image.%s' % (rcv33.THUMB_IMAGE_URL_PROLOG, 
                                                   WOMEN_DRESSES_IMAGE_SUBFOLDER,
                                                   updated_record ['value']['attributes']['pid'], 'webp')
            updated_record ['value']['attributes']['large_image'] = thumb_img_url
            updated_record ['value']['attributes']['thumb_image'] = thumb_img_url
            if ('variants' in updated_record ['value']) and (updated_record ['value']['variants']):
                variant_list = updated_record ['value']['variants']
                for variant_id, variant_obj in variant_list.items():
                    variant_obj ['attributes']['swatch_image'] = thumb_img_url
                    variant_obj ['attributes']['thumb_image'] = thumb_img_url

            # add record to aws_upload list. 
            img_upload_record = { 'pid': updated_record ['value']['attributes']['pid'],
                                  'local_image_path': local_webp_image_path
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
                      Prouct  Gender: %s, \n
                      Product Color: %s """  % (
                                                    updated_record ['value']['attributes']['title'],
                                                    updated_record ['value']['attributes']['brand'],
                                                    updated_record ['value']['attributes']['description'],
                                                    updated_record ['value']['attributes']['gender'],
                                                    color
                                               )
        img_prompt = "%s\n%s" % (PROMPT_HEADER, product_details)
        openai.api_key = rcv33.OPENAI_KEY
        try:
            openai_response = openai.images.generate (
                 model = rcv33.OPENAI_MODEL_IMAGE_GENERATION,
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
    def _convert_png_to_webp (self, local_image_path_png: str, local_image_path_webp: str):
        try:
            image = Image.open (local_image_path_png)
            image.save (local_image_path_webp, "webp")
        except Exception as e:
            logging.warning ('Exception in converting png to webp: %s' % local_image_path_png)
        return 

    # return total count of new images uploaded
    def _prepare_upload_script (self):
        upload_count = 0
        upload_script_file = open (rcv33.FILENAME_AWS_UPLOAD_SCRIPT_OUT, 'w')

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
    # eg, aws --profile bloomreach-demo_main s3 cp --acl public-read  ./data/images/<filename> s3://pacific-demo-data.bloomreach.cloud/home/images/webp/gen/<subfolder>/<pid>_image.webp
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
        s3_path = '%s/%s/%s' % (rcv33.AWS_S3_GEN_GPTIMAGE_1_IMAGES_FOLDER, 
                                WOMEN_DRESSES_IMAGE_SUBFOLDER,
                                s3_file_name) 
        aws_cp_command = '%s %s %s' % (rcv33.AWS_CP_COMMAND_PREAMBLE, local_image_path, s3_path)
        logging.debug ('AWS s3 copy command: %s', aws_cp_command)
        return aws_cp_command

    # return string for s3 rm command
    # eg, aws --profile bloomreach-demo_main s3 rm s3://pacific-demo-data.bloomreach.cloud/apparel/images/jewellery/<pid>_image.png
    # NOTE: images uploaded to a sub-dir 'jewellery'
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
        s3_path = '%s/%s/%s' % (rcv33.AWS_S3_GEN_GPTIMAGE_1_IMAGES_FOLDER, 
                                WOMEN_DRESSES_IMAGE_SUBFOLDER,
                                s3_file_name)
        aws_rm_command = '%s %s' % (rcv33.AWS_RM_COMMAND_PREAMBLE, s3_path)
        logging.debug ('AWS s3 rm command: %s', aws_rm_command)
        return aws_rm_command

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV33 ()
    logging.info ('RevisionV33 finish...')



