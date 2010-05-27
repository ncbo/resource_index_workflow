/**
 * 
 */
package org.ncbo.stanford.obr.service.statistics;

/**
 * @author Kuladip Yadav
 *
 */
public interface StatisticsService{
	
	/**
	 * Method removes statistics for given ontology version.
	 * 
	 * @param localOntologyID String containing ontology version.
	 */
	public void removeStatistics(String localOntologyID);

}
