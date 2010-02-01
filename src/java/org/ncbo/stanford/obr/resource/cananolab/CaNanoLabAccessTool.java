package org.ncbo.stanford.obr.resource.cananolab;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import obs.obr.populate.Element;
import obs.obr.populate.Structure;

import org.ncbo.stanford.obr.resource.ResourceAccessTool;

import edu.wustl.utill.CaNanoLabContextDetail;
import edu.wustl.utill.CaNanoLabUtility;
import gov.nih.nci.cananolab.domain.particle.NanoparticleSample;

/**
 * This Class defines all details needed for annotating free context from caNanoLab resource.It also populates ET table .. 
 * @author lalit_chand
 */
public class CaNanoLabAccessTool extends ResourceAccessTool {

    // Home URL of caNanoLab
    private static final String CANANO_LAB_URL = CaNanoLabUtility.getCananoLabHomeURL();

    // Resource Name
    private static final String CANANO_LAB_NAME = CaNanoLabUtility.getCananoLabResourceName();

    // Resource Id
    private static final String CANANO_LAB_RESOURCEID = CaNanoLabUtility.getCananoLabResourceId();

    // Resource Description
    private static final String CANANO_DESCRIPTION = CaNanoLabUtility.getCananoLabDescription();

    // URL of caNano logo
    private static final String CANANO_LOGO = CaNanoLabUtility.getCananoLabLogoURL();

    // This is the URL which points to specific information of caNanoLab nanoParticle by appending nanoParticle Id
    private static final String CANANO_ELT_URL = CaNanoLabUtility.getCananoLabElementURL();
 
    // Context Names for caNanoLab
    private static final String[] CANANO_ITEMKEYS = CaNanoLabContextDetail.getItemKeys();

    // Weights associated with each context
    private static final Double[] CANANO_WEIGHTS = CaNanoLabContextDetail.getWeights();

    // Default Ontology Id associated with each context
    private static final String[] CANANO_ONTOIDS = CaNanoLabContextDetail.getDefaultOntologyIds();

    private static Structure CANANO_STRUCTURE = new Structure(CANANO_ITEMKEYS, CANANO_LAB_RESOURCEID,
            CANANO_WEIGHTS, CANANO_ONTOIDS);

    private static String CANANO_MAIN_ITEMKEY = CANANO_ITEMKEYS[0];

    public CaNanoLabAccessTool() {
        
        super(CANANO_LAB_NAME, CANANO_LAB_RESOURCEID, CANANO_STRUCTURE);
        try {
            this.getToolResource().setResourceURL(new URL(CANANO_LAB_URL));
            this.getToolResource().setResourceLogo(new URL(CANANO_LOGO));
        } catch (MalformedURLException e) {
            logger.error("Malformed URL Exception Occured", e);
        }
        this.getToolResource().setResourceDescription(CANANO_DESCRIPTION);
        this.getToolResource().setResourceElementURL(CANANO_ELT_URL);

    }

    @Override
    /**
     * It returns element URL of cananolab.
     */
    public String elementURLString(String localElementID) {
        return CANANO_ELT_URL + localElementID;
    }

    
    public String itemKeyForAnnotationForBP() {
        return CANANO_MAIN_ITEMKEY;
    }

    @Override
    public HashSet<String> queryOnlineResource(String query) {
        // Not implemented for caNanoLab resource
        return null;
    }

    @Override
    /**
     * This method updates the ET table
     */
    public int updateResourceContent() {
        int nbElement = 0;
        Element caNanoElement = null;

        logger.info("Started fecthing All NanoParticles ...");
        Set<NanoparticleSample> nanoparticleList = getLocalElementIDs();
      
        logger.info("Total Number of NanoParticle Fetched " + nanoparticleList.size());
        // get data associated with each of these elements
        // and populate the ElementTable
        //
        for (NanoparticleSample nanoSample : nanoparticleList) {
            // get data of this element
            caNanoElement = this.getElement(nanoSample);
            // populates OBR_REAC_ET with this element

            if (caNanoElement != null && resourceUpdateService.addElement(caNanoElement)) {
                nbElement++;
            }
        }
        logger.info(nbElement + " elements from " + CANANO_LAB_NAME + " added to the table.");
        return nbElement;
    }

    /**
     * 
     * @param np NanoParticleSample for which context to be populated. 
     * @return It returns the element 
     */
    private Element getElement(NanoparticleSample np) {

        Element element = null;
        logger.info("Started extracting  data for Element " + np.getId() + " ... ");
        try {
            GetCananoLabData myExtractor = new GetCananoLabData(getToolResource(), this);
            element = myExtractor.getElement(np);
        } catch (Exception e) {
            logger.error("Could not extract " + np.getId() + " from " + CANANO_LAB_NAME);
        }
        logger.info("Finished  extracting  data for Element " + np.getId().toString() + " ... ");
        return element;
    }

    /*
     * It returns the all local elementId list as list of NanoParticles
     */
    private Set<NanoparticleSample> getLocalElementIDs() {
        Set<NanoparticleSample> nanoparticleList = new HashSet<NanoparticleSample>();
        logger.info("Fetching all the NanoParticle from " + CANANO_LAB_NAME + " ... ");
        try {
            GetCananoLabData dataProvider = new GetCananoLabData(getToolResource(), this);
            nanoparticleList = dataProvider.getAllCaNanoLabParticles();
        } catch (Exception e) {
            logger.error("Could not extract the total number of NanoParticles from " + CANANO_LAB_NAME);
        }
        return nanoparticleList;
    }

    @Override
    public void updateResourceInformation() {
        // not implemented
    }

    @Override
    public String mainContextDescriptor() {        
        return CANANO_MAIN_ITEMKEY;
    }

}
