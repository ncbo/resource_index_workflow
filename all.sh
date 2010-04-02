#!/bin/sh
#
#Shell Script to execute resource index workflow
#

ant clean all
cd dist
chmod 777 run.sh
./run.sh >resource_grant_workflow_10k.log &
tail -f resource_grant_workflow_10k.log
 
