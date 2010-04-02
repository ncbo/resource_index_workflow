package org.ncbo.stanford.obr.dao.dictionary;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;

import obs.common.beans.DictionaryBean;

import org.ncbo.stanford.obr.dao.AbstractObrDao;
import org.ncbo.stanford.obr.dao.obs.concept.ConceptDao;
import org.ncbo.stanford.obr.dao.obs.ontology.OntologyDao;
import org.ncbo.stanford.obr.dao.obs.term.TermDao;
import org.ncbo.stanford.obr.util.FileResourceParameters;
import org.ncbo.stanford.obr.util.MessageUtils;

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
* This class is a representation for the the OBS DB OBS_DVT table. The table contains 
* the following columns:
* <ul>
 * <li> dictionaryID 	SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
 * <li> dictionaryName 	CHAR(13) NOT NULL UNIQUE),
 * <li> dictionaryDate 	DATETIME NOT NULL.
* </ul>
*  
* @author Clement Jonquet
* @version OBS_v1		
* @created 25-Sept-2008
*
*/
public class DictionaryDao extends AbstractObrDao {

	protected static final String TABLE_SUFFIX = MessageUtils.getMessage("obr.dictionary.table.suffix");
	
	private static PreparedStatement addEntryStatement;
	private static PreparedStatement getLastDictionaryBeanStatement;
	private PreparedStatement numberOfEntryStatement;
	
	private DictionaryDao() {
		super(EMPTY_STRING, TABLE_SUFFIX );
	}
	
	@Override
	protected String creationQuery(){
		return "CREATE TABLE " + this.getTableSQLName() +" (" +
					"id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
					"name CHAR(13) NOT NULL UNIQUE, " +
					"date_created DATETIME" +
				")ENGINE=InnoDB ;";
	}
	
	@Override
	protected void openPreparedStatements(){
		super.openPreparedStatements();
		this.openAddEntryStatement();
		this.openGetLastDictionaryBeanStatement();
		this.openNumberOfEntryStatement();
	}
	
	@Override
	protected void closePreparedStatements() throws SQLException {
		super.closePreparedStatements();
		addEntryStatement.close();
		getLastDictionaryBeanStatement.close();
	}
	
	private static class DictionaryDaoHolder {
		private final static DictionaryDao DICTIOANRY_DAO_INSTANCE = new DictionaryDao();
	}

	/**
	 * Returns a DictionaryDao object by creating one if a singleton not already exists.
	 */
	public static DictionaryDao getInstance(){
		return DictionaryDaoHolder.DICTIOANRY_DAO_INSTANCE;
	}
	
	/****************************************** FUNCTIONS ON THE TABLE ***************************/ 
	
	@Override
	protected void openAddEntryStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("INSERT INTO ");
		queryb.append(this.getTableSQLName());
		queryb.append(" (name, date_created) VALUES (?,NOW());");
		addEntryStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	/**
	 * Add an new entry in corresponding SQL table.
	 * @return True if the entry was added to the SQL table, false if a problem occurred during insertion.
	 */
	public boolean addEntry(String dictionaryName){
		boolean inserted = false;
		try {
			addEntryStatement.setString(1, dictionaryName);
			this.executeSQLUpdate(addEntryStatement);
			inserted = true;
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openAddEntryStatement();
			return this.addEntry(dictionaryName);
		}
		catch (MySQLIntegrityConstraintViolationException e){
			logger.error("Table " + this.getTableSQLName() + " already contains an entry for dictionaryName: " + dictionaryName +".");
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot add an entry on table " + this.getTableSQLName(), e);
			logger.error(dictionaryName);
		}
		return inserted;	
	}
	
	private void openGetLastDictionaryBeanStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT id, name, date_created  FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(" WHERE id =(SELECT MAX(id) FROM ");
		queryb.append(this.getTableSQLName());
		queryb.append(");");
		getLastDictionaryBeanStatement = this.prepareSQLStatement(queryb.toString());
	}
	
	public DictionaryBean getLastDictionaryBean(){
		DictionaryBean dictionary;
		try {
			ResultSet rSet = this.executeSQLQuery(getLastDictionaryBeanStatement);
			if(rSet.first()){
				Calendar cal = Calendar.getInstance();
				cal.setTime(rSet.getDate(3));
				dictionary = new DictionaryBean(rSet.getInt(1), rSet.getString(2), cal);
			}
			else{
				dictionary = null;
			}
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openGetLastDictionaryBeanStatement();
			return this.getLastDictionaryBean();
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get last dictionary from "+this.getTableSQLName()+". Null returned.", e);
			dictionary = null;
		}
		return dictionary;
	}
	
	
	private void openNumberOfEntryStatement() {
		String query = "SELECT COUNT(*) FROM " + this.getTableSQLName() + ";";
		this.numberOfEntryStatement= this.prepareSQLStatement(query);
	}
	
	/**
	 * Returns the number of elements in the table (-1 if a problem occurs during the count). 
	 */
	public int numberOfEntry(){
		int nbEntry = -1;
		try{
			ResultSet rSet = this.executeSQLQuery(numberOfEntryStatement);
			rSet.first();
			nbEntry = rSet.getInt(1);
			rSet.close();
		} 		
		catch (MySQLNonTransientConnectionException e) {
			this.openNumberOfEntryStatement();
			return this.numberOfEntry();
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get number of entry on table " + this.getTableSQLName()+". -1 returned.", e);
		}
		return nbEntry;
	}

	
	/*
	 * Moving methods from ObsOntologiesAccessTool
	 * 
	 */
	
	public static String dictionaryFileName(DictionaryBean dictionary){
		return FileResourceParameters.dictionaryFolder() + dictionary.getDictionaryName() + "_MGREP.txt";
	}
	
	public static String completeDictionaryFileName(DictionaryBean dictionary){
		return FileResourceParameters.dictionaryFolder() + dictionary.getDictionaryName() + "_CMP_MGREP.txt";
	}
	
	/**
	 * Adds to the query to create the dictionary a restriction on the terms selected 
	 * according to a given blacklist.
	 */
	private String blackListFilter(){
		// Specify the black list to use here
		//String blacklist = "OBS_MGREP_basics.txt";
		String blacklist = "OBS_MGREP_empty.txt";
		StringBuffer sb = new StringBuffer();
		sb.append("name NOT IN(''");
		// reads the black list file
		File blackFile = new File(FileResourceParameters.blackListFolder() + blacklist);
		try{
		FileReader fstream = new FileReader(blackFile);
		BufferedReader in = new BufferedReader(fstream);
		String line = in.readLine();
		while (line != null){
			sb.append(", '");
			sb.append(line);
			sb.append("'");
			line = in.readLine();
		}
		in.close();
		fstream.close();
		}
		catch (IOException e) {
			logger.error("** PROBLEM ** Cannot read balck list to filter the dictionary.", e);
		}
		sb.append(")");
		return sb.toString();
	}
	
	/**
	 * Write the given file with the [id name] couples present in the corresponding SQL table
	 * for a given dictionaryID. Used to generate a dictionary file for Mgrep.
	 * @return The number of lines written in the given file.
	 */
	public int writeDictionaryFile(File file, int dictionaryID, HashSet<String> localOntologyIDs){
		StringBuffer queryb = new StringBuffer();
		if(localOntologyIDs== null ||localOntologyIDs.size()== 0){
			queryb.append("SELECT id, name FROM ");
			queryb.append(TermDao.name());
			queryb.append("TT WHERE TT.");
		}else{
			queryb.append("SELECT TT.id, TT.name FROM ");
			queryb.append(TermDao.name());
			queryb.append("TT, ");
			queryb.append(ConceptDao.name());
			queryb.append("CT, ");
			queryb.append(OntologyDao.name());
			queryb.append("OT WHERE TT.concept_id = CT.id AND CT.ontology_id = OT.id AND ");
			queryb.append(localOntologyIDFilter(localOntologyIDs));
			queryb.append(" AND TT.");
		}		
		queryb.append(this.blackListFilter());
		queryb.append(" AND OT.dictionary_id = ");
		queryb.append(dictionaryID);
		queryb.append("; "); 
		
		int nbLines = 0;
		try{
			ResultSet couplesSet = this.executeSQLQuery(queryb.toString());
			nbLines = this.writeFile(file, couplesSet);
			couplesSet.close();
			this.closeTableGenericStatement();
		}
		catch(Exception e){
			logger.error("** PROBLEM ** Cannot write dictionary file " + file.getName()+" with dictionaryID: " + dictionaryID, e);
		}
		return nbLines;
	}
	
	/**
	 * Write the given file with all the [id name] couples present in the corresponding SQL table.
	 * Used to generate a complete dictionary file for Mgrep.
	 * @return The number of lines written in the given file.
	 */
	public int writeDictionaryFile(File file, HashSet<String> ontologies){
		
		StringBuffer queryb = new StringBuffer();
		if(ontologies== null ||ontologies.size()== 0){
			queryb.append("SELECT id, name FROM ");
			queryb.append(TermDao.name());
			queryb.append(" TT WHERE TT.");
		}else{
			queryb.append("SELECT TT.id, TT.name FROM ");
			queryb.append(TermDao.name());
			queryb.append(" TT, ");
			queryb.append(ConceptDao.name());
			queryb.append(" CT, ");
			queryb.append(OntologyDao.name());
			queryb.append(" OT WHERE TT.concept_id = CT.id AND CT.ontology_id = OT.id AND ");
			queryb.append(localOntologyIDFilter(ontologies));
			queryb.append(" AND ");
		}		
		queryb.append(this.blackListFilter());
		queryb.append("; "); 
		
		int nbLines = 0;
		try{
			ResultSet couplesSet = this.executeSQLQuery(queryb.toString());
			nbLines = this.writeFile(file, couplesSet);
			couplesSet.close();
			this.closeTableGenericStatement();
		}
		catch(Exception e){
			logger.error("** PROBLEM ** Cannot write complete dictionary file " + file.getName(), e);
		}
		return nbLines;
	}
	
	private String localOntologyIDFilter(HashSet<String> localOntologyIDs){
		
		StringBuffer queryb = new StringBuffer();
		queryb.append(" local_ontology_id IN(");
		for(Iterator<String> it = localOntologyIDs.iterator(); it.hasNext();){
			queryb.append("'");
			queryb.append(it.next());
			queryb.append("'");
			if(it.hasNext()){
				queryb.append(", ");
			}
		}
		queryb.append(") ");
		
		return queryb.toString();
	}
	
	private int writeFile(File file, ResultSet couplesSet) throws IOException, SQLException {
		int nbLines = 0;
		FileWriter fstream = new FileWriter(file);
		BufferedWriter out = new BufferedWriter(fstream);
		StringBuffer lineb = new StringBuffer();
		while(couplesSet.next()){
			String column1 = couplesSet.getString(1);
			String column2 = couplesSet.getString(2);
			lineb.append(column1);
			lineb.append("\t");
			lineb.append(column2.replaceAll(NEW_LINE_REGEX, BLANK_SPACE));
			out.write(lineb.toString());
			lineb.delete(0, lineb.length());
			//out.newLine();
			// Have to be in Unix format for the mgrep tool
			out.write("\n");
			nbLines++;
		}
		out.close();
		fstream.close();
		return nbLines;
	}
		
}
