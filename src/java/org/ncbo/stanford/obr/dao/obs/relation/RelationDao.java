package org.ncbo.stanford.obr.dao.obs.relation;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.ncbo.stanford.obr.dao.obs.AbstractObsDao;
import org.ncbo.stanford.obr.dao.obs.concept.ConceptDao;
import org.ncbo.stanford.obr.dao.obs.ontology.OntologyDao;
import org.ncbo.stanford.obr.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
/**
 * This class is a representation for the OBS(slave) DB obs_relation table. The table contains 
 * the following columns:
 * <ul>
 * <li>id INT(11) NOT NULL PRIMARY KEY
   <li>concept_id INT(11) NOT NULL
   <li>parent_concept_id INT(11) NOT NULL
   <li>level INT(11) NOT NULL
 * </ul>
 * 
 */
public class RelationDao extends AbstractObsDao{

	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obs.relation.table.suffix");

	private PreparedStatement addEntryStatement;
	private static PreparedStatement deleteEntriesFromOntologyStatement;

	private RelationDao() {
		super(TABLE_SUFFIX);

	}

	private static class RelationDaoHolder {
		private final static RelationDao RELATION_DAO_INSTANCE = new RelationDao();
	}

	/**
	 * Returns a ConceptTable object by creating one if a singleton not already exists.
	 */
	public static RelationDao getInstance(){
		return RelationDaoHolder.RELATION_DAO_INSTANCE;
	}

	public static String name(String resourceID){		
		return OBS_PREFIX + TABLE_SUFFIX;
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
		deleteEntriesFromOntologyStatement.close();
	}

	@Override
	protected void openAddEntryStatement() {
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (id, concept_id, parent_concept_id, level) VALUES (?,?,?,?);");
		this.addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}

	@Override
	protected String creationQuery() {
		return "CREATE TABLE " + getTableSQLName() +" (" +
		"id INT(11) NOT NULL PRIMARY KEY, " +
		"concept_id INT(11) NOT NULL, " +
		"parent_concept_id INT(11) NOT NULL, " +
		"level INT(11) NOT NULL, " +
		//"UNIQUE (localConceptID, parentLocalConceptID), " +
		"FOREIGN KEY (concept_id) REFERENCES " + conceptDao.getTableSQLName() + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
		"FOREIGN KEY (parent_concept_id) REFERENCES " + conceptDao.getTableSQLName() + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
		"INDEX X_" + getTableSQLName() +"_level (level)" +
		");";
	}

	public boolean addEntry(RelationEntry entry){
		boolean inserted = false;
		try {
			this.addEntryStatement.setInt(1, entry.getId());
			this.addEntryStatement.setInt(2, entry.getConceptID());
			this.addEntryStatement.setInt(3, entry.getParentConceptID());
			this.addEntryStatement.setInt(4, entry.getLevel());
			this.executeSQLUpdate(this.addEntryStatement);
			inserted = true;
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openAddEntryStatement();
			return this.addEntry(entry);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot add an entry on table " + this.getTableSQLName(), e);
			logger.error(entry.toString());
		}
		return inserted;	
	} 

	/**
	 * Method loads the data entries from given file to relation table.
	 * 
	 * @param relationEntryFile File containing relation table entries.
	 * @return Number of entries populated in relation table.
	 */
	public int populateSlaveRelationTableFromFile(File relationEntryFile) {
		int nbInserted =0 ;

		StringBuffer queryb = new StringBuffer();
		queryb.append("LOAD DATA INFILE '");
		queryb.append(relationEntryFile.getAbsolutePath());
		queryb.append("' IGNORE INTO TABLE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FIELDS TERMINATED BY '\t' IGNORE 1 LINES");		
		try{
			nbInserted = this.executeSQLUpdate(queryb.toString());			
		} catch (SQLException e) {			 
			logger.error("Problem in populating map table from file : " + relationEntryFile.getAbsolutePath(), e);
		} 	
		return nbInserted;
	}
	private void openDeleteEntriesFromOntologyStatement(){
		/*DELETE obs_relation FROM obs_term 
		 	WHERE obs_relation.concept_id IN (SELECT id FROM obs_concept 
		 		WHERE obs_concept.ontology_id = (SELECT obs_ontology.id FROM obs_ontology 
		 			WHERE obs_ontology.local_ontology_id = ?)); */
		StringBuffer queryb = new StringBuffer();
		queryb.append("DELETE ");
		queryb.append(this.getTableSQLName());
		queryb.append(" FROM ");
		queryb.append(this.getTableSQLName());		
		queryb.append(" WHERE ");
		queryb.append(this.getTableSQLName());
		queryb.append(".concept_id IN ( ");
		queryb.append("SELECT id FROM ");
		queryb.append(ConceptDao.name(""));		
		queryb.append("WHERE ");
		queryb.append(ConceptDao.name(""));	
		queryb.append(".ontology_id =( ");
		queryb.append("SELECT id FROM ");
		queryb.append(OntologyDao.name(""));
		queryb.append(" WHERE local_ontology_id=?))");		
		deleteEntriesFromOntologyStatement = this.prepareSQLStatement(queryb.toString());
	}
	/**
	 * Deletes the rows for the given local_ontology_id.
	 * @return True if the rows were successfully removed. 
	 */
	public boolean deleteEntriesFromOntology(String localOntologyID){
		boolean deleted = false;
		try{
			deleteEntriesFromOntologyStatement.setString(1, localOntologyID);
			executeSQLUpdate(deleteEntriesFromOntologyStatement);
			deleted = true;
		}		
		catch (MySQLNonTransientConnectionException e) {
			this.openDeleteEntriesFromOntologyStatement();
			return this.deleteEntriesFromOntology(localOntologyID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot delete entries from "+this.getTableSQLName()+" for local_ontology_id: "+ localOntologyID+". False returned.", e);
		}
		return deleted;
	}
	
	public static class RelationEntry{

		private int id;
		private int conceptID;
		private int parentConceptID;
		private int level;

		public RelationEntry(int id, int conceptID, int parentConceptID,
				int level) {
			this.id = id;
			this.conceptID = conceptID;
			this.parentConceptID = parentConceptID;
			this.level = level;
		}
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public int getConceptID() {
			return conceptID;
		}
		public void setConceptID(int conceptID) {
			this.conceptID = conceptID;
		}
		public int getParentConceptID() {
			return parentConceptID;
		}
		public void setParentConceptID(int parentConceptID) {
			this.parentConceptID = parentConceptID;
		}
		public int getLevel() {
			return level;
		}
		public void setLevel(int level) {
			this.level = level;
		}
		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("RelationEntry: [");
			sb.append(this.id);
			sb.append(", ");
			sb.append(this.conceptID);
			sb.append(", ");
			sb.append(this.parentConceptID);
			sb.append(", ");
			sb.append(this.level);
			sb.append("]");
			return sb.toString();
		}		
	} 
}
