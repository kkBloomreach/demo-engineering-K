# base class for actual 'revision update' task to be performed
import logging

class RevisionBase ():

    def __init__ (self):
        return

    def set_english_source_records (self, source_records):
        self._english_source_records = source_records
        return

    def set_german_source_records (self, source_records):
        self._german_source_records = source_records
        return

    def set_inject_av_map (self, inject_av_map):
        self._inject_av_map = inject_av_map 
        return

    # perform actual update, additions, ... return updated
    def perform_revision (self):
        updated_products = None

        # initialize - once, if any
        if self._initialize (self._english_source_records, self._german_source_records, self._inject_av_map ) == False:
            logging.error ('Revision module initialization failed')
            return (None, None)

        # perform updates to existing records, if any
        updated_products = self.__perform_updates ()
        if (updated_products == None) or (len (updated_products) == 0):
            logging.error ('Revision module perform_update failed')
            return (None, None)

        updated_products = self._perform_additions (updated_products)
        if (updated_products == None) or (len (updated_products) == 0):
            logging.error ('Revision module perform_additions failed')
            return (None, None)

        # finalize - once, if any
        if self._finalize (updated_products) == False:
            logging.error ('Revision module finalization failed')
            return (None, None)

        # collect all attributes in entire catalog
        updated_attributes = self.__collect_attributes (updated_products)

        return updated_products, updated_attributes

    # let derived class do any one-time initialization
    def _initialize (self, english_source_records, german_source_records, inject_av_map ):
        return True

    # perform additions and return total catalog
    # derived class MAY override this method to return total products including additionals
    def _perform_additions (self, current_products):
        return current_products # default

    # let derived class do any one-time finalization
    def _finalize (self, updated_products):
        return True

    ### For derived class
    def _perform_record_update (self, record):
        raise Exception ('Record update method must be implemented in derived classs')
        return

    # methods called within Base class
    def __perform_updates (self):
        updated_records = []
        for german_record in self._german_source_records:
            try:
                updated_record = self._perform_record_update (german_record)
                if (updated_record != None):
                    updated_records.append (updated_record)
            except Exception as e:
                logging.warning ('Record update failed for pid: %s, error = %s' % (german_record ['value']['attributes']['pid'], e))
                continue
        return updated_records

    # collect all attributes, across entire catalog
    def __collect_attributes (self, updated_products):
        updated_attributes = []
        for record in updated_products:
            product_attribs = record ['value']['attributes']
            for attrib in product_attribs.keys():
                if attrib not in updated_attributes:
                    updated_attributes.append (attrib)

            if ('variants' in record ['value']) and (record ['value']['variants']):
                variant_list = record ['value']['variants']
                for variant_id, variant_obj in variant_list.items():
                    variant_attribs = variant_obj ['attributes'].keys ()
                    for attrib in variant_attribs:
                        if attrib not in updated_attributes:
                            updated_attributes.append (attrib)
        return updated_attributes

    # methods called by derived class
    def _lookup_inject_av_record (self, pid):
        if self._inject_av_map == None:
            return None

        for inject_record in  self._inject_av_map:
            if inject_record ['pid'] == pid:
                return inject_record

        return None

    # given a pid, return corresponding english source record. May return None
    def _lookup_english_source_record (self, pid):
        for english_record in self._english_source_records:
            if english_record ['value']['attributes']['pid'] == str (pid):
                return english_record
        return None


