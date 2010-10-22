DELIMITER $$

DROP PROCEDURE IF EXISTS `resource_index`.`load_context_table_into_memory`$$

CREATE PROCEDURE `load_context_table_into_memory`(IN ResourceId VARCHAR(100))
BEGIN
    
     DROP TABLE IF EXISTS `resource_index`.`obr_context_mem`;     
     
     SET @CreateQuery:= Concat( 'CREATE TABLE `obr_context_mem` (
	 `id` int(11) unsigned,
	 `weight` double ,
	 KEY `X_obr_context_mem_id` (`id`)
	) ENGINE=MEMORY DEFAULT CHARSET=latin1
	SELECT `id`, `weight`
	FROM obr_context WHERE name like "', ResourceId,'%";'); 
     
     PREPARE CreateStmt FROM @CreateQuery;	
     EXECUTE CreateStmt;
     
END$$

DELIMITER ;