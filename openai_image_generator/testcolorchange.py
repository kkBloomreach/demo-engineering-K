import csv
import logging
import requests
import os
import sys
import openai
import base64

OLD_OPENAI_KEY = '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'
OPENAI_KEY = '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'

OPENAI_MODEL = 'gpt-image-1'
HTTP_STATUS_OK = 200

IMAGE_EDIT_PROMPT = f"""
Change the primary color in this image to %s, keeping all other details the same.
"""

LOCAL_SOURCE_IMAGE_STORE_PATH_PNG = './purse.png'
LOCAL_VARIANT_IMAGE_STORE_PATH_PNG = './purse_3.png'

class TestOpenAIColorChangeGenerator ():

    def __init__ (self):
        return

    def generate_variant_image (self, product_image, new_color, local_variant_image_path_png):
        img_bytes = self._get_openAI_variant_image_bytes (product_image, new_color)
        if img_bytes and len (img_bytes) > 0:
            with open (local_variant_image_path_png, 'wb') as local_image_file_png:
                local_image_file_png.write (img_bytes)
                local_image_file_png.flush ()
                local_image_file_png.close ()
        return

    # generate image bytes using openAI
    def _get_openAI_variant_image_bytes (self, product_image, new_color):
        openai.api_key = OPENAI_KEY
        actual_prompt = IMAGE_EDIT_PROMPT % new_color
        try:
            openai_response = openai.images.edit (
                 model = OPENAI_MODEL,
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

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    generator = TestOpenAIColorChangeGenerator ()
    generator.generate_variant_image (LOCAL_SOURCE_IMAGE_STORE_PATH_PNG, 'blue', LOCAL_VARIANT_IMAGE_STORE_PATH_PNG)
    logging.info ('Finish generate variant image ...')

