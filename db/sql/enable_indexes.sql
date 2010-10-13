DELIMITER $$

DROP PROCEDURE IF EXISTS `resource_index`.`enable_indexes`$$

CREATE  PROCEDURE `enable_indexes`(IN TableToPopulate VARCHAR(100), IN BigTable BOOL)
BEGIN	 
	SET @EnableKeysQuery =  Concat( 'ALTER TABLE ',TableToPopulate, ' ENABLE KEYS;');
	  
	# Prepare statements	 
	PREPARE EnableKeysStmt FROM @EnableKeysQuery;
	
	IF BigTable=TRUE THEN 
		SET SESSION myisam_repair_threads=1; 		
        END IF;	
	EXECUTE EnableKeysStmt;		 
	
	IF BigTable=TRUE THEN 
		SET SESSION myisam_repair_threads=8; 		 
        END IF;	 
END$$

DELIMITER ;