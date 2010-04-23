DELIMITER $$

DROP PROCEDURE IF EXISTS `resource_index_test`.`CommonBatchInsertProcedure`$$

CREATE DEFINER=`optra`@`%` PROCEDURE `CommonBatchInsertProcedure`(IN TableToPopulate VARCHAR(100),IN InsertQueryString text, IN DisableKeys BOOL, OUT RowsAffected BIGINT)
BEGIN
        # Define counter 
        DECLARE counter BIGINT(20) unsigned DEFAULT 0;       
	
	#Creating SQL queries 
	SET @BulkInsertQuery = InsertQueryString;
	SET @SetCounterQuery = Concat( 'SELECT if (MAX(id) iS NULL, 0, MAX(id)) INTO @counter FROM ',TableToPopulate,'; ');
	SET @DisableKeysQuery = Concat( 'ALTER TABLE ',TableToPopulate, ' DISABLE KEYS;');
	SET @EnableKeysQuery =  Concat( 'ALTER TABLE ',TableToPopulate, ' ENABLE KEYS;');
	 
	# Prepare statements 
	PREPARE InsertStmt FROM @BulkInsertQuery;
	PREPARE SetCounterStmt FROM @SetCounterQuery;
	PREPARE DisableKeysStmt FROM @DisableKeysQuery;
	PREPARE EnableKeysStmt FROM @EnableKeysQuery;
	
	# Initialize counter used for id    
	EXECUTE SetCounterStmt; 
	
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