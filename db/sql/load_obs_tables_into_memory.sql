DELIMITER $$

DROP PROCEDURE IF EXISTS `resource_index`.`load_obs_tables_into_memory`$$

CREATE PROCEDURE `load_obs_tables_into_memory`()
BEGIN
    -- 
    -- LOAD only the necessary part of obr_term and stuff it into memory.
    -- 
    DROP TABLE IF EXISTS `resource_index`.`obr_term_mem`;       
       
    CREATE TABLE `obr_term_mem` (
	 `id` int(11) unsigned,
	 `concept_id` int(11) unsigned,
	 `is_preferred` tinyint(1) NOT NULL,
	 KEY `X_obr_term_mem_id` (`id`)
	) ENGINE=MEMORY DEFAULT CHARSET=latin1
	SELECT `id`, `concept_id`, `is_preferred`
	FROM obs_term;
	
	--
	-- LOAD obr_map and stuff it into memory.
	-- 
	DROP TABLE IF EXISTS `resource_index`.`obr_map_mem`;	
	
	CREATE TABLE `obr_map_mem` (
	 `concept_id` int(11) unsigned NOT NULL,
	 `mapped_concept_id` int(11) unsigned NOT NULL,
	 `mapping_type` varchar(246) NOT NULL,
	 KEY `X_obr_map_MEM_concept_id`(`concept_id`)
	) ENGINE=MEMORY DEFAULT CHARSET=latin1
	SELECT `concept_id`, `mapped_concept_id`, `mapping_type`
	FROM obs_map;
	
	--
	-- Create the lookup table for mapping_type.
	--
	DROP TABLE IF EXISTS `resource_index`.`obr_mapping_type_mem`;		
	 
	CREATE TABLE `obr_mapping_type_mem` (
	 `id` TINYINT(1) unsigned NOT NULL AUTO_INCREMENT,
	 `mapping_type` varchar(30) NOT NULL,
	 PRIMARY KEY (id)
	) ENGINE=MEMORY DEFAULT CHARSET=latin1
	SELECT id, mapping_type FROM obs_mapping_type;
        
        -- Prepare the new integer field.
	ALTER TABLE `obr_map_mem` ADD mapping_type_id int(11);
	
	-- Update the integer field with the lookup key.
	UPDATE `obr_map_mem` m SET m.mapping_type_id = 
	 @id:=(SELECT t.id FROM obr_mapping_type_mem t
	   WHERE t.mapping_type = m.mapping_type);
	
	-- Save the integer field as the new mapping_type field.	 
	ALTER TABLE `obr_map_mem` DROP COLUMN mapping_type;
	ALTER TABLE `obr_map_mem` CHANGE COLUMN 
	mapping_type_id mapping_type TINYint(1) unsigned NOT NULL;
                
        -- 
	-- LOAD obs_relation and stuff it into memory.
	-- 	
	DROP TABLE IF EXISTS `resource_index`.`obr_relation_mem`;		
	
	CREATE TABLE `obr_relation_mem` (
	 `concept_id` int(11) unsigned NOT NULL,
	 `parent_concept_id` int(11) unsigned NOT NULL,
	 `level` tinyint(4) unsigned NOT NULL,
	 KEY `X_obr_relation_MEM_concept_id` (`concept_id`)
	) ENGINE=MEMORY DEFAULT CHARSET=latin1
	SELECT `concept_id`, `parent_concept_id`, `level`
	FROM obs_relation; 
	
	-- 
	-- LOAD obr_comcept and stuff it into memory.
	-- 
	DROP TABLE IF EXISTS `resource_index`.`obr_concept_mem`;	
	
	CREATE TABLE `obr_concept_mem` (
	  `id` int(11) NOT NULL,   
	  `ontology_id` int(11) NOT NULL,  
	   KEY `X_obr_concept_mem_id` (`id`),
	  KEY `X_obs_concept_ontology_id` (`ontology_id`)  
	) ENGINE=MEMORY DEFAULT CHARSET=latin1 
	SELECT id, ontology_id from obs_concept;
	
	-- 
	-- LOAD obs_ontology and stuff it into memory.
	-- 
	DROP TABLE IF EXISTS `resource_index`.`obr_ontology_mem`;
	
	CREATE TABLE `obr_ontology_mem` (
	  `id` int(11) NOT NULL AUTO_INCREMENT,   
	  `local_ontology_id` varchar(246) NOT NULL,
	  `dictionary_id` smallint(5) unsigned NOT NULL,
	   KEY `X_obr_ontology_mem_id` (`id`),  
	   KEY `X_obr_ontology_mem_local_ontology_id` (`local_ontology_id`),
	   KEY `X_obs_ontology_dictionary_id` (`dictionary_id`)
	) ENGINE=MEMORY DEFAULT CHARSET=latin1	
	SELECT id, local_ontology_id, dictionary_id from obs_ontology;
END$$

DELIMITER ;