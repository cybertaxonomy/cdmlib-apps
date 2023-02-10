/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.caryo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.common.DoubleResult;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.reference.ReferenceType;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 02.02.2023
 */
@Component
public class CaryoSileneaeNomRefImport extends CaryoSileneaeImportBase {

    private static final long serialVersionUID = 7227226331297614469L;
    private static final Logger logger = LogManager.getLogger();

    private static final String NOMEN_ID = "nomen_ID";
    private static final String NAME = "name";
    private static final String PUBLICATION = "Publication";
    private static final String PUB_TYPE_ED = "PubTypeEd";
    private static final String PUB_TYPE_KEW = "PubTypeKew";
    private static final String PUB_KEW = "PubKew";
    private static final String NIMM_KEW = "NimmKew";
    private static final String ORIG_SPELLING = "Original spelling";
    private static final String NOM_STATUS = "Nom. Status";

    @SuppressWarnings("unused")
    private static final String SECOND_PUBLICATION = "SecondPublication";
    @SuppressWarnings("unused")
    private static final String IMPORT = "import";
    @SuppressWarnings("unused")
    private static final String DUPL = "dupl";

    private static final NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();

    private SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state;

    @Override
    protected String getWorksheetName(CaryoSileneaeImportConfigurator config) {
        return "NomRef";
    }

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {
        int line = state.getCurrentLine();
//        if ((line % 500) == 0){
//            newTransaction(state);
//            System.out.println(line);
//        }

        this.state = state;
        Map<String, String> record = state.getOriginalRecord();

        Integer nomenId = Integer.valueOf(getValue(record, NOMEN_ID));
        String nameStr = getValue(record, NAME);
        String origPublication = getValue(record, PUBLICATION);
        String pubTypeEd = getValue(record, PUB_TYPE_ED);
        String pubTypeKew = getValue(record, PUB_TYPE_KEW);
        String pubKew = getValue(record, PUB_KEW);

        String nimmKew = getValue(record, NIMM_KEW);
        String origSpelling = getValue(record, ORIG_SPELLING);

        @SuppressWarnings("unused")
        String nomStatus = getValue(record, NOM_STATUS);

        String row = String.valueOf(line) + "("+nomenId+"): ";

        origNameMap.remove(nomenId);
        TaxonName name = getName(nomenId);
        if (name == null) {
            return;   //record did not exist
            //TODO minor check if it is really a duplicate
        }

        boolean isKew = isNotBlank(nimmKew) && "x".equals(nimmKew);

        String publication = isKew ? pubKew : origPublication;
        String pubType = isKew ? pubTypeKew : pubTypeEd;

        DoubleResult<String, String> origPubl = origPublicationMap.get(nomenId);
        boolean useOrigPubl = false;
        if (isBlank(publication) && origPubl != null) {
            publication = origPubl.getFirstResult();
            useOrigPubl = true;
            logger.warn(row + "use original (Nomen.xlsx) publication and/or year");
        }

        if ("ined.".equals(publication)) {
            publication = null;
            NomenclaturalStatusType type = NomenclaturalStatusType.INED();
            if (name.hasStatus(type)) {
                name.addStatus(type, null, null);
            }
        }

        ReferenceType refType = getRefType(pubType);
        if (refType == null && isNotBlank(publication)) {
            logger.warn(row + "reference type not found for: " + publication);
        }else if (publication == null) {
            if (!name.isAutonym()) {
                logger.warn(row + "no publication");
            }
        }else if (refType == ReferenceType.Article) {
            if (!publication.startsWith("in ")) {
                publication = " in " + publication;
            }else {
                publication = " " + publication;
            }
        }else if (refType == ReferenceType.Book) {
            if (publication.startsWith("in ")) {
                publication = " " + publication;
            }else if (publication.contains(",")) {
//                logger.warn(row + "book with ',': " + publication);
                String[] split = publication.split(",");
                String potentialAuthor = split[0];
                if (potentialAuthor.split(" ").length <= 2) {
                    boolean noAbbrev = true;
                    for(String str : potentialAuthor.split(" ")) {
                        if (str.endsWith(".")) {
                            noAbbrev = false;
                            break;
                        }
                    }
                    if (noAbbrev) {
                        refType = ReferenceType.BookSection;
                        publication = " in " + publication;
                    }else {
//                        logger.warn(row + "probably only abbrev title");
                        publication = ", " + publication;
                    }
                } else {
//                    logger.warn(row + "probably not booksection");
                    publication = ", " + publication;
                }
            }else {
                publication = ", " + publication;
            }
        }else {
            logger.warn(row + "reference type not handled: " + refType);
            publication = ", " + publication;
        }
        String referenceName = CdmUtils.concat("", name.getTitleCache(), publication);
        TaxonName parsedName = parser.parseReferencedName(referenceName, NomenclaturalCode.ICNAFP, null);
        if (parsedName.isProtectedFullTitleCache() || parsedName.isProtectedTitleCache() ) {
            logger.warn(row + "name could not be parsed: " + referenceName);
        }else {
            Reference ref = parsedName.getNomenclaturalReference();
            if (useOrigPubl && origPubl != null && origPubl.getSecondResult() != null) {
                if (ref != null) {
                    ref.setDatePublished(TimePeriodParser.parseStringVerbatim(origPubl.getSecondResult()));
                }else {
                    ref = ReferenceFactory.newGeneric();
                    ref.setDatePublished(TimePeriodParser.parseStringVerbatim(origPubl.getSecondResult()));
                }
                logger.warn(row + "set original (Nomen.xlsx) year");
            }
            name.setNomenclaturalReference(ref);
            String microRef = parsedName.getNomenclaturalMicroReference();
            name.setNomenclaturalMicroReference(microRef);
        }

        //validateName (name);
        validateName(name, nameStr, row);

        //deduplicate
        dedupliateNameParts(name);

        //orig spelling
        if (isNotBlank(origSpelling)) {
            TaxonName origName = (TaxonName)parser.parseFullName(origSpelling);
            if (origName.isProtectedTitleCache()) {
                logger.warn(row + "orig name could not be parsed");
            }
            if (name.getNomenclaturalSource() == null) {
                logger.warn(row + "no nomsource yet");
            }
            name.getNomenclaturalSource(true).setNameUsedInSource(origName);
            origSpellingNames.add(origName);
        }
    }

    private void validateName(TaxonName name, String nomRefStr, String row) {
        nomRefStr = nomRefStr.replace("× ", "×");
        nomRefStr = nomRefStr.replace(" unranked ", " [unranked] ");
        nomRefStr = nomRefStr.replace(" [infrasp.unranked] ", " [infraspec.] ");

        if (!name.getTitleCache().equals(nomRefStr)) {
            TaxonName nomRefName = (TaxonName)parser.parseFullName(nomRefStr, NomenclaturalCode.ICNAFP, null);
            if (!nomRefName.getNameCache().equals(name.getNameCache())) {
                logger.warn(row+ "nameCache does not match: " + name.getNameCache() + "<->" + nomRefName.getNameCache());
                if (!CdmUtils.Nz(name.getAuthorshipCache()).equals(nomRefName.getAuthorshipCache())) {
                    logger.warn(row+ "also authorship differs: " + name.getAuthorshipCache() + "<->" + nomRefName.getAuthorshipCache());
                }
            }else {
                logger.warn(row+ "authors/titleCache do not match: " + name.getTitleCache() + "<->" + nomRefStr);
            }
            if (!CdmUtils.Nz(name.getAuthorshipCache()).equals(nomRefName.getAuthorshipCache())) {
                if (isBlank(nomRefName.getAuthorshipCache())) {
                    logger.warn(row + "'NomRef' authorship is empty but differs. Kept 'Nomen' authorship");
                }else {
                    name.setCombinationAuthorship(nomRefName.getCombinationAuthorship());
                    name.setExCombinationAuthorship(nomRefName.getExCombinationAuthorship());
                    name.setBasionymAuthorship(nomRefName.getBasionymAuthorship());
                    name.setExBasionymAuthorship(nomRefName.getExBasionymAuthorship());
                }
            }
        }
    }

    private ReferenceType getRefType(String pubType) {
        if ("A".equals(pubType)){
            return ReferenceType.Article;
        }else if ("B".equals(pubType)) {
            return ReferenceType.Book;
        }
        return null;
    }

    private TaxonName dedupliateNameParts(TaxonName name) {
        if (state.getConfig().isDoDeduplicate()){
            state.getDeduplicationHelper().replaceAuthorNamesAndNomRef(name);
        }
        return name;
    }


    private boolean first = true;
    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CaryoSileneaeImportConfigurator> state) {

         if (first) {
            if (origNameMap.size() > 0) {
                logger.warn("There are " +  origNameMap.size() + " unhandled names");
                for (Integer key : origNameMap.keySet()) {
                    System.out.println(key + ": " + origNameMap.get(key).getTitleCache());
                }
            }

            Set<TaxonName> commonSet = new HashSet<>(nameMap.values());
            commonSet.addAll(origNameMap.values());
            commonSet.addAll(origSpellingNames);
            try {
                getNameService().saveOrUpdate(commonSet);
            } catch (Exception e) {
                e.printStackTrace();
            }
            first = false;
        }
    }
}