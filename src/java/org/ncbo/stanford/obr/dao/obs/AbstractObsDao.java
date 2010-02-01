package org.ncbo.stanford.obr.dao.obs;

import org.ncbo.stanford.obr.dao.AbstractObrDao;
import org.ncbo.stanford.obr.util.MessageUtils;
 
public abstract class AbstractObsDao extends AbstractObrDao { 
	
	protected static final String OBS_PREFIX = MessageUtils.getMessage("obs.tables.prefix");
	  	
	public AbstractObsDao(String suffix) {
		 super(OBS_PREFIX + suffix);
	} 
	 	
}