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

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.Namespace;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.IVocabularyService;
import eu.etaxonomy.cdm.common.ResultWrapper;
import eu.etaxonomy.cdm.common.XmlHelp;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.model.common.DefinedTermBase;
import eu.etaxonomy.cdm.model.common.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.common.TermType;
import eu.etaxonomy.cdm.model.common.TermVocabulary;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.FeatureTree;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;
/**
 *
 * @author a.oppermann
 * @date 04.07.2013
 *
 */
@Component
public class BfnXmlImportFeature extends BfnXmlImportBase implements ICdmIO<BfnXmlImportState> {
	private static final Logger logger = Logger.getLogger(BfnXmlImportFeature.class);

	public BfnXmlImportFeature(){
		super();
	}


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
            "terms"

    });

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void doInvoke(BfnXmlImportState state){

		IVocabularyService vocabularyService = getVocabularyService();


		logger.warn("start create Features in CDM...");
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

				if(currentElement.getName().equalsIgnoreCase("ROTELISTEDATEN")){

					TransactionStatus tx = startTransaction();

					childName = "EIGENSCHAFTEN";
					obligatory = false;
					Element elFeatureNames = XmlHelp.getSingleChildElement(success, currentElement, childName, bfnNamespace, obligatory);

					String bfnElementName = "EIGENSCHAFT";
					List<Element> elFeatureList = elFeatureNames.getChildren(bfnElementName, bfnNamespace);
					List<Feature> featureList = new ArrayList<Feature>();
					//for each taxonName
					for (Element elFeature : elFeatureList){

						if(elFeature.getAttributeValue("standardname", bfnNamespace).equalsIgnoreCase("RL Kat.")){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, state);
						}
						String featureLabel = "Kat. +/-";
						if(elFeature.getAttributeValue("standardname").equalsIgnoreCase(featureLabel)){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, state);
						}else
						if(elFeature.getAttributeValue("standardname").equalsIgnoreCase("aktuelle Bestandsstituation")){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, state);
						}else
						if(elFeature.getAttributeValue("standardname").equalsIgnoreCase("langfristiger Bestandstrend")){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, state);
						}else
						if(elFeature.getAttributeValue("standardname").equalsIgnoreCase("kurzfristiger Bestandstrend")){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, state);
						}else
						if(elFeature.getAttributeValue("standardname").equalsIgnoreCase("Risikofaktoren")){
						    makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, state);
						}else
						if(elFeature.getAttributeValue("standardname").equalsIgnoreCase("Verantwortlichkeit")){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, state);
						}else
						if(elFeature.getAttributeValue("standardname").equalsIgnoreCase("alte RL- Kat.")){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, state);
						}else
						if(elFeature.getAttributeValue("standardname").equalsIgnoreCase("Neobiota")){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, state);
						}else
						if(elFeature.getAttributeValue("standardname").equalsIgnoreCase("Eindeutiger Code")){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, state);
						}else
						if(elFeature.getAttributeValue("standardname").equalsIgnoreCase("Kommentar zur Taxonomie")){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, state);
						}else
						if(elFeature.getAttributeValue("standardname").equalsIgnoreCase("Kommentar zur Gefährdung")){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, state);
						}else
						if(elFeature.getAttributeValue("standardname").equalsIgnoreCase("Sonderfälle")){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, state);
						}else
						if(elFeature.getAttributeValue("standardname").equalsIgnoreCase("Letzter Nachweis")){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, state);
						}else
						if(elFeature.getAttributeValue("standardname").equalsIgnoreCase("Weitere Kommentare")){
							makeFeature(vocabularyService, featureList,success, obligatory, bfnNamespace,elFeature, state);
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

	/**
	 * @param featureList
	 */
	private void createFeatureTree(List<Feature> featureList) {
		FeatureTree featureTree = FeatureTree.NewInstance(featureList);
		String featureTreeName = "RedListFeatureTree";
		featureTree.setTitleCache(featureTreeName, true);
		getFeatureTreeService().save(featureTree);
	}

	/**
	 *
	 * @param vocabularyService
	 * @param featureList
	 * @param success
	 * @param obligatory
	 * @param bfnNamespace
	 * @param elFeature
	 * @param state
	 */
	private void makeFeature(IVocabularyService vocabularyService,
			List<Feature> featureList,
			ResultWrapper<Boolean> success, boolean obligatory,
			Namespace bfnNamespace, Element elFeature, BfnXmlImportState state) {
		String childName;
		String strRlKat = elFeature.getAttributeValue("standardname");
		UUID featureUUID = null;
		try {
			featureUUID = BfnXmlTransformer.getRedlistFeatureUUID(strRlKat);
		} catch (UnknownCdmTypeException e) {
			e.printStackTrace();
		}
		Feature redListCat = getFeature(state, featureUUID, strRlKat, strRlKat, strRlKat, null);
		redListCat.setSupportsCategoricalData(true);
		//TODO implement German, but currently titleCache generation does not yet work correctly with another language
//		redListCat.getRepresentation(Language.DEFAULT()).setLanguage(Language.GERMAN());
		featureList.add(redListCat);
		childName = "LISTENWERTE";
		Element elListValues = XmlHelp.getSingleChildElement(success, elFeature, childName, bfnNamespace, obligatory);
		if(elListValues != null && !elListValues.getContent().isEmpty()){
			String childElementName = "LWERT";
			createOrUpdateStates(bfnNamespace, elListValues, childElementName, redListCat, state);
		}
		createOrUpdateTermVocabulary(TermType.Feature, vocabularyService, redListCat, "RedList Feature");
	}

	/**
	 * @param vocabularyService
	 * @param term
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private TermVocabulary createOrUpdateTermVocabulary(TermType termType, IVocabularyService vocabularyService, DefinedTermBase term, String strTermVocabulary) {
		TermVocabulary termVocabulary = null;
		List<TermVocabulary> vocList = vocabularyService.list(TermVocabulary.class, null, null, null, VOC_CLASSIFICATION_INIT_STRATEGY);
		for(TermVocabulary tv : vocList){
			if(tv.getTitleCache().equalsIgnoreCase(strTermVocabulary)){
				termVocabulary = tv;
			}
		}
		if(termVocabulary == null){
			termVocabulary = TermVocabulary.NewInstance(termType, strTermVocabulary, strTermVocabulary, strTermVocabulary, null);
		}
		termVocabulary.addTerm(term);
		vocabularyService.saveOrUpdate(termVocabulary);

		return termVocabulary;
	}


	/**
	 * @param success
	 * @param bfnNamespace
	 * @param elListValues
	 * @param childElementName
	 * @param redListCat
	 */

	@SuppressWarnings({ "unchecked", "rawtypes"})
	private void createOrUpdateStates(Namespace bfnNamespace,
	        Element elListValues,
	        String childElementName,
			Feature redListCat,
			BfnXmlImportState state) {

		List<Element> elListValueList = elListValues.getChildren(childElementName, bfnNamespace);
//		List<StateData> stateList = new ArrayList<StateData>();

		OrderedTermVocabulary termVocabulary = null;
		for(Element elListValue:elListValueList){
			String listValue = elListValue.getTextNormalize();
			String matchedListValue;
			UUID stateTermUuid = null;
			UUID vocabularyStateUuid = null;
			try {
				vocabularyStateUuid = BfnXmlTransformer.getRedlistVocabularyUUID(redListCat.toString());
			} catch (UnknownCdmTypeException e1) {
				vocabularyStateUuid = UUID.randomUUID();
				logger.warn("Element: " + listValue + "\n"+ e1);
			}
			try {
				matchedListValue = BfnXmlTransformer.redListString2RedListCode(listValue);
			} catch (UnknownCdmTypeException e) {
				matchedListValue = listValue;
				logger.warn("No matched red list code found for \""+redListCat.toString()+":" + listValue + "\". Use original label instead. ");
			}
			try {
				stateTermUuid = BfnXmlTransformer.getRedlistStateTermUUID(matchedListValue, redListCat.getTitleCache());
			} catch (UnknownCdmTypeException e) {
//				stateTermUuid = UUID.randomUUID();
				//TODO: needs to be fixed for "eindeutiger Code"
				logger.warn("could not finde state term uuid for " + matchedListValue + " and redlist category"+ redListCat.getTitleCache()+"\n"+e);
			}
			String vocName = redListCat.toString()+" States";
			termVocabulary = (OrderedTermVocabulary) getVocabulary(TermType.State, vocabularyStateUuid, vocName, vocName, vocName, null, true, null);
			State stateTerm = getStateTerm(state, stateTermUuid, matchedListValue, matchedListValue, matchedListValue, termVocabulary);
		}
		if(termVocabulary != null){
			redListCat.addSupportedCategoricalEnumeration(termVocabulary);
			getTermService().saveOrUpdate(redListCat);
		}

	}


    @Override
    public boolean doCheck(BfnXmlImportState state){
        boolean result = true;
        //TODO needs to be implemented
        return result;
    }

    @Override
	protected boolean isIgnore(BfnXmlImportState state){
		return ! state.getConfig().isDoFeature();
	}


}