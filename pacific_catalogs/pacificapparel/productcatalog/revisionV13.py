# V13 changes
# -- replace specific images

import logging
import random
import os
import copy
import csv
import openai

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV13 as rcv13
import imageloader as il

class RevisionV13 (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update, version v13')
        super().__init__ ()
        return

    def _initialize (self, source_records, inject_av_map):
        # read source records
        self._images_to_replace = self._read_images_to_replace ()
        if ((self._images_to_replace == None) or (len (self._images_to_replace) == 0)):
            return False

        # image loader, construct once
        self._image_loader = il.ImageLoader ()

        # list of images to upload to aws
        self._aws_image_upload_records = []

        return True

    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)

        if (inject_av_record == None):
            logging.debug  ('No inject attrib_value record for pid: %s', pid)

        updated_record = self._perform_update_internal (record, inject_av_record)
        return updated_record

    # generate aws upload script (to be executed separately)
    def _finalize (self, updated_products):
        if (len (self._aws_image_upload_records) > 0):
            self._prepare_upload_script ()
        else:
            logging.warning ('No images to upload to AWS')
        return True

    # INTERNAL METHODS
    # read list of image urls where image needs to be replaced
    def _read_images_to_replace (self):
        images_to_replace = []
        if (os.path.exists (rcv13.FILENAME_IMAGE_REPLACEMENT_LIST_TSV_IN)):
            with open (rcv13.FILENAME_IMAGE_REPLACEMENT_LIST_TSV_IN, 'r') as input_file:
                dict_reader = csv.DictReader (input_file, delimiter='\t')
                for row in dict_reader:
                    url = row ['pdp_url']
                    if (len (url) == 0):
                        continue

                    if url.startswith ('#'):    # skip lines starting with #
                        continue

                    # line is url, extract pid from it
                    indx = url.rindex ('/')
                    pid = url [indx+1:]
                    pid = pid.replace ('_', '')
                    images_to_replace.append (pid)
                input_file.close ()
        return images_to_replace

    # This revision does not change any existing product record
    def _perform_update_internal (self, record, inject_av_record):
        pid = record ['value']['attributes']['pid']
        updated_record = copy.deepcopy (record)
        if pid in self._images_to_replace:
            self._prepare_thumb_image (record)
        return updated_record

    def _replace_image (self, record):
        return record # @@@ TEMP

    # generate image, save locally + add aws_upload_record, then return thumb_image_url
    def _prepare_thumb_image (self, src_product):
        thumb_img_url = None
        pid = src_product ['value']['attributes']['pid']

        # load image and save it locally if we haven't generated image yet
        local_image_store_path = '%s/%s.png' % (uc.THUMB_IMAGE_LOCAL_DIR, pid)
        if (os.path.exists (local_image_store_path) == False):
            gen_img_url = self._generate_thumb_image_url (src_product)
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
        upload_script_file = open (rcv13.FILENAME_AWS_UPLOAD_SCRIPT_OUT, 'w')

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
        s3_path = '%s/%s/%s' % (rcv13.AWS_S3_IMAGES_FOLDER, 'hlthbeauty', s3_file_name)  # sub-folder = hlthbeauty
        aws_cp_command = '%s %s %s' % (rcv13.AWS_CP_COMMAND_PREAMBLE, local_image_path, s3_path)
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
        s3_path = '%s/%s/%s' % (rcv13.AWS_S3_IMAGES_FOLDER, 'hlthbeauty', s3_file_name)  # sub-folder = hlthbeauty
        aws_rm_command = '%s %s' % (rcv13.AWS_RM_COMMAND_PREAMBLE, s3_path)
        logging.debug ('AWS s3 rm command: %s', aws_rm_command)
        return aws_rm_command


if __name__ == '__main__':
    rv = RevisionV13 ()

