import logging
import os
import sys
import openai
import jsonlines
import json
import csv
import uuid
import random
import time
import copy
from pydantic import BaseModel, Field

import p13nTrainerConstants as p13ntc

SYSTEM_PROMPT_HEADER = """
You are a search query generator for an e-commerce website. The list of products available in this website is as follows:

Product List:
 {product_info_list}

Each product information is provided using these attributes:
 title,
 description,
 colors,
 gender,
 brand,
 categories

Your Task:
Understand the visitor's intent in the {initial_query} and generate a sequence of highly relevant refined queries. 
"""

RESPONSE_RULES = """
   - Each refined query must closely relate to the original query: {initial_query}.
   - Each query must apply only to the products offered by this e-commerce website.
   - Use {minimum_word_count} to {maximum_word_count} words in the query.
   - Do not generate previously generated queries: {previous_queries}
   - Use only this gender to generate queries: {gender}
   - For each query, provide a score between 1 to 100 indicating probability of its use in real world.
"""

USER_MESSAGE = """
Generate only {query_count} refined queries for each initial query.
"""

# given a 'primary' query, how many 'next-queries' to generate
REFINED_QUERY_COUNT_DEFAULT = 2 #@@@ 

# pydantic model for LLM response
class ResponseFormat (BaseModel):
    refined_queries: list[str] = Field (description = "list of generated queries")
    scores: list[int] = Field (description = "probability score for each generated query")


class RefinedQueryGenerator ():
    def __init__ (self):
        self._product_info_list = None
        self._training_config = None
        self._previous_queries = []
        return

    def set_selected_product_list (self, selected_product_list):
        self._product_info_list = selected_product_list
        return 

    def set_training_config (self, training_config):
        self._training_config = training_config
        return 

    def generate_refined_queries (self, start_query, refinement_depth):
        refined_queries_info = []
        refined_queries_records = []

        try:
            refined_queries_info = self._get_openAI_refined_queries (start_query)
            if refined_queries_info != None:
                refined_queries = refined_queries_info.refined_queries
                scores = refined_queries_info.scores
                for i in range (0, len (refined_queries)):
                    refined_query_record = { 'refined_query': refined_queries [i],
                                             'score': scores [i],
                                             'refinement_depth': refinement_depth
                                           }
                    refined_queries_records.append (refined_query_record)
                    self._previous_queries.append (refined_queries [i])
        except Exception as e:
            logging.error ('Exception in get openAI refined queries: %s' % str (e))

        return refined_queries_records

    def _prepare_product_info_list (self, source_catalog):
        # extract specific fields from each product
        product_info_list = []

        constrain_attrib = self._training_config ['catalog_constraints']['attribute']
        constrain_value = self._training_config ['catalog_constraints']['value']
        for product in source_catalog:
            if constrain_attrib in product ['value']['attributes']:
                attrib_value = product ['value']['attributes'][constrain_attrib].lower ()
            else:
                attrib_value = None

            if attrib_value == None:
                continue    # exclude this product since it does not have constrain_attrib
            elif product ['value']['attributes'][constrain_attrib].lower() != constrain_value:
                continue # exclude products with value != constrain_value

            #@@@ Due to openAI token limit, include only N products
            if len (product_info_list) > self._training_config ['catalog_constraints']['max_products_to_include']:
                continue

            colors = None
            if 'variants' in product ['value']:
                product_variants = product ['value']['variants'].values()
                for variant in product_variants:
                    if 'color' in variant ['attributes']:
                        if colors == None:
                            colors = variant ['attributes']['color']
                        else:
                            colors = '%s,%s' % (colors, variant ['attributes']['color'])
            if 'colors' == None:
                colors = ''

            categories = None
            category_paths = product ['value']['attributes']['category_paths']
            for branch in category_paths:
                for leaf in branch:
                    if categories == None:
                        categories = leaf ['name']
                    else:
                        categories = '%s,%s' % (categories, leaf ['name'])

            product_info = {
                            'title' : product ['value']['attributes']['title'],
                            'description': product ['value']['attributes']['description'],
                            'brand': product ['value']['attributes']['brand'],
                            'colors': colors,
                            'categories': categories
                           }
            product_info_list.append (product_info)
        return product_info_list

    # generate review text using openAI
    def _get_openAI_refined_queries (self, start_query):

        system_message = SYSTEM_PROMPT_HEADER.format (product_info_list = self._product_info_list, initial_query = start_query)
        response_rule = RESPONSE_RULES.format (
                                                initial_query = start_query,
                                                minimum_word_count = 3, 
                                                maximum_word_count = 5,
                                                gender = self._training_config ['training_constraints']['genders'],
                                                previous_queries = self._previous_queries
                                              )
        system_message = "%s\n%s" % (system_message, response_rule)
        logging.debug ('System message: %s', system_message)

        user_message = USER_MESSAGE.format (query_count = self._training_config ['training_parameters']['refined_query_count'])
        logging.debug ('User message: %s', user_message)

        client = openai.OpenAI (api_key = p13ntc.OPENAI_KEY)
        try:
            openai_response = client.chat.completions.parse (
                 model = p13ntc.OPENAI_MODEL,
                 messages = [
                    {'role': 'system',
                     'content': system_message
                    },
                    {'role': 'user',
                     'content': user_message
                    }
                 ],
                 temperature = 0.4,
                 response_format = ResponseFormat
            )
            refined_queries_info = openai_response.choices [0].message.parsed
        except Exception as e:
            logging.error ('\ncannot generate refined_queries, error = %s' % str(e))
            refined_queries_info = None
        return refined_queries_info

    def write_refined_query_records (self, records, filename):
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
    generator = RefinedQueryGenerator ()

    # product catalog info
    # source_catalog = read_source_catalog (p13ntc.SOURCE_CATALOG_JSONL_IN)
    source_catalog = []
    with open (p13ntc.SOURCE_CATALOG_JSONL_IN, 'r') as input:
        reader = jsonlines.Reader (input)
        for product in reader:
            source_catalog.append (product)
        input.close ()
    generator.set_source_catalog (source_catalog)

    training_config = None
    with open (p13ntc.TRAINING_CONFIG_JSON_IN, 'r') as input:
        training_config = json.load (input)
        input.close ()
    generator.set_training_config (training_config)

    initial_query = training_config ['training_parameters']['initial_query']
    refined_query_records = generator.generate_refined_queries (initial_query)
    if refined_query_records != None and len (refined_query_records) > 0:
        generator.write_refined_query_records (refined_query_records, p13ntc.REFINED_QUERIES_OUTPUT_CSV_OUT)

    logging.info ('Finish generate next queries text...')

'''
RESPONSE_RULES_SAVE = ===
Rules:
- Each query you generate must apply to the products offered by this e-commerce site.
- Each query should help the customers to further narrow their search for products of their choice.
- Use the title and description of each product to generate your query.
- Apply rule attribute {select_attribute_name} = {select_attribute_value} to generate your query.
- Do not use attribute {exclude_attribute} when generating your query.
- For each query, separately provide a score between 1 to 100 indicating probability of its use in real world.
- Generate maximum {query_count} refined queries.
- Use {minimum_word_count} to {maximum_word_count} words in the query.
- Be sure to use simple language to construct each query.
- Return only a plain list, one query per line.
===

USER_MESSAGE_SAVE = ===
Each next query must meaningfully follow the query {initial_query} and must apply to the products offered by this site. You must restrict the next query using the categories, colors, gender and brands listed below \n
categories: {categories}\n
colors: {colors}\n
gender: {gender}\n
brands: {brands}\n
===
'''
