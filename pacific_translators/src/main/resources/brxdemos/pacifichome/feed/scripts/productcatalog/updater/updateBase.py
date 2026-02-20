# base class for actual 'update' task to be performed
import logging

class UpdateBase ():

    _source_records = None

    def __init__ (self):
        return

    def set_source_records (self, source_records):
        self._source_records = source_records
        return

    # perform actual update and return updated records
    def perform_updates (self):
        updated_records = []
        updated_attributes = []

        for record in self._source_records:
            try:
                updated_record = self._perform_record_update (record)
                if (updated_record != None):
                    updated_records.append (updated_record)
                    self._collect_attributes (updated_record, updated_attributes)
            except Exception as e:
                logging.warning ('Record update failed for pid: %s, error = %s' % (record ['value']['attributes']['pid'], e))
                continue

        # finish (aka complete) all updates (eg, generate aws-upload-script etc)
        self._finalize_updates ()
        return updated_records, updated_attributes

    # perform product additions, if any, and return total catalog
    # derived class MAY override this method to return total products including additionals
    def perform_additions (self, current_products, current_attributes):
        return current_products, current_attributes # default

    # derived calss MAY override this method to return subset products and corresponding attributes
    # By default, no subset is created; therefore this method returns None, None (products, attributes)
    def prepare_subset (self, current_products):
        return None, None

    # method MUST be overridden in derived class
    def _perform_record_update (self, record):
        raise Exception ('Record update method must be implemented in derived classs')
        return

    # method called within Base class - override in derived class as needed
    def _finalize_updates (self):
        return

    # method called within Base class - collect all attributes, across entire catalog
    def _collect_attributes (self, record, updated_attributes):
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
        return


