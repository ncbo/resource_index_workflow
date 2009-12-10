USE `resource_index`;

/*************************************************/ 
	-- Populate obs_map_temp from table obs_hibernate.obs_map
	-- by replacing local_concept_id and mapped_local_concept_id with concept_id and mapped_concept_id	 
/*************************************************/ 

DELIMITER $$

DROP PROCEDURE IF EXISTS `resource_index`.`populate_obs_map_temp`$$

CREATE PROCEDURE `resource_index`.`populate_obs_map_temp`()
BEGIN 
	-- Create table resource_index.obs_map_temp
	DROP TABLE IF EXISTS `resource_index`.`obs_map_temp`;
	CREATE TABLE  `resource_index`.`obs_map_temp` (
	  `id` int(11) NOT NULL auto_increment,
	  `concept_id` int(11) NOT NULL,
	  `mapped_concept_id` int(11) default NULL,
	  `mapping_type` varchar(246) NOT NULL,
	  PRIMARY KEY  (`id`),
	  KEY `IDX_concept_id` (`concept_id`),
	  KEY `IDX_mapped_concept_id` (`mapped_concept_id`),
	  KEY `IDX_mapping_type` (`mapping_type`)
	) ENGINE=InnoDB AUTO_INCREMENT=5691133 DEFAULT CHARSET=latin1;
 
        -- Inserting rows to obs_map_temp 
	INSERT  resource_index.obs_map_temp(id, concept_id, mapped_concept_id, mapping_type)
		SELECT map.id, ct1.id , ct2.id  , map.mapping_type 
			FROM obs_hibernate.obs_map map, obs_hibernate.obs_concept ct1, obs_hibernate.obs_concept ct2 
	 		WHERE map.local_concept_id= ct1.local_concept_id 
	 			AND map.mapped_local_concept_id=ct2.local_concept_id;
	 				 
END$$

DELIMITER ;