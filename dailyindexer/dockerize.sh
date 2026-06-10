#!/bin/bash

# build docker image so it will run in linux/amd64 environment
# GCP vm has debian / amd64
docker buildx build --platform linux/amd64  -t reindexer-3.2-docker-amd64 .

# create a tar for this image
docker save -o ~/tmp/misc/reindexer-3.2-docker-amd64.tar reindexer-3.2-docker-amd64:latest

# upload the tar to GCP then load and run it
# ON GCP-VM
   # load docker image uploaded in ~/uploads
   # sudo docker load -i /home/kirankundargi/uploads/reindexer-docker-amd64.tar
   # run docker image - image name must match
   # sudo docker run --mount type=bind,src=/home/kirankundargi/projects/reindexer_new/uniprocess/data,dst=/reindexer/data  reindexer-docker-amd64
