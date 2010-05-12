DELIMITER $$

DROP PROCEDURE IF EXISTS `resource_index`.`common_batch_insert`$$

CREATE DEFINER=`optra`@`%` PROCEDURE `common_batch_insert`(IN TableToPopulate VARCHAR(100),IN InsertQueryString text, IN DisableKeys BOOL, OUT RowsAffected BIGINT)
BEGIN
        # Define counter 
        DECLARE counter BIGINT(20) unsigned DEFAULT 0; 
	
	#Creating SQL queries 
	SET @BulkInsertQuery = InsertQueryString;	 
	SET @DisableKeysQuery = Concat( 'ALTER TABLE ',TableToPopulate, ' DISABLE KEYS;');
	SET @EnableKeysQuery =  Concat( 'ALTER TABLE ',TableToPopulate, ' ENABLE KEYS;');
	 
	# Prepare statements 
	PREPARE InsertStmt FROM @BulkInsertQuery;	 
	PREPARE DisableKeysStmt FROM @DisableKeysQuery;
	PREPARE EnableKeysStmt FROM @EnableKeysQuery;
	 
	# Disbale keys
        IF DisableKeys=TRUE THEN 
		EXECUTE DisableKeysStmt; 
		SET autocommit=0;
		SET unique_checks=0;
		SET foreign_key_checks=0;
        END IF;		
	
	#Exceute insert statemente
	EXECUTE InsertStmt;
	SELECT ROW_COUNT() INTO RowsAffected ;	 
       
        # Enable keys
	IF DisableKeys=TRUE THEN 
		# Commiting it
		COMMIT;  
		
		EXECUTE EnableKeysStmt;
		SET autocommit=1;
		SET unique_checks=1;
		SET foreign_key_checks=1; 	
	END IF;	
       
             
    END$$

DELIMITER ;