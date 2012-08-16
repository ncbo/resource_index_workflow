/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ncbo.stanford.obr.resource.nif.pubmedhealth;

import org.ncbo.stanford.obr.resource.nif.AbstractNifResourceAccessTool;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import obs.obr.populate.Element;
import obs.obr.populate.Structure;
import org.jsoup.Jsoup;
import org.ncbo.stanford.obr.enumeration.ResourceType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * AccessTool for NIF PubMedHealth
 * @author s.kharat
 */
public class PMHAccessTool extends AbstractNifResourceAccessTool{
    
    private static final String PMH_URL = "http://www.ncbi.nlm.nih.gov/pubmedhealth";
    private static final String PMH_NAME = "PubMedHealth (via NIF)";
    private static final String PMH_RESOURCEID = "PMH";
    private static final String PMH_DESCRIPTION = "PubMed Health is a consumer health Web site produced by the National Center for Biotechnology Information (NCBI), a division of the National Library of Medicine (NLM) at the National Institutes of Health (NIH). PubMed Health provides up-to-date information on diseases, conditions, injuries, drugs, supplements, treatment options, and healthy living, with a special focus on comparative effectiveness research from institutions around the world.";
    private static final String PMH_LOGO = "http://neurolex.org/w/images/8/8c/PubMed_Health.PNG";
    private static final String PMH_ELT_URL = "http://www.ncbi.nlm.nih.gov/pubmedhealth/";    
    private static final String[] PMH_ITEMKEYS = {"Name", "Title", "Text"};
    private static final Double[] PMH_WEIGHTS = {1.0, 0.7, 0.9};
    private static final String[] PMH_ONTOIDS = {Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION, Structure.FOR_CONCEPT_RECOGNITION};
    private static Structure PMH_STRUCTURE = new Structure(PMH_ITEMKEYS, PMH_RESOURCEID, PMH_WEIGHTS, PMH_ONTOIDS);
    private static String PMH_MAIN_ITEMKEY = "Name";
    
    // Constant 
    private static final String PMH_Database = "PubMedHealth";
    private static final String PMH_Indexable = "Drug";
    
    private static final String PMH_Name = "Name";
    private static final String PMH_Title = "Title";
    private static final String PMH_Text = "Text";
           
    private Map<String, String> localOntologyIDMap;

    // constructors
    public PMHAccessTool() {
        super(PMH_NAME, PMH_RESOURCEID, PMH_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(PMH_URL));
            this.getToolResource().setResourceDescription(PMH_DESCRIPTION);
            this.getToolResource().setResourceLogo(new URL(PMH_LOGO));
            this.getToolResource().setResourceElementURL(PMH_ELT_URL);
        } catch (MalformedURLException e) {
            logger.error(EMPTY_STRING, e);
        }
        localOntologyIDMap = createLocalOntologyIDMap(PMH_STRUCTURE);
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.BIG;
    }

    @Override
    public void updateResourceInformation() {
        // TODO 
        // can be used to update resource name, description, logo, elt_url.
    }

    @Override
    public HashSet<String> queryOnlineResource(String query) {
        // TODO 
        // not used for caArray 
        return new HashSet<String>();
    }

    @Override
    public String elementURLString(String elementLocalID) {
        String[] elementId = elementLocalID.split(SLASH_STRING);        
        return PMH_ELT_URL + elementId[0] ;
    }

    @Override
    public String mainContextDescriptor() {
        return PMH_MAIN_ITEMKEY;
    }

    /**
     * This method creates map of latest version of ontology with contexts as key.
     * It uses virtual ontology ids associated with contexts. 
     * 
     * @param structure {@code Structure} for given resource
     * @return {@code HashMap} of latest local ontology id with context as key.
     */
    public HashMap<String, String> createLocalOntologyIDMap(Structure structure) {
        HashMap<String, String> localOntologyIDMap = new HashMap<String, String>();
        String virtualOntologyID;
        for (String contextName : structure.getOntoIds().keySet()) {
            virtualOntologyID = structure.getOntoIds().get(contextName);
            if (!virtualOntologyID.equals(Structure.FOR_CONCEPT_RECOGNITION)
                    && !virtualOntologyID.equals(Structure.NOT_FOR_ANNOTATION)) {
                localOntologyIDMap.put(contextName, ontlogyService.getLatestLocalOntologyID(virtualOntologyID));
            }
        }
        return localOntologyIDMap;
    }

    @Override
    public int updateResourceContent() {
        int nbElement = 0;
        try {
            Element myExp;
            //Get all elements from resource site
            HashSet<Element> allElementList = this.getAllElements();
            logger.info("Number of new elements to dump: " + allElementList.size());

            // for each experiments accessed by the tool
            Iterator<Element> i = allElementList.iterator();
            while (i.hasNext()) {
                // populates OBR_PMH_ET with each of these experiments.
                myExp = i.next();
                try {
                    if (!myExp.getElementStructure().hasNullValues()) {
                        if (this.resourceUpdateService.addElement(myExp)) {
                            nbElement++;
                        }
                    }
                } catch (Exception e) {
                    logger.error("** PROBLEM ** Problem with id " + myExp.getLocalElementId() + " when populating the OBR_PMH_ET table.", e);
                }
            }
        } catch (Exception e) {
            logger.error("** PROBLEM ** Cannot update resource " + this.getToolResource().getResourceName(), e);
        }
        logger.info(nbElement + " elements added to the OBR_PMH_ET table.");
        return nbElement;
    }

    /**
     * get all Elements.
     */
    public HashSet<Element> getAllElements() {
        logger.info("* Get All Elements for PubMedHealth ... ");
        HashSet<Element> elementSet = new HashSet<Element>();
        int nbAdded = 0;
        int offset = 0;
        int totalCount = 0;

        try {
            //get all elements from _ET table
            HashSet<String> allElementsInET = this.resourceUpdateService.getAllLocalElementIDs();
            Map<String, Map<String, String>> allRowsData = new HashMap<String, Map<String, String>>();

            //parsing data
            do {
                Document dom = queryFederation(PMH_Database, PMH_Indexable, query, offset, rowCount);
                if(dom != null){
                    Node tableData = dom.getFirstChild();
                    //get total records
                    totalCount = Integer.parseInt(tableData.getAttributes().getNamedItem(resultCount).getNodeValue());
                    offset += rowCount;
                    
                    Node results = tableData.getFirstChild();

                    // Iterate over the returned structure 
                    NodeList rows = results.getChildNodes();
                    for (int i = 0; i < rows.getLength(); i++) {
                        String localElementId = EMPTY_STRING;
                        Map<String, String> elementAttributes = new HashMap<String, String>();

                        Node row = rows.item(i);
                        for (int j = 0; j < row.getChildNodes().getLength(); j++) {
                            NodeList vals = row.getChildNodes().item(j).getChildNodes();
                            String name = null;
                            String value = null;
                            for (int k = 0; k < vals.getLength(); k++) {
                                if (nodeName.equals(vals.item(k).getNodeName())) {
                                    name = vals.item(k).getTextContent();
                                } else if (nodeValue.equals(vals.item(k).getNodeName())) {
                                    value = vals.item(k).getTextContent();
                                }
                            }
                            if (name.equalsIgnoreCase(PMH_Name)) {                       //Name & localElementId
                                localElementId =value.substring(value.indexOf(PMH_ELT_URL) + PMH_ELT_URL.length(), value.indexOf(endTag)-1 ) ;      
                                elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[0]),  Jsoup.parse(value).text());                                                
                            } else if (name.equalsIgnoreCase(PMH_Title)) {               //Text                                                          
                                elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[1]), value);
                            } else if (name.equalsIgnoreCase(PMH_Text)) {                //Title
                                elementAttributes.put(Structure.generateContextName(PMH_RESOURCEID, PMH_ITEMKEYS[2]), value);
                            } 
                        }

                        //Check if elementId is present locally.
                        if (allElementsInET.contains(localElementId)) {
                            continue;
                        } else {
                            //(SLASH_STRING and i value appended for unique local element Id purpose)
                            allRowsData.put(localElementId + SLASH_STRING + i, elementAttributes); 
                        }
                    }
                }else{
                    offset += rowCount;                   
                }
            } while (offset < totalCount);

            //parsing ends

            // Second phase: creation of elements           
            for (String localElementID : allRowsData.keySet()) {
                Map<String, String> elementAttributes = new HashMap<String, String>();
                elementAttributes = allRowsData.get(localElementID);

                // PUT DATA INTO A STRUCTURE++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
                Structure elementStructure = new Structure(PMH_STRUCTURE.getContextNames());
                for (String contextName : PMH_STRUCTURE.getContextNames()) {
                    boolean attributeHasValue = false;

                    for (String att : elementAttributes.keySet()) {
                        if (contextName.equals(att)) {
                            // not an existing annotation
                            if (PMH_STRUCTURE.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION)
                                    || PMH_STRUCTURE.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION)) {
                                elementStructure.putContext(contextName, elementAttributes.get(att));
                                attributeHasValue = true;

                            }
                        }
                    }

                    // to avoid null value in the structure
                    if (!attributeHasValue) {
                        elementStructure.putContext(contextName, EMPTY_STRING);
                    }
                }
                // put the element structure in a new element
                try {                    
                    Element exp = new Element(localElementID, elementStructure);
                    elementSet.add(exp);
                } catch (Element.BadElementStructureException e) {
                    logger.error(EMPTY_STRING, e);
                }
            }

        } catch (Exception e) {
            logger.error("** PROBLEM ** Problem in getting rows.", e);
        }
        nbAdded = elementSet.size();
        logger.info((nbAdded) + " rows found.");
        return elementSet;
    }
}


