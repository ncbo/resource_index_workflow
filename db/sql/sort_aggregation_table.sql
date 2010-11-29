DELIMITER $$

DROP PROCEDURE IF EXISTS `sort_aggregation_table`$$

CREATE PROCEDURE `sort_aggregation_table`(IN TableToSort VARCHAR(100), IN SortedTable VARCHAR(100), IN BigTable BOOL)
BEGIN	 
	
	SET @DropTableQuery =  Concat( 'DROP TABLE IF EXISTS ',SortedTable, ';');
	SET @CreateSortedTableQuery =  Concat('CREATE TABLE ',SortedTable,' (
					  element_id int(10) unsigned NOT NULL,
					  concept_id int(10) unsigned NOT NULL,
					  score float DEFAULT NULL,
					  KEY X_',SortedTable,'_element_id (element_id) USING BTREE,
					  KEY X_',SortedTable,'_concept_id (concept_id) USING BTREE,
					  KEY X_',SortedTable,'_score (score) USING BTREE
					) ENGINE=MyISAM DEFAULT CHARSET=latin1 
						SELECT * FROM ',TableToSort,'
						ORDER BY concept_id, score DESC;'
					); 
	
	# Prepare statements	 
	PREPARE CreateSortedTableStmt FROM @CreateSortedTableQuery;
	PREPARE DropTablesStmt FROM @DropTableQuery;	 
	
	IF BigTable=TRUE THEN 
		SET SESSION myisam_repair_threads=1; 		 
        END IF;	
	
	EXECUTE DropTablesStmt;
	EXECUTE CreateSortedTableStmt;	
	 
	IF BigTable=TRUE THEN 
		SET SESSION myisam_repair_threads=8; 		 
        END IF;	 
END$$

DELIMITER ;