#!/bin/sh
#
#Shell Script to execute resource index workflow
#

ant clean all
cd dist
chmod 777 run.sh

#./run.sh >resource_index_workflow_all.log &
#tail -f resource_index_workflow_all.log
./run.sh &
