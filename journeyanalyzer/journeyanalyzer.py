# use .tsv created by trafficgenerator to build a 
# query -> segmented pid-list that has been 'travel'd'

import logging
import csv
import sys
import os
import glob

import journeyanalyzer_constants as jac

class JourneyAnalyzer ():

    def __init__ (self):
        # list of struct 
        #  {"qry": { 
        #            "seg1": [
        #               { pid, browse, atc, convert}, 
        #               {...}, 
        #               ... 
        #            ],
        #            "seg2": [
        #               { pid, browse, atc, convert}, 
        #               {...}, 
        #               ... 
        #            ]
        #          }
        #  }
        self._query_data_list = []
        return

    def read_generator_data (self, tsv_logpath):
        _log_records = []

        with open (tsv_logpath, 'r') as tsv_logfile:
            dict_reader = csv.DictReader (tsv_logfile, delimiter='\t')
            for row in dict_reader:
                if (row ['JourneyType'] == 'predefined') and (row ['SessionType'] != '-'): #use only 'predefined' journey records
                    _log_records.append (row)
            tsv_logfile.close ()

        _sorted_log_records = sorted (_log_records, key=lambda record: record ['UserId'])
        return _sorted_log_records

    def build_query_journey (self, sorted_log_records):
        start_at = 0
        while start_at < len (sorted_log_records):
            # collect records for one user
            user_id, user_records = self._collect_user_records (sorted_log_records, start_at)
            logging.debug ('\n\n--- User records, userid = %s, start_at %s, record count = %s ' % (user_id, start_at, len (user_records)))

            # for this user, collect their query records
            # sort each user's records according to 'stepStart'
            _sorted_user_records = sorted (user_records, key=lambda record: int (record ['stepStart']))
            # @@@
            #for i in range (0,len (_sorted_user_records)):
            #    logging.debug (_sorted_user_records [i])

            self._collect_query_journey_for_user (_sorted_user_records)
            start_at = start_at + len (_sorted_user_records)
        return

    def write_query_journey (self):
        # first generate a tsv-formatted-list
        if (self._query_data_list == None) or (len (self._query_data_list) == 0):
            logging.warning ('query data list is empty')
            return

        log_list = []
        log_record_template = { 'query': '', 
                                'segment': '',
                                'session_type': '',
                                'pid': '',
                                'flow': '',
                                'price': ''
                     }

        for query_data in self._query_data_list:
            #query_str, query_data_obj = query_data.items ()
            query_str = list (query_data.keys ())[0]
            query_data_obj = query_data [query_str]
            for segname, segment_sessions in query_data_obj.items ():
                for a_segment_session in segment_sessions:
                    log_record = log_record_template.copy ()
                    log_record ['query'] = query_str
                    log_record ['segment'] = segname
                    log_record ['session_type'] = a_segment_session ['session_type']
                    log_record ['pid'] = a_segment_session ['pid']
                    log_record ['flow'] = a_segment_session ['flow']
                    log_record ['price'] = a_segment_session ['price']
                    log_list.append (log_record)

        with open (jac.FILENAME_QUERY_JOURNEY_LOG_TSV_OUT,  'w') as journey_log_file:
            tsv_writer = csv.writer (journey_log_file, delimiter = '\t')
            header_line = log_list [0].keys ()
            tsv_writer.writerow (header_line)

            for log_record in log_list:
                tsv_writer.writerow (log_record.values())
            journey_log_file.close ()

        return

    def _collect_user_records (self, sorted_log_records, start_at):
        _user_records = []

        _user_id = sorted_log_records [start_at]['UserId']
        for i in range (start_at, len(sorted_log_records)): 
            if _user_id != sorted_log_records [i]['UserId']:
                break
            _user_records.append (sorted_log_records [i])
        return _user_id, _user_records

    # single journey can have multiple sessions
    def _collect_query_journey_for_user (self, user_records):
        session_record_start_num = 0
        while session_record_start_num < len (user_records):
            session_step_record = user_records [session_record_start_num]
            session_type = session_step_record ['SessionType']
            segment = session_step_record ['User Segment']

            if session_type == 'c':
                _session_data, count = self._process_session_type_c (user_records, session_record_start_num)
                if (_session_data != None):
                    self._accumulate_session_data (segment, 'c', _session_data)
                session_record_start_num = session_record_start_num + count + 1
            elif session_type == 's':
                _session_data, count = self._process_session_type_s (user_records, session_record_start_num)
                if (_session_data != None):
                    self._accumulate_session_data (segment, 's', _session_data)
                session_record_start_num = session_record_start_num + count + 1
            elif session_type == 'ps2s':
                _session_data, count = self._process_session_type_ps2s (user_records, session_record_start_num)
                if (_session_data != None):
                    self._accumulate_session_data (segment, 'ps2s', _session_data)
                session_record_start_num = session_record_start_num + count + 1
            elif session_type == 'ps2c':
                _session_data, count = self._process_session_type_ps2c (user_records, session_record_start_num)
                if (_session_data != None):
                    self._accumulate_session_data (segment, 'ps2c', _session_data)
                session_record_start_num = session_record_start_num + count + 1
            elif session_type == 'pc2s':
                _session_data, count = self._process_session_type_pc2s (user_records, session_record_start_num)
                if (_session_data != None):
                    self._accumulate_session_data (segment, 'pc2s', _session_data)
                session_record_start_num = session_record_start_num + count + 1
            elif session_type == 'pc2c':
                _session_data, count = self._process_session_type_pc2c (user_records, session_record_start_num)
                if (_session_data != None):
                    self._accumulate_session_data (segment, 'pc2c', _session_data)
                session_record_start_num = session_record_start_num + count + 1
            elif session_type == 'gp':
                _session_data, count = self._process_session_type_gp (user_records, session_record_start_num)
                if (_session_data != None):
                    self._accumulate_session_data (segment, 'gp', _session_data)
                session_record_start_num = session_record_start_num + count + 1
            elif session_type == 'gt':
                _session_data, count = self._process_session_type_gt (user_records, session_record_start_num)
                if (_session_data != None):
                    self._accumulate_session_data (segment, 'gt', _session_data)
                session_record_start_num = session_record_start_num + count + 1
            elif session_type == 'gc':
                _session_data, count = self._process_session_type_gc (user_records, session_record_start_num)
                if (_session_data != None):
                    self._accumulate_session_data (segment, 'gc', _session_data)
                session_record_start_num = session_record_start_num + count + 1
            elif session_type == 's2s':
                _session_data_1, _session_data_2, count = self._process_session_type_s2s (user_records, session_record_start_num)
                if (_session_data_1 != None):
                    self._accumulate_session_data (segment, 's2s', _session_data_1)
                if (_session_data_2 != None):
                    self._accumulate_session_data (segment, 's2s', _session_data_2)
                session_record_start_num = session_record_start_num + count + 1
            elif session_type == 's2c':
                _session_data_1, _session_data_2, count = self._process_session_type_s2c (user_records, session_record_start_num)
                if (_session_data_1 != None):
                    self._accumulate_session_data (segment, 's2c', _session_data_1)
                if (_session_data_2 != None):
                    self._accumulate_session_data (segment, 's2c', _session_data_2)
                session_record_start_num = session_record_start_num + count + 1
            elif session_type == 'c2s':
                _session_data_1, _session_data_2, count = self._process_session_type_c2s (user_records, session_record_start_num)
                if (_session_data_1 != None):
                    self._accumulate_session_data (segment, 'c2s', _session_data_1)
                if (_session_data_2 != None):
                    self._accumulate_session_data (segment, 'c2s', _session_data_2)
                session_record_start_num = session_record_start_num + count + 1
            elif session_type == 'c2c':
                _session_data_1, _session_data_2, count = self._process_session_type_c2c (user_records, session_record_start_num)
                if (_session_data_1 != None):
                    self._accumulate_session_data (segment, 'c2c', _session_data_1)
                if (_session_data_2 != None):
                    self._accumulate_session_data (segment, 'c2c', _session_data_2)
                session_record_start_num = session_record_start_num + count + 1
            else:
                logging.debug ('ignore session type %s' % session_type)
                session_record_start_num = session_record_start_num + 1
        return

    def _process_session_type_s (self, user_records, session_record_start_num):
        logging.debug ('process session type %s' % 's')

        valid_count = self._validate_records (user_records, session_record_start_num, 5, 's')
        if (valid_count != 5):
            return (None, valid_count)

        flow = '' # browse, atc, convert
        start_num = session_record_start_num

        count = 0
        query = user_records [start_num + count]['Query']

        count = 2
        pid = user_records [start_num + count]['Query']
        flow = 'b'

        count = 3
        flow = flow + 'a'

        count = 4
        flow = flow + 'c'
        price = self._eval_price (user_records [start_num + count]['Query'])

        session_data = { 'query': query,
                         'pid': pid,
                         'flow': flow,
                         'price': price
                        }
        return (session_data, count)

    def _process_session_type_c (self, user_records, session_record_start_num):
        logging.debug ('process session type %s' % 'c')

        valid_count = self._validate_records (user_records, session_record_start_num, 5, 'c')
        if (valid_count != 5):
            return (None, valid_count)

        flow = '' # browse, atc, convert
        start_num = session_record_start_num

        count = 0
        query = 'cat-' + user_records [start_num + count]['Query']

        count = 2
        pid = user_records [start_num + count]['Query']
        flow = 'b'

        count = 3
        flow = flow + 'a'

        count = 4
        flow = flow + 'c'
        price = self._eval_price (user_records [start_num + count]['Query'])

        session_data = { 'query': query,
                         'pid': pid,
                         'flow': flow,
                         'price': price
                        }
        return (session_data, count)

    # partial s2s - first search is abandoned
    def _process_session_type_ps2s (self, user_records, session_record_start_num):
        logging.debug ('process session type %s' % 'ps2s')

        valid_count = self._validate_records (user_records, session_record_start_num, 7, 'ps2s')
        if (valid_count != 7):
            return (None, valid_count)

        flow = '' # browse, atc, convert
        start_num = session_record_start_num

        count = 2
        query = user_records [start_num + count]['Query']

        count = 4
        pid = user_records [start_num + count]['Query']
        flow = 'b'

        count = 5
        flow = flow + 'a'

        count = 6
        flow = flow + 'c'
        price = self._eval_price (user_records [start_num + count]['Query'])

        session_data = { 'query': query,
                         'pid': pid,
                         'flow': flow,
                         'price': price
                        }
        return (session_data, count)

    # partial ps2c - first search is abandoned
    def _process_session_type_ps2c (self, user_records, session_record_start_num):
        logging.debug ('process session type %s' % 'ps2c')

        valid_count = self._validate_records (user_records, session_record_start_num, 7, 'ps2c')
        if (valid_count != 7):
            return (None, valid_count)

        flow = '' # browse, atc, convert
        start_num = session_record_start_num

        count = 2
        query = 'cat-' + user_records [start_num + count]['Query']

        count = 4
        pid = user_records [start_num + count]['Query']
        flow = 'b'

        count = 5
        flow = flow + 'a'

        count = 6
        flow = flow + 'c'
        price = self._eval_price (user_records [start_num + count]['Query'])

        session_data = { 'query': query,
                         'pid': pid,
                         'flow': flow,
                         'price': price
                        }
        return (session_data, count)

    # partial c2c - first search is abandoned
    def _process_session_type_pc2c (self, user_records, session_record_start_num):
        logging.debug ('process session type %s' % 'pc2c')

        valid_count = self._validate_records (user_records, session_record_start_num, 7, 'pc2c')
        if (valid_count != 7):
            return (None, valid_count)

        flow = '' # browse, atc, convert
        start_num = session_record_start_num

        count = 2
        query = 'cat-' + user_records [start_num + count]['Query']

        count = 4
        pid = user_records [start_num + count]['Query']
        flow = 'b'

        count = 5
        flow = flow + 'a'

        count = 6
        flow = flow + 'c'
        price = self._eval_price (user_records [start_num + count]['Query'])

        session_data = { 'query': query,
                         'pid': pid,
                         'flow': flow,
                         'price': price
                        }
        return (session_data, count)

    def _process_session_type_gp (self, user_records, session_record_start_num):
        logging.debug ('process session type %s' % 'gp')

        valid_count = self._validate_records (user_records, session_record_start_num, 5, 'gp')
        if (valid_count != 5):
            return (None, valid_count)

        flow = '' # browse, atc, convert
        start_num = session_record_start_num

        count = 0
        query = user_records [start_num + count]['Query']

        count = 2
        pid = user_records [start_num + count]['Query']
        flow = 'b'

        count = 3
        flow = flow + 'a'

        count = 4
        flow = flow + 'c'
        price = self._eval_price (user_records [start_num + count]['Query'])

        session_data = { 'query': query,
                         'pid': pid,
                         'flow': flow,
                         'price': price
                        }

        return (session_data, count)

    def _process_session_type_gt (self, user_records, session_record_start_num):
        logging.debug ('process session type %s' % 'gt')

        valid_count = self._validate_records (user_records, session_record_start_num, 6, 'gt')
        if (valid_count != 6):
            return (None, valid_count)

        flow = '' # browse, atc, convert
        start_num = session_record_start_num

        count = 1
        query = user_records [start_num + count]['Query']

        count = 3
        pid = user_records [start_num + count]['Query']
        flow = 'b'

        count = 4
        flow = flow + 'a'

        count = 5
        flow = flow + 'c'
        price = self._eval_price (user_records [start_num + count]['Query'])

        session_data = { 'query': query,
                         'pid': pid,
                         'flow': flow,
                         'price': price
                        }

        return (session_data, count)

    def _process_session_type_gc (self, user_records, session_record_start_num):
        logging.debug ('process session type %s' % 'gc')

        valid_count = self._validate_records (user_records, session_record_start_num, 6, 'gc')
        if (valid_count != 6):
            return (None, valid_count)

        flow = '' # browse, atc, convert
        start_num = session_record_start_num

        count = 1
        query = 'cat-' + user_records [start_num + count]['Query']

        count = 3
        pid = user_records [start_num + count]['Query']
        flow = 'b'

        count = 4
        flow = flow + 'a'

        count = 5
        flow = flow + 'c'
        price = self._eval_price (user_records [start_num + count]['Query'])

        session_data = { 'query': query,
                         'pid': pid,
                         'flow': flow,
                         'price': price
                        }

        return (session_data, count)

    def _process_session_type_c2s (self, user_records, session_record_start_num):
        logging.debug ('process session type %s' % 'c2s')

        valid_count = self._validate_records (user_records, session_record_start_num, 10, 'c2s')
        if (valid_count != 10):
            return (None, None, valid_count)

        flow = '' # browse, atc, convert
        start_num = session_record_start_num

        count = 0
        query = 'cat-' + user_records [start_num + count]['Query']  # category

        count = 2
        pid = user_records [start_num + count]['Query']
        flow = 'b'

        count = 3
        flow = flow + 'a'

        count = 4
        flow = flow + 'c'
        price = self._eval_price (user_records [start_num + count]['Query'])

        session_data_1 = { 'query': query,
                         'pid': pid,
                         'flow': flow,
                         'price': price
                        }

        count = 5
        query = user_records [start_num + count]['Query']  # search term 

        count = 7
        pid = user_records [start_num + count]['Query']
        flow = 'b'

        count = 8
        flow = flow + 'a'

        count = 9
        flow = flow + 'c'
        price = self._eval_price (user_records [start_num + count]['Query'])

        session_data_2 = { 'query': query,
                         'pid': pid,
                         'flow': flow,
                         'price': price
                        }


        return (session_data_1, session_data_2, count)

    def _process_session_type_c2c (self, user_records, session_record_start_num):
        logging.debug ('process session type %s' % 'c2c')

        valid_count = self._validate_records (user_records, session_record_start_num, 10, 'c2c')
        if (valid_count != 10):
            return (None, None, valid_count)

        flow = '' # browse, atc, convert
        start_num = session_record_start_num

        count = 0
        query = 'cat-' + user_records [start_num + count]['Query']  # category

        count = 2
        pid = user_records [start_num + count]['Query']
        flow = 'b'

        count = 3
        flow = flow + 'a'

        count = 4
        flow = flow + 'c'
        price = self._eval_price (user_records [start_num + count]['Query'])

        session_data_1 = { 'query': query,
                         'pid': pid,
                         'flow': flow,
                         'price': price
                        }

        count = 5
        query = 'cat-' + user_records [start_num + count]['Query']  # category 

        count = 7
        pid = user_records [start_num + count]['Query']
        flow = 'b'

        count = 8
        flow = flow + 'a'

        count = 9
        flow = flow + 'c'
        price = self._eval_price (user_records [start_num + count]['Query'])

        session_data_2 = { 'query': query,
                         'pid': pid,
                         'flow': flow,
                         'price': price
                        }


        return (session_data_1, session_data_2, count)

    def _process_session_type_s2s (self, user_records, session_record_start_num):
        logging.debug ('process session type %s' % 's2s')

        valid_count = self._validate_records (user_records, session_record_start_num, 10, 's2s')
        if (valid_count != 10):
            return (None, None, valid_count)

        flow = '' # browse, atc, convert
        start_num = session_record_start_num

        count = 0
        query = user_records [start_num + count]['Query']

        count = 2
        pid = user_records [start_num + count]['Query']
        flow = 'b'

        count = 3
        flow = flow + 'a'

        count = 4
        flow = flow + 'c'
        price = self._eval_price (user_records [start_num + count]['Query'])

        session_data_1 = { 'query': query,
                         'pid': pid,
                         'flow': flow,
                         'price': price
                        }

        count = 5
        query = user_records [start_num + count]['Query']  # search term 

        count = 7
        pid = user_records [start_num + count]['Query']
        flow = 'b'

        count = 8
        flow = flow + 'a'

        count = 9
        flow = flow + 'c'
        price = self._eval_price (user_records [start_num + count]['Query'])

        session_data_2 = { 'query': query,
                         'pid': pid,
                         'flow': flow,
                         'price': price
                        }

        return (session_data_1, session_data_2, count)

    def _process_session_type_s2c (self, user_records, session_record_start_num):
        logging.debug ('process session type %s' % 's2c')

        valid_count = self._validate_records (user_records, session_record_start_num, 10, 's2c')
        if (valid_count != 10):
            return (None, None, valid_count)

        flow = '' # browse, atc, convert
        start_num = session_record_start_num

        count = 0
        query = user_records [start_num + count]['Query']

        count = 2
        pid = user_records [start_num + count]['Query']
        flow = 'b'

        count = 3
        flow = flow + 'a'

        count = 4
        flow = flow + 'c'
        price = self._eval_price (user_records [start_num + count]['Query'])

        session_data_1 = { 'query': query,
                         'pid': pid,
                         'flow': flow,
                         'price': price
                        }

        count = 5
        query = 'cat-' + user_records [start_num + count]['Query']  # category

        count = 7
        pid = user_records [start_num + count]['Query']
        flow = 'b'

        count = 8
        flow = flow + 'a'

        count = 9
        flow = flow + 'c'
        price = self._eval_price (user_records [start_num + count]['Query'])

        session_data_2 = { 'query': query,
                         'pid': pid,
                         'flow': flow,
                         'price': price
                        }

        return (session_data_1, session_data_2, count)

    # partial c2s - first search is abandoned
    def _process_session_type_pc2s (self, user_records, session_record_start_num):
        logging.debug ('process session type %s' % 'pc2s')

        valid_count = self._validate_records (user_records, session_record_start_num, 7, 'pc2s')
        if (valid_count != 7):
            return (None, valid_count)

        flow = '' # browse, atc, convert
        start_num = session_record_start_num

        count = 2
        query = user_records [start_num + count]['Query']

        count = 4
        pid = user_records [start_num + count]['Query']
        flow = 'b'

        count = 5
        flow = flow + 'a'

        count = 6
        flow = flow + 'c'
        price = self._eval_price (user_records [start_num + count]['Query'])

        session_data = { 'query': query,
                         'pid': pid,
                         'flow': flow,
                         'price': price
                        }
        return (session_data, count)

    # if invalid, returns numRecords to skip to start next session. Otherwise return 'count'
    def _validate_records (self, user_records, start_num, count, req_session_type):
        # check if the min number of records are available
        # if we don't have necessary count of input log records, ignore this journey
        if ((start_num + count) > len (user_records)):
            logging.warning ('validation count too high, session_type: %s, start_num: %s, count: %s, max_len: %s' % (req_session_type, start_num, count, len (user_records)))
            if (user_records [start_num]['UserId'] == 'tg1-7529-3000631-567812'):
                print ('Stop here') # @@@
            # skip all records for the req_session_type
            rem_count = len (user_records) - start_num
            for i in range (0, rem_count):
                if (user_records [start_num + i]['SessionType'] != req_session_type):
                    return i - 1
            if ((i+1) == rem_count):    # if at end-of session records
                return i 

        # if session caused an exception, ignore this query-journey
        for i in range (0, count):
            if (user_records [start_num + i]['Step'].find ('exception') >= 0):  # exception_restart
                logging.warning ('validation failed, exception restart: session_type: %s, start_num: %s, num: %s' % (req_session_type, start_num, i))
                return i

        # if we don't have necessary type of input log records, ignore this journey
        for i in range (0, count):
            if (user_records [start_num + i]['SessionType'] != req_session_type):
                logging.warning ('validation failed, session_type mismatch: req: %s, got: %s, start_num: %s, num: %s' % (req_session_type, user_records [start_num + i]['SessionType'], start_num, i))
                return (i-1)

        # otherwise, query-journey is valid
        logging.info ('validation successful, session_type: %s' % req_session_type)
        return count 

    # string between 'p and following ','
    # sample: !i108367'nVan Heusen Men Grey Regular Fit Solid Formal Trousers'q1.0'p28.3, value = 28.300000, orderId = 2024_11_17_46
    def _eval_price (self, convert_query):
        price = ''
        start_indx = convert_query.find ("'p")
        if (start_indx >= 0):
            end_indx = convert_query.find (',', start_indx)
            if (end_indx >= 0):
                price = convert_query [start_indx+2:end_indx]
            else:
                price = convert_query [start_indx+2]
        return price

    def _accumulate_session_data (self, segment, session_type, session_data):
        query_str = session_data ['query']

        query_data_obj = self._lookup_query_data (query_str)
        if (query_data_obj == None):
            query_data = {}
            query_data [query_str] = {}
            self._query_data_list.append (query_data)
            query_data_obj = query_data [query_str]

        if segment not in query_data_obj: # 'low-value', 'high-value', ...
            query_data_obj [segment] = []
        query_segment_data = query_data_obj [segment]

        query_segment_session_data = { 'session_type': session_type,
                                       'pid': session_data ['pid'],
                                       'flow': session_data ['flow'],
                                       'price': session_data ['price']
                                      }
        query_segment_data.append (query_segment_session_data)
        return

    def _lookup_query_data (self, new_query):
        if (len (self._query_data_list) == 0):
            return None

        for query_data_elem in self._query_data_list:
            query_str = list (query_data_elem.keys())[0]
            if query_str == new_query:
                return query_data_elem [query_str]
        return None 

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    if (len (sys.argv) < 2):
        logging.error ('Journeyanalyzer <dirpath>')
    else:
        src_dir = sys.argv [1]
        if (os.path.isdir (src_dir) == False):
            logging.error ('%s is not a directory' % src_dir)
        else:
            glob_path = '%s/%s' % (src_dir, '*.tsv')
            j = JourneyAnalyzer ()
            for log_file_path in glob.glob (glob_path):
                logging.debug ('log file path: %s' % log_file_path)

                sorted_log_records = j.read_generator_data (log_file_path)
                logging.debug ('predefined journey log count: %s' % len (sorted_log_records))

                j.build_query_journey (sorted_log_records)
                j.write_query_journey ()

    logging.info ('Finish...')


