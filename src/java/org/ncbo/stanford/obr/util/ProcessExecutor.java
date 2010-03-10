package org.ncbo.stanford.obr.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.ncbo.stanford.obr.util.helper.StringHelper;

/**
 * This is utility class for executing process in java
 * 
 * @author Kuladip Yadav
 *
 */
public class ProcessExecutor implements StringHelper {	  
	/**
	 * This method execute base command with given parameter. 
	 * 
	 * @param baseCommand
	 * @param parameters
	 * @return
	 * @throws Exception
	 */
	public static HashMap<Integer, String> executeCommand(String baseCommand, String... parameters) throws Exception{
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
		
		BufferedReader resultReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		
		String resultLine = EMPTY_STRING;
		HashMap<Integer, String> lines = new HashMap<Integer, String>();
		int line_nb = 0;
		// Tab separated string containing id and name of organism.
		while((resultLine = resultReader.readLine()) != null) {
			lines.put(line_nb, resultLine) ;
			line_nb++;
		}
		resultReader.close(); 
         
        return lines;
       
	}

}
