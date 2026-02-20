# script to upload pixel part* files to s3
import os
import glob

# script inside run/scripts directory
S3_UPLOAD_CMD_PREAMBLE = "s3cmd --acl-public put "
S3_FILE_PREAMBLE = "s3://br-user/kiran/pacificsupply/clone/pixel/v6/"

def uploadSeqFilesToS3 ():
    # using the clone'd log file, generate corresponding seq file
    # for i in range (0,1): -- testing
    for i in range (0,31):
        seqoutDir = "../data/output/" + str (i)
        print ("DEBUG seqDir: " + seqoutDir)
       
        # part-0* for translated(cloned) pixels. 
        # part-1* for simulated pixels
        # part-*  for both translated(cloned) and simulated pixels
        seqFilesList = glob.glob (seqoutDir + '/output' + '/part-0*')
        for aSeqFileLoc in seqFilesList:
            # fileName = 'part-xxxxx'
            indx = aSeqFileLoc.rindex ('/')
            seqFileName = aSeqFileLoc [indx+1:]

            # s3FilePath = S3 file preamble + d{i} + filename
            s3FilePath = S3_FILE_PREAMBLE + str (i) + "/" + seqFileName
            s3UploadCmd = S3_UPLOAD_CMD_PREAMBLE + ' ' + aSeqFileLoc + ' ' + s3FilePath 
            print ("DEBUG s3Upload cmd: " + s3UploadCmd)

            exec_value = os.system (s3UploadCmd)
            print ("DEBUG s3UploadCmd exec value: " + str (exec_value))

def main ():
    # upload seq files from local d{i}/seqout/part-* to S3
    uploadSeqFilesToS3 ()

if __name__ == "__main__":
    main ()

