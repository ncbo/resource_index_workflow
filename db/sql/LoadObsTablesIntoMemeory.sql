DELIMITER $$

DROP PROCEDURE IF EXISTS `resource_index`.`LoadObsTablesIntoMemeory`$$

CREATE DEFINER=`optra`@`%` PROCEDURE `LoadObsTablesIntoMemeory`()
BEGIN
    -- 
	-- LOAD only the necessary part of obs_term and stuff it into memory.
	-- 
    DROP TABLE IF EXISTS `resource_index`.`obs_term_mem`;       
       
    CREATE TABLE `obs_term_mem` (
	 `id` int(11) unsigned,
	 `concept_id` int(11) unsigned,
	 KEY `X_obs_term_mem_id` (`id`)
	) ENGINE=MEMORY DEFAULT CHARSET=latin1
	SELECT `id`, `concept_id` 
	FROM obs_term;
	
	--
	-- LOAD obs_map and stuff it into memory.
	-- 
	DROP TABLE IF EXISTS `resource_index`.`obs_map_mem`;	
	
	CREATE TABLE `obs_map_mem` (
	 `concept_id` int(11) unsigned NOT NULL,
	 `mapped_concept_id` int(11) unsigned NOT NULL,
	 `mapping_type` varchar(246) NOT NULL,
	 KEY `X_obs_map_MEM_concept_id`(`concept_id`)
	) ENGINE=MEMORY DEFAULT CHARSET=latin1
	SELECT `concept_id`, `mapped_concept_id`, `mapping_type`
	FROM obs_map;
	
	--
	-- Create the lookup table for mapping_type.
	--
	DROP TABLE IF EXISTS `resource_index`.`obs_mapping_type_mem`;		
	 
	CREATE TABLE `obs_mapping_type_mem` (
	 `id` TINYINT(1) unsigned NOT NULL AUTO_INCREMENT,
	 `mapping_type` varchar(30) NOT NULL,
	 PRIMARY KEY (id)
	) ENGINE=MEMORY DEFAULT CHARSET=latin1
	SELECT id, mapping_type FROM obs_mapping_type;
        
        -- Prepare the new integer field.
	ALTER TABLE `obs_map_mem` ADD mapping_type_id int(11);
	
	-- Update the integer field with the lookup key.
	UPDATE `obs_map_mem` m SET m.mapping_type_id = 
	 @id:=(SELECT t.id FROM obs_mapping_type_mem t
	   WHERE t.mapping_type = m.mapping_type);
	
	-- Save the integer field as the new mapping_type field.	 
	ALTER TABLE `obs_map_mem` DROP COLUMN mapping_type;
	ALTER TABLE `obs_map_mem` CHANGE COLUMN 
	mapping_type_id mapping_type int(11) unsigned NOT NULL;
                
        -- 
	-- LOAD obs_map and stuff it into memory.
	-- 	
	DROP TABLE IF EXISTS `resource_index`.`obs_relation_mem`;		
	
	CREATE TABLE `obs_relation_mem` (
	 `concept_id` int(11) unsigned NOT NULL,
	 `parent_concept_id` int(11) unsigned NOT NULL,
	 `level` tinyint(4) unsigned NOT NULL,
	 KEY `X_obs_relation_MEM_concept_id` (`concept_id`)
	) ENGINE=MEMORY DEFAULT CHARSET=latin1
	SELECT `concept_id`, `parent_concept_id`, `level`
	FROM obs_relation; 
	
END$$

DELIMITER ;