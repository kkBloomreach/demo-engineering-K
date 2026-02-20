# base class for actual 'revision update' task to be performed
import logging

class RevisionBase ():

    _inject_av_map = None
    _site_documents_api_response = None

    def __init__ (self):
        return

    def set_site_documents_api_response (self, api_response):
        self._site_documents_api_response = api_response
        return

    def set_inject_av_map (self, inject_av_map):
        self._inject_av_map = inject_av_map
        return

    # perform actual update, additions, ... return updated
    def perform_revision (self):
        updated_records = [] 
        updated_attributes =  []

        # initialize - once, if any
        if self._initialize (self._site_documents_api_response, self._inject_av_map ) == False:
            logging.error ('Revision module initialization failed')
            return (None, None)

        # 'regular' documents
        updated_records = self.__perform_updates ()
        if (updated_records == None) or (len (updated_records) == 0):
            logging.error ('Revision module perform_update failed')
            return (None, None)

        # add content records if any (eg, pdf records)
        updated_records = self._perform_additions (updated_records)
        if (updated_records == None) or (len (updated_records) == 0):
            logging.error ('Revision module perform_additions failed')
            return (None, None)

        # collect all attributes
        for record in updated_records:
            self.__collect_attributes (record, updated_attributes)

        # finalize - once, if any
        if self._finalize (updated_records) == False:
            logging.error ('Revision module finalization failed')
            return (None, None)

        return updated_records, updated_attributes

     # let derived class do any one-time initialization
    def _initialize (self, documents_api_response, inject_av_map ):
        return True

    # perform additions and return total catalog
    # derived class MAY override this method to return total records including additionals
    def _perform_additions (self, current_records):
        return current_records # default

    # let derived class do any one-time finalization
    def _finalize (self, updated_records):
        return True

    ### For derived class
    def _perform_record_update (self, record):
        raise Exception ('Record update method must be implemented in derived classs')
        return

    # methods called within Base class
    def __perform_updates (self):
        updated_records = []
        if (self._site_documents_api_response != None):
            for cms_record in self._site_documents_api_response ['documents']:
                try:
                    updated_record = self._perform_record_update (cms_record)
                    if (updated_record != None):
                        updated_records.append (updated_record)
                except Exception as e:
                    logging.warning ('Record update failed for id: %s, error = %s' % (cms_record ['id'], e))
                    continue

        return updated_records

    # collect all attributes, across entire catalog
    def __collect_attributes (self, record, updated_attributes):
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



