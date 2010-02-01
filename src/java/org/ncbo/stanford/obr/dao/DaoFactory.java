package org.ncbo.stanford.obr.dao;

import org.ncbo.stanford.obr.dao.context.ContexDao;
import org.ncbo.stanford.obr.dao.dictionary.DictionaryDao;
import org.ncbo.stanford.obr.dao.obs.concept.ConceptDao;
import org.ncbo.stanford.obr.dao.obs.map.MapDao;
import org.ncbo.stanford.obr.dao.obs.ontology.OntologyDao;
import org.ncbo.stanford.obr.dao.obs.relation.RelationDao;
import org.ncbo.stanford.obr.dao.obs.term.TermDao;
import org.ncbo.stanford.obr.dao.resource.ResourceDao;
import org.ncbo.stanford.obr.dao.statistics.StatisticsDao;

/**
 * @author Kuladip Yadav
 *
 */
public interface DaoFactory {
	// **** OBR Dao 
	public static final ContexDao contextTableDao = ContexDao.getInstance();
	public static final ResourceDao resourceTableDao = ResourceDao.getInstance();	
	public static final StatisticsDao statisticsDao = StatisticsDao.getInstance();
	public static final DictionaryDao dictionaryDao = DictionaryDao.getInstance();
	 
	// ***** OBS Dao
	public static final OntologyDao ontologyDao = OntologyDao.getInstance();
	public static final ConceptDao conceptDao = ConceptDao.getInstance();
	public static final TermDao termDao = TermDao.getInstance();
	public static final MapDao mapDao = MapDao.getInstance();
	public static final RelationDao relationDao = RelationDao.getInstance();

}
