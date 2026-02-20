# base class for actual 'revision update' task to be performed
import logging

class RevisionBase ():

    _site_api_response = None
    _inject_av_map = None

    def __init__ (self):
        return

    def set_site_api_response (self, api_response):
        self._site_api_response = api_response 
        return

    def set_inject_av_map (self, inject_av_map):
        self._inject_av_map = inject_av_map 
        return

    # perform actual update and return updated
    def perform_updates (self):
        updated_records = []
        updated_attributes = []

        # initialize - once, if any
        if self._initialize (self._site_api_response, self._inject_av_map ) == False:
            logging.error ('Revision module initialization failed')
            return (None, None)

        for cms_record in self._site_api_response ['documents']:
            try:
                updated_record = self._perform_record_update (cms_record)
                if (updated_record != None):
                    updated_records.append (updated_record)
                    self._collect_attributes (updated_record, updated_attributes)
            except Exception as e:
                logging.warning ('Record update failed for id: %s, error = %s' % (cms_record ['id'], e))
                continue

        # finalize - once, if any
        if self._finalize () == False:
            logging.error ('Revision module finalization failed')
            return (None, None)

        return updated_records, updated_attributes

    # let derived class do any one-time initialization
    def _initialize (self, api_response, inject_av_map ):
        return True

    def _perform_record_update (self, record):
        raise Exception ('Record update method must be implemented in derived class')
        return

    # let derived class do any one-time finalization
    def _finalize (self):
        return True

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

    # methods called by derived class
    def _lookup_inject_av_record (self, pid):
        if self._inject_av_map == None:
            return None

        for inject_record in  self._inject_av_map:
            if inject_record ['pid'] == pid:
                return inject_record

        return None



