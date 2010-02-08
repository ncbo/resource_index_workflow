package org.ncbo.stanford.obr.dao.index;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import obs.obr.populate.ObrWeight;

import org.ncbo.stanford.obr.dao.AbstractObrDao;
import org.ncbo.stanford.obr.dao.annoation.DirectAnnotationDao;
import org.ncbo.stanford.obr.dao.element.ElementDao;
import org.ncbo.stanford.obr.dao.semantic.ExpandedAnnotationDao;
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
	private PreparedStatement getIndexedAnnotationStatsStatement;
	
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
					"id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"element_id INT UNSIGNED NOT NULL, " +
					"concept_id INT UNSIGNED NOT NULL, " +
					"score FLOAT, " +
					"UNIQUE (element_id, concept_id), " +
					"FOREIGN KEY (element_id) REFERENCES " + ElementDao.name(this.resourceID)  + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
					"FOREIGN KEY (concept_id) REFERENCES " + conceptDao.getTableSQLName() 		+ "(id) ON DELETE CASCADE ON UPDATE CASCADE" +												
				");";
	}
	
	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();
		this.openAddEntryStatement();		 
		this.openDeleteEntriesFromOntologyStatement();
		this.openGetIndexedAnnotationStatsStatement();
	}

	@Override
	protected void closePreparedStatements() throws SQLException {
		super.closePreparedStatements();
		this.addEntryStatement.close();		 
		this.deleteEntriesFromOntologyStatement.close();
		this.getIndexedAnnotationStatsStatement.close();
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
		int nbAnnotation = 0;
		// Adds to _IT the direct annotations done with a term that is a preferredName.
		String query1 = indexationQueryForMgrepAnnotations(true, weights);
		try{
			nbAnnotation = this.executeSQLUpdate(query1);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot index annotations from _DAT (isPreferred=true)", e);
		}
		logger.info(nbAnnotation + " annotations indexed with direct annotations (isPreferred=true).");
		
		// Adds to _IT the direct annotations done with a term that is NOT preferredName.
		String query2 = indexationQueryForMgrepAnnotations(false, weights);
		try{
			nbAnnotation = this.executeSQLUpdate(query2);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot index annotations from _DAT (isPreferred=false)", e);
		}
		logger.info(nbAnnotation + " annotations indexed with direct annotations (isPreferred=false).");

		// Adds to _IT the direct reported annotations.
		String query3 = indexationQueryForReportedAnnotations(weights);
		try{
			nbAnnotation = this.executeSQLUpdate(query3);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot index reported annotations from _DAT.", e);
		}
		logger.info(nbAnnotation + " annotations indexed with direct reported annotations.");

		// Switches the indexingDone flags on DAT
		StringBuffer updatingQueryb1 = new StringBuffer();
		updatingQueryb1.append("UPDATE ");
		updatingQueryb1.append(DirectAnnotationDao.name(this.resourceID));
		updatingQueryb1.append(" SET indexing_done=true WHERE indexing_done=false;");
		try{
			nbAnnotation = this.executeSQLUpdate(updatingQueryb1.toString());
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot switch indexingDone flags on _DAT.", e);
		}
		
		// Adds to _IT the isa expanded annotations.
		String query4 = indexationQueryForExpandedAnnotations(1, weights);
		try{
			nbAnnotation = this.executeSQLUpdate(query4);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot index isa expanded annotations from _EAT.", e);
		}
		logger.info(nbAnnotation + " annotations indexed with isa expanded annotations.");

		// Adds to _IT the mapping expanded annotations.
		String query5 = indexationQueryForExpandedAnnotations(2,weights);
		try{
			nbAnnotation = this.executeSQLUpdate(query5);
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot index mapping expanded annotations from _EAT.", e);
		}
		logger.info(nbAnnotation + " annotations indexed with mapping expanded annotations.");
		
		// Switches the indexingDone flags on EAT
		StringBuffer updatingQueryb2 = new StringBuffer();
		updatingQueryb2.append("UPDATE ");
		updatingQueryb2.append(ExpandedAnnotationDao.name(this.resourceID));
		updatingQueryb2.append(" SET indexing_done=true WHERE indexing_done=false;");
		try{
			nbAnnotation = this.executeSQLUpdate(updatingQueryb2.toString());
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot switch indexingDone flags on _EAT.", e);
		}
		
		return this.numberOfEntry();
	}
	
	private String indexationQueryForMgrepAnnotations(boolean doneWithPreferredName, ObrWeight weights){
		/* INSERT INTO OBR_TR_IT (elementID, conceptID, score) 
		SELECT elementID, OBR_TR_DAT.conceptID, @s:=SUM(10*contextWeight)
			FROM OBR_TR_DAT, OBR_CXT, OBS_TT
			WHERE OBR_TR_DAT.contextID=OBR_CXT.contextID AND OBR_TR_DAT.termID=OBS_TT.termID
			AND isPreferred=true AND OBR_TR_DAT.termID IS NOT NULL AND indexingDone=false GROUP BY elementID, conceptID
		ON DUPLICATE KEY UPDATE score=score+@s;  */
	
		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO ");
		query.append(this.getTableSQLName());
		query.append(" (element_id, concept_id, score) SELECT element_id, ");
		query.append(DirectAnnotationDao.name(this.resourceID));
		query.append(".concept_id, @s:=SUM(");
		if (doneWithPreferredName){
			query.append(weights.getPreferredNameDA());	
		}
		else{
			query.append(weights.getSynonymDA());
		}
		query.append("*weight) FROM ");
		query.append(DirectAnnotationDao.name(this.resourceID));
		query.append(", ");
		query.append(contextTableDao.getTableSQLName());
		query.append(", ");
		query.append(termDao.getTableSQLName());
		query.append(" WHERE ");
		query.append(DirectAnnotationDao.name(this.resourceID));
		query.append(".context_id=");
		query.append(contextTableDao.getTableSQLName());
		query.append(".id AND ");
		query.append(DirectAnnotationDao.name(this.resourceID));
		query.append(".term_id=");
		query.append(termDao.getTableSQLName());
		query.append(".id AND is_preferred=");
		if (doneWithPreferredName){
			query.append("true");	
		}
		else{
			query.append("false");
		}
		query.append(" AND ");
		query.append(DirectAnnotationDao.name(this.resourceID));
		query.append(".term_id IS NOT NULL");
		query.append(" AND indexing_done=false GROUP BY element_id, concept_id ON DUPLICATE KEY UPDATE score=score+@s;");
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
		query.append(" (element_id, concept_id, score) SELECT element_id, ");
		query.append(DirectAnnotationDao.name(this.resourceID));
		query.append(".concept_id, @s:=SUM(");
		query.append(weights.getReportedDA());
		query.append("*weight) FROM ");
		query.append(DirectAnnotationDao.name(this.resourceID));
		query.append(", ");
		query.append(contextTableDao.getTableSQLName());
		query.append(" WHERE ");
		query.append(DirectAnnotationDao.name(this.resourceID));
		query.append(".context_id=");
		query.append(contextTableDao.getTableSQLName());
		query.append(".id AND term_id IS NULL AND indexing_done=false GROUP BY element_id, concept_id ON DUPLICATE KEY UPDATE score=score+@s;");
		return query.toString();
	}
	
	private String indexationQueryForExpandedAnnotations(int component, ObrWeight weights){
		/*INSERT INTO OBR_TR_IT (elementID, conceptID, score) 
	    	SELECT elementID, OBR_TR_EAT.conceptID, @s:=SUM(5*contextWeight)
	        	FROM OBR_TR_EAT, OBR_CXT
	        	WHERE OBR_TR_EAT.contextID=OBR_CXT.contextID
	        	AND childConceptID IS NOT NULL AND indexingDone=false GROUP BY elementID, conceptID
			ON DUPLICATE KEY UPDATE score=score+@s; */
		
		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO ");
		query.append(this.getTableSQLName());
		query.append(" (element_id, concept_id, score) SELECT element_id, ");
		query.append(ExpandedAnnotationDao.name(this.resourceID));
		query.append(".concept_id, @s:=SUM(");
		switch (component) {
		// case 1 is equivalent to function 3 in ObrWeights
		case 1: query.append("FLOOR(10*EXP(-").append(weights.getIsaFactor());
				query.append("*");
				query.append(ExpandedAnnotationDao.name(this.resourceID));
				query.append(".parent_level)+1)");
			break;
		case 2: query.append(weights.getMappingEA());
			break;
		}
		query.append("*weight) FROM ");
		query.append(ExpandedAnnotationDao.name(this.resourceID));
		query.append(", ");
		query.append(contextTableDao.getTableSQLName());
		query.append(" WHERE ");
		query.append(ExpandedAnnotationDao.name(this.resourceID));
		query.append(".context_id=");
		query.append(contextTableDao.getTableSQLName());
		query.append(".id AND");
		switch (component) {
		case 1: query.append(" child_concept_id IS NOT NULL");
			break;
		case 2: query.append(" mapped_concept_id IS NOT NULL");
			break;
		}
		query.append(" AND indexing_done=false GROUP BY element_id, concept_id ON DUPLICATE KEY UPDATE score=score+@s;");
		return query.toString();
	}
		
	//********************************* DELETE FUNCTIONS *****************************************************/
	
	/**
	 * Selecting the ontology id from OBS_OT table given local ontology id.
	 * Selecting the concept id from OBS_CT table given ontology id.
	 * Deleting concept id from OBR_IT table given concept id. 
	 */
	private void openDeleteEntriesFromOntologyStatement(){
		/*DELETE OBR_IT FROM OBR_IT WHERE OBR_IT.conceptID IN (
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
		queryb.append(conceptDao.getTableSQLName());
		queryb.append(".id ");
		queryb.append("FROM ");
		queryb.append(conceptDao.getTableSQLName());
		queryb.append(" WHERE ");
		queryb.append(conceptDao.getTableSQLName());
		queryb.append(".ontology_id= ( ");
		queryb.append("SELECT ");
		queryb.append(ontologyDao.getTableSQLName());
		queryb.append(".id ");
		queryb.append("FROM ");
		queryb.append(ontologyDao.getTableSQLName());
		queryb.append(" WHERE ");
		queryb.append(ontologyDao.getTableSQLName());
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
	
	//**********************************Statistics Method****************
	
	private void openGetIndexedAnnotationStatsStatement(){
	    StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT OT.id, COUNT(IT.id) AS COUNT FROM ");
		queryb.append(this.getTableSQLName());		 	 
		queryb.append(" AS IT, ");
		queryb.append(conceptDao.getTableSQLName());
		queryb.append(" AS CT, ");
		queryb.append(ontologyDao.getTableSQLName());
		queryb.append(" AS OT WHERE IT.concept_id=CT.id AND CT.ontology_id = OT.id GROUP BY OT.id; ");
 		this.getIndexedAnnotationStatsStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	/**
	 * This method gives number of indexed annotations for each ontologyID 
	 *  
	 * @return map containing number of indexed annotations for each ontologyID as key.
	 */
	public HashMap<Integer, Integer> getIndexedAnnotationStatistics(){
		HashMap<Integer, Integer> annotationStats = new HashMap<Integer, Integer>();
		 
		try {			 			
			ResultSet rSet = this.executeSQLQuery(this.getIndexedAnnotationStatsStatement);
			while(rSet.next()){
				annotationStats.put(rSet.getInt(1), rSet.getInt(2));
			}			
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openGetIndexedAnnotationStatsStatement();
			return this.getIndexedAnnotationStatistics();
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
