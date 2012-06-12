/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.stanford.obr.resource.nif;

import org.w3c.dom.Document;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import javax.ws.rs.core.MediaType;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import obs.obr.populate.Structure;
import org.ncbo.stanford.obr.resource.AbstractXmlResourceAccessTool;

/**
 * Abstract class for all NIF resources.
 * @author s.kharat
 */
public abstract class AbstractNifResourceAccessTool extends AbstractXmlResourceAccessTool {

    private static final String SERVER = "http://nif-services.neuinfo.org/nif/services/federationQuery/";
    private final WebResource resource = getClient().resource(SERVER);
    protected static final int rowCount = 100;  //100 records for each request.
    protected static final String query = "*";  //mins all records.
    
    // String constant for all NIF resources.
    protected static final String nodeName = "name";
    protected static final String nodeValue = "value";
    protected static final String resultCount = "resultCount";

    protected AbstractNifResourceAccessTool(String resourceName, String resourceID, Structure resourceStructure) {
        super(resourceName, resourceID, resourceStructure);
    }

    /***
     * Query the data federation 
     * @param db The database name
     * @param indexable The indexable name
     * @param query The query string
     * @param offset The offset to start into the results
     * @param count The number of results to return
     * @return Document
     */
    protected Document queryFederation(String db, String indexable, String query, int offset, int count) {
        Document dom = null;
        try {
           // logger.info("Getting federation data...");
            String response = resource.path(db).path(indexable).
                    queryParam("q", query).
                    queryParam("offset", Integer.toString(offset)).
                    queryParam("count", Integer.toString(count)).
                    accept(MediaType.APPLICATION_XML_TYPE).get(String.class);
            dom = buildDom(response);
        } catch (Exception e) {
            logger.error("** PROBLEM ** in getting federation data.", e);
        }
        return dom;
    }

    @Provides
    @Singleton
    Client getClient() {
        return Client.create();
    }
}
