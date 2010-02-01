package org.ncbo.stanford.obr.dao.element;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import obs.obr.populate.Element;
import obs.obr.populate.Structure;

import org.ncbo.stanford.obr.dao.AbstractObrDao;
import org.ncbo.stanford.obr.dao.annoation.DirectAnnotationDao.DirectAnnotationEntry;
import org.ncbo.stanford.obr.enumeration.ObsSchemaEnum;
import org.ncbo.stanford.obr.util.MessageUtils;
import org.ncbo.stanford.obr.util.StringUtilities;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
import com.mysql.jdbc.exceptions.MySQLSyntaxErrorException;

/**
 * This class is a generic representation for OBR element table OBR_XX_ET table. The table contains 
 * the following columns:
 * 
 * <ul>
 * <li> id 			            INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 * <li> local_element_id  	            VARCHAR(255) NOT NULL UNIQUE,
 * <li> dictionary_id 	            	SMALLINT UNSIGNED FOREIGN KEY,
 * <li> CHANGING:: contextIDText1       TEXT,
 * <li> CHANGING:: contextIDText2       TEXT,
 * <li> etc.
 * </ul>
 *  
 * DictionaryID specifies the latest version (ordered) of the dictionary used to annotate the element.
 * Before the element is annotated, this field is NULL.
 *  
 * @author Adrien Coulet, Clement Jonquet
 * @version OBR_v0.2		
 * @created 20-Nov-2008
 *
 */
public class ElementDao extends AbstractObrDao {

	private static final String TABLE_SUFFIX = MessageUtils.getMessage("obr.element.table.suffix");
	
	private ArrayList<String> contextNames;
	
	private PreparedStatement addEntryStatement;	 
	private PreparedStatement getAllLocalElementIDsStatement;
	
	/**
	 * Creates a new elementTable with a given resourceID and a resource structure.
	 * The suffix that will be added for AnnotationTable is "_ET".
	 * This constructor is used for the population and the update of the element tables (OBR_XX_ET)
	 */	
	public ElementDao(String resourceID, Structure structure) {
		super(resourceID, TABLE_SUFFIX);
		this.contextNames = structure.getContextNames();
		this.alterElementTable();
		this.openAddEntryStatement();		 
	}
	
	/**
	 * Returns the SQL table name for a given resourceID 
	 */
	public static String name(String resourceID){
		return OBR_PREFIX + resourceID.toLowerCase() + TABLE_SUFFIX;
	}
	
	@Override
	protected String creationQuery(){
		return "CREATE TABLE " + this.getTableSQLName() +" (" +
					"id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"local_element_id VARCHAR(255) NOT NULL UNIQUE, " +
					"dictionary_id SMALLINT UNSIGNED, " +
					"FOREIGN KEY (dictionary_id) REFERENCES " + ObsSchemaEnum.DICTIONARY_VERSION_TABLE.getTableSQLName()  + "(dictionary_id) ON DELETE CASCADE ON UPDATE CASCADE"+
				");";
	}
	
	@Override
	public void reInitializeSQLTable(){
		super.reInitializeSQLTable();
		this.alterElementTable();
		this.openAddEntryStatement();		 
	}
	
	@Override
	protected void openPreparedStatements() {
		super.openPreparedStatements();
		// Exception for that one, because the contextNames is not affected yet.
		// this.openAddEntryStatement();
		// this.openGetElementStatement();
		this.openGetAllLocalElementIDsStatement();
	}

	@Override
	protected void closePreparedStatements() throws SQLException {
		super.closePreparedStatements();
		this.addEntryStatement.close();		
		this.getAllLocalElementIDsStatement.close();
	}

	/****************************************** ALTERING TABLE ***************************/ 

	private void alterElementTable() {		
		String query = "ALTER TABLE " + this.getTableSQLName() +" ADD (" + this.contextsForCreateQuery() + ");";
		try{
			this.executeSQLUpdate(query);
		}
		catch(MySQLSyntaxErrorException e){
			//e.printStackTrace();
			logger.info("No needs to alter table " + this.getTableSQLName());
		}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot alter ElementTable " + this.getTableSQLName(), e);
		}
	}
	
	private String contextsForCreateQuery(){
		StringBuffer queryb = new StringBuffer();
		for(Iterator<String> it = this.contextNames.iterator(); it.hasNext();){
			queryb.append(it.next().toLowerCase());
			queryb.append(" TEXT");
			if (it.hasNext()){
				queryb.append(", ");
			}
		}
		return queryb.toString();
	}
	
	/****************************************** FUNCTIONS ON THE TABLE ***************************/ 
	
	@Override
	protected void openAddEntryStatement(){		
		StringBuffer queryb = new StringBuffer();		
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" SET local_element_id=?, ");
		queryb.append(this.contextsForInsertQuery());
		queryb.append(";");	
		this.addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	private String contextsForInsertQuery(){
		StringBuffer queryb = new StringBuffer();
		for(Iterator<String> it = this.contextNames.iterator(); it.hasNext();){
			queryb.append(it.next());
			queryb.append("=?");
			if (it.hasNext()){
				queryb.append(", ");
			}
		}
		return queryb.toString();
	}

	/**
	 * Add an new entry in the corresponding _ET table.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addEntry(Element element){
		boolean inserted = false;
		int index = 2;
		try {
			this.addEntryStatement.setString(1, element.getLocalElementID());
			for(String contextName: this.contextNames){
				String itemValue = element.getElementStructure().getText(contextName);
				this.addEntryStatement.setString(index, StringUtilities.escapeLine(itemValue));				
				index++;
			}
			this.executeSQLUpdate(this.addEntryStatement);
			inserted = true;
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openAddEntryStatement();
			return this.addEntry(element);
		}
		catch (MySQLIntegrityConstraintViolationException e){
			// TODO: not to catch this exception here 
			//logger.info("Table " + this.getTableSQLName() + " already contains a row for element " + element.getLocalElementID() +".");
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot add an entry on table " + this.getTableSQLName(), e);
		}
		return inserted;	
	} 
	
	private void openGetAllLocalElementIDsStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT local_element_id FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(";");
		this.getAllLocalElementIDsStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	/**
	 * Returns a set of all the localElementIDs contained in the table. 
	 */
	public HashSet<String> getAllLocalElementIDs(){
		HashSet<String> localElementIDs = new HashSet<String>();
		try {
			ResultSet rSet = this.executeSQLQuery(this.getAllLocalElementIDsStatement);
			while(rSet.next()){
				localElementIDs.add(rSet.getString(1));
			}
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openGetAllLocalElementIDsStatement();
			return this.getAllLocalElementIDs();
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get lcoalElementIDs from "+this.getTableSQLName()+". Empty set returned.", e);
		}
		return localElementIDs;
	}
	
	/**
	 * Returns the value of a given context for a given element in the table.
	 */
	public String getContextValueByContextName(String localElementID, String contextName){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT "+contextName+" FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" WHERE "+this.getTableSQLName()+".local_element_id='");
		queryb.append(localElementID);
		queryb.append("';");
		
		String contextValue;
		try {
			ResultSet rSet = this.executeSQLQuery(queryb.toString());
			rSet.first();
			contextValue = rSet.getString(1);
			rSet.close();
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get contexteValue for "+contextName+" and for localElementID:"+localElementID+" in "+this.getTableSQLName()+". Null returned.", e);
			contextValue = null;
		}
		return contextValue;
	}
	
	/**
	 * Returns a set of all the values contained in the given column of table. 
	 */
	public HashSet<String> getAllValuesByColumn(String columName){
		HashSet<String> values = new HashSet<String>();
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT ").append(columName).append(" FROM ").append(this.getTableSQLName()).append(";");
		try {
			ResultSet valuesRSet = this.executeSQLQuery(queryb.toString());
			while (valuesRSet.next()){
				values.add(valuesRSet.getString(1));
			}
			valuesRSet.close();
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get values of column "+columName+" for table " + this.getTableSQLName(), e);
		}
		return values;	
	}
	
	/**
	 * Writes the given file with all the non annotated elements according to a given dictionaryID. 
	 */
	public void writeNonAnnotatedElements(File mgrepResourceFile, int dictionaryID, Structure structure){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT * FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" WHERE dictionary_id IS NULL OR dictionary_id<");
		queryb.append(dictionaryID);
		queryb.append(";");
		
		//loads the contextName-contextID in a temporary structure to avoid querying the DB when executing the resultset streaming
		Hashtable<String, Integer> contexts = new Hashtable<String, Integer>();
		for(String contextName: structure.getContextNames()){
			contexts.put(contextName, contextTableDao.getContextIDByContextName(contextName));
		}
		
		try{
			FileWriter foutstream = new FileWriter(mgrepResourceFile);
			BufferedWriter out = new BufferedWriter(foutstream);
			ResultSet rSet = this.executeSQLQueryWithFetching(queryb.toString());
			// For each row in the table, splits the row in several lines in the file
			while(rSet.next()){
				// For each of the contextName in the structure
				for(String contextName: structure.getContextNames()){
					// The annotation via mgrep must be done only for contexts with FOR_CONCEPT_RECOGNITION value
					// (not for reported annotation or contexts not for annotation) 
					if(structure.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION)){
						// Writes the elementID + tab
						out.write(rSet.getInt(1) + "\t");
						// Writes the contextID + tab
						out.write(contexts.get(contextName)+ "\t");
						// Writes the context text
						out.write(rSet.getString(contextName));
						out.newLine();
					}
				}
			}
			rSet.close();
			this.closeTableGenericStatement();
			out.close();
			foutstream.close();
		}
		catch (IOException e) {
			logger.error("** PROBLEM ** Cannot write the Mgrep file for exporting resource.", e);
		}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot write the file " + mgrepResourceFile.getName()+".", e);
		}
	}
	
	/**
	 * Updates the field dictionaryID of all the rows in the table where the dictionaryID is null or < to the given one. 
	 * Returns the number of updated elements. (to be verified)
	 */
	public int updateDictionary(int dictionaryID){
		int nbUpdated;
		StringBuffer updatingQueryb = new StringBuffer();
		updatingQueryb.append("UPDATE ");
		updatingQueryb.append(this.getTableSQLName());
		updatingQueryb.append(" SET dictionary_id=");
		updatingQueryb.append(dictionaryID);
		updatingQueryb.append(" WHERE dictionary_id IS NULL OR dictionary_id<");
		updatingQueryb.append(dictionaryID);
		updatingQueryb.append(";");
		try{
			nbUpdated = this.executeSQLUpdate(updatingQueryb.toString());
		}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot update the dictionary field on table " + this.getTableSQLName() +". 0 returned", e);
			nbUpdated = 0;
		}
		return nbUpdated;
	}
	
	/**
	 * Returns the reported annotations that pre-exist in the resource, as DirectAnnotationEntry in order
	 * to insert them in the corresponding _DAT table.
	 * Reported annotations come from context with staticOntologyID in _CXT that is not null or -1. 
	 */
	public HashSet<DirectAnnotationEntry> getExistingAnnotations(int dictionaryID, Structure structure, String contextName, String localOntologyID, boolean isNewVirsion ){		
				
		HashSet<DirectAnnotationEntry> reportedAnnotations = new HashSet<DirectAnnotationEntry>();
		try{				
			StringBuffer queryb = new StringBuffer();
			queryb.append("SELECT local_element_id, ");
			queryb.append(contextName+" FROM ");
			queryb.append(this.getTableSQLName());
			queryb.append(" WHERE dictionary_id IS NULL ");
			
			if(isNewVirsion){
				queryb.append("OR dictionary_id<");
				queryb.append(dictionaryID);
			} 
			
			queryb.append(";");		
			ResultSet rSet = this.executeSQLQuery(queryb.toString());
			while(rSet.next()){
				String localElementID = rSet.getString(1);		
				String annotationSet  = rSet.getString(2);
				String[] splittedLocalConceptIDs = annotationSet.split(GT_SEPARATOR_STRING);
				for (int i =0;i<splittedLocalConceptIDs.length;i++){			
					try{
						//if the this is a valid localConceptID (to exclude case with "" or " " in the reported annotation column
						if(splittedLocalConceptIDs[i].matches(".*/.*")){
						reportedAnnotations.add(
								new DirectAnnotationEntry(localElementID, 
										splittedLocalConceptIDs[i].replace(structure.getOntoID(contextName), localOntologyID),
										contextName, 
										dictionaryID, // dictionaryID for existing annotations
										false, false, false, false));// for now the semantic distance expansion is not done
						}
					}
					catch (Exception e) {
						logger.error("** PROBLEM ** Problem with existing annotations of element: "+ localElementID +" on table " + this.getTableSQLName() +".", e);
					}
				}
			}
			rSet.close();
				 
		}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot report annotation from the table "+this.getTableSQLName()+". Empty set returned.", e);
		}
		return reportedAnnotations;
	}
	
}
