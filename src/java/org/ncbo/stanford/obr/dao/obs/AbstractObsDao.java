package org.ncbo.stanford.obr.dao.obs;

import org.ncbo.stanford.obr.dao.AbstractObrDao;
 
public abstract class AbstractObsDao extends AbstractObrDao {  
	  	
	public AbstractObsDao(String suffix) {
		 super(OBS_PREFIX + suffix);
	}  
	
	 	
}