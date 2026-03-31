import logging
import os
import sys
import openai

OPENAI_KEY = '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'

OPENAI_MODEL = 'gpt-5.1'
HTTP_STATUS_OK = 200

PROMPT_HEADER = """
You are a customer that has purchased a product from a merchant's website. Write a review for that product as %s customer 
"""

RESPONSE_RULES = """
Return only the review text.
Limit the review text to 10 lines.
The information you provide should help the merchant and other customers in determining whether they might want to purchase the product.
You can speak about any of its attributes but be sure to use human language.
The review text should include information like product rating, complementary products and variants.
"""

class TestOpenAIReviewGenerator ():
    def __init__ (self):
        return

    def generate_product_review (self):
        review_text = self._get_openAI_review_text ()
        if review_text != None:
            logging.info ('Review: %s' % review_text)
        else:
            logging.warning ('Could not create review text')
        return

    # generate image bytes using openAI
    def _get_openAI_review_text (self):

        # CHANGE PRODUCT DETAILS
        product_details = """
                        Product Name: %s, \n
                        Product Brand: %s, \n
                        Product Description: %s, \n
                        Product Color: %s \n
                        Product Gender: %s """  % (
                                                      'Blue & Gold-Toned Net Semi-Stitched ankle-length Dress',
                                                      'Pacific 25',
                                                      'This royal blue dress is a true masterpiece. Delicate floral embroidery dances across the fabric, creating an enchanting visual. The sheer sleeves and overlay add an air of mystique, while the flowing silhouette ensures graceful movement.',
                                                      'Blue',
                                                      'female' )

        system_message = "%s %s" % (PROMPT_HEADER, 'extremely satisfied')
        system_message = "%s\n%s" % (system_message, product_details)
        system_message = "%s\n%s" % (system_message, RESPONSE_RULES)
        logging.debug ('System message: %s', system_message)

        user_message = "%s" % (product_details)
        logging.debug ('User message: %s', user_message)

        client = openai.OpenAI (api_key = OPENAI_KEY)
        try:
            openai_response = client.chat.completions.create (
                 model = OPENAI_MODEL,
                 messages = [
                    {'role': 'system',
                     'content': system_message
                    },
                    {'role': 'user',
                     'content': user_message
                    }
                 ],
                 temperature = 0.5
            )
            review_text = openai_response.choices [0].message.content
        except Exception as e:
            logging.error ('\ncannot generate review text for %s, error = %s' % (test_pid, str(e)))
            review_text = None
        return review_text 

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    generator = TestOpenAIReviewGenerator ()
    generator.generate_product_review ()
    logging.info ('Finish generate review text...')

