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

PROMPT_HEADER = """
Generate a premium, high-resolution product image for a retail store. The product should be the sole focal point, presented against a clean, white backdrop that eliminates distractions and reinforces a modern, upscale aesthetic. Lighting should be soft. Do not include any text or labels in the image. The image must feel refined and editorial—ideal for showcasing the product on a high-end retail website so that shoppers will be enticed to purchase.
"""

class TestOpenAIImageGenerator ():

    def __init__ (self):
        return

    def generate_image_bytes (self):
        img_bytes = self._get_openAI_image_bytes ()
        if img_bytes != None:
            logging.info ('Image byte count: %s' % len (img_bytes))
        return


    # generate image bytes using openAI
    def _get_openAI_image_bytes (self):

        # CHANGE PRODUCT DETAILS
        product_details = """
                      Product Name: %s, \n
                      Product Brand: %s, \n
                      Product Description: %s, \n
                      Gender: %s, \n
                      Product Color: %s """  % (
                                                    'Vibrant Green Handbag',
                                                    'Pacific Style',
                                                    'This vibrant green handbag is a statement piece that combines functionality with high fashion. Its sleek, structured design exudes elegance and style, while the bold color adds a pop of brightness to any outfit. The unique chain link strap adds an edgy touch, making it perfect for those who love to stand out in a crowd. Whether you’re heading to a chic event or simply want to express your personality, this handbag is a must-have accessory.',
                                                    'female',
                                                    'Green'
                                               )


        img_prompt = "%s\n%s" % (PROMPT_HEADER, product_details)

        openai.api_key = OPENAI_KEY
        try:
            openai_response = openai.images.generate (
                 model = OPENAI_MODEL,
                 prompt = img_prompt,
                 n = 1,
                 size = '1024x1024',
                 quality = 'medium',
                 # style = 'natural'
            )
            img_bytes = base64.b64decode (openai_response.data[0].b64_json)
            with open ("output.png", "wb") as f:
                f.write (img_bytes)
            #logging.debug ('gen image byte count: %s' % len (img_bytes)
        except Exception as e:
            logging.error ('cannot generate openAI image for %s, error = %s' % (product_text, str(e)))
            img_bytes = None
        return img_bytes

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    generator = TestOpenAIImageGenerator ()
    generator.generate_image_bytes ()
    logging.info ('Finish generate image bytes ...')



'''
PRODUCT_TEXT_EXAMPLE = f"
    The Divine Setting Spray by Laura Mercier is a luxurious beauty product designed for women seeking a flawless makeup finish. This monthly essential comes in a convenient jar packaging. Customers rave about its long-lasting formula and skin-friendly ingredients, giving it a high rating. Pair it with Laura Mercier's foundation and primer for a complete makeup routine. Available in different variants to suit various skin types and preferences. Elevate your beauty routine with the Divine Setting Spray for a radiant and long-lasting look
    "

PRODUCT_TEXT = f"
A 15-inch ultrabook designed for multitasking with a high-resolution screen and ample storage. Perfect for creative professionals.
    "
        #img_prompt = 'Studio shot of %s, must not include any text description or labels. Image will be used in a product catalog. Image must be child friendly. Image must not include any without human body part' % (product_text)
        #img_prompt = 'Cook book cover with title consisting only english dictionary words for %s. Title must have maximum 5 words ' % (product_text)

        #img_prompt = """You are a professional photographer. Generate stock photo with plain white background for a product to be included in a online shopping catalog. Do not include any text description or lables in the image.  Product details are: \n

----------
You are a professional photographer. Generate stock photo with plain white background for a product to be included in a online shopping catalog. Do not include any text description or lables in the image.  Product details are: \n
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
------
#        product_details = """
#                      Product Name: %s, \n
#                      Product Brand: %s, \n
#                      Product Description: %s, \n
#                      Product Gender: %s, \n
#                      Product Color: %s """  % (
#                                                    'Rectangular Wood Gibson Dining Collection',
#                                                    'Pacific Goods',
#                                                    'Exclusive ring, large model, 18K white gold, 18K pink gold, 18K yellow gold, set with 387 brilliant-cut diamonds totaling 4.64 carats.',
#                                                    'unisex',
#                                                    'Pink'
#                                               )
'''
