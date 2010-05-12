DELIMITER $$

DROP PROCEDURE IF EXISTS `resource_index`.`load_context_table_into_memory`$$

CREATE DEFINER=`optra`@`%` PROCEDURE `load_context_table_into_memory`()
BEGIN
    
     DROP TABLE IF EXISTS `resource_index`.`obr_context_mem`;     
       
     CREATE TABLE `obr_context_mem` (
	 `id` int(11) unsigned,
	 `weight` double ,
	 KEY `X_obr_context_mem_id` (`id`)
	) ENGINE=MEMORY DEFAULT CHARSET=latin1
	SELECT `id`, `weight`
	FROM obr_context;
END$$

DELIMITER ;