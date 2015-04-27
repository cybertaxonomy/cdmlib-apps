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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.Namespace;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.IClassificationService;
import eu.etaxonomy.cdm.api.service.ITaxonService;
import eu.etaxonomy.cdm.common.ResultWrapper;
import eu.etaxonomy.cdm.common.XmlHelp;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.DefinedTermBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.TermVocabulary;
import eu.etaxonomy.cdm.model.description.CategoricalData;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.NonViralName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymRelationshipType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;
import eu.etaxonomy.cdm.strategy.parser.ParserProblem;
/**
 *
 * @author a.oppermann
 * @date 04.07.2013
 *
 */
//@Component("bfnXmlTaxonNameIO")
@Component
public class BfnXmlImportTaxonName extends BfnXmlImportBase implements ICdmIO<BfnXmlImportState> {


	private static final Logger logger = Logger.getLogger(BfnXmlImportTaxonName.class);

	private static String strNomenclaturalCode = null;// "Zoological";//"Botanical";
	private static int parsingProblemCounter = 0;
	private Map<Integer, Taxon> firstList;
	private Map<Integer, Taxon> secondList;


	public BfnXmlImportTaxonName(){
		super();
	}

	@Override
	public boolean doCheck(BfnXmlImportState state){
		boolean result = true;
		return result;
	}

	@Override
	@SuppressWarnings({"rawtypes" })
	public void doInvoke(BfnXmlImportState state){
		ITaxonService taxonService = getTaxonService();

		BfnXmlImportConfigurator config = state.getConfig();
		strNomenclaturalCode = config.getNomenclaturalSig();
		Element elDataSet = getDataSetElement(config);
		//TODO set Namespace
		Namespace bfnNamespace = config.getBfnXmlNamespace();

		List<?> contentXML = elDataSet.getContent();
		Element currentElement = null;
		for(Object object:contentXML){

			if(object instanceof Element){
				currentElement = (Element)object;
				//import taxon lists
				if(currentElement.getName().equalsIgnoreCase("ROTELISTEDATEN")){
					TransactionStatus tx = startTransaction();
					Map<UUID, TaxonBase> savedTaxonMap = extractTaxonNames(state, taxonService, config, currentElement, bfnNamespace);
					createOrUdateClassification(config, taxonService, savedTaxonMap, currentElement, state);
					commitTransaction(tx);
				}//import concept relations of taxon lists
				if(config.isHasSecondList()){
					if(currentElement.getName().equalsIgnoreCase("KONZEPTBEZIEHUNGEN")){
						TransactionStatus tx = startTransaction();
						extractTaxonConceptRelationShips(bfnNamespace,currentElement);
						commitTransaction(tx);
					}
				}
			}
		}
		return;
	}

	/**
	 * This method will parse the XML concept relationships and tries to map them into cdm relationship types.
	 *
	 * @param bfnNamespace
	 * @param currentElement
	 */
	private void extractTaxonConceptRelationShips(Namespace bfnNamespace, Element currentElement) {
		String childName;
		String bfnElementName = "KONZEPTBEZIEHUNG";
		ResultWrapper<Boolean> success = ResultWrapper.NewInstance(true);
		List<Element> elConceptList = currentElement.getChildren(bfnElementName, bfnNamespace);
		List<TaxonBase> updatedTaxonList = new ArrayList<TaxonBase>();
		for(Element element:elConceptList){

			childName = "TAXONYM1";
			Element elTaxon1 = XmlHelp.getSingleChildElement(success, element, childName, bfnNamespace, false);
			String taxNr1 = elTaxon1.getAttributeValue("taxNr");
			int int1 = Integer.parseInt(taxNr1);
			Taxon taxon1 = firstList.get(int1);
			TaxonBase<?> taxonBase1 = getTaxonService().load(taxon1.getUuid());
			taxon1 = (Taxon)taxonBase1;

			childName = "TAXONYM2";
			Element elTaxon2 = XmlHelp.getSingleChildElement(success, element, childName, bfnNamespace, false);
			String taxNr2 = elTaxon2.getAttributeValue("taxNr");
			int int2 = Integer.parseInt(taxNr2);
			Taxon taxon2 = secondList.get(int2);
			TaxonBase<?> taxonBase2 = getTaxonService().load(taxon2.getUuid());
			taxon2 = (Taxon) taxonBase2;

			childName = "STATUS";
			Element elConceptStatus = XmlHelp.getSingleChildElement(success, element, childName, bfnNamespace, false);
			String conceptStatusValue = elConceptStatus.getValue();
			conceptStatusValue = conceptStatusValue.replaceAll("\u00A0", "").trim();
			TaxonRelationshipType taxonRelationType = null;
			/**
			 * This if case only exists because it was decided not to have a included_in relationship type.
			 */
			if(conceptStatusValue.equalsIgnoreCase("<")){
				taxon2.addTaxonRelation(taxon1, TaxonRelationshipType.INCLUDES(), null, null);
			}else{
				try {
					taxonRelationType = BfnXmlTransformer.concept2TaxonRelation(conceptStatusValue);
				} catch (UnknownCdmTypeException e) {
					e.printStackTrace();
				}
				taxon1.addTaxonRelation(taxon2, taxonRelationType , null, null);
			}
			if(taxonRelationType != null && taxonRelationType.equals(TaxonRelationshipType.ALL_RELATIONSHIPS())){
				List<TaxonRelationship> relationsFromThisTaxon = (List<TaxonRelationship>) taxon1.getRelationsFromThisTaxon();
				TaxonRelationship taxonRelationship = relationsFromThisTaxon.get(0);
				taxonRelationship.setDoubtful(true);
			}
			updatedTaxonList.add(taxon2);
			updatedTaxonList.add(taxon1);
		}
		getTaxonService().saveOrUpdate(updatedTaxonList);
		logger.info("taxon relationships imported...");
	}

	/**
	 * This method stores the current imported maps in global variables to make
	 * them later available for matching the taxon relationships between these
	 * imported lists.
	 *
	 * @param config
	 * @param taxonMap
	 */
	private void prepareListforConceptImport(BfnXmlImportConfigurator config,Map<Integer, Taxon> taxonMap) {
		if(config.isFillSecondList()){
			secondList = taxonMap;
		}else{
			firstList = taxonMap;
		}
	}

	/**
	 *
	 * @param state
	 * @param taxonService
	 * @param config
	 * @param elDataSet
	 * @param bfnNamespace
	 * @return
	 */
	private Map<UUID, TaxonBase> extractTaxonNames(BfnXmlImportState state,
			ITaxonService taxonService, BfnXmlImportConfigurator config,
			Element elDataSet, Namespace bfnNamespace) {
		logger.info("start make TaxonNames...");
		Map<Integer, Taxon> taxonMap = new LinkedHashMap<Integer, Taxon>();
		ResultWrapper<Boolean> success = ResultWrapper.NewInstance(true);
		String childName;
		boolean obligatory;
		String idNamespace = "TaxonName";

		childName = "TAXONYME";
		obligatory = false;
		Element elTaxonNames = XmlHelp.getSingleChildElement(success, elDataSet, childName, bfnNamespace, obligatory);

		String bfnElementName = "TAXONYM";
		List<Element> elTaxonList = elTaxonNames.getChildren(bfnElementName, bfnNamespace);

		//for each taxonName
		for (Element elTaxon : elTaxonList){
			//create Taxon
			String taxonId = elTaxon.getAttributeValue("taxNr");
			childName = "WISSNAME";
			Element elWissName = XmlHelp.getSingleChildElement(success, elTaxon, childName, bfnNamespace, obligatory);
			String childElementName = "NANTEIL";
			Taxon taxon = createOrUpdateTaxon(success, idNamespace, config, bfnNamespace, elWissName, childElementName, state);

			//for each synonym
			childName = "SYNONYME";
			Element elSynonyms = XmlHelp.getSingleChildElement(success, elTaxon, childName, bfnNamespace, obligatory);
			if(elSynonyms != null){
				childElementName = "SYNONYM";
				createOrUpdateSynonym(taxon, success, obligatory, bfnNamespace, childElementName,elSynonyms, taxonId, state);
			}
			//for vernacular name
			childName = "DEUTSCHENAMEN";
			Element elVernacularName = XmlHelp.getSingleChildElement(success, elTaxon, childName, bfnNamespace, obligatory);
			if(elVernacularName != null){
				childElementName = "DNAME";
				createOrUpdateVernacularName(taxon, bfnNamespace, childElementName, elVernacularName, state);
			}
			//for each information concerning the taxon element
			//TODO Information block
			if(config.isDoInformationImport()){
				childName = "INFORMATIONEN";
				Element elInformations = XmlHelp.getSingleChildElement(success, elTaxon, childName, bfnNamespace, obligatory);
				if(elInformations != null){
					childElementName = "BEZUGSRAUM";
					createOrUpdateInformation(taxon, bfnNamespace, childElementName,elInformations, state);
				}
			}
			taxonMap.put(Integer.parseInt(taxonId), taxon);
		}

		//Quick'n'dirty to set concept relationships between two imported list
		prepareListforConceptImport(config, taxonMap);

		Map<UUID, TaxonBase> savedTaxonMap = taxonService.saveOrUpdate((Collection)taxonMap.values());
		//FIXME: after first list don't import metadata yet
		//TODO: import information for second taxon list.
		config.setDoInformationImport(false);
		logger.info("end makeTaxonNames ...");
		if (!success.getValue()){
			state.setUnsuccessfull();
		}
		return savedTaxonMap;
	}




	/**
	 * This will put the prior imported list into a classification
	 *
	 * @param config
	 * @param taxonService
	 * @param config
	 * @param savedTaxonMap
	 * @param currentElement
	 * @param state
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private boolean createOrUdateClassification(BfnXmlImportConfigurator config, ITaxonService taxonService, Map<UUID, TaxonBase> savedTaxonMap, Element currentElement, BfnXmlImportState state) {
		boolean isNewClassification = true;
		String classificationName = state.getFirstClassificationName();
		if(config.isFillSecondList()){
			classificationName = state.getSecondClassificationName();
		}
//		if(classificationName == null){
//			classificationName = config.getClassificationName();
//		}
		//TODO make classification name dynamically depending on its value in the XML.
		Classification classification = Classification.NewInstance(classificationName+" "+currentElement.getAttributeValue("inhalt"), state.getCompleteSourceRef());
		classification.addImportSource(Integer.toString(classification.getId()), classification.getTitleCache(), state.getCompleteSourceRef(), state.getCurrentMicroRef().toString());
//		List<Classification> classificationList = getClassificationService().list(Classification.class, null, null, null, VOC_CLASSIFICATION_INIT_STRATEGY);
//		for(Classification c : classificationList){
//			if(c.getTitleCache().equalsIgnoreCase(classification.getTitleCache())){
//				classification = c;
//				isNewClassification = false;
//			}
//		}

//		ArrayList<TaxonBase> taxonBaseList = (ArrayList<TaxonBase>) taxonService.list(TaxonBase.class, null, null, null, VOC_CLASSIFICATION_INIT_STRATEGY);
		for(TaxonBase tb:savedTaxonMap.values()){
			if(tb instanceof Taxon){
				TaxonBase tbase = CdmBase.deproxy(tb, TaxonBase.class);
				Taxon taxon = (Taxon)tbase;
				taxon = CdmBase.deproxy(taxon, Taxon.class);
				classification.addChildTaxon(taxon, null, null);
			}
		}
		IClassificationService classificationService = getClassificationService();
		classificationService.saveOrUpdate(classification);
		//set boolean for reference and internal mapping of concept relations
		if(config.isHasSecondList()){
			config.setFillSecondList(true);
		}
		return isNewClassification;
	}



	/**
	 * Matches the XML attributes against CDM entities.<BR>
	 * Imports Scientific Name, Rank, etc. and creates a taxon.<br>
	 * <b>Existing taxon names won't be matched yet</b>
	 *
	 * @param success
	 * @param idNamespace
	 * @param config
	 * @param bfnNamespace
	 * @param elTaxonName
	 * @param childElementName
	 * @param state
	 * @return
	 */

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Taxon createOrUpdateTaxon(
			ResultWrapper<Boolean> success, String idNamespace,
			BfnXmlImportConfigurator config, Namespace bfnNamespace,
			Element elTaxonName, String childElementName, BfnXmlImportState state) {

		List<Element> elWissNameList = elTaxonName.getChildren(childElementName, bfnNamespace);
		Rank rank = null;
		String strAuthor = null;
		String strSupplement = null;
		Taxon taxon = null;
		String uniqueID = null;
		String uriNameSpace = null;
//		Long uniqueID = null;
		for(Element elWissName:elWissNameList){

			if(elWissName.getAttributeValue("bereich", bfnNamespace).equalsIgnoreCase("Eindeutiger Code")){
				uriNameSpace = elWissName.getAttributeValue("bereich");
				String textNormalize = elWissName.getTextNormalize();
				if(StringUtils.isBlank(textNormalize)){
					uniqueID = "";
				}else{
					uniqueID = textNormalize;
				}
			}
			if(elWissName.getAttributeValue("bereich", bfnNamespace).equalsIgnoreCase("Autoren")){
				strAuthor = elWissName.getTextNormalize();
			}
			if(elWissName.getAttributeValue("bereich", bfnNamespace).equalsIgnoreCase("Rang")){
				String strRank = elWissName.getTextNormalize();
				rank = makeRank(strRank);
			}
			if(elWissName.getAttributeValue("bereich", bfnNamespace).equalsIgnoreCase("Zusätze")){
				strSupplement = elWissName.getTextNormalize();
			}
			if(elWissName.getAttributeValue("bereich", bfnNamespace).equalsIgnoreCase("wissName")){
				try{
					TaxonNameBase<?, ?> nameBase = parseNonviralNames(rank,strAuthor,strSupplement,elWissName);
					if(nameBase.isProtectedTitleCache() == true){
						logger.warn("Taxon " + nameBase.getTitleCache());
					}

					//TODO  extract to method?
					if(strSupplement != null){
						nameBase.setAppendedPhrase(strSupplement);
					}
					if(strSupplement != null && strSupplement.equalsIgnoreCase("nom. illeg.")){
						nameBase.addStatus(NomenclaturalStatus.NewInstance(NomenclaturalStatusType.ILLEGITIMATE()));
					}
					/**
					 *  BFN does not want any name matching yet
					 */
//					TaxonBase<?> taxonBase = null;
//					//TODO find best matching Taxa
//					Pager<TaxonNameBase> names = getNameService().findByTitle(null, nameBase.getTitleCache(), null, null, null, null, null, null);
//					//TODO  correct handling for pager
//					List<TaxonNameBase> nameList = names.getRecords();
//					if (nameList.isEmpty()){
//						taxonBase = Taxon.NewInstance(nameBase, config.getSourceReference());
//					}else{
//						taxonBase = Taxon.NewInstance(nameList.get(0), config.getSourceReference());
//						if (nameList.size()>1){
//							logger.warn("More than 1 matching taxon name found for " + nameBase.getTitleCache());
//						}
//					}
					state.setCurrentMicroRef(state.getFirstListSecRef());
					if(config.isFillSecondList()){
						state.setCurrentMicroRef(state.getSecondListSecRef());
					}
					taxon = Taxon.NewInstance(nameBase, state.getCurrentMicroRef());
					//set create and set path of nameSpace
					Element parentElement = elWissName.getParentElement();
					Element grandParentElement = parentElement.getParentElement();
					taxon.addImportSource(uniqueID, grandParentElement.getName()+":"+parentElement.getName()+":"+elWissName.getName()+":"+uriNameSpace, state.getCompleteSourceRef(), state.getCurrentMicroRef().getTitle());
				} catch (UnknownCdmTypeException e) {
					success.setValue(false);
				}
			}
		}
		return taxon;
	}

	/**
	 * Matches the XML attributes against CDM entities.<BR>
	 * Imports Scientific Name, Rank etc. and create a synonym.<br>
	 * <b>Existing synonym names won't be matched yet</b>
	 *
	 * @param taxon
	 * @param success
	 * @param obligatory
	 * @param bfnNamespace
	 * @param childElementName
	 * @param elSynonyms
	 * @param taxonId
	 * @param config
	 * @param state
	 */

	@SuppressWarnings({ "unchecked" })
	private void createOrUpdateSynonym(Taxon taxon, ResultWrapper<Boolean> success, boolean obligatory, Namespace bfnNamespace,
			     String childElementName, Element elSynonyms, String taxonId, BfnXmlImportState state) {

		String childName;
		List<Element> elSynonymList = elSynonyms.getChildren(childElementName, bfnNamespace);

		for(Element elSyn:elSynonymList){
			Rank rank = null;
			String strAuthor = null;
			String strSupplement = null;
			childName = "WISSNAME";
			Element elSynScientificName = XmlHelp.getSingleChildElement(success, elSyn, childName, bfnNamespace, obligatory);

			childElementName = "NANTEIL";
			List<Element> elSynDetails = elSynScientificName.getChildren(childElementName, bfnNamespace);

			for(Element elSynDetail:elSynDetails){
				if(elSynDetail.getAttributeValue("bereich").equalsIgnoreCase("Rang")){
					String strRank = elSynDetail.getTextNormalize();
					rank = makeRank(strRank);
				}
				if(elSynDetail.getAttributeValue("bereich").equalsIgnoreCase("Autoren")){
					strAuthor = elSynDetail.getTextNormalize();
				}
				if(elSynDetail.getAttributeValue("bereich", bfnNamespace).equalsIgnoreCase("Zusätze")){
					strSupplement = elSynDetail.getTextNormalize();
				}
				if(elSynDetail.getAttributeValue("bereich").equalsIgnoreCase("wissName")){
					try{
						TaxonNameBase<?, ?> nameBase = parseNonviralNames(rank,strAuthor,strSupplement,elSynDetail);

						//TODO find best matching Taxa
						Synonym synonym = Synonym.NewInstance(nameBase, state.getCurrentMicroRef());
						taxon.addSynonym(synonym, SynonymRelationshipType.SYNONYM_OF());

					} catch (UnknownCdmTypeException e) {
						logger.warn("Name with id " + taxonId + " has unknown nomenclatural code.");
						success.setValue(false);
					}

				}

			}
		}
	}


	/**
	 *
	 * @param taxon
	 * @param bfnNamespace
	 * @param childElementName
	 * @param elVernacularName
	 * @param state
	 */
	private void createOrUpdateVernacularName(Taxon taxon,
			Namespace bfnNamespace, String childElementName,
			Element elVernacularName, BfnXmlImportState state) {

		List<Element> elVernacularNameList = elVernacularName.getChildren(childElementName, bfnNamespace);

		TaxonDescription taxonDescription = getTaxonDescription(taxon, false, true);

		for(Element elVernacular : elVernacularNameList){
			Element child = elVernacular.getChild("TRIVIALNAME");
			if(child != null){
				makeCommonName(taxonDescription, child, state);
			}
		}

	}

	/**
	 *
	 * @param taxon
	 * @param bfnNamespace
	 * @param childElementName
	 * @param elInformations
	 * @param state
	 * @throws UnknownCdmTypeException
	 */

	@SuppressWarnings("unchecked")
	private void createOrUpdateInformation(Taxon taxon,
			Namespace bfnNamespace, String childElementName,
			Element elInformations,
			BfnXmlImportState state){

		List<Element> elInformationList = elInformations.getChildren(childElementName, bfnNamespace);

		for(Element elInfo:elInformationList){
			//check if geographical scope is Bund and import only these information for now
			//TODO create several taxon descriptions for different geographical scope
			if(elInfo.getName().equalsIgnoreCase("BEZUGSRAUM") && elInfo.getAttributeValue("name").equalsIgnoreCase("Bund")){
				childElementName = "IWERT";
				TaxonDescription taxonDescription = getTaxonDescription(taxon, false, true);
				UUID germanStateUUID;
				try {
					germanStateUUID = BfnXmlTransformer.getGermanStateUUID("Deutschland");
					NamedArea area = (NamedArea)getTermService().load(germanStateUUID);
					//FIXME GEOSCOPE_ID CANNOT BE NULL Exception
//					taxonDescription.addGeoScope(area);
				} catch (UnknownCdmTypeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				List<Element> elInfoDetailList = elInfo.getChildren(childElementName, bfnNamespace);

				for(Element elInfoDetail : elInfoDetailList){
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("RL Kat.")){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("Kat. +/-")){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("aktuelle Bestandsstituation")){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("langfristiger Bestandstrend")){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("kurzfristiger Bestandstrend")){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("Risikofaktoren")){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("Verantwortlichkeit")){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("alte RL- Kat.")){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("Neobiota")){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("Eindeutiger Code")){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("Kommentar zur Taxonomie")){
						makeFeatures(taxonDescription, elInfoDetail, state, true);
					}
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("Kommentar zur Gefährdung")){
						makeFeatures(taxonDescription, elInfoDetail, state, true);
					}
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("Sonderfälle")){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("Letzter Nachweis")){
						makeFeatures(taxonDescription, elInfoDetail, state, true);
					}
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("Weitere Kommentare")){
						makeFeatures(taxonDescription, elInfoDetail, state, true);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("BW")){
                        createGermanDistributionStatus(taxon, elInfoDetail, state, taxonDescription);
                    }
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("BY")){
					    createGermanDistributionStatus(taxon, elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("BE")){
					    createGermanDistributionStatus(taxon, elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("BB")){
					    createGermanDistributionStatus(taxon, elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("HB")){
					    createGermanDistributionStatus(taxon, elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("HH")){
					    createGermanDistributionStatus(taxon, elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("HE")){
					    createGermanDistributionStatus(taxon, elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("MV")){
					    createGermanDistributionStatus(taxon, elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("NI")){
					    createGermanDistributionStatus(taxon, elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("NW")){
					    createGermanDistributionStatus(taxon, elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("RP")){
					    createGermanDistributionStatus(taxon, elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("SL")){
					    createGermanDistributionStatus(taxon, elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("SN")){
					    createGermanDistributionStatus(taxon, elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("ST")){
					    createGermanDistributionStatus(taxon, elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("SH")){
					    createGermanDistributionStatus(taxon, elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue("standardname").equalsIgnoreCase("TH")){
					    createGermanDistributionStatus(taxon, elInfoDetail, state, taxonDescription);
					}
				}
			}
		}
	}


    private void makeCommonName(TaxonDescription taxonDescription,
			Element child, BfnXmlImportState state) {
		String commonNameValue = child.getValue();
		NamedArea area = getTermService().getAreaByTdwgAbbreviation("GER");
		CommonTaxonName commonName = CommonTaxonName.NewInstance(commonNameValue, Language.GERMAN(), area);
		taxonDescription.addElement(commonName);
	}


	/**
	 *
	 * @param taxonDescription
	 * @param elInfoDetail
	 * @param state
	 * @param isTextData
	 */
	private void makeFeatures(
			TaxonDescription taxonDescription,
			Element elInfoDetail,
			BfnXmlImportState state,
			boolean isTextData) {

		String transformedRlKatValue = null;
		UUID featureUUID = null;
		UUID stateTermUUID = null;
		String strRlKatValue = elInfoDetail.getChild("WERT").getValue();
		String strRlKat = elInfoDetail.getAttributeValue("standardname");
		boolean randomStateUUID = false;
		try {
			featureUUID = BfnXmlTransformer.getRedlistFeatureUUID(strRlKat);
			transformedRlKatValue = BfnXmlTransformer.redListString2RedListCode(strRlKatValue);
		} catch (UnknownCdmTypeException e) {
			transformedRlKatValue = strRlKatValue;
		}
		Feature redListFeature = getFeature(state, featureUUID);
		State rlState = null;
		//if is text data a state is not needed
		if(!isTextData){
			try {
				stateTermUUID = BfnXmlTransformer.getRedlistStateTermUUID(transformedRlKatValue, strRlKat);
			} catch (UnknownCdmTypeException e) {
				stateTermUUID = UUID.randomUUID();
				randomStateUUID = true;
			}
			if(randomStateUUID || stateTermUUID == BfnXmlTransformer.stateTermEmpty){
				if(stateTermUUID == BfnXmlTransformer.stateTermEmpty) {
                    transformedRlKatValue = "keine Angabe";
                }
				rlState = getStateTerm(state, stateTermUUID, transformedRlKatValue, transformedRlKatValue, transformedRlKatValue, null);
			}else{
				rlState = getStateTerm(state, stateTermUUID);
			}
		}
		if(isTextData){
			TextData textData = TextData.NewInstance(redListFeature);
			textData.putText(Language.GERMAN(), strRlKatValue);
			DescriptionElementBase descriptionElement = textData;
			taxonDescription.addElement(descriptionElement);
		}else{
			CategoricalData catData = CategoricalData.NewInstance(rlState, redListFeature);
			DescriptionElementBase descriptionElement = catData;
			taxonDescription.addElement(descriptionElement);
		}
	}

	/**
	 * Returns the rank represented by the rank element.<br>
	 * Returns <code>null</code> if the element is null.<br>
	 * Returns <code>null</code> if the code and the text are both either empty or do not exists.<br>
	 * Returns the rank represented by the code attribute, if the code attribute is not empty and could be resolved.<br>
	 * If the code could not be resolved it returns the rank represented most likely by the elements text.<br>
	 * Returns UNKNOWN_RANK if code attribute and element text could not be resolved.
	 * @param strRank bfn rank element
	 * @return
	 */
	protected static Rank makeRank(String strRank){
		Rank result;
 		if (strRank == null){
			return null;
		}
		Rank codeRank = null;
		try {
			codeRank = BfnXmlTransformer.rankCode2Rank(strRank);
		} catch (UnknownCdmTypeException e1) {
			codeRank = Rank.UNKNOWN_RANK();
		}
		//codeRank exists
		if ( (codeRank != null) && !codeRank.equals(Rank.UNKNOWN_RANK())){
			result = codeRank;
		}
		//codeRank does not exist
		else{
			result = codeRank;
			logger.warn("string rank used, because code rank does not exist or was not recognized: " + codeRank.getTitleCache()+" "+strRank);
		}
		return result;
	}

	/**
	 * @param rank
	 * @param strAuthor
	 * @param strSupplement
	 * @param elWissName
	 * @return
	 * @throws UnknownCdmTypeException
	 */
	private TaxonNameBase<?, ?> parseNonviralNames(Rank rank, String strAuthor, String strSupplement, Element elWissName)
			throws UnknownCdmTypeException {
		TaxonNameBase<?,?> taxonNameBase = null;

		NomenclaturalCode nomCode = BfnXmlTransformer.nomCodeString2NomCode(strNomenclaturalCode);
		String strScientificName = elWissName.getTextNormalize();
		/**
		 *
		 * trim strScienctificName because sometimes
		 * getTextNormalize() does not removes all the
		 * whitespaces
		 *
		 **/
		strScientificName = StringUtils.trim(strScientificName);
		strScientificName = StringUtils.remove(strScientificName, "\u00a0");
		strScientificName = StringUtils.remove(strScientificName, "\uc281");

		if(strSupplement != null && !strSupplement.isEmpty()){
			strScientificName = StringUtils.remove(strScientificName, strSupplement);
		}
		NonViralName<?> nonViralName = null;
		NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();
		nonViralName = parser.parseFullName(strScientificName, nomCode, rank);
		if(nonViralName.hasProblem()){
			for(ParserProblem p:nonViralName.getParsingProblems()){
				logger.warn(++parsingProblemCounter + " " +nonViralName.getTitleCache() +" "+p.toString());
			}
		}
		//check for parsed rank
		Rank parsedRank = nonViralName.getRank();
		if(parsedRank != rank){
			nonViralName.setRank(rank);
		}
		//check for parsed author
		String parsedAuthor = nonViralName.getAuthorshipCache();
		strAuthor = StringUtils.trim(strAuthor);
		parsedAuthor = StringUtils.trim(parsedAuthor);
		if(parsedAuthor.equalsIgnoreCase(strAuthor)){
			logger.info("Taxon " + nonViralName.getTitleCache() +":"
					+"\t Author field: " + strAuthor +" and parsed AuthorshipCache: "+nonViralName.getAuthorshipCache());
		}
		taxonNameBase = nonViralName;
		return taxonNameBase;
	}

	/**
	 * This method will match the BFN XML status to a distribution status
	 * and map it to the german federal state area. The vocabulary needs to be
	 * created first by the Importer, in order to map the terms correctly. Have a look
	 * for further details at the file BfnXmlImportAdditionalTerms.
	 *
	 *
     * @param taxon, for saving the distribution and its status
     * @param elInfoDetail, keeps the details from the import, in this case the distribution detail
     * @param state, import state
     * @param germanState, the abbreviated label for the German state
     *
     */
    private void createGermanDistributionStatus(Taxon taxon, Element elInfoDetail, BfnXmlImportState state,
            TaxonDescription taxonDescription){

        String strDistributionValue = elInfoDetail.getChild("WERT").getValue();
        String strGermanState = elInfoDetail.getAttributeValue("standardname");
        //match DistributionValue
        UUID matchedDistributionUUID = null;
        PresenceAbsenceTerm status = null;
        try {
            matchedDistributionUUID = BfnXmlTransformer.matchDistributionValue(strDistributionValue);
            DefinedTermBase load = getTermService().load(matchedDistributionUUID);
            if(load.isInstanceOf(PresenceAbsenceTerm.class)) {
                status = CdmBase.deproxy(load, PresenceAbsenceTerm.class);
            }else{
                logger.warn(strDistributionValue + " is not PresenceAbsence Term " + load.getTitleCache() + " " + load.getTermType().toString());
                return;
            }
        } catch (UnknownCdmTypeException e1) {
            logger.warn("could not match xml value "+ strDistributionValue +" to distribution status for "+strGermanState);
            e1.printStackTrace();
            return;
        }
        //load vocabulary and german state
        UUID vocabularyUUID = null;
        TermVocabulary vocabulary = null;
        UUID stateUUID = null;

        try {
            stateUUID = BfnXmlTransformer.getGermanStateUUID(strGermanState);
        } catch (UnknownCdmTypeException e1) {
            logger.warn("could not match state" + strGermanState + " to UUID");
            e1.printStackTrace();
            return;
        }
        NamedArea area = (NamedArea)getTermService().load(stateUUID);

//        try {
//            vocabularyUUID =  BfnXmlTransformer.getRedlistVocabularyUUID("Bundesländer");
//            vocabulary = getVocabularyService().load(vocabularyUUID);
//        } catch (UnknownCdmTypeException e) {
//            logger.warn("could not load vocabulary");
//            e.printStackTrace();
//            return;
//        }
//        NamedArea area = null;
//        for(Object term: vocabulary){
//            //TODO match german state
//            NamedArea narea = (NamedArea) term;
//            Set<Representation> representations = narea.getRepresentations();
//            for(Representation r:representations){
//                if(r.getAbbreviatedLabel().equalsIgnoreCase(strGermanState)){
//                    area = narea;
//                }
//            }
//
//        }
        //create new taxon description
        DescriptionElementBase descriptionElement = Distribution.NewInstance(area, status);
        taxonDescription.addElement(descriptionElement);
    }


	@Override
	protected boolean isIgnore(BfnXmlImportState state){
		return ! state.getConfig().isDoTaxonNames();
	}
}
