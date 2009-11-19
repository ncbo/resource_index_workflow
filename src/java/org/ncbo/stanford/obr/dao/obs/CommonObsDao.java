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
import obs.common.files.FileParameters;

import org.ncbo.stanford.obr.dao.AbstractObrDao;
import org.ncbo.stanford.obr.enumeration.ObsSchemaEnum;

import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;
 
public class CommonObsDao extends AbstractObrDao {

	private static PreparedStatement getLastDictionaryBeanStatement;
	private static PreparedStatement exactMapStringToLocalConceptIDsStatement;
	
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
		return FileParameters.dictionaryFolder() + dictionary.getDictionaryName() + "_MGREP.txt";
	}
	
	public static String completeDictionaryFileName(DictionaryBean dictionary){
		return FileParameters.dictionaryFolder() + dictionary.getDictionaryName() + "_CMP_MGREP.txt";
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
		File blackFile = new File(FileParameters.LOCAL_FOLDER + FileParameters.BLACK_LIST_FOLDER + blacklist);
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
			lineb.append(column2);
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
		
}
