package org.ncbo.stanford.obr.resource.reactome;

import java.util.HashSet;

import javax.xml.namespace.QName;
 
import obs.obr.populate.Element;
import obs.obr.populate.Resource; 
import obs.obr.populate.Structure;
import obs.obr.populate.Element.BadElementStructureException;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.ser.ArrayDeserializerFactory;
import org.apache.axis.encoding.ser.ArraySerializerFactory;
import org.apache.axis.encoding.ser.BeanDeserializerFactory;
import org.apache.axis.encoding.ser.BeanSerializerFactory;
import org.apache.axis.encoding.ser.EnumDeserializerFactory;
import org.apache.axis.encoding.ser.EnumSerializerFactory;
import org.apache.log4j.Logger;
import org.ncbo.stanford.obr.resource.ResourceAccessTool;
import org.reactome.cabig.domain.CatalystActivity;
import org.reactome.cabig.domain.Complex;
import org.reactome.cabig.domain.DatabaseCrossReference;
import org.reactome.cabig.domain.Event;
import org.reactome.cabig.domain.EventEntity;
import org.reactome.cabig.domain.EventEntitySet;
import org.reactome.cabig.domain.GeneOntology;
import org.reactome.cabig.domain.GeneOntologyRelationship;
import org.reactome.cabig.domain.GenomeEncodedEntity;
import org.reactome.cabig.domain.ModifiedResidue;
import org.reactome.cabig.domain.Pathway;
import org.reactome.cabig.domain.Polymer;
import org.reactome.cabig.domain.PublicationSource;
import org.reactome.cabig.domain.Reaction;
import org.reactome.cabig.domain.ReferenceChemical;
import org.reactome.cabig.domain.ReferenceEntity;
import org.reactome.cabig.domain.ReferenceGene;
import org.reactome.cabig.domain.ReferenceProtein;
import org.reactome.cabig.domain.ReferenceRNA;
import org.reactome.cabig.domain.ReferenceSequence;
import org.reactome.cabig.domain.Regulation;
import org.reactome.cabig.domain.RegulationType;
import org.reactome.cabig.domain.Regulator;
import org.reactome.cabig.domain.SmallMoleculeEntity;
import org.reactome.cabig.domain.Summation;
import org.reactome.cabig.domain.Taxon;
import org.reactome.servlet.InstanceNotFoundException;
import org.reactome.servlet.ReactomeRemoteException;

/**
 * This class enables to  - get the list of all localElementID of Pathways and Reactions (ie Events) 
 * in the resource Reactome,
 *                        - get data related to a specific Pathway or Reaction.
 * This class use as a client the Reactome SOAP Web Service.
 * See http://www.reactome.org/download/index.html
 * @author Adrien Coulet
 * @version OBR_v1		
 * @created 26-Fev-2009
 *
 */

public class GetReactomeData {
	
	// Logger for this class
	private static Logger logger = Logger.getLogger(GetReactomeData.class);

	//attributes	
	private final Object[] EMPTY_ARG = new Object[]{};
    private final String SERVICE_URL_NAME="http://www.reactome.org:8080/caBIOWebApp/services/caBIOService";
    private Service caBIOService;
    
	Resource  resource       = null;
	Structure basicStructure = null;
	String    resourceID     = "";
	ResourceAccessTool tool  = null;
		
	//constructor
	public GetReactomeData(Resource myResource, ResourceAccessTool tool){
		this.resource       = myResource;
		this.basicStructure = myResource.getResourceStructure();
		this.resourceID     = myResource.getResourceID();
		this.tool           = tool;		 
	}
	
	// methods
	public HashSet<Long> getLocalElementIDs() {			
		
		HashSet<Long> localElementIDs = new HashSet<Long>() ;	
		try {					
			Call call = createCall("listObjects");
	        String[] domainClsNames = new String[]{
	              "org.reactome.cabig.domain.Pathway",
	              "org.reactome.cabig.domain.Reaction"
	        };
	        int length = getMaxSizeInListObjects();
	        for (String clsName : domainClsNames) {
	            int total  = 0;
	            int total2 = 0;
	            
	            Object[] objects = null;
	            int counter = 0;
	            while (counter < 1) {
	                objects = (Object[]) call.invoke(new Object[]{clsName, total, length});
	                if (objects == null || objects.length == 0)
	                    break;
	                for (int i = 0; i < objects.length; i++) {
	                	Event myEvent = (Event) objects[i];
	                	localElementIDs.add(myEvent.getId());
	                }
	                total  += objects.length;
	                total2 += length;
	                counter ++;
	                if(total<total2){
	                	break;
	                }
	            }
	        }			
		} catch(Exception e) {
			logger.error("**PROBLEM: when querying the list of localElementID.", e);
		}		
		return localElementIDs;
	}
	
	public Element getElement(Long localElementID) {			
		 
		Structure elementStructure = basicStructure;
		Element element = null;
		try {					
			Call callQueryByID = null;
	         callQueryByID = createCall("queryById");         
	         
	         Object obj = callQueryByID.invoke(new Object[]{localElementID});
	         Event myEvent = (Event) obj;
	         for (String contextName: elementStructure.getContextNames()){
		         if(elementStructure.getOntoID(contextName).equals(Structure.FOR_CONCEPT_RECOGNITION) ||
		        		 elementStructure.getOntoID(contextName).equals(Structure.NOT_FOR_ANNOTATION) ){	
		        	 // get name
		        	 if(contextName.equals(this.resourceID+"_"+"name")){
		        		 if(myEvent.getName()!=null || myEvent.getName().equals("null")){		        	 
		        			 elementStructure.putContext(contextName,myEvent.getName());
		        		 }else{
		        			 elementStructure.putContext(contextName,"");
		        		 }
		        	 }
		        	 // get participants
		        	 if(contextName.equals(this.resourceID+"_"+"participants")){
		        		 HashSet<String> participants = getParticipants(localElementID);	
		        		 String particpantList = "";
		        		 if(participants!=null){		        
		        			 for(String name : participants){
		        				 if(!particpantList.equals("")){
		        					 particpantList += "> "+name;
		        				 }else{
		        					 particpantList += name;
		        				 }
		        	         } 		 
		        			 elementStructure.putContext(contextName, particpantList);
		        		 }else{
		        			 elementStructure.putContext(contextName,"");
		        		 }
		        	 }
		        	 // get text description
		        	 if(contextName.equals(this.resourceID+"_"+"summation")){
		        		 String summationList = "";
		        		 if(myEvent.getSummation()!=null){		        
		        			 for(Summation sum : myEvent.getSummation()){
		        				 if(sum.getText()!=null){
		        					 if(!summationList.equals("")){		        				 
		        						 summationList += "> "+sum.getText();
			        				 }else{
			        					 summationList += sum.getText();
			        				 }
		        				 }
		        	         } 		 
		        			 elementStructure.putContext(contextName, summationList);
		        		 }else{
		        			 elementStructure.putContext(contextName,"");
		        		 }
		        	 }		        	 
		         }else{
		         // REPORTED ANNOTATIONS	 
		        	 // TODO to be re-implemented with mapStringToLocalConceptIDs(String s, String localOntologyID, boolean exactMap)
		        	 // handle the case where several concept ID will show up
		        	 // Exceptions handling to be changed and logged
		        	 // get GO annotations
		        	 if(contextName.equals(this.resourceID+"_"+"goBiologicalProcess")){
		        		 try{
		        			 if (myEvent.getGoBiologicalProcess()!=null){

		        				 String localConceptID = tool.getResourceUpdateService().getLocalConceptIdByPrefNameAndOntologyId(elementStructure.getOntoID(contextName),myEvent.getGoBiologicalProcess().getName());
		        				 elementStructure.putContext(contextName,localConceptID);
		        			 }else{
		        				 elementStructure.putContext(contextName,"");			    			 
		        			 }
		        		 }
		        		 catch (Exception e) {
		        			 elementStructure.putContext(contextName,"");
		        		 }
		        	 }
		        	 if(contextName.equals(this.resourceID+"_"+"goCellCompartiment")){
		        		 try{
		        			 if (myEvent.getCompartment()!=null){
		        				 String localConceptID = tool.getResourceUpdateService().getLocalConceptIdByPrefNameAndOntologyId(elementStructure.getOntoID(contextName),myEvent.getCompartment().getName());
		        				 elementStructure.putContext(contextName,localConceptID);				        	
		        			 }else{
		        				 elementStructure.putContext(contextName,"");			    			 
		        			 }
		        		 }
		        		 catch (Exception e) {
		        			 elementStructure.putContext(contextName,"");
		        		 }
		        	 }
		         }
	         }
		} catch(Exception e) {
			logger.error("**PROBLEM: when querying data for an element.", e);
		}		
		// put the elementStructure in a new element
		try{							
			element = new Element(localElementID.toString(), elementStructure);		
		}catch(BadElementStructureException e){
			logger.error("", e);
		}		
		return element;
	}
	
	 /**
     * Get an HashSet of the name of all participating molecules for a given pathway.
     * @param id
     * @return
     * @throws Exception
     */
    public HashSet<String> getParticipants(Long id) throws Exception {
    	HashSet<String> participants = new HashSet<String>();
    	
        Call callForPathwayParticipants = null;
        callForPathwayParticipants = createCall("listPathwayParticipantsForId");
        Object[] rtn = (Object[]) callForPathwayParticipants.invoke(new Object[]{id});
        
        Call callByObject = null;
        callByObject = createCall("queryByObjects");
        rtn = (Object[]) callByObject.invoke(new Object[]{rtn});
        
        for (int i = 0; i < rtn.length; i++) {
        	EventEntity entity = (EventEntity) rtn[i];
        	String[] nameAndCompart = entity.getName().split(" \\[");
        	if(nameAndCompart.length==2 && !participants.contains(nameAndCompart[0])){
            	participants.add(nameAndCompart[0]);
        	}
        }    
        return participants;
    }
	
    /**
     * Return the nb max of object that can be send back by the Web Service.
     * This parameter is used when we asking for the list of all Pathways and Reactions localElementIDs
     * @return int : The max size
     * @throws Exception
     */
    private int getMaxSizeInListObjects() throws Exception {
        Call call = createCall("getMaxSizeInListObjects");
        Integer rtn = (Integer) call.invoke(EMPTY_ARG);
        return rtn;
    }
    
	/*******************************Reactome Web Service******************************************/
	/**
	 * Reactome Web Service createCall class
	 * enables to create a Call that can then be invoked to get data from the web service.
	 * @param callName
	 * @return
	 * @throws Exception
	 */
    private Call createCall(String callName) throws Exception {
        if (caBIOService == null) {
            caBIOService = new Service(SERVICE_URL_NAME + "?wsdl", 
                                       new QName(SERVICE_URL_NAME, 
                                                 "CaBioDomainWSEndPointService"));
        }
        String portName = "caBIOService";
        Call call = (Call) caBIOService.createCall(new QName(SERVICE_URL_NAME, portName),
                                                   callName);
        registerTypeMappings(call);
        return call;
    }
	
    private void registerTypeMappings(Call call) {
        QName instanceNotFoundModel = new QName("http://www.reactome.org/caBIOWebApp/schema", 
                                                "InstanceNotFoundException");
        call.registerTypeMapping(InstanceNotFoundException.class, instanceNotFoundModel,
                new BeanSerializerFactory(InstanceNotFoundException.class, instanceNotFoundModel),
                new BeanDeserializerFactory(InstanceNotFoundException.class, instanceNotFoundModel));
        QName reactomeAxisFaultModel = new QName("http://www.reactome.org/caBIOWebApp/schema", 
                                                 "ReactomeRemoteException");
        call.registerTypeMapping(ReactomeRemoteException.class, reactomeAxisFaultModel,
                new BeanSerializerFactory(ReactomeRemoteException.class, reactomeAxisFaultModel),
                new BeanDeserializerFactory(ReactomeRemoteException.class, reactomeAxisFaultModel));
        QName CatalystActivityModel= new QName("http://www.reactome.org/caBIOWebApp/schema", 
                                               "CatalystActivity");
        call.registerTypeMapping(CatalystActivity.class, CatalystActivityModel,
              new BeanSerializerFactory(CatalystActivity.class, CatalystActivityModel),
              new BeanDeserializerFactory(CatalystActivity.class, CatalystActivityModel));
        QName ComplexModel= new QName("http://www.reactome.org/caBIOWebApp/schema", 
                                      "Complex");
        call.registerTypeMapping(Complex.class, ComplexModel,
              new BeanSerializerFactory(Complex.class, ComplexModel),
              new BeanDeserializerFactory(Complex.class, ComplexModel));
        QName DatabaseCrossReferenceModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "DatabaseCrossReference");
        call.registerTypeMapping(DatabaseCrossReference.class, DatabaseCrossReferenceModel,
              new BeanSerializerFactory(DatabaseCrossReference.class, DatabaseCrossReferenceModel),
              new BeanDeserializerFactory(DatabaseCrossReference.class, DatabaseCrossReferenceModel));
        QName EventModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "Event");
        call.registerTypeMapping(Event.class, EventModel,
              new BeanSerializerFactory(Event.class, EventModel),
              new BeanDeserializerFactory(Event.class, EventModel));
        QName EventEntityModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "EventEntity");
        call.registerTypeMapping(EventEntity.class, EventEntityModel,
              new BeanSerializerFactory(EventEntity.class, EventEntityModel),
              new BeanDeserializerFactory(EventEntity.class, EventEntityModel));
        QName EventEntitySetModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "EventEntitySet");
        call.registerTypeMapping(EventEntitySet.class, EventEntitySetModel,
              new BeanSerializerFactory(EventEntitySet.class, EventEntitySetModel),
              new BeanDeserializerFactory(EventEntitySet.class, EventEntitySetModel));
        QName GeneOntologyModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "GeneOntology");
        call.registerTypeMapping(GeneOntology.class, GeneOntologyModel,
              new BeanSerializerFactory(GeneOntology.class, GeneOntologyModel),
              new BeanDeserializerFactory(GeneOntology.class, GeneOntologyModel));
        QName GeneOntologyRelationshipModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "GeneOntologyRelationship");
        call.registerTypeMapping(GeneOntologyRelationship.class, GeneOntologyRelationshipModel,
              new BeanSerializerFactory(GeneOntologyRelationship.class, GeneOntologyRelationshipModel),
              new BeanDeserializerFactory(GeneOntologyRelationship.class, GeneOntologyRelationshipModel));
        QName GenomeEncodedEntityModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "GenomeEncodedEntity");
        call.registerTypeMapping(GenomeEncodedEntity.class, GenomeEncodedEntityModel,
              new BeanSerializerFactory(GenomeEncodedEntity.class, GenomeEncodedEntityModel),
              new BeanDeserializerFactory(GenomeEncodedEntity.class, GenomeEncodedEntityModel));
        QName ModifiedResidueModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "ModifiedResidue");
        call.registerTypeMapping(ModifiedResidue.class, ModifiedResidueModel,
              new BeanSerializerFactory(ModifiedResidue.class, ModifiedResidueModel),
              new BeanDeserializerFactory(ModifiedResidue.class, ModifiedResidueModel));
        QName PathwayModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "Pathway");
        call.registerTypeMapping(Pathway.class, PathwayModel,
              new BeanSerializerFactory(Pathway.class, PathwayModel),
              new BeanDeserializerFactory(Pathway.class, PathwayModel));
        QName PolymerModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "Polymer");
        call.registerTypeMapping(Polymer.class, PolymerModel,
              new BeanSerializerFactory(Polymer.class, PolymerModel),
              new BeanDeserializerFactory(Polymer.class, PolymerModel));
        QName PublicationSourceModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "PublicationSource");
        call.registerTypeMapping(PublicationSource.class, PublicationSourceModel,
              new BeanSerializerFactory(PublicationSource.class, PublicationSourceModel),
              new BeanDeserializerFactory(PublicationSource.class, PublicationSourceModel));
        QName ReactionModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "Reaction");
        call.registerTypeMapping(Reaction.class, ReactionModel,
              new BeanSerializerFactory(Reaction.class, ReactionModel),
              new BeanDeserializerFactory(Reaction.class, ReactionModel));
        QName ReferenceChemicalModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "ReferenceChemical");
        call.registerTypeMapping(ReferenceChemical.class, ReferenceChemicalModel,
              new BeanSerializerFactory(ReferenceChemical.class, ReferenceChemicalModel),
              new BeanDeserializerFactory(ReferenceChemical.class, ReferenceChemicalModel));
        QName ReferenceEntityModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "ReferenceEntity");
        call.registerTypeMapping(ReferenceEntity.class, ReferenceEntityModel,
              new BeanSerializerFactory(ReferenceEntity.class, ReferenceEntityModel),
              new BeanDeserializerFactory(ReferenceEntity.class, ReferenceEntityModel));
        QName ReferenceGeneModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "ReferenceGene");
        call.registerTypeMapping(ReferenceGene.class, ReferenceGeneModel,
              new BeanSerializerFactory(ReferenceGene.class, ReferenceGeneModel),
              new BeanDeserializerFactory(ReferenceGene.class, ReferenceGeneModel));
        QName ReferenceProteinModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "ReferenceProtein");
        call.registerTypeMapping(ReferenceProtein.class, ReferenceProteinModel,
              new BeanSerializerFactory(ReferenceProtein.class, ReferenceProteinModel),
              new BeanDeserializerFactory(ReferenceProtein.class, ReferenceProteinModel));
        QName ReferenceRNAModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "ReferenceRNA");
        call.registerTypeMapping(ReferenceRNA.class, ReferenceRNAModel,
              new BeanSerializerFactory(ReferenceRNA.class, ReferenceRNAModel),
              new BeanDeserializerFactory(ReferenceRNA.class, ReferenceRNAModel));
        QName ReferenceSequenceModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "ReferenceSequence");
        call.registerTypeMapping(ReferenceSequence.class, ReferenceSequenceModel,
              new BeanSerializerFactory(ReferenceSequence.class, ReferenceSequenceModel),
              new BeanDeserializerFactory(ReferenceSequence.class, ReferenceSequenceModel));
        QName RegulationModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "Regulation");
        call.registerTypeMapping(Regulation.class, RegulationModel,
              new BeanSerializerFactory(Regulation.class, RegulationModel),
              new BeanDeserializerFactory(Regulation.class, RegulationModel));
        QName RegulationTypeModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "RegulationType");
        call.registerTypeMapping(RegulationType.class, RegulationTypeModel,
              new EnumSerializerFactory(RegulationType.class, RegulationTypeModel),
              new EnumDeserializerFactory(RegulationType.class, RegulationTypeModel));
        QName RegulatorModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "Regulator");
        call.registerTypeMapping(Regulator.class, RegulatorModel,
              new BeanSerializerFactory(Regulator.class, RegulatorModel),
              new BeanDeserializerFactory(Regulator.class, RegulatorModel));
        QName SmallMoleculeEntityModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "SmallMoleculeEntity");
        call.registerTypeMapping(SmallMoleculeEntity.class, SmallMoleculeEntityModel,
              new BeanSerializerFactory(SmallMoleculeEntity.class, SmallMoleculeEntityModel),
              new BeanDeserializerFactory(SmallMoleculeEntity.class, SmallMoleculeEntityModel));
        QName SummationModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "Summation");
        call.registerTypeMapping(Summation.class, SummationModel,
              new BeanSerializerFactory(Summation.class, SummationModel),
              new BeanDeserializerFactory(Summation.class, SummationModel));
        QName TaxonModel= new QName("http://www.reactome.org/caBIOWebApp/schema", "Taxon");
        call.registerTypeMapping(Taxon.class, TaxonModel,
              new BeanSerializerFactory(Taxon.class, TaxonModel),
              new BeanDeserializerFactory(Taxon.class, TaxonModel));
        QName arrayModel = new QName("http://www.reactome.org/caBIOWebApp/services/caBIOService", "ArrayOf_xsd_anyType");
        QName componentModel = new QName("http://www.w3.org/2001/XMLSchema", "anyType");
        call.registerTypeMapping(Object[].class, arrayModel,
                new ArraySerializerFactory(Object.class, componentModel),
                new ArrayDeserializerFactory(componentModel));
        arrayModel = new QName("http://www.reactome.org/caBIOWebApp/schema", "ArrayOfAnyType");
        componentModel = new QName("http://www.w3.org/2001/XMLSchema", "anyType");
        call.registerTypeMapping(Object.class, arrayModel,
                new ArraySerializerFactory(Object.class, componentModel),
                new ArrayDeserializerFactory(componentModel));        
    }
}
