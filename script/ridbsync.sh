#!/bin/sh
#Script for replicating mysql data for Resource_Index
#Syncronising individual resouce tables
# 1 rsync myisam binary database files one resouce at a time 
# 2 drop and recreate indexes
# 3 lock tables, move binary myisam files and then unlock tables
# 4 move all non resouce related tables

#List of Resouces that need to be repliated
RESOURCELIST="ae bsm cdd ct dbk gap gm micad omim pc pgdi pgdr pgge reac rxrd smd upkb wp"


#Database parameters:
#temporary DB
TMPDB=resouce_index_temp
DB=resource_index
DBUSERNAME=username
DBPASSWD=passwd
MASTERDBUSER=username
MASTERDBPASSWD=passwd

#rsync parameters 
SERVERFROM=ncboprod-ridb1.sunet
DIRFROM=/var/lib/mysql/mysql_data/resource_index
DIRTO=/var/lib/mysql/mysql_data/$TMPDB
RSYNCOPTS=" -vva bprsync@"
#RSYNCOPTS="-e \"ssh -i /root/rsync/mirror-rsync-key\" -vva bprsync@"

#usefull mysql commands
DBLOCK="FLUSH TABLES WITH READ LOCK;"
DBUNLOCK="UNLOCK TABLES;"

lockmasterdb(){
mysql -u$MASTERDBUSER -p$MASTERDBPASSWD -h$SERVERFROM $DB -e "$DBLOCK"
}

unlockmasterdb(){
mysql -u$MASTERDBUSER -p$MASTERDBPASSWD -h$SERVERFROM $DB -e "$DBLOCK"
}

lockslavedb(){
mysql -u$DBUSERNAME -p$DBPASSWD $DB -e "$DBLOCK"
}

unlockslavedb(){
mysql -u$DBUSERNAME -p$DBPASSWD $DB -e "$DBLOCK"
}

execmysql(){
echo $1
mysql -u$DBUSERNAME -p$DBPASSWD $TMPDB <<EOFMYSQL
$1
EOFMYSQL
}


#Syncronising individual resouce tables

sync_resource_tables() {
echo "syncing resouces $RESOURCELIST"
for RESOURCE in $RESOURCELIST 
do
	echo "Starting to work on Resouce $RESOURCE"	
	lockmasterdb
	echo "rsync $RSYNCOPTS$SERVERFROM:$DIRFROM/obr_${RESOURCE}_* $DIRTO"
	time rsync -e "ssh -i /root/rsync/mirror-rsync-key" $RSYNCOPTS${SERVERFROM}:$DIRFROM/obr_${RESOURCE}_* $DIRTO
	unlockmasterdb
	
	MYSQLQ1="ALTER TABLE $TMPDB.obr_${RESOURCE}_annotation DROP INDEX IDX_obr_${RESOURCE}_annotation_workflow_status, 
			ADD INDEX X_obr_${RESOURCE}_annotation_element_id USING BTREE(element_id), 
			ADD INDEX X_obr_${RESOURCE}_annotation_concept_id USING BTREE(concept_id),
			ADD INDEX X_obr_${RESOURCE}_annotation_context_id USING BTREE(context_id),
			ADD INDEX X_obr_${RESOURCE}annotation_term_id USING BTREE(term_id),  
			ADD INDEX X_obr_${RESOURCE}annotation_dictionary_id USING BTREE(dictionary_id);"

	MYSQLQ2="ALTER TABLE $TMPDB.obr_${RESOURCE}_isa_annotation DROP INDEX IDX_obr_${RESOURCE}_isa_annotation_workflow_status, 
			ADD INDEX X_obr_${RESOURCE}_isa_annotation_element_id USING BTREE(element_id), 
			ADD INDEX X_obr_${RESOURCE}_isa_annotation_concept_id USING BTREE(concept_id), 
			ADD INDEX X_obr_${RESOURCE}_isa_annotation_context_id USING BTREE(context_id), 
			ADD INDEX X_obr_${RESOURCE}_isa_annotation_child_concept_id USING BTREE(child_concept_id), 
			ADD INDEX X_obr_${RESOURCE}_isa_annotation_parent_level USING BTREE(parent_level);"

	MYSQLQ3="ALTER TABLE $TMPDB.obr_${RESOURCE}_map_annotation DROP INDEX IDX_obr_${RESOURCE}_map_annotation_workflow_status, 
			ADD INDEX X_obr_${RESOURCE}_map_annotation_element_id USING BTREE(element_id), 
			ADD INDEX X_obr_${RESOURCE}_map_annotation_concept_id USING BTREE(concept_id), 
			ADD INDEX X_obr_${RESOURCE}_map_annotation_context_id USING BTREE(context_id), 
			ADD INDEX X_obr_${RESOURCE}_map_annotation_mapped_concept_id USING BTREE(mapped_concept_id), 
			ADD INDEX X_obr_${RESOURCE}_map_annotation_mapping_type USING BTREE(mapping_type);"

	MYSQLQ4="ALTER TABLE $TMPDB.obr_${RESOURCE}_aggregation DROP INDEX element_id,  
			ADD INDEX X_obr_${RESOURCE}_aggregation_element_id USING BTREE(element_id, concept_id), 
			ADD INDEX X_obr_${RESOURCE}_aggregation_concept_id USING BTREE(concept_id);"

#	MYSQLQ5="lock tables resource_index.obr_${RESOURCE}_aggregation READ, resource_index.obr_${RESOURCE}_annotation READ, 
#			resource_index.obr_${RESOURCE}_element READ, resource_index.obr_${RESOURCE}_isa_annotation READ, resource_index.obr_${RESOURCE}_map_annotation READ;"

	time execmysql "$MYSQLQ1"
	time execmysql "$MYSQLQ2"
	time execmysql "$MYSQLQ3"
	time execmysql "$MYSQLQ4"
	#Lock tables before moving
	lockslavedb
	#Move tables
	/bin/mv $DIRTO/obr_${RESOURCE}_* $DIRFROM/
	#Unlock tables
	unlockslavedb
	echo "Done working with $RESOUCE"
done
echo "done syncing all resouces"
}

#sync all non resouce specific tables
sync_tables(){

#rsync all tables exept for memory tables and non specific resouces tables:
RSYNCEXCLUDE="--exclude=*_annotation.* --exclude=*isa_annotation.* --exclude=*map_annotation.* --exclude=*_aggregation.* --exclude=*_element.* --exclude=*_mem.frm"
echo "syncing all non specific resouces related tables"

lockmasterdb
rsync -e "ssh -i /root/rsync/mirror-rsync-key" $RSYNCEXCLUDE $RSYNCOPTS${SERVERFROM}:$DIRFROM/* $DIRTO
unlockmasterdb
lockslavedb
/bin/mv $DIRTO/*.MYD $DIRTO/*.MYI $DIRTO/*.frm $DIRFROM/
unlockslavedb
}

time sync_tables
time sync_resource_tables