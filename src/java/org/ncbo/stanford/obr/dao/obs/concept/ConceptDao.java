package org.ncbo.stanford.obr.dao.obs.concept;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.ncbo.stanford.obr.dao.AbstractObrDao;
import org.ncbo.stanford.obr.dao.obs.ontology.OntologyDao;
import org.ncbo.stanford.obr.enumeration.ObsSchemaEnum;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
/**
 * This class is a representation for the OBS(slave) DB obs_concept table. The table contains 
 * the following columns:
 * <ul>
 * <li>id INT(11) NOT NULL PRIMARY KEY
   <li>local_concept_id VARCHAR(246) NOT NULL UNIQUE
   <li>ontology_id INT(11) NOT NULL
   <li>is_toplevel TINY NOT NULL
 * </ul>
 * 
 */
public class ConceptDao extends AbstractObrDao{
		
	private static OntologyDao ontologyDao = OntologyDao.getInstance();
	private PreparedStatement addEntryStatement;
	
	protected ConceptDao() {
		super(ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName());

	}
	public static String name(String resourceID){		
		return ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName();
	}
	
	@Override
	protected String creationQuery() {
		return "CREATE TABLE " + this.getTableSQLName() +" (" +
		"id INT(11) NOT NULL PRIMARY KEY, " +		
		"local_concept_id VARCHAR(246) NOT NULL UNIQUE, " +
		"ontology_id INT(11) NOT NULL, " +
		"is_toplevel TINY NOT NULL, " +
		"FOREIGN KEY (ontology_id) REFERENCES " + ontologyDao.getTableSQLName() + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
		"INDEX X_" + this.getTableSQLName() +"_isTopLevel (is_toplevel)" +
	");";
}
	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();
		this.openAddEntryStatement();		
	}
	
	@Override
	protected void closePreparedStatements() throws SQLException {
		super.closePreparedStatements();
		this.addEntryStatement.close();		
	}
	@Override
	protected void openAddEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (id, local_concept_id, ontology_id, is_toplevel) VALUES (?,?,?,?);");
		this.addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}
	public boolean addEntry(ConceptEntry entry){
		boolean inserted = false;
		try {
			addEntryStatement.setInt(1, entry.getId());
			addEntryStatement.setString(2, entry.getLocalConceptID());
			addEntryStatement.setInt(3, entry.getOntologyID());
			addEntryStatement.setBoolean(4, entry.isTopLevel());
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
	public static class ConceptEntry{
		
		private int id;
		private String localConceptID;
		private int ontologyID;
		private boolean isTopLevel;
				
		protected ConceptEntry(int id, String localConceptID, int ontologyID,
				boolean isToplevel) {
			this.id = id;
			this.localConceptID = localConceptID;
			this.ontologyID = ontologyID;
			this.isTopLevel = isToplevel;
		}
		
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getLocalConceptID() {
			return localConceptID;
		}
		public void setLocalConceptID(String localConceptID) {
			this.localConceptID = localConceptID;
		}
		public int getOntologyID() {
			return ontologyID;
		}
		public void setOntologyID(int ontologyID) {
			this.ontologyID = ontologyID;
		}
		
		public boolean isTopLevel() {
			return isTopLevel;
		}
		public void setTopLevel(boolean isTopLevel) {
			this.isTopLevel = isTopLevel;
		}
		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("ConceptEntry: [");
			sb.append(this.localConceptID + ",");
			sb.append(this.localConceptID);
			sb.append(", ");
			sb.append(this.ontologyID);
			sb.append(", ");
			sb.append(this.isTopLevel);
			sb.append("]");
			return sb.toString();
		}		
	}	
	private static class ConceptTableHolder {
		private final static ConceptDao OBS_CONCEPT_INSTANCE = new ConceptDao();
	}

	/**
	 * Returns a ConceptTable object by creating one if a singleton not already exists.
	 */
	public static ConceptDao getInstance(){
		return ConceptTableHolder.OBS_CONCEPT_INSTANCE;
	}
}
