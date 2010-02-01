package org.ncbo.stanford.obr.dao.statistics;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

import org.ncbo.stanford.obr.dao.AbstractObrDao;
import org.ncbo.stanford.obr.enumeration.ObsSchemaEnum;
import org.ncbo.stanford.obr.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
 * This class is a representation for the the OBR DB OBR_PGDI_CTX table. The table contains 
 * the following columns:
 * 
 * <ul>
 * <li> id                  SMALLINT UNSIGNED  
 * <li> resource_id          INT UNSIGNED NOT NULL 
 * <li>	ontology_id          INT UNSIGNED NOT NULL 
 * <li>	indexed_annotations  INT UNSIGNED 
 * <li>	mgrep_annotations    INT UNSIGNED 
 * <li>	reported_annotations INT UNSIGNED 
 * <li>	isa_annotations      INT UNSIGNED 
 * <li>	mapping_annotations  INT UNSIGNED 
 * <li>  
 * </ul>
 *  
 * @author kyadav
 * @version OBR_v0.2		
 * @created 01-Dec-2009
 *
 */
public class StatisticsDao extends AbstractObrDao {

	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obr.statistics.table.suffix");

	private static PreparedStatement addEntryStatement;
	private static PreparedStatement getOntolgyIDsForResourceStatement;
	private static PreparedStatement deleteEntryStatement;
	private static PreparedStatement deleteStatisticsForResourceStatement;
	
		
	private StatisticsDao() {
		super(EMPTY_STRING, TABLE_SUFFIX);
	}

	@Override
	protected String creationQuery(){
		return "CREATE TABLE " + this.getTableSQLName() +" (" +
					"id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"resource_id INT UNSIGNED NOT NULL, " +
					"ontology_id INT UNSIGNED NOT NULL, " +
					"indexed_annotations INT UNSIGNED," +
					"mgrep_annotations INT UNSIGNED," +
					"reported_annotations INT UNSIGNED," +
					"isa_annotations INT UNSIGNED," +
					"mapping_annotations INT UNSIGNED," +
					"UNIQUE (resource_id, ontology_id), " +
					"FOREIGN KEY (resource_id) REFERENCES " + resourceTableDao.getTableSQLName()  + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
					"FOREIGN KEY (ontology_id) REFERENCES " + ObsSchemaEnum.ONTOLOGY_TABLE.getTableSQLName()	+ "(id) ON DELETE CASCADE ON UPDATE CASCADE" +		
				");";
	}

	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();
		this.openAddEntryStatement();
		this.openGetOntolgyIDsForResourceStatement();
		this.openDeleteEntryStatement();
		this.openDeleteStatisticsForResourceStatement();
	}

	@Override
	protected void closePreparedStatements() throws SQLException {
		super.closePreparedStatements();
		addEntryStatement.close();
		getOntolgyIDsForResourceStatement.close();
		deleteEntryStatement.close();
		deleteStatisticsForResourceStatement.close();
	}

	private static class ContextTableHolder {
		private final static StatisticsDao OBR_STATS_INSTANCE = new StatisticsDao();
	}

	/**
	 * Returns a StatisticsDao object by creating one if a singleton not already exists.
	 */
	public static StatisticsDao getInstance(){
		return ContextTableHolder.OBR_STATS_INSTANCE;
	}

	/****************************************** FUNCTIONS ON THE TABLE ***************************/ 
	
	@Override
	protected void openAddEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (resource_id, ontology_id, indexed_annotations, mgrep_annotations, reported_annotations, isa_annotations, mapping_annotations) VALUES (?,?,?,?,?,?,?) ");	
		queryb.append(" ON DUPLICATE KEY UPDATE indexed_annotations=?, mgrep_annotations= ?, reported_annotations= ?, isa_annotations=?, mapping_annotations=? ;");
		addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}

	/**
	 * Add an new entry in corresponding SQL table.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addEntry(StatisticsEntry entry){
		boolean inserted = false;
		try {
			addEntryStatement.setInt(1, entry.getResourceID());
			addEntryStatement.setInt(2, entry.getOntologyID());
			addEntryStatement.setInt(3, entry.getIndexedAnnotations());
			addEntryStatement.setInt(4, entry.getMgrepAnnotations());
			addEntryStatement.setInt(5, entry.getReportedAnnotations());
			addEntryStatement.setInt(6, entry.getIsaAnnotations());
			addEntryStatement.setInt(7, entry.getMappingAnnotations());
			
			addEntryStatement.setInt(8, entry.getIndexedAnnotations());
			addEntryStatement.setInt(9, entry.getMgrepAnnotations());
			addEntryStatement.setInt(10, entry.getReportedAnnotations());
			addEntryStatement.setInt(11, entry.getIsaAnnotations());
			addEntryStatement.setInt(12, entry.getMappingAnnotations());
			 
			this.executeSQLUpdate(addEntryStatement);
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
			logger.error("** PROBLEM ** Cannot add an entry on table " + this.getTableSQLName(), e);
			logger.error(entry.toString());
		}
		return inserted;	
	}
	
	/**
	 * Add a set of StatisticsEntry to the table.
	 * @param HashSet<StatisticsEntry> entries
	 * @return the number of added entries
	 */
	public int addEntries(HashSet<StatisticsEntry> entries){
		int nbInserted = 0;
		for(StatisticsEntry entry: entries){
			if (this.addEntry(entry)){
				nbInserted++;
			}
		}
		return nbInserted;
	} 

	private void openGetOntolgyIDsForResourceStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT ontology_id FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" WHERE resource_id =  ?");	
		getOntolgyIDsForResourceStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	/**
	 * Get list of ontologies used for indexing resource with given resourceID.
	 * 
	 * @param resourceID
	 * @return List of ontology IDs
	 */
	public ArrayList<Integer> getOntolgyIDsForResource(int resourceID){		 
		ArrayList<Integer> ontolgyIDs = new ArrayList<Integer>();
		try {
			getOntolgyIDsForResourceStatement.setInt(1, resourceID); 			 
			ResultSet rSet = this.executeSQLQuery(getOntolgyIDsForResourceStatement);
			
			while(rSet.next()){
				ontolgyIDs.add(rSet.getInt(1));
			}
			 
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openGetOntolgyIDsForResourceStatement();
			return this.getOntolgyIDsForResource(resourceID);
		} 
		catch (SQLException e) {			 
			logger.error("** PROBLEM ** Cannot get ontology IDs for resource from table " + this.getTableSQLName(), e);			 
		}
		 
		return ontolgyIDs;
	}
	
	private void openDeleteEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" WHERE resource_id = ? AND ontology_id = ?");	
		deleteEntryStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	/**
	 * Delete entry from STATS table for given resourceID and ontologyID
	 * 
	 * @param resourceID
	 * @param ontologyID
	 * @return
	 */
	public boolean deleteEntry(int resourceID, int ontologyID){
		boolean deleted = false;
		try{
			deleteEntryStatement.setInt(1, resourceID);
			deleteEntryStatement.setInt(2, ontologyID);
			this.executeSQLUpdate(deleteEntryStatement);
			deleted = true;
		}		
		catch (MySQLNonTransientConnectionException e) {
			this.openDeleteEntryStatement();
			return this.deleteEntry(resourceID, ontologyID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot delete entries from "+this.getTableSQLName()+" for resourceID: " +resourceID + " ontologyID: "+ ontologyID+". False returned.", e);
		}
		return deleted;
	}
	
	private void openDeleteStatisticsForResourceStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FROM ");
		queryb.append(this.getTableSQLName());	
		queryb.append(" ");
		queryb.append(" WHERE resource_id= (SELECT id FROM ");
		queryb.append(resourceTableDao.getTableSQLName());
		queryb.append("  WHERE resource_id= ?);"); 	
		
		deleteStatisticsForResourceStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	/**
	 * Delete entry from STATS table for given resourceID and ontologyID
	 * 
	 * @param resourceID
	 * @param ontologyID
	 * @return
	 */
	public boolean deleteStatisticsForResource(String resourceID){
		boolean deleted = false;
		try{
			deleteStatisticsForResourceStatement.setString(1, resourceID);			 
			this.executeSQLUpdate(deleteStatisticsForResourceStatement);
			deleted = true;
		}		
		catch (MySQLNonTransientConnectionException e) {
			this.openDeleteStatisticsForResourceStatement();
			return this.deleteStatisticsForResource(resourceID );
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot delete entries from "+this.getTableSQLName()+" for resourceID: " +resourceID + ". False returned.", e);
		}
		return deleted;
	}
 

	/********************************* ENTRY CLASS *****************************************************/

	/**
	 * This class is a representation for a OBR_STATS table entry.
	 * 
	 * @author kyadav
	 * @version  	
	 * @created 01-Dec-2008
	 */
	public static class StatisticsEntry {

		private int resourceID;
		private int ontologyID;
		private int indexedAnnotations;
		private int mgrepAnnotations;
		private int reportedAnnotations;
		private int isaAnnotations;
		private int mappingAnnotations;
	 
				
		public StatisticsEntry(int resourceID, int ontologyID,
				int indexedAnnotations, int mgrepAnnotations,
				int reportedAnnotations, int isaAnnotations,
				int mappingAnnotations) {
			super();
			this.resourceID = resourceID;
			this.ontologyID = ontologyID;
			this.indexedAnnotations = indexedAnnotations;
			this.mgrepAnnotations = mgrepAnnotations;
			this.reportedAnnotations = reportedAnnotations;
			this.isaAnnotations = isaAnnotations;
			this.mappingAnnotations = mappingAnnotations;
		} 

		

		/**
		 * @return the resourceID
		 */
		public int getResourceID() {
			return resourceID;
		}



		/**
		 * @return the ontologyID
		 */
		public int getOntologyID() {
			return ontologyID;
		}



		/**
		 * @return the indexedAnnotations
		 */
		public int getIndexedAnnotations() {
			return indexedAnnotations;
		}



		/**
		 * @return the mgrepAnnotations
		 */
		public int getMgrepAnnotations() {
			return mgrepAnnotations;
		}



		/**
		 * @return the reportedAnnotations
		 */
		public int getReportedAnnotations() {
			return reportedAnnotations;
		}



		/**
		 * @return the isaAnnotations
		 */
		public int getIsaAnnotations() {
			return isaAnnotations;
		}



		/**
		 * @return the mappingAnnotations
		 */
		public int getMappingAnnotations() {
			return mappingAnnotations;
		}



		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("StatisticsEntry: [");
			sb.append(this.resourceID);
			sb.append(", ");
			sb.append(this.ontologyID);
			sb.append(", ");
			sb.append(this.indexedAnnotations);
			sb.append(", ");
			sb.append(this.mgrepAnnotations);
			sb.append(", ");
			sb.append(this.reportedAnnotations);
			sb.append(", ");
			sb.append(this.isaAnnotations);			 
			sb.append(", ");
			sb.append(this.mappingAnnotations);
			sb.append("]");
			return sb.toString();
		}
	}
}
