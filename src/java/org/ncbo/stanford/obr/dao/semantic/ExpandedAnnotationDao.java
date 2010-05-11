package org.ncbo.stanford.obr.dao.semantic;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import obs.common.beans.DictionaryBean;

import org.ncbo.stanford.obr.dao.AbstractObrDao;
import org.ncbo.stanford.obr.dao.annoation.DirectAnnotationDao;
import org.ncbo.stanford.obr.dao.element.ElementDao;
import org.ncbo.stanford.obr.dao.obs.concept.ConceptDao;
import org.ncbo.stanford.obr.dao.obs.ontology.OntologyDao;
import org.ncbo.stanford.obr.enumeration.ExpansionTypeEnum;
import org.ncbo.stanford.obr.enumeration.WorkflowStatusEnum;
import org.ncbo.stanford.obr.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
 * This class is a representation for the the OBR DB OBR_XX_EAT table. The table contains 
 * the following columns:
 * 
 * <ul>
 * <li> id 			            		BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 * <li> elementID  	            			INT UNSIGNED NOT NULL FOREIGN KEY,
 * <li> conceptID							INT UNSIGNED NOT NULL FOREIGN KEY,
 * <li> contextID							SMALLINT UNSIGNED NOT NULL FOREIGN KEY,
 * <li> dictionaryID						SMALLINT UNSIGNED NOT NULL FOREIGN KEY,
 * <li> childConceptID						INT UNSIGNED FOREIGN KEY,
 * <li> level								SMALLINT UNSIGNED,	
 * <li> mappedConceptID						INT UNSIGNED FOREIGN KEY,
 * <li> mappingType							VARVHAR(20),  
 * <li> distantConceptID					INT UNSIGNED FOREIGN KEY,
 * <li> distance							SMALLINT UNSIGNED,
 * <li> indexingDone						BOOL     
 * </ul>
 *  
 * @author Adrien Coulet, Clement Jonquet
 * @version OBR_v0.2		
 * @created 13-Nov-2008
 *
 */
public class ExpandedAnnotationDao extends AbstractObrDao {

	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obr.expanded.annotation.table.suffix");
	
	private PreparedStatement addEntryStatement; 
	private PreparedStatement deleteEntriesFromOntologyStatement;
	
	/**
	 * Creates a new ExpandedAnnotationTable with a given resourceID.
	 * The suffix that will be added for AnnotationTable is "_EAT".
	 */
	public ExpandedAnnotationDao(String resourceID) {
		super(resourceID, TABLE_SUFFIX);
	}

	/**
	 * Returns the SQL table name for a given resourceID 
	 */
	public static String name(String resourceID){
		return OBR_PREFIX + resourceID.toLowerCase() + TABLE_SUFFIX;
	}
	
	@Override
	protected String creationQuery(){
		//logger.info("creation of the table "+ this.getTableSQLName());
		return "CREATE TABLE " + getTableSQLName() +" (" +
					"id BIGINT(20) UNSIGNED NOT NULL, " +
					"element_id INT(11) UNSIGNED NOT NULL, " +
					"concept_id INT(11) UNSIGNED NOT NULL, " +			
					"context_id SMALLINT(5) UNSIGNED NOT NULL, " +	
					"expansion_type TINYINT(1) UNSIGNED NOT NULL, " +
					"expansion_concept_id INT(11) UNSIGNED, " +
					"expansion_value SMALLINT(5) UNSIGNED NOT NULL, " +	
					"workflow_status TINYINT(1) UNSIGNED NOT NULL DEFAULT '0'" +
					")ENGINE=MyISAM DEFAULT CHARSET=latin1;";				 
	}
	
	protected String getIndexCreationQuery(){
		  return "ALTER TABLE " + this.getTableSQLName() +
	  			" ADD PRIMARY KEY(id), " +
	  			" ADD INDEX IDX_"+ this.getTableSQLName() +"_element_id(element_id), " +
	  			" ADD INDEX IDX_"+ this.getTableSQLName() +"_concept_id(concept_id), " +
	  			" ADD INDEX IDX_"+ this.getTableSQLName() +"_expansion_type(expansion_type), " +
	  			" ADD INDEX IDX_"+ this.getTableSQLName() +"_workflow_status(workflow_status) "; 
	}

	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();
		this.openAddEntryStatement();		 
		this.openDeleteEntriesFromOntologyStatement();
	}

	@Override
	protected void closePreparedStatements() throws SQLException {
		super.closePreparedStatements();
		this.addEntryStatement.close();		 
		this.deleteEntriesFromOntologyStatement.close();
	}
	
	/****************************************** FUNCTIONS ON THE TABLE ***************************/ 

	@Override
	protected void openAddEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (element_id, concept_id, context_id, child_concept_id, parent_level, mapped_concept_id, mapping_type, distant_concept_id, distance, indexing_done) "); 
		queryb.append("VALUES (");
			// sub query to get the elementID from the localElementID
			queryb.append("(SELECT id FROM ");
			queryb.append(ElementDao.name(this.resourceID));
			queryb.append(" WHERE local_element_id=?)");
		queryb.append("	,");
			// sub query to get the conceptID from the localConceptID
			queryb.append("(SELECT id FROM ");
			queryb.append(conceptDao.getTableSQLName());
			queryb.append(" WHERE local_concept_id=?)");
		queryb.append("	,");
			// sub query to get the contextID from the contextName
			queryb.append("(SELECT id FROM ");
			queryb.append(contextTableDao.getTableSQLName());
			queryb.append(" WHERE name=?)");
		queryb.append("	,");
			// sub query to get the conceptID from the childConceptID
			queryb.append("(SELECT id FROM ");
			queryb.append(conceptDao.getTableSQLName());
			queryb.append(" WHERE local_concept_id=?)");
		queryb.append("	,?,"); //level
			// sub query to get the conceptID from the mappedConceptID
			queryb.append("(SELECT id FROM ");
			queryb.append(conceptDao.getTableSQLName());
			queryb.append(" WHERE local_concept_id=?)");
		queryb.append("	,?,"); //mappingType
			// sub query to get the conceptID from the distantConceptID
			queryb.append("(SELECT id FROM ");
			queryb.append(conceptDao.getTableSQLName());
			queryb.append(" WHERE local_concept_id=?)");
		queryb.append(",?,?)"); //distance					
		this.addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}

	/**
	 * Add an new entry in corresponding SQL table.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addEntry(ExpandedAnnotationEntry entry){
		boolean inserted = false;
		try {
			this.addEntryStatement.setString(1, entry.getLocalElementID());
			this.addEntryStatement.setString(2, entry.getLocalConceptID());
			this.addEntryStatement.setString(3, entry.getContextName());
			this.addEntryStatement.setString(4, entry.getChildConceptID());
			this.addEntryStatement.setInt(5, entry.getLevel());
			this.addEntryStatement.setString(6, entry.getMappedConceptID());
			this.addEntryStatement.setString(7, entry.getMappingType());
			this.addEntryStatement.setString(8, entry.getDistantConceptID());
			this.addEntryStatement.setInt(9, entry.getDistance());
			this.addEntryStatement.setBoolean(10, entry.getIndexingDone());
			this.executeSQLUpdate(this.addEntryStatement);
			inserted = true;
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openAddEntryStatement();
			return this.addEntry(entry);
		}
		catch (MySQLIntegrityConstraintViolationException e){
			//logger.error("Table " + this.getTableSQLName() + " already contains an entry for the concept: " + entry.getLocalConceptID() +".");
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot add entry "+entry.toString()+" on table " + this.getTableSQLName(), e);
			logger.error(entry.toString());
		}
		return inserted;	
	}
		 
	//********************************* SEMANTIC EXPANSION FUNCTIONS *****************************************************/
	
	/**
	 * Populates the table with isa transitive closure annotations computed 
	 * with the annotations contained in the given table. 
	 * Only the annotations for which isaClosureDone=false are selected in the given table.
	 *  
	 * @param annotationDao
	 * @param maxLevel {@code int} if greater than zero then restrict is closure expansion annotations upto this level  
	 * @return {@code int} the number of isaClosure annotations created in the corresponding _EAT.
	 */
	public int isaClosureExpansion(DirectAnnotationDao annotationDao){
		int nbAnnotation;		 
		// Query Used :
		// 		INSERT obr_tr_expanded_annotation(element_id, concept_id, context_id, child_concept_id, parent_level, indexing_done)
		//			SELECT element_id, ISAPT.parent_concept_id, context_id, DAT.concept_id, level, false 
		//				FROM obr_gm_annotation AS DAT, obs_relation AS ISAPT 
		//				WHERE DAT.concept_id = ISAPT.concept_id
		//					AND is_a_closure_done = false; 
		
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (id, element_id, concept_id, context_id, expansion_type, expansion_concept_id, expansion_value, workflow_status) SELECT @counter:=@counter+1, element_id, ISAPT.parent_concept_id, context_id,");
		queryb.append(ExpansionTypeEnum.IS_A_CLOSURE.getType());
		queryb.append(", DAT.concept_id, level, ");
		queryb.append(WorkflowStatusEnum.INDEXING_NOT_DONE.getStatus());
		queryb.append(" FROM ");
		queryb.append(annotationDao.getTableSQLName());
		queryb.append(" AS DAT, ");			 
		queryb.append(relationDao.getMemoryTableSQLName()); // JOin with memory table.
		queryb.append(" AS ISAPT WHERE DAT.concept_id = ISAPT.concept_id AND DAT.workflow_status = ");
		queryb.append(WorkflowStatusEnum.DIRECT_ANNOTATION_DONE.getStatus());		 
		queryb.append("; ");
		
		StringBuffer updatingQueryb = new StringBuffer();
		updatingQueryb.append("UPDATE ");
		updatingQueryb.append(annotationDao.getTableSQLName());
		updatingQueryb.append(" SET workflow_status = ");
		updatingQueryb.append(WorkflowStatusEnum.IS_A_CLOSURE_DONE.getStatus());
		updatingQueryb.append(" WHERE workflow_status = ");
		updatingQueryb.append(WorkflowStatusEnum.DIRECT_ANNOTATION_DONE.getStatus());
		try{
			nbAnnotation = this.executeWithStoreProcedure(this.getTableSQLName(), queryb.toString(), true);
			this.executeWithStoreProcedure(annotationDao.getTableSQLName(), updatingQueryb.toString(), true);
		}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot execute the isa transitive closure on table " + this.getTableSQLName() +". 0 returned", e);
			nbAnnotation = 0;
		}
		return nbAnnotation;
	}
	
	/**
	 * Populates the table with mapping annotations computed 
	 * with the annotations contained in the given table. 
	 * Only the annotations for which mappingDone=false are selected in the given table.
	 *  
	 * @param DirectAnnotationDao
	 * @return Returns the number of mapping annotations created in the corresponding _EAT.
	 */
	public int mappingExpansion(DirectAnnotationDao annotationDao){
		int nbAnnotation;	 
		// Query Used :
		// 		INSERT obr_tr_expanded_annotation(element_id, concept_id, context_id, mapped_concept_id, mapping_type, indexing_done)
		//			SELECT element_id, MAPT.mapped_concept_id, context_id, DAT.concept_id, mapping_type, false 
		//				FROM obr_gm_annotation AS DAT, obs_map AS MAPT 
		//				WHERE DAT.concept_id = MAPT.concept_id
		//					AND mapping_done = false ;		
		
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (id, element_id, concept_id, context_id, expansion_type, expansion_concept_id, expansion_value, workflow_status) SELECT @counter:=@counter+1, element_id, MAPT.mapped_concept_id, context_id, ");
		queryb.append(ExpansionTypeEnum.MAPPING.getType());
		queryb.append(", DAT.concept_id, mapping_type, ");
		queryb.append(WorkflowStatusEnum.INDEXING_NOT_DONE.getStatus());	
		queryb.append(" FROM ");	   
		queryb.append(annotationDao.getTableSQLName());
		queryb.append(" AS DAT, ");		
		queryb.append(mapDao.getMemoryTableSQLName()); // JOin with memory map table
		queryb.append(" AS MAPT WHERE DAT.concept_id = MAPT.concept_id AND DAT.workflow_status = ");
		queryb.append(WorkflowStatusEnum.IS_A_CLOSURE_DONE.getStatus());		 
		queryb.append("; ");
		
		StringBuffer updatingQueryb = new StringBuffer();
		updatingQueryb.append("UPDATE ");
		updatingQueryb.append(annotationDao.getTableSQLName());
		updatingQueryb.append(" SET workflow_status = ");
		updatingQueryb.append(WorkflowStatusEnum.MAPPING_DONE.getStatus());
		updatingQueryb.append(" WHERE workflow_status = ");
		updatingQueryb.append(WorkflowStatusEnum.IS_A_CLOSURE_DONE.getStatus());
		
		try{
			nbAnnotation = this.executeWithStoreProcedure(this.getTableSQLName(), queryb.toString(), true);
			this.executeWithStoreProcedure(annotationDao.getTableSQLName(), updatingQueryb.toString(), true);
		}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot execute the mapping expansion query on table " + this.getTableSQLName() +". 0 returned", e);
			nbAnnotation = 0;
		}
		return nbAnnotation;
	}
	
	//********************************* DELETE FUNCTIONS *****************************************************/
	
	/**
	 * Selecting the ontology id from OBS_OT table given local ontology id.
	 * Selecting the concept id from OBS_CT table given ontology id.
	 * Deleting concept id from OBR_EAT table given concept id. 
	 */
	private void openDeleteEntriesFromOntologyStatement(){
		// Query Used :
		//	DELETE EAT FROM obr_tr_expanded_annotation EAT, obs_concept CT, obs_ontology OT
		//		WHERE EAT.conept_id = CT.id
		//			AND CT.ontology_id = OT.id
		//			AND OT.local_ontology_id = ?;		
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE EAT FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" EAT, ");
		queryb.append(ConceptDao.name( ));	
		queryb.append(" CT, ");
		queryb.append(OntologyDao.name());
		queryb.append(" OT ");
		queryb.append(" WHERE EAT.concept_id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id = ?");
		this.deleteEntriesFromOntologyStatement = this.prepareSQLStatement(queryb.toString());
	}
		
	/**
	 * Deletes the rows corresponding to annotations done with a concept in the given localOntologyID.
	 * @return True if the rows were successfully removed. 
	 */
	public boolean deleteEntriesFromOntology(String localOntologyID){
		boolean deleted = false;
		try{
			this.deleteEntriesFromOntologyStatement.setString(1, localOntologyID);
			this.executeSQLUpdate(this.deleteEntriesFromOntologyStatement);
			deleted = true;
		}		
		catch (MySQLNonTransientConnectionException e) {
			this.openDeleteEntriesFromOntologyStatement();
			return this.deleteEntriesFromOntology(localOntologyID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot delete entries from "+this.getTableSQLName()+" for localOntologyID: "+ localOntologyID+". False returned.", e);
		}
		return deleted;
	}
	
	/**
	 * Deletes the rows corresponding to expanded annotations done with a concept in the given list of localOntologyIDs.
	 * 
	 * @param {@code List} of local ontology ids
	 * @return True if the rows were successfully removed. 
	 */
	public boolean deleteEntriesFromOntologies(List<String> localOntologyIDs){		
		boolean deleted = false;
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE EAT FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" EAT, ");
		queryb.append(ConceptDao.name( ));	
		queryb.append(" CT, ");
		queryb.append(OntologyDao.name());
		queryb.append(" OT ");
		queryb.append(" WHERE EAT.concept_id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id IN (");
		
		for (String localOntologyID : localOntologyIDs) {
			queryb.append("'");
			queryb.append(localOntologyID);
			queryb.append("', ");
		}
		queryb.delete(queryb.length()-2, queryb.length());
		queryb.append(");");

		try{			 
			this.executeSQLUpdate(queryb.toString() );
			deleted = true;
		}		
		catch (MySQLNonTransientConnectionException e) {			 
			return this.deleteEntriesFromOntologies(localOntologyIDs);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot delete entries from "+this.getTableSQLName()+" for localOntologyIDs: "+ localOntologyIDs+". False returned.", e);
		}
		return deleted;
	}
	
	//**********************Annotations Statistics ******************/
	
	/**
	 * 
	 *  Get number of IS A Annotations for each ontlogyID
	 * @param dictionary 
	 * @param withCompleteDictionary 
	 *  
	 *  @return HashMap<Integer, Integer>
	 */
	public HashMap<Integer, Integer> getISAAnnotationStatistics(boolean withCompleteDictionary, DictionaryBean dictionary){
		HashMap<Integer, Integer> annotationStats = new HashMap<Integer, Integer>();
		
		StringBuffer queryb = new StringBuffer(); 
		if(withCompleteDictionary){
			queryb.append("SELECT CT.ontology_id, COUNT(EAT.id) AS COUNT FROM ");
			queryb.append(this.getTableSQLName());		 	 
			queryb.append(" AS EAT, ");
			queryb.append(conceptDao.getTableSQLName());
			queryb.append(" AS CT WHERE EAT.concept_id=CT.id AND EAT.expansion_type =");
			queryb.append(ExpansionTypeEnum.IS_A_CLOSURE.getType());
			queryb.append(" GROUP BY CT.ontology_id; ");		 
		}else{
			queryb.append("SELECT OT.id, COUNT(EAT.id) AS COUNT FROM ");
			queryb.append(this.getTableSQLName());		 	 
			queryb.append(" AS EAT, ");
			queryb.append(conceptDao.getTableSQLName());
			queryb.append(" AS CT, ");
			queryb.append(ontologyDao.getTableSQLName());
			queryb.append(" AS OT WHERE EAT.concept_id=CT.id AND CT.ontology_id=OT.id AND EAT.expansion_type =");
			queryb.append(ExpansionTypeEnum.IS_A_CLOSURE.getType());
			queryb.append(" OT.dictionary_id = ");
			queryb.append(dictionary.getDictionaryID());				 
			queryb.append( " GROUP BY OT.id; ");
		}
		
		try {			 			
			ResultSet rSet = this.executeSQLQuery(queryb.toString());
			while(rSet.next()){
				annotationStats.put(rSet.getInt(1), rSet.getInt(2));
			}			
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {			 
			return this.getISAAnnotationStatistics(withCompleteDictionary, dictionary);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get IS A annotations statistics from "+this.getTableSQLName()+" .", e);
		}
		return annotationStats;
		 
	}
	
	
	/**
	 * 
	 *  Get number of Mapping Annotations for each ontlogyID
	 * @param dictionary 
	 * @param withCompleteDictionary 
	 *  
	 *  @return HashMap<Integer, Integer>
	 */
	public HashMap<Integer, Integer> getMappingAnnotationStatistics(boolean withCompleteDictionary, DictionaryBean dictionary){
		HashMap<Integer, Integer> annotationStats = new HashMap<Integer, Integer>();
		
		StringBuffer queryb = new StringBuffer(); 
		if(withCompleteDictionary){
			queryb.append("SELECT CT.ontology_id, COUNT(EAT.id) AS COUNT FROM ");
			queryb.append(this.getTableSQLName());		 	 
			queryb.append(" AS EAT, ");
			queryb.append(conceptDao.getTableSQLName());
			queryb.append(" AS CT WHERE EAT.concept_id=CT.id AND EAT.expansion_type =");
			queryb.append(ExpansionTypeEnum.MAPPING.getType());
			queryb.append(" GROUP BY CT.ontology_id; ");		 
		}else{
			queryb.append("SELECT OT.id, COUNT(EAT.id) AS COUNT FROM ");
			queryb.append(this.getTableSQLName());		 	 
			queryb.append(" AS EAT, ");
			queryb.append(conceptDao.getTableSQLName());
			queryb.append(" AS CT, ");
			queryb.append(ontologyDao.getTableSQLName());
			queryb.append(" AS OT WHERE EAT.concept_id=CT.id AND CT.ontology_id=OT.id AND EAT.expansion_type =");
			queryb.append(ExpansionTypeEnum.MAPPING.getType());
			queryb.append(" OT.dictionary_id = ");
			queryb.append(dictionary.getDictionaryID());				 
			queryb.append( " GROUP BY OT.id; ");
		}
		
		try {			 			
			ResultSet rSet = this.executeSQLQuery(queryb.toString());
			while(rSet.next()){
				annotationStats.put(rSet.getInt(1), rSet.getInt(2));
			}			
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {			 
			return this.getMappingAnnotationStatistics(withCompleteDictionary, dictionary);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get mapping annotations statistics from " + this.getTableSQLName()+ " .", e);
		}
		return annotationStats;
		 
	}
	
	public boolean isIndexExist(){
		boolean isIndexExist= false;
		try {			 			
			ResultSet rSet = this.executeSQLQuery("SHOW INDEX FROM "+ this.getTableSQLName());
			if(rSet.first()){
				isIndexExist= true;
			} 
			
			rSet.close();
		} 
		catch (SQLException e) {
			logger.error("** PROBLEM **  Problem in getting index from " + this.getTableSQLName()+ " .", e);
		}
		
		return isIndexExist;
	}
	
	public boolean createIndex() {
		boolean result = false;
		try{
			this.executeSQLUpdate(getIndexCreationQuery());
			result = true;
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot delete the temporary table.", e);
		}
		 return result; 
	}
	
	/********************************* ENTRY CLASS *****************************************************/

	/**
	 * This class is a representation for an annotation extracted from OBR_XX_DAT table.
	 * 
	 * @author Adrien Coulet
	 * @version OBR_v0.2		
	 * @created 15-Dec-2008
	 */
	public static class ExpandedAnnotation {

		private String  localElementID;
		private String  localConceptID;
		private String  contextName;
		private String  childConceptLocalID;
		private Integer level;
		private String  mappedConceptLocalID;
		private String  mappingType;
		private Float   score;
		
		public ExpandedAnnotation(String localElementID, String localConceptID,
				String contextName, String childConceptLocalID, Integer level, 
				String mappedConceptLocalID, String mappingType, Float score) {
			super();
			this.localElementID       = localElementID;
			this.localConceptID       = localConceptID;
			this.contextName          = contextName;
			this.childConceptLocalID  = childConceptLocalID;
			this.level                = level;
			this.mappedConceptLocalID = mappedConceptLocalID;
			this.mappingType          = mappingType;
			this.score                = score;
			
		}

		public String getLocalElementID() {
			return localElementID;
		}

		public String getLocalConceptID() {
			return localConceptID;
		}

		public String getContextName() {
			return contextName;
		}
		

		public String getChildConceptLocalID() {
			return childConceptLocalID;
		}

		public Integer getLevel() {
			return level;
		}

		public String getMappedConceptLocalID() {
			return mappedConceptLocalID;
		}

		public String getMappingType() {
			return mappingType;
		}
		
		public Float getScore() {
			return score;
		}
		
		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("ElementEntry: [");
			sb.append(this.localElementID);
			sb.append(", ");
			sb.append(this.localConceptID);
			sb.append(", ");
			sb.append(this.contextName);
			sb.append(", ");
			sb.append(this.childConceptLocalID);
			sb.append(", ");
			sb.append(this.level);
			sb.append(", ");
			sb.append(this.mappedConceptLocalID);
			sb.append(", ");
			sb.append(this.mappingType);
			sb.append(", ");			
			sb.append(this.score);		
			sb.append("]");
			return sb.toString();
		}
	}
	
	/**
	 * This class is a representation for a OBR_XX_EAT table entry.
	 * 
	 * @author Adrien Coulet
	 * @version OBR_v0.2		
	 * @created 12-Nov-2008
	 */
	static class ExpandedAnnotationEntry {

		private String localElementID;
		private String localConceptID;
		private String contextName;
		private String childConceptID;
		private Integer level;
		private String  mappedConceptID;
		private String  mappingType;
		private String  distantConceptID;
		private Integer distance;
		private Boolean indexingDone;
				
		public ExpandedAnnotationEntry(String localElementID,
				String localConceptID, String contextName,
				String childConceptID, Integer level, String mappedConceptID,
				String mappingType, String distantConceptID, Integer distance, Boolean indexingDone) {
			super();
			this.localElementID = localElementID;
			this.localConceptID = localConceptID;
			this.contextName = contextName;
			this.childConceptID = childConceptID;
			this.level = level;
			this.mappedConceptID = mappedConceptID;
			this.mappingType = mappingType;
			this.distantConceptID = distantConceptID;
			this.distance = distance;
			this.indexingDone = indexingDone;
		}

		public String getLocalElementID() {
			return localElementID;
		}

		public String getLocalConceptID() {
			return localConceptID;
		}

		public String getContextName() {
			return contextName;
		}

		public String getChildConceptID() {
			return childConceptID;
		}

		public Integer getLevel() {
			return level;
		}

		public String getMappedConceptID() {
			return mappedConceptID;
		}

		public String getMappingType() {
			return mappingType;
		}

		public String getDistantConceptID() {
			return distantConceptID;
		}

		public Integer getDistance() {
			return distance;
		}

		public Boolean getIndexingDone() {
			return indexingDone;
		}

		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("expandedAnnotationEntry: [");
			sb.append(this.localElementID);
			sb.append(", ");
			sb.append(this.localConceptID);
			sb.append(", ");
			sb.append(this.contextName);
			sb.append(", ");
			sb.append(this.childConceptID);
			sb.append(", ");
			sb.append(this.level);
			sb.append(", ");
			sb.append(this.mappedConceptID);
			sb.append(", ");
			sb.append(this.mappingType);
			sb.append(", ");
			sb.append(this.distantConceptID);
			sb.append(", ");
			sb.append(this.distance);
			sb.append(", ");
			sb.append(this.indexingDone);
			sb.append("]");
			return sb.toString();
		}
	}

}
