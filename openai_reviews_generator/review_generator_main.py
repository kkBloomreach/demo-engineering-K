import logging
import os
import sys
import openai
import jsonlines
import csv
import uuid
import random
import time
import copy
from pydantic import BaseModel, Field

SOURCE_CATALOG_JSONL_IN = './data/input/pacific_apparel/pa_en_full_03252026.jsonl'
#SOURCE_CATALOG_JSONL_IN = './data/input/pacific_apparel/pa_en_full_03262026_2.jsonl'
REVIEWS_OUTPUT_CSV_DEBUG_OUT = './data/output/pacific_apparel/pa_en_full_debug_03262026_2.csv'
REVIEWS_OUTPUT_CSV_OUT = './data/output/pacific_apparel/pa_en_full_03262026_2.csv'

OPENAI_KEY = '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'
OPENAI_MODEL = 'gpt-5.1'
HTTP_STATUS_OK = 200

PROMPT_HEADER = """
You are a customer that has purchased a product from a merchant's website. Write a review for that product as %s customer 
"""

RESPONSE_RULES = """
Return only the review text.
Limit the review text to %s lines.
The information you provide should help the merchant and other customers in determining whether they might want to purchase the product.
You can speak about any of its attributes except product brand but be sure to use human language.
The review text should include information like complementary products and variants.
"""

SECONDS_IN_DAY = 24 * 60 * 60
SECONDS_IN_6_MONTHS = 6 * 30 * SECONDS_IN_DAY

# pydantic model for LLM response
class ResponseFormat (BaseModel):
    title: str = Field (description = "the title of the review")
    content: str = Field (description = "review text")

class ReviewGenerator ():
    def __init__ (self):
        return

    def read_source_catalog (self, filename):
        source_catalog = []
        with open (filename, 'r') as input:
            reader = jsonlines.Reader (input)
            for product in reader:
                source_catalog.append (product)
            input.close ()
        return source_catalog

    def generate_product_reviews (self, source_catalog):
        review_records_debug = []
        review_records = []

        # single product can have 1 to 10 separate reviews
        for product in source_catalog:
            reviews_per_product_count = random.randint (1, 11)
            rating = 0
            for i in range (1, reviews_per_product_count):
                # satisfaction_level
                random_int = random.randint (1, 5)
                satisfaction_level = 'happy' # default
                match random_int:
                    case 1:
                        satisfaction_level = 'Very unhappy'
                        rating = 1
                    case 2:
                        satisfaction_level = 'partly happy'
                        rating = 2
                    case 2:
                        satisfaction_level = 'happy'
                        rating = 3
                    case 3:
                        satisfaction_level = 'very happy'
                        rating = 4
                    case 4:
                        satisfaction_level = 'extremely happy'
                        rating = 5

                review_info = self._generate_one_review (product, satisfaction_level)
                if review_info != None:
                    logging.debug ('Review title: %s, content: %s' % (review_info.title, review_info.content))

                    review_title = review_info.title.replace (',', ' ')
                    review_text = review_info.content.replace (',', ' ')

                    # timestamp, sometime during the past 6 months
                    time_delta = random.randint (SECONDS_IN_DAY, SECONDS_IN_6_MONTHS)   # some seconds
                    timestamp = int (time.time ()) - time_delta

                    # verified purchaser
                    random_int = random.randint (1, 100)
                    verified = 'TRUE' if random_int < 40 else 'FALSE'

                    # approved
                    random_int = random.randint (1, 100)
                    moderationStat = 'APPROVED' if random_int < 30 else 'NOT APPROVED'

                    # helpfulVotes
                    helpfulVotes = random.randint (0, 25)
                    notHelpfulVotes = random.randint (0, 10)

                    a_review_record       = { 'reviewId': uuid.uuid4 (),
                                              'productId': product ['value']['attributes']['pid'],
                                              'title': review_title,
                                              'content': review_text,
                                              'rating': rating,
                                              'submissionTimestamp': timestamp,
                                              'verifiedPurchaser': verified,
                                              'moderationStat': moderationStat,
                                              'helpfulVotes': helpfulVotes,
                                              'notHelpfulVotes': notHelpfulVotes
                                          }

                    review_records.append (a_review_record)

                    # following for 'debugging'
                    a_review_record_debug = copy.copy (a_review_record)
                    a_review_record_debug ['actual_satisfaction_level'] = satisfaction_level
                    a_review_record_debug ['actual_product_title'] = product ['value']['attributes']['title']
                    actual_product_description = product ['value']['attributes']['description'].replace (',', ' ')  # avoid csv conflict
                    a_review_record_debug ['actual_product_description'] = actual_product_description
                    a_review_record_debug ['actual_product_url'] = product ['value']['attributes']['url']
                    review_records_debug.append (a_review_record_debug)
        return review_records, review_records_debug

    def _generate_one_review (self, product, satisfaction_level):
        try:
            review_info = self._get_openAI_review_info (product, satisfaction_level)
        except Exception as e:
            logging.error ('Could not generate review info for pid: %s, err = %s' % (product ['value']['attributes']['pid'], str (e)))
            review_info = None
        return review_info

    # generate review text using openAI
    def _get_openAI_review_info (self, product_record, satisfaction_level):
        # CHANGE PRODUCT DETAILS
        if 'gender' in product_record ['value']['attributes']:
            gender = product_record ['value']['attributes']['gender']
        else:
            gender = 'neutral'
        # exclude brand because some of the brand names are 'real' brands (eg, Adidas)
        # and if the customer is 'very unhappy', the generated text includes the 'brand'
        # name in a negative way
        #       Product Brand: %s, \n
        #       product_record ['value']['attributes']['brand'],
        product_details = """
                        Product Name: %s, \n
                        Product Description: %s, \n
                        Product Gender: %s """  % (
                                                      product_record ['value']['attributes']['title'],
                                                      product_record ['value']['attributes']['description'],
                                                      gender)

        line_count = random.randint (2, 10) # line count in response
        response_rule = RESPONSE_RULES % line_count

        system_message = PROMPT_HEADER % satisfaction_level
        system_message = "%s\n%s" % (system_message, product_details)
        system_message = "%s\n%s" % (system_message, response_rule)
        logging.debug ('System message: %s', system_message)

        user_message = "%s" % (product_details)
        logging.debug ('User message: %s', user_message)

        client = openai.OpenAI (api_key = OPENAI_KEY)
        try:
            openai_response = client.chat.completions.parse (
                 model = OPENAI_MODEL,
                 messages = [
                    {'role': 'system',
                     'content': system_message
                    },
                    {'role': 'user',
                     'content': user_message
                    }
                 ],
                 temperature = 0.5,
                 response_format = ResponseFormat
            )
            review_info = openai_response.choices [0].message.parsed
        except Exception as e:
            logging.error ('\ncannot generate review text for %s, error = %s' % (product_record ['value']['attributes']['pid'], str(e)))
            review_info = None
        return review_info

    def write_review_records (self, records, filename):
        with open (filename, 'w') as output:
            csv_writer = csv.writer (output, delimiter = ',')
            header_line = records [0].keys ()
            csv_writer.writerow (header_line)

            for row in records:
                csv_writer.writerow (row.values ())
            output.flush ()
            output.close ()
        return True

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    generator = ReviewGenerator ()

    # source_catalog
    source_catalog = generator.read_source_catalog (SOURCE_CATALOG_JSONL_IN)
    if source_catalog == None or len (source_catalog) == 0:
        logging.error ('Could not read source catalog')
        sys.exit (-1)

    review_records, review_records_debug = generator.generate_product_reviews (source_catalog)
    if review_records != None and len (review_records) > 0:
        generator.write_review_records (review_records, REVIEWS_OUTPUT_CSV_OUT)
        generator.write_review_records (review_records_debug, REVIEWS_OUTPUT_CSV_DEBUG_OUT)

    logging.info ('Finish generate review text...')

'''
                    a_review_record_debug = { 'reviewId': uuid.uuid4 (),
                                              'productId': product ['value']['attributes']['pid'],
                                              'title': review_title,
                                              'content': review_text,
                                              'rating': '',
                                              'submissionTimestamp': timestamp,
                                              'verifiedPurchaser': verified,
                                              'moderationStat': moderationStat,
                                              'helpfulVotes': helpfulVotes,
                                              'notHelpfulVotes': notHelpfulVotes,
                                              'actual_satisfaction_level': satisfaction_level,
                                              'actual_product_title': product ['value']['attributes']['title'],
                                              'actual_product_description': actual_product_description,
                                              'actual_product_url': product ['value']['attributes']['url']
                                          }
'''

