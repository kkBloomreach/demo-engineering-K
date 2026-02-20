import csv
import logging
import requests
import os
import sys
import openai
import base64

OPENAI_KEY = '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'
OPENAI_MODEL = 'gpt-image-1'
HTTP_STATUS_OK = 200

MASK_TEXT = f"""
Find the most prominent color in the original image and replace it with PINK color
    """

LOCAL_SOURCE_IMAGE_STORE_PATH_PNG = './product.png'
LOCAL_VARIANT_IMAGE_STORE_PATH_PNG = './variant.png'
IMAGE_MASK_PNG = './mask.png'

class TestOpenAIVariantImageGenerator ():

    def __init__ (self):
        return

    def generate_variant_image (self, product_image, mask_image, mask_text, local_variant_image_path_png):
        img_bytes = self._get_openAI_variant_image_bytes (product_image, mask_image, mask_text)
        if img_bytes and len (img_bytes) > 0:
            with open (local_variant_image_path_png, 'wb') as local_image_file_png:
                local_image_file_png.write (img_bytes)
                local_image_file_png.flush ()
                local_image_file_png.close ()
        return

    # generate image bytes using openAI
    def _get_openAI_variant_image_bytes (self, product_image, mask_image, mask_text):
        openai.api_key = OPENAI_KEY
        try:
            openai_response = openai.images.edit (
                 model = OPENAI_MODEL,
                 image = open (product_image, 'rb'),
                 mask = open (mask_image, 'rb'),
                 prompt = mask_text
                 #quality = 'medium',
                 #n = 1,
                 #size = '1024x1024',
                 #style = 'natural'
            )
            img_bytes = base64.b64decode (openai_response.data[0].b64_json)
            logging.debug ('gen image byte count: %s' % len (img_bytes))
        except Exception as e:
            logging.error ('cannot generate openAI variant image error %s' % str(e))
            img_bytes  = None
        return img_bytes

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    generator = TestOpenAIVariantImageGenerator ()
    generator.generate_variant_image (LOCAL_SOURCE_IMAGE_STORE_PATH_PNG, IMAGE_MASK_PNG, MASK_TEXT, LOCAL_VARIANT_IMAGE_STORE_PATH_PNG)
    logging.info ('Finish generate variant image ...')

