# V5 changes
# -- Change 'book' images (19 of them)
# -- Add 'collection' and 'author' attributes to the book images
# -- No 'subset' is created in this process

import logging
import copy
import csv
import openai
import os
import random

from updateBase import UpdateBase
import updaterConstants as uc
import updaterConstantsV5 as ucv5
import imageloader as il

OPENAI_KEY = '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'
OPENAI_MODEL = 'gpt-4o'
HTTP_STATUS_OK = 200

class UpdateV5 (UpdateBase) :

    _image_loader = None
    _aws_image_upload_records = None 

    def __init__ (self):
        logging.info ('Perform update + generate subset, version v5')
        super().__init__ ()
        self._image_loader = il.ImageLoader () # to save openai generated images to local disk
        self._aws_image_upload_records = []
        return

    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        updated_record = copy.deepcopy  (record)

        # book-only update
        if pid in ucv5.BOOKS_PRODUCT_ID_LIST_TO_CHANGE:
            logging.debug ('Update book image for pid: %s' % pid)
            updated_record = self._perform_update_internal (updated_record)

        return updated_record

    # override base class method to generate aws_upload script
    def _finalize_updates (self):
        image_upload_count = self._prepare_upload_script ()
        logging.debug ('Image upload count: %s', image_upload_count)
        return

    def _perform_update_internal (self, updated_record):
        pid = updated_record ['value']['attributes']['pid']

        # image
        # DEBUG - don't re-generate if file already generated + stored locally
        local_store_path = '%s/%s%s' % (ucv5.DIRNAME_BOOK_PRODUCT_IMAGES, pid, '_0_image.png')
        if (os.path.exists (local_store_path) == False):
            updated_image_url = self._generate_image (updated_record)
            if (updated_image_url == None):
                logging.error ('Cannot generate openAI image for pid: %s' % pid)
                return updated_record

            # save image locally
            if (self._image_loader.load_image (updated_image_url, local_store_path) == ucv5.IMAGE_LOADER_STATUS_FAIL):
                logging.error ('Cannot download and save openAI image: %s', pid)
                return updated_record

        # add a img_upload_record to self._image_uploads. It will be used later to prepare
        # upload script
        img_upload_record = {
                                'pid': pid,
                                'local_image_path': local_store_path
                             }
        self._aws_image_upload_records.append (img_upload_record)
        
        thumb_image_url = '%s%s%s' % (ucv5.THUMB_IMAGE_URL_PROLOG, pid, '_0_image.png')
        # product images
        updated_record ['value']['attributes']['thumb_image'] = thumb_image_url
        updated_record ['value']['attributes']['large_image'] = thumb_image_url
        # variant images
        if ('variants' in updated_record ['value']) and (updated_record ['value']['variants']):
            variant_list = updated_record ['value']['variants']
            for variant_id, variant_obj in variant_list.items():
                variant_obj ['attributes']['swatch_image'] = thumb_image_url

        # author
        indx = int (random.random () * len (ucv5.BOOK_AUTHORS))
        updated_record ['value']['attributes']['author'] = ucv5.BOOK_AUTHORS [indx]

        # collection
        indx = int (random.random () * len (ucv5.BOOK_COLLECTIONS))
        updated_record ['value']['attributes']['collection'] = ucv5.BOOK_COLLECTIONS [indx]

        # description - remove proper names
        for proper_name in ucv5.ORIGINAL_PROPER_NAMES:
            description = updated_record ['value']['attributes']['description']
            if proper_name in description:
                description = description.replace (proper_name, '')
                updated_record ['value']['attributes']['description'] = description

        return updated_record


    # DEBUGGING --- generate image using openAI
    def _generate_image_DEBUG (self, record):
        return record ['value']['attributes']['thumb_image']
 
    def _generate_image (self, record):
        description = record ['value']['attributes']['description']
        title = record ['value']['attributes']['title']
        product_text = '%s %s' % (description, title)
        logging.debug ('product text = %s' % product_text)

        image_url = self._get_openAI_image_url (product_text)
        if (image_url == None):
            logging.warning ('Could not generate openAI image for pid = %s' % (record ['value']['attributes']['pid']))
            imgage_url = record ['value']['attributes']['thumb_image']
        return image_url
 
    # generate image url using openAI
    def _get_openAI_image_url (self, product_text):
        img_prompt = 'Cook book cover with title consisting only english dictionary words for %s. Title must have maximum 5 words ' % (product_text)
        openai.api_key = OPENAI_KEY
        try:
            openai_response = openai.images.generate (
                 model = 'dall-e-3',
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

    # prepare upload script (to be executed separately)
    # returns total count of new images uploaded
    def _prepare_upload_script (self):
        upload_count = 0
        upload_script_file = open (ucv5.FILENAME_AWS_UPLOAD_SCRIPT, 'w')

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
    # eg, aws --profile bloomreach-demo_main s3 cp --acl public-read  ./data/images/bagimages/<filename> 
    #                     s3://pacific-demo-data.bloomreach.cloud/home/images/images/gen//<pid>_0_image.png
    def _construct_s3_cp_command (self, img_upload_record):
        # using pid, first lookup associated src_image_id
        pid = img_upload_record ['pid']
        local_image_path = img_upload_record ['local_image_path']

        # s3 path
        indx = local_image_path.rindex ('/')
        s3_file_name = local_image_path [indx+1:]
        s3_path = '%s/%s' % (ucv5.AWS_S3_IMAGES_FOLDER, s3_file_name)
        aws_cp_command = '%s %s %s' % (ucv5.AWS_CP_COMMAND_PREAMBLE, local_image_path, s3_path)
        logging.debug ('AWS s3 copy command: %s', aws_cp_command)
        return aws_cp_command

if __name__ == '__main__':
    u = UpdateV5 ()


'''
============
# -- Unrelated, add another attribute value = category-child-leaf name (eg, category = "A/B/C", leafname = "C") 
#    Hand-in-hand, make category 'non-searchable' for Discovery via catalogMgmt
        # for all records, add 'leafname'
        # @@@ TEMP - commented out
        # updated_record = self._perform_leafname_update_internal (updated_record)

    # add 'leafname' attribute
    def _perform_leafname_update_internal (self, updated_record):
        leaf_names = []
        category_paths = updated_record ['value']['attributes']['category_paths']
        for branch in category_paths:
            branch_len = len (branch)
            leaf_name = branch [branch_len - 1]['name']
            leaf_names.append (leaf_name)
        attrib_name = ucv5.ATTRIB_NAME_LEAFNAMES
        updated_record ['value']['attributes'][attrib_name] = leaf_names

        return updated_record
'''
