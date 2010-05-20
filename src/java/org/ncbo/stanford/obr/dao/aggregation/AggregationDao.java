package org.ncbo.stanford.obr.dao.aggregation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import obs.common.beans.DictionaryBean;
import obs.obr.populate.ObrWeight;

import org.ncbo.stanford.obr.dao.AbstractObrDao;
import org.ncbo.stanford.obr.dao.annotation.DirectAnnotationDao;
import org.ncbo.stanford.obr.dao.annotation.expanded.IsaExpandedAnnotationDao;
import org.ncbo.stanford.obr.dao.annotation.expanded.MapExpandedAnnotationDao;
import org.ncbo.stanford.obr.dao.element.ElementDao;
import org.ncbo.stanford.obr.dao.obs.concept.ConceptDao;
import org.ncbo.stanford.obr.dao.obs.ontology.OntologyDao;
import org.ncbo.stanford.obr.enumeration.WorkflowStatusEnum;
import org.ncbo.stanford.obr.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
 * This class is a representation for the the OBR DB OBR_XX_EAT table. The table contains 
 * the following columns:
 * 
 * <ul>
 * <li> id 			            	BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 * <li> element_id  	            			INT UNSIGNED NOT NULL FOREIGN KEY,
 * <li> concept_id							INT UNSIGNED NOT NULL FOREIGN KEY,
 * <li> score  						        FLOAT,
 * </ul>
 *  
 * @author Adrien Coulet, Clement Jonquet
 * @version OBR_v0.2		
 * @created 12-Nov-2008
 *
 */
public class AggregationDao extends AbstractObrDao {

	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obr.aggregation.table.suffix"); // element Index Table
	
	private PreparedStatement addEntryStatement;	 
	private PreparedStatement deleteEntriesFromOntologyStatement;
	 
	/**
	 * Creates a new IndexDao with a given resourceID.
	 * The suffix that will be added for AnnotationTable is "_index".
	 */
	public AggregationDao(String resourceID) {
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
		return "CREATE TABLE " + getTableSQLName() +" (" +
					"element_id INT UNSIGNED NOT NULL, " +
					"concept_id INT UNSIGNED NOT NULL, " +
					"score FLOAT, " +
					"UNIQUE element_id(element_id, concept_id), " +					 
					"INDEX IDX_" + this.getTableSQLName() +"_score(score) " +					 
				")ENGINE=MyISAM DEFAULT CHARSET=latin1; ;";
	}
	
	protected String tempTableCreationQuery(){
		return "CREATE TABLE " + getTempTableSQLName() +" (" +
					"element_id INT UNSIGNED NOT NULL, " +
					"concept_id INT UNSIGNED NOT NULL, " +
					"score FLOAT" +								 			 
				")ENGINE=MyISAM DEFAULT CHARSET=latin1; ;";
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
		queryb.append(" (element_id, concept_id, score) VALUES (");
			// sub query to get the elementID from the localElementID
			queryb.append("(SELECT id FROM ");
			queryb.append(ElementDao.name(this.resourceID));
			queryb.append(" WHERE local_element_id=?)");
		queryb.append("	,");
			// sub query to get the conceptID from the localConceptID
			queryb.append("(SELECT id FROM ");
			queryb.append(conceptDao.getTableSQLName());
			queryb.append(" WHERE local_concept_id=?)");
		queryb.append("	,?);");
		this.addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}

	/**
	 * Add an new entry in corresponding SQL table.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addEntry(IndexAnnotation entry){
		boolean inserted = false;
		try {
			this.addEntryStatement.setString(1, entry.getLocalElementID());
			this.addEntryStatement.setString(2, entry.getLocalConceptID());
			this.addEntryStatement.setFloat(3, entry.getScore());
			this.executeSQLUpdate(this.addEntryStatement);
			inserted = true;
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openAddEntryStatement();
			return this.addEntry(entry);
		}
		catch (MySQLIntegrityConstraintViolationException e){
			//logger.info("Table " + this.getTableSQLName() + " already contains an entry for the concept: " + entry.getLocalConceptID() +".");
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot add an entry on table " + this.getTableSQLName(), e);
			logger.error(entry.toString());
		}
		return inserted;	
	}

	// ********************************* AGGREGATION FUNCTIONS  *****************************************************/
	
	/**
	 * Index the content of _DAT and _EAT in the table by computing the right score.
	 * Returns the number of annotations added to the table. 
	 */
	public int aggregation(ObrWeight weights){
		
		// Load obr_context table in memeory
		contextTableDao.loadTableIntoMemory();
		// Create temp table.
		try{
			this.executeSQLUpdate(tempTableCreationQuery());
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot create temporary table " + getTempTableSQLName(), e);
		}
		
		int nbAnnotation = 0;
		// Adds to _IT the direct annotations done with a term that is a preferredName.
		String query1 = aggregationQueryForDirectAnnotations(weights);
		try{
			nbAnnotation = this.executeSQLUpdate(query1);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot index annotations from _DAT  ", e);
		}
		logger.info(nbAnnotation + " annotations indexed with direct annotations  .");

		// Switches the workflow_status flags on DAT
		StringBuffer updatingQueryb1 = new StringBuffer();
		updatingQueryb1.append("UPDATE ");
		updatingQueryb1.append(DirectAnnotationDao.name(this.resourceID));
		updatingQueryb1.append(" SET workflow_status = ");
		updatingQueryb1.append(WorkflowStatusEnum.INDEXING_DONE.getStatus());
		updatingQueryb1.append(" WHERE workflow_status = ");
		updatingQueryb1.append(WorkflowStatusEnum.MAPPING_DONE.getStatus());
		try{			 
			nbAnnotation = this.executeSQLUpdate(updatingQueryb1.toString());
			logger.info("workflow_status updated to "+ WorkflowStatusEnum.INDEXING_DONE.getStatus()+ " in table " + DirectAnnotationDao.name(this.resourceID));
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot switch workflow_status flags on DAT.", e);
		}
		
		// Adds to _IT the isa expanded annotations.
		String query4 = aggregationQueryForIsaExpandedAnnotations(weights);
		try{
			nbAnnotation = this.executeSQLUpdate(query4);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot index isa expanded annotations from _EAT.", e);
		}
		logger.info(nbAnnotation + " annotations indexed with isa expanded annotations.");

		// Switches the indexingDone flags on EAT
		StringBuffer updatingQueryb2 = new StringBuffer();
		updatingQueryb2.append("UPDATE ");
		updatingQueryb2.append(IsaExpandedAnnotationDao.name(this.resourceID));
		updatingQueryb2.append(" SET workflow_status = ");
		updatingQueryb2.append(WorkflowStatusEnum.INDEXING_DONE.getStatus());
		updatingQueryb2.append(" WHERE workflow_status = ");
		updatingQueryb2.append(WorkflowStatusEnum.INDEXING_NOT_DONE.getStatus());
		try{
			 nbAnnotation = this.executeSQLUpdate(updatingQueryb2.toString());
			 logger.info("workflow_status updated to "+ WorkflowStatusEnum.INDEXING_DONE.getStatus()+ " in table " + IsaExpandedAnnotationDao.name(this.resourceID));
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot switch indexingDone flags on _EAT.", e);
		}
		
		// Adds to _IT the mapping expanded annotations.
		String query5 = aggregationQueryForMapExpandedAnnotations(weights);
		try{
			nbAnnotation = this.executeSQLUpdate(query5);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot index mapping expanded annotations from _EAT.", e);
		}
		logger.info(nbAnnotation + " annotations indexed with mapping expanded annotations.");
		
		// Switches the workflow_status flags on mapping annotation
		StringBuffer updatingQueryb3 = new StringBuffer();
		updatingQueryb3.append("UPDATE ");
		updatingQueryb3.append(MapExpandedAnnotationDao.name(this.resourceID));
		updatingQueryb3.append(" SET workflow_status = ");
		updatingQueryb3.append(WorkflowStatusEnum.INDEXING_DONE.getStatus());
		updatingQueryb3.append(" WHERE workflow_status = ");
		updatingQueryb3.append(WorkflowStatusEnum.INDEXING_NOT_DONE.getStatus());
		try{
			 nbAnnotation = this.executeSQLUpdate(updatingQueryb3.toString());
			 logger.info("workflow_status updated to "+ WorkflowStatusEnum.INDEXING_DONE.getStatus()+ " in table " + MapExpandedAnnotationDao.name(this.resourceID));
		  }
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot switch workflow_status flags on " + MapExpandedAnnotationDao.name(this.resourceID), e);
		}
		
//		String query6 = "CREATE INDEX "+getTempTableSQLName()+"_elt_cpt ON "+ getTempTableSQLName()+ "(element_id, concept_id); ";
//		try{
//			nbAnnotation = this.executeSQLUpdate(query6);
//			}
//		catch(SQLException e){
//			logger.error("** PROBLEM ** Cannot index mapping expanded annotations from _EAT.", e);
//		}
//		logger.info("Index "+getTempTableSQLName()+"_elt_cpt added to temp table "+ getTempTableSQLName());
		
		// Populate Aggregation table  from temp table
		String query7 = populateAggregationTableFromTempTable();
		try{
			nbAnnotation = this.executeSQLUpdate(query7);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot index mapping expanded annotations from _EAT.", e);
		}
		logger.info(nbAnnotation + " entries updated in aggrigation table form temp table.");
		
		// Deletes the temporary table
		StringBuffer deleteQuery = new StringBuffer();
		deleteQuery.append("DROP TABLE ");
		deleteQuery.append(this.getTempTableSQLName());
		deleteQuery.append(";");
		try{
			this.executeSQLUpdate(deleteQuery.toString());
			logger.info("Dropped temp table : " + this.getTempTableSQLName());
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot delete the temporary table.", e);
		} 
		
		return this.numberOfEntry();
	}
	
	private String populateAggregationTableFromTempTable() {
		
		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO ");
		query.append(this.getTableSQLName());
		query.append(" (element_id, concept_id, score) SELECT element_id,  concept_id, SUM(score) FROM ");
		query.append(getTempTableSQLName()); 
		query.append(" GROUP BY element_id, concept_id;");
		
		return query.toString();
	}

	private String aggregationQueryForDirectAnnotations(ObrWeight weights){
		 
	
//		StringBuffer query = new StringBuffer();
//		query.append("INSERT INTO ");
//		query.append(this.getTempTableSQLName());
//		query.append(" (element_id, concept_id, score) SELECT element_id, DAT.concept_id, ");
//		query.append("IF(DAT.term_id IS NULL, @s:=SUM(");
//		query.append(weights.getReportedDA());
//		query.append("*weight), ");
//		query.append("IF(TT.is_preferred, @s:=SUM(");
//		query.append(weights.getPreferredNameDA());	
//		query.append("*weight), @s:=SUM(");
//		query.append(weights.getSynonymDA());	
//		query.append("*weight))");
//		query.append(") calc_score FROM ");
//		query.append(DirectAnnotationDao.name(this.resourceID));
//		query.append(" DAT LEFT JOIN ");
//		query.append(termDao.getMemoryTableSQLName());
//		query.append(" TT ON DAT.term_id= TT.id, ");
//		query.append(contextTableDao.getMemoryTableSQLName());
//		query.append(" CXT WHERE DAT.context_id = CXT.id AND workflow_status= ");
//		query.append(WorkflowStatusEnum.MAPPING_DONE.getStatus());
//		query.append(" GROUP BY element_id, concept_id ON DUPLICATE KEY UPDATE score=score+@s;");
		

		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO ");
		query.append(this.getTempTableSQLName());
		query.append(" (element_id, concept_id, score) SELECT element_id, DAT.concept_id, ");
		query.append("IF(DAT.term_id IS NULL, (");
		query.append(weights.getReportedDA());
		query.append("*weight), ");
		query.append("IF(TT.is_preferred,(");
		query.append(weights.getPreferredNameDA());	
		query.append("*weight),(");
		query.append(weights.getSynonymDA());	
		query.append("*weight))");
		query.append(") calc_score FROM ");
		query.append(DirectAnnotationDao.name(this.resourceID));
		query.append(" DAT LEFT JOIN ");
		query.append(termDao.getMemoryTableSQLName());
		query.append(" TT ON DAT.term_id= TT.id, ");
		query.append(contextTableDao.getMemoryTableSQLName());
		query.append(" CXT WHERE DAT.context_id = CXT.id AND workflow_status= ");
		query.append(WorkflowStatusEnum.MAPPING_DONE.getStatus());
		query.append(";");
		return query.toString();
	} 
	 
	private String aggregationQueryForIsaExpandedAnnotations(ObrWeight weights){
		/*INSERT INTO OBR_TR_IT (elementID, conceptID, score) 
	    	SELECT elementID, OBR_TR_EAT.conceptID, @s:=SUM(5*contextWeight)
	        	FROM OBR_TR_EAT, OBR_CXT
	        	WHERE OBR_TR_EAT.contextID=OBR_CXT.contextID
	        	AND childConceptID IS NOT NULL AND indexingDone=false GROUP BY elementID, conceptID
			ON DUPLICATE KEY UPDATE score=score+@s; */
		
//		StringBuffer query = new StringBuffer();
//		query.append("INSERT INTO ");
//		query.append(this.getTempTableSQLName());
//		query.append(" (element_id, concept_id, score) SELECT element_id, EAT.concept_id, @s:=SUM(");
//		query.append("FLOOR(10*EXP(-").append(weights.getIsaFactor());
//		query.append("* EAT.parent_level)+1)");			 
//		query.append("*weight) FROM ");
//		query.append(IsaExpandedAnnotationDao.name(this.resourceID));
//		query.append(" EAT, ");
//		query.append(contextTableDao.getMemoryTableSQLName());
//		query.append(" CXT WHERE EAT.context_id= CXT.id AND workflow_status=");
//		query.append(WorkflowStatusEnum.INDEXING_NOT_DONE.getStatus());
//		query.append(" GROUP BY element_id, concept_id ON DUPLICATE KEY UPDATE score=score+@s;");
		
		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO ");
		query.append(this.getTempTableSQLName());
		query.append(" (element_id, concept_id, score) SELECT element_id, EAT.concept_id, (");
		query.append("FLOOR(10*EXP(-").append(weights.getIsaFactor());
		query.append("* EAT.parent_level)+1)");			 
		query.append("*weight) FROM ");
		query.append(IsaExpandedAnnotationDao.name(this.resourceID));
		query.append(" EAT, ");
		query.append(contextTableDao.getMemoryTableSQLName());
		query.append(" CXT WHERE EAT.context_id= CXT.id AND workflow_status=");
		query.append(WorkflowStatusEnum.INDEXING_NOT_DONE.getStatus());
		query.append(";");
		
		return query.toString();
	}
	
	private String aggregationQueryForMapExpandedAnnotations(ObrWeight weights){
		/*INSERT INTO OBR_TR_IT (elementID, conceptID, score) 
	    	SELECT elementID, OBR_TR_EAT.conceptID, @s:=SUM(5*contextWeight)
	        	FROM OBR_TR_EAT, OBR_CXT
	        	WHERE OBR_TR_EAT.contextID=OBR_CXT.contextID
	        	AND childConceptID IS NOT NULL AND indexingDone=false GROUP BY elementID, conceptID
			ON DUPLICATE KEY UPDATE score=score+@s; */
		
		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO ");
		query.append(this.getTempTableSQLName());
		query.append(" (element_id, concept_id, score) SELECT element_id, EAT.concept_id, (");
		query.append(weights.getMappingEA());
		query.append("*weight) FROM ");
		query.append(MapExpandedAnnotationDao.name(this.resourceID));
		query.append(" EAT, ");
		query.append(contextTableDao.getMemoryTableSQLName());
		query.append(" CXT WHERE EAT.context_id= CXT.id AND workflow_status=");
		query.append(WorkflowStatusEnum.INDEXING_NOT_DONE.getStatus());
		query.append(";");
		return query.toString();
	}
		
	//********************************* DELETE FUNCTIONS *****************************************************/
	  
	private void openDeleteEntriesFromOntologyStatement(){	 
		// Query Used :
		//	DELETE IT FROM obr_tr_index IT, obs_concept CT, obs_ontology OT
		//		WHERE IT.conept_id = CT.id
		//			AND CT.ontology_id = OT.id
		//			AND OT.local_ontology_id = ?;		
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE IT FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" IT, ");
		queryb.append(ConceptDao.name( ));	
		queryb.append(" CT, ");
		queryb.append(OntologyDao.name());
		queryb.append(" OT ");
		queryb.append(" WHERE IT.concept_id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id = ?");

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
	 * Deletes the rows for indexing done with a concept in the given list of localOntologyIDs.
	 * 
	 * @param {@code List} of local ontology ids
	 * @return True if the rows were successfully removed. 
	 */
	public boolean deleteEntriesFromOntologies(List<String> localOntologyIDs){		
		boolean deleted = false;
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE IT FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" IT, ");
		queryb.append(ConceptDao.name( ));	
		queryb.append(" CT, ");
		queryb.append(OntologyDao.name());
		queryb.append(" OT ");
		queryb.append(" WHERE IT.concept_id = CT.id AND CT.ontology_id = OT.id AND OT.local_ontology_id IN (");
		
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
	
	//**********************************Statistics Method****************
	 
	/**
	 * This method gives number of indexed annotations for each ontologyID 
	 * @param dictionary 
	 * @param withCompleteDictionary 
	 *  
	 * @return map containing number of indexed annotations for each ontologyID as key.
	 */
	public HashMap<Integer, Integer> getIndexedAnnotationStatistics(boolean withCompleteDictionary, DictionaryBean dictionary){
		HashMap<Integer, Integer> annotationStats = new HashMap<Integer, Integer>();
		
		StringBuffer queryb = new StringBuffer();
		if(withCompleteDictionary){
			queryb.append("SELECT CT.ontology_id, COUNT(IT.concept_id) AS COUNT FROM ");
			queryb.append(this.getTableSQLName());		 	 
			queryb.append(" AS IT, ");
			queryb.append(conceptDao.getTableSQLName());
			queryb.append(" AS CT WHERE IT.concept_id=CT.id GROUP BY CT.ontology_id; "); 
		}else{
			queryb.append("SELECT OT.id, COUNT(IT.concept_id) AS COUNT FROM ");
			queryb.append(this.getTableSQLName());		 	 
			queryb.append(" AS IT, ");
			queryb.append(conceptDao.getTableSQLName());
			queryb.append(" AS CT, ");
			queryb.append(ontologyDao.getTableSQLName());
			queryb.append(" AS OT WHERE IT.concept_id=CT.id AND CT.ontology_id=OT.id AND OT.dictionary_id = ");
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
			return this.getIndexedAnnotationStatistics(withCompleteDictionary, dictionary);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get indexed annotations statistics from "+this.getTableSQLName()+" .", e);
		}
		return annotationStats;
		 
	}
		
	// ********************************* ENTRY CLASS *****************************************************/

	/**
	 * This class is a representation for an annotation in OBR_XX_IT table.
	 * 
	 * @author Adrien Coulet, Clement Jonquet
	 * @version OBR_v0.2		
	 * @created 13-Nov-2008
	 */
	static class IndexAnnotation {

		private String localElementID;
		private String localConceptID;
		private float score;
		
		public IndexAnnotation(String localElementID, String localConceptID, float score) {
			super();
			this.localElementID = localElementID;
			this.localConceptID = localConceptID;
			this.score = score;
		}

		public String getLocalElementID() {
			return localElementID;
		}

		public String getLocalConceptID() {
			return localConceptID;
		}
		
		public float getScore() {
			return score;
		}
		
		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("ElementEntry: [");
			sb.append(this.localElementID);
			sb.append(", ");
			sb.append(this.localConceptID);
			sb.append(", ");			
			sb.append(this.score);			
			sb.append("]");
			return sb.toString();
		}
	}
	
	/**
	 * This class is a representation for a OBR_XX_IT table entry.
	 * 
	 * @author Adrien Coulet, Clement Jonquet
	 * @version OBR_v0.2		
	 * @created 12-Nov-2008

	 */
	public static class IndexEntry {

		private Integer elementID;
		private Integer conceptID;
		private float score;
		
		public IndexEntry(Integer elementID, Integer conceptID, float score) {
			super();
			this.elementID = elementID;
			this.conceptID = conceptID;
			this.score = score;
		}

		public Integer getElementID() {
			return elementID;
		}

		public Integer getConceptID() {
			return conceptID;
		}
		
		public float getScore() {
			return score;
		}
		
		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("IndexEntry: [");
			sb.append(this.elementID);
			sb.append(", ");
			sb.append(this.conceptID);
			sb.append(", ");			
			sb.append(this.score);			
			sb.append("]");
			return sb.toString();
		}
	}
}
