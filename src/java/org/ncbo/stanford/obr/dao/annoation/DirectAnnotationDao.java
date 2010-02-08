package org.ncbo.stanford.obr.dao.annoation;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.ncbo.stanford.obr.dao.AbstractObrDao;
import org.ncbo.stanford.obr.dao.element.ElementDao;
import org.ncbo.stanford.obr.util.FileResourceParameters;
import org.ncbo.stanford.obr.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
 * This class is a representation for the the OBR DB OBR_XX_DAT table. The table contains 
 * the following columns:
 * 
 * <ul>
 * <li> id 			            		BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 * <li> elementID  	            			INT UNSIGNED NOT NULL FOREIGN KEY,
 * <li> conceptID							INT UNSIGNED NOT NULL FOREIGN KEY,
 * <li> contextID							SMALLINT UNSIGNED NOT NULL FOREIGN KEY,
 * <li> termID 								INT UNSIGNED FOREIGN KEY,
 * <li> from								INT,
 * <li> to									INT,
 * <li> dictionaryID						SMALLINT UNSIGNED FOREIGN KEY,
 * <li> isaClosureDone 						BOOL, 
 * <li> mappingDone 						BOOL, 
 * <li> distanceDone						BOOL,     
 * <li> indexingDone						BOOL     
 * </ul>
 *  
 * @author Adrien Coulet, Clement Jonquet
 * @version OBR_v0.2		
 * @created 12-Nov-2008
 *
 */
public class DirectAnnotationDao extends AbstractObrDao {

	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obr.annotation.table.suffix");
	
	private PreparedStatement addEntryStatement;
	private PreparedStatement addMgrepEntryStatement;	 
	private PreparedStatement deleteEntriesFromOntologyStatement;
	
	/**
	 * Creates a new DirectAnnotationTable with a given resourceID.
	 * The suffix that will be added for AnnotationTable is "_DAT".
	 */
	public DirectAnnotationDao(String resourceID) {
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
		return "CREATE TABLE " + this.getTableSQLName() +" (" +
					"id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"element_id INT UNSIGNED NOT NULL, " +
					"concept_id INT UNSIGNED NOT NULL, " +
					"context_id SMALLINT UNSIGNED NOT NULL, " +
					"term_id INT UNSIGNED, " +				
					this.getTableSQLName()+".position_from INTEGER, " +
					this.getTableSQLName()+".position_to INTEGER, " +
					"dictionary_id SMALLINT UNSIGNED NOT NULL, " +
					"is_a_closure_done BOOL NOT NULL, " +
					"mapping_done BOOL NOT NULL, " +
					"distance_done BOOL NOT NULL, " +
					"indexing_done BOOL NOT NULL, " +
					"FOREIGN KEY (element_id) REFERENCES "    + ElementDao.name(this.resourceID)        + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
					"FOREIGN KEY (concept_id) REFERENCES "    + conceptDao.getTableSQLName()        	 + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
					"FOREIGN KEY (context_id) REFERENCES "    + contextTableDao.getTableSQLName()            + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
					"FOREIGN KEY (term_id) REFERENCES "       + termDao.getTableSQLName()           	 + "(id) ON DELETE CASCADE ON UPDATE CASCADE, "    +					
					"FOREIGN KEY (dictionary_id) REFERENCES " + dictionaryDao.getTableSQLName()  + "(id) ON DELETE CASCADE ON UPDATE CASCADE, "+
					"INDEX X_" + this.getTableSQLName() +"is_a_closure_done (is_a_closure_done), " +
					"INDEX X_" + this.getTableSQLName() +"mapping_done (mapping_done), " +
					"INDEX X_" + this.getTableSQLName() +"distance_done (distance_done)," +
					"INDEX X_" + this.getTableSQLName() +"indexing_done (indexing_done)" +
				");";
	}

	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();
		this.openAddEntryStatement();
		this.openAddMgrepEntryStatement();		 
		this.openDeleteEntriesFromOntologyStatement();
	}

	@Override
	protected void closePreparedStatements() throws SQLException {
		super.closePreparedStatements();
		this.addEntryStatement.close();
		this.addMgrepEntryStatement.close();		 
		this.deleteEntriesFromOntologyStatement.close();
	}

	/****************************************** FUNCTIONS ON THE TABLE ***************************/ 

	@Override
	protected void openAddEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (element_id, concept_id, context_id, dictionary_id, is_a_closure_done, mapping_done, distance_done, indexing_done) "); 
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
		queryb.append(",?,?,?,?,?)");	
		this.addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}

	/**
	 * Add an new entry in corresponding SQL table.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addEntry(DirectAnnotationEntry entry){
		boolean inserted = false;
		try {
			this.addEntryStatement.setString (1, entry.getLocalElementID());
			this.addEntryStatement.setString (2, entry.getLocalConceptID());
			this.addEntryStatement.setString (3, entry.getContextName());
			this.addEntryStatement.setInt    (4, entry.getDictionaryID());
			this.addEntryStatement.setBoolean(5, entry.getIsaClosureDone());
			this.addEntryStatement.setBoolean(6, entry.getMappingDone());
			this.addEntryStatement.setBoolean(7, entry.getDistanceDone());
			this.addEntryStatement.setBoolean(8, entry.getIndexingDone());
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

	private void openAddMgrepEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (element_id, concept_id, context_id, term_id, "+this.getTableSQLName()+".position_from, "+this.getTableSQLName()+".position_to, dictionary_id, is_a_closure_done, mapping_done, distance_done, indexing_done) "); 
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
		queryb.append(",?,?,?,?,?,?,?,?)");	
		this.addMgrepEntryStatement = this.prepareSQLStatement(queryb.toString());
	}

	/**
	 * Add an new entry in corresponding SQL table.
	 * This entry corresponds to an annotation done with Mgrep.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addMgrepEntry(DirectMgrepAnnotationEntry entry){
		boolean inserted = false;
		try {
			this.addMgrepEntryStatement.setString (1, entry.getLocalElementID());
			this.addMgrepEntryStatement.setString (2, entry.getLocalConceptID());
			this.addMgrepEntryStatement.setString (3, entry.getContextName());
			this.addMgrepEntryStatement.setInt    (4, entry.getTermID());
			this.addMgrepEntryStatement.setInt    (5, entry.getFrom());
			this.addMgrepEntryStatement.setInt    (6, entry.getTo());
			this.addMgrepEntryStatement.setInt    (7, entry.getDictionaryID());
			this.addMgrepEntryStatement.setBoolean(8, entry.getIsaClosureDone());
			this.addMgrepEntryStatement.setBoolean(9, entry.getMappingDone());
			this.addMgrepEntryStatement.setBoolean(10,entry.getDistanceDone());
			this.addMgrepEntryStatement.setBoolean(11,entry.getIndexingDone());
			this.executeSQLUpdate(this.addMgrepEntryStatement);
			inserted = true;
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openAddMgrepEntryStatement();
			return this.addMgrepEntry(entry);
		}
		catch (MySQLIntegrityConstraintViolationException e){
			//logger.error("Table " + this.getTableSQLName() + " already contains an entry for the concept: " + entry.getLocalConceptID() +".");
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot add an Mgrep entry "+entry.toString()+" on table " + this.getTableSQLName(), e);
			logger.error(entry.toString());
		}
		return inserted;	
	}
	
	/**
	 * Add a set of DirectAnnotationEntry to the table.
	 * @param HashSet<DirectAnnotationEntry> entries
	 * @return the number of added entries
	 */
	public int addEntries(HashSet<DirectAnnotationEntry> entries){
		int nbInserted = 0;
		for(DirectAnnotationEntry entry: entries){
			if (this.addEntry(entry)){
				nbInserted++;
			}
		}
		return nbInserted;
	} 
	 
	//********************************* MGREP FUNCTIONS *****************************************************

	/**
	 * Loads a Mgrep file (that respects the Mgrep specification, 5 columns: termID/from/to/elementID/contextID) into the table and completes
	 * the information in the table.
	 * Returns the number of annotations added to the table. 
	 */
	public int loadMgrepFile(File mgrepFile, int dictionaryID){
		int nbAnnotation;

		// Creates a temporary table with the same columns than the Mgrep result file
		/* CREATE TEMPORARY TABLE OBR_TR_MGREP
    	(termID INT UNSIGNED, OBR_TR_MGREP.from INT UNSIGNED, OBR_TR_MGREP.to INT UNSIGNED, elementID INT UNSIGNED, contextID INT UNSIGNED,); */
		
		StringBuffer createQuery = new StringBuffer();
		createQuery.append("CREATE TEMPORARY TABLE ");
		createQuery.append(this.getTableSQLName());
		createQuery.append("_MGREP (term_id INT UNSIGNED, ");
		createQuery.append(this.getTableSQLName());
		createQuery.append("_MGREP.position_from INT UNSIGNED, ");
		createQuery.append(this.getTableSQLName());
		createQuery.append("_MGREP.position_to INT UNSIGNED, element_id INT UNSIGNED, context_id INT UNSIGNED);");
		try{
			this.executeSQLUpdate(createQuery.toString());
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot create temporary table to load the file " + mgrepFile.getName(), e);
		}
		
		// Loads the Mgrep results from the file to the temporary table
		/* Example of query
		 * LOAD DATA INFILE '/ncbodata/OBR/MgrepResult/OBR_RESOURCE_TR_V1_MGREP.txt.mgrep'  
			INTO TABLE OBR_TR_MGREP FIELDS TERMINATED BY '	' (termID, OBR_TR_MGREP.from, OBR_TR_MGREP.to, elementID, contextID) ; */
		// DO NOT USE a embedded SELECT IT SLOWS DOWN SIGNIFICANTLY THE QUERY
		StringBuffer loadingQuery = new StringBuffer();
		loadingQuery.append("LOAD DATA INFILE '");
		loadingQuery.append(FileResourceParameters.mgrepOutputFolder());
		loadingQuery.append(mgrepFile.getName());
		loadingQuery.append("' INTO TABLE ");
		loadingQuery.append(this.getTableSQLName());
		loadingQuery.append("_MGREP FIELDS TERMINATED BY '\t' (term_id, ");
		loadingQuery.append(this.getTableSQLName());
		loadingQuery.append("_MGREP.position_from, ");
		loadingQuery.append(this.getTableSQLName());
		loadingQuery.append("_MGREP.position_to, element_id, context_id);");
		try{
			this.executeSQLUpdate(loadingQuery.toString());
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot load the file " + mgrepFile.getName(), e);
		}
		
		// Joins the temporary table and OBS_TT to populate the table
		/* INSERT INTO OBR_TR_DAT (elementID, conceptID, contextID, termID, OBR_TR_DAT.from, OBR_TR_DAT.to, dictionaryID, isaClosureDone, mappingDone, distanceDone, indexingDone)
 			SELECT elementID, conceptID, contextID, OBR_TR_MGREP.termID, OBR_TR_MGREP.from, OBR_TR_MGREP.to, 1, false, false, false, false
    		FROM OBR_TR_MGREP, OBS_TT WHERE OBR_TR_MGREP.termID=OBS_TT.termID;  */
	 
		StringBuffer joinQuery = new StringBuffer();
		joinQuery.append("INSERT INTO ");
		joinQuery.append(this.getTableSQLName());
		joinQuery.append(" (element_id, concept_id, context_id, term_id, ");
		joinQuery.append(this.getTableSQLName());
		joinQuery.append(".position_from, ");
		joinQuery.append(this.getTableSQLName());
		joinQuery.append(".position_to, dictionary_id, is_a_closure_done, mapping_done, distance_done, indexing_done) SELECT element_id, concept_id, context_id, ");
		joinQuery.append(this.getTableSQLName());
		joinQuery.append("_MGREP.term_id, ");
		joinQuery.append(this.getTableSQLName());
		joinQuery.append("_MGREP.position_from, ");
		joinQuery.append(this.getTableSQLName());
		joinQuery.append("_MGREP.position_to, ");
		joinQuery.append(dictionaryID);
		joinQuery.append(", false, false, false, false FROM ");
		joinQuery.append(this.getTableSQLName());
		joinQuery.append("_MGREP, ");
		joinQuery.append(termDao.getTableSQLName());
		joinQuery.append(" WHERE ");
		joinQuery.append(this.getTableSQLName());
		joinQuery.append("_MGREP.term_id=");
		joinQuery.append(termDao.getTableSQLName());
		joinQuery.append(".id;");
		try{
			nbAnnotation = this.executeSQLUpdate(joinQuery.toString());
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot join the temporary table and OBS_TT to load the file " + mgrepFile.getName()+". 0 returned", e);
			nbAnnotation = 0;
		}
		
		// Deletes the temporary table
		StringBuffer deleteQuery = new StringBuffer();
		deleteQuery.append("DROP TABLE ");
		deleteQuery.append(this.getTableSQLName());
		deleteQuery.append("_MGREP;");
		try{
			this.executeSQLUpdate(deleteQuery.toString());
			}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot delete the temporary table.", e);
		}
		return nbAnnotation;
	}
	
	//********************************* DELETE FUNCTIONS *****************************************************/
	
	/**
	 * Selecting the ontology id from OBS_OT table given local ontology id.
	 * Selecting the concept id from OBS_CT table given ontology id.
	 * Deleting concept id from OBR_DAT table given concept id. 
	 */
	private void openDeleteEntriesFromOntologyStatement(){
		/*DELETE OBR_DAT FROM OBR_DAT WHERE OBR_DAT.conceptID IN (
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
	
	/**
	 * Deletes the rows corresponding to annotations done with a termName in the given String list.
	 * @return Number of rows deleted. 
	 */
	public int deleteEntriesFromStopWords(HashSet<String> stopwords){
		int nbDelete = -1; 
		/* DELETE OBR_GEO_DAT FROM OBR_GEO_DAT, OBS_TT
		WHERE OBR_GEO_DAT.termID=OBS_TT.termID AND termName IN ();*/
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(",");		
		queryb.append(termDao.getTableSQLName());		
		queryb.append(" WHERE ");
		queryb.append(this.getTableSQLName());
		queryb.append(".term_id=");
		queryb.append(termDao.getTableSQLName());		
		queryb.append(".id AND ");
		queryb.append(" name IN(");
		for(Iterator<String> it = stopwords.iterator(); it.hasNext();){
			queryb.append("'");
			queryb.append(it.next());
			queryb.append("'");
			if(it.hasNext()){
				queryb.append(", ");
			}
		}
		queryb.append(");");
		try {
			if(stopwords.size()>0){
				nbDelete = this.executeSQLUpdate(queryb.toString());
				this.closeTableGenericStatement();	
			}
			else{
				nbDelete = 0;
			}
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot delete entries from "+this.getTableSQLName()+" with given list of stopwords. -1 returned.", e);
		}
		return nbDelete;
	}
	
	//**********************Annotations Statistics ******************/
	
	/**
	 * 
	 *  Get number of Mgrep Annotations for each ontlogyID
	 *  
	 *  @return Map containing number of mgerp annotations for each ontology as key. 
	 *   
	 */
	public HashMap<Integer, Integer> getMgrepAnnotationStatistics(){
		HashMap<Integer, Integer> annotationStats = new HashMap<Integer, Integer>();
		
		StringBuffer queryb = new StringBuffer();		 
		queryb.append("SELECT OT.id, COUNT(DAT.id) AS COUNT FROM ");
		queryb.append(this.getTableSQLName());		 	 
		queryb.append(" AS DAT, ");
		queryb.append(conceptDao.getTableSQLName());
		queryb.append(" AS CT, ");
		queryb.append(ontologyDao.getTableSQLName());
		queryb.append(" AS OT WHERE DAT.concept_id=CT.id AND CT.ontology_id=OT.id AND DAT.term_id IS NOT NULL GROUP BY OT.id; ");
		
		try {			 			
			ResultSet rSet = this.executeSQLQuery(queryb.toString());
			while(rSet.next()){
				annotationStats.put(rSet.getInt(1), rSet.getInt(2));
			}			
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {			 
			return this.getMgrepAnnotationStatistics();
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get mgrep annotations statistics from "+this.getTableSQLName()+" .", e);
		}
		return annotationStats;
		 
	}
	
	/**
	 *  Get number of reported annotations for each ontlogyID
	 *  
	 *  @return Map containing number of reported annotations for each ontology as key. 
	 */
	public HashMap<Integer, Integer> getReportedAnnotationStatistics(){
		HashMap<Integer, Integer> annotationStats = new HashMap<Integer, Integer>();
		
		StringBuffer queryb = new StringBuffer();		 
		queryb.append("SELECT OT.id, COUNT(DAT.id) AS COUNT FROM ");
		queryb.append(this.getTableSQLName());		 	 
		queryb.append(" AS DAT, ");
		queryb.append(conceptDao.getTableSQLName());
		queryb.append(" AS CT, ");
		queryb.append(ontologyDao.getTableSQLName());
		queryb.append(" AS OT WHERE DAT.concept_id = CT.id AND CT.ontology_id = OT.id AND DAT.term_id IS NULL GROUP BY OT.id; ");
		
		try {			 			
			ResultSet rSet = this.executeSQLQuery(queryb.toString());
			while(rSet.next()){
				annotationStats.put(rSet.getInt(1), rSet.getInt(2));
			}			
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {			 
			return this.getReportedAnnotationStatistics();
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get reported annotations statistics from " +this.getTableSQLName()+" .", e);
		}
		return annotationStats;
		 
	}
	
	/********************************* ENTRY CLASSES *****************************************************/

	/**
	 * This class is a representation for a OBR_XX_DAT table entry.
	 * 
	 * @author Adrien Coulet, Clement Jonquet
	 * @version OBR_v0.2		
	 * @created 12-Nov-2008
	 */
	public static class DirectAnnotationEntry {

		private String localElementID;
		private String localConceptID;
		private String contextName;
		private Integer dictionaryID;
		private Boolean isaClosureDone;
		private Boolean mappingDone;
		private Boolean distanceDone;
		private Boolean indexingDone;
		
		public DirectAnnotationEntry(String localElementID, String localConceptID, String contextName, Integer dictionaryID,
				Boolean isaClosureDone, Boolean mappingDone, Boolean distanceDone, Boolean indexingDone) {
			super();
			this.localElementID = localElementID;
			this.localConceptID = localConceptID;
			this.contextName = contextName;
			this.dictionaryID = dictionaryID;
			this.isaClosureDone = isaClosureDone;
			this.mappingDone = mappingDone;
			this.distanceDone = distanceDone;
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

		public Integer getDictionaryID() {
			return dictionaryID;
		}

		public Boolean getIsaClosureDone() {
			return isaClosureDone;
		}

		public Boolean getMappingDone() {
			return mappingDone;
		}

		public Boolean getDistanceDone() {
			return distanceDone;
		}

		public Boolean getIndexingDone() {
			return indexingDone;
		}

		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("DirectAnnotationEntry: [");
			sb.append(this.localElementID);
			sb.append(", ");
			sb.append(this.localConceptID);
			sb.append(", ");
			sb.append(this.contextName);
			sb.append(", ");
			sb.append(this.dictionaryID);
			sb.append(", ");			
			sb.append(this.isaClosureDone);
			sb.append(", ");			
			sb.append(this.mappingDone);
			sb.append(", ");			
			sb.append(this.distanceDone);
			sb.append(", ");			
			sb.append(this.indexingDone);
			sb.append("]");
			return sb.toString();
		}
	}

	
	/**
	 * This class is a representation for a OBR_XX_DAT table entry.
	 * This class corresponds to an annotation done with Mgrep.
	 * 
	 * @author Adrien Coulet, Clement Jonquet
	 * @version OBR_v0.2		
	 * @created 12-Nov-2008
	 */
	static class DirectMgrepAnnotationEntry extends DirectAnnotationEntry {

		private Integer termID;
		private Integer from;
		private Integer to;
		
		public DirectMgrepAnnotationEntry(String localElementID,
				String localConceptID, String contextName, Integer termID,
				Integer from, Integer to, Integer dictionaryID,
				Boolean isaClosureDone, Boolean mappingDone,
				Boolean distanceDone, Boolean indexingDone) {
			super(localElementID, localConceptID, contextName, dictionaryID,
					isaClosureDone, mappingDone, distanceDone, indexingDone);
			this.termID = termID;
			this.from = from;
			this.to = to;
		}

		public Integer getTermID() {
			return termID;
		}

		public Integer getFrom() {
			return from;
		}

		public Integer getTo() {
			return to;
		}

		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("DirectMgrepAnnotationEntry: [");
			sb.append(this.getLocalElementID());
			sb.append(", ");
			sb.append(this.getLocalConceptID());
			sb.append(", ");
			sb.append(this.getContextName());
			sb.append(", ");
			sb.append(this.termID);
			sb.append(", ");
			sb.append(this.from);
			sb.append(", ");
			sb.append(this.to);
			sb.append(", ");			
			sb.append(this.getDictionaryID());
			sb.append(", ");			
			sb.append(this.getIsaClosureDone());
			sb.append(", ");			
			sb.append(this.getMappingDone());
			sb.append(", ");			
			sb.append(this.getDistanceDone());
			sb.append(", ");			
			sb.append(this.getIndexingDone());
			sb.append("]");
			return sb.toString();
		}
	}
}
