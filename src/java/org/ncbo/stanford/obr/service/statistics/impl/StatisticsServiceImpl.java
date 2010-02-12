/**
 * 
 */
package org.ncbo.stanford.obr.service.statistics.impl;

import org.ncbo.stanford.obr.dao.DaoFactory;
import org.ncbo.stanford.obr.service.statistics.StatisticsService;

/**
 * @author k.palanisamy
 *
 */
public class StatisticsServiceImpl implements StatisticsService, DaoFactory{

	/**
	 * Method removes statistics for given ontology version.
	 * 
	 * @param localOntologyID String containing ontology version.
	 */
	public void removeStatistics(String localOntologyID) {
		statisticsDao.deleteEntriesFromOntology(localOntologyID);
	}
	
}
