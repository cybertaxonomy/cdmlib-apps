/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.bfnXml.in;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom.Element;
import org.jdom.Namespace;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.IVocabularyService;
import eu.etaxonomy.cdm.common.ResultWrapper;
import eu.etaxonomy.cdm.common.XmlHelp;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.io.redlist.bfnXml.BfnXmlConstants;
import eu.etaxonomy.cdm.model.common.IdentifiableEntity;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.term.DefinedTermBase;
import eu.etaxonomy.cdm.model.term.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.term.TermTree;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;
/**
 *
 * @author a.oppermann
 * @since 04.07.2013
 */
@Component
public class BfnXmlImportFeature extends BfnXmlImportBase implements ICdmIO<BfnXmlImportState> {

    private static final long serialVersionUID = 3545757825059662424L;
    private static final Logger logger = LogManager.getLogger();

	public BfnXmlImportFeature(){
		super();
	}


	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void doInvoke(BfnXmlImportState state){
	    logger.info("start create Features in CDM...");

		IVocabularyService vocabularyService = getVocabularyService();

		ResultWrapper<Boolean> success = ResultWrapper.NewInstance(true);
		String childName;
		boolean obligatory;
		BfnXmlImportConfigurator config = state.getConfig();
		Element elDataSet = getDataSetElement(config);
		Namespace bfnNamespace = config.getBfnXmlNamespace();

		List contentXML = elDataSet.getContent();
		Element currentElement = null;

		for(Object object:contentXML){

			if(object instanceof Element){
				currentElement = (Element)object;

				if(currentElement.getName().equalsIgnoreCase(BfnXmlConstants.EL_ROTELISTEDATEN)){

					TransactionStatus tx = startTransaction();

					childName = BfnXmlConstants.EL_EIGENSCHAFTEN;
					obligatory = false;
					Element elFeatureNames = XmlHelp.getSingleChildElement(success, currentElement, childName, bfnNamespace, obligatory);

					String bfnElementName = BfnXmlConstants.EL_EIGENSCHAFT;
					List<Element> elFeatureList = elFeatureNames.getChildren(bfnElementName, bfnNamespace);
					List<Feature> featureList = new ArrayList<>();
					//for each taxonName
					for (Element elFeature : elFeatureList){

						if(elFeature.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME, bfnNamespace).equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, true, state);
						}
						if(elFeature.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_KAT)){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, true, state);
						}else
						if(elFeature.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, true, state);
						}else
						if(elFeature.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, true, state);
						}else
						if(elFeature.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, true, state);
						}else
						if(elFeature.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_RISIKOFAKTOREN)){
						    makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, true, state);
						}else
						if(elFeature.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, true, state);
						}else
						if(elFeature.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, true, state);
						}else
						if(elFeature.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_NEOBIOTA)){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, true, state);
						}else
						if(elFeature.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_EINDEUTIGER_CODE)){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, true, state);
						}else
						if(elFeature.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.FEAT_KOMMENTAR_TAXONOMIE)){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, false, state);
						}else
						if(elFeature.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.FEAT_KOMMENTAR_GEFAEHRDUNG)){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, false, state);
						}else
						if(elFeature.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_SONDERFAELLE)){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, true, state);
						}else
						if(elFeature.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.FEAT_LETZTER_NACHWEIS)){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, false, state);
						}else
						if(elFeature.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.FEAT_WEITERE_KOMMENTARE)){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, false, state);
						}
					}
					createFeatureTree(featureList);
					commitTransaction(tx);

					logger.info("end create features ...");

					if (!success.getValue()){
						state.setUnsuccessfull();
					}
					return;
				}
			}
		}
		return;

	}

	private void createFeatureTree(List<Feature> featureList) {
	    TermTree<Feature> featureTree = TermTree.NewFeatureInstance(featureList);
		String featureTreeName = "RedListFeatureTree";
		featureTree.setTitleCache(featureTreeName, true);
		getTermTreeService().save(featureTree);
	}

	private void makeFeature(IVocabularyService vocabularyService,
			List<Feature> featureList,
			ResultWrapper<Boolean> success, boolean obligatory,
			Namespace bfnNamespace, Element elFeature, boolean supportsCategoricalData, BfnXmlImportState state) {
		String childName;
		String strRlKat = elFeature.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME);
		UUID featureUUID = null;
		try {
			featureUUID = BfnXmlTransformer.getRedlistFeatureUUID(strRlKat);
		} catch (UnknownCdmTypeException e) {
			e.printStackTrace();
		}
		Feature redListCat = getFeature(state, featureUUID, strRlKat, strRlKat, strRlKat, null);
		addSource(redListCat, state);
		redListCat.setSupportsCategoricalData(supportsCategoricalData);
		//TODO implement German, but currently titleCache generation does not yet work correctly with another language
//		redListCat.getRepresentation(Language.DEFAULT()).setLanguage(Language.GERMAN());
		featureList.add(redListCat);
		childName = BfnXmlConstants.EL_LISTENWERTE;
		Element elListValues = XmlHelp.getSingleChildElement(success, elFeature, childName, bfnNamespace, obligatory);
		if(elListValues != null && !elListValues.getContent().isEmpty()){
			String childElementName = BfnXmlConstants.EL_LWERT;
			createOrUpdateStates(bfnNamespace, elListValues, childElementName, redListCat, state);
		}
		TermVocabulary<?> voc = createOrUpdateTermVocabulary(state, TermType.Feature, vocabularyService, redListCat, BfnXmlConstants.VOC_REDLIST_FEATURES);
	    addSource(voc, state);
	}

	/**
     * @param redListCat
     * @param state
     */
    private void addSource(IdentifiableEntity<?> redListCat, BfnXmlImportState state) {
        if (redListCat.getSources().isEmpty()){
            String id = null;
            String idNamespace = null;
            String detail = null;
            redListCat.addImportSource(id, idNamespace, state.getCompleteSourceRef(), detail);
        }
    }


    @SuppressWarnings({ "rawtypes" })
	private TermVocabulary createOrUpdateTermVocabulary(BfnXmlImportState state, TermType termType, IVocabularyService vocabularyService, DefinedTermBase term, String strTermVocabulary) {

        //create/get red list feature vocabulary
        TermVocabulary<DefinedTermBase> termVocabulary = getVocabulary(state, termType, BfnXmlTransformer.vocRLFeatures, BfnXmlConstants.VOC_REDLIST_FEATURES, BfnXmlConstants.VOC_REDLIST_FEATURES, BfnXmlConstants.VOC_REDLIST_FEATURES, null, false, null);
		termVocabulary.addTerm(term);
		vocabularyService.saveOrUpdate(termVocabulary);

		return termVocabulary;
	}

	@SuppressWarnings({ "unchecked", "rawtypes"})
	private void createOrUpdateStates(Namespace bfnNamespace,
	        Element elListValues,
	        String childElementName,
			Feature redListCat,
			BfnXmlImportState state) {

		List<Element> elListValueList = elListValues.getChildren(childElementName, bfnNamespace);

		OrderedTermVocabulary stateVocabulary = null;
		for(Element elListValue:elListValueList){
			String listValue = elListValue.getTextNormalize();
			String matchedListValue;
			UUID stateTermUuid = null;
			UUID vocabularyStateUuid = null;
			try {
				vocabularyStateUuid = BfnXmlTransformer.getRedlistVocabularyUUID(redListCat.getLabel());
			} catch (UnknownCdmTypeException e1) {
				vocabularyStateUuid = UUID.randomUUID();
				logger.warn("Element: " + listValue + "\n"+ e1);
			}
			try {
				matchedListValue = BfnXmlTransformer.redListString2RedListCode(listValue);
			} catch (UnknownCdmTypeException e) {
				matchedListValue = listValue;
				logger.warn("No matched red list code found for \"" + redListCat.getTitleCache() + ":" + listValue + "\". Use original label instead. ");
			}
			try {
				stateTermUuid = BfnXmlTransformer.getRedlistStateTermUUID(matchedListValue, redListCat.getTitleCache());
			} catch (UnknownCdmTypeException e) {
//				stateTermUuid = UUID.randomUUID();
				//TODO: needs to be fixed for "eindeutiger Code"
				logger.warn("could not finde state term uuid for " + matchedListValue + " and redlist category"+ redListCat.getTitleCache()+"\n"+e);
			}
			String vocName = redListCat.getTitleCache() + " States";
			stateVocabulary = (OrderedTermVocabulary) getVocabulary(state, TermType.State, vocabularyStateUuid, vocName, vocName, vocName, null, true, null);
	        addSource(stateVocabulary, state);
			State stateTerm = getStateTerm(state, stateTermUuid, matchedListValue, matchedListValue, matchedListValue, stateVocabulary);
			addSource(stateTerm, state);
			if(stateVocabulary != null){
			    redListCat.addSupportedCategoricalEnumeration(stateVocabulary);
			    getTermService().saveOrUpdate(redListCat);
			}
			stateVocabulary = null;
		}
	}


    @Override
    public boolean doCheck(BfnXmlImportState state){
        boolean result = true;
        return result;
    }

    @Override
	protected boolean isIgnore(BfnXmlImportState state){
		return ! state.getConfig().isDoFeature();
	}

}
