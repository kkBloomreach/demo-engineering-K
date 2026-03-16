import csv
import logging
import requests
import os
import sys
import openai
import base64

OPENAI_KEY = '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'
NEW_OPENAI_KEY = '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'

OPENAI_MODEL = 'gpt-image-1'
HTTP_STATUS_OK = 200

BANNER_TEXT_SPORTINGGOODS = f"""
A dynamic, action-shot banner image showing a mix of sports products and game-related imagery, with a basket ball flying through the air and a player's hand reaching for baskeball hoop. Use a subtle but high-contrast color palette with a competitive mood. Use high-energy floodlighting. Motion blur, wide shot, high-energy, high-resolution.
    """

BANNER_TEXT = f"""
a modern action-shot banner image with attractive and simple background showing a mix of consumer electronics products such as computers, televisions, mobile devices, cameras. Use a subtle but high-contrast color palette with appealing mood.
    """

LOCAL_IMAGE_STORE_PATH_PNG = './banner.png'
LOCAL_IMAGE_STORE_PATH_PNG = './banner.png'

class TestOpenAIImageGenerator ():

    def __init__ (self):
        return

    def generate_image (self, text, local_image_store_path_png):
        img_bytes = self._get_openAI_image_bytes (text)
        if img_bytes and len (img_bytes) > 0:
            with open (local_image_store_path_png, 'wb') as local_image_file_png:
                local_image_file_png.write (img_bytes)
                local_image_file_png.flush ()
                local_image_file_png.close ()
        return

    # generate image bytes using openAI
    def _get_openAI_image_bytes (self, banner_text):
        img_prompt = """Generate an artistic banner for %s: \n """  % banner_text

        openai.api_key = OPENAI_KEY
        try:
            openai_response = openai.images.generate (
                 model = OPENAI_MODEL,
                 prompt = img_prompt,
                 n = 1,
                 size = '1024x1024',
                 quality = 'medium',
                 #style = 'natural'
            )
            img_bytes = base64.b64decode (openai_response.data[0].b64_json)
            logging.debug ('gen image byte count: %s' % len (img_bytes))
        except Exception as e:
            logging.error ('cannot generate openAI image for %s, error = %s' % (updated_record ['value']['attributes']['pid'], str(e)))
            img_bytes  = None
        return img_bytes

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    generator = TestOpenAIImageGenerator ()
    product_text = BANNER_TEXT
    generator.generate_image (product_text, LOCAL_IMAGE_STORE_PATH_PNG)
    logging.info ('Finish generate image ...')

