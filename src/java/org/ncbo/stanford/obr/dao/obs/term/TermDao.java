package org.ncbo.stanford.obr.dao.obs.term;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.ncbo.stanford.obr.dao.AbstractObrDao;
import org.ncbo.stanford.obr.dao.obs.concept.ConceptDao;
import org.ncbo.stanford.obr.dao.obs.ontology.OntologyDao;
import org.ncbo.stanford.obr.enumeration.ObsSchemaEnum;

import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
/**
 * This class is a representation for the OBS(slave) DB obs_term table. The table contains 
 * the following columns:
 * <ul>
 * <li>id INT(11) NOT NULL PRIMARY KEY
   <li>name TEXT NOT NULL
   <li>concept_id INT(11) NOT NULL
   <li>dictionary_id INT(11) NOT NULL
 * </ul>
 * 
 */
public class TermDao extends AbstractObrDao{

	private static ConceptDao conceptDao = ConceptDao.getInstance();
	private static OntologyDao ontologyDao = OntologyDao.getInstance();

	private PreparedStatement addEntryStatement;


	protected TermDao() {
		super(ObsSchemaEnum.TERM_TABLE.getTableSQLName());

	}
	public static String name(String resourceID){		
		return ObsSchemaEnum.TERM_TABLE.getTableSQLName();
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
		queryb.append(" (id, name, concept_id, is_preferred, dictionary_id) VALUES (?,?,?,?,?);");
		addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}
	@Override
	protected String creationQuery() {
		//need to change this query for dictionary_id
		return "CREATE TABLE " + getTableSQLName() +" (" +
		"id INT(11) NOT NULL PRIMARY KEY, " +
		"name TEXT NOT NULL, " +
		"concept_id INT(11) NOT NULL, " +
		"is_preferred TINYINT(1) NOT NULL, " +
		"dictionary_id INT(11) NOT NULL, " +
		"FOREIGN KEY (concept_id) REFERENCES " + conceptDao.getTableSQLName() + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
		"FOREIGN KEY (dictionary_id) REFERENCES " + ontologyDao.getTableSQLName() + "(dictionary_id) ON DELETE CASCADE ON UPDATE CASCADE, " +
		"INDEX X_" + getTableSQLName() +"_termName (name(255)), " +
		"INDEX X_" + getTableSQLName() +"_isPreferred (is_preferred)" +
		");";
	}
	public boolean addEntry(TermEntry entry){
		boolean inserted = false;
		try {
			addEntryStatement.setInt(1, entry.getId());
			addEntryStatement.setString(2, AbstractObrDao.escapeLine(entry.getName()));
			addEntryStatement.setString(3, entry.getConceptID());
			addEntryStatement.setBoolean(4, entry.isPreferred());
			addEntryStatement.setInt(5, entry.getDictionaryID());
			this.executeSQLUpdate(addEntryStatement);
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

	public static class TermEntry{

		private int id;
		private String name;
		private String conceptID;
		private boolean isPreferred;
		private int dictionaryID;

		protected TermEntry(int id, String name, String conceptID,
				boolean isPreferred, int dictionaryID) {
			this.id = id;
			this.name = name;
			this.conceptID = conceptID;
			this.isPreferred = isPreferred;
			this.dictionaryID = dictionaryID;
		}

		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getConceptID() {
			return conceptID;
		}
		public void setConceptID(String conceptID) {
			this.conceptID = conceptID;
		}
		public boolean isPreferred() {
			return isPreferred;
		}
		public void setPreferred(boolean isPreferred) {
			this.isPreferred = isPreferred;
		}
		public int getDictionaryID() {
			return dictionaryID;
		}
		public void setDictionaryID(int dictionaryID) {
			this.dictionaryID = dictionaryID;
		}
		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("TermEntry: [");
			sb.append(this.id);
			sb.append(", ");			
			sb.append(this.name);
			sb.append(", ");
			sb.append(this.conceptID);
			sb.append(", ");
			sb.append(this.isPreferred);
			sb.append(", ");
			sb.append(this.dictionaryID);
			sb.append("]");
			return sb.toString();
		}
	}
}
