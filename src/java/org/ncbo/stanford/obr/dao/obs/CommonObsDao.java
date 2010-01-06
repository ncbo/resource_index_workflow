package org.ncbo.stanford.obr.dao.obs;

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

import obs.common.beans.DictionaryBean;
import org.ncbo.stanford.obr.util.FileResourceParameters;

import org.ncbo.stanford.obr.dao.AbstractObrDao;
import org.ncbo.stanford.obr.enumeration.ObsSchemaEnum;

import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
 
public class CommonObsDao extends AbstractObrDao {

	private static PreparedStatement getLastDictionaryBeanStatement;
	private static PreparedStatement exactMapStringToLocalConceptIDsStatement;
	private static PreparedStatement getLatestLocalOntologyIDStatement;
	private static PreparedStatement hasNewVersionOfOntologyStatement;
	private static PreparedStatement getLocalConceptIdByPrefNameAndOntologyIdStatement;
	
	public CommonObsDao() {
		 super();
	} 
	
	@Override
	protected String creationQuery() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void openAddEntryStatement() throws SQLException {
				
	}
	
	@Override
	protected void openPreparedStatements(){	 
		this.openGetLastDictionaryBeanStatement();
		this.openExactMapStringToLocalConceptIDsStatement();
		this.openGetLatestLocalOntologyIDStatement();
		this.openHasNewVersionOfOntologyStatement();
		this.openGetLocalConceptIdByPrefNameAndOntologyId();
	} 

	protected void closePreparedStatements() throws SQLException { 
		getLastDictionaryBeanStatement.close();
	}
	
	private static class CommonObsDaoHolder {
		private final static CommonObsDao COMMON_OBS_DAO_INSTANCE = new CommonObsDao();
	}

	/**
	 * Returns a CommonObsDao object by creating one if a singleton not already exists.
	 */
	public static CommonObsDao getInstance(){
		return CommonObsDaoHolder.COMMON_OBS_DAO_INSTANCE;
	}
	
	/****************************************** FUNCTIONS ON THE TABLE ***************************/ 
	 
	
	private void openGetLastDictionaryBeanStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT id, name, date_created  FROM ");
		queryb.append(ObsSchemaEnum.DICTIONARY_VERSION_TABLE.getTableSQLName());
		queryb.append(" WHERE id =(SELECT MAX(id) FROM ");
		queryb.append(ObsSchemaEnum.DICTIONARY_VERSION_TABLE.getTableSQLName());
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
		sb.append(" WHERE name NOT IN(''");
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
	public int writeDictionaryFile(File file, int dictionaryID){
		String query = "SELECT id, name FROM " + ObsSchemaEnum.TERM_TABLE.getTableSQLName() + this.blackListFilter()+ " AND dictionary_id=" + dictionaryID +";";
		int nbLines = 0;
		try{
			ResultSet couplesSet = this.executeSQLQuery(query);
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
	public int writeDictionaryFile(File file){
		String query = "SELECT id, name FROM " + ObsSchemaEnum.TERM_TABLE.getTableSQLName() + this.blackListFilter() + ";";
		int nbLines = 0;
		try{
			ResultSet couplesSet = this.executeSQLQuery(query);
			nbLines = this.writeFile(file, couplesSet);
			couplesSet.close();
			this.closeTableGenericStatement();
		}
		catch(Exception e){
			logger.error("** PROBLEM ** Cannot write complete dictionary file " + file.getName(), e);
		}
		return nbLines;
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
	
	
	/******************* Term Table related query*********************/
	
	private void openExactMapStringToLocalConceptIDsStatement(){
		StringBuffer queryb = new StringBuffer();
		queryb.append(mapStringQueries());
		queryb.append(" AND ");
		queryb.append(ObsSchemaEnum.TERM_TABLE.getTableSQLName());
		queryb.append(".name=? AND local_ontology_id=?;");
		exactMapStringToLocalConceptIDsStatement = this.prepareSQLStatement(queryb.toString());
	} 
	
	private String mapStringQueries(){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT local_concept_id FROM ");
		queryb.append(ObsSchemaEnum.TERM_TABLE.getTableSQLName());
		queryb.append(", ");
		queryb.append(ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName());
		queryb.append(", ");
		queryb.append(ObsSchemaEnum.ONTOLOGY_TABLE.getTableSQLName());
		queryb.append(" WHERE ");
		queryb.append(ObsSchemaEnum.TERM_TABLE.getTableSQLName());
		queryb.append(".concept_id=");
		queryb.append(ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName());
		queryb.append(".id AND ");
		queryb.append(ObsSchemaEnum.CONCEPT_TABLE.getTableSQLName());
		queryb.append(".ontology_id=");
		queryb.append(ObsSchemaEnum.ONTOLOGY_TABLE.getTableSQLName());
		queryb.append(".id");
		return queryb.toString();
	}
	
	public HashSet<String> mapStringToLocalConceptIDs(String s, String localOntologyID){
		HashSet<String> localConceptIDs = new HashSet<String>();
		try {
			ResultSet rSet;			 
			exactMapStringToLocalConceptIDsStatement.setString(1, s);
			exactMapStringToLocalConceptIDsStatement.setString(2, localOntologyID);
			rSet = this.executeSQLQuery(exactMapStringToLocalConceptIDsStatement);
			 
			 
			while(rSet.next()){
				localConceptIDs.add(rSet.getString(1));
			}
			rSet.close();
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openExactMapStringToLocalConceptIDsStatement();
			 
			return this.mapStringToLocalConceptIDs(s, localOntologyID);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get concepts from "+this.getTableSQLName()+" that map string: "+ s +" in ontology: "+localOntologyID+". Empty set returned.", e);
		}
		return localConceptIDs;
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
			this.openExactMapStringToLocalConceptIDsStatement();
			 
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
			this.openExactMapStringToLocalConceptIDsStatement();
			 
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
		String localConceptID = "";
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
	 
		
}