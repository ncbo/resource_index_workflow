package org.ncbo.stanford.obr.dao.obs.map;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.ncbo.stanford.obr.dao.obs.AbstractObsDao;
import org.ncbo.stanford.obr.enumeration.ObsSchemaEnum;
import org.ncbo.stanford.obr.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
/**
 * This class is a representation for the OBS(slave) DB obs_map table. The table contains 
 * the following columns:
 * <ul> 
 * <li>id INT(11) NOT NULL PRIMARY KEY
   <li>concept_id INT(11) NOT NULL
   <li>mapped_concept_id INT(11) NOT NULL
   <li>mapping_type VARCHAR(246) NOT NULL
 * </ul>
 * 
 */
public class MapDao extends AbstractObsDao{
	
	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obs.map.table.suffix");
 
	private PreparedStatement addEntryStatement;

	private MapDao() {
		super(TABLE_SUFFIX);

	}
	
	private static class MapDaoHolder {
		private final static MapDao MAP_DAO_INSTANCE = new MapDao();
	}

	/**
	 * Returns a ConceptTable object by creating one if a singleton not already exists.
	 */
	public static MapDao getInstance(){
		return MapDaoHolder.MAP_DAO_INSTANCE;
	}
	
	public static String name(String resourceID){		
		return ObsSchemaEnum.MAPPING_TABLE.getTableSQLName();
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
	protected void openAddEntryStatement() {
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (id, concept_id, mapped_concept_id, mapping_type) VALUES (?,?,?,?);");
		this.addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}

	@Override
	protected String creationQuery() {
		return "CREATE TABLE " + this.getTableSQLName() +" (" +
		"id INT(11) NOT NULL PRIMARY KEY, " +
		"concept_id INT(11) NOT NULL, " +
		"mapped_concept_id INT(11) NOT NULL, " +
		"mapping_type VARCHAR(246) NOT NULL, " +
		"UNIQUE (concept_id, mapped_concept_id ), " +
		"FOREIGN KEY (concept_id) REFERENCES " + conceptDao.getTableSQLName() + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
		"FOREIGN KEY (mapped_concept_id) REFERENCES " + conceptDao.getTableSQLName() + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
		"INDEX X_" + this.getTableSQLName() +"_mappingType (mapping_type)" +
		");";
	}
	public boolean addEntry(MapEntry entry){
		boolean inserted = false;
		try {
			addEntryStatement.setInt(1, entry.getId());
			addEntryStatement.setInt(2, entry.getConceptID());
			addEntryStatement.setInt(3, entry.getMappedConceptID());
			addEntryStatement.setString(4, entry.getMappingType());
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

	public static class MapEntry{

		private int id;
		private int conceptID;
		private int mappedConceptID;
		private String mappingType;


		protected MapEntry(int id, int conceptID, int mappedConceptID,
				String mappingType) {
			this.id = id;
			this.conceptID = conceptID;
			this.mappedConceptID = mappedConceptID;
			this.mappingType = mappingType;
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
		public int getMappedConceptID() {
			return mappedConceptID;
		}
		public void setMappedConceptID(int mappedConceptID) {
			this.mappedConceptID = mappedConceptID;
		}
		public String getMappingType() {
			return mappingType;
		}
		public void setMappingType(String mappingType) {
			this.mappingType = mappingType;
		}

		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("MapEntry: [");
			sb.append(this.id);
			sb.append(", ");			
			sb.append(this.conceptID);
			sb.append(" maps to ");
			sb.append(this.mappedConceptID);
			sb.append(" (");
			sb.append(this.mappingType);
			sb.append(")");
			return sb.toString();
		}
	}
}
