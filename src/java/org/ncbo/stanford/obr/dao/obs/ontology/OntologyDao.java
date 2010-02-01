package org.ncbo.stanford.obr.dao.obs.ontology;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.ncbo.stanford.obr.dao.obs.AbstractObsDao;
import org.ncbo.stanford.obr.enumeration.ObsSchemaEnum;
import org.ncbo.stanford.obr.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
 * This class is a representation for the OBS(slave) DB obs_ontology table. The table contains 
 * the following columns:
 * <ul>
   <li>id INT(11) NOT NULL PRIMARY KEY
   <li>local_ontology_id VARCHAR(246) NOT NULL UNIQUE
   <li>name VARCHAR(246) NOT NULL
   <li>version VARCHAR(246) NOT NULL
   <li>description VARCHAR(246) NOT NULL
   <li>status INT(11) NOT NULL
   <li>virtual_ontology_id VARCHAR(246) NOT NULL
   <li>format VARCHAR(32) DEFAULT NULL
 * </ul>
 * 
 */
public class OntologyDao extends AbstractObsDao{
	
	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obs.ontology.table.suffix");

	private PreparedStatement addEntryStatement;	
	private static PreparedStatement getLatestLocalOntologyIDStatement;
	private static PreparedStatement hasNewVersionOfOntologyStatement;
	private static PreparedStatement getLocalConceptIdByPrefNameAndOntologyIdStatement;

	
	private OntologyDao() {		
		super(TABLE_SUFFIX);
	} 
	
	private static class OntologyDaoHolder {
		private final static OntologyDao ONTOLOGY_DAO_INSTANCE = new OntologyDao();
	}
	
	/**
	 * Returns a OntologyTable object by creating one if a singleton not already exists.
	 */
	public static OntologyDao getInstance(){
		return OntologyDaoHolder.ONTOLOGY_DAO_INSTANCE;
	}
	
	public static String name(String resourceID){		
		return ObsSchemaEnum.ONTOLOGY_TABLE.getTableSQLName();
	}
	
	@Override
	protected String creationQuery(){
		return "CREATE TABLE " + this.getTableSQLName() +" (" +
		"id INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
		"local_ontology_id VARCHAR(246) NOT NULL UNIQUE, " +
		"name VARCHAR(246) NOT NULL, " +
		"version VARCHAR(246) NOT NULL, " +
		"description VARCHAR(246) NOT NULL, " +
		"status INT(11) NOT NULL, " +
		"virtual_ontology_id VARCHAR(246) NOT NULL, " +
		"format VARCHAR(32) DEFAULT NULL, "+
		"dictionary_id SMALLINT NOT NULL, " +
		"INDEX X_" + this.getTableSQLName() + "_virtualOntologyID (virtual_ontology_id), " +
		"FOREIGN KEY (dictionary_id) REFERENCES " + dictionaryDao.getTableSQLName() + "(id)" +
		");";
	}
	
	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();
		this.openAddEntryStatement();	
		this.openGetLatestLocalOntologyIDStatement();
		this.openHasNewVersionOfOntologyStatement();
		this.openGetLocalConceptIdByPrefNameAndOntologyId();
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
		queryb.append(" (id, local_ontology_id, name, version, description, status, virtual_ontology_id, format) VALUES (?,?,?,?,?,?,?,?);");
		this.addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	public boolean addEntry(OntologyEntry entry){
		boolean inserted = false;
		try {
			this.addEntryStatement.setInt(1, entry.getId());
			this.addEntryStatement.setString(2, entry.getLocalOntologyID());
			this.addEntryStatement.setString(3, entry.getName());
			this.addEntryStatement.setString(4, entry.getVersion());
			this.addEntryStatement.setString(5, entry.getDescription());
			this.addEntryStatement.setInt(6, entry.getStatus());
			this.addEntryStatement.setString(7, entry.getVirtualOntologyID());
			this.addEntryStatement.setString(8, entry.getFormat());
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
			logger.error("** PROBLEM ** Cannot add entry "+entry.toString()+" on table " + this.getTableSQLName(), e);
			logger.error(entry.toString());
		}
		return inserted;	
	}
	
	/**************************Methos on ontology Table***************************************/

	/**
	 * 
	 */
	private void openGetLatestLocalOntologyIDStatement() {
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT local_ontology_id FROM ");
		queryb.append(ObsSchemaEnum.ONTOLOGY_TABLE.getTableSQLName());
		queryb.append(" where virtual_ontology_id= ? order by id DESC;");
		 
		getLatestLocalOntologyIDStatement = this.prepareSQLStatement(queryb.toString());		
	}
	
	/**
	 * @param virtualOntologyID
	 * @return
	 */
	public String getLatestLocalOntologyID(String virtualOntologyID) {
		String localOntologyID= null;
		try {
			ResultSet rSet;			 
			getLatestLocalOntologyIDStatement.setString(1, virtualOntologyID);
			rSet = this.executeSQLQuery(getLatestLocalOntologyIDStatement);
			 
			if(rSet.first()){
				localOntologyID=rSet.getString(1); 
			} 
			
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openGetLatestLocalOntologyIDStatement();
			 
			return this.getLatestLocalOntologyID(virtualOntologyID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get local ontology ID for "+virtualOntologyID+". Empty set returned.", e);
		}
		return localOntologyID;
	}
	
	private void openHasNewVersionOfOntologyStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT DISTINCT local_ontology_id, dictionary_id FROM ");
		queryb.append(ObsSchemaEnum.ONTOLOGY_TABLE.getTableSQLName());
		queryb.append(" OT, ");
		queryb.append(ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName());
		queryb.append(" CT, ");
		queryb.append(ObsSchemaEnum.TERM_TABLE.getTableSQLName());
		queryb.append(" TT WHERE TT.concept_id = CT.id AND  CT.ontology_id=OT.id");
		queryb.append(" AND OT.virtual_ontology_id= ? order BY OT.id DESC;");
		 
		hasNewVersionOfOntologyStatement = this.prepareSQLStatement(queryb.toString());	
	}

	/**
	 * Check the new version for given virtualOntologyID present which is not processed(not annotated )
	 * by given resourceID.
	 * 
	 * @param ontoID
	 * @param resourceID
	 * @return
	 */
	public boolean hasNewVersionOfOntology(String virtualOntologyID, String resourceID) {
	 
		int dictionaryID= 0;
		try {
			ResultSet rSet;			 
			hasNewVersionOfOntologyStatement.setString(1, virtualOntologyID);
			rSet = this.executeSQLQuery(hasNewVersionOfOntologyStatement);
			 
			if(rSet.first()){				 
				dictionaryID = rSet.getInt(2);
			} 
			rSet.close();
			 
			if(dictionaryID >0){
				if(dictionaryID > resourceTableDao.getDictionaryID(resourceID)){
					return true;
				}
			} 
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openHasNewVersionOfOntologyStatement();
			 
			return this.hasNewVersionOfOntology(virtualOntologyID, resourceID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get local ontology ID for "+virtualOntologyID+". Empty set returned.", e);
		}
		return false;
	
	}

	private void openGetLocalConceptIdByPrefNameAndOntologyId(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT local_concept_id ");
		queryb.append("FROM ");
		queryb.append(ObsSchemaEnum.TERM_TABLE.getTableSQLName());
		queryb.append(" TT, ");
		queryb.append(ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName());
		queryb.append(" CT, ");
		queryb.append(ObsSchemaEnum.ONTOLOGY_TABLE.getTableSQLName());
		queryb.append(" OT WHERE TT.concept_id= CT.id AND CT.ontology_id=OT.id AND ");
		queryb.append("TT.is_preferred=true AND OT.local_ontology_id=? AND TT.name=?;");		 
		getLocalConceptIdByPrefNameAndOntologyIdStatement = this.prepareSQLStatement(queryb.toString());
	}

	public String getLocalConceptIdByPrefNameAndOntologyId(String localOntologyID, String termName){
		String localConceptID = EMPTY_STRING;
		try {
			getLocalConceptIdByPrefNameAndOntologyIdStatement.setString(1, localOntologyID);
			getLocalConceptIdByPrefNameAndOntologyIdStatement.setString(2, termName);
			ResultSet rSet = this.executeSQLQuery(getLocalConceptIdByPrefNameAndOntologyIdStatement);
			if(rSet.first()){
				localConceptID = rSet.getString(1);
			} 
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openGetLocalConceptIdByPrefNameAndOntologyId();
			return this.getLocalConceptIdByPrefNameAndOntologyId(localOntologyID,termName);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get localConceptID from "+this.getTableSQLName()+" for localConceptID: "+ localConceptID +" and termName: "+termName+". EmptySet returned.", e);
		}
		return localConceptID;
	}
	
	
	public static class OntologyEntry{

		private int id;
		private String localOntologyID;
		private String name;
		private String version;
		private String description;
		private int status;
		private String virtualOntologyID;
		private String format;
		
		public OntologyEntry(int id, String localOntologyID, String ontologyName, String ontologyVersion, String ontologyDescription, int ontologyStatus, String virtualOntologyID, String format) {
			super();
			this.id = id;
			this.localOntologyID = localOntologyID;
			this.name = ontologyName;
			this.version = ontologyVersion;
			this.description = ontologyDescription;
			this.status = ontologyStatus;
			this.virtualOntologyID = virtualOntologyID;
			this.format = format;
		}		
				
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getLocalOntologyID() {
			return localOntologyID;
		}

		public void setLocalOntologyID(String localOntologyID) {
			this.localOntologyID = localOntologyID;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public int getStatus() {
			return status;
		}

		public void setStatus(int status) {
			this.status = status;
		}

		public String getVirtualOntologyID() {
			return virtualOntologyID;
		}

		public void setVirtualOntologyID(String virtualOntologyID) {
			this.virtualOntologyID = virtualOntologyID;
		}

		public String getFormat() {
			return format;
		}

		public void setFormat(String format) {
			this.format = format;
		}

		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("OntologyEntry: [");
			sb.append(this.id).append(", ");
			sb.append(this.localOntologyID).append(", ");
			sb.append(this.name).append(", ");
			sb.append(this.version).append(", ");
			sb.append(this.description).append(", ");
			sb.append(this.status).append(", ");
			sb.append(this.virtualOntologyID).append("]");
			sb.append(this.format).append("]");
			return sb.toString();
		}
	}
	 
}
