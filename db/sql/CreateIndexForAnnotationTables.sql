DELIMITER $$

DROP PROCEDURE IF EXISTS `resource_index`.`CreateIndexForAnnotationTables`$$

CREATE DEFINER=`optra`@`%` PROCEDURE `CreateIndexForAnnotationTables`(IN AnnotationTable VARCHAR(100), IN ExpandedAnnotationTable VARCHAR(100) )
BEGIN
	SET @AnnotationTableIndexQuery = Concat( 'ALTER TABLE ',AnnotationTable ,' ADD PRIMARY KEY (id), ADD INDEX IDX_',AnnotationTable,'_element_id(element_id), ADD INDEX IDX_',AnnotationTable,'_concept_id(concept_id), ADD INDEX IDX_',AnnotationTable,'_dictionary_id(dictionary_id), ADD INDEX IDX_',AnnotationTable,'_workflow_status(workflow_status);');
	SET @ExpandedAnnotationTableIndexQuery = Concat( 'ALTER TABLE ',ExpandedAnnotationTable ,' ADD PRIMARY KEY (id), ADD INDEX IDX_',ExpandedAnnotationTable ,'_element_id(element_id), ADD INDEX IDX_',ExpandedAnnotationTable ,'_concept_id(concept_id), ADD INDEX IDX_',ExpandedAnnotationTable ,'_expansion_type(expansion_type), ADD INDEX IDX_',ExpandedAnnotationTable ,'_workflow_status(workflow_status);');
	 
	# Prepare statements 
	PREPARE AnnotationTableIndex FROM @AnnotationTableIndexQuery;
	PREPARE ExpandedAnnotationTableIndex  FROM @ExpandedAnnotationTableIndexQuery;
	 
	# Execute statement
	EXECUTE AnnotationTableIndex; 
	EXECUTE ExpandedAnnotationTableIndex; 
	    
END$$

DELIMITER ;