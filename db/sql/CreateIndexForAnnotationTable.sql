DELIMITER $$

DROP PROCEDURE IF EXISTS `resource_index`.`CreateIndexForAnnotationTable`$$

CREATE DEFINER=`optra`@`%` PROCEDURE `CreateIndexForAnnotationTable`(IN AnnotationTable VARCHAR(100), IN ExpandedAnnotationTable VARCHAR(100) )
BEGIN
	SET @ConceptIdIndexQuery = Concat( 'CREATE INDEX IDX_',AnnotationTable, '_concept_id ON ',AnnotationTable, '(concept_id); ');
	SET @ElementIdIndexQuery = Concat( 'CREATE INDEX IDX_',AnnotationTable, '_element_id ON ',AnnotationTable, '(element_id); ');
        SET @DictionaryIdIndexQuery = Concat( 'CREATE INDEX IDX_',AnnotationTable, '_dictionary_id ON ',AnnotationTable, '(dictionary_id); ');
	SET @StatusIndexQuery = Concat( 'CREATE INDEX IDX_',AnnotationTable, '_workflow_status ON ',AnnotationTable, '(workflow_status); ');
	SET @TermIdIndexQuery = Concat( 'CREATE INDEX IDX_',AnnotationTable, '_term_id ON ',AnnotationTable, '(term_id); ');
    
	# Prepare statements 
	PREPARE ConceptIdIndex FROM @ConceptIdIndexQuery;
	PREPARE ElementIdIndex  FROM @ElementIdIndexQuery;
	PREPARE TermIdIndex FROM @TermIdIndexQuery;
	PREPARE DictionaryIdIndex  FROM @DictionaryIdIndexQuery;
	PREPARE StatusIndex FROM @StatusIndexQuery;	
	
	# Execute statement
	EXECUTE ConceptIdIndex; 
	EXECUTE ElementIdIndex; 
	EXECUTE TermIdIndex; 
	EXECUTE DictionaryIdIndex; 
	EXECUTE StatusIndex; 
	
	SET @ConceptIdIndexQuery_E = Concat( 'CREATE INDEX IDX_',ExpandedAnnotationTable, '_concept_id ON ',ExpandedAnnotationTable, '(concept_id); ');
	SET @ElementIdIndexQuery_E = Concat( 'CREATE INDEX IDX_',ExpandedAnnotationTable, '_element_id ON ',ExpandedAnnotationTable, '(element_id); ');
        SET @ExpansionTypeIndexQuery_E = Concat( 'CREATE INDEX IDX_',ExpandedAnnotationTable, '_expansion_type ON ',ExpandedAnnotationTable, '(expansion_type); ');
	SET @StatusIndexQuery_E = Concat( 'CREATE INDEX IDX_',ExpandedAnnotationTable, '_workflow_status ON ',ExpandedAnnotationTable, '(workflow_status); ');
	 
	# Prepare statements 
	PREPARE ConceptIdIndex_E FROM @ConceptIdIndexQuery_E;
	PREPARE ElementIdIndex_E FROM @ElementIdIndexQuery_E;	
	PREPARE ExpansionTypeIndex_E FROM @ExpansionTypeIndexQuery_E;
	PREPARE StatusIndex_E FROM @StatusIndexQuery_E;	
	
	# Execute statement
	EXECUTE ConceptIdIndex_E; 
	EXECUTE ElementIdIndex_E; 	 
	EXECUTE ExpansionTypeIndex_E; 
	EXECUTE StatusIndex_E; 
    
END$$

DELIMITER ;