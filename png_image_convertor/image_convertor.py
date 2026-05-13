import logging
import os
import csv
from PIL import Image

import image_convertor_constants as icc

class Convertor:

    def __init__ (self):
        return

    def convert_and_upload_images (self, s3_src_bucket : str, s3_target_bucket: str,
                            local_download_work_dir: str, local_converted_work_dir : str, count : int = -1 ) -> int:
        filenames = self._collect_s3_image_paths (local_download_work_dir, icc.FILENAME_IMAGE_LIST_TSV_IN)
        uploaded_filenames = []

        if count < 0:
            max_count = len (filenames) # convert all images
        else:
            max_count = count

        converted_count = 0
        for row in filenames:
            name: str
            if converted_count >= max_count:
                break

            name = row ['filename']
            local_download_image_path = self._download_image (s3_src_bucket, name, local_download_work_dir)
            if (local_download_image_path != None):
                logging.debug ('image file downloaded: %s' % name)
                # file can be .jpg or .png

                dot_indx = local_download_image_path.rindex ('.')
                file_ext = local_download_image_path [dot_indx+1:]
                slash_indx = local_download_image_path.rindex ('/')
                base_name = local_download_image_path [slash_indx+1:dot_indx]
                if (file_ext == 'jpg'):
                    local_converted_image_path = self._convert_jpg_to_webp (local_download_image_path, base_name, local_converted_work_dir)
                elif (file_ext == 'png'):
                    local_converted_image_path = self._convert_png_to_webp (local_download_image_path, base_name, local_converted_work_dir)
                else:
                    logging.error ('Unknown image file extension: %s' % file_ext)
                    local_converted_image_path = None

                if (local_converted_image_path != None):
                    # upload converted image
                    op_stat = self._upload_converted_image (local_converted_image_path, s3_target_bucket)
                    if (op_stat == True):
                        # delete local downloaded and converted image file
                        try:
                            os.remove (local_download_image_path)
                        except Exception as e:
                            logging.warning ('Exception in removing local download file %s' % local_download_image_path)

                        try:
                            os.remove (local_converted_image_path)
                        except Exception as e:
                            logging.warning ('Exception in removing local converted file %s' % local_converted_image_path)

                        uploaded_filenames.append ({'filename': name})    # collect which files have been successfully converted
                        converted_count = converted_count + 1

        # save converted-file-names to tsv
        if (len (uploaded_filenames) > 0):
            uploaded_image_list_file_path = '%s/%s' % (local_converted_work_dir, icc.FILENAME_UPLOADED_IMAGE_LIST_TSV_OUT)
            with open (uploaded_image_list_file_path, 'w') as uploaded_image_list_file:
                list_writer = csv.writer (uploaded_image_list_file, delimiter='\t')
                header_line = uploaded_filenames[0].keys()
                list_writer.writerow  (header_line)
                for row in uploaded_filenames:
                    list_writer.writerow (row.values())
                uploaded_image_list_file.flush ()
                uploaded_image_list_file.close ()

        return converted_count

    # image filenames pre-loaded in a local .tsv file
    def _collect_s3_image_paths (self, local_download_work_dir: str, image_list_tsv : str) -> list:
        filenames: list = []
        image_list_file_path = '%s/%s' % (local_download_work_dir, image_list_tsv)
        with open (image_list_file_path, 'r') as image_list_file:
            list_reader = csv.DictReader (image_list_file, delimiter='\t')
            for row in list_reader:
                filenames.append (row)
        return filenames

    def _download_image (self, s3_src_bucket: str, image_filename: str, local_work_dir: str) -> str:
        exec_stat = False
        local_download_image_path = None

        local_download_image_path = '%s/%s' % (local_work_dir, image_filename)
        s3_download_cmd = '%s %s%s %s' % (icc.S3_DOWNLOAD_CMD_PREAMBLE, s3_src_bucket, image_filename, local_download_image_path)
        logging.debug ('s3 download cmd: %s' % s3_download_cmd)
        try:
            cmd_stat = os.system (s3_download_cmd)
            if (cmd_stat == 0):
                exec_stat = True
        except Exception as e:
            logging.error ('Image download exception: %s' % str (e))

        if (exec_stat == True):
            return local_download_image_path
        return None

    def _convert_jpg_to_webp (self, local_download_image_path: str, base_name: str, local_converted_work_dir: str) -> str:
        local_converted_image_path = '%s/%s.webp' % (local_converted_work_dir, base_name)
        try:
            image = Image.open (local_download_image_path).convert ("RGB")
            image.save (local_converted_image_path, "webp", quality = icc.JPG_CONVERSION_QUALITY_FACTOR)
        except Exception as e:
            logging.warning ('Exception in converting jpg to webp: %s' % base_name)
            local_converted_image_path = None
        return local_converted_image_path

    def _convert_png_to_webp (self, local_download_image_path: str, base_name: str, local_converted_work_dir: str) -> str:
        local_converted_image_path = '%s/%s.webp' % (local_converted_work_dir, base_name)
        try:
            image = Image.open (local_download_image_path)
            image.save (local_converted_image_path, "webp")
        except Exception as e:
            logging.warning ('Exception in converting png to webp: %s' % base_name)
            local_converted_image_path = None
        return local_converted_image_path

    def _upload_converted_image (self, local_converted_image_path: str, s3_target_bucket: str) -> bool:
        exec_stat = False
        slash_indx = local_converted_image_path.rindex ('/')
        base_name = local_converted_image_path [slash_indx+1:]
        s3_upload_image_path = '%s%s' % (s3_target_bucket, base_name) 
        s3_upload_cmd = '%s%s %s' % (icc.S3_UPLOAD_CMD_PREAMBLE, local_converted_image_path, s3_upload_image_path)
        logging.debug ('s3 upload cmd: %s' % s3_upload_cmd)
        try:
            cmd_stat = os.system (s3_upload_cmd)
            if (cmd_stat == 0):
                exec_stat = True
        except Exception as e:
            logging.error ('Image download exception: %s' % str (e))
        return exec_stat 

if __name__ == '__main__':
    logging.basicConfig (level = logging.DEBUG)
    c = Convertor ()
    count = c.convert_and_upload_images (icc.S3_SRC_IMAGE_BUCKET, icc.S3_TARGET_IMAGE_BUCKET,
                                         icc.LOCAL_DOWNLOAD_WORK_DIR, icc.LOCAL_CONVERTED_WORK_DIR, -1)
    if (count > 0):
        logging.info ('Converted %s images.' % count)
    else:
        logging.info ('Failed to convert images')
    logging.info ('Finish...')

