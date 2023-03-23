/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.germanSL;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.Country;
import eu.etaxonomy.cdm.model.name.IBotanicalName;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.RankClass;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.IdentifierType;
import eu.etaxonomy.cdm.model.term.OrderedTermVocabulary;

/**
 * @author a.mueller
 * @since 25.11.2016
 */
@Component
public class GermanSLTaxonImport
            extends GermanSLImporBase {

    private static final long serialVersionUID = 236093186271666895L;
    private static final Logger logger = LogManager.getLogger();

    static final String SPECIES_NR = "SPECIES_NR";
    private static final String AUTHOR = "AUTHOR";
    private static final String ABBREVIAT = "ABBREVIAT";
    private static final String SEC = "SECUNDUM";
    private static final String RANG = "RANG";
    private static final String EXTERNAL_ID = "external_ID";
    private static final String GRUPPE = "GRUPPE";
    static final String VALID_NR = "VALID_NR";
    static final String SYNONYM = "SYNONYM";
    private static final String NATIVENAME = "NATIVENAME";
    private static final String LETTER_CODE = "LETTERCODE";
    static final String AGG = "AGG";

    private static final String AGG_NAME = "AGG_NAME";
    private static final String VALID_NAME = "VALID_NAME";

    private static final String NACHWEIS = "NACHWEIS";
    private static final String HYBRID = "HYBRID";
    private static final String BEGRUEND = "BEGRUEND";
    private static final String EDITSTATUS = "EDITSTATUS";

    private static final String UUID_ = "UUID";

    public static final String TAXON_NAMESPACE = "1.3.4";

    @Override
    protected String getWorksheetName(GermanSLImportConfigurator config) {
        return "1.3.4";
    }

    //dirty I know, but who cares, needed by distribution and common name import
    protected static final Map<String, TaxonBase<?>> taxonIdMap = new HashMap<>();


    private  static List<String> expectedKeys= Arrays.asList(new String[]{
            SPECIES_NR,EXTERNAL_ID,ABBREVIAT,
            AUTHOR,SEC,SYNONYM,
            LETTER_CODE, AGG,
            NATIVENAME,VALID_NR,RANG,GRUPPE,
            UUID_,
            NACHWEIS, HYBRID, BEGRUEND, EDITSTATUS, AGG_NAME, VALID_NAME
        });


    @Override
    protected void firstPass(SimpleExcelTaxonImportState<GermanSLImportConfigurator> state) {
        String line = state.getCurrentLine() + ": ";
        Map<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();

        checkAllKeysExist(line, keys, expectedKeys);

        //Name
        NameResult nameResult = makeName(line, record, state);
        IBotanicalName taxonName = nameResult.name;

        //sec
        String secRefStr = getValue(record, SEC);
        Reference sec = getSecRef(state, secRefStr, line);

        //status
        String statusStr = getValue(record, SYNONYM);
        TaxonBase<?> taxonBase;
        if (isAccepted(statusStr, nameResult)){
            taxonBase = Taxon.NewInstance(taxonName, sec);
//            if (nameResult.proParte){
//                logger.warn(line + "accepted taxon can not be pro parte in GermanSL");
//            }
        }else{
            Synonym syn = Synonym.NewInstance(taxonName, sec);
//            if (nameResult.proParte){
//                syn.setProParte(true);
//            }
            taxonBase = syn;
        }
        if (!isBlank(nameResult.sensu)){
            taxonBase.setAppendedPhrase(nameResult.sensu);
        }
        //TODO right order?
        taxonBase.setAppendedPhrase(CdmUtils.concat(" ", nameResult.auct, taxonBase.getAppendedPhrase()));

        //lettercode
        String lettercode = getValue(record, LETTER_CODE);
        if (isNotBlank(lettercode)){
            UUID idTypeUUID;
            try {
                idTypeUUID = state.getTransformer().getIdentifierTypeUuid("LETTERCODE");
                IdentifierType idType = getIdentiferType(state, idTypeUUID, "GermanSL lettercode", "GermanSL lettercode", "LETTERCODE", null);
                taxonBase.addIdentifier(lettercode, idType);
            } catch (UndefinedTransformerMethodException e) {
               e.printStackTrace();
            }
        }

//        //annotation
//        String annotation = getValue(record, "Anotacion al Taxon");
//        if (annotation != null && (!annotation.equals("nom. illeg.") || !annotation.equals("nom. cons."))){
//            taxonBase.addAnnotation(Annotation.NewInstance(annotation, AnnotationType.EDITORIAL(), Language.SPANISH_CASTILIAN()));
//        }

        //UUID
        String uuid = getValue(record, UUID_);
        //TOOD why sometimes null?
        if (uuid != null){
            taxonBase.setUuid(UUID.fromString(uuid));
        }


        //NATIVE NAME
        String commonNameStr = getValue(record, NATIVENAME);
        //Ann.: synonym common names should be removed!
        if (isNotBlank(commonNameStr)){
            makeCommonName(commonNameStr, taxonBase, line);
        }


        //id
        String id = getValue(record, SPECIES_NR);
        this.addOriginalSource(taxonBase, id, TAXON_NAMESPACE, getSourceReference(state));

        //save
        getTaxonService().saveOrUpdate(taxonBase);
        saveNameRelations(taxonBase.getName());
        taxonIdMap.put(id, taxonBase);
    }



    private String removeProparte(String authorStr) {
        String regEx = "\\s+p\\.\\s*p\\.$";
        if (authorStr == null || !authorStr.matches(".*" + regEx)){
            return authorStr;
        }else{
            return authorStr.replaceAll(regEx, "");
        }
    }

    private String removeSensuLatoStricto(String authorStr) {
        String regEx = "\\s+s\\.\\s*(l|str)\\.$";

        if (authorStr == null || !authorStr.matches(".*" + regEx)){
            return authorStr;
        }else{
            return authorStr.replaceAll(regEx, "");
        }
    }

    private String removeAuct(String authorStr) {
        String regEx = "auct\\.\\??$";

        if (authorStr == null || !authorStr.matches(/*".*" + */regEx)){
            return authorStr;
        }else{
            return ""; //authorStr.replaceAll(regEx, "");
        }
    }


    /**
     * @param state
     * @param secRefStr
     * @return
     */
    private Reference getSecRef(SimpleExcelTaxonImportState<GermanSLImportConfigurator> state, String secRefStr, String line) {
        Reference result = state.getReference(secRefStr);
        if (result == null && secRefStr != null){
            result = ReferenceFactory.newGeneric();
            result.setTitleCache(secRefStr, true);
            state.putReference(secRefStr, result);
        }

        return result;
    }



    /**
     * @param record
     * @param state
     * @return
     */
    public NameResult makeName(String line, Map<String, String> record, SimpleExcelTaxonImportState<GermanSLImportConfigurator> state) {

        String specieNrStr = getValue(record, SPECIES_NR);
        String nameStr = getValue(record, ABBREVIAT);
        String authorStr = getValue(record, AUTHOR);
        String rankStr = getValue(record, RANG);

        NameResult result = new NameResult();

        //rank
        Rank rank = makeRank(line, state, rankStr);

        //name
        nameStr = normalizeNameStr(nameStr);
        String nameStrWithoutSensu = removeSensuLatoStricto(nameStr);
        if (nameStrWithoutSensu.length() < nameStr.length()){
            result.sensu = nameStr.substring(nameStrWithoutSensu.length()).trim();
            nameStr = nameStrWithoutSensu;
        }

        //author
        //pp
        authorStr = normalizeAuthorStr(authorStr);
        String authorStrWithoutProParte = removeProparte(authorStr);
        result.proParte = authorStrWithoutProParte.length() < authorStr.length();
        authorStr = authorStrWithoutProParte;

        //auct.
        String authorStrWithoutAuct = removeAuct(authorStr);
        if (authorStrWithoutAuct.length() < authorStr.length()){
            result.auct = authorStr.substring(authorStrWithoutAuct.length()).trim();
        }
        authorStr = authorStrWithoutAuct;


        //name+author
        String fullNameStr = CdmUtils.concat(" ", nameStr, authorStr);

        IBotanicalName fullName = (IBotanicalName)nameParser.parseReferencedName(fullNameStr, NomenclaturalCode.ICNAFP, rank);
        if (fullName.isProtectedTitleCache()){
            logger.warn(line + "Name could not be parsed: " + fullNameStr );
        }else{
            state.getDeduplicationHelper().replaceAuthorNamesAndNomRef(fullName);
//            replaceAuthorNamesAndNomRef(state, fullName);
        }
//        BotanicalName existingName = getExistingName(state, fullName);

        //TODO handle existing name
        IBotanicalName name = fullName;
        this.addOriginalSource(name, specieNrStr, TAXON_NAMESPACE + "_Name", getSourceReference(state));

        result.name = name;
        return result;
    }

    private Rank makeRank(String line, SimpleExcelTaxonImportState<GermanSLImportConfigurator> state, String rankStr) {
        Rank rank = null;
        try {
            rank = state.getTransformer().getRankByKey(rankStr);
            if (rank == null){
                UUID rankUuid = state.getTransformer().getRankUuid(rankStr);
                OrderedTermVocabulary<Rank> voc = (OrderedTermVocabulary<Rank>)Rank.SPECIES().getVocabulary();
                //TODO
                Rank lowerRank = Rank.FORM();
                rank = getRank(state, rankUuid, rankStr, rankStr, rankStr, voc, lowerRank, RankClass.Infraspecific);
                if (rank == null){
                    logger.warn(line + "Rank not recognized: " + rankStr);
                }
            }
        } catch (Exception e1) {
                logger.warn(line + "Exception when trying to define rank '" + rankStr + "': " + e1.getMessage());
                e1.printStackTrace();
        }
        return rank;
    }


    /**
     * @param authorStr
     * @return
     */
    private String normalizeAuthorStr(String authorStr) {
        if (isBlank(authorStr)){
            return "";
        }else{
            if (authorStr.equals("-") || authorStr.equals("#")){
                authorStr = "";
            }
            return authorStr;
        }
    }

    private String normalizeNameStr(String nameStr) {
        nameStr = nameStr
                .replace(" agg.", " aggr.")
                .replace(" fo. ", " f. ")
             ;
        return nameStr;
    }


    boolean nameMapIsInitialized = false;
    /**
     * @param state
     * @param fullName
     * @return
     */
    private IBotanicalName getExistingName(SimpleExcelTaxonImportState<GermanSLImportConfigurator> state, IBotanicalName fullName) {
        initExistinNames(state);
        return (IBotanicalName)state.getName(fullName.getTitleCache());
    }

    /**
     * @param state
     */
    @SuppressWarnings("rawtypes")
    private void initExistinNames(SimpleExcelTaxonImportState<GermanSLImportConfigurator> state) {
        if (!nameMapIsInitialized){
            List<String> propertyPaths = Arrays.asList("");
            List<TaxonName> existingNames = this.getNameService().list(null, null, null, null, propertyPaths);
            for (TaxonName tnb : existingNames){
                state.putName(tnb.getTitleCache(), tnb);
            }
            nameMapIsInitialized = true;
        }
    }


    /**
     * @param commmonNameStr
     * @param taxonBase
     */
    private void makeCommonName(String commmonNameStr, TaxonBase<?> taxonBase, String line) {
        if (taxonBase.isInstanceOf(Synonym.class)){
            //synonym common names should be neglected
            return;
        }
        Taxon acceptedTaxon = getAccepted(taxonBase);
        if (acceptedTaxon != null){
            TaxonDescription desc = getTaxonDescription(acceptedTaxon, false, true);
            desc.setDefault(true);
            CommonTaxonName commonName = CommonTaxonName.NewInstance(commmonNameStr, Language.GERMAN(), Country.GERMANY());
            desc.addElement(commonName);
        }else{
            logger.warn(line + "No accepted taxon available");
        }

    }


    /**
     * @param next
     * @return
     */
    private Taxon getAccepted(TaxonBase<?> taxonBase) {
        if (taxonBase.isInstanceOf(Taxon.class)){
            return CdmBase.deproxy(taxonBase, Taxon.class);
        }else{
            Synonym syn = CdmBase.deproxy(taxonBase, Synonym.class);
            return syn.getAcceptedTaxon();
        }
    }


    @Override
    protected boolean isIgnore(SimpleExcelTaxonImportState<GermanSLImportConfigurator> state) {
        return ! state.getConfig().isDoTaxa();
    }
}
