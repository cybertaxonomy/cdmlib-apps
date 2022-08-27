/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.uzbekistan;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.NameTypeDesignationStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceType;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * Uzbekistan taxon import.
 *
 * @author a.mueller
 * @since 05.05.2020
 */
@Component
public class UzbekistanTaxonImport<CONFIG extends UzbekistanTaxonImportConfigurator>
            extends SimpleExcelTaxonImport<CONFIG>{

    private static final long serialVersionUID = 7793140600785382094L;
    private static final Logger logger = LogManager.getLogger();

    private static final String COMMON_NAME = "common name";
    private static final String PARENT_TAXON_UUID = "parentTaxonUuid";
    private static final String ACCEPTED_TAXON_UUID = "acceptedTaxonUuid";
    private static final String NAME_TYPE_UUID = "nameTypeUuid";
    private static final String REPLACED_SYNONYM_UUID = "replacedSynonymUuid";
    private static final String BASIONYM_UUID = "basionymUuid";
    private static final String TAXON_UUID = "taxonUuid";
    private static final String NAME_STATUS = "nameStatus";
    private static final String STATUS = "status";
    private static final String RANK = "rank";
    private static final String FULL_NAME = "fullName";
    private static final String FULL_TITLE = "fullTitle";
    private static final String REFERENCE_TYPE = "referenceType";

	private static UUID rootUuid = UUID.fromString("3ee689d2-17c2-4f02-9905-7092a45aa1b9");

    private  static List<String> expectedKeys= Arrays.asList(new String[]{
            TAXON_UUID,FULL_TITLE,FULL_NAME,"pureName",
            RANK,STATUS,REFERENCE_TYPE,PARENT_TAXON_UUID,"parenTaxonName",
            ACCEPTED_TAXON_UUID,"acceptedTaxonName","homotypicGroupUuid",BASIONYM_UUID,
            REPLACED_SYNONYM_UUID,NAME_STATUS,NAME_TYPE_UUID,COMMON_NAME
    });

    private Reference sourceReference;
    private Reference secReference;

    private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();

//    @Override
//    protected String getWorksheetName(CONFIG config) {
//        return "valid taxa names";
//    }

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {

        String line = getLine(state, 50);
        System.out.println(line);
        Map<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();
        for (String key: keys) {
            if (! expectedKeys.contains(key)){
                logger.warn(line + "Unexpected Key: " + key);
            }
        }

        makeTaxon(state, line, record);
    }

    private void makeTaxon(SimpleExcelTaxonImportState<CONFIG> state, String line, Map<String, String> record) {
        state.getTransactionStatus().flush();

        Reference sec = getSecReference(state);
        state.getTransactionStatus().flush();

        String fullTitle = getValue(record, FULL_TITLE);
        String fullName = getValue(record, FULL_NAME);
        Rank rank = makeRank(line, record);

        //name
        TaxonName taxonName = parser.parseReferencedName(fullTitle, NomenclaturalCode.ICNAFP, rank);

        //TODO validation, deduplication, source, ...

        //name status
        makeNameStatus(line, record, taxonName);

        taxonName = TaxonName.castAndDeproxy(taxonName);
       //TODO
        checkParsed(TaxonName.castAndDeproxy(taxonName), fullTitle, fullName, line);
        checkReference(record, line, taxonName, fullTitle);
        replaceNameAuthorsAndReferences(state, taxonName);
        taxonName.addSource(makeOriginalSource(state));

        //taxon
        TaxonBase<?> taxonBase = makeTaxonBase(state, line, record, taxonName, sec);
        //common name
        makeCommonName(line, record, taxonBase);

        getNameService().saveOrUpdate(taxonName);
        if (taxonBase != null){
            getTaxonService().saveOrUpdate(taxonBase);
        }
        state.getTransactionStatus().flush();

        return;
    }

    private void checkReference(Map<String, String> record, String line,
            TaxonName taxonName, String fullTitle) {
        String refTypeStr = getValue(record, REFERENCE_TYPE);
        String statusStr = getValue(record, STATUS);
        Reference ref = taxonName.getNomenclaturalReference();
        if(ref == null){
            if (isNotBlank(refTypeStr)){
                logger.warn(line + "RefType given but no nom. ref. parsed: " + fullTitle);
            }
            if(isNotBlank(statusStr)){
                logger.warn(line+"Taxon status exist but name has no nom. ref.: " + fullTitle);
            }
        }else{
            if ("A".equals(refTypeStr)){
                if (!ref.getType().equals(ReferenceType.Article)){
                    logger.warn(line+"RefType should be article but was not: " + ref.getType().getLabel() + ";" + fullTitle);
                }
            }else if ("B".equals(refTypeStr)){
                if (!ref.getType().equals(ReferenceType.Book)){
                    logger.warn(line+"RefType should be book but was not: " + ref.getType().getLabel() + ";" + fullTitle);
                }
            }else if ("BS".equals(refTypeStr)){
                if (!ref.getType().equals(ReferenceType.BookSection)){
                    logger.warn(line+"RefType should be book section but was not: " + ref.getType().getLabel() + ";" + fullTitle) ;
                }
            }else{
                logger.warn(line+"Name has nom. ref. but ref type could not be recognized/was empty: " + refTypeStr + ";" + fullTitle);
            }
        }
    }

    private void makeCommonName(String line,
            Map<String, String> record,
            TaxonBase<?> taxonBase) {
        String commonNameStr = getValue(record, COMMON_NAME);
        if(isBlank(commonNameStr)){
            return;
        }else if(taxonBase == null){
            logger.warn(line + "No taxon exists for common name: " + commonNameStr);
        }else if(! (taxonBase instanceof Taxon)){
            logger.warn(line + "Taxon is not accepted for common name: " + commonNameStr);
        }else{
            Taxon taxon = (Taxon)taxonBase;
            CommonTaxonName commonName = CommonTaxonName.NewInstance(commonNameStr, Language.RUSSIAN());
            getTaxonDescription(taxon, false, true).addElement(commonName);
        }
    }

    private void makeNameStatus(String line, Map<String, String> record,
            TaxonName taxonName) {
        String nameStatus = getValue(record, NAME_STATUS);
        NomenclaturalStatusType status;
        if (isBlank(nameStatus)){
            status = null;
        }else if ("nom. cons.".equals(nameStatus)){
            status = NomenclaturalStatusType.CONSERVED();
        }else if ("nom. inval.".equals(nameStatus)){
            status = NomenclaturalStatusType.INVALID();
        }else if ("nom. illeg.".equals(nameStatus)){
            status = NomenclaturalStatusType.ILLEGITIMATE();
        }else if ("nom. rej.".equals(nameStatus)){
            status = NomenclaturalStatusType.REJECTED();
        }else{
            logger.warn(line + "Nom. status not recognized: " + nameStatus);
            status = null;
        }
        if (status != null){
            taxonName.addStatus(NomenclaturalStatus.NewInstance(status));
        }
    }


    private TaxonBase<?> makeTaxonBase(SimpleExcelTaxonImportState<CONFIG> state, String line,
            Map<String, String> record, TaxonName taxonName, Reference sec) {
        TaxonBase<?> taxonBase;
        String statusStr = getValue(record, STATUS);
        String taxonUuidStr = getValue(record, TAXON_UUID);
        UUID taxonUuid = UUID.fromString(taxonUuidStr);
        if ("A".equals(statusStr)){
            taxonBase = Taxon.NewInstance(taxonName, sec);
        }else if ("S".equals(statusStr)){
            taxonBase = Synonym.NewInstance(taxonName, sec);
        }else if (statusStr == null){
            taxonName.setUuid(taxonUuid);
            return null;
        }else{
            logger.warn(line + "Status not handled: " + statusStr);
            return null;
        }
        taxonBase.setUuid(taxonUuid);
        taxonBase.addSource(makeOriginalSource(state));
        return taxonBase;
    }

    private Rank makeRank(String line, Map<String, String> record) {
        String rankStr = getValue(record, RANK);
        if (rankStr.equals("family")){
            return Rank.FAMILY();
        }else if(rankStr.equals("genus")){
            return Rank.GENUS();
        }else if(rankStr.equals("species")){
            return Rank.SPECIES();
        }else if(rankStr.equals("section")){
            return Rank.SECTION_BOTANY();
        }else if(rankStr.equals("subsection")){
            return Rank.SUBSECTION_BOTANY();
        }else if(rankStr.equals("series")){
            return Rank.SERIES();
        }else if(rankStr.equals("subgenus")){
            return Rank.SUBGENUS();
        }else if(rankStr.equals("subspecies")){
            return Rank.SUBSPECIES();
        }else if(rankStr.equals("variety")){
            return Rank.VARIETY();
        }else if(rankStr.equals("unranked infrageneric")){
            return Rank.UNRANKED_INFRAGENERIC();
        }else{
            logger.warn(line + "Unknown rank: " + rankStr);
        }
        return null;
    }

    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CONFIG> state) {
        String line = state.getCurrentLine() + ": ";
        System.out.println(line);
        Map<String, String> record = state.getOriginalRecord();

        String statusStr = getValue(record, STATUS);
        String rankStr = getValue(record, RANK);

        UUID taxonUuid = getUuid(record, TAXON_UUID, true, line);
        TaxonBase<?> taxonBase = getTaxonService().find(taxonUuid);
        TaxonName taxonName;
        if (taxonBase == null){
            taxonName = getNameService().find(taxonUuid);
        }else{
            taxonName = taxonBase.getName();
        }

        UUID parentUuid = getUuid(record, PARENT_TAXON_UUID, false, line);
        if (parentUuid != null){
            TaxonBase<?> parentBase = getTaxonService().find(parentUuid);
            if(!parentBase.isInstanceOf(Taxon.class)){
                logger.warn(line + "Parent taxon is not accepted: " + parentUuid);
            }else if (taxonBase == null || (!taxonBase.isInstanceOf(Taxon.class))){
                logger.warn(line + "Taxon has parent uuid but has not status accepted");
            }else{
                Taxon child = CdmBase.deproxy(taxonBase, Taxon.class);
                Taxon parentTaxon = CdmBase.deproxy(parentBase, Taxon.class);
                Classification classification = getClassification(state).getClassification();
                TaxonNode node = classification.addParentChild(parentTaxon, child, null, null);
                getTaxonNodeService().saveOrUpdate(node);
            }
        }else if("A".equals(statusStr) && !"family".equals(rankStr)){
            logger.warn(line + "No parent given for accepted non-family taxon");
        }

        UUID acceptedUuid = getUuid(record, ACCEPTED_TAXON_UUID, false, line);
        if (acceptedUuid != null){
            TaxonBase<?> acceptedBase = getTaxonService().find(acceptedUuid);
            if(acceptedBase == null){
                logger.warn(line + "Taxon for existing uuid could not be found. This should not happen");
                return;
            }else if(!acceptedBase.isInstanceOf(Taxon.class)){
                logger.warn(line + "Accepted taxon is not accepted: " + acceptedUuid);
            }else if (taxonBase == null || (!taxonBase.isInstanceOf(Synonym.class))){
                logger.warn(line + "Synonym has accepted uuid but has not status accepted");
            }else{
                Synonym syn = CdmBase.deproxy(taxonBase, Synonym.class);
                Taxon acc = CdmBase.deproxy(acceptedBase, Taxon.class);
                //TODO synType
                acc.addSynonym(syn, SynonymType.HETEROTYPIC_SYNONYM_OF());
            }
        }else if("S".equals(statusStr)){
            logger.warn(line + "No accepted taxon given for synonym");
        }

        UUID basionymUuid = getUuid(record, BASIONYM_UUID, false, line);
        if (basionymUuid != null){
            TaxonBase<?> basionymTaxon = getTaxonService().find(basionymUuid);
            if(basionymTaxon == null){
                logger.warn(line + "Basionym does not exist as taxon but only as name: " + basionymUuid);
            }else{
                TaxonName basionymName = basionymTaxon.getName();
                taxonName.addBasionym(basionymName);
                taxonName.mergeHomotypicGroups(basionymName);  //just in case this is not automatically done
                adjustSynonymType(taxonBase, basionymTaxon, line);
            }
        }

        UUID replacedSynonymUuid = getUuid(record, REPLACED_SYNONYM_UUID, false, line);
        if (replacedSynonymUuid != null){
            TaxonBase<?> replacedTaxon = getTaxonService().find(replacedSynonymUuid);
            if(replacedTaxon == null){
                logger.warn(line + "Replaced synonym does not exist as taxon but only as name: " + replacedSynonymUuid);
            }else{
                TaxonName replacedName = replacedTaxon.getName();
                taxonName.addBasionym(replacedName);
                taxonName.mergeHomotypicGroups(replacedName);  //just in case this is not automatically done
                adjustSynonymType(taxonBase, replacedTaxon, line);
            }
        }

        UUID nameTypeUuid = getUuid(record, NAME_TYPE_UUID, false, line);
        if (nameTypeUuid != null){
            TaxonBase<?> typeTaxon = getTaxonService().find(nameTypeUuid);
            TaxonName typeName;
            if (typeTaxon == null){
                typeName = getNameService().find(nameTypeUuid);
            }else{
                typeName = typeTaxon.getName();
            }
            //TODO
            NameTypeDesignationStatus status = null; //NameTypeDesignationStatus.
            taxonName.addNameTypeDesignation(typeName, null, null, null, status, false);
        }
    }

    private void adjustSynonymType(TaxonBase<?> taxonBase, TaxonBase<?> homotypicTaxon, String line) {
        adjustSynonymTypeOrdered(taxonBase, homotypicTaxon, line);
        adjustSynonymTypeOrdered(homotypicTaxon, taxonBase, line);
    }

    private void adjustSynonymTypeOrdered(TaxonBase<?> firstTaxon, TaxonBase<?> secondTaxon, String line) {
        if (firstTaxon == null){
            logger.warn(line + "first taxon is null for adjust synonym type");
        }else if (secondTaxon == null){
            logger.warn(line + "second taxon is null for adjust synonym type");
        }else if (secondTaxon.isInstanceOf(Synonym.class)){
            Synonym syn = CdmBase.deproxy(secondTaxon, Synonym.class);
            if (firstTaxon.equals(syn.getAcceptedTaxon())){
                syn.setType(SynonymType.HOMOTYPIC_SYNONYM_OF());
            }
        }
    }

    protected UUID getUuid(Map<String, String> record, String columnName, boolean required, String line) {
        String uuidStr = getValue(record, columnName);
        if (isNotBlank(uuidStr)){
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
                return uuid;
            } catch (Exception e) {
                logger.warn(line + "UUID could not be parsed: " + uuidStr);
                return null;
            }
        }else{
            if (required){
                logger.warn(line + "UUID required but does not exist");
            }
            return null;
        }
    }

	private void checkParsed(TaxonName name, String fullName, String nameStr, String line) {
		if (name.isProtectedTitleCache() || name.isProtectedFullTitleCache() || name.isProtectedNameCache()) {
			logger.warn(line + "Name could not be parsed: " + fullName);
		}
		if (name.getNomenclaturalReference() != null && name.getNomenclaturalReference().isProtectedTitleCache()){
		    logger.warn(line + "Nom ref could not be parsed: " + fullName);
		}
		if (!name.getTitleCache().equals(nameStr)){
            logger.warn(line + "Name part not parsed correctly: " + name.getTitleCache() + "<-> expected: " + nameStr);
        }
	}

    private TaxonNode rootNode;

    private TaxonNode getClassification(SimpleExcelTaxonImportState<CONFIG> state) {
        if (rootNode == null){
            logger.warn("Load root node");
            rootNode = getTaxonNodeService().find(UUID.fromString("3ee689d2-17c2-4f02-9905-7092a45aa1b9"));
        }
        if (rootNode == null){
            logger.warn("Create root node");
            Reference sec = getSecReference(state);
            String classificationName = state.getConfig().getClassificationName();
            Language language = Language.DEFAULT();
            Classification classification = Classification.NewInstance(classificationName, sec, language);
            classification.setUuid(state.getConfig().getClassificationUuid());
            classification.getRootNode().setUuid(rootUuid);
            getClassificationService().save(classification);

            rootNode = classification.getRootNode();
        }
        return rootNode;
    }

    private Reference getSecReference(SimpleExcelTaxonImportState<CONFIG> state) {
        if (this.secReference == null){
            logger.warn("Load sec ref");
            this.secReference = getPersistentReference(state.getConfig().getSecReference());
            if (this.secReference == null){
                logger.warn("Sec ref is null");
            }
        }
        return this.secReference;
    }

    private Reference getSourceCitation(SimpleExcelTaxonImportState<CONFIG> state) {
        if (this.sourceReference == null){
            this.sourceReference = getPersistentReference(state.getConfig().getSourceReference());
            this.sourceReference.setInReference(getSecReference(state));  //special for Uzbekistan
        }
        return this.sourceReference;
    }

    private Reference getPersistentReference(Reference reference) {
        Reference result = getReferenceService().find(reference.getUuid());
        logger.warn("Loaded persistent reference: "+ reference.getUuid());
        if (result == null){
            logger.warn("Persistent reference is null: " + reference.getUuid());
            result = reference;
            getReferenceService().saveOrUpdate(result);
        }
        return result;
    }

    private void replaceNameAuthorsAndReferences(SimpleExcelTaxonImportState<CONFIG> state, INonViralName name) {
        state.getDeduplicationHelper().replaceAuthorNamesAndNomRef(name);
    }


    @Override
    protected IdentifiableSource makeOriginalSource(SimpleExcelTaxonImportState<CONFIG> state) {
    	String noStr = getValue(state.getOriginalRecord(), "taxonUuid");
        return IdentifiableSource.NewDataImportInstance(noStr, "taxonUuid", getSourceCitation(state));
    }

}
