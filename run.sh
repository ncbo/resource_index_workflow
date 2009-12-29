#!/bin/sh
#
#Shell Script to execute resource index workflow
#

CLASSPATH=".:${CLASSPATH}:"
for i in `ls lib/*.jar`
do
   CLASSPATH="${CLASSPATH}:${i}"
done
 
echo "${CLASSPATH}"

java -Xms512M -Xmx1024M -cp "script:conf:${CLASSPATH}:resource_index_workflow.jar" org.ncbo.stanford.obr.populate.main.PopulateResourceIndex