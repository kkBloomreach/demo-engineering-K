import logging
import sys
import jsonlines
import csv
import random
import json

from gen_refined_queries import RefinedQueryGenerator
from eval_product_counts import EvalProductCounts
from product_list_selector import ProductListSelector
import p13nTrainerConstants as p13ntc

class P13NTrainer ():
    def __init__ (self):
        self._refined_query_generator = None
        self._product_count_evaluator = None
        self._product_list_selector = None
        self._training_config = None
        self._training_data = []
        return

    def read_source_catalog (self, filename):
        source_catalog = []
        with open (filename, 'r') as input:
            reader = jsonlines.Reader (input)
            for product in reader:
                source_catalog.append (product)
            input.close ()
        return source_catalog

    def read_training_config (self, filename):
        training_config = None
        with open (filename, 'r') as input:
            training_config = json.load (input)
            input.close ()
        return training_config

    # set training parameters
    def init_trainer (self, source_catalog, training_config):
        self._training_config = training_config

        self._product_list_selector = ProductListSelector ()
        self._product_list_selector.set_training_config (training_config)
        self._product_list_selector.set_source_catalog (source_catalog)

        # next query generator
        self._refined_query_generator = RefinedQueryGenerator ()
        self._refined_query_generator.set_training_config (training_config)
        
        # numFound evaluator = use discovery API call to verify valid results
        self._product_count_evaluator = EvalProductCounts ()
        self._product_count_evaluator.set_training_config (training_config)

        return True

    def collect_training_data (self):
        # using training_config catalog constraints, collect selected_product_list
        selected_product_list = self._product_list_selector.build_selected_product_list ()
        if selected_product_list == None or len (selected_product_list) == 0:
            logging.error ('Insufficient selected product list')
            return None
        self._refined_query_generator.set_selected_product_list (selected_product_list)

        # training data for the 'initial' query itself
        initial_query_list = self._training_config ['training_parameters']['initial_query_list']
        for start_query in initial_query_list:
            logging.info ('Generating refined queries for start_query: %s' % start_query)

            # start with 'start_query'
            initial_query_record = { 'refined_query': start_query,
                                     'score':         random.randint (10,90),
                                     'refinement_depth': 0 
                                   }

            logging.info ('collect_refined_query training data, depth  = %s, initial query: %s, refined query: %s' % 
                                                                                                     (initial_query_record ['refinement_depth'],
                                                                                                      '-',
                                                                                                      start_query)) 
            # training data for refined queries, containing product_count, ...
            # invokes recursively upto MAX levels
            self._collect_refined_queries_training_data ('-', initial_query_record) # eg, q0
            if len (self._training_data) == 0:
                logging.error ('Could not generate training_data for initial query: %s' % start_query)

        return self._training_data

    # INTERNAL METHODS
    # Invoked recursively upto MAX levels
    def _collect_refined_queries_training_data (self, initial_query, refined_query_record):
        if refined_query_record ['refinement_depth'] > p13ntc.MAX_REFINED_QUERY_LEVELS:
            return # exit recursion
        else:
            # collect training data for current query itself
            response_numfound, pid_list = self._product_count_evaluator.collect_query_response_count (refined_query_record ['refined_query'])
            if response_numfound > 0 and len(pid_list) > 0:
                training_data_record = {
                                        'initial_query': initial_query,
                                        'refined_query': refined_query_record ['refined_query'],
                                        'score':         refined_query_record ['score'],
                                        'numFound':      response_numfound,
                                        'pid_list':      pid_list
                                       }
                self._training_data.append (training_data_record)
            else:
                logging.warning ('Rejecting refined query due to lack of numfound: %s' % refined_query_record ['refined_query'])

            # then go to next 'depth' level
            initial_query = refined_query_record ['refined_query']
            refined_query_records = self._refined_query_generator.generate_refined_queries (initial_query,
                                                                                            refined_query_record ['refinement_depth']+1)

            # sometimes (randomly) LLM generates less (or more) than required refined queries. Restrict to required-max
            select_count = min (self._training_config ['training_parameters']['refined_query_count'], len (refined_query_records))
            refind_query_records = refined_query_records [0:select_count]
            logging.info ('For query: %s, LLM generated refined query count: %s' % (initial_query, len (refined_query_records)))

            for refined_query_record in refined_query_records:
                # make recursive call with 
                logging.info ('collect_refined_query training data, depth  = %s, initial query: %s, refined query: %s' % 
                                                                                                     (refined_query_record ['refinement_depth'],
                                                                                                      initial_query,
                                                                                                      refined_query_record ['refined_query']))
                self._collect_refined_queries_training_data (initial_query, refined_query_record) 
        return

    def write_refined_query_records (self, records, filename):
        with open (filename, 'w') as output:
            csv_writer = csv.writer (output, delimiter = '\t')
            header_line = records [0].keys ()
            csv_writer.writerow (header_line)

            for row in records:
                csv_writer.writerow (row.values ())
            output.flush ()
            output.close ()
        return True

if __name__ == '__main__':
    logging.basicConfig (level = logging.INFO)
    logging.info ('Start p13n trainer')

    p13n_trainer = P13NTrainer ()

    # source catalog
    source_catalog = p13n_trainer.read_source_catalog (p13ntc.SOURCE_CATALOG_JSONL_IN)
    if source_catalog == None:
        logging.error ('Could not read source catalog')
        sys.exit (-1)

    # training config, ...
    training_config = p13n_trainer.read_training_config (p13ntc.TRAINING_CONFIG_JSON_IN)
    if training_config == None:
        logging.error ('Could not read training config')
        sys.exit (-2)

    # prepare traininer
    try:
        p13n_trainer.init_trainer (source_catalog, training_config)
    except Exception as e:
        logging.fatal ('Cannot prepare p13n trainer, %s' % str (e))
        sys.exit (-3)

    # run the trainer
    try:
        training_data = p13n_trainer.collect_training_data ()
    except Exception as e:
        logging.fatal ('Cannot collect training data, %s' % str (e))
        sys.exit (-4)

    # write results
    p13n_trainer.write_refined_query_records (training_data, p13ntc.REFINED_QUERIES_OUTPUT_TSV_OUT)
    logging.info ('Finish p13n trainer')


''' ==========================
        # training-data for 'initial-query' itself
        refined_query_records, training_data = self._collect_initial_query_training_data (initial_query, training_data)

        # training data for next queries, containing product_count, ...
        #for level in range (1, p13ntc.MAX_REFINED_QUERY_LEVELS):
        #    refined_query_records, training_data = self._collect_refined_queries_training_data (start_query, training_data)
        #    if refined_query_records != None:
        #        for refined_query_record in refined_query_records:
        #            refined_query_records, training_data = self._collect_refined_queries_training_data (refined_query_record ['refined_query'], training_data)
        #    else:
        #        logging.error ('Could not generate next query records for initial query: %s' % initial_query)

    def _collect_initial_query_training_data_UNUSED (self, initial_query):

        # eval product count for 'initial query' itself
        response_numfound, pid_list = self._product_count_evaluator.collect_query_response_count (initial_query)
        training_data_record = {
                                 'initial_query': '-',
                                 'refined_query': initial_query,
                                 'score':         initial_query_record ['score'],
                                 'numFound':      response_numfound,
                                 'pid_list':      pid_list
                               }
        self._training_data.append (training_data_record)
        return initial_query_record
'''

