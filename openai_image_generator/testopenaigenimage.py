import csv
import logging
import requests
import os
import sys
import openai

OPENAI_KEY = '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'
NEW_OPENAI_KEY = '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'

OPENAI_MODEL = 'dall-e-3'
HTTP_STATUS_OK = 200

PRODUCT_TEXT_EXAMPLE = f"""
    The Divine Setting Spray by Laura Mercier is a luxurious beauty product designed for women seeking a flawless makeup finish. This monthly essential comes in a convenient jar packaging. Customers rave about its long-lasting formula and skin-friendly ingredients, giving it a high rating. Pair it with Laura Mercier's foundation and primer for a complete makeup routine. Available in different variants to suit various skin types and preferences. Elevate your beauty routine with the Divine Setting Spray for a radiant and long-lasting look
    """

PRODUCT_TEXT = f"""
A 15-inch ultrabook designed for multitasking with a high-resolution screen and ample storage. Perfect for creative professionals.
    """

class TestOpenAIImageGenerator ():

    def __init__ (self):
        return

    def generate_image_url (self, text):
        img_url = self._get_openAI_image_url (text)
        logging.info ('Image url: %s' % img_url)
        return


    # generate image url using openAI
    def _get_openAI_image_url (self, product_text):
        #img_prompt = 'Studio shot of %s, must not include any text description or labels. Image will be used in a product catalog. Image must be child friendly. Image must not include any without human body part' % (product_text)
        #img_prompt = 'Cook book cover with title consisting only english dictionary words for %s. Title must have maximum 5 words ' % (product_text)
        img_prompt = """You are a professional photographer. Generate stock photo with plain white background for a product to be included in a electronics online shopping catalog. Product details are: \n
                      Product Name: %s, \n
                      Product Brand: %s, \n
                      Product Description: %s, \n
                      Used by: %s, \n
                      Use frequency: %s, \n
                      Product Color: %s """  % (
                                                    'Pacific Style Vortex X',
                                                    'Pacific Style',
                                                    'Vortex X is designed for gamers who demand the best. With a robust build and superior graphics, it handles the most demanding games effortlessly.',
                                                    'Gamers',
                                                    'Daily',
                                                    'gray'
                                               )


        openai.api_key = OPENAI_KEY
        try:
            openai_response = openai.images.generate (
                 model = OPENAI_MODEL, # 'dall-e-3',
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

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    generator = TestOpenAIImageGenerator ()
    product_text = PRODUCT_TEXT
    generator.generate_image_url (product_text)
    logging.info ('Finish generate image url...')

