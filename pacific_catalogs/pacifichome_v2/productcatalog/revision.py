# Revision of prevision catalog version
import logging
import copy
import os
import csv
import random

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstants as rcv

class Revision (RevisionBase) :
    def __init__ (self):
        logging.info ('Perform update')
        super().__init__ ()
        return

    def _initialize (self, source_records, inject_av_map):
        return True

    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        inject_av_record = super()._lookup_inject_av_record (pid)
        #if (inject_av_record == None):
        #    logging.debug ('No inject attrib_value record for pid: %s', pid)

        updated_record = self._perform_update_internal (record, inject_av_record)
        return updated_record

    def _finalize (self, updated_products):
        return updated_products

    # INTERNAL METHODS
    def _perform_update_internal (self, record, inject_av_record):
        pid = record ['value']['attributes']['pid']

        updated_record = copy.deepcopy (record)
        category_paths = updated_record ['value']['attributes']['category_paths']
        all_branch_paths = []
        for branch in category_paths:
            # build 'path' strings for each branch
            path = '' 
            for leaf in branch:
                if path == '':
                    path = '%s:%s' % (leaf ['id'], leaf ['name']) # id:name
                else:
                    path = '%s>%s:%s' % (path, leaf ['id'], leaf ['name'])    # '>' delimiter
            all_branch_paths.append (path)

        # next, sort the all_branch_path list
        all_branch_paths.sort ()

        # go thru all_branch_paths list and remove duplicates
        # eg, [A>B, A>B>C, ...], then remove 'A>B' since pid is also in 'A>B>C'
        for i in range (0, len (all_branch_paths)-1):
            path = all_branch_paths [i]
            for j in range (i+1, len (all_branch_paths)):
                nxt_path = all_branch_paths [j]
                if nxt_path.find (path) >= 0:
                    # 'path' is in 'nxt_path' -- meaning 'path' is redundant
                    all_branch_paths [i] = None
                    break

        # go thru curated list of paths and reconstruct category_paths object
        new_category_paths = []
        for i in range (0, len (all_branch_paths)):
            path = all_branch_paths  [i]
            if path == None:
                continue
            # un-parse path (A>B>C) and build branch-object in category_paths
            branch_leaves = path.split ('>')
            branch = []
            for leaf_num in range (0, len (branch_leaves)):
                branch_leaf_info = branch_leaves [leaf_num].split (':')
                branch_leaf = { 'id': branch_leaf_info [0],
                                'name': branch_leaf_info [1]
                              }
                branch.append (branch_leaf)
            # append branch to category_paths
            new_category_paths.append (branch)

        updated_record ['value']['attributes']['category_paths'] = new_category_paths
        return updated_record

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = Revision ()
    logging.info ('Revision finish...')


