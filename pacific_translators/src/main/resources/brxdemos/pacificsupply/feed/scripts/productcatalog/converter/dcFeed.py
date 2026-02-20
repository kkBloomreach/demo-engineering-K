import logging

import attributeMap as am

class DCFeed ():

    def __init__ (self):
        return

    def setBRSMFeed (self, brsmFeed):
        self._brsmFeedHandler = brsmFeed

    # generate DC-style object comprising info for all brsm products
    # returns list of DC-style-product-objects
    def generateDCFeed (self):
        logging.info ("Generate dataConnect feed")
        self._dcProducts = []
        self._attributeList = []

        _brsmProductIterator = self._brsmFeedHandler.getProductIterator ();
        for _aBrsmProduct in _brsmProductIterator: 
            # returns tuple ('views', master-view)
            _dcProductViews, _masterView = self._parseViewAttributes (_aBrsmProduct)

            _transProductRecord = self._transformProduct (_aBrsmProduct, _masterView)

            # construct DC-style object
            _aDCProduct = {}
            _aDCProduct ["op"] = "add"
            _aDCProduct ["path"] = "/products/"+str(_transProductRecord ['pid'])

            # product attributes
            _aDCProduct ["value"] = {}
            _aDCProduct ["value"]["attributes"] = _transProductRecord

            # product views
            _aDCProduct ['value']['views'] = _dcProductViews

            # add this to class level dcFeed
            self._dcProducts.append (_aDCProduct) 

        # sort attributeList
        self._attributeList.append ('category_paths')
        self._attributeList.sort ();

        logging.info ("finish generating dataConnect feed")
        return (self._dcProducts, self._attributeList)

    # using view_id and view_specific_attribs in source record
    # generate list of view_id, {view-specific-attrib-value}*
    # The src is expected to have 1-to-1 mapping between view_id_list
    # and the view_specific_attrib_values
    def _parseViewAttributes (self, _aBrsmProduct):
        _src_view_id_list = _aBrsmProduct [am.VIEWID_ATTRIBUTE_NAME] # id1|id2|...
        _view_ids = _src_view_id_list.split (am.VIEWID_DELIMITER)
        _view_object_list = [] # list of {id=,attr=,...} objects, one for each view 
        # remember 'master' view. The attrib-values in master view are set as product's 'base' values
        _view_master = None 

        for _aViewId in _view_ids:
            _viewObject = {}
            _viewObject ['view_id'] = _aViewId
            _view_object_list.append (_viewObject)

            # remember 'master' view
            if (_aViewId == am.VIEWID_NAME_MASTER):
                _view_master = _viewObject

        # go thru all view_specific_attrib_names and for each,
        # set the attrib_values to associated view. 
        # Note : view_id_list is expected to have 1:1 mapping with view_specific_attrib_names
        for _attribName in am.VIEW_SPECIFIC_ATTRIBUTE_NAMES:
            _src_attribValues = _aBrsmProduct [_attribName] # val1|val2|...
            _attribValues = _src_attribValues.split (am.VIEW_VALUE_DELIMITER)
            for _viewIndx, _oneAttribVal in enumerate (_attribValues):
                # Float (eg number) values
                if (_attribName in am.FLOAT_ATTRIBUTES):
                    _view_object_list [_viewIndx][_attribName] = float (_oneAttribVal)
                elif (_attribName in am.BOOLEAN_ATTRIBUTES):
                    if (_oneAttribVal == 'true'):
                        _view_object_list [_viewIndx][_attribName] = True
                    else:
                        _view_object_list [_viewIndx][_attribName] = False
                else:
                    _view_object_list [_viewIndx][_attribName] = _oneAttribVal

        # go thru all view_objects for this product to build an object in dataConnect format
        # Note - if a view_specific_value is same as the one in 'master', we don't
        # need to re-include it as view_specific_value
        _dc_product_views = {}
        for _viewObject in _view_object_list:
            _view_id = _viewObject ['view_id']  # eg, 'id1'
            if (_view_id == am.VIEWID_NAME_MASTER):
                # add an 'empty' master view. All attribs in the master view
                # are added as 'base' attribs therefore don't need to repeat
                # again
                _dc_product_views [am.VIEWID_NAME_MASTER] = {}
                _dc_product_views [am.VIEWID_NAME_MASTER]['attributes'] = {}
            else:
                _dc_product_views [_view_id] = {}
                _dc_product_views [_view_id]['attributes'] = {}
                for key in _viewObject:
                    if (key == 'view_id'):
                        continue
                    # include this view_specific attribValue if it is different from 'master' attribValue
                    if (_view_master [key] != _viewObject [key]):
                        _dc_product_views [_view_id]['attributes'][key] = _viewObject [key]

        # return dc formated view object and master-view. Latter is used
        # to set base values in the product record
        return (_dc_product_views, _view_master)

    # transfer all attributes from original brsmProduct attributes
    # Currently this feed has all the 'required' attributes names as expected (eg, 'price')
    def _transformProduct (self, brsmProduct, productMasterView):
        transRecord = {}

        # keys from source record
        for _key in brsmProduct.keys ():
            # skip 'view_id'
            if (_key == am.VIEWID_ATTRIBUTE_NAME):
                continue
            # skip view_specific_attributes; those are added below
            if (_key in productMasterView):
                continue
            # skip crumbs, crumbsId - they are transformed to categoryPaths
            if (_key == 'crumbs') or (_key == 'crumbs_id'):
                continue

            # add key-value to transformedRecord
            transRecord [_key] = brsmProduct [_key]

            # for partialValue (aka partnumber-search)
            # by convention, the new attribute is named 'xxx_partialvalue'
            # eg, pid_partialvalue
            if (_key in am.PARTIAL_VALUE_ATTRIBUTE_LIST):
                if (len (brsmProduct [_key]) > 0):
                    # following 'catalog_number_partialvalue' name for backward compatibility
                    if (_key == 'Catalog Number'):
                        _partialValueAttribName = 'catalog_number' + '_partialvalue'
                    else:
                        _partialValueAttribName = _key + '_partialvalue'
                    _splitValues = self._splitPartialValueAttrib (brsmProduct [_key], am.MIN_PARTIALVALUE_LENGTH, am.MAX_PARTIALVALUE_LENGTH)
                    transRecord [_partialValueAttribName] = _splitValues

        # keys from master view for view_specific_attributes
        for _key in productMasterView:
            if (_key == 'view_id'):
                continue
            transRecord [_key] = productMasterView [_key]

        totalCrumbs = brsmProduct ['crumbs']
        totalCrumbsId = brsmProduct ['crumbs_id']
        categoryPaths = self._constructCategoryPath (totalCrumbs, totalCrumbsId, brsmProduct ['pid'])
        transRecord["category_paths"] = categoryPaths
   
        # collect ALL attrib names, across ALL products 
        for _key in transRecord:
            if ((_key in self._attributeList) == False):
                self._attributeList.append (_key)
 
        return transRecord
 
    # totalCrumbs, totalCrumbsId -> DC style categoryPath 
    # In this catalog, a product belongs to only one category branch. The same product
    # does not occur in multiple separate branches. Therefore there is only
    # 'value' delimiter. There is no parent-value-delimiter
    # value delimiter: '|'
    # 'pid' included to report error
    def _constructCategoryPath (self, totalCrumbs, totalCrumbsId, pid):
        categoryPaths = []
        if ((totalCrumbs != None) and (totalCrumbsId != None)):
            if (totalCrumbs.find ('|') > 0):
                # this category branch (crumb) has member elements (eg, A|B|C)
                crumbsList = totalCrumbs.split ('|')
                crumbsIdList = totalCrumbsId.split ('|')
                if (len (crumbsList) == len (crumbsIdList)):
                    aCategoryPath = []
                    for i in range (0, len (crumbsList)):
                        aCrumbElement = crumbsList [i]
                        aCrumbElementId = crumbsIdList [i]
                        aCategoryPath.append ({'id': aCrumbElementId,
                                               'name': aCrumbElement } )
                    categoryPaths.append (aCategoryPath)
                else:
                    logging.error ('crumbs, crumbsId mismatch. Pid = %s', pid)
            else:
                # this category branch (crumb) has no member elements (eg, A)
                aCategoryPath = []
                aCrumbElement = totalCrumbs
                aCrumbElementId = totalCrumbsId
                aCategoryPath.append ({'id': aCrumbElementId,
                                        'name': aCrumbElement } )
                categoryPaths.append (aCategoryPath)
        return categoryPaths 

    # given a value such as 'abc123456', return ['abc', 'abc1', 'abc12', ..., 'abc123456'] 
    def _splitPartialValueAttrib (self, value, minLen, maxLen):
        _splitValues = []
        if (len (value) <= minLen):
            _splitValues.append (value)
        else:
            maxToSplit = min (len (value), maxLen)
            for _i in range (minLen, maxToSplit):
                _partial = value [0:_i]
                _splitValues.append (_partial)

            #finally append the full actual value
            #if len(value) > max, the entire full value is appended
            _splitValues.append (value)

        return _splitValues
 
if __name__ == '__main__':
    _dcFeed = DCFeed ()
    _dcFeed.setBRSMFeed (None)
    _dcFeed.generateDCFeed ()
 
