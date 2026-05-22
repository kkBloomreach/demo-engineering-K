# current changes
# -- set thumb and swatch images same as ph2
# -- for simplicity, copy the ph2 image from s3 to phsc's s3 bucket (instead of refering to the same image)
# -- Algo: create a bash script
#       -- for each pid, copy ph2 file to local-dir
#       -- If copy successful, upload to homesc bucket
# -- also, change thumb,swatch-image urls accordingly

import logging
import os
import copy
import json
import csv

from revisionBase import RevisionBase
import updaterConstants as uc
import revisionConstants as rc

class Revision (RevisionBase) :

    def __init__ (self):
        logging.info ('Perform update')
        super().__init__ ()
        self._aws_image_download_upload_records = []
        return

    def _initialize (self, source_records, inject_av_map):
        return True
 
    # override base class method
    def _perform_record_update (self, record):
        pid = record ['value']['attributes']['pid']
        if pid.startswith ('C'):
            pid = pid [1:]
        inject_av_record = super()._lookup_inject_av_record (pid)
        #if (inject_av_record == None):
        #    logging.debug ('No inject attrib_value record for pid: %s', pid)

        updated_record = self._perform_update_internal (record, pid, inject_av_record)
        return updated_record

    def _finalize (self, updated_products):
        if (len (self._aws_image_download_upload_records) > 0):
            self._prepare_download_upload_script ()
        else:
            logging.warning ('No images to copy/upload to AWS')
        return updated_products
 
    # INTERNAL METHODS
    def _perform_update_internal (self, record, pid, inject_av_record):
        # product to be processed
        logging.debug ('@@@ Processing pid %s' % pid)

        ph2_s3_path = '%s%s%s' % (rc.PH2_IMAGE_S3_BUCKET_PREAMBLE, pid, '_0_image.webp')
        phsc_s3_path = '%s%s%s' % (rc.PHSC_IMAGE_S3_BUCKET_PREAMBLE, pid, '_0_image.webp')
        local_download_path = '%s/%s%s' % (rc.THUMB_IMAGE_LOCAL_DIR, pid, '_0_image.webp')
        img_download_upload_record = { 'pid': pid,
                                       'ph2_s3_path': ph2_s3_path,
                                       'phsc_s3_path': phsc_s3_path,
                                       'local_image_path': local_download_path
                                 }
        self._aws_image_download_upload_records.append (img_download_upload_record)

        # adjust image urls
        updated_record = copy.deepcopy (record)
        self._adjust_image_urls (updated_record, pid)

        return updated_record

    # image url = .../images/webp/pid_0_image.webp
    def _adjust_image_urls (self, updated_record, pid):
        img_url = '%s%s%s' % (rc.THUMB_IMAGE_URL_PROLOG, pid, '_0_image.webp')
        if 'thumb_image' in updated_record ['value']['attributes']:
            updated_record ['value']['attributes']['thumb_image'] = img_url
        if 'large_image' in updated_record ['value']['attributes']:
            updated_record ['value']['attributes']['large_image'] = img_url

        if ('variants' in updated_record ['value']) and (updated_record ['value']['variants']):
            variant_list = updated_record ['value']['variants']
            for variant_id, variant_obj in variant_list.items():
                if 'swatch_image' in variant_obj ['attributes']:
                    variant_obj ['attributes']['swatch_image'] = img_url
        return

    # return total count of new images uploaded
    def _prepare_download_upload_script (self):
        upload_count = 0
        download_upload_script_file = open (rc.FILENAME_AWS_DOWNLOAD_UPLOAD_SCRIPT_OUT, 'w')

        # initial commands
        download_upload_script_file.write ('\nset -e\n')
        download_upload_script_file.write ('\ndate\n\n') # start-date

        # individual image download from ph2/s3 to local-dir commands
        for img_download_upload_record in self._aws_image_download_upload_records:
            # download image from ph2-s3
            aws_s3_download_command = self._construct_s3_download_command (img_download_upload_record)
            if aws_s3_download_command == None:
                continue    # warning already issued

            # rm from phsc/s3
            aws_s3_rm_command = self._construct_s3_rm_command (img_download_upload_record)
            if aws_s3_rm_command == None:
                continue    # warning already issued

            # upload image to phsc-s3
            aws_s3_upload_command = self._construct_s3_upload_command (img_download_upload_record)
            if aws_s3_download_command == None:
                continue    # warning already issued

            download_upload_script_file.write ('%s\n\n' % aws_s3_download_command)

            # bash template to check if downloaded file exists. Template contains a %s
            bash_if_condition = rc.BASH_TEMPLATE_IF_FILE_EXISTS % img_download_upload_record ['local_image_path']
            bash_cmd = '%s\n\t%s\n\t%s\n%s\n\t%s\n%s\n' % (bash_if_condition, 
                                                         aws_s3_rm_command, 
                                                         aws_s3_upload_command, 
                                                         'else',
                                                         'echo *** No such image in Ph2 catalog ***',
                                                         'fi') 
            download_upload_script_file.write ('%s\n\n' % bash_cmd)
            upload_count = upload_count + 1

        download_upload_script_file.write ('\ndate\n\n') # end date
        download_upload_script_file.flush ()
        download_upload_script_file.close ()
        logging.debug ('Total new images to upload to AWS: %s', upload_count)
        return upload_count

    # return string for s3 upload command
    # eg, aws --profile bloomreach-demo_main s3 cp s3://pacific-demo-data.bloomreach.cloud/home/images/gen/webp/<pid>_0_image.webp <local_path>
    def _construct_s3_download_command (self, img_download_upload_record):
        # using pid, first lookup associated src_image_id
        pid = img_download_upload_record ['pid']
        local_image_path = img_download_upload_record ['local_image_path']
        ph2_s3_path = img_download_upload_record ['ph2_s3_path']

        aws_download_command = '%s %s %s' % (rc.AWS_CP_COMMAND_PREAMBLE, ph2_s3_path, local_image_path)
        logging.debug ('AWS s3 download command: %s', aws_download_command)
        return aws_download_command

    def _construct_s3_rm_command (self, img_download_upload_record):
        # using pid, first lookup associated src_image_id
        pid = img_download_upload_record ['pid']
        phsc_s3_path = img_download_upload_record ['phsc_s3_path']

        aws_rm_command = '%s %s' % (rc.AWS_RM_COMMAND_PREAMBLE, phsc_s3_path)
        logging.debug ('AWS s3 rm command: %s', aws_rm_command)
        return aws_rm_command

    def _construct_s3_upload_command (self, img_download_upload_record):
        # using pid, first lookup associated src_image_id
        pid = img_download_upload_record ['pid']
        local_image_path = img_download_upload_record ['local_image_path']
        phsc_s3_path = img_download_upload_record ['phsc_s3_path']

        aws_upload_command = '%s %s %s' % (rc.AWS_CP_COMMAND_PREAMBLE, local_image_path, phsc_s3_path)
        logging.debug ('AWS s3 upload command: %s', aws_upload_command)
        return aws_upload_command

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    rv = Revision ()
    logging.info ('Revision Finish...')

'''
        s3_file_name = '%s_0_image.%s' % (pid, extension)  # .../pid_0_image.<extension>
        s3_path = '%s/%s' % (rc.PH2_IMAGE_S3_BUCKET_PREAMBLE, s3_file_name) 
        s3_file_name = '%s_0_image.%s' % (pid, extension)  # .../pid_0_image.<extension>
        s3_path = '%s/%s' % (rc.PHSC_IMAGE_S3_BUCKET_PREAMBLE, s3_file_name) 
        s3_file_name = '%s_image.%s' % (pid, extension)  # .../pid_image.<extension>
        s3_path = '%s/%s' % (rc.PHSC_IMAGE_S3_BUCKET_PREAMBLE, s3_file_name)
'''
