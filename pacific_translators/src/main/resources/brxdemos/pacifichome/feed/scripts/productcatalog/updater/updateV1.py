# V1 changes
# -- generate enhanced description and add it as another attribute
# -- introduct 'category*' attributes for L0, L1, L2

import logging
import copy
import openai

from updateBase import UpdateBase
import updaterConstants as uc
import updaterConstantsV1 as ucv1

OPENAI_KEY = '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'
OPENAI_MODEL = 'gpt-4o'
HTTP_STATUS_OK = 200

class UpdateV1 (UpdateBase) :

    def __init__ (self):
        logging.info ('Perform update, version v1')
        super().__init__ ()
        return

    # override base class method
    def _perform_record_update (self, record):
        updated_record = self._perform_update_internal (record)
        return updated_record

    def _perform_update_internal (self, record):
        logging.debug ('process pid = %s', record ['value']['attributes']['pid'])

        updated_record = copy.deepcopy  (record)
        # enhanced_description
        description_enh = self._generate_enhanced_description (record)
        if description_enh != None:
            updated_record ['value']['attributes'][ucv1.ATTRIB_NAME_ENHANCED_DESCRIPTION] = description_enh

        # category levels
        category_levels = self._collect_category_levels (record)
        for i in range (0, ucv1.MAX_CATEGORY_LEVELS):
            level_name = ''
            if (i < len (category_levels)) and (category_levels [i]):
                level_name = category_levels [i]
            attrib_name = '%s%s' % (ucv1.PREAMBLE_ATTRIB_NAME_CATEGORY_LEVEL, i+1)
            updated_record ['value']['attributes'][attrib_name] = level_name
 
        return updated_record

    # DEBUGGING --- generate enhanced description using openAI
    def _generate_enhanced_description (self, record):
        return record ['value']['attributes']['description']

    def _generate_enhanced_description_TRUE (self, record):
        txt_prompt = ''

        # collect valid values of specific attributes (product and its variants)
        product_attribs = record ['value']['attributes'].keys()
        for attrib in ucv1.ATTRIB_LIST_FOR_ENHANCED_DESCRIPTION_PROMPT:
            if (attrib in product_attribs) and (record ['value']['attributes'][attrib]):
                txt_prompt = '%s %s' % (txt_prompt, record ['value']['attributes'][attrib])

        if ('variants' in record ['value']) and (record ['value']['variants']):
            variant_list = record ['value']['variants']
            for variant_id, variant_obj in variant_list.items():
                variant_attribs = variant_obj ['attributes'].keys ()
                for attrib in ucv1.ATTRIB_LIST_FOR_ENHANCED_DESCRIPTION:
                    if (attrib in variant_attribs) and (variant_obj ['attributes'] [attrib]):
                        txt_prompt = '%s %s' % (txt_prompt, variant_obj ['attributes'][attrib])
        logging.debug ('txt prompt = %s' % txt_prompt)

        # use txt_prompt in openAI API call
        client = openai.OpenAI (api_key=OPENAI_KEY)
        try:
            openai_response = client.chat.completions.create (
                model = OPENAI_MODEL,
                messages = [
                    {'role': 'system', 'content': '''
                    You are an ecommerce expert who helps provide additional information on products given a products JSON.

                    Rules:
                    Return only the description
                    Your responses are limited to 10 lines.
                    The information you provide should be helpful to the user in determining where they might use this product.
                    You can speak about any of its attributes but be sure to use human language.
                    Do not mention variants unless there are multiple. 
                    Ignore null values.
                    It should include info like: who the product is for, product rating info, complementary products and variants.
                    Answers should be definitive
                    '''},
                    {'role': 'user',   'content': txt_prompt }
                ],
                temperature = 0.5
            )
            resp_text = openai_response.choices [0].message.content
            # log this resp_text along with pid. In case openAI fails for some reason, we could use previous
            # run's logs and avoid re-making those calls
            logging.debug ('OpenAI: %s\t%s' % (record ['value']['attributes']['pid'], resp_text))

        except Exception as e:
            logging.error ('cannot generate openAI text for %s, error = %s' % (record ['value']['attributes']['pid'], str(e)))
            resp_text = None
        return resp_text

    # use bread_crumb to return list of [l0, l1, ...] of first category
    def _collect_category_levels (self, record):
        category_levels = []

        category_paths = record ['value']['attributes']['category_paths']
        if (category_paths == None) or (len (category_paths) == 0):
            return category_levels # zero-length list

        branch_path_0 = category_paths [0]   # use first branch
        for branch_nodes in branch_path_0:
            category_levels.append (branch_nodes ['name'])
        return category_levels

if __name__ == '__main__':
    u = UpdateV1 ()
    t = u._generate_enhanced_description (None)


