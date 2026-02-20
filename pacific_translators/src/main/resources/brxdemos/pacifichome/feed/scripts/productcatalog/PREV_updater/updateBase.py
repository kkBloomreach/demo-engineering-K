# base class for actual 'update' task to be performed
import logging

class UpdateBase ():

    _source_records = None

    def __init__ (self):
        return

    def set_source_records (self, source_records):
        self._source_records = source_records
        return

    # perform actual update and return updated
    def perform_updates (self):
        updated_records = []
        updated_attributes = []

        for record in self._source_records:
            try:
                updated_record = self._perform_record_update (record, self._gen_image_map)
                if (updated_record != None):
                    updated_records.append (updated_record)
                    self._collect_attributes (record, updated_attributes)
            except Exception as e:
                logging.warning ('Record update failed for pid: %s, error = %s' % (record ['value']['attributes']['pid'], e))
                continue

        return updated_records, updated_attributes

    # perform product additions, if any, and return total catalog
    # derived class MAY override this method to return total products including additionals
    def perform_additions (self, current_products, current_attributes):
        return current_products, current_attributes # default

    def _perform_record_update (self, record, gen_image_map):
        raise Exception ('Record update method must be implemented in derived classs')
        return

    # method called within Base class - collect all attributes, across entire catalog
    def _collect_attributes (self, record, updated_attributes):
        product_attribs = record ['value']['attributes']
        for attrib in product_attribs.keys():
            if attrib not in updated_attributes:
                updated_attributes.append (attrib)
        return



