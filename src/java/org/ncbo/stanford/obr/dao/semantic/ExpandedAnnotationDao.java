package org.ncbo.stanford.obr.dao.semantic;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.ncbo.stanford.obr.dao.AbstractObrDao;
import org.ncbo.stanford.obr.dao.annoation.DirectAnnotationDao;
import org.ncbo.stanford.obr.dao.element.ElementDao;
import org.ncbo.stanford.obr.enumeration.ObsSchemaEnum;
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

	private static final String TABLE_SUFIX = MessageUtils.getMessage("obr.expanded.annotation.table.suffix");
	
	private PreparedStatement addEntryStatement; 
	private PreparedStatement deleteEntriesFromOntologyStatement;
	
	/**
	 * Creates a new ExpandedAnnotationTable with a given resourceID.
	 * The suffix that will be added for AnnotationTable is "_EAT".
	 */
	public ExpandedAnnotationDao(String resourceID) {
		super(resourceID, TABLE_SUFIX);
	}

	/**
	 * Returns the SQL table name for a given resourceID 
	 */
	public static String name(String resourceID){
		return OBR_PREFIX + resourceID.toLowerCase() + TABLE_SUFIX;
	}
	
	@Override
	protected String creationQuery(){
		//logger.info("creation of the table "+ this.getTableSQLName());
		return "CREATE TABLE " + getTableSQLName() +" (" +
					"id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"element_id INT UNSIGNED NOT NULL, " +
					"concept_id INT UNSIGNED NOT NULL, " +			
					"context_id SMALLINT UNSIGNED NOT NULL, " +			
					"child_concept_id INT UNSIGNED, " +
					"parent_level SMALLINT UNSIGNED, " +
					"mapped_concept_id INT UNSIGNED, " +
					"mapping_type VARCHAR(20), " +
					"distant_concept_id INT UNSIGNED, " +
					"distance SMALLINT UNSIGNED, " +
					"indexing_done BOOL NOT NULL, " +
					// Removed march 2009. Valid but too expensive in size. Not verified.
					//"UNIQUE (elementID, conceptID, contextID, childConceptID, mappedConceptID, distantConceptID), " +				
					"FOREIGN KEY (element_id) REFERENCES "         + ElementDao.name(this.resourceID)  + "(elementID) ON DELETE CASCADE ON UPDATE CASCADE, " +
					"FOREIGN KEY (concept_id) REFERENCES "         + ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName() 		+ "(conceptID) ON DELETE CASCADE ON UPDATE CASCADE, " +
					"FOREIGN KEY (context_id) REFERENCES "         + contextTableDao.getTableSQLName()			 		+ "(contextID) ON DELETE CASCADE ON UPDATE CASCADE, " +
					"FOREIGN KEY (child_concept_id) REFERENCES "    + ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName() 		+ "(conceptID) ON DELETE CASCADE ON UPDATE CASCADE, " +
					"FOREIGN KEY (mapped_concept_id) REFERENCES "   + ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName() 		+ "(conceptID) ON DELETE CASCADE ON UPDATE CASCADE, " +
					"FOREIGN KEY (distant_concept_id) REFERENCES "  + ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName() 		+ "(conceptID) ON DELETE CASCADE ON UPDATE CASCADE, " +
					"INDEX X_" + this.getTableSQLName() +"parent_level (parent_level), " +
					"INDEX X_" + this.getTableSQLName() +"mapping_type (mapping_type), " +
					"INDEX X_" + this.getTableSQLName() +"distance (distance)," +
					"INDEX X_" + this.getTableSQLName() +"indexing_done (indexing_done)" +
				");";
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
			queryb.append(ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName());
			queryb.append(" WHERE local_concept_id=?)");
		queryb.append("	,");
			// sub query to get the contextID from the contextName
			queryb.append("(SELECT id FROM ");
			queryb.append(contextTableDao.getTableSQLName());
			queryb.append(" WHERE name=?)");
		queryb.append("	,");
			// sub query to get the conceptID from the childConceptID
			queryb.append("(SELECT id FROM ");
			queryb.append(ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName());
			queryb.append(" WHERE local_concept_id=?)");
		queryb.append("	,?,"); //level
			// sub query to get the conceptID from the mappedConceptID
			queryb.append("(SELECT id FROM ");
			queryb.append(ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName());
			queryb.append(" WHERE local_concept_id=?)");
		queryb.append("	,?,"); //mappingType
			// sub query to get the conceptID from the distantConceptID
			queryb.append("(SELECT id FROM ");
			queryb.append(ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName());
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
	 * Returns the number of isaClosure annotations created in the corresponding _EAT.
	 */
	public int isaClosureExpansion(DirectAnnotationDao table){
		int nbAnnotation;
		/* INSERT OBR_TR_EAT (elementID, conceptID, contextID, childConceptID, level, indexingDone) 
		SELECT elementID, CT2.conceptID, contextID, DAT.conceptID, level, false
			FROM OBR_TR_DAT AS DAT, OBS_CT AS CT1, OBS_CT AS CT2, OBS_ISAPT AS ISAPT
				WHERE CT2.localConceptID = ISAPT.parentLocalConceptID
				AND CT1.localConceptID = ISAPT.localConceptID
				AND OBR_TR_DAT.conceptID = CT1.conceptID AND isaClosureDone = false; */

		//TODO : check that discussion with Nigam sept 2009
		/* INSERT OBR_TR_EAT (elementID, conceptID, contextID, childConceptID, level, indexingDone) 
		SELECT elementID, CT2.conceptID, contextID, DAT.conceptID, level, false
			FROM OBR_TR_DAT AS DAT, OBS_CT AS CT1, OBS_CT AS CT2, OBS_ISAPT AS ISAPT
				WHERE CT2.conceptID = ISAPT.parentConceptID
				AND OBR_TR_DAT.conceptID = ISAPT.conceptID AND isaClosureDone = false; */

		
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (element_id, concept_id, context_id, child_concept_id, parent_level, indexing_done) SELECT element_id, CT.id, context_id, DAT.concept_id, level, false FROM ");
		queryb.append(table.getTableSQLName());
		queryb.append(" AS DAT, ");
		queryb.append(ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName());
		queryb.append(" AS CT, ");		 
		queryb.append(ObsSchemaEnum.IS_A_PARENT_TABLE.getTableSQLName());
		queryb.append(" AS ISAPT WHERE CT.id = ISAPT.parent_concept_id AND DAT.concept_id = ISAPT.concept_id AND is_a_closure_done = false;");
		
		StringBuffer updatingQueryb = new StringBuffer();
		updatingQueryb.append("UPDATE ");
		updatingQueryb.append(table.getTableSQLName());
		updatingQueryb.append(" SET is_a_closure_done=true WHERE is_a_closure_done=false;");
		
		try{
			nbAnnotation = this.executeSQLUpdate(queryb.toString());
			this.executeSQLUpdate(updatingQueryb.toString());
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
	 * Returns the number of mapping annotations created in the corresponding _EAT.
	 */
	public int mappingExpansion(DirectAnnotationDao table){
		int nbAnnotation;
		/* INSERT OBR_TR_EAT (elementID, conceptID, contextID, mappedConceptID, mappingType, indexingDone) 
				SELECT elementID, CT2.conceptID, contextID, MAPT.conceptID, mappingType, false
    				FROM OBR_TR_DAT AS DAT, OBS_CT AS CT1, OBS_CT AS CT2, OBS_MAPT AS MAPT
    					WHERE CT2.localConceptID = MAPT.mappedLocalConceptID
    					AND CT1.localConceptID = MAPT.localConceptID
    					AND OBR_TR_DAT.conceptID = CT1.conceptID AND mappingDone = false; */		
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (element_id, concept_id, context_id, mapped_concept_id, mapping_type, indexing_done) SELECT element_id, CT.id, context_id, DAT.concept_id, mapping_type, false FROM ");
		queryb.append(table.getTableSQLName());
		queryb.append(" AS DAT, ");
		queryb.append(ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName());
		queryb.append(" AS CT, ");	 
		queryb.append(ObsSchemaEnum.MAPPING_TABLE.getTableSQLName());
		queryb.append(" AS MAPT WHERE CT.id = MAPT.mapped_concept_id AND DAT.concept_id = MAPT.concept_id AND mapping_done = false;");
		
		StringBuffer updatingQueryb = new StringBuffer();
		updatingQueryb.append("UPDATE ");
		updatingQueryb.append(table.getTableSQLName());
		updatingQueryb.append(" SET mapping_done=true WHERE mapping_done=false;");
		
		try{
			nbAnnotation = this.executeSQLUpdate(queryb.toString());
			this.executeSQLUpdate(updatingQueryb.toString());
		}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot execute the isa transitive closure on table " + this.getTableSQLName() +". 0 returned", e);
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
		/*DELETE OBR_EAT FROM OBR_EAT WHERE OBR_EAT.conceptID IN (
			SELECT OBS_CT.conceptID FROM OBS_CT
				WHERE OBS_CT.ontologyID= (SELECT OBS_OT.ontologyID from OBS_OT
					WHERE OBS_OT.localOntologyID=?)); */
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" WHERE ");
		queryb.append(this.getTableSQLName());
		queryb.append(".concept_id IN ( ");
		queryb.append("SELECT ");
		queryb.append(ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName());
		queryb.append(".id ");
		queryb.append("FROM ");
		queryb.append(ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName());
		queryb.append(" WHERE ");
		queryb.append(ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName());
		queryb.append(".ontology_id= ( ");
		queryb.append("SELECT ");
		queryb.append(ObsSchemaEnum.ONTOLOGY_TABLE.getTableSQLName());
		queryb.append(".id ");
		queryb.append("FROM ");
		queryb.append(ObsSchemaEnum.ONTOLOGY_TABLE.getTableSQLName());
		queryb.append(" WHERE ");
		queryb.append(ObsSchemaEnum.ONTOLOGY_TABLE.getTableSQLName());
		queryb.append(".local_ontology_id=? ))");
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
	
	//**********************Annotations Statistics ******************/
	
	/**
	 * 
	 *  Get number of IS A Annotations for each ontlogyID
	 *  
	 *  @return HashMap<Integer, Integer>
	 */
	public HashMap<Integer, Integer> getISAAnnotationStatistics(){
		HashMap<Integer, Integer> annotationStats = new HashMap<Integer, Integer>();
		
		StringBuffer queryb = new StringBuffer();		 
		queryb.append("SELECT OT.id, COUNT(EAT.id) AS COUNT FROM ");
		queryb.append(this.getTableSQLName());		 	 
		queryb.append(" AS EAT, ");
		queryb.append(ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName());
		queryb.append(" AS CT, ");
		queryb.append(ObsSchemaEnum.ONTOLOGY_TABLE.getTableSQLName());
		queryb.append(" AS OT WHERE EAT.concept_id = CT.id AND CT.ontology_id=OT.id AND EAT.child_concept_id IS NOT NULL GROUP BY OT.id; ");
		
		try {			 			
			ResultSet rSet = this.executeSQLQuery(queryb.toString());
			while(rSet.next()){
				annotationStats.put(rSet.getInt(1), rSet.getInt(2));
			}			
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {			 
			return this.getISAAnnotationStatistics();
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get IS A annotations statistics from "+this.getTableSQLName()+" .", e);
		}
		return annotationStats;
		 
	}
	
	
	/**
	 * 
	 *  Get number of Mapping Annotations for each ontlogyID
	 *  
	 *  @return HashMap<Integer, Integer>
	 */
	public HashMap<Integer, Integer> getMappingAnnotationStatistics(){
		HashMap<Integer, Integer> annotationStats = new HashMap<Integer, Integer>();
		
		StringBuffer queryb = new StringBuffer();		 
		queryb.append("SELECT OT.id, COUNT(EAT.id) AS COUNT FROM ");
		queryb.append(this.getTableSQLName());		 	 
		queryb.append(" AS EAT, ");
		queryb.append(ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName());
		queryb.append(" AS CT, ");
		queryb.append(ObsSchemaEnum.ONTOLOGY_TABLE.getTableSQLName());
		queryb.append(" AS OT WHERE EAT.concept_id = CT.id AND CT.ontology_id=OT.id AND EAT.mapped_concept_id IS NOT NULL GROUP BY OT.id; ");
		
		try {			 			
			ResultSet rSet = this.executeSQLQuery(queryb.toString());
			while(rSet.next()){
				annotationStats.put(rSet.getInt(1), rSet.getInt(2));
			}			
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {			 
			return this.getMappingAnnotationStatistics();
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get mapping annotations statistics from " + this.getTableSQLName()+ " .", e);
		}
		return annotationStats;
		 
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
