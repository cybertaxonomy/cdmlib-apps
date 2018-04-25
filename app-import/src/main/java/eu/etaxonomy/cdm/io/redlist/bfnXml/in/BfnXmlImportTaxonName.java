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
import eu.etaxonomy.cdm.io.redlist.bfnXml.BfnXmlConstants;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.DefinedTermBase;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.Language;
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
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
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
 * @since 04.07.2013
 *
 */
//@Component("bfnXmlTaxonNameIO")
@Component
public class BfnXmlImportTaxonName extends BfnXmlImportBase {

    private static final long serialVersionUID = -6684136204048549833L;

    private static final Logger logger = Logger.getLogger(BfnXmlImportTaxonName.class);

	private static NomenclaturalCode nomenclaturalCode = null;
	private static int parsingProblemCounter = 0;
	private Map<Integer, Taxon> firstList;
	private Map<Integer, Taxon> secondList;

//    private ImportDeduplicationHelper<BfnXmlImportState> deduplicationHelper = (ImportDeduplicationHelper<BfnXmlImportState>)ImportDeduplicationHelper.NewInstance(this);

	public BfnXmlImportTaxonName(){
		super();
	}


	@Override
	@SuppressWarnings({"rawtypes" })
	public void doInvoke(BfnXmlImportState state){

		TransactionStatus tx = startTransaction();

		ITaxonService taxonService = getTaxonService();
		BfnXmlImportConfigurator config = state.getConfig();
		nomenclaturalCode = config.getNomenclaturalCode();
		Element elDataSet = getDataSetElement(config);
		//TODO set Namespace
		Namespace bfnNamespace = config.getBfnXmlNamespace();

		List<?> contentXML = elDataSet.getContent();
		Element currentElement = null;
		state.setFillSecondList(false);
		for(Object object:contentXML){

			if(object instanceof Element){
				currentElement = (Element)object;
				//import taxon lists
				if(currentElement.getName().equalsIgnoreCase(BfnXmlConstants.EL_ROTELISTEDATEN)){

					Map<UUID, TaxonBase> savedTaxonMap = extractTaxonNames(state, taxonService, config, currentElement, bfnNamespace);
					createOrUpdateClassification(state, taxonService, savedTaxonMap, currentElement);
				    state.setFillSecondList(true);

				}//import concept relations of taxon lists
				if(config.isHasSecondList()){
					if(currentElement.getName().equalsIgnoreCase(BfnXmlConstants.EL_KONZEPTBEZIEHUNGEN)){
						extractTaxonConceptRelationShips(bfnNamespace,currentElement);
					}
				}
			}
		}
        commitTransaction(tx);
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
		String bfnElementName = BfnXmlConstants.EL_KONZEPTBEZIEHUNG;
		ResultWrapper<Boolean> success = ResultWrapper.NewInstance(true);
		@SuppressWarnings("unchecked")
        List<Element> elConceptList = currentElement.getChildren(bfnElementName, bfnNamespace);
		List<TaxonBase> updatedTaxonList = new ArrayList<>();
		for(Element element:elConceptList){

			childName = "TAXONYM1";
			Element elTaxon1 = XmlHelp.getSingleChildElement(success, element, childName, bfnNamespace, false);
			String taxNr1 = elTaxon1.getAttributeValue(BfnXmlConstants.ATT_TAXNR);
			int int1 = Integer.parseInt(taxNr1);
			Taxon taxon1 = firstList.get(int1);
			TaxonBase<?> taxonBase1 = getTaxonService().load(taxon1.getUuid());
			taxon1 = (Taxon)taxonBase1;

			childName = "TAXONYM2";
			Element elTaxon2 = XmlHelp.getSingleChildElement(success, element, childName, bfnNamespace, false);
			String taxNr2 = elTaxon2.getAttributeValue(BfnXmlConstants.ATT_TAXNR);
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
				taxonRelationType = BfnXmlTransformer.getTaxonRelationForConceptCode(conceptStatusValue);
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
	 * @param state
	 * @param taxonMap
	 */
	private void prepareListforConceptImport(BfnXmlImportState state,Map<Integer, Taxon> taxonMap) {
		if(state.isFillSecondList()){
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
		Map<Integer, Taxon> taxonMap = new LinkedHashMap<>();
		ResultWrapper<Boolean> success = ResultWrapper.NewInstance(true);
		String childName;
		boolean obligatory;

		childName = BfnXmlConstants.EL_TAXONYME;
		obligatory = false;
		Element elTaxonNames = XmlHelp.getSingleChildElement(success, elDataSet, childName, bfnNamespace, obligatory);

		String bfnElementName = BfnXmlConstants.EL_TAXONYM;
		@SuppressWarnings("unchecked")
        List<Element> elTaxonList = elTaxonNames.getChildren(bfnElementName, bfnNamespace);

		//for each taxonName
		for (Element elTaxon : elTaxonList){
			//create Taxon
			String taxonId = elTaxon.getAttributeValue(BfnXmlConstants.ATT_TAXNR);
			String reihenfolge = elTaxon.getAttributeValue(BfnXmlConstants.ATT_REIHENFOLGE);
			childName = BfnXmlConstants.EL_WISSNAME;
			Element elWissName = XmlHelp.getSingleChildElement(success, elTaxon, childName, bfnNamespace, obligatory);
			String childElementName = BfnXmlConstants.EL_NANTEIL;
			Taxon taxon = createOrUpdateTaxon(success, taxonId, reihenfolge, config, bfnNamespace, elWissName, childElementName, state);

			//for each synonym
			childName = "SYNONYME";
			Element elSynonyms = XmlHelp.getSingleChildElement(success, elTaxon, childName, bfnNamespace, obligatory);
			if(elSynonyms != null){
				childElementName = "SYNONYM";
				createOrUpdateSynonym(taxon, success, obligatory, bfnNamespace, childElementName,elSynonyms, taxonId, state);
			}

			//for vernacular name
			childName = BfnXmlConstants.EL_DEUTSCHENAMEN;
			Element elVernacularName = XmlHelp.getSingleChildElement(success, elTaxon, childName, bfnNamespace, obligatory);
			if(elVernacularName != null){
				childElementName = BfnXmlConstants.EL_DNAME;
				createOrUpdateVernacularName(taxon, bfnNamespace, childElementName, elVernacularName, state);
			}

			//for each information concerning the taxon element
			//TODO Information block
			if(config.isDoInformationImport()){
				childName = BfnXmlConstants.EL_INFORMATIONEN;
				Element elInformations = XmlHelp.getSingleChildElement(success, elTaxon, childName, bfnNamespace, obligatory);
				if(elInformations != null){
				    createOrUpdateInformation(taxon, bfnNamespace, elInformations, state);
				}
			}
			taxonMap.put(Integer.parseInt(taxonId), taxon);
		}

		//Quick'n'dirty to set concept relationships between two imported list
		prepareListforConceptImport(state, taxonMap);

		Map<UUID, TaxonBase> savedTaxonMap = taxonService.saveOrUpdate((Collection)taxonMap.values());

		logger.info("end makeTaxonNames ...");
		if (!success.getValue()){
			state.setUnsuccessfull();
		}
		return savedTaxonMap;
	}




	/**
	 * This will put the prior imported list into a classification
	 *
	 * @param state
	 * @param taxonService
	 * @param state
	 * @param savedTaxonMap
	 * @param currentElement
	 * @param state
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private boolean createOrUpdateClassification(BfnXmlImportState state, ITaxonService taxonService, Map<UUID, TaxonBase> savedTaxonMap, Element currentElement) {
		boolean isNewClassification = true;
		String name = state.getFirstClassificationName() + " " + currentElement.getAttributeValue("inhalt");
		Classification classification = Classification.NewInstance(name, state.getCurrentSecRef());
		String microRef = null; //state.getCurrentSecRef() == null ? null : state.getCurrentSecRef().getTitleCache();
		classification.addImportSource(null, null, state.getCompleteSourceRef(), microRef);

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
		return isNewClassification;
	}


	/**
	 * Matches the XML attributes against CDM entities.<BR>
	 * Imports Scientific Name, Rank, etc. and creates a taxon.<br>
	 * <b>Existing taxon names won't be matched yet</b>
	 *
	 * @param success
	 * @param taxonId
	 * @param reihenfolge
	 * @param config
	 * @param bfnNamespace
	 * @param elTaxonName
	 * @param childElementName
	 * @param state
	 * @return
	 */

	@SuppressWarnings({ "unchecked" })
	private Taxon createOrUpdateTaxon(
			ResultWrapper<Boolean> success, String taxonId,
			String reihenfolge, BfnXmlImportConfigurator config, Namespace bfnNamespace,
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

			if(elWissName.getAttributeValue(BfnXmlConstants.ATT_BEREICH, bfnNamespace).equalsIgnoreCase("Eindeutiger Code")){
				uriNameSpace = elWissName.getAttributeValue(BfnXmlConstants.ATT_BEREICH);
				String textNormalize = elWissName.getTextNormalize();
				if(StringUtils.isBlank(textNormalize)){
					uniqueID = "";
				}else{
					uniqueID = textNormalize;
				}
			}
			if(elWissName.getAttributeValue(BfnXmlConstants.ATT_BEREICH, bfnNamespace).equalsIgnoreCase("Autoren")){
				strAuthor = elWissName.getTextNormalize();
			}
			if(elWissName.getAttributeValue(BfnXmlConstants.ATT_BEREICH, bfnNamespace).equalsIgnoreCase("Rang")){
				String strRank = elWissName.getTextNormalize();
				rank = makeRank(strRank);
			}
			if(elWissName.getAttributeValue(BfnXmlConstants.ATT_BEREICH, bfnNamespace).equalsIgnoreCase("Zusätze")){
				strSupplement = elWissName.getTextNormalize();
			}
			if(elWissName.getAttributeValue(BfnXmlConstants.ATT_BEREICH, bfnNamespace).equalsIgnoreCase("wissName")){
				try{
					TaxonName name = parseNonviralNames(rank,strAuthor,strSupplement,elWissName);
					if(name.isProtectedTitleCache() == true){
						logger.warn("Taxon " + name.getTitleCache());
					}

					//TODO  extract to method?
					if(strSupplement != null){
						name.setAppendedPhrase(strSupplement);
					}
					if(strSupplement != null && strSupplement.equalsIgnoreCase("nom. illeg.")){
						name.addStatus(NomenclaturalStatus.NewInstance(NomenclaturalStatusType.ILLEGITIMATE()));
					}

	                //no deduplication wanted by BfN
//                  deduplicationHelper.replaceAuthorNamesAndNomRef(state, name);

					Reference secRef = state.isFillSecondList() ?
					        state.getSecondListSecRef():
					        state.getFirstListSecRef();
					state.setCurrentSecundumRef(secRef);
					taxon = Taxon.NewInstance(name, state.getCurrentSecRef());
					//set create and set path of nameSpace

					String namespace = getNamespace(uriNameSpace, elWissName);
					taxon.addImportSource(uniqueID, namespace, state.getCompleteSourceRef(), null);
                    name.addImportSource(uniqueID, namespace, state.getCompleteSourceRef(), null);

					taxon.addIdentifier(taxonId, getIdentiferType(state, BfnXmlTransformer.UUID_TAX_NR_IDENTIFIER_TYPE, "taxNr", "TaxNr attribute of Bfn Xml file", "taxNr", null));
					taxon.addExtension(reihenfolge, ExtensionType.ORDER());
					taxon.addIdentifier(reihenfolge, getIdentiferType(state, BfnXmlTransformer.UUID_REIHENFOLGE_IDENTIFIER_TYPE, "reihenfolge", "reihenfolge attribute of Bfn Xml file", "reihenfolge", null));
				} catch (UnknownCdmTypeException e) {
					success.setValue(false);
				}
			}
		}
		return taxon;
	}


    /**
     * @param uriNameSpace
     * @param elWissName
     * @return
     */
    protected String getNamespace(String uriNameSpace, Element elWissName) {
        Element parentElement = elWissName.getParentElement();
        Element grandParentElement = parentElement.getParentElement();
        String namespace = grandParentElement.getName() + ":" + parentElement.getName() + ":"+elWissName.getName() + ":" + uriNameSpace;
        return namespace;
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
			childName = BfnXmlConstants.EL_WISSNAME;
			Element elSynScientificName = XmlHelp.getSingleChildElement(success, elSyn, childName, bfnNamespace, obligatory);

			childElementName = BfnXmlConstants.EL_NANTEIL;
			List<Element> elSynDetails = elSynScientificName.getChildren(childElementName, bfnNamespace);

			for(Element elSynDetail:elSynDetails){
				if(elSynDetail.getAttributeValue(BfnXmlConstants.ATT_BEREICH).equalsIgnoreCase("Rang")){
					String strRank = elSynDetail.getTextNormalize();
					rank = makeRank(strRank);
				}
				if(elSynDetail.getAttributeValue(BfnXmlConstants.ATT_BEREICH).equalsIgnoreCase("Autoren")){
					strAuthor = elSynDetail.getTextNormalize();
				}
				if(elSynDetail.getAttributeValue(BfnXmlConstants.ATT_BEREICH, bfnNamespace).equalsIgnoreCase("Zusätze")){
					strSupplement = elSynDetail.getTextNormalize();
				}
				if(elSynDetail.getAttributeValue(BfnXmlConstants.ATT_BEREICH).equalsIgnoreCase("wissName")){
					try{
						TaxonName name = parseNonviralNames(rank,strAuthor,strSupplement,elSynDetail);
						//BfN does not want any deduplication, to be on the save side that no
//				        deduplicationHelper.replaceAuthorNamesAndNomRef(state, name);

						//TODO find best matching Taxa
						Synonym synonym = Synonym.NewInstance(name, state.getCurrentSecRef());
						taxon.addSynonym(synonym, SynonymType.SYNONYM_OF());

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

		@SuppressWarnings("unchecked")
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
			Namespace bfnNamespace,
			Element elInformations,
			BfnXmlImportState state){

	    String childElementName = BfnXmlConstants.EL_BEZUGSRAUM;

		List<Element> elInformationList = elInformations.getChildren(childElementName, bfnNamespace);

		for(Element elInfo:elInformationList){
			//check if geographical scope is Bund and import only these information for now
			//TODO create several taxon descriptions for different geographical scope
			if(elInfo.getName().equalsIgnoreCase(BfnXmlConstants.EL_BEZUGSRAUM) && elInfo.getAttributeValue("name").equalsIgnoreCase("Bund")){
				childElementName = BfnXmlConstants.EL_IWERT;
				TaxonDescription taxonDescription = getTaxonDescription(taxon, false, true);
				try {
				    UUID  germanStateUUID = BfnXmlTransformer.getAreaUUID("Deutschland");
					NamedArea area = (NamedArea)getTermService().load(germanStateUUID);
					//FIXME GEOSCOPE_ID CANNOT BE NULL Exception
					if (area != null){
					    taxonDescription.addGeoScope(area);
					}else{
					    logger.warn("Did not find area 'Deutschland'");
					}
				} catch (UnknownCdmTypeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				List<Element> elInfoDetailList = elInfo.getChildren(childElementName, bfnNamespace);

				for(Element elInfoDetail : elInfoDetailList){
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_RL_KAT)){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
						createDistributionStatus(elInfoDetail, state, taxonDescription);
					}
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_KAT)){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_AKTUELLE_BESTANDSSTITUATION)){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_LANGFRISTIGER_BESTANDSTREND)){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_KURZFRISTIGER_BESTANDSTREND)){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_RISIKOFAKTOREN)){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_VERANTWORTLICHKEIT)){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_ALTE_RL_KAT)){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_NEOBIOTA)){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_EINDEUTIGER_CODE)){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.FEAT_KOMMENTAR_TAXONOMIE)){
						makeFeatures(taxonDescription, elInfoDetail, state, true);
					}
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.FEAT_KOMMENTAR_GEFAEHRDUNG)){
						makeFeatures(taxonDescription, elInfoDetail, state, true);
					}
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.VOC_SONDERFAELLE)){
						makeFeatures(taxonDescription, elInfoDetail, state, false);
					}
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.FEAT_LETZTER_NACHWEIS)){
						makeFeatures(taxonDescription, elInfoDetail, state, true);
					}
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase(BfnXmlConstants.FEAT_WEITERE_KOMMENTARE)){
						makeFeatures(taxonDescription, elInfoDetail, state, true);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("BW")){
                        createDistributionStatus(elInfoDetail, state, taxonDescription);
                    }
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("BY")){
					    createDistributionStatus(elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("BE")){
					    createDistributionStatus(elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("BB")){
					    createDistributionStatus(elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("HB")){
					    createDistributionStatus(elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("HH")){
					    createDistributionStatus(elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("HE")){
					    createDistributionStatus(elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("MV")){
					    createDistributionStatus(elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("NI")){
					    createDistributionStatus(elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("NW")){
					    createDistributionStatus(elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("RP")){
					    createDistributionStatus(elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("SL")){
					    createDistributionStatus(elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("SN")){
					    createDistributionStatus(elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("ST")){
					    createDistributionStatus(elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("SH")){
					    createDistributionStatus(elInfoDetail, state, taxonDescription);
					}
					//create german federal states distribution status
					if(elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME).equalsIgnoreCase("TH")){
					    createDistributionStatus(elInfoDetail, state, taxonDescription);
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
		String strRlKatValue = elInfoDetail.getChild(BfnXmlConstants.EL_WERT).getValue();
		String strRlKat = elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME);
		boolean randomStateUUID = false;
		try {
			featureUUID = BfnXmlTransformer.getRedlistFeatureUUID(strRlKat);
			transformedRlKatValue = BfnXmlTransformer.redListString2RedListCode(strRlKatValue);
		} catch (UnknownCdmTypeException e) {
			transformedRlKatValue = strRlKatValue;
		}
		if (featureUUID == null){
		    logger.warn("featureUUID is null for " + strRlKat);
		}
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

		if (featureUUID != null){
		    Feature redListFeature = getFeature(state, featureUUID);
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
		}else{
		    logger.warn("Not descriptive data imported");
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
 		if (StringUtils.isBlank(strRank)){
			return null;
		}
		Rank codeRank = BfnXmlTransformer.getRankForRankCode(strRank);
		if(codeRank==null){
		    codeRank = Rank.UNKNOWN_RANK();
		}
		//codeRank exists
		if ( (codeRank != null) && !codeRank.equals(Rank.UNKNOWN_RANK())){
			result = codeRank;
		}
		//codeRank does not exist
		else{
			result = codeRank;
			logger.warn("string rank ('"+strRank+"') used, because code rank does not exist or was not recognized: " + codeRank.getTitleCache()+" "+strRank);
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
	private TaxonName parseNonviralNames(Rank rank, String strAuthor, String strSupplement, Element elWissName)
			throws UnknownCdmTypeException {
		TaxonName taxonNameBase = null;

		String strScientificName = elWissName.getTextNormalize();
		strScientificName = normalizeScientificName(strScientificName, strSupplement);
		NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();
		TaxonName nonViralName = (TaxonName)parser.parseFullName(strScientificName, nomenclaturalCode, rank);
		if(nonViralName.hasProblem()){
			for(ParserProblem p: nonViralName.getParsingProblems()){
				logger.warn(++parsingProblemCounter + " " +nonViralName.getTitleCache() + " " + p.toString());
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
     * @param strSupplement
     * @param strScientificName
     * @return
     */
    protected String normalizeScientificName(String strScientificName, String strSupplement) {
        // trim strScienctificName because sometimes getTextNormalize() does not removes all the whitespaces
		strScientificName = StringUtils.trim(strScientificName);
		strScientificName = StringUtils.remove(strScientificName, "\u00a0");
		strScientificName = StringUtils.remove(strScientificName, "\uc281");

		if(StringUtils.isNotBlank(strSupplement)){
			strScientificName = StringUtils.remove(strScientificName, strSupplement);
		}
		//Eulenspinner/spanner have different taxon name syntax like "pupillata (Thunberg, 1788); Epirrhoe"
		if(strScientificName.contains(";")){
		    String[] splits = strScientificName.split(";");
		    if (splits.length != 2){
		        logger.warn("Unexpected length of semicolon scientific name: " + splits.length + "; " + strScientificName);
		    }
		    strScientificName = splits[1].trim() + " " + splits[0].trim();
		}

        return strScientificName;
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
    private void createDistributionStatus(Element elInfoDetail, BfnXmlImportState state,
            TaxonDescription taxonDescription){

        String strDistributionValue = elInfoDetail.getChild(BfnXmlConstants.EL_WERT).getValue();
        if (strDistributionValue.startsWith("#dtpl_RLKat")){
            try {
                strDistributionValue = BfnXmlTransformer.redListString2RedListCode(strDistributionValue);
            } catch (UnknownCdmTypeException e) {
                logger.warn("RL Kategorie " + strDistributionValue + " konnte nicht gematched werden ");
            }
        }
        String strArea = elInfoDetail.getAttributeValue(BfnXmlConstants.ATT_STANDARDNAME);
        if (strArea.equals("RL Kat.")){
            strArea = "Deutschland";
        }
        //match DistributionValue
        UUID matchedDistributionUUID;
        try {
            matchedDistributionUUID = BfnXmlTransformer.matchDistributionValue(strDistributionValue);
        } catch (UnknownCdmTypeException e1) {
            logger.warn("could not match xml value "+ strDistributionValue +" to distribution status for "+strArea);
//            e1.printStackTrace();
            return;
        }
        if (matchedDistributionUUID == null){
            return;
        }
        PresenceAbsenceTerm status;
        DefinedTermBase<?> load = getTermService().load(matchedDistributionUUID);
        if(load.isInstanceOf(PresenceAbsenceTerm.class)) {
            status = CdmBase.deproxy(load, PresenceAbsenceTerm.class);
        }else{
            logger.warn(strDistributionValue + " is not PresenceAbsence Term " + load.getTitleCache() + " " + load.getTermType().toString());
            return;
        }
        //load vocabulary and german state
        UUID stateUUID = null;

        try {
            stateUUID = BfnXmlTransformer.getAreaUUID(strArea);
        } catch (UnknownCdmTypeException e1) {
            logger.warn("could not match state" + strArea + " to UUID");
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
        //add to taxon description
        DescriptionElementBase descriptionElement = Distribution.NewInstance(area, status);
        taxonDescription.addElement(descriptionElement);
    }


    @Override
    public boolean doCheck(BfnXmlImportState state){
        boolean result = true;
        return result;
    }

	@Override
	protected boolean isIgnore(BfnXmlImportState state){
		return ! state.getConfig().isDoTaxonNames();
	}
}
