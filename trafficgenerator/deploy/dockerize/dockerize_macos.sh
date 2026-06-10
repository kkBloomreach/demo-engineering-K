#!/bin/bash

# build docker image so it will run in local macos environment
docker build  -t trafficgenerator-docker-macos .

# Run docker
   # run docker image - image name must match
   # docker run --mount type=bind,src=/Users/kirankundargi/tmp/brlab/test/trafficgenerator/data,dst=/trafficgenerator/data  --rm trafficgenerator-docker-macos

