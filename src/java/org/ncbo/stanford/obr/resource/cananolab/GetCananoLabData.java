package org.ncbo.stanford.obr.resource.cananolab;

import edu.wustl.obr.QueryExecutor;
import edu.wustl.utill.ContextInvoker;
import gov.nih.nci.cananolab.domain.particle.NanoparticleSample;

import java.util.Map;
import java.util.Set;

import obs.obr.populate.Element;
import obs.obr.populate.Resource;
import obs.obr.populate.Structure;
import obs.obr.populate.Element.BadElementStructureException;

import org.apache.log4j.Logger;
import org.ncbo.stanford.obr.resource.ResourceAccessTool;

/**
 * This class main task is to fetch contextKeys values from context and create a element.
 * @author lalit_chand 
 */

public class GetCananoLabData {

    Resource resource = null;

    ResourceAccessTool tool = null;

    private static Logger logger = Logger.getLogger(GetCananoLabData.class);

    public GetCananoLabData(Resource resource, ResourceAccessTool tool) {
        this.resource = resource;
        this.tool = tool;

    }

    /**
     * @return It returns all cananoLab particles
     */
    public Set<NanoparticleSample> getAllCaNanoLabParticles() {
        return new QueryExecutor().getAllCananolabNanoParticles();
    }

    /**
     * @param nanoParticleId
     * @return It fetches context values for each contextItem and construct a  Element and returns it
     */
    public Element getElement(NanoparticleSample np) {
        ContextInvoker caNanoContextInvoker = new ContextInvoker(np.getId());
        Structure elementStructure = resource.getResourceStructure();
        Map<Double, String> nanoData = null;
        nanoData = caNanoContextInvoker.getContextValue();

        for (String contextName : elementStructure.getContextNames()) {
            //  default ontology is given to all context ..
            if (elementStructure.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION)) {

                Double weight = elementStructure.getWeight(contextName);

                String val = nanoData.get(weight);
                if (val == null) {
                    val = "";
                }

                if (contextName.equals("CANANO_Sample")) {
                    val = val + " " + np.getName();
                }
                elementStructure.putContext(contextName, val);
            }
        }
        Element element = null;
        // put the elementStructure in a new element
        try {
            element = new Element(np.getId().toString(), elementStructure);
        } catch (BadElementStructureException e) {
            logger.error("Bad Element Structure Exception Occured .." + e);
        }
        return element;
    }
}
