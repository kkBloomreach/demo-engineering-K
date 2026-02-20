# V21 changes
# -- generate 2 to 4 variants for each product in 'shoes' categories
# -- first merge existing products with same title
# -- then create variants for products that have not been merged

import logging
import copy
import os
import csv
import random
import openai
import base64
from PIL import Image

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstantsV21 as rcv21
import categorybuilder as cb

# in this revision, products only in these categories are considered for generating variants
SELECT_CATEGORY_IDS = [
    '10000>10800',   # men > shoes
    '20000>20800',   # women > shoes
    '70000>70100',   # shoes > casual
    '70000>70200',   # shoes > dress
    '70000>70300'    # sneakers
]

# sub-folder within 'variants'. 
# Note: for a given category, all variants of all its products are in one s3 bucket
# eg: AWS_S3_IMAGES_FOLDER/<shoes>/variants/<variant_image>
SHOES_VARIANTS_IMAGE_SUBFOLDER = 'shoes' 

PROMPT_HEADER = """
Generate a premium, high-resolution product image for a retail store. The product should be the sole focal point, presented against a clean, white backdrop that eliminates distractions and reinforces a modern, upscale aesthetic. Lighting should be soft. Do not include any text or labels in the image. The image must feel refined and editorial—ideal for showcasing the product on a high-end retail website so that shoppers will be enticed to purchase.
"""

class RevisionV21 (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update, version v21')
        super().__init__ ()

        # {"pid, title, images=[]}
        self._merged_product_list = []
        self._aws_image_download_records = [] # original PRODUCT-image downloaded to local-dir
        self._aws_image_upload_records = []
        return

    def _initialize (self, source_records, inject_av_map):
        # merge products with same title (case insensitive)
        if self._merge_source_records (source_records) == False:
            return False
        return True

    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)

        if (inject_av_record == None):
            logging.debug ('No inject attrib_value record for pid: %s', pid)

        # change image only if product is in 'jewellery' categories
        if self._is_select_category (record) == True:
            logging.debug ('Adding variants for pid: %s' % pid)
            updated_record = self._perform_update_internal (record, inject_av_record)
            return updated_record
        else:
            return record
        return updated_record

    # generate aws upload script (to be executed separately)
    def _finalize (self, updated_products):
        # original product-images download to local-dir
        if (len (self._aws_image_download_records) > 0):
            self._prepare_download_script ()
        else:
            logging.warning ('No images to download to AWS')

        if (len (self._aws_image_upload_records) > 0):
            self._prepare_upload_script ()
        else:
            logging.warning ('No images to upload to AWS')
        return True

    # INTERNAL METHODS
    def _merge_source_records (self, source_records):
        for record in source_records:
            merged_record = self._lookup_product_with_same_title (record ['value']['attributes']['title'])
            if (merged_record != None):
                # we already have a record with same title
                logging.debug ('pid %s merged with: %s' % (record ['value']['attributes']['pid'], merged_record ['pid']))
                merged_record ['images'].append (record ['value']['attributes']['thumb_image'])
            else:
                # no prior merged record for this title. Create a 'merged' record for future ref
                merged_record = { 'pid': record ['value']['attributes']['pid'],
                                  'title': record ['value']['attributes']['title'],
                                  'images': []
                                }
                merged_record['images'].append (record ['value']['attributes']['thumb_image'])
                self._merged_product_list.append (merged_record)
        return True

    # for a given record, return prior 'merged' record if title matches
    def _lookup_product_with_same_title (self, product_title):
        for merged_record in self._merged_product_list:
            if merged_record ['title'].lower() == product_title.lower():
                return merged_record 
        return None

    def _perform_update_internal (self, record, inject_av_record):
        pid = record ['value']['attributes']['pid']
        updated_record = copy.deepcopy (record)

        # if pid is not in merged-products-list, then it has been merged
        # into another product with same title. In that case, return None
        # (ie, remove this pid from output catalog)
        merged_record = self._lookup_merged_record_if_any (pid)
        if merged_record  == None:
            return None

        # update previous record - 
        # use ai image generator to generate new image.  new image is uploaded to s3
        self._generate_variants (updated_record, merged_record)
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

    def _lookup_merged_record_if_any (self, pid):
        for merged_record in self._merged_product_list:
            if merged_record ['pid'] == str (pid):
                # this pid is in merged-product-list (ie, it has not been merged with any other product)
                return merged_record
        return None

    def _generate_variants (self, updated_record, merged_record):
        updated_record ['value']['variants'] = {}

        count_colors_to_add = int (random.random () * (rcv21.MAX_COLORS_PER_PRODUCT)) + 1
        count_sizes_to_add = int (random.random () * (rcv21.MAX_SIZES_PER_PRODUCT)) + 1
        count_variants_to_add = count_colors_to_add * count_sizes_to_add
        count_variants_to_add = max (rcv21.MIN_VARIANTS_PER_PRODUCT, count_variants_to_add)
        logging.debug ('Total variant count = %s for pid = %s ' % (count_variants_to_add, updated_record ['value']['attributes']['pid']))

        available_colors = rcv21.COLOR_CHOICES.copy ()
        available_sizes = rcv21.SIZE_CHOICES.copy ()
        for count in range (0, count_variants_to_add):
            variant_id = '%s_%s' % (updated_record ['value']['attributes']['pid'], count)

            # select a uniq color at random
            select_color, available_colors = self._select_uniq_color (available_colors)
            select_size, available_sizes = self._select_uniq_size (available_sizes)

            # if the marged record already has an image, use it. If not, generate a new image
            if count < len (merged_record ['images']):
                # copy product-image from s3 bucket to its 'variants' subfolder
                orig_product_thumb_image_url = merged_record ['images'][count]
                rindx = orig_product_thumb_image_url.rindex ('/')    # extract s3 file name
                orig_image_file_name = orig_product_thumb_image_url [rindx+1:]
                orig_product_image_s3_path = '%s/%s' % (rcv21.AWS_S3_ORIG_PRODUCT_IMAGES_FOLDER, orig_image_file_name)

                # orig filename = '<pid>_image.webp'
                # variant image filename = '<pid>_count_image.webp' (ie, 'variant_id.webp)
                variant_image_file_name = '%s_image.webp' % (variant_id)
                local_download_image_path = '%s/%s' % (rcv21.THUMB_IMAGE_LOCAL_DIR, variant_image_file_name)

                # add record to aws_upload list. 
                img_download_record = { 'variant_id': variant_id,
                                        's3_product_image_path': orig_product_image_s3_path,
                                        'local_download_image_path': local_download_image_path
                                      }
                # add img path to be downloaded to AWS
                self._aws_image_download_records.append (img_download_record)

                # add record to aws_upload list. 
                img_upload_record = { 'variant_id': variant_id,
                                      'local_image_path': local_download_image_path
                                    }
                self._aws_image_upload_records.append (img_upload_record)

                # new variant-image url to set in catalog
                variant_swatch_image_url = '%s/%s/variants/%s' % (rcv21.THUMB_IMAGE_URL_PROLOG, 
                                                                  SHOES_VARIANTS_IMAGE_SUBFOLDER,
                                                                  variant_image_file_name)
            else:
                # generate image, save it to local dir, add it to aws_script, return 
                # Note: 'size' is not used in generating image
                # swatch_img_url to include in catalog
                variant_swatch_image_url = self._generate_variant_image (updated_record, variant_id, select_color)
                if (variant_swatch_image_url == None):
                    return  # warning already issued

            # add new variant
            logging.debug ('Creating variant for pid: %s' % updated_record ['value']['attributes']['pid'])
            # variant_id: <pid>_<count> (EG, 123321_1)
            updated_record ['value']['variants'][variant_id] = {}
            updated_record ['value']['variants'][variant_id]['attributes'] = {}
            updated_record ['value']['variants'][variant_id]['attributes']['skuid'] = variant_id
            updated_record ['value']['variants'][variant_id]['attributes']['color'] = select_color
            updated_record ['value']['variants'][variant_id]['attributes']['size'] = select_size
            updated_record ['value']['variants'][variant_id]['attributes']['swatch_image'] = variant_swatch_image_url
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
        updated_record ['value']['attributes']['thumb_image'] = updated_record ['value']['variants'][default_variant_id]['attributes']['swatch_image']

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
                        discount_factor = 1 - (random.random () * 0.20)  # variant price = default_price - {0 -> 0.2}
                        print ('discount factor: %s' % discount_factor)
                    else:
                        discount_factor = 1.0

                    this_variant_price = default_variant_price * discount_factor
                    this_variant_sale_price = default_variant_sale_price * discount_factor
                    print ('price: %s, sale_price: %s' % (this_variant_price, this_variant_sale_price))
                    updated_record ['value']['variants'][this_variant_id]['attributes']['price'] = round (this_variant_price, 2)
                    updated_record ['value']['variants'][this_variant_id]['attributes']['sale_price'] = round (this_variant_sale_price, 2)
        return

    # once a color is selected, set its value = None
    def _select_uniq_color (self, color_list):
        color_indx = int (random.random () * (len (rcv21.COLOR_CHOICES)))
        while True:
            select_color = rcv21.COLOR_CHOICES [color_indx]
            if select_color != None:
                color_list [color_indx] = None
                return (select_color, color_list)
        return (None, None) # should never happen

    # once a size is selected, set its value = None
    def _select_uniq_size (self, size_list):
        size_indx = int (random.random () * (len (rcv21.SIZE_CHOICES)))
        while True:
            select_size = rcv21.SIZE_CHOICES [size_indx]
            if select_size != None:
                size_list [size_indx] = None
                return (select_size, size_list)
        return (None, None) # should never happen

    # variant_id = <pid>_<num> (EG, 123213_1)
    # Note: 'size' is not used to generate image
    def _generate_variant_image (self, updated_record, variant_id, variant_color):
        # generate new image if previous does not exist
        file_base_name = '%s_image' % (variant_id)
        local_webp_image_path = '%s/%s.webp' % (rcv21.THUMB_IMAGE_LOCAL_DIR, file_base_name)

        if (os.path.exists (local_webp_image_path) == False):
            img_bytes = self._generate_variant_image_bytes (updated_record, variant_color)
            if (img_bytes != None) and (len (img_bytes) > 0):
                local_image_store_path_png = '%s/%s.png' % (rcv21.THUMB_IMAGE_LOCAL_DIR, file_base_name)
                with open (local_image_store_path_png, 'wb') as local_image_file_png:
                    local_image_file_png.write (img_bytes)
                    local_image_file_png.flush ()
                    local_image_file_png.close ()

                # convert png-format to webp. Converted .webp stored in the same image folder as the .png images
                self._convert_png_to_webp (local_image_store_path_png, local_webp_image_path)

                # delete .png (to save local disk space)
                os.remove (local_image_store_path_png)
            else:
                logging.warning ('Could not generate image for a pid: %s, variant: %s' % (updated_record ['value']['attributes']['pid'], variant_id))
                local_webp_image_path = None

        # prepare swatch_image url in variant record 
        if local_webp_image_path != None:
            s3_file_name = '%s_image.%s' % (variant_id, 'webp')  # .../variant_id_image.<extension>
            swatch_img_url = '%s/%s/variants/%s' % (rcv21.THUMB_IMAGE_URL_PROLOG, 
                                                             SHOES_VARIANTS_IMAGE_SUBFOLDER,
                                                             s3_file_name)

            # add record to aws_upload list. 
            img_upload_record = { 'variant_id': variant_id,
                                  'local_image_path': local_webp_image_path
                                }
            # add img path to be uploaded to AWS
            self._aws_image_upload_records.append (img_upload_record)
        else:
            swatch_img_url = None
        return swatch_img_url

    def _generate_variant_image_bytes (self, updated_record, select_color):
        # for purpose of generating image, add 'color' in title and description
        title_txt = '%s. Use %s color to generate image.' % (updated_record ['value']['attributes']['title'], select_color)
        description_txt = '%s. Use %s color to generate image.' % (updated_record ['value']['attributes']['description'], select_color)
        product_details = """
                      Product Name: %s, \n
                      Product Brand: %s, \n
                      Product Description: %s, \n
                      Prouct  Gender: %s, \n
                      Product Color: %s """  % (
                                                    title_txt,
                                                    updated_record ['value']['attributes']['brand'],
                                                    description_txt,
                                                    updated_record ['value']['attributes']['gender'],
                                                    select_color
                                               )
        img_prompt = "%s\n%s" % (PROMPT_HEADER, product_details)
        openai.api_key = rcv21.OPENAI_KEY
        try:
            openai_response = openai.images.generate (
                 model = rcv21.OPENAI_MODEL_IMAGE_GENERATION,
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

    # return total count of product images downloaded
    def _prepare_download_script (self):
        download_count = 0
        download_script_file = open (rcv21.FILENAME_AWS_DOWNLOAD_SCRIPT_OUT, 'w')

        # initial commands
        download_script_file.write ('\nset -e\n\n')
        download_script_file.write ('\ndate\n\n') # start-date

        # individual image cp commands
        download_count = 0
        for img_download_record in self._aws_image_download_records:
            # download image from to s3 to local dir
            aws_s3_download_command = self._construct_s3_download_command (img_download_record)
            if aws_s3_download_command == None:
                continue    # warning already issued

            download_script_file.write ('%s\n\n' % aws_s3_download_command)
            download_count = download_count + 1

        download_script_file.write ('\ndate\n\n') # end date
        download_script_file.flush ()
        download_script_file.close ()
        logging.debug ('Total product images to download to AWS: %s', download_count)
        return download_count

    # return total count of new images uploaded
    def _prepare_upload_script (self):
        upload_count = 0
        upload_script_file = open (rcv21.FILENAME_AWS_UPLOAD_SCRIPT_OUT, 'w')

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

    # return string for s3 download command
    # eg, aws --profile bloomreach-demo_main s3 cp --acl public-read  
    # s3://pacific-demo-data.bloomreach.cloud/home/images/webp/gen/<subfolder>/variants/variantid_image.webp 
    # ./data/images/<filename>
    def _construct_s3_download_command (self, img_download_record):
        # using variant_id, first lookup associated src_image_id
        local_download_image_path = img_download_record ['local_download_image_path']

        # s3 path to download original product image file 
        s3_path = img_download_record ['s3_product_image_path']
        aws_download_command = '%s %s %s' % (rcv21.AWS_CP_COMMAND_PREAMBLE, s3_path, local_download_image_path)
        logging.debug ('AWS s3 download command: %s', aws_download_command)
        return aws_download_command

    # return string for s3 cp command
    # eg, aws --profile bloomreach-demo_main s3 cp --acl public-read \
    # ./data/images/<filename> 
    # s3://pacific-demo-data.bloomreach.cloud/home/images/webp/gen/<subfolder>/variants/variantid_image.webp
    def _construct_s3_cp_command (self, img_upload_record):
        # using variant_id, first lookup associated src_image_id
        variant_id = img_upload_record ['variant_id']
        local_image_path = img_upload_record ['local_image_path']

        s3_file_name = '%s_image.%s' % (variant_id, 'webp')  # .../variant_id_image.webp
        s3_path = '%s/%s/variants/%s' % (rcv21.AWS_S3_GEN_GPTIMAGE_1_IMAGES_FOLDER, 
                                         SHOES_VARIANTS_IMAGE_SUBFOLDER, 
                                         s3_file_name) 
        aws_cp_command = '%s %s %s' % (rcv21.AWS_CP_COMMAND_PREAMBLE, local_image_path, s3_path)
        logging.debug ('AWS s3 copy command: %s', aws_cp_command)
        return aws_cp_command

    # return string for s3 rm command
    # eg, aws --profile bloomreach-demo_main s3 rm s3://pacific-demo-data.bloomreach.cloud/apparel/images/webp/gen/<category>/variants/file_name
    # NOTE: images uploaded to a sub-dir 'jewellery'
    def _construct_s3_rm_command (self, img_upload_record):
        # using variant_id, first lookup associated src_image_id
        variant_id = img_upload_record ['variant_id']
        local_image_path = img_upload_record ['local_image_path']

        s3_file_name = '%s_image.%s' % (variant_id, 'webp')  # .../variant_id_image.webp
        s3_path = '%s/%s/variants/%s' % (rcv21.AWS_S3_GEN_GPTIMAGE_1_IMAGES_FOLDER, 
                                         SHOES_VARIANTS_IMAGE_SUBFOLDER, 
                                         s3_file_name)
        aws_rm_command = '%s %s' % (rcv21.AWS_RM_COMMAND_PREAMBLE, s3_path)
        logging.debug ('AWS s3 rm command: %s', aws_rm_command)
        return aws_rm_command

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = RevisionV21 ()
    logging.info ('RevisionV21 finish...')


