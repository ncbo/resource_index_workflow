package org.ncbo.stanford.obr.util;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.ncbo.stanford.obr.util.helper.StringHelper;

import obs.common.utils.StreamGobbler;

/**
 * This is utility class for executing process in java
 * 
 * @author Kuladip Yadav
 *
 */
public class ProcessExecutor implements StringHelper {
	
	/** logger for this class*/
	private static Logger logger = Logger.getLogger(ProcessExecutor.class);
	/** Maximum number of attempt allowed. */	
	private static final int MAX_ATTEMPT_ALLOWED = 5;
	
	/**
	 * This method execute base command with given paramater.If the process gives error then try to re-execute recursively 
	 * upto {@code MAX_ATTEMPT_ALLOWED} time.
	 * 
	 * @param baseCommand
	 * @param attemptNumber
	 * @param parameters
	 * @return
	 * @throws Exception
	 */
	public static HashMap<Integer, String> executeCommand(String baseCommand, int attemptNumber, String... parameters) throws Exception{
		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		
		StringBuffer command =  new StringBuffer();
		command.append(baseCommand);
		command.append(BLANK_SPACE);
		for (int i = 0; i < parameters.length; i++) {
			command.append(parameters[i]);
			command.append(BLANK_SPACE);
		} 
		 
		process = runtime.exec(command.toString()); 
		// error message and script output management
        StreamGobbler errorGobbler  = new StreamGobbler(process.getErrorStream(), "ERROR");            
        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "OUTPUT");	            
        
        errorGobbler.start();
        outputGobbler.start();
        
        process.waitFor();      
         
        if(StreamGobbler.lines.size() == 0 && attemptNumber<MAX_ATTEMPT_ALLOWED){  
        	  logger.info("Trying to execute again after" +(attemptNumber +1)+ "s...");
        	  Thread.sleep((attemptNumber +1)*1000);
        	  return executeCommand(baseCommand, ++attemptNumber, parameters);        	
        }         
        
        return StreamGobbler.lines;
       
	}

}
