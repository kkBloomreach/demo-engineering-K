#!/bin/bash

# build docker image so it will run in linux/amd64 environment
# GCP vm has debian / amd64
docker buildx build --platform linux/amd64  -t trafficgenerator-docker-amd64 .

# create a tar for this image
docker save -o ~/tmp/misc/trafficgenerator-docker-amd64.tar trafficgenerator-docker-amd64:latest

# upload the tar to GCP then load and run it
# ON GCP-VM
   # load docker image uploaded in ~/uploads
   # sudo docker load -i /home/kirankundargi/uploads/trafficgenerator-docker-amd64.tar
   # run docker image - image name must match
   # If running via CRON, make sure $USER is added to the docker group. Then don't need to use sudo
   #  sudo usermod -aG docker $USER
   # sudo docker run --mount type=bind,src=/home/kirankundargi/projects/trafficgenerator/data,dst=/trafficgenerator/data  trafficgenerator-docker-amd64
   # Default docker runs in prod env but 'UNDEFINED' account. It must be overridden using "-a" option
   # Sample script to run docker in GCP
   #--   #!/bin/bash
   #--
   #--if [ "$#" -eq 0 ]
   #--then
   #--    echo "Must specify account name"
   #--    exit 1
   #--fi
   #--
   #--# ACCOUNT
   #--account=$1
   #--echo "Generating traffic for $account..."
   #--
   #--docker run --mount type=bind,src=/home/kirankundargi/projects/trafficgenerator/data,dst=/trafficgenerator/data  --rm --name $account trafficgenerator-docker-macos2 -a $account

