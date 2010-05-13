package org.ncbo.stanford.obr.dao.index;

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
public class IndexDao extends AbstractObrDao {

	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obr.index.table.suffix"); // element Index Table
	
	private PreparedStatement addEntryStatement;	 
	private PreparedStatement deleteEntriesFromOntologyStatement;
	 
	/**
	 * Creates a new IndexDao with a given resourceID.
	 * The suffix that will be added for AnnotationTable is "_index".
	 */
	public IndexDao(String resourceID) {
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
					"UNIQUE (element_id, concept_id), " +					 
					"INDEX X_" + this.getTableSQLName() +"_concept_id (concept_id) " +					 
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

	// ********************************* INDEXATION FUNCTIONS  *****************************************************/
	
	/**
	 * Index the content of _DAT and _EAT in the table by computing the right score.
	 * Returns the number of annotations added to the table. 
	 */
	public int indexation(ObrWeight weights){
		
		// Load obr_context table in memeory
		contextTableDao.loadTableIntoMemory();
		
		int nbAnnotation = 0;
		// Adds to _IT the direct annotations done with a term that is a preferredName.
		String query1 = indexationQueryForDirectAnnotations(weights);
		try{
			nbAnnotation = this.executeSQLUpdate(query1);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot index annotations from _DAT  ", e);
		}
		logger.info(nbAnnotation + " annotations indexed with direct annotations  .");
		
//		// Adds to _IT the direct annotations done with a term that is NOT preferredName.
//		String query2 = indexationQueryForMgrepAnnotations(false, weights);
//		try{
//			nbAnnotation = this.executeSQLUpdate(query2);
//			}
//		catch(SQLException e){
//			logger.error("** PROBLEM ** Cannot index annotations from _DAT (isPreferred=false)", e);
//		}
//		logger.info(nbAnnotation + " annotations indexed with direct annotations (isPreferred=false).");

		 
		// Adds to _IT the direct reported annotations.
//		String query3 = indexationQueryForReportedAnnotations(weights);
//		try{
//			nbAnnotation = this.executeSQLUpdate(query3);
//			}
//		catch(SQLException e){
//			logger.error("** PROBLEM ** Cannot index reported annotations from _DAT.", e);
//		}
//		logger.info(nbAnnotation + " annotations indexed with direct reported annotations.");

		// Switches the indexingDone flags on DAT
		StringBuffer updatingQueryb1 = new StringBuffer();
		updatingQueryb1.append("UPDATE ");
		updatingQueryb1.append(DirectAnnotationDao.name(this.resourceID));
		updatingQueryb1.append(" SET workflow_status = ");
		updatingQueryb1.append(WorkflowStatusEnum.INDEXING_DONE.getStatus());
		updatingQueryb1.append(" WHERE workflow_status = ");
		updatingQueryb1.append(WorkflowStatusEnum.MAPPING_DONE.getStatus());
		try{
			nbAnnotation = this.executeWithStoreProcedure(DirectAnnotationDao.name(this.resourceID), updatingQueryb1.toString(), true);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot switch indexingDone flags on _DAT.", e);
		}
		
		// Adds to _IT the isa expanded annotations.
		String query4 = indexationQueryForIsaExpandedAnnotations(weights);
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
			nbAnnotation = this.executeWithStoreProcedure(IsaExpandedAnnotationDao.name(this.resourceID), updatingQueryb2.toString(), true);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot switch indexingDone flags on _EAT.", e);
		}
		
		// Adds to _IT the mapping expanded annotations.
		String query5 = indexationQueryForMapExpandedAnnotations(weights);
		try{
			nbAnnotation = this.executeSQLUpdate(query5);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot index mapping expanded annotations from _EAT.", e);
		}
		logger.info(nbAnnotation + " annotations indexed with mapping expanded annotations.");
		
		// Switches the indexingDone flags on EAT
		StringBuffer updatingQueryb3 = new StringBuffer();
		updatingQueryb3.append("UPDATE ");
		updatingQueryb3.append(MapExpandedAnnotationDao.name(this.resourceID));
		updatingQueryb3.append(" SET workflow_status = ");
		updatingQueryb3.append(WorkflowStatusEnum.INDEXING_DONE.getStatus());
		updatingQueryb3.append(" WHERE workflow_status = ");
		updatingQueryb3.append(WorkflowStatusEnum.INDEXING_NOT_DONE.getStatus());
		try{
			nbAnnotation = this.executeWithStoreProcedure(MapExpandedAnnotationDao.name(this.resourceID), updatingQueryb3.toString(), true);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot switch indexingDone flags on _EAT.", e);
		}
		
		
		return this.numberOfEntry();
	}
	
	private String indexationQueryForDirectAnnotations(ObrWeight weights){
		/* INSERT INTO OBR_TR_IT (elementID, conceptID, score) 
		SELECT elementID, OBR_TR_DAT.conceptID, @s:=SUM(10*contextWeight)
			FROM OBR_TR_DAT, OBR_CXT, OBS_TT
			WHERE OBR_TR_DAT.contextID=OBR_CXT.contextID AND OBR_TR_DAT.termID=OBS_TT.termID
			AND isPreferred=true AND OBR_TR_DAT.termID IS NOT NULL AND indexingDone=false GROUP BY elementID, conceptID
		ON DUPLICATE KEY UPDATE score=score+@s;  */
	
		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO ");
		query.append(this.getTableSQLName());
		query.append(" (element_id, concept_id, score) SELECT element_id, DAT.concept_id, ");
		query.append("IF(DAT.term_id IS NULL, @s:=SUM(");
		query.append(weights.getReportedDA());
		query.append("*weight), ");
		query.append("IF(TT.is_preferred, @s:=SUM(");
		query.append(weights.getPreferredNameDA());	
		query.append("*weight), @s:=SUM(");
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
		query.append(" GROUP BY element_id, concept_id ON DUPLICATE KEY UPDATE score=score+@s;");
		return query.toString();
	}
	
	private String indexationQueryForMgrepAnnotations(ObrWeight weights){
		/* INSERT INTO OBR_TR_IT (elementID, conceptID, score) 
		SELECT elementID, OBR_TR_DAT.conceptID, @s:=SUM(10*contextWeight)
			FROM OBR_TR_DAT, OBR_CXT, OBS_TT
			WHERE OBR_TR_DAT.contextID=OBR_CXT.contextID AND OBR_TR_DAT.termID=OBS_TT.termID
			AND isPreferred=true AND OBR_TR_DAT.termID IS NOT NULL AND indexingDone=false GROUP BY elementID, conceptID
		ON DUPLICATE KEY UPDATE score=score+@s;  */
	
		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO ");
		query.append(this.getTableSQLName());
		query.append(" (element_id, concept_id, score) SELECT element_id, DAT.concept_id, ");
		query.append("IF(TT.is_preferred, @s:=SUM(");
		query.append(weights.getPreferredNameDA());	
		query.append("*weight), @s:=SUM(");
		query.append(weights.getSynonymDA());	
		query.append("*weight)) ");			 
		query.append(" score FROM ");
		query.append(DirectAnnotationDao.name(this.resourceID));
		query.append(" DAT, ");
		query.append(contextTableDao.getMemoryTableSQLName());
		query.append(" CXT, ");
		query.append(termDao.getMemoryTableSQLName());
		query.append(" TT WHERE DAT.context_id = CXT.id AND DAT.term_id= TT.id  AND workflow_status= ");
		query.append(WorkflowStatusEnum.MAPPING_DONE.getStatus());
		query.append(" GROUP BY element_id, concept_id ON DUPLICATE KEY UPDATE score=score+@s;");
		return query.toString();
	}
	
	private String indexationQueryForReportedAnnotations(ObrWeight weights){
		/* INSERT INTO OBR_TR_IT (elementID, conceptID, score) 
			SELECT elementID, conceptID, @s:=SUM(5*contextWeight)
    		FROM OBR_TR_DAT, OBR_CXT 
    		WHERE OBR_TR_DAT.contextID=OBR_CXT.contextID
    		AND termID IS NULL AND indexingDone=false GROUP BY elementID, conceptID ON DUPLICATE KEY UPDATE score=score+@s; */
		
		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO ");
		query.append(this.getTableSQLName());
		query.append(" (element_id, concept_id, score) SELECT element_id, DAT.concept_id, @s:=SUM(");
		query.append(weights.getReportedDA());
		query.append("*weight) FROM ");
		query.append(DirectAnnotationDao.name(this.resourceID));
		query.append(" DAT, ");
		query.append(contextTableDao.getMemoryTableSQLName());
		query.append(" CXT WHERE DAT.context_id = CXT.id AND term_id IS NULL AND workflow_status= ");
		query.append(WorkflowStatusEnum.MAPPING_DONE.getStatus());
		query.append(" GROUP BY element_id, concept_id ON DUPLICATE KEY UPDATE score=score+@s;");
		return query.toString();
	} 
	 
	private String indexationQueryForIsaExpandedAnnotations(ObrWeight weights){
		/*INSERT INTO OBR_TR_IT (elementID, conceptID, score) 
	    	SELECT elementID, OBR_TR_EAT.conceptID, @s:=SUM(5*contextWeight)
	        	FROM OBR_TR_EAT, OBR_CXT
	        	WHERE OBR_TR_EAT.contextID=OBR_CXT.contextID
	        	AND childConceptID IS NOT NULL AND indexingDone=false GROUP BY elementID, conceptID
			ON DUPLICATE KEY UPDATE score=score+@s; */
		
		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO ");
		query.append(this.getTableSQLName());
		query.append(" (element_id, concept_id, score) SELECT element_id, EAT.concept_id, @s:=SUM(");
		query.append("FLOOR(10*EXP(-").append(weights.getIsaFactor());
		query.append("* EAT.parent_level)+1)");			 
		query.append("*weight) FROM ");
		query.append(IsaExpandedAnnotationDao.name(this.resourceID));
		query.append(" EAT, ");
		query.append(contextTableDao.getMemoryTableSQLName());
		query.append(" CXT WHERE EAT.context_id= CXT.id AND workflow_status=");
		query.append(WorkflowStatusEnum.INDEXING_NOT_DONE.getStatus());
		query.append(" GROUP BY element_id, concept_id ON DUPLICATE KEY UPDATE score=score+@s;");
		
		return query.toString();
	}
	
	private String indexationQueryForMapExpandedAnnotations(ObrWeight weights){
		/*INSERT INTO OBR_TR_IT (elementID, conceptID, score) 
	    	SELECT elementID, OBR_TR_EAT.conceptID, @s:=SUM(5*contextWeight)
	        	FROM OBR_TR_EAT, OBR_CXT
	        	WHERE OBR_TR_EAT.contextID=OBR_CXT.contextID
	        	AND childConceptID IS NOT NULL AND indexingDone=false GROUP BY elementID, conceptID
			ON DUPLICATE KEY UPDATE score=score+@s; */
		
		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO ");
		query.append(this.getTableSQLName());
		query.append(" (element_id, concept_id, score) SELECT element_id, EAT.concept_id, @s:=SUM(");
		query.append(weights.getMappingEA());
		query.append("*weight) FROM ");
		query.append(MapExpandedAnnotationDao.name(this.resourceID));
		query.append(" EAT, ");
		query.append(contextTableDao.getMemoryTableSQLName());
		query.append(" CXT WHERE EAT.context_id= CXT.id AND workflow_status=");
		query.append(WorkflowStatusEnum.INDEXING_NOT_DONE.getStatus());
		query.append(" GROUP BY element_id, concept_id ON DUPLICATE KEY UPDATE score=score+@s;");
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
