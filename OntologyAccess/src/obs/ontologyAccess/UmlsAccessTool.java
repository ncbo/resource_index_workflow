package obs.ontologyAccess;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.mysql.jdbc.CommunicationsException;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

/**
 * This class is used to access UMLS ontologies. 
 * This tool needs a release of UMLS installed on a MySQL database server.
 * It works with the [2007AC] version of UMLS. Other versions have not been tested. <br>
 *  
 * See also {@link OntologyAccess}, {@link AbstractOntologyAccessTool}.<br>
 *  
 * ParametersBean to connect to the MySQL server should be in a String[3] array such as:
 * {"jdbc:mysql://ncbo-db2.stanford.edu:3306/umls","ontrez","ontrez"}   <br>
 * 
 * Accessing ontologies can be restricted by UMLS semantic types.
 * Semantic types are accessed with TUIs as they appear in the MRSTY table, later called <it>localSemanticTypeID</it>.
 * A tool has a set of semantic types (i.e., TUI Strings) that may be null. 
 * This set of semantic types is used as a restriction on certain queries.<br>
 * 
 * Remark: The tool only deal with english-based ontologies
 * i.e., all query to the DB tables MRCONSO & MRSAB are restricted by LAT = 'ENG'.<br>
 * 
 * Remark: The tool only deal with isa hierarchies
 * i.e., all query to the DB table MRHIER & MRSAB are restricted by MRHIER.RELA='isa' OR MRHIER.RELA IS NULL.<br>
 * 
 * The creation of a UmlsAccessTool open a set of prepared statements that are used by the tool to execute the queries.
 * These prepared statements are closed (as well as the connection to the DB) when the tool is destructed. 
 *     
 * @author Clement Jonquet
 * @author Nipun Bathia
 * @author Nigam Shah
 * @version 1.0
 * @created 22-Jul-2008
 */
public class UmlsAccessTool extends AbstractOntologyAccessTool {

	// Logger for this class
	private static Logger logger = Logger.getLogger(UmlsAccessTool.class);
	
	public static final String UMLS_VERSION = "UMLS 2007AC";  
	
	protected Connection toolUMLSConnection;
	private Statement toolStatement;
	private String[] connectionParameters;
	private HashSet<String> toolSemanticTypes;
	// this structure is used both to have the set of all UMLS SAB (keys) and to store the root concepts (values)
	// for each SAB. It avoids computing several time the root concepts. 
	private static Hashtable<String, HashSet<String>> umlsSABs = null;
	// this structure is used to store all the UMLS TUIs
	private static HashSet<String> umlsTUIs = null;
	// this structure is used to store all the UMLS RCUI (i.e., RCUI in MRSAB are CUI that identifies source in UMLS)
	private static HashSet<String> umlsRCUIs = null;
	// this boolean is used only to store the fact that a tool is or not restricted to semantic types
	// if a tool is not restricted, then restrictionToSemanticTypes() is never called and the queries are faster (no join with MRSTY)
	protected boolean isRestricted;
	
	public static final String CUI_BASED_MAPPING = "inter-cui";
	public static final String MAPPING_FROM_MRREL = "from-mrrel";
	private static final String ERROR_NAME = "No name";
	
	// prepared statements that correspond to the queries executed by the tool
	private PreparedStatement getLocalConceptIDsStatement;
	private PreparedStatement getLocalConceptIDsRestrictedStatement;
	private PreparedStatement getAllLocalConceptIDsStatement;
	private PreparedStatement getAllLocalConceptIDsRestrictedStatement;
	private PreparedStatement getConceptPreferredNameStatement;
	private PreparedStatement getConceptSynonymsStatement;
	private PreparedStatement getOntologyNameStatement;
	private PreparedStatement getOntologyDescriptionStatement;
	private PreparedStatement getOntologyDescription2Statement;
	private PreparedStatement getOntologyVersionStatement;
	private PreparedStatement getRootConceptsStatement;
	private PreparedStatement getParentOfSomethingConceptsStatement;
	private PreparedStatement getConceptSemanticTypeStatement;
	private PreparedStatement getSemanticTypeNameStatement;
	private PreparedStatement exactMapToLocalConceptIDStatement;
	private PreparedStatement exactMapToLocalConceptIDRestrictedStatement;
	private PreparedStatement allExactMapToLocalConceptIDStatement;
	private PreparedStatement allExactMapToLocalConceptIDRestrictedStatement;
	private PreparedStatement mapToLocalConceptIDStatement;
	private PreparedStatement mapToLocalConceptIDRestrictedStatement;
	private PreparedStatement allMapToLocalConceptIDStatement;
	private PreparedStatement allMapToLocalConceptIDRestrictedStatement;
	private PreparedStatement getConceptDirectParentsStatement;
	private PreparedStatement getConceptDirectParentsRestrictedStatement;
	private PreparedStatement getConceptDirectChildrenStatement;
	private PreparedStatement getConceptDirectChildrenRestrictedStatement;
	protected PreparedStatement getConceptParentsWithAUIsStatement;
	private PreparedStatement getConceptChildrenWithAUIsStatement;
	private PreparedStatement getConceptChildrenWithAUIs2Statement;
	private PreparedStatement getConceptChildrenWithAUIs2RestrictedStatement;
	private PreparedStatement ptrParsingStatement;
	private PreparedStatement ptrParsingRestrictedStatement;
	private PreparedStatement filterBySemanticTypesStatement;
	private PreparedStatement getConceptMappings1Statement;
	private PreparedStatement getConceptMappings1RestrictedStatement;
	private PreparedStatement getConceptMappings2Statement;
	private PreparedStatement getConceptMappings2RestrictedStatement;
	private PreparedStatement getOntologyMappings1Statement;
	private PreparedStatement getOntologyMappings1RestrictedStatement;
	private PreparedStatement getOntologyMappings2Statement;
	private PreparedStatement getOntologyMappings2RestrictedStatement;
	private PreparedStatement getAllConceptMappings1Statement;
	private PreparedStatement getAllConceptMappings1RestrictedStatement;
	private PreparedStatement getAllConceptMappings2Statement;
	private PreparedStatement getAllConceptMappings2RestrictedStatement;
	private PreparedStatement getAllOntologyMappings1Statement;
	private PreparedStatement getAllOntologyMappings1RestrictedStatement;
	private PreparedStatement getAllOntologyMappings2Statement;
	private PreparedStatement getAllOntologyMappings2RestrictedStatement;
		
	/**
	 * Constructs a tool given a specific array of String parameters for connecting to the UMLS MySQL database.
	 * The connection parameters are [database server address, username, password]. For more details, 
	 * see also JDBC driver documentation.
	 * The new UmlsAccessTool is constructed with an empty set of ontologies and a name.
	 * The tool set of restricted semantic types is also null
	 * i.e., the tool accesses all the semantic type in UMLS (no restriction at all)
	 */
	public UmlsAccessTool(String[] connParameters, String name){
		super("UMLS_" + name);
		this.createUMLSAccessTool(connParameters);
		this.toolSemanticTypes = new HashSet<String>();
		this.isRestricted = false;
		this.openPreparedStatements();
		//populates the key of the tables
		if (umlsSABs == null){
			umlsSABs = getUMLSSABs();
			//populates the values of the table
			this.populateRootConcepts();
		}
		if (umlsTUIs == null){
			umlsTUIs = getUMLSTUIs();
		}
		if (umlsRCUIs == null){
			umlsRCUIs = getUMLSRCUIs();
		}
	}

	/**
	 * Constructs a tool given a specific array of String parameters for connecting to the UMLS MySQL database,
	 * an array of ontologies (i.e., UMLS SABs), a customized generated name and
	 * an array of semantic types to be restricted to (i.e., UMLS TUIs).
	 */
	public UmlsAccessTool(String[] localOntologyIDs, String name, String[] connectionParameters, String[] localSemanticTypeIDs)
		throws NonValidLocalOntologyIDException, NonValidLocalSemanticTypeIDException {
		this(connectionParameters, name);
		// adds the ontologies
		this.addOntologies(localOntologyIDs);
		// adds the semanticTypes
		this.addSemanticTypes(localSemanticTypeIDs);
		// changes the flag isRestricted
		if(this.toolSemanticTypes.isEmpty()){
			this.isRestricted = false;
		}
		else{
			this.isRestricted = true;
		}
	}
		
	private void createUMLSAccessTool(String[] connParameters){
		logger.info("UmlsAccessTool creation...");
		this.connectionParameters = new String[3];
		this.connectionParameters = connParameters;
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			this.toolUMLSConnection = DriverManager.getConnection(this.connectionParameters[0],this.connectionParameters[1],this.connectionParameters[2]);
		}
		catch (Exception e) {
			 
			logger.error("** PROBLEM ** Cannot create connection for UMLSAcccessTool.", e);
			this.toolUMLSConnection = null;
		}
		logger.info("UmlsAccessTool " + this.getToolName() + " created.\n");
	}

	/**
	 * Reopens the DB connection if closed.
	 */
	public void reOpenConnectionIfClosed(){
		try{
			if (this.toolUMLSConnection.isClosed()){
				this.toolUMLSConnection = DriverManager.getConnection(this.connectionParameters[0],this.connectionParameters[1],this.connectionParameters[2]);
				logger.info("SQL connection to UMLS DB just reopenned.");
			}
		}
		catch(SQLException e){
			logger.error("** PROBLEM ** Cannot create connection to database " + this.connectionParameters[0], e);
		}
		this.openPreparedStatements();
	}
	
	public void finalize() throws Throwable {
		super.finalize();
		this.closePreparedStatements();
		this.toolUMLSConnection.close();
	}
	
	protected void openDependentPreparedStatements(){
		this.openGetLocalConceptIDsRestrictedStatement();
		this.openGetAllLocalConceptIDsStatement();
		this.openGetAllLocalConceptIDsRestrictedStatement();
		this.openExactMapToLocalConceptIDRestrictedStatement();
		this.openAllExactMapToLocalConceptIDStatement();
		this.openAllExactMapToLocalConceptIDRestrictedStatement();
		this.openMapToLocalConceptIDRestrictedStatement();
		this.openAllMapToLocalConceptIDStatement();
		this.openAllMapToLocalConceptIDRestrictedStatement();
		this.openGetConceptDirectParentsRestrictedStatement();
		this.openGetConceptDirectChildrenRestrictedStatement();
		this.openPtrParsingRestrictedStatement();
		this.openGetConceptChildrenWithAUIs2RestrictedStatement();
		this.openFilterBySemanticTypesStatement();
		this.openGetConceptMappings1RestrictedStatement();
		this.openGetConceptMappings2RestrictedStatement();
		this.openGetAllConceptMappings1RestrictedStatement();
		this.openGetAllConceptMappings2RestrictedStatement();
		this.openGetOntologyMappings1RestrictedStatement();
		this.openGetOntologyMappings2RestrictedStatement();
		this.openGetAllOntologyMappings1RestrictedStatement();
		this.openGetAllOntologyMappings2RestrictedStatement();
	}
	
	protected void openPreparedStatements(){
		// opens all the dependents prepared statements (i.e., the ones that depends of the tool ontologies and semantic types
		this.openDependentPreparedStatements();
		// opens the other ones
		this.openGetLocalConceptIDsStatement();
		this.openGetConceptPreferredNameStatement();
		this.openGetConceptSynonymsStatement();
		this.openGetOntologyNameStatement();
		this.openGetOntologyDescriptionStatement();
		this.openGetOntologyDescription2Statement();
		this.openGetOntologyVersionStatement();
		this.openGetRootConceptsStatement();
		this.openGetParentOfSomethingConceptsStatement();
		this.openGetConceptSemanticTypeStatement();
		this.openGetSemanticTypeNameStatement();
		this.openExactMapToLocalConceptIDStatement();
		this.openMapToLocalConceptIDStatement();
		this.openGetConceptDirectParentsStatement();
		this.openGetConceptDirectChildrenStatement();
		this.openGetConceptParentsWithAUIsStatement();
		this.openGetConceptChildrenWithAUIsStatement();
		this.openGetConceptChildrenWithAUIs2Statement();
		this.openPtrParsingStatement();
		this.openGetConceptMappings1Statement();
		this.openGetConceptMappings2Statement();
		this.openGetAllConceptMappings1Statement();
		this.openGetAllConceptMappings2Statement();
		this.openGetOntologyMappings1Statement();
		this.openGetOntologyMappings2Statement();
		this.openGetAllOntologyMappings1Statement();
		this.openGetAllOntologyMappings2Statement();
	}
	
	protected void closeDependentPreparedStatements(){
		try{
			this.getLocalConceptIDsRestrictedStatement.close();
			this.getAllLocalConceptIDsStatement.close();
			this.getAllLocalConceptIDsRestrictedStatement.close();
			this.exactMapToLocalConceptIDRestrictedStatement.close();
			this.allExactMapToLocalConceptIDStatement.close();
			this.allExactMapToLocalConceptIDRestrictedStatement.close();
			this.mapToLocalConceptIDRestrictedStatement.close();
			this.allMapToLocalConceptIDStatement.close();
			this.allMapToLocalConceptIDRestrictedStatement.close();
			this.getConceptDirectParentsRestrictedStatement.close();
			this.getConceptDirectChildrenRestrictedStatement.close();
			this.ptrParsingRestrictedStatement.close();
			this.getConceptChildrenWithAUIs2RestrictedStatement.close();
			this.filterBySemanticTypesStatement.close();
			this.getConceptMappings1RestrictedStatement.close();
			this.getConceptMappings2RestrictedStatement.close();
			this.getAllConceptMappings1RestrictedStatement.close();
			this.getAllConceptMappings2RestrictedStatement.close();
			this.getOntologyMappings1RestrictedStatement.close();
			this.getOntologyMappings2RestrictedStatement.close();
			this.getAllOntologyMappings1RestrictedStatement.close();
			this.getAllOntologyMappings2RestrictedStatement.close();
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot close one of the dependent prepared statements of the tool.", e);
		}
	}
	
	protected void closePreparedStatements(){
		try{
			// closes all the dependents prepared statements (i.e., the ones that depends of the tool ontologies and semantic types
			this.closeDependentPreparedStatements();
			// closes the others ones
			this.getLocalConceptIDsStatement.close();
			this.getConceptPreferredNameStatement.close();
			this.getConceptSynonymsStatement.close();
			this.getOntologyNameStatement.close();
			this.getOntologyDescriptionStatement.close();
			this.getOntologyDescription2Statement.close();
			this.getOntologyVersionStatement.close();
			this.getRootConceptsStatement.close();
			this.getParentOfSomethingConceptsStatement.close();
			this.getConceptSemanticTypeStatement.close();
			this.getSemanticTypeNameStatement.close();
			this.exactMapToLocalConceptIDStatement.close();
			this.mapToLocalConceptIDStatement.close();
			this.getConceptDirectParentsStatement.close();
			this.getConceptDirectChildrenStatement.close();
			this.getConceptParentsWithAUIsStatement.close();
			this.getConceptChildrenWithAUIsStatement.close();
			this.getConceptChildrenWithAUIs2Statement.close();
			this.ptrParsingStatement.close();
			this.getConceptMappings1Statement.close();
			this.getConceptMappings2Statement.close();
			this.getAllConceptMappings1Statement.close();
			this.getAllConceptMappings2Statement.close();
			this.getOntologyMappings1Statement.close();
			this.getOntologyMappings2Statement.close();
			this.getAllOntologyMappings1Statement.close();
			this.getAllOntologyMappings2Statement.close();
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot close one of the prepared statements of the tool.", e);
		}
	}
	
	private Hashtable<String, HashSet<String>> getUMLSSABs(){
		// Gets the SAB from UMLS
		HashSet<String> allUMLSSABs = new HashSet<String>();
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT DISTINCT RSAB FROM MRSAB WHERE LAT='ENG';");
		try{
			ResultSet sabRSet = this.executeSQLQuery(queryb.toString());
			while(sabRSet.next()){
				allUMLSSABs.add(sabRSet.getString(1));
			}
			sabRSet.close();
			this.closeStatement();
		}	
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get SABs from MRSAB.", e);
		}
		Hashtable<String, HashSet<String>> table = new Hashtable<String, HashSet<String>>(allUMLSSABs.size());
		// For each SABs, populate the table
		for(String sab: allUMLSSABs){
			// populates here with empty root concepts because we cannot call now
			//getRootConcepts(), because umlsSABs is not affected yet
			table.put(sab, new HashSet<String>());
		}
		return table;
	}
	
	private void populateRootConcepts(){
		for(String sab: umlsSABs.keySet()){
			try {
				umlsSABs.put(sab, this.getOntologyRootConcepts(sab));
			} catch (NonValidLocalOntologyIDException e) {
				// nothing - Should not happen because we get the SABs directly from UMLS
				logger.error("This should not happen - populateRootConcepts().", e); 
			}
		}
	}
	
	private HashSet<String> getUMLSTUIs(){
		// Example of query:
		// SELECT DISTINCT TUI FROM MRSTY;
		HashSet<String> allUMLSTUIs = new HashSet<String>();
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT DISTINCT TUI FROM MRSTY;");
		try{
			ResultSet tuiRSet = this.executeSQLQuery(queryb.toString());
			while(tuiRSet.next()){
				allUMLSTUIs.add(tuiRSet.getString(1));
			}
			tuiRSet.close();
			this.closeStatement();
		}		
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get TUIs from MRSTY.", e);
		}
		return allUMLSTUIs;
	}
	
	private HashSet<String> getUMLSRCUIs(){
		// Example of query:
		// SELECT DISTINCT RCUI FROM MRSAB;
		HashSet<String> allUMLSRCUIs = new HashSet<String>();
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT DISTINCT RCUI FROM MRSAB;");
		try{
			ResultSet rcuiRSet = this.executeSQLQuery(queryb.toString());
			while(rcuiRSet.next()){
				allUMLSRCUIs.add(rcuiRSet.getString(1));
			}
			rcuiRSet.close();
			this.closeStatement();
		}		
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot get RCUIs from MRSAB.", e);
		}
		return allUMLSRCUIs;
	}
	
	/**
	 * Returns the set of UMLS semantic types that the tool deals with.
	 */
	public HashSet<String> getToolSemanticTypes() {
		return toolSemanticTypes;
	}

	private void closeStatement() throws SQLException{
		this.toolStatement.close();
	}
	
	/**
	 * Executes a given SQL query as String using the generic class statement. As it returns a ResultSet,
	 * this statement needs to be explicitly closed after the processing of the ResultSet with function
	 * {see closeStatement()}.
	 */
	protected ResultSet executeSQLQuery(String query) throws SQLException {
		try{
			this.toolStatement = this.toolUMLSConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
		}
		catch (CommunicationsException e) {
			this.reOpenConnectionIfClosed();
			this.toolStatement = this.toolUMLSConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
		}
		//System.out.println("Query executed: " + query);
		return this.toolStatement.executeQuery(query);
	}
	
	/**
	 * Executes the SQL query on the given prepared statement. As it returns a ResultSet,
	 * this statement needs to be explicitly closed after the processing of the ResultSet. 
	 */
	protected ResultSet executeSQLQuery(PreparedStatement stmt) throws SQLException {
		ResultSet rSet = null;
		//System.out.println("Query:"+ stmt.toString());
		try{
			rSet = stmt.executeQuery();
		}
		catch (CommunicationsException e) {
			//e.printStackTrace();
			this.reOpenConnectionIfClosed();
			// Re-calling the execution will generate a MySQLNonTransientConnectionException
			// Those exceptions are catched in each functions to re-execute the query correctly.
			rSet = stmt.executeQuery();
		}
		return rSet;
	}
	
	/**
	 * Open a prepared statement that corresponds to the given SQL query. 
	 */
	protected PreparedStatement prepareSQLStatement(String query){
		try {
			return this.toolUMLSConnection.prepareStatement(query);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot open prepared statement for query: "+query+". Null returned.", e);
			return null;
		}
	}
	
	public void addAllOntology(){
		this.addAllUmlsOntologies();
	}
	
	/**
	 * Adds all the UMLS ontologies to the tool (only the English one).
	 */
	public void addAllUmlsOntologies(){
		for(String ontology: umlsSABs.keySet()){
			try{
				this.addOntology(ontology);
			}
			catch (NonValidLocalOntologyIDException e) {
				// nothing - Should not happen because we get the SABs directly from UMLS
				logger.error("This should not happen - addAllUMLSOntologies()."); 
			}
		}
		this.closeDependentPreparedStatements();
		this.openDependentPreparedStatements();
	}		
	
	@Override
	public void addOntologies(String[] localOntologyIDs) throws NonValidLocalOntologyIDException {
		super.addOntologies(localOntologyIDs);
		this.closeDependentPreparedStatements();
		this.openDependentPreparedStatements();
	}
	
	@Override
	public void addOntology(String localOntologyID) throws NonValidLocalOntologyIDException {
		super.addOntology(localOntologyID);
		this.closeDependentPreparedStatements();
		this.openDependentPreparedStatements();
	}
	
	@Override
	public void removeOntology(String localOntologyID) throws NonValidLocalOntologyIDException {
		super.removeOntology(localOntologyID);
		this.closeDependentPreparedStatements();
		this.openDependentPreparedStatements();
	}
	
	/**
	 * Adds the given semantic types to the ones manipulated by the tool. 
	 */
	public void addSemanticTypes(String[] localSemanticTypeIDs) throws NonValidLocalSemanticTypeIDException {
		HashSet<String> localSemanticTypeIDsSet = arrayToHashSet(localSemanticTypeIDs);
		for(String localSemanticTypeID: localSemanticTypeIDsSet){
			this.addSemanticType(localSemanticTypeID);
		}
		this.closeDependentPreparedStatements();
		this.openDependentPreparedStatements();
	}
	
	/**
	 * Adds a given semantic type to the ones manipulated by the tool. 
	 */
	public void addSemanticType(String localSemanticTypeID) throws NonValidLocalSemanticTypeIDException {
		if (this.isValidLocalSemanticTypeID(localSemanticTypeID)){
			if(this.toolSemanticTypes.add(localSemanticTypeID)){
				logger.info("Semantic type <"+ this.getSemanticTypeName(localSemanticTypeID) + " ("+ localSemanticTypeID + ")> added to the set of semantic types manipulated by " + this.getToolName() + ".");
			}
			else {
				//System.out.println("Semantic type <"+ this.getSemanticTypeName(localSemanticTypeID) + " ("+ localSemanticTypeID + "> already in the set of semantic types manipulated by " + this.getToolName() + ".");
			}
			this.closeDependentPreparedStatements();
			this.openDependentPreparedStatements();
		}
		else {
			throw new NonValidLocalSemanticTypeIDException(localSemanticTypeID);
		}
	}
	
	/**
	 * Removes a given semantic type to the ones manipulated by the tool. 
	 */
	public void removeSemanticType(String localSemanticTypeID) throws NonValidLocalSemanticTypeIDException {
		if (this.isValidLocalSemanticTypeID(localSemanticTypeID)){
			if(this.toolSemanticTypes.remove(localSemanticTypeID)){
				logger.info("Semantic type <" + this.getSemanticTypeName(localSemanticTypeID) + "> removed from the set of semantic types manipulated by " + this.getToolName() + ".");
			}
			else {
				logger.info("Semantic type <" + this.getSemanticTypeName(localSemanticTypeID) + "> was not in the set of semantic types manipulated by " + this.getToolName() + ".");
			}
			this.closeDependentPreparedStatements();
			this.openDependentPreparedStatements();
		}
		else {
			throw new NonValidLocalSemanticTypeIDException(localSemanticTypeID);
		}
	}
	
	// **************************** CONCEPT FUNCTIONS ************************************************
	
	public String getConceptConceptIDPart(String localConceptID) throws NonValidLocalConceptIDException{
		return getConceptCUI(localConceptID);
	}
	
	/**
	 * Returns the CUI part of the localConceptID.
	 */
	public String getConceptCUI(String localConceptID) throws NonValidLocalConceptIDException {
		String cui;
		if (this.isValidLocalConceptID(localConceptID)){
			String[] sarray = localConceptID.split("/");
			cui = sarray[1];
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		return cui;
	}
	
	/**
	 * Returns the set of all localConceptID contains in a given ontology.
	 * Overrided for better performance, because it can be done with one SQL query.
	 * Equivalent to getLocalConceptIDsUMLS(localOntologyID, false).
	 */
	public HashSet<String> getLocalConceptIDs(String localOntologyID) throws NonValidLocalOntologyIDException {
		return this.getLocalConceptIDsUMLS(localOntologyID, false);
	}
	
	private void openGetLocalConceptIDsStatement(){
		String query  = "SELECT DISTINCT MRCONSO.CUI FROM MRCONSO WHERE MRCONSO.SAB=? AND MRCONSO.LAT='ENG';";
		this.getLocalConceptIDsStatement = this.prepareSQLStatement(query);
	}
	
	private void openGetLocalConceptIDsRestrictedStatement() {
		String query  = "SELECT DISTINCT MRCONSO.CUI FROM MRCONSO, MRSTY WHERE MRCONSO.CUI=MRSTY.CUI AND MRCONSO.LAT='ENG' AND MRCONSO.SAB=?" + this.restrictionToSemanticTypes() + ";";
		this.getLocalConceptIDsRestrictedStatement = this.prepareSQLStatement(query);
	}
	
	/**
	 * Returns the set of all localConceptID contains in a given ontology by filtering or not
	 * to the tool semantic types, according to the given flag.
	 */
	public HashSet<String> getLocalConceptIDsUMLS(String localOntologyID, boolean filter) throws NonValidLocalOntologyIDException {
		HashSet<String> localConceptIDs = new HashSet<String>();
		if (this.isValidLocalOntologyID(localOntologyID)){
			try{
				ResultSet rSet;
				if(filter){
					this.getLocalConceptIDsRestrictedStatement.setString(1, localOntologyID);
					rSet = this.executeSQLQuery(this.getLocalConceptIDsRestrictedStatement);
				}
				else{
					this.getLocalConceptIDsStatement.setString(1, localOntologyID);
					rSet = this.executeSQLQuery(this.getLocalConceptIDsStatement);
				}
				while(rSet.next()){
					localConceptIDs.add(this.createLocalConceptID(localOntologyID, rSet.getString(1)));
				}
				rSet.close();
			}		
			catch (MySQLNonTransientConnectionException e) {
				this.openGetLocalConceptIDsStatement();
				this.openGetLocalConceptIDsRestrictedStatement();
				return this.getLocalConceptIDsUMLS(localOntologyID, filter);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get localConceptIDs from MRCONSO and MRSTY for localOntologyID: "+ localOntologyID +". Empty set returned.", e);
			}
		}
		else{
			throw new NonValidLocalOntologyIDException(localOntologyID);
		}
		return localConceptIDs;
	}
	
	/**	
	 * Returns the set of all localConceptIDs in all the ontologies accessed by the tool.
	 * Overrided for better performance, because it can be done with one SQL query.
	 * Equivalent to getAllLocalConceptIDsUMLS(false).
	 */
	public HashSet<String> getAllLocalConceptIDs(){
		return this.getAllLocalConceptIDsUMLS(false);
	}

	private void openGetAllLocalConceptIDsStatement(){
		//String query = "SELECT DISTINCT MRCONSO.CUI, MRCONSO.SAB FROM MRCONSO WHERE MRCONSO.LAT='ENG'" + this.restictionToOntologies() + ";";
		String query = "SELECT DISTINCT MRCONSO.CUI, MRCONSO.SAB, MRCONSO.STR FROM MRCONSO WHERE MRCONSO.LAT='ENG'" + this.restictionToOntologies() + ";";
		this.getAllLocalConceptIDsStatement = this.prepareSQLStatement(query);
	}
	
	private void openGetAllLocalConceptIDsRestrictedStatement(){
		String query = "SELECT DISTINCT MRCONSO.CUI, MRCONSO.SAB FROM MRCONSO, MRSTY WHERE MRCONSO.CUI=MRSTY.CUI AND MRCONSO.LAT='ENG'" + this.restictionToOntologies() + this.restrictionToSemanticTypes() + ";";
		this.getAllLocalConceptIDsRestrictedStatement = this.prepareSQLStatement(query);	
	}
	
	/**	
	 * Returns the set of all localConceptIDs in all the ontologies accessed by the tool 
	 * by filtering or not to the tool semantic types, according to the given flag.
	 */
	public HashSet<String> getAllLocalConceptIDsUMLS(boolean filter){
		HashSet<String> localConceptIDs = new HashSet<String>();
		if(!this.getToolOntologies().isEmpty()){
			try{
				ResultSet rSet;
				if(filter && this.isRestricted){
					rSet = this.executeSQLQuery(this.getAllLocalConceptIDsRestrictedStatement);
				}
				else{
					rSet = this.executeSQLQuery(this.getAllLocalConceptIDsStatement);
				}
				while(rSet.next()){
					localConceptIDs.add(this.createLocalConceptID(rSet.getString(2), rSet.getString(1)));
				}
				rSet.close();
			}		
			catch (MySQLNonTransientConnectionException e) {
				this.openGetAllLocalConceptIDsStatement();
				this.openGetAllLocalConceptIDsRestrictedStatement();
				return this.getAllLocalConceptIDsUMLS(filter);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get localConceptIDs from MRCONSO and MRSTY for all ontologies accessed by the tool. Empty set returned.", e);
			}
		}
		return localConceptIDs;
	}
	// note: the size of the set returned by this function will never bigger than 165Mb (MRCONSO.size * 8 * 3 * 8)
	// (supposing character are utf8 (3 bytes each))
			
	private void openGetConceptPreferredNameStatement() {
		String query = "SELECT DISTINCT STR FROM MRCONSO WHERE CUI=? AND SAB=? AND LAT='ENG' ORDER BY LOWER(TS), LUI, STT, SUI, ISPREF DESC LIMIT 1;";
		this.getConceptPreferredNameStatement = this.prepareSQLStatement(query); 
	}
	
	/**
	 * For a given concept, returns the preferred name of the concept.
	 * Remark: In UMLS, preferred name are for CUIs.
	 * It means that a concept restricted to a specific ontology (i.e., localConceptID) may not 
	 * have any preferred name because the UMLS preferred name for the corresponding CUI is not 
	 * in the restricted ontology. In that case, this function returns the first result of the following SQL query:
	 * SELECT DISTINCT STR FROM MRCONSO WHERE cui = 'C0025202' AND sab = 'SNOMEDCT'	AND lat = 'ENG' 
	 * ORDER BY lower(ts), lui, stt, sui, ispref desc LIMIT 1;
	 */
	public String getConceptPreferredName(String localConceptID) throws NonValidLocalConceptIDException {
		String preferredName;
		if (this.isValidLocalConceptID(localConceptID)){
			try{
				this.getConceptPreferredNameStatement.setString(1, this.getConceptCUI(localConceptID));
				this.getConceptPreferredNameStatement.setString(2, this.getConceptOntology(localConceptID));
				ResultSet rSet = this.executeSQLQuery(this.getConceptPreferredNameStatement);
				rSet.first();
				preferredName = rSet.getString(1);
				rSet.close();
			}
			catch (MySQLNonTransientConnectionException e) {
				this.openGetConceptPreferredNameStatement();
				return this.getConceptPreferredName(localConceptID);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get concept preferred name from MRCONSO for localConceptID: "+ localConceptID +". 'No name' String returned.", e);
				preferredName = ERROR_NAME;
			}
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		return preferredName;
	}

	private void openGetConceptSynonymsStatement() {
		String query = "SELECT DISTINCT STR FROM MRCONSO WHERE CUI=? AND SAB=? AND LAT='ENG' ORDER BY LOWER(TS), LUI, STT, SUI, ISPREF DESC;";
		this.getConceptSynonymsStatement = this.prepareSQLStatement(query); 
	}
	
	/**
	 * For a given concept, returns a set of String, synonym names of the concept.
	 * Returns the rest of the results of the query done by the getPreferredName function 
	 * (without the LIMIT 1).
	 */
	public HashSet<String> getConceptSynonyms(String localConceptID) throws NonValidLocalConceptIDException {
		HashSet<String> synonyms = new HashSet<String>();
		if (this.isValidLocalConceptID(localConceptID)){
			try{
				this.getConceptSynonymsStatement.setString(1, this.getConceptCUI(localConceptID));
				this.getConceptSynonymsStatement.setString(2, this.getConceptOntology(localConceptID));
				ResultSet rSet = this.executeSQLQuery(this.getConceptSynonymsStatement);
				// jumps the first one
				rSet.next();
				while(rSet.next()){
					synonyms.add(rSet.getString(1));
				}
				rSet.close();
			}		
			catch (MySQLNonTransientConnectionException e) {
				this.openGetConceptSynonymsStatement();
				return this.getConceptSynonyms(localConceptID);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get synonyms from MRCONSO for localConceptID: "+ localConceptID +". Empty set returned.");
			}
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		return synonyms;
	}
		
	// ************************************* MAPPING FUNCTIONS *****************************

	private void openGetConceptMappings1Statement() {
		String query = "SELECT DISTINCT CUI, SAB FROM MRCONSO WHERE CUI=? AND SAB=? AND LAT='ENG';";
		/*  SELECT DISTINCT CUI, SAB FROM MRCONSO
    		WHERE CUI='C0025202'
    		AND SAB='NCI'
    		AND LAT='ENG';	*/
		this.getConceptMappings1Statement = this.prepareSQLStatement(query); 
	}
	
	private void openGetConceptMappings1RestrictedStatement() {
		String query = "SELECT DISTINCT MRCONSO.CUI, MRCONSO.SAB FROM MRCONSO, MRSTY WHERE MRCONSO.CUI=MRSTY.CUI AND CUI=? AND SAB=? AND LAT='ENG'"+ this.restrictionToSemanticTypes() +";";
		this.getConceptMappings1RestrictedStatement = this.prepareSQLStatement(query); 
	}
	
	private void openGetConceptMappings2Statement() {
		String query = "SELECT DISTINCT MRREL.CUI1 FROM MRREL, MRCONSO WHERE MRREL.CUI2=? AND MRCONSO.CUI=MRREL.CUI1 AND MRCONSO.CUI<>MRREL.CUI2 AND MRCONSO.SAB=? AND MRCONSO.LAT='ENG' AND (MRREL.REL='RL' OR (MRREL.REL='RO' AND MRREL.RELA IN ('default_mapped_to', 'mapped_to', 'multiply_mapped_to', 'other_mapped_to', 'primary_mapped_to', 'uniquely_mapped_to')));";
		/*  SELECT DISTINCT MRREL.CUI1 FROM MRREL, MRCONSO
    		WHERE MRREL.CUI2='C0025202'
    			AND MRCONSO.CUI=MRREL.CUI1
    			AND MRCONSO.CUI<>MRREL.CUI2
    			AND MRCONSO.SAB='MSH'
    			AND MRCONSO.LAT='ENG'
    			AND (MRREL.REL='RL'
        			OR
        		(MRREL.REL='RO' AND MRREL.RELA IN ('default_mapped_to', 'mapped_to', 'multiply_mapped_to', 'other_mapped_to', 'primary_mapped_to', 'uniquely_mapped_to')
        	)); */
		this.getConceptMappings2Statement = this.prepareSQLStatement(query); 
	}
	
	private void openGetConceptMappings2RestrictedStatement() {
		String query = "SELECT DISTINCT MRREL.CUI1 FROM MRREL, MRCONSO, MRSTY WHERE MRREL.CUI2=? AND MRCONSO.CUI=MRREL.CUI1 AND MRCONSO.CUI=MRSTY.CUI AND MRCONSO.CUI<>MRREL.CUI2 AND MRCONSO.SAB=? AND MRCONSO.LAT='ENG' AND (MRREL.REL='RL' OR (MRREL.REL='RO' AND MRREL.RELA IN ('default_mapped_to', 'mapped_to', 'multiply_mapped_to', 'other_mapped_to', 'primary_mapped_to', 'uniquely_mapped_to')))"+ this.restrictionToSemanticTypes() +";";
		this.getConceptMappings2RestrictedStatement = this.prepareSQLStatement(query); 
	}
	
	/**
	 * Equivalent to this.getConceptMappingsUMLS(localConceptID, localOntologyID, false).
	 */
	public HashSet<String[]> getConceptMappings(String localConceptID, String localOntologyID) throws NonValidLocalConceptIDException, NonValidLocalOntologyIDException{
		return this.getConceptMappingsUMLS(localConceptID, localOntologyID, false);
	}
	
	/**
	 * Returns a set of couples [mappedLocalConceptID, mappingType] saying that the given concept is  
	 * mapped to (according to mappingType) mappedLocalConceptID in the given ontology.
	 * The given ontology can be the one in which the given concept is defined (internal mappings).
	 * Mappings come both from inter-CUI-mappings (i.e., concepts in other ontologies with the same CUI
	 * are considered mapped - mappingType='inter-cui')
	 * and form MRREL table with REL='RL' OR (REL='RO' AND RELA IN ('default_mapped_to', 'mapped_to', 'multiply_mapped_to', 'other_mapped_to', 'primary_mapped_to', 'uniquely_mapped_to')).
	 * Mappings from MRREL are not considered tranistive. 
	 */
	public HashSet<String[]> getConceptMappingsUMLS(String localConceptID, String localOntologyID, boolean filter) throws NonValidLocalConceptIDException, NonValidLocalOntologyIDException{
		HashSet<String[]> mappedConcepts = new HashSet<String[]>();
		if (this.isValidLocalConceptID(localConceptID)){
			if (this.isValidLocalOntologyID(localOntologyID)){
				try{
					ResultSet rSet1;
					ResultSet rSet2;
					if(filter && this.isRestricted){
						this.getConceptMappings1RestrictedStatement.setString(1, this.getConceptCUI(localConceptID));
						this.getConceptMappings1RestrictedStatement.setString(2, localOntologyID);
						rSet1 = this.executeSQLQuery(this.getConceptMappings1RestrictedStatement);
						this.getConceptMappings2RestrictedStatement.setString(1, this.getConceptCUI(localConceptID));
						this.getConceptMappings2RestrictedStatement.setString(2, localOntologyID);
						rSet2 = this.executeSQLQuery(this.getConceptMappings2RestrictedStatement);
					}
					else{
						this.getConceptMappings1Statement.setString(1, this.getConceptCUI(localConceptID));
						this.getConceptMappings1Statement.setString(2, localOntologyID);
						rSet1 = this.executeSQLQuery(this.getConceptMappings1Statement);
						this.getConceptMappings2Statement.setString(1, this.getConceptCUI(localConceptID));
						this.getConceptMappings2Statement.setString(2, localOntologyID);
						rSet2 = this.executeSQLQuery(this.getConceptMappings2Statement);
					}
					// get mappings from CUIs
					// if a result exists, don't even need to take the result
					// checks also that the ontologies are different to avoid returning the same concept
					if(rSet1.first() && !localOntologyID.equals(this.getConceptOntology(localConceptID))){
						String[] couple = new String[2];
						couple[0] = this.createLocalConceptID(localOntologyID, this.getConceptCUI(localConceptID));
						couple[1] = CUI_BASED_MAPPING;
						mappedConcepts.add(couple);	
					}
					rSet1.close();
					// get mappings from MRREL
					while(rSet2.next()){
						String[] couple = new String[2];
						couple[0] = this.createLocalConceptID(localOntologyID, rSet2.getString(1));
						couple[1] = MAPPING_FROM_MRREL;
						mappedConcepts.add(couple);
					}
					rSet2.close();
				}		
				catch (MySQLNonTransientConnectionException e) {
					this.openGetConceptMappings1Statement();
					this.openGetConceptMappings2Statement();
					this.openGetConceptMappings1RestrictedStatement();
					this.openGetConceptMappings1RestrictedStatement();
					return this.getConceptMappingsUMLS(localConceptID, localOntologyID, filter);
				}
				catch (SQLException e) {
					logger.error("** PROBLEM ** Cannot get mappings for localConceptID: "+ localConceptID +" in ontology: "+ localOntologyID+". Empty set returned.", e);
				}
			}
			else{
				throw new NonValidLocalOntologyIDException(localOntologyID);
			}
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		return mappedConcepts;
	}	

	private void openGetAllConceptMappings1Statement() {
		String query = "SELECT DISTINCT MRCONSO.CUI, MRCONSO.SAB FROM MRCONSO WHERE MRCONSO.CUI=? AND MRCONSO.SAB<>?"+ this.restictionToOntologies()+" AND MRCONSO.LAT='ENG';";
		/* SELECT DISTINCT CUI, SAB FROM MRCONSO
    		WHERE CUI='C0025202' AND SAB<>'NCI'
    		AND SAB IN ('MSH', 'AOD', 'SNOMEDCT')
    		AND LAT='ENG'; */
		this.getAllConceptMappings1Statement = this.prepareSQLStatement(query); 
	}
	
	private void openGetAllConceptMappings1RestrictedStatement(){
		String query = "SELECT DISTINCT MRCONSO.CUI, MRCONSO.SAB FROM MRCONSO, MRSTY WHERE MRCONSO.CUI=MRSTY.CUI AND MRCONSO.CUI=? AND MRCONSO.SAB<>?"+ this.restictionToOntologies()+" AND MRCONSO.LAT='ENG'"+ this.restrictionToSemanticTypes() +";";
		this.getAllConceptMappings1RestrictedStatement = this.prepareSQLStatement(query); 
	}
	
	private void openGetAllConceptMappings2Statement() {
		String query = "SELECT DISTINCT MRREL.CUI1, MRCONSO.SAB FROM MRREL, MRCONSO WHERE MRREL.CUI2=? AND MRCONSO.CUI=MRREL.CUI1 AND MRCONSO.CUI<>MRREL.CUI2 AND MRCONSO.SAB<>?"+ this.restictionToOntologies() +" AND MRCONSO.LAT='ENG' AND (MRREL.REL='RL' OR (MRREL.REL='RO' AND MRREL.RELA IN ('default_mapped_to', 'mapped_to', 'multiply_mapped_to', 'other_mapped_to', 'primary_mapped_to', 'uniquely_mapped_to')));";
		/* SELECT DISTINCT MRREL.CUI1, MRCONSO.SAB FROM MRREL, MRCONSO
	    	WHERE MRREL.CUI2='C0025202' AND MRCONSO.CUI=MRREL.CUI1
	    	AND MRCONSO.CUI<>MRREL.CUI2 AND MRCONSO.SAB<>'NCI' 
	    	AND MRCONSO.SAB IN ('NCI', 'MSH', 'SNOMEDCT') AND MRCONSO.LAT='ENG'
	    	AND (MRREL.REL='RL' OR
	    	(MRREL.REL='RO' AND MRREL.RELA IN ('default_mapped_to', 'mapped_to', 'multiply_mapped_to', 'other_mapped_to', 'primary_mapped_to', 'uniquely_mapped_to')
	        	)); */
		this.getAllConceptMappings2Statement = this.prepareSQLStatement(query); 
	}
	
	private void openGetAllConceptMappings2RestrictedStatement(){
		String query = "SELECT DISTINCT MRREL.CUI1, MRCONSO.SAB FROM MRREL, MRCONSO, MRSTY WHERE MRCONSO.CUI=MRSTY.CUI AND MRREL.CUI2=? AND MRCONSO.CUI=MRREL.CUI1 AND MRCONSO.CUI<>MRREL.CUI2 AND MRCONSO.SAB<>?"+ this.restictionToOntologies() +" AND MRCONSO.LAT='ENG'"+ this.restrictionToSemanticTypes() +" AND (MRREL.REL='RL' OR (MRREL.REL='RO' AND MRREL.RELA IN ('default_mapped_to', 'mapped_to', 'multiply_mapped_to', 'other_mapped_to', 'primary_mapped_to', 'uniquely_mapped_to')));";
		this.getAllConceptMappings2RestrictedStatement = this.prepareSQLStatement(query); 
	}
	
	/**
	 * Equivalent to this.getAllConceptMappingsUMLS(localConceptID, false).
	 */
	public HashSet<String[]> getAllConceptMappings(String localConceptID) throws NonValidLocalConceptIDException {
		return this.getAllConceptMappingsUMLS(localConceptID, false);
	}
	
	public HashSet<String[]> getAllConceptMappingsUMLS(String localConceptID, boolean filter) throws NonValidLocalConceptIDException {
		HashSet<String[]> mappedConcepts = new HashSet<String[]>();
		if (this.isValidLocalConceptID(localConceptID)){
			try{
				ResultSet rSet1;
				ResultSet rSet2;
				if(filter && this.isRestricted){
					this.getAllConceptMappings1RestrictedStatement.setString(1, this.getConceptCUI(localConceptID));
					this.getAllConceptMappings1RestrictedStatement.setString(2, this.getConceptOntology(localConceptID));
					rSet1 = this.executeSQLQuery(this.getAllConceptMappings1RestrictedStatement);
					this.getAllConceptMappings2RestrictedStatement.setString(1, this.getConceptCUI(localConceptID));
					this.getAllConceptMappings2RestrictedStatement.setString(2, this.getConceptOntology(localConceptID));
					rSet2 = this.executeSQLQuery(this.getAllConceptMappings2RestrictedStatement);
				}
				else{
					this.getAllConceptMappings1Statement.setString(1, this.getConceptCUI(localConceptID));
					this.getAllConceptMappings1Statement.setString(2, this.getConceptOntology(localConceptID));
					rSet1 = this.executeSQLQuery(this.getAllConceptMappings1Statement);
					this.getAllConceptMappings2Statement.setString(1, this.getConceptCUI(localConceptID));
					this.getAllConceptMappings2Statement.setString(2, this.getConceptOntology(localConceptID));
					rSet2 = this.executeSQLQuery(this.getAllConceptMappings2Statement);
				}
				// get mappings from CUIs
				while(rSet1.next()){
					String[] couple = new String[2];
					couple[0] = this.createLocalConceptID(rSet1.getString(2), rSet1.getString(1));
					couple[1] = CUI_BASED_MAPPING;
					mappedConcepts.add(couple);	
				}
				rSet1.close();
				// get mappings from MRREL
				while(rSet2.next()){
					String[] couple = new String[2];
					couple[0] = this.createLocalConceptID(rSet2.getString(2), rSet2.getString(1));
					couple[1] = MAPPING_FROM_MRREL;
					mappedConcepts.add(couple);
				}
				rSet2.close();
			}		
			catch (MySQLNonTransientConnectionException e) {
				this.openGetAllConceptMappings1Statement();
				this.openGetAllConceptMappings2Statement();
				this.openGetAllConceptMappings1RestrictedStatement();
				this.openGetAllConceptMappings1RestrictedStatement();
				return this.getAllConceptMappingsUMLS(localConceptID, filter);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get mappings for localConceptID: "+ localConceptID +" in all ontologies accessed by the tool. Empty set returned.", e);
			}
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
	return mappedConcepts;
	}	
	
	private void openGetOntologyMappings1Statement() {
		String query = "SELECT DISTINCT m1.CUI, m2.CUI FROM MRCONSO as m1, MRCONSO as m2 WHERE m1.CUI=m2.CUI AND m1.SAB=? AND m2.SAB=? AND m1.LAT='ENG' AND m2.LAT='ENG';";
		/* SELECT DISTINCT m1.CUI, m2.CUI FROM MRCONSO as m1, MRCONSO as m2
    		WHERE m1.CUI=m2.CUI
    			AND m1.SAB='NCI' AND m2.SAB='RXNORM' AND m1.LAT='ENG' AND m2.LAT='ENG'; */
		this.getOntologyMappings1Statement = this.prepareSQLStatement(query); 
	}
	
	private void openGetOntologyMappings1RestrictedStatement() {
		String query = "SELECT DISTINCT m1.CUI, m2.CUI FROM MRCONSO as m1, MRCONSO as m2, MRSTY WHERE m1.CUI=m2.CUI AND m1.CUI=MRSTY.CUI AND m1.SAB=? AND m2.SAB=? AND m1.LAT='ENG' AND m2.LAT='ENG'"+ this.restrictionToSemanticTypes() +";";
		this.getOntologyMappings1RestrictedStatement = this.prepareSQLStatement(query); 
	}
	
	private void openGetOntologyMappings2Statement() {
		String query = "SELECT DISTINCT MRREL.CUI2, MRREL.CUI1 FROM MRREL, MRCONSO as m1, MRCONSO as m2 WHERE MRREL.CUI2=m1.CUI AND MRREL.CUI1=m2.CUI AND m2.CUI<>m1.CUI AND m1.SAB=? AND m2.SAB=? AND m1.LAT='ENG' AND m2.LAT='ENG' AND (MRREL.REL='RL' OR (MRREL.REL='RO' AND MRREL.RELA IN ('default_mapped_to', 'mapped_to', 'multiply_mapped_to', 'other_mapped_to', 'primary_mapped_to', 'uniquely_mapped_to')));";
		/* SELECT DISTINCT MRREL.CUI2, MRREL.CUI1 FROM MRREL, MRCONSO as m1, MRCONSO as m2
	    	WHERE MRREL.CUI2=m1.CUI AND MRREL.CUI1=m2.CUI AND m2.CUI<>m1.CUI
	    		AND m1.SAB='NCI' AND m2.SAB='MSH'  AND m1.LAT='ENG'  AND m2.LAT='ENG'
	    		AND (MRREL.REL='RL'
	        		OR
	        		(MRREL.REL='RO' AND MRREL.RELA IN ('default_mapped_to', 'mapped_to', 'multiply_mapped_to', 'other_mapped_to', 'primary_mapped_to', 'uniquely_mapped_to')
	        	));*/
		this.getOntologyMappings2Statement = this.prepareSQLStatement(query); 
	}
	
	private void openGetOntologyMappings2RestrictedStatement() {
		String query = "SELECT DISTINCT MRREL.CUI2, MRREL.CUI1 FROM MRREL, MRCONSO as m1, MRCONSO as m2, MRSTY, MRSTY as t2 WHERE MRREL.CUI2=m1.CUI AND MRREL.CUI1=m2.CUI AND m2.CUI<>m1.CUI AND m1.CUI=MRSTY.CUI AND m2.CUI=t2.CUI AND m1.SAB=? AND m2.SAB=? AND m1.LAT='ENG' AND m2.LAT='ENG' AND (MRREL.REL='RL' OR (MRREL.REL='RO' AND MRREL.RELA IN ('default_mapped_to', 'mapped_to', 'multiply_mapped_to', 'other_mapped_to', 'primary_mapped_to', 'uniquely_mapped_to')))"+ this.restrictionToSemanticTypes()+" AND MRSTY.TUI=t2.TUI;";
		/* SELECT DISTINCT MRREL.CUI2, MRREL.CUI1 FROM MRREL, MRCONSO as m1, MRCONSO as m2, MRSTY, MRSTY as t2
    		WHERE MRREL.CUI2=m1.CUI AND MRREL.CUI1=m2.CUI AND m2.CUI<>m1.CUI
    			AND m1.CUI=MRSTY.CUI AND m2.CUI=t2.CUI
    			AND m1.SAB='NCI' AND m2.SAB='MSH' AND m1.LAT='ENG' AND m2.LAT='ENG'
    			AND (MRREL.REL='RL'
        			OR
        			(MRREL.REL='RO' AND MRREL.RELA IN ('default_mapped_to', 'mapped_to', 'multiply_mapped_to', 'other_mapped_to', 'primary_mapped_to', 'uniquely_mapped_to')
        			))
    			AND MRSTY.TUI IN ("T047", "T048", "T191", "T037", "T184", "T019", "T020", "T190", "T050", "T200")
    			AND t2.TUI=MRSTY.TUI; */
		this.getOntologyMappings2RestrictedStatement = this.prepareSQLStatement(query); 
	}
	
	/**
	 * Equivalent to this.getOntologyMappingsUMLS(localOntologyID1, localOntologyID2, false).
	 */
	public HashSet<String[]> getOntologyMappings(String localOntologyID1, String localOntologyID2) throws NonValidLocalOntologyIDException {
		return this.getOntologyMappingsUMLS(localOntologyID1, localOntologyID2, false);
	}
	
	public HashSet<String[]> getOntologyMappingsUMLS(String localOntologyID1, String localOntologyID2, boolean filter) throws NonValidLocalOntologyIDException {
		HashSet<String[]> mappedConcepts = new HashSet<String[]>();
		if (this.isValidLocalOntologyID(localOntologyID1)){
			if (this.isValidLocalOntologyID(localOntologyID2)){
				try{
					ResultSet rSet1;
					ResultSet rSet2;
					if(filter && this.isRestricted){
						this.getOntologyMappings1RestrictedStatement.setString(1, localOntologyID1);
						this.getOntologyMappings1RestrictedStatement.setString(2, localOntologyID2);
						rSet1 = this.executeSQLQuery(this.getOntologyMappings1RestrictedStatement);
						this.getOntologyMappings2RestrictedStatement.setString(1, localOntologyID1);
						this.getOntologyMappings2RestrictedStatement.setString(2, localOntologyID2);
						rSet2 = this.executeSQLQuery(this.getOntologyMappings2RestrictedStatement);
					}
					else{
						this.getOntologyMappings1Statement.setString(1, localOntologyID1);
						this.getOntologyMappings1Statement.setString(2, localOntologyID2);
						rSet1 = this.executeSQLQuery(this.getOntologyMappings1Statement);
						this.getOntologyMappings2Statement.setString(1, localOntologyID1);
						this.getOntologyMappings2Statement.setString(2, localOntologyID2);
						rSet2 = this.executeSQLQuery(this.getOntologyMappings2Statement);
					}
					// get mappings from CUIs (if localOntologyID are different)
					if(!localOntologyID1.equals(localOntologyID2)){
						while(rSet1.next()){
							String[] triplet = new String[3];
							triplet[0] = this.createLocalConceptID(localOntologyID1, rSet1.getString(1));
							triplet[1] = this.createLocalConceptID(localOntologyID2, rSet1.getString(2));
							triplet[2] = CUI_BASED_MAPPING;
							mappedConcepts.add(triplet);
						}
						rSet1.close();
					}
					// get mappings from MRREL
					while(rSet2.next()){
						String[] triplet = new String[3];
						triplet[0] = this.createLocalConceptID(localOntologyID1, rSet2.getString(1));
						triplet[1] = this.createLocalConceptID(localOntologyID2, rSet2.getString(2));
						triplet[2] = MAPPING_FROM_MRREL;
						mappedConcepts.add(triplet);
					}
					rSet2.close();
				}
				catch (MySQLNonTransientConnectionException e) {
					this.openGetOntologyMappings1Statement();
					this.openGetOntologyMappings2Statement();
					this.openGetOntologyMappings1RestrictedStatement();
					this.openGetOntologyMappings2RestrictedStatement();
					return this.getOntologyMappingsUMLS(localOntologyID1, localOntologyID2, filter);
				}
				catch (SQLException e) {
					logger.error("** PROBLEM ** Cannot get mappings for ontology: "+ localOntologyID1 +" in ontology: "+ localOntologyID2 +". Empty set returned.", e);
				}
			}
			else{
				throw new NonValidLocalOntologyIDException(localOntologyID2);
			}
		}
		else{
			throw new NonValidLocalOntologyIDException(localOntologyID1);
		}
		return mappedConcepts;
	}
	
	private void openGetAllOntologyMappings1Statement() {
		String query = "SELECT DISTINCT m1.CUI, MRCONSO.CUI, MRCONSO.SAB FROM MRCONSO as m1, MRCONSO WHERE m1.CUI=MRCONSO.CUI AND m1.SAB=? AND MRCONSO.SAB<>?"+ this.restictionToOntologies() +" AND m1.LAT='ENG' AND MRCONSO.LAT='ENG';"; 
		/* SELECT DISTINCT m1.CUI, MRCONSO.CUI, MRCONSO.SAB
     		FROM MRCONSO as m1, MRCONSO 
    		WHERE m1.CUI=MRCONSO.CUI AND m1.SAB='NCI' AND MRCONSO.SAB<>'NCI' 
    			AND MRCONSO.SAB IN ('SNOMEDCT', 'MSH') AND m1.LAT='ENG' AND MRCONSO.LAT='ENG';*/
		this.getAllOntologyMappings1Statement = this.prepareSQLStatement(query); 
	}
	
	private void openGetAllOntologyMappings1RestrictedStatement(){
		String query = "SELECT DISTINCT m1.CUI, MRCONSO.CUI, MRCONSO.SAB FROM MRCONSO as m1, MRCONSO, MRSTY WHERE m1.CUI=MRCONSO.CUI AND m1.CUI=MRSTY.CUI AND m1.SAB=? AND MRCONSO.SAB<>?"+ this.restictionToOntologies() +" AND m1.LAT='ENG' AND MRCONSO.LAT='ENG'"+ this.restrictionToSemanticTypes() +";";
		this.getAllOntologyMappings1RestrictedStatement = this.prepareSQLStatement(query); 
	}
	
	private void openGetAllOntologyMappings2Statement() {
		String query = "SELECT DISTINCT MRREL.CUI2, MRREL.CUI1, MRCONSO.SAB FROM MRREL, MRCONSO as m1, MRCONSO WHERE MRREL.CUI2=m1.CUI AND MRREL.CUI1=MRCONSO.CUI AND MRCONSO.CUI<>m1.CUI AND m1.SAB=? AND MRCONSO.SAB<>?"+ this.restictionToOntologies() +" AND m1.LAT='ENG' AND MRCONSO.LAT='ENG' AND (MRREL.REL='RL' OR (MRREL.REL='RO' AND MRREL.RELA IN ('default_mapped_to', 'mapped_to', 'multiply_mapped_to', 'other_mapped_to', 'primary_mapped_to', 'uniquely_mapped_to')));";
		/* SELECT DISTINCT MRREL.CUI2, MRREL.CUI1, MRCONSO.SAB FROM MRREL, MRCONSO as m1, MRCONSO
				WHERE MRREL.CUI2=m1.CUI AND MRREL.CUI1=MRCONSO.CUI  AND MRCONSO.CUI<>m1.CUI
				AND m1.SAB='NCI'
        		AND MRCONSO.SAB<>'NCI'
        		AND MRCONSO.SAB IN ('SNOMEDCT', 'MSH')
        			AND m1.LAT='ENG' AND MRCONSO.LAT='ENG'
        		AND (MRREL.REL='RL'
	        		OR
	        		(MRREL.REL='RO' AND MRREL.RELA IN ('default_mapped_to', 'mapped_to', 'multiply_mapped_to', 'other_mapped_to', 'primary_mapped_to', 'uniquely_mapped_to')
	        	)); */
        this.getAllOntologyMappings2Statement = this.prepareSQLStatement(query); 
	}
	
	private void openGetAllOntologyMappings2RestrictedStatement() {
		String query = "SELECT DISTINCT MRREL.CUI2, MRREL.CUI1, MRCONSO.SAB FROM MRREL, MRCONSO as m1, MRCONSO, MRSTY, MRSTY as t2 WHERE MRREL.CUI2=m1.CUI AND MRREL.CUI1=MRCONSO.CUI AND MRCONSO.CUI=MRSTY.CUI AND MRCONSO.CUI=t2.CUI AND MRCONSO.CUI<>m1.CUI AND m1.SAB=? AND MRCONSO.SAB<>?"+ this.restictionToOntologies() +" AND m1.LAT='ENG' AND MRCONSO.LAT='ENG' AND (MRREL.REL='RL' OR (MRREL.REL='RO' AND MRREL.RELA IN ('default_mapped_to', 'mapped_to', 'multiply_mapped_to', 'other_mapped_to', 'primary_mapped_to', 'uniquely_mapped_to')))"+ this.restrictionToSemanticTypes() +" AND t2.TUI=MRSTY.TUI;";
		/* SELECT DISTINCT MRREL.CUI2, MRREL.CUI1, MRCONSO.SAB FROM MRREL, MRCONSO as m1, MRCONSO, MRSTY, MRSTY as t2
			WHERE MRREL.CUI2=m1.CUI
	        	AND MRREL.CUI1=MRCONSO.CUI AND MRCONSO.CUI=MRSTY.CUI
	        	AND MRCONSO.CUI=t2.CUI  AND MRCONSO.CUI<>m1.CUI
			AND m1.SAB='NCI' AND MRCONSO.SAB<>'NCI'
	        AND MRCONSO.SAB IN ('SNOMEDCT', 'MSH')
	        AND m1.LAT='ENG' AND MRCONSO.LAT='ENG'
	        AND (MRREL.REL='RL'
		    	OR
		    	(MRREL.REL='RO' AND MRREL.RELA IN ('default_mapped_to', 'mapped_to', 'multiply_mapped_to', 'other_mapped_to', 'primary_mapped_to', 'uniquely_mapped_to')        	))
	       AND MRSTY.TUI IN ("T047", "T048", "T191", "T037", "T184", "T019", "T020", "T190", "T050", "T200")
	    			AND t2.TUI=MRSTY.TUI; */
		this.getAllOntologyMappings2RestrictedStatement = this.prepareSQLStatement(query); 
	}
	
	/**
	 * Equivalent to this.getAllOntologyMappingsUMLS(localOntologyID, false).
	 */
	public HashSet<String[]> getAllOntologyMappings(String localOntologyID) throws NonValidLocalOntologyIDException {
		return this.getAllOntologyMappingsUMLS(localOntologyID, false);
	}
	
	public HashSet<String[]> getAllOntologyMappingsUMLS(String localOntologyID, boolean filter) throws NonValidLocalOntologyIDException {
		HashSet<String[]> mappedConcepts = new HashSet<String[]>();
		if (this.isValidLocalOntologyID(localOntologyID)){
			try{
				ResultSet rSet1;
				ResultSet rSet2;
				if(filter && this.isRestricted){
					this.getAllOntologyMappings1RestrictedStatement.setString(1, localOntologyID);
					this.getAllOntologyMappings1RestrictedStatement.setString(2, localOntologyID);
					rSet1 = this.executeSQLQuery(this.getAllOntologyMappings1RestrictedStatement);
					this.getAllOntologyMappings2RestrictedStatement.setString(1, localOntologyID);
					this.getAllOntologyMappings2RestrictedStatement.setString(2, localOntologyID);
					rSet2 = this.executeSQLQuery(this.getAllOntologyMappings2RestrictedStatement);
				}
				else{
					this.getAllOntologyMappings1Statement.setString(1, localOntologyID);
					this.getAllOntologyMappings1Statement.setString(2, localOntologyID);
					rSet1 = this.executeSQLQuery(this.getAllOntologyMappings1Statement);
					this.getAllOntologyMappings2Statement.setString(1, localOntologyID);
					this.getAllOntologyMappings2Statement.setString(2, localOntologyID);
					rSet2 = this.executeSQLQuery(this.getAllOntologyMappings2Statement);
				}
				// get mappings from CUIs
				while(rSet1.next()){
					String[] triplet = new String[3];
					triplet[0] = this.createLocalConceptID(localOntologyID, rSet1.getString(1));
					triplet[1] = this.createLocalConceptID(rSet1.getString(3), rSet1.getString(2));
					triplet[2] = CUI_BASED_MAPPING;
					mappedConcepts.add(triplet);	
				}
				rSet1.close();
				// get mappings from MRREL
				while(rSet2.next()){
					String[] triplet = new String[3];
					triplet[0] = this.createLocalConceptID(localOntologyID, rSet2.getString(1));
					triplet[1] = this.createLocalConceptID(rSet2.getString(3), rSet2.getString(2));
					triplet[2] = MAPPING_FROM_MRREL;
					mappedConcepts.add(triplet);
				}
				rSet2.close();
			}		
			catch (MySQLNonTransientConnectionException e) {
				this.openGetAllOntologyMappings1Statement();
				this.openGetAllOntologyMappings2Statement();
				this.openGetAllOntologyMappings1RestrictedStatement();
				this.openGetAllOntologyMappings2RestrictedStatement();
				return this.getAllOntologyMappingsUMLS(localOntologyID, filter);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get mappings for ontology: "+ localOntologyID +" and all ontologies accessed by the tool. Empty set returned.", e);
			}
		}
		else{
			throw new NonValidLocalOntologyIDException(localOntologyID);
		}
	return mappedConcepts;
}
		
	// ******************************** ONTOLOGY FUNCTIONS ******************************************

	private void openGetOntologyNameStatement() {
		String query = this.queryMRSABTable("SON");
		this.getOntologyNameStatement = this.prepareSQLStatement(query);
	}
	
	/**
	 * Returns the SON column value in the MRSAB table (always exists).
	 */
	public String getOntologyName(String localOntologyID) throws NonValidLocalOntologyIDException {
		String ontologyName;
		if (this.isValidLocalOntologyID(localOntologyID)){
			try{
				this.getOntologyNameStatement.setString(1, localOntologyID);
				ResultSet rSet = this.executeSQLQuery(this.getOntologyNameStatement);
				if (rSet.first()){
					ontologyName = rSet.getString(1);	
				}
				else{
					ontologyName = "[no name available]"; 
				}
				rSet.close();
			}	
			catch (MySQLNonTransientConnectionException e) {
				this.openGetOntologyNameStatement();
				return this.getOntologyName(localOntologyID);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get column SON (name) from MRSAB for localOntologyID: "+ localOntologyID +". Empty string returned.", e);
				ontologyName = "";
			}
		}
		else{
			throw new NonValidLocalOntologyIDException(localOntologyID);
		}
		return ontologyName;
	}	

	private void openGetOntologyDescriptionStatement() {
		// Remark: by doing the join MRSAB.RSAB=MRDEF.SAB we exclude description of an ontology that may be in another ontology.
		String query = "SELECT DISTINCT DEF FROM MRSAB, MRDEF WHERE MRSAB.RCUI=MRDEF.CUI AND MRSAB.RSAB=MRDEF.SAB AND MRSAB.RSAB=?;";
		this.getOntologyDescriptionStatement = this.prepareSQLStatement(query);
	}
	
	private void openGetOntologyDescription2Statement() {
		String query = "SELECT DISTINCT DEF FROM MRSAB, MRDEF WHERE MRSAB.RCUI=MRDEF.CUI AND MRSAB.RSAB=?;";
		this.getOntologyDescription2Statement = this.prepareSQLStatement(query);
	}
	
	/**
	 * Returns the DEF column value in the MRDEF table if exists (else returns the empty String).
	 */
	public String getOntologyDescription(String localOntologyID) throws NonValidLocalOntologyIDException {
		String ontologyDescription;
		if (this.isValidLocalOntologyID(localOntologyID)){
			try{
				this.getOntologyDescriptionStatement.setString(1, localOntologyID);
				ResultSet rSet = this.executeSQLQuery(this.getOntologyDescriptionStatement);
				if (rSet.first()){
					ontologyDescription = rSet.getString(1);
				}
				else{
					// If there is no description we look in the other ontologies by removing the join condition MRSAB.RSAB=MRDEF.SAB  
					this.getOntologyDescription2Statement.setString(1, localOntologyID);
					ResultSet rSet2 = this.executeSQLQuery(this.getOntologyDescription2Statement);
					if (rSet2.first()){
						ontologyDescription = rSet2.getString(1);
					}
					else{
						ontologyDescription = "[no description available]";
					}
				}
				rSet.close();
			}		
			catch (MySQLNonTransientConnectionException e) {
				this.openGetOntologyDescriptionStatement();
				return this.getOntologyDescription(localOntologyID);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get description from MRSAB and MRDEF for localOntologyID: "+ localOntologyID +". Empty string returned.", e);
				ontologyDescription = "";
			}
		}
		else{
			throw new NonValidLocalOntologyIDException(localOntologyID);
		}
		return ontologyDescription;
	}

	private void openGetOntologyVersionStatement() {
		String query = this.queryMRSABTable("SVER");
		this.getOntologyVersionStatement = this.prepareSQLStatement(query);
	}
	
	/**
	 * Returns the SVER column value in the MRSAB table if exists (else return the empty String);
	 */
	public String getOntologyVersion(String localOntologyID) throws NonValidLocalOntologyIDException {
		String ontologyVersion;
		if (this.isValidLocalOntologyID(localOntologyID)){
			try{
				this.getOntologyVersionStatement.setString(1, localOntologyID);
				ResultSet rSet = this.executeSQLQuery(this.getOntologyVersionStatement);
				if (rSet.first()){
					ontologyVersion = rSet.getString(1);
					if (ontologyVersion ==  null){
						// The only 2 SAB without version are SRC (Metathesaurus Source Terminology Names) and MTH (UMLS Metathesaurus)
						// Thus, we put the UMLS version here.
						
						ontologyVersion = UMLS_VERSION;	
					}
				}
				else{
					ontologyVersion = "[no version available]"; 
				}
				rSet.close();
			}		
			catch (MySQLNonTransientConnectionException e) {
				this.openGetOntologyVersionStatement();
				return this.getOntologyVersion(localOntologyID);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get column SVER (version) from MRSAB for localOntologyID: "+ localOntologyID +". Empty string returned.", e);
				ontologyVersion = "";
			}
		}
		else{
			throw new NonValidLocalOntologyIDException(localOntologyID);
		}
		return ontologyVersion;
	}
	
	private String queryMRSABTable(String columnName){
		StringBuffer queryb = new StringBuffer();
		queryb.append("SELECT DISTINCT ");
		queryb.append(columnName);
		queryb.append(" FROM MRSAB WHERE RSAB=? AND LAT='ENG';");
		return queryb.toString();
	}
	
	private void openGetRootConceptsStatement(){
		String query = this.queryMRSABTable("RCUI");
		this.getRootConceptsStatement = this.prepareSQLStatement(query);
	}
	
	public HashSet<String> getOntologyRootConcepts(String localOntologyID) throws NonValidLocalOntologyIDException {
		HashSet<String> rootConcepts = new HashSet<String>();
		if (this.isValidLocalOntologyID(localOntologyID)){
			// Uses the the direct children of the CUI (MRSAB.RCUI) that identify the SAB (MRSAB.RSAB) in MRSAB.
			String ontologyCUI = new String();
			try{
				this.getRootConceptsStatement.setString(1, localOntologyID);
				ResultSet rSet = this.executeSQLQuery(this.getRootConceptsStatement);
				if (rSet.first()){
					ontologyCUI = rSet.getString(1);	
				}
				rSet.close();
			}		
			catch (MySQLNonTransientConnectionException e) {
				this.openGetRootConceptsStatement();
				return this.getOntologyRootConcepts(localOntologyID);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get column RCUI from MRSAB for localOntologyID: "+ localOntologyID +". Empty string returned.", e);
			}
			try{
				if(ontologyCUI != null){
					rootConcepts.addAll(this.getConceptDirectChildrenUMLS(this.createLocalConceptID(localOntologyID, ontologyCUI), false));
				}
				else {
					logger.info("** PROBLEM ** Cannot get root concepts because no CUI was returned for the localOntologyID: "+ localOntologyID +". Empty set returned.");
				}
			}
			catch (NonValidLocalConceptIDException e) {
				// nothing - Should not happen because we are constructing here the conceptLocalID
				logger.error("This should not happen - getOntologyRootConcepts(String localOntologyID).");
			}
		}
		else{
			throw new NonValidLocalOntologyIDException(localOntologyID);
		}
		return rootConcepts;
	}
	
	private void openGetParentOfSomethingConceptsStatement() {
		String query = "SELECT DISTINCT MRCONSO.CUI FROM MRHIER, MRCONSO WHERE MRHIER.PAUI=MRCONSO.AUI AND LAT='ENG' AND MRHIER.SAB=? AND (MRHIER.RELA='isa' OR MRHIER.RELA IS NULL);";
		this.getParentOfSomethingConceptsStatement= this.prepareSQLStatement(query);
	}
	
	private HashSet<String> getParentOfSomethingConcepts(String localOntologyID) throws NonValidLocalOntologyIDException{
		HashSet<String> parentsOfSomething = new HashSet<String>();
		if (this.isValidLocalOntologyID(localOntologyID)){
			try{
				this.getParentOfSomethingConceptsStatement.setString(1, localOntologyID);
				ResultSet rSet = this.executeSQLQuery(this.getParentOfSomethingConceptsStatement);
				while (rSet.next()){
					parentsOfSomething.add(this.createLocalConceptID(localOntologyID, rSet.getString(1)));
				}
				rSet.close();
			}		
			catch (MySQLNonTransientConnectionException e) {
				e.printStackTrace();
				this.openGetParentOfSomethingConceptsStatement();
				// TODO: here a proble.. I will loop when the query that need to return something fail
				// it's a big geneal bug.... hard too fix
				return this.getParentOfSomethingConcepts(localOntologyID);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get parents of something from MRHIER, MRCONSO for localOntologyID: "+ localOntologyID +". Empty set returned.", e);
			}
		}
		else{
			throw new NonValidLocalOntologyIDException(localOntologyID);
		}
		return parentsOfSomething;
	}
	
	/**
	 * Returns the a set of the ontology leaves (i.e., concepts without children).
	 */
	public HashSet<String> getOntologyLeafConcepts(String localOntologyID) throws NonValidLocalOntologyIDException {
		// Gets all the concepts in that ontologies
		HashSet<String> concepts = this.getLocalConceptIDs(localOntologyID);
		// Removes from the set of all concepts the ones that are parents of something
		concepts.removeAll(this.getParentOfSomethingConcepts(localOntologyID));
		// The difference is done on the Java side because test on the SQL side using NOT EXISTS take hours.
		return concepts;
	}
	
	// ************************************** SEMANTIC TYPE FUNCTIONS *****************************
	
	private void openGetConceptSemanticTypeStatement() {
		String query = "SELECT DISTINCT MRSTY.TUI, MRSTY.STY FROM MRSTY, MRCONSO WHERE MRCONSO.CUI=MRSTY.CUI AND MRCONSO.CUI=? AND MRCONSO.SAB=? AND MRCONSO.LAT='ENG';";
		this.getConceptSemanticTypeStatement = this.prepareSQLStatement(query);
	}
	
	/**
	 * Returns a table of couples [semanticTypeID (UMLS TUI), semanticTypeName (UMLS STY)] for a given concept.
	 */
	public Hashtable<String, String> getConceptSemanticType(String localConceptID) throws NonValidLocalConceptIDException {
		Hashtable<String, String> semanticTypeCouples = new Hashtable<String, String>();
		if (this.isValidLocalConceptID(localConceptID)){
			try{
				this.getConceptSemanticTypeStatement.setString(1, this.getConceptCUI(localConceptID));
				this.getConceptSemanticTypeStatement.setString(2, this.getConceptOntology(localConceptID));
				ResultSet rSet = this.executeSQLQuery(this.getConceptSemanticTypeStatement);
				while (rSet.next()){
					semanticTypeCouples.put(rSet.getString(1), rSet.getString(2));
				}
				rSet.close();
			}
			catch (MySQLNonTransientConnectionException e) {
				this.openGetConceptSemanticTypeStatement();
				return this.getConceptSemanticType(localConceptID);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get semantic type information [TUI, STY] from MRSTY, MRCONSO for localConceptID: "+ localConceptID +". ['', ''] returned.", e);
			}
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		return semanticTypeCouples;
	}
	
	private void openGetSemanticTypeNameStatement() {
		String query = "SELECT DISTINCT STY FROM MRSTY WHERE TUI=?;";
		this.getSemanticTypeNameStatement = this.prepareSQLStatement(query);
	}
	
	public String getSemanticTypeName(String localSemanticTypeID) throws NonValidLocalSemanticTypeIDException {
		String semanticTypeName;
		if (this.isValidLocalSemanticTypeID(localSemanticTypeID)){
			try{
				this.getSemanticTypeNameStatement.setString(1, localSemanticTypeID);
				ResultSet rSet = this.executeSQLQuery(this.getSemanticTypeNameStatement);
				rSet.first();
				semanticTypeName = rSet.getString(1);
				rSet.close();
			}	
			catch (MySQLNonTransientConnectionException e) {
				this.openGetSemanticTypeNameStatement();
				return this.getSemanticTypeName(localSemanticTypeID);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get semantic type name from MRSTY for localSemanticTypeID: "+ localSemanticTypeID +". Empty string returned.", e);
				semanticTypeName = "";
			}
		}
		else{
			throw new NonValidLocalSemanticTypeIDException(localSemanticTypeID);
		}
		return semanticTypeName;
	}
	
	public boolean isRootConcept(String localConceptID) throws NonValidLocalConceptIDException {
		if (this.isValidLocalConceptID(localConceptID)){
			String localOntologyID = this.getConceptOntology(localConceptID);
			return umlsSABs.get(localOntologyID).contains(localConceptID);
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
	}
	
	/**
	 * LocalConceptID must match the following regular expression: [\\p{Upper}\\d]+/C\\d{7}
	 * For instance: MSH/C0000039.
	 * This function do check if the SAB part of the localConceptID is a valid SAB using 
	 * isValidLocalOntologyID.
	 */
	public boolean isValidLocalConceptID(String localConceptID) {
		return isValidLocalConceptIDWithRegExp(localConceptID, "[\\p{Upper}\\d_\\.-]+/C\\d{7}");
	}

	/**
	 * Creates a localConceptID with a given ontology (i.e., UMLS SAB) and UMLS CUI.
	 * LocalConceptID form is: 'SAB/CUI'.
	 */
	public String createLocalConceptID(String localOntologyID, String cui){
		return localOntologyID+"/"+cui;
	}
	
	/**
	 * LocalOntologyID must match the following regular expression: [\\p{Upper}\\d_\\.-]+
	 * For instance: MSH or ICPC2EENG.
	 * This function checks also with UMLS if the given localOntologyID is a valid SAB in UMLS.
	 */
	public boolean isValidLocalOntologyID(String localOntologyID) {
		String regex = "[\\p{Upper}\\d_\\.-]+";
		return (localOntologyID.matches(regex) && umlsSABs.containsKey(localOntologyID));
	}

	/**
	 * LocalSemanticTypeID must match the following regular expression: T\\d{3}
	 * For instance: T116.
	 * This function checks also with UMLS if the given localSemanticID is a valid TUI in UMLS.
	 */
	public boolean isValidLocalSemanticTypeID(String localSemanticTypeID){
		String regex = "T\\d{3}";
		return (localSemanticTypeID.matches(regex) && umlsTUIs.contains(localSemanticTypeID));
	}
	
	/**
	 * Equivalent to mapStringToLocalConceptIDsUMLS(s, localOntologyID, exactMap, true).
	 */
	public HashSet<String> mapStringToLocalConceptIDs(String s, String localOntologyID, boolean exactMap) throws NonValidLocalOntologyIDException {
		return this.mapStringToLocalConceptIDsUMLS(s, localOntologyID, exactMap, true);
	}
	
	private void openExactMapToLocalConceptIDStatement(){
		String query = "SELECT DISTINCT MRCONSO.CUI FROM MRCONSO WHERE MRCONSO.LAT='ENG' AND MRCONSO.SAB=? AND MRCONSO.STR=?;";
		this.exactMapToLocalConceptIDStatement = this.prepareSQLStatement(query);
	}

	private void openMapToLocalConceptIDStatement() {
		String query = "SELECT DISTINCT MRCONSO.CUI FROM MRCONSO WHERE MRCONSO.LAT='ENG' AND MRCONSO.SAB=? AND MRCONSO.STR REGEXP ?;";
		this.mapToLocalConceptIDStatement = this.prepareSQLStatement(query);
	}

	private void openExactMapToLocalConceptIDRestrictedStatement(){
		String query = "SELECT DISTINCT MRCONSO.CUI FROM MRCONSO, MRSTY WHERE MRCONSO.CUI=MRSTY.CUI AND MRCONSO.LAT='ENG' AND MRCONSO.SAB=? AND MRCONSO.STR=?"+ this.restrictionToSemanticTypes() +";";		
		this.exactMapToLocalConceptIDRestrictedStatement = this.prepareSQLStatement(query);
	}

	private void openMapToLocalConceptIDRestrictedStatement() {
		String query = "SELECT DISTINCT MRCONSO.CUI FROM MRCONSO, MRSTY WHERE MRCONSO.CUI=MRSTY.CUI AND MRCONSO.LAT='ENG' AND MRCONSO.SAB=? AND MRCONSO.STR REGEXP ?"+ this.restrictionToSemanticTypes() +";";		
		this.mapToLocalConceptIDRestrictedStatement = this.prepareSQLStatement(query);
	}

	/**
	 * For a given String, returns the set of concepts for which the given String is exactly equal
	 * to one of the concept names in the given ontology by filtering or not
	 * to the tool semantic types, according to the given flag.
	 */
	public HashSet<String> mapStringToLocalConceptIDsUMLS(String s, String localOntologyID, boolean exactMap, boolean filter) throws NonValidLocalOntologyIDException {
		HashSet<String> localConceptIDs = new HashSet<String>();
		if (this.isValidLocalOntologyID(localOntologyID)){
			try{
				ResultSet rSet;
				if(filter && this.isRestricted){
					if(exactMap){
						this.exactMapToLocalConceptIDRestrictedStatement.setString(1, localOntologyID);
						this.exactMapToLocalConceptIDRestrictedStatement.setString(2, this.checkGivenString(s));
						rSet = this.executeSQLQuery(this.exactMapToLocalConceptIDRestrictedStatement);
					}
					else{
						this.mapToLocalConceptIDRestrictedStatement.setString(1, localOntologyID);
						this.mapToLocalConceptIDRestrictedStatement.setString(2, this.checkGivenString(s));
						rSet = this.executeSQLQuery(this.mapToLocalConceptIDRestrictedStatement);
					}
				}
				else{
					if(exactMap){
						this.exactMapToLocalConceptIDStatement.setString(1, localOntologyID);
						this.exactMapToLocalConceptIDStatement.setString(2, this.checkGivenString(s));
						rSet = this.executeSQLQuery(this.exactMapToLocalConceptIDStatement);
					}
					else{
						this.mapToLocalConceptIDStatement.setString(1, localOntologyID);
						this.mapToLocalConceptIDStatement.setString(2, this.checkGivenString(s));
						rSet = this.executeSQLQuery(this.mapToLocalConceptIDStatement);
					}
				}
				while(rSet.next()){
					localConceptIDs.add(this.createLocalConceptID(localOntologyID, rSet.getString(1)));
				}
				rSet.close();
			}		
			catch (MySQLNonTransientConnectionException e) {
				this.openMapToLocalConceptIDStatement();
				this.openExactMapToLocalConceptIDStatement();
				this.openExactMapToLocalConceptIDRestrictedStatement();
				this.openMapToLocalConceptIDRestrictedStatement();
				return this.mapStringToLocalConceptIDsUMLS(s, localOntologyID, exactMap, filter);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot map string: "+ s + " to localConceptIDs from MRCONSO and MRSTY for localOntologyID: "+ localOntologyID +". Empty set returned.", e);
			}
		}
		else{
			throw new NonValidLocalOntologyIDException(localOntologyID);
		}
		return localConceptIDs;
	}
		
	/**
	 * For a given String, returns the set of concepts in all the ontologies accessed by the tool
	 * for which the given String is
	 * (i) exactly equal to one of the concept names if the flag is true;
	 * (ii) contained into one of the concept names if the flag is false;
	 * Overrided for better performance, because it can be done with one SQL query.
	 * Equivalent to mapStringToLocalConceptIDs(s, exactMap, true).
	 */
	@Override
	public HashSet<String> mapStringToLocalConceptIDs(String s, boolean exactMap){
		return mapStringToLocalConceptIDsUMLS(s, exactMap, true);
	}

	private void openAllExactMapToLocalConceptIDStatement(){
		String query = "SELECT DISTINCT MRCONSO.CUI, MRCONSO.SAB FROM MRCONSO WHERE MRCONSO.LAT='ENG'"+ this.restictionToOntologies() +" AND MRCONSO.STR=?;";
		this.allExactMapToLocalConceptIDStatement = this.prepareSQLStatement(query);
	}
	
	private void openAllMapToLocalConceptIDStatement(){
		String query = "SELECT DISTINCT MRCONSO.CUI, MRCONSO.SAB FROM MRCONSO WHERE MRCONSO.LAT='ENG'"+ this.restictionToOntologies() +" AND MRCONSO.STR REGEXP ?;";
		this.allMapToLocalConceptIDStatement = this.prepareSQLStatement(query);
	}
	
	private void openAllExactMapToLocalConceptIDRestrictedStatement(){
		String query = "SELECT DISTINCT MRCONSO.CUI, MRCONSO.SAB FROM MRCONSO, MRSTY WHERE MRCONSO.CUI=MRSTY.CUI AND MRCONSO.LAT='ENG'"+ this.restictionToOntologies() +" AND MRCONSO.STR=?"+ this.restrictionToSemanticTypes() +";";		
		this.allExactMapToLocalConceptIDRestrictedStatement = this.prepareSQLStatement(query);
	}

	private void openAllMapToLocalConceptIDRestrictedStatement(){
		String query = "SELECT DISTINCT MRCONSO.CUI, MRCONSO.SAB FROM MRCONSO, MRSTY WHERE MRCONSO.CUI=MRSTY.CUI AND MRCONSO.LAT='ENG'"+ this.restictionToOntologies() +" AND MRCONSO.STR REGEXP ?"+ this.restrictionToSemanticTypes() +";";		
		this.allMapToLocalConceptIDRestrictedStatement = this.prepareSQLStatement(query);
	}
	
	/**
	 * For a given String, returns the set of concept for which the given String is exactly equal
	 * to one of the concept names in all the ontologies accessed by the tool by filtering or not
	 * to the tool semantic types, according to the given flag.
	 */
	public HashSet<String> mapStringToLocalConceptIDsUMLS(String s, boolean exactMap, boolean filter){
		HashSet<String> localConceptIDs = new HashSet<String>();
		if(!this.getToolOntologies().isEmpty()){
			try{
				ResultSet rSet;
				if(filter && this.isRestricted){
					if(exactMap){
						this.allExactMapToLocalConceptIDRestrictedStatement.setString(1, this.checkGivenString(s));
						rSet = this.executeSQLQuery(this.allExactMapToLocalConceptIDRestrictedStatement);
					}
					else{
						this.allMapToLocalConceptIDRestrictedStatement.setString(1, this.checkGivenString(s));
						rSet = this.executeSQLQuery(this.allMapToLocalConceptIDRestrictedStatement);
					}
				}
				else{
					if(exactMap){
						this.allExactMapToLocalConceptIDStatement.setString(1, this.checkGivenString(s));
						rSet = this.executeSQLQuery(this.allExactMapToLocalConceptIDStatement);
					}
					else{
						this.allMapToLocalConceptIDStatement.setString(1, this.checkGivenString(s));
						rSet = this.executeSQLQuery(this.allMapToLocalConceptIDStatement);
					}
				}
				while(rSet.next()){
					localConceptIDs.add(this.createLocalConceptID(rSet.getString(2), rSet.getString(1)));
				}
				rSet.close();
			}		
			catch (MySQLNonTransientConnectionException e) {
				this.openAllMapToLocalConceptIDStatement();
				this.openAllExactMapToLocalConceptIDStatement();
				this.openAllExactMapToLocalConceptIDRestrictedStatement();
				this.openAllMapToLocalConceptIDRestrictedStatement();
				return this.mapStringToLocalConceptIDsUMLS(s, exactMap, filter);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get localConceptIDs from MRCONSO and MRSTY for all ontologies accessed by the tool. Empty set returned.", e);
			}
		}
		return localConceptIDs;
	}
		
	private String checkGivenString(String s){
		// TODO: Must check for strange characters that can make the query fail. E.g., ' 
		return s;
	}
	
	//*************************************IS-A HIERARCHY FUNCTIONS *****************************
	
	/**
	 * Equivalent to getConceptDirectParentsUMLS(localConceptID, false).
	 */
	public HashSet<String> getConceptDirectParents(String localConceptID) throws NonValidLocalConceptIDException {
		return this.getConceptDirectParentsUMLS(localConceptID, false);
	}
	
	private void openGetConceptDirectParentsStatement() {
		String query = "SELECT DISTINCT MRCONSO.CUI FROM MRCONSO, MRHIER WHERE MRCONSO.AUI=MRHIER.PAUI AND MRCONSO.LAT='ENG' AND MRHIER.CUI=? AND MRHIER.SAB=? AND (MRHIER.RELA='isa' OR MRHIER.RELA IS NULL);";
		this.getConceptDirectParentsStatement = this.prepareSQLStatement(query);
	}
	
	private void openGetConceptDirectParentsRestrictedStatement() {
		String query = "SELECT DISTINCT MRCONSO.CUI FROM MRCONSO, MRHIER, MRSTY WHERE MRCONSO.AUI=MRHIER.PAUI AND MRCONSO.CUI=MRSTY.CUI AND MRCONSO.LAT='ENG' AND MRHIER.CUI=? AND MRHIER.SAB=? AND (MRHIER.RELA='isa' OR MRHIER.RELA IS NULL)"+ this.restrictionToSemanticTypes() +";";
		this.getConceptDirectParentsRestrictedStatement = this.prepareSQLStatement(query);
	}
	
	/**
	 * This function does not remove the cycles (i.e. concepts that are parent of themselves).
	 */
	public HashSet<String> getConceptDirectParentsUMLS(String localConceptID, boolean filter) throws NonValidLocalConceptIDException {
		HashSet<String> directParents = new HashSet<String>();
		if (this.isValidLocalConceptID(localConceptID)){
			try{
				ResultSet rSet;
				if(filter && this.isRestricted){
					this.getConceptDirectParentsRestrictedStatement.setString(1, this.getConceptCUI(localConceptID));
					this.getConceptDirectParentsRestrictedStatement.setString(2, this.getConceptOntology(localConceptID));
					rSet = this.executeSQLQuery(this.getConceptDirectParentsRestrictedStatement);
				}
				else{
					this.getConceptDirectParentsStatement.setString(1, this.getConceptCUI(localConceptID));
					this.getConceptDirectParentsStatement.setString(2, this.getConceptOntology(localConceptID));
					rSet = this.executeSQLQuery(this.getConceptDirectParentsStatement);
				}
				while(rSet.next()){
					directParents.add(this.createLocalConceptID(this.getConceptOntology(localConceptID), rSet.getString(1)));
				}
				rSet.close();
				// to avoid return the parents of roots (that do exists in UMLS and are RCUI in MRSAB)
				// test if the concept is a root concept
				if(this.isRootConcept(localConceptID)){
					// August 2009:
					// We cannot retain only the root concepts in the directParents set 
					// There are some cases where root concepts have parents that are not root concepts!!
					//
					// We need to explicitly check all parents to remove the ones which identify the UMLS SAB
					// (i.e., are present in MRSAB).
					HashSet<String> parentsToRemove = new HashSet<String>();
					// For each direct parents
					for (String parent: directParents){
						// If the direct parent is in MRSAB, it means that it is a CUI that identify a UMLS SAB
						if (umlsRCUIs.contains(this.getConceptCUI(parent))){
							parentsToRemove.add(parent);	
						}
					}
					directParents.removeAll(parentsToRemove);
				}
			}		
			catch (MySQLNonTransientConnectionException e) {
				this.openGetConceptDirectParentsStatement();
				this.openGetConceptDirectParentsRestrictedStatement();
				return this.getConceptDirectParentsUMLS(localConceptID, filter);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get direct parents from MRCONSO, MRHIER for localConceptID: "+ localConceptID +". Empty set is returned.", e);
			}
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		return directParents;
	}

	/**
	 * Equivalent to getConceptDirectChildrenUMLS(localConceptID, false).
	 */
	public HashSet<String> getConceptDirectChildren(String localConceptID) throws NonValidLocalConceptIDException {
		return getConceptDirectChildrenUMLS(localConceptID, false);
	}
	
	private void openGetConceptDirectChildrenStatement() {
		// Example of query: 
		/*
		SELECT DISTINCT m2.CUI FROM MRCONSO AS m1, MRHIER, MRSTY, MRCONSO AS m2 
	    	WHERE m1.AUI=MRHIER.PAUI 
	    	AND m2.CUI=MRSTY.CUI
	    	AND m2.AUI=MRHIER.AUI
	    	AND m1.LAT='ENG'
	    	AND m1.CUI='C1623497' 
	    	AND MRHIER.SAB='SNOMEDCT'
	    	AND (MRHIER.RELA='isa' OR MRHIER.RELA IS NULL)
	    	AND MRSTY.TUI IN ('T200', 'T037', 'T050', 'T190', 'T191', 'T048', 'T020', 'T019', 'T184', 'T047', 'T001'); 
	    */
		String query = "SELECT DISTINCT m2.CUI FROM MRCONSO AS m1, MRHIER, MRCONSO AS m2 WHERE m1.AUI=MRHIER.PAUI AND m2.AUI=MRHIER.AUI AND m2.LAT='ENG' AND m1.CUI=? AND MRHIER.SAB=? AND (MRHIER.RELA='isa' OR MRHIER.RELA IS NULL);";
		this.getConceptDirectChildrenStatement = this.prepareSQLStatement(query);
	}
	
	private void openGetConceptDirectChildrenRestrictedStatement(){
		String query = "SELECT DISTINCT m2.CUI FROM MRCONSO AS m1, MRHIER, MRSTY, MRCONSO AS m2 WHERE m1.AUI=MRHIER.PAUI AND m2.CUI=MRSTY.CUI AND m2.AUI=MRHIER.AUI AND m2.LAT='ENG' AND m1.CUI=? AND MRHIER.SAB=? AND (MRHIER.RELA='isa' OR MRHIER.RELA IS NULL)"+ this.restrictionToSemanticTypes() +";";
		this.getConceptDirectChildrenRestrictedStatement = this.prepareSQLStatement(query);
	}
	
	/**
	 * This function does not remove the cycles (i.e. concepts that are child of themselves).
	 */
	public HashSet<String> getConceptDirectChildrenUMLS(String localConceptID, boolean filter) throws NonValidLocalConceptIDException {
		HashSet<String> directChildren = new HashSet<String>();
		if (this.isValidLocalConceptID(localConceptID)){
			try{
				ResultSet rSet;
				if(filter && this.isRestricted){
					this.getConceptDirectChildrenRestrictedStatement.setString(1, this.getConceptCUI(localConceptID));
					this.getConceptDirectChildrenRestrictedStatement.setString(2, this.getConceptOntology(localConceptID));
					rSet = this.executeSQLQuery(this.getConceptDirectChildrenRestrictedStatement);
				}
				else{
					this.getConceptDirectChildrenStatement.setString(1, this.getConceptCUI(localConceptID));
					this.getConceptDirectChildrenStatement.setString(2, this.getConceptOntology(localConceptID));
					rSet = this.executeSQLQuery(this.getConceptDirectChildrenStatement);
				}
				while(rSet.next()){
					directChildren.add(this.createLocalConceptID(this.getConceptOntology(localConceptID), rSet.getString(1)));
				}
				rSet.close();
			}		
			catch (MySQLNonTransientConnectionException e) {
				this.openGetConceptDirectChildrenStatement();
				this.openGetConceptDirectChildrenRestrictedStatement();
				return this.getConceptDirectChildrenUMLS(localConceptID, filter);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get direct children from MRCONSO, MRHIER for localConceptID: "+ localConceptID +". Empty set returned.", e);
			}
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		return directChildren;
	}
	
	/**
	 * Equivalent to getConceptParentsWithAUIs(localConceptID, false).
	 */
	@Override
	public Hashtable<String, Integer> getConceptParents(String localConceptID) throws NonValidLocalConceptIDException{
		return this.getConceptParentsWithAUIs(localConceptID, false);
		//return this.getConceptParentsWithCUIs(localConceptID, false);
	}
	
	/**
	 * This function implements a recursive manner to get the parents. This function returns all the parent concepts
	 * based on the CUI information in UMLS, not the AUI one.
	 * Equivalent to super.getConceptParents(localConceptID) + filtering.
	 */
	public Hashtable<String, Integer> getConceptParentsWithCUIs(String localConceptID, boolean filter) throws NonValidLocalConceptIDException{
		if(filter && this.isRestricted){
			return this.fiterBySemanticTypes(super.getConceptParents(localConceptID), localConceptID);
		}
		else{
			return super.getConceptParents(localConceptID);
		}
	}
		
	protected void openGetConceptParentsWithAUIsStatement() {
		String query = "SELECT PTR FROM MRHIER WHERE CUI=? AND SAB=? AND (MRHIER.RELA='isa' OR MRHIER.RELA IS NULL);";
		this.getConceptParentsWithAUIsStatement = this.prepareSQLStatement(query);
	}
	
	/**
	 * For a given concept, returns a hashtable that represents the set of concepts of all the parents (recursively to the ontology root)
	 * by filtering or not to the tool semantic types, according to the given flag.
	 * When a parent that has another semantic type than the one the tool manipulates is found, then it is not added
	 * to the table, but the recursion go through it.
	 * Uses the PTR information from the table MRHIER. Note the last concept in the PTR is not returned, because 
	 * it identifies the ontology itself. 
	 * Cycles are not removed, only shortest distance are kept.
	 */
	public Hashtable<String, Integer> getConceptParentsWithAUIs(String localConceptID, boolean filter) throws NonValidLocalConceptIDException{
		Hashtable<String, Integer> conceptParentsTable = new Hashtable<String, Integer>();
		if (this.isValidLocalConceptID(localConceptID)){
			try{
				this.getConceptParentsWithAUIsStatement.setString(1, this.getConceptCUI(localConceptID));
				this.getConceptParentsWithAUIsStatement.setString(2, this.getConceptOntology(localConceptID));
				ResultSet rSet = this.executeSQLQuery(this.getConceptParentsWithAUIsStatement);
				String parent;
				Integer level;
				while(rSet.next()){
					// for each PTR parse the PTR and put the results in the main table
					// we cannot use putAll because it will keep the level founded in the last PTR, and
					// we want to keep only the shortest.
					//conceptParentsTable.putAll(this.ptrParsing(rSet.getString(1), this.getConceptOntology(localConceptID), filter));
					if(rSet.getString(1) !=null){
						// if the PTR returned is not null
						for(Iterator<Map.Entry<String, Integer>> it = this.ptrParsing(rSet.getString(1), this.getConceptOntology(localConceptID), filter).entrySet().iterator(); it.hasNext();){
							Map.Entry<String, Integer> entry = it.next();
							parent = entry.getKey();
							level = entry.getValue();
							// if the current entry is already in the main table
							if (conceptParentsTable.containsKey(parent)){
								// if the level of the current entry in the current ptr is smaller than the one already present
								if (entry.getValue() < conceptParentsTable.get(parent)){
									conceptParentsTable.put(parent, level);
								}
							}
							else{
								// checks for cycle that may happen when merging PTRs
								// commented May 07, 2009 because that should not happen here
								//if(!localConceptID.equals(parent)){
									conceptParentsTable.put(parent, level);	
								//}
							}
						}
					}
				}
				rSet.close();
			}		
			catch (MySQLNonTransientConnectionException e) {
				this.openGetConceptParentsWithAUIsStatement();
				return this.getConceptParentsWithAUIs(localConceptID, filter);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get parents from MRHIER for localConceptID: "+ localConceptID +". Empty set is returned.", e);
			}
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		return conceptParentsTable;
	}
		
	private void openPtrParsingStatement() {
		String query = "SELECT MRCONSO.CUI FROM MRCONSO WHERE MRCONSO.AUI=?;";
		this.ptrParsingStatement = this.prepareSQLStatement(query);
	}
	
	private void openPtrParsingRestrictedStatement() {
		String query = "SELECT MRCONSO.CUI FROM MRCONSO, MRSTY WHERE MRCONSO.CUI=MRSTY.CUI AND MRCONSO.AUI=?"+ this.restrictionToSemanticTypes() +";";
		this.ptrParsingRestrictedStatement = this.prepareSQLStatement(query);
	}
	
	/**
	 * Returns the PTRs (without the first concept) under the form of a Hashtable<localConceptID, level>.  
	 */
	protected Hashtable<String, Integer> ptrParsing(String ptr, String localOntologyID, boolean filter){
		Hashtable<String, Integer> parentsTable = new Hashtable<String, Integer>();
		// splits the given PTR to an array of AUIs
		String[] auisArray = ptr.split("\\.");
		try{
			// for each AUI in the PTR, except the first one (the first one identifies the ontology)
			String aui;
			String cui;
			for (int i=1; i<auisArray.length; i++){
				aui = auisArray[i];
				// executes the query
				ResultSet rSet;
				if(filter && this.isRestricted){
					this.ptrParsingRestrictedStatement.setString(1, aui);
					rSet = this.executeSQLQuery(this.ptrParsingRestrictedStatement);
				}
				else{
					this.ptrParsingStatement.setString(1, aui);
					rSet = this.executeSQLQuery(this.ptrParsingStatement);
				}
				if(rSet.first()){
					cui = rSet.getString(1);
					// if a cui is returned
					// adds the concept to the main table with the right level 
					parentsTable.put(this.createLocalConceptID(localOntologyID, cui), auisArray.length-i);	
				}
				rSet.close();
			}
		}
		catch (MySQLNonTransientConnectionException e) {
			this.openPtrParsingStatement();
			this.openPtrParsingRestrictedStatement();
			return this.ptrParsing(ptr, localOntologyID, filter);
		}
		catch (SQLException e) {
			logger.error("** PROBLEM ** Cannot parse the folowing PTR: "+ ptr +". Empty table is returned.", e);
		}
		return parentsTable;
	}
	
	/**
	 * Returns the PTR or PTL under the form of a ArrayList<localConceptID>.  
	 */
	protected static ArrayList<String> ptrlParsingAsList(String ptrl){
		ArrayList<String> ptrlList = new ArrayList<String>();
		// splits the given PTRL to an array
		String[] ptrsArray = ptrl.split("\\.");
		if(ptrsArray.length>0 && ptrsArray !=null){
			for(int i=0; i<ptrsArray.length; i++){
				ptrlList.add(ptrsArray[i]);
			}
		}
		return ptrlList;
	}
	
	private int ptrSize(String ptr){
		// splits the given PTR to an array of AUIs
		String[] auisArray = ptr.split("\\.");
		return auisArray.length;
	}

	private static String substractAndInversePtr(String longPtr, String shortPtr){
		// splits the given PTRs to an arrays of AUIs
		String[] longArray = longPtr.split("\\.");
		String[] shortArray = shortPtr.split("\\.");
		StringBuffer ptl = new StringBuffer();
		
		for(int i=longArray.length-1; i>shortArray.length-1; i--){
			ptl.append(longArray[i]);
			if(i>shortArray.length-1+1){
				ptl.append(".");
			}
		}
		return ptl.toString();
	}
	
	/**
	 * Equivalent to getConceptChildrenWithAUIs(localConceptID, false).
	 */
	@Override
	public Hashtable<String, Integer> getConceptChildren(String localConceptID) throws NonValidLocalConceptIDException{
		return this.getConceptChildrenWithAUIs(localConceptID, false);
		//return this.getConceptChildrenWithCUIs(localConceptID, false);
	}
		
	/**
	 * This function implements a recursive manner to get the children. This function returns all the children concepts
	 * based on the CUI information in UMLS, not the AUI one.
	 * Equivalent to super.getConceptChildren(localConceptID) + filtering.
	 */
	public Hashtable<String, Integer> getConceptChildrenWithCUIs(String localConceptID, boolean filter) throws NonValidLocalConceptIDException{
		if(filter && this.isRestricted){
			return this.fiterBySemanticTypes(super.getConceptChildren(localConceptID), localConceptID);
		}
		else{
			return super.getConceptChildren(localConceptID);
		}
	}
	
	private void openGetConceptChildrenWithAUIsStatement() {
		// Example of query:
		// SELECT DISTINCT CONCAT(CONCAT_WS('.', MRHIER.PTR, MRHIER.AUI), '%') FROM MRCONSO, MRHIER 
        // WHERE MRCONSO.AUI=MRHIER.AUI AND MRCONSO.CUI='C0025202' AND MRCONSO.SAB='NCI' AND MRCONSO.LAT='ENG' AND (MRHIER.RELA='isa' OR MRHIER.RELA IS NULL);
		String query = "SELECT DISTINCT CONCAT(CONCAT_WS('.', MRHIER.PTR, MRHIER.AUI), '%') FROM MRCONSO, MRHIER WHERE MRCONSO.AUI=MRHIER.AUI AND MRCONSO.CUI=? AND MRCONSO.SAB=? AND MRCONSO.LAT='ENG' AND (MRHIER.RELA='isa' OR MRHIER.RELA IS NULL);";
		this.getConceptChildrenWithAUIsStatement = this.prepareSQLStatement(query);
	}
	
	private void openGetConceptChildrenWithAUIs2Statement() {
		String query = "SELECT DISTINCT MRHIER.CUI, MRHIER.PTR FROM MRHIER WHERE MRHIER.PTR LIKE ? AND (MRHIER.RELA='isa' OR MRHIER.RELA IS NULL);";
		this.getConceptChildrenWithAUIs2Statement = this.prepareSQLStatement(query);
	}

	private void openGetConceptChildrenWithAUIs2RestrictedStatement() {
		String query = "SELECT DISTINCT MRHIER.CUI, MRHIER.PTR FROM MRHIER, MRSTY WHERE MRHIER.CUI=MRSTY.CUI MRHIER.PTR LIKE ? AND (MRHIER.RELA='isa' OR MRHIER.RELA IS NULL)"+ this.restrictionToSemanticTypes() +";";
		this.getConceptChildrenWithAUIs2RestrictedStatement = this.prepareSQLStatement(query);
	}
	
	/**
	 * For a given concept, returns a hashtable that represents the set of concepts of all the children (recursively to the ontology leaves)
	 * by filtering or not to the tool semantic types, according to the given flag.
	 * When a child that has another semantic type than the one the tool manipulates is found, then it is not added
	 * to the table, but the recursion go through it.
	 */
	public Hashtable<String, Integer> getConceptChildrenWithAUIs(String localConceptID, boolean filter) throws NonValidLocalConceptIDException{
		// Remark: there is no equivalent to the PTR in MRHIER that can be used. 
		// Therefore, for the given concept, we look up for the PTRs of the given concept and concat all the possible AUIs for this concept
		// After we can lookup in MRHIER the PTRs that contain (substring) the generated PTRs.
		Hashtable<String, Integer> conceptChildrenTable = new Hashtable<String, Integer>();
		if (this.isValidLocalConceptID(localConceptID)){
			/* This commented version does the function in one query but for a reason (?) 
			 * indexes are not used rightly on this query... we split that in 2 subqueries that use the indexes rightly
			// select the children CUIs and PTRs in order to be able to process the level
			// example of query 
			//SELECT DISTINCT CUI, PTR, originalPTR FROM MRHIER, 
			//	(SELECT DISTINCT CONCAT(CONCAT_WS('.', MRHIER.PTR, MRHIER.AUI), '%') AS genPTR, MRHIER.PTR AS originalPTR
			//			FROM MRCONSO, MRHIER 
			//			WHERE MRCONSO.AUI=MRHIER.AUI 
			//			AND MRCONSO.CUI='C0025202' 
			//				AND MRCONSO.SAB='NCI' 
			//					AND MRCONSO.LAT='ENG') AS genPTRs
			//	WHERE PTR LIKE genPTR;
			
			StringBuffer queryb = new StringBuffer();
			queryb.append("SELECT DISTINCT CUI, PTR, originalPTR FROM MRHIER, (SELECT DISTINCT CONCAT(CONCAT_WS('.', MRHIER.PTR, MRHIER.AUI), '%') AS genPTR, MRHIER.PTR AS originalPTR FROM MRCONSO, MRHIER WHERE MRCONSO.AUI=MRHIER.AUI AND MRCONSO.CUI='");
			queryb.append(this.getConceptCUI(localConceptID)); 
			queryb.append("' AND MRCONSO.SAB='");
			queryb.append(this.getConceptOntology(localConceptID));
			queryb.append("' AND MRCONSO.LAT='ENG') AS genPTRs WHERE PTR LIKE genPTR;");
			try{
				ResultSet rSet = this.executeSQLQuery(this.executeSQLQuery(queryb.toString());
				while(rSet.next()){
					// for each CUI put the corrsponding children concept in the table with the right level  
					conceptChildrenTable.put(this.createLocalConceptID(this.getConceptOntology(localConceptID), rSet.getString(1)), this.ptrSize(rSet.getString(2)) - this.ptrSize(rSet.getString(3)));
				}
				rSet.close();
				this.closeStatement();
			}
			
			* The query works but for one reason, the index on the PTR column is not used...  therefore we have to split that into 2 queries
			* To be seen later.
			*/
			
			// generate the PTRs
			try{
				this.getConceptChildrenWithAUIsStatement.setString(1, this.getConceptCUI(localConceptID));
				this.getConceptChildrenWithAUIsStatement.setString(2, this.getConceptOntology(localConceptID));
				ResultSet rSet = this.executeSQLQuery(this.getConceptChildrenWithAUIsStatement);
				while(rSet.next()){
					// for each PTRs look up for the CUI that have this PTR as substring and filter by semantic type  
					String genPTR = rSet.getString(1); 
					ResultSet rSet2;
					if(filter && this.isRestricted){
						this.getConceptChildrenWithAUIs2RestrictedStatement.setString(1, genPTR);
						rSet2 = this.executeSQLQuery(this.getConceptChildrenWithAUIs2RestrictedStatement);
					}
					else{
						this.getConceptChildrenWithAUIs2Statement.setString(1, genPTR);
						rSet2 = this.executeSQLQuery(this.getConceptChildrenWithAUIs2Statement);
					}
					while(rSet2.next()){
						conceptChildrenTable.put(this.createLocalConceptID(this.getConceptOntology(localConceptID), rSet2.getString(1)), this.ptrSize(rSet2.getString(2)) - this.ptrSize(genPTR) + 1);
					}
					rSet2.close();
				}
				rSet.close();
			}
			catch (MySQLNonTransientConnectionException e) {
				this.openGetConceptChildrenWithAUIsStatement();
				this.openGetConceptChildrenWithAUIs2Statement();
				this.openGetConceptChildrenWithAUIs2RestrictedStatement();
				return this.getConceptChildrenWithAUIs(localConceptID, filter);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get parents from MRHIER for localConceptID: "+ localConceptID +". Empty set is returned.", e);
			}
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		return conceptChildrenTable;
	}
	
	/**
	 * Returns a set of path to the roots (PTRs) for a given concept.
	 * PTRs are represented as a list of localConceptIDs, separated by periods (.)
	 * The first one in the list is top of the hierarchy;
	 * the last one in the list is the direct parent of the concept.
	 * Equivalent to getConceptPtrsWithAUIs(localConceptID).
	 */
	@Override
	public HashSet<String> getConceptPtrs(String localConceptID) throws NonValidLocalConceptIDException {
		return getConceptPtrsWithAUIs(localConceptID);
	}
	
	/**
	 * Returns a set of path to the leaves (PTLs) for a given concept.
	 * PTLs are represented as a list of localConceptIDs, separated by periods (.)
	 * The first one in the list is the leaf in the hierarchy;
	 * the last one in the list is the direct child of the concept.
	 * Equivalent to getConceptPtlsFromAuiPtrs(localConceptID).
	 */
	public HashSet<String> getConceptPtls(String localConceptID) throws NonValidLocalConceptIDException {
		return getConceptPtlsFromAuiPtrs(localConceptID);
	}
	
	/**
	 * PTRs are recursively computed based on the MRHIER direct parent information.
	 * NOT IMPLEMENTED.
	 */
	public HashSet<String> getConceptPtrsWithCUIs(String localConceptID) throws NonValidLocalConceptIDException {
		// main list to contain the ptrs
		HashSet<String> ptrs = new HashSet<String>();
		if (this.isValidLocalConceptID(localConceptID)){
	
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		return ptrs;
	}

	/**
	 * PTLs are recursively computed based on the MRHIER direct parent information.
	 * NOT IMPLEMENTED.
	 */
	public HashSet<String> getConceptPtlsWithCUIs(String localConceptID) throws NonValidLocalConceptIDException {
		// main list to contain the ptls
		HashSet<String> ptls = new HashSet<String>();
		if (this.isValidLocalConceptID(localConceptID)){
	
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		return ptls;
	}
	
	/* version qui marche pas
	public HashSet<String> getConceptPtrsWithCUIs(String localConceptID) throws NonValidLocalConceptIDException {
		// main table <lastParentProcessed, HashSet<ptr>> to contain the ptrs
		Hashtable<String, HashSet<String>> ptrs = new Hashtable<String, HashSet<String>>();
		if (this.isValidLocalConceptID(localConceptID)){
			// we use an arraylist to store the concept on which to do the iteration
			// in order to be able to use ListIterator that can be dynamically changed
			ArrayList<String> conceptParentsList = new ArrayList<String>();
			// used to store the concept already processed
			HashSet<String> alreadyProcessed = new HashSet<String>();
			
			// we start with the concept to process in the list but not in the table
			conceptParentsList.add(localConceptID);
			
			// used to temporary store the parents from one level and add them to the iterator in one shot
			ArrayList<String> tempConceptParentsList = new ArrayList<String>();
			
			String currentConcept;
			HashSet<String> currentConceptParents;
			
			// for each concept in the list  
			ListIterator<String> it = conceptParentsList.listIterator();
			while(it.hasNext()){
				currentConcept = it.next();
				System.out.println("currentConcept: "+currentConcept);
				// removes the currentConcept from the main list
				it.remove();
				// compute the parents of the current concept
				currentConceptParents = this.getConceptDirectParents(currentConcept);
				// for each parents of the currentConcept
				for (String currentParent: currentConceptParents){
					System.out.println("\tcurrentParent: "+currentParent);
					// if there is not already a prt with key = currentConcept
					if (!ptrs.containsKey(currentConcept)){
						// adds a PTR into the main table 
						// if the currentParent!=currentConcept (cycle)
						if(!currentParent.equals(currentConcept)){
							HashSet<String> ptrSet = new HashSet<String>(1);
							ptrSet.add(currentParent);
							ptrs.put(currentParent,ptrSet);
							System.out.println("\tptr added (if=true): "+ currentParent + "=" + ptrSet);
						}
					}
					else{
						// updates the existing PTR into the main table 
						// if the currentParent!=currentCOncept (cycle)
						if(!currentParent.equals(currentConcept)){
							HashSet<String> ptrSet = new HashSet<String>();
							// adds to the ptrSetToUse the ptrs for the current concept
							HashSet<String> ptrSetToUSe = ptrs.get(currentConcept);
							
							// adds to the ptrSetToUSe the ptrs for the current parent (if a shorter path have been already traversed)
							if(ptrs.containsKey(currentParent)){
								ptrSetToUSe.addAll(ptrs.get(currentParent));
							}
							for (String ptr: ptrSetToUSe){
								ptrSet.add(currentParent+"."+ptr);
							}
							ptrs.put(currentParent, ptrSet);
							System.out.println("\tptr added (if=false): "+ currentParent + "=" + ptrSet);
						}
					}
					tempConceptParentsList.add(currentParent);
				}
				// adds the current concept to the one already processed
				alreadyProcessed.add(currentConcept);
				// if there is no more concept at that level to process
				if(conceptParentsList.isEmpty()){
					// adds all the parents to the main list
					for (String currentParent: tempConceptParentsList){
						if(!alreadyProcessed.contains(currentParent)){
							it.add(currentParent);
						}
					}
					tempConceptParentsList.clear();
					// refreshes the listIterator 
					it = conceptParentsList.listIterator();
				}
				//removes the old PTRs from the main table
				//ptrs.remove(currentConcept);
				System.out.println("ptrs: "+ptrs);
				System.out.println("--");
			}
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		System.out.println(ptrs);
		HashSet<String> finalPtrs = new HashSet<String>();
		for (HashSet<String> ptrSet: ptrs.values()){
			finalPtrs.addAll(ptrSet);
		}
		return finalPtrs;
	}*/
	/**
	 * PTRs are selected in MRHIER and then transformed with localConceptIDs.
	 * Cycles that may appen by going from AUI to localConceptID are not removed.
	 */
	public HashSet<String> getConceptPtrsWithAUIs(String localConceptID) throws NonValidLocalConceptIDException {
		HashSet<String> ptrs = new HashSet<String>();
		if (this.isValidLocalConceptID(localConceptID)){
			try{
				this.getConceptParentsWithAUIsStatement.setString(1, this.getConceptCUI(localConceptID));
				this.getConceptParentsWithAUIsStatement.setString(2, this.getConceptOntology(localConceptID));
				ResultSet rSet = this.executeSQLQuery(this.getConceptParentsWithAUIsStatement);
				while(rSet.next()){
					// for each PTR parse the PTR and put the results in the main set
					// if the PTR returned is not null
					if(rSet.getString(1) !=null){
						// truncate the PTR because first concept identifies the ontology
						ptrs.add(transformAuiPtrl(truncateFirstConcept(rSet.getString(1)), this.getConceptOntology(localConceptID)));
					}
				}
				rSet.close();
			}		
			catch (MySQLNonTransientConnectionException e) {
				this.openGetConceptParentsWithAUIsStatement();
				this.openPtrParsingStatement();
				return this.getConceptPtrsWithAUIs(localConceptID);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get ptrs from MRHIER for localConceptID: "+ localConceptID +". Empty set is returned.", e);
			}
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		return ptrs;
	}	
	
	private String transformAuiPtrl(String auiPtr, String localOntologyID){
		StringBuffer buffer = new StringBuffer();
		// splits the given PTR to an array of AUIs
		String[] auisArray = auiPtr.split("\\.");
			String aui;
			String cui;
			for (int i=0; i<auisArray.length; i++){
				aui = auisArray[i];
				// executes the query
				try {
					this.ptrParsingStatement.setString(1, aui);
					ResultSet rSet = this.executeSQLQuery(this.ptrParsingStatement);
					if(rSet.first()){
						cui = rSet.getString(1);
						// 	if a cui is returned
						// 	adds the concept to string buffer that forms the localConceptID ptr
						buffer.append(createLocalConceptID(localOntologyID, cui));	
						if(i<auisArray.length-1){
							buffer.append(".");
						}
					}
					rSet.close();
				}
				catch (MySQLNonTransientConnectionException e) {
					this.openPtrParsingStatement();
					return this.transformAuiPtrl(auiPtr, localOntologyID);
				}
				catch (SQLException e) {
					logger.error("** PROBLEM ** Cannot parse the PTR:"+ auiPtr+" to PTR of localConceptIDs. Empty string is returned.", e);
				}
			} // ends of AUIs processing
			return buffer.toString();
	}
	
	private String truncateFirstConcept(String ptrl){
		// splits the given PTR to an array
		String[] ptrArray = ptrl.split("\\.");
		StringBuffer buffer = new StringBuffer();
		for (int i=1; i<ptrArray.length; i++){
			buffer.append(ptrArray[i]);
			if(i<ptrArray.length-1){
				buffer.append(".");
			}
		}
		return buffer.toString();		
	}
	
	/**
	 * PTLs are reconstructed (and then transformed with localConceptIDs) thanks to the PTRs selected in MRHIER.
	 * The given set of leave is used in the recontsruction.
	 */
	public HashSet<String> getConceptPtlsFromAuiPtrs(String localConceptID, boolean filter, HashSet<String> ontologyLeaves) throws NonValidLocalConceptIDException {
		// This methods look up for the PTRs of the given concept and concat all the possible AUIs for this concept
		// Then lookup in MRHIER the PTRs that contain (substring) the generated PTRs.
		// The PTLs returned are those PTRs truncated of the PTRs of the given concept.
		HashSet<String> ptls = new HashSet<String>();
		String childConcept;
		String auiPtl;
		String lcuiPtl;
		String lcuiPtlFinal;
		if (this.isValidLocalConceptID(localConceptID)){
			// generate the PTRs
			try{
				this.getConceptChildrenWithAUIsStatement.setString(1, this.getConceptCUI(localConceptID));
				this.getConceptChildrenWithAUIsStatement.setString(2, this.getConceptOntology(localConceptID));
				ResultSet rSet = this.executeSQLQuery(this.getConceptChildrenWithAUIsStatement);
				while(rSet.next()){
					// for each PTRs look up for the CUI that have this PTR as substring and filter by semantic type  
					String genPTR = rSet.getString(1);
					ResultSet rSet2;
					if(filter && this.isRestricted){
						this.getConceptChildrenWithAUIs2RestrictedStatement.setString(1, genPTR);
						rSet2 = this.executeSQLQuery(this.getConceptChildrenWithAUIs2RestrictedStatement);
					}
					else{
						this.getConceptChildrenWithAUIs2Statement.setString(1, genPTR);
						rSet2 = this.executeSQLQuery(this.getConceptChildrenWithAUIs2Statement);
					}
					while(rSet2.next()){
						// for each of the children returned
						childConcept = createLocalConceptID(this.getConceptOntology(localConceptID), rSet2.getString(1));
						// if the children is a leave
						if(ontologyLeaves.contains(childConcept)){
							// substracts and inverses the ptr of the leave to get a ptl for the given concept
							auiPtl = substractAndInversePtr(rSet2.getString(2), genPTR.substring(0, genPTR.length()-1));
							// parses the PTL of AUI generated to a PTL of localconceptID
							lcuiPtl = transformAuiPtrl(auiPtl, this.getConceptOntology(localConceptID)); 
							// adds to that ptl the leave to get the final PTL 
							if(lcuiPtl.length()!=0){
								lcuiPtlFinal = childConcept.concat("."+lcuiPtl);
							}
							else{
								lcuiPtlFinal = childConcept.concat(lcuiPtl);	
							}
							ptls.add(lcuiPtlFinal);
						}
					}
					rSet2.close();
				}
				rSet.close();
			}
			catch (MySQLNonTransientConnectionException e) {
				this.openGetConceptChildrenWithAUIsStatement();
				this.openGetConceptChildrenWithAUIs2Statement();
				this.openGetConceptChildrenWithAUIs2RestrictedStatement();
				return this.getConceptPtlsFromAuiPtrs(localConceptID, filter, ontologyLeaves);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get parents from MRHIER for localConceptID: "+ localConceptID +". Empty set is returned.", e);
			}
		}
		else{
			throw new NonValidLocalConceptIDException(localConceptID);
		}
		return ptls;
	}
		
	/**
	 * PTLs are reconstructed (and then transformed with localConceptIDs) thanks to the PTRs selected in MRHIER.
	 * The given set of leave necessary is computed and then getConceptPtlsFromAuiPtrs is called.
	 */
	public HashSet<String> getConceptPtlsFromAuiPtrs(String localConceptID) throws NonValidLocalConceptIDException {
		HashSet<String> ontologyLeaves = new HashSet<String>();
		try {
			logger.info("Computing ontology leaves...");
			ontologyLeaves.addAll(this.getOntologyLeafConcepts(this.getConceptOntology(localConceptID)));
			logger.info("done");
			return getConceptPtlsFromAuiPtrs(localConceptID, false, ontologyLeaves);
		} catch (NonValidLocalOntologyIDException e) {
			logger.error("Should not append - getConceptPtlsFromAuiPtrs");
			return ontologyLeaves;
		}
	}
	
	private void openFilterBySemanticTypesStatement(){
		String query = "SELECT DISTINCT MRSTY.TUI FROM MRCONSO, MRSTY WHERE MRCONSO.CUI=MRSTY.CUI AND MRCONSO.LAT='ENG' AND MRCONSO.CUI=? AND MRCONSO.SAB=?;";
		this.filterBySemanticTypesStatement = this.prepareSQLStatement(query);
	}
 	
	private Hashtable<String, Integer> fiterBySemanticTypes(Hashtable<String, Integer> table, String localConceptID) throws NonValidLocalConceptIDException {
		Hashtable<String, Integer> filteredTable = new Hashtable<String, Integer>();
		boolean toBeKept = false;
		for(String concept: table.keySet()){
			try{
				this.filterBySemanticTypesStatement.setString(1, this.getConceptCUI(concept));
				this.filterBySemanticTypesStatement.setString(2, this.getConceptOntology(localConceptID));
				ResultSet rSet = this.executeSQLQuery(this.filterBySemanticTypesStatement);
				while(rSet.next()){
					// for each possible TUIs returned for a localConceptID
					if(this.toolSemanticTypes.contains(rSet.getString(1))){
						toBeKept = true;
						break;
					}
				}
				rSet.close();
			}
			catch (MySQLNonTransientConnectionException e) {
				this.openFilterBySemanticTypesStatement();
				return this.fiterBySemanticTypes(table, localConceptID);
			}
			catch (SQLException e) {
				logger.error("** PROBLEM ** Cannot get TUIs from MRCONSO, MRSTY for localConceptID: "+ concept +". Empty set is returned.", e);
			}
			if(toBeKept){
				filteredTable.put(concept, table.get(concept));
			}
		}
		return filteredTable;
	}
	
	// ********************************* QUERY PIECES ****************************************
	
	protected String restrictionToSemanticTypes(){
		StringBuffer queryb = new StringBuffer();
		if (!this.toolSemanticTypes.isEmpty()){
			String localSemanticTypeID;
			queryb.append(" AND MRSTY.TUI IN (");
			for (Iterator<String> it = this.toolSemanticTypes.iterator(); it.hasNext();){ 
				localSemanticTypeID = it.next();
				queryb.append("'");
				queryb.append(localSemanticTypeID);
				queryb.append("'");
				if(it.hasNext()){
					queryb.append(", ");
				}
			}
			queryb.append(")");
		}
		return queryb.toString();
	}
	
	protected String restictionToOntologies(){
		StringBuffer queryb = new StringBuffer();
		if(!this.getToolOntologies().isEmpty()){
			String localOntologyID;
			queryb.append(" AND MRCONSO.SAB IN (");
			for (Iterator<String> it = this.getToolOntologies().iterator(); it.hasNext();){ 
				localOntologyID = it.next();
				queryb.append("'");
				queryb.append(localOntologyID);
				queryb.append("'");
				if(it.hasNext()){
					queryb.append(", ");
				}
			}
			queryb.append(")");
		}
		return queryb.toString();
	}

	// ******************************* DISPLAY FUNCTIONS *****************************************

	/**
	 * Returns a String that contains all the information about a concept (GUI function).
	 */
	@Override
	public String conceptInformation(String localConceptID, boolean withAllParents, boolean withAllChildren) throws NonValidLocalConceptIDException {
		StringBuffer buffer = new StringBuffer();
		buffer.append(super.conceptInformation(localConceptID, withAllParents, withAllChildren));
		buffer.append("\n\tCUI: ");
		buffer.append(this.getConceptCUI(localConceptID));
		
		if (withAllParents) {
			// all parents with CUIs
			Hashtable<String, Integer> parentsCui = this.getConceptParentsWithCUIs(localConceptID, false);
			buffer.append("\n\tParents (with CUIs) (").append(parentsCui.size()).append("): ").append(parentsCui);
			parentsCui = null;
			
			// PTRs with CUIs
			buffer.append("\n\tPTRs (based on CUIs) (?): ").append("Not implemented directly, use OBS.");
			
			// PTRs with AUIs
			HashSet<String> ptrs = this.getConceptPtrsWithAUIs(localConceptID);
			buffer.append("\n\tPTRs (based on AUIs) (").append(ptrs.size()).append("): ").append(ptrs);
		}

		if (withAllChildren) {
			// all children with CUIs
			Hashtable<String, Integer> childrenCui = this.getConceptChildrenWithCUIs(localConceptID, false);
			buffer.append("\n\tChildren (with CUIs) (").append(childrenCui.size()).append("): ").append(childrenCui);
			childrenCui = null;
	
			// PTLs with CUIs
			buffer.append("\n\tPTLs (based on CUIs) (?): ").append("Not implemented directly, use OBS.");
			
			// PTLs with CUIs
			buffer.append("\n\tPTLs (based on AUIs) (?): ").append("Not implemented.");
			
			// PTL with AUI PTRs
			HashSet<String> ptls = this.getConceptPtlsFromAuiPtrs(localConceptID);
			buffer.append("\n\tPTLs (based on AUI PTRs) (").append(ptls.size()).append("): ").append(ptls);
		}

		buffer.append("\n");
		return buffer.toString();
	}
	
	/**
	 * Returns a string which contains the hierarchy for a given concept based on the path-to-the-root
	 * information (from MRHIER), in a pretty form. 
	 */
	public String displayConceptHierarchyWithAUIs(String localConceptID) throws NonValidLocalConceptIDException{
		StringBuffer buffer = new StringBuffer();
		buffer.append(this.getConceptOntology(localConceptID)).append("\n");
		// gets the PTR from OBS DB
		HashSet<String> ptrs = this.getConceptPtrsWithAUIs(localConceptID);
		// for each PTR
		for(String ptr: ptrs){
			buffer.append(this.displayPtr(localConceptID, ptr));
		} 
		return buffer.toString();
	}
	
	/**
	 * Returns a string which contains the hierarchy for a given concept based on the path-to-the-root
	 * information (recursively recomputed), in a pretty form. 
	 */
	public String displayConceptHierarchyWithCUIs(String localConceptID) throws NonValidLocalConceptIDException{
		StringBuffer buffer = new StringBuffer();
		buffer.append(this.getConceptOntology(localConceptID)).append("\n");
		// gets the PTR from OBS DB
		HashSet<String> ptrs = this.getConceptPtrsWithCUIs(localConceptID);
		// for each PTR
		for(String ptr: ptrs){
			buffer.append(this.displayPtr(localConceptID, ptr));
		} 
		return buffer.toString();
	}
		
	// ****************************** EXCEPTIONS *********************************************
	
	public class NonValidLocalSemanticTypeIDException extends Exception {
		private static final long serialVersionUID = 1L;
		public NonValidLocalSemanticTypeIDException(String nonValidLocalSemanticTypeID){
			super("The given localSemanticTypeID is not valid: " + nonValidLocalSemanticTypeID);
		}
	}

	
}
