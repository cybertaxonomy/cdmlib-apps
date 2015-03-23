/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.redlist.bfnXml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.management.ObjectInstance;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.id.UUIDGenerator;
import org.jdom.Element;
import org.jdom.Namespace;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.IClassificationService;
import eu.etaxonomy.cdm.api.service.IDescriptionService;
import eu.etaxonomy.cdm.api.service.IFeatureNodeService;
import eu.etaxonomy.cdm.api.service.ITaxonService;
import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.api.service.IVocabularyService;
import eu.etaxonomy.cdm.common.ResultWrapper;
import eu.etaxonomy.cdm.common.XmlHelp;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.MapWrapper;
import eu.etaxonomy.cdm.model.common.DefinedTermBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.common.TermType;
import eu.etaxonomy.cdm.model.common.TermVocabulary;
import eu.etaxonomy.cdm.model.common.VocabularyEnum;
import eu.etaxonomy.cdm.model.description.CategoricalData;
import eu.etaxonomy.cdm.model.description.DescriptionBase;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.FeatureNode;
import eu.etaxonomy.cdm.model.description.FeatureTree;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.description.StateData;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaLevel;
import eu.etaxonomy.cdm.model.location.NamedAreaType;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NonViralName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymRelationshipType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.persistence.dao.description.IFeatureDao;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;
import eu.etaxonomy.cdm.strategy.parser.ParserProblem;
/**
 * 
 * @author a.oppermann
 * @date 04.07.2013
 *
 */
@Component
public class BfnXmlImportNamedArea extends BfnXmlImportBase implements ICdmIO<BfnXmlImportState> {
	private static final Logger logger = Logger.getLogger(BfnXmlImportNamedArea.class);

	public BfnXmlImportNamedArea(){
		super();
	}

	@Override
	public boolean doCheck(BfnXmlImportState state){
		boolean result = true;
		//TODO needs to be implemented
		return result;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void doInvoke(BfnXmlImportState state){
		logger.info("create german federal states ...");
		IVocabularyService vocabularyService = getVocabularyService();
		ITermService termService = getTermService();
		TransactionStatus tx = startTransaction();
		createGermanNamedAreas(state, vocabularyService, termService);
		commitTransaction(tx);
		logger.info("end create german federal states.");
		return;

	}


	private void createGermanNamedAreas(BfnXmlImportState state ,IVocabularyService vocabularyService, ITermService termService) {
		int id = 0;
		for(String strGermanState:GERMAN_FEDERAL_STATES){
			UUID germanStateUUID;
			try {
				germanStateUUID = BfnXmlTransformer.getGermanStateUUID(strGermanState);
			} catch (UnknownCdmTypeException e) {
				// TODO Auto-generated catch block
				logger.warn("Could not match german state to uuid: "+e.toString());
				germanStateUUID = UUID.randomUUID();
			}
			NamedArea germanState = (NamedArea)termService.load(germanStateUUID);
			if(germanState != null){
				//already in the db, so no need to step through the whole process again.
				return;
			}else{
				germanState = NamedArea.NewInstance(strGermanState, strGermanState, strGermanState);
				germanState.setUuid(germanStateUUID);
				germanState.setType(NamedAreaType.ADMINISTRATION_AREA());
				germanState.setIdInVocabulary(Integer.toString(id));;
				if(strGermanState.equalsIgnoreCase("Deutschland")){
					germanState.setLevel(NamedAreaLevel.COUNTRY());
				}else{
					germanState.setLevel(NamedAreaLevel.STATE());
				}
			}
			createOrUpdateTermVocabulary(TermType.NamedArea, vocabularyService, germanState, "German Federal States");
			id++;
		}
	}


	/**
	 * @param vocabularyService
	 * @param term
	 * @param vocUUID 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private TermVocabulary createOrUpdateTermVocabulary(TermType termType, IVocabularyService vocabularyService, DefinedTermBase term, String strTermVocabulary) {
		TermVocabulary termVocabulary = null;
		UUID vocUUID = null;
		try {
			vocUUID=BfnXmlTransformer.getRedlistVocabularyUUID(strTermVocabulary);
		} catch (UnknownCdmTypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(vocUUID != null){
			termVocabulary = vocabularyService.load(vocUUID);
		}
		//lookup via String in case uuid lookup does not work
		if(termVocabulary == null && vocUUID == null){
			List<TermVocabulary> vocList = vocabularyService.list(TermVocabulary.class, null, null, null, VOC_CLASSIFICATION_INIT_STRATEGY);
			for(TermVocabulary tv : vocList){
				if(tv.getTitleCache().equalsIgnoreCase(strTermVocabulary)){
					termVocabulary = tv;
				}
			}
		}
		//create termvocabulary
		if(termVocabulary == null){
			termVocabulary = TermVocabulary.NewInstance(termType, strTermVocabulary, strTermVocabulary, strTermVocabulary, null);
			if(vocUUID != null){
				termVocabulary.setUuid(vocUUID);
			}
		}
		termVocabulary.addTerm(term);			
		vocabularyService.saveOrUpdate(termVocabulary);
		
		return termVocabulary;
	}

		
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	protected boolean isIgnore(BfnXmlImportState state){
		return ! state.getConfig().isDoTaxonNames();
	}
	
    private static final List<String> GERMAN_FEDERAL_STATES = Arrays.asList(new String[] {
    		"Deutschland",
    		"Baden-Württemberg",
    		"Bayern",
    		"Berlin",
            "Brandenburg",
            "Hamburg",
            "Hessen",
            "Mecklenburg-Vorpommern",
            "Niedersachsen",
            "Nordrhein-Westfalen",
            "Rheinland-Pfalz",
            "Saarland",
            "Sachsen",
            "Sachsen-Anhalt",
            "Schleswig-Holstein",
            "Thüringen"
    });
	
	
    /** Hibernate classification vocabulary initialisation strategy */
    private static final List<String> VOC_CLASSIFICATION_INIT_STRATEGY = Arrays.asList(new String[] {
    		"classification.$",
    		"classification.rootNodes",
    		"childNodes",
    		"childNodes.taxon",
            "childNodes.taxon.name",
            "taxonNodes",
            "taxonNodes.taxon",
            "synonymRelations",
            "taxon.*",
            "taxon.sec",
            "taxon.name.*",
            "taxon.synonymRelations",
            "termVocabulary.*",
            "terms",
            "namedArea"

    });


}
