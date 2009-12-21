package org.ncbo.stanford.obr.enumeration;

import org.ncbo.stanford.obr.util.MessageUtils;
/**
 * 
 * @author Kuladip Yadav
 *
 */
public enum ObsSchemaEnum {
	ONTOLOGY_TABLE(MessageUtils.getMessage("obs.ontology.table")), 
	CONCEPT_TABLE(MessageUtils.getMessage("obs.concept.table")), 
	DICTIONARY_VERSION_TABLE(MessageUtils.getMessage("obs.dictionary.table")), 
	TERM_TABLE(MessageUtils.getMessage("obs.term.table")), 
	//TODO : Use obs_hibernate.obs_map after fixed by Cherie
	MAPPING_TABLE("resource_index.obs_map_temp"), 
	IS_A_PARENT_TABLE(MessageUtils.getMessage("obs.relation.table")),
	;

	private String name;
	
	/** OBS Schema name. */
	private static final String OBS_SCHEMA_NAME = MessageUtils.getMessage("obs.schema.name") +".";

	private ObsSchemaEnum(String name) {
		this.name = name;
	}

	/**
	 * 
	 * @return table name with schema
	 */
	public String getTableSQLName() {	
		// TODO : Remove condition after fixing obs_map issue.
		if(this== MAPPING_TABLE){
			return  this.name;
		}
		
		return OBS_SCHEMA_NAME + this.name;
	}

}
