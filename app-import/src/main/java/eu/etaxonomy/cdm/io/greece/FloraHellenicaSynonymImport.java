/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.NameRelationshipType;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author a.mueller
 * @date 14.12.2016
 */

@Component
public class FloraHellenicaSynonymImport<CONFIG extends FloraHellenicaImportConfigurator>
            extends FloraHellenicaImportBase<CONFIG>{

    private static final long serialVersionUID = -3565782012921316901L;
    private static final Logger logger = Logger.getLogger(FloraHellenicaSynonymImport.class);

    private static final String ACCEPTED_NAME = "Accepted name";
    private static final String SYNONYM = "synonym";
    private static final String UNIQUE_ID_OF_ACCEPTED_NAME = "Unique ID of accepted name";

   private  static List<String> expectedKeys= Arrays.asList(new String[]{
            SYNONYM, UNIQUE_ID_OF_ACCEPTED_NAME, ACCEPTED_NAME
    });

    private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();

    @Override
    protected String getWorksheetName() {
        return "synonyms";
    }

    boolean isFirst = true;
    /**
     * {@inheritDoc}
     */
    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {

        String line = state.getCurrentLine() + ": ";
        HashMap<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();
        for (String key: keys) {
            if (! expectedKeys.contains(key)){
                logger.warn(line + "Unexpected Key: " + key);
            }
        }
        if (isFirst){
            System.out.println("Start synonyms");
            isFirst = false;
        }

        String row = "row" + state.getCurrentLine();
        TaxonBase<?> relatedTaxon = makeSynonym(state, line, record, row);
        if (relatedTaxon != null){
            getTaxonService().saveOrUpdate(relatedTaxon);
        }
    }


    /**
     * @param state
     * @param line
     * @param record
     * @param noStr
     * @return
     */
    private TaxonBase<?> makeSynonym(SimpleExcelTaxonImportState<CONFIG> state, String line,
            HashMap<String, String> record,
            String lineId) {

        Taxon acceptedTaxon = getAcceptedTaxon(record, state, UNIQUE_ID_OF_ACCEPTED_NAME);
        if (acceptedTaxon == null){
            logger.warn(line + "Accepted not found: " + record.get(UNIQUE_ID_OF_ACCEPTED_NAME));
            return null;
//            acceptedTaxon = Taxon.NewInstance(null, null);
        }

        String synonymStr = getValue(record, SYNONYM);

        String[] parsedSynStr = parseAuct(synonymStr, line);

        boolean isMisapplied = parsedSynStr[1] != null;
        boolean hasNonAuthor = parsedSynStr[2] != null;
        boolean hasStatus = parsedSynStr[3] != null;
        boolean isNec = hasNonAuthor && parsedSynStr[2].contains(" nec ");


        String misappliedNecAuthor = null;
        if (isMisapplied && hasNonAuthor && !isNec){
            parsedSynStr[0] = parsedSynStr[0] + " " + parsedSynStr[2];
        }else if (isMisapplied && hasNonAuthor && isNec){
            misappliedNecAuthor = parsedSynStr[2];
        }

        INonViralName nvn = parser.parseFullName(parsedSynStr[0], NomenclaturalCode.ICNAFP, null);
        if (nvn.isProtectedTitleCache()){
            logger.warn(line + "Name could not be parsed: " + parsedSynStr[0]  + "  (full:"  + synonymStr + ")");
        }
        if (misappliedNecAuthor != null){
            nvn.setAuthorshipCache(misappliedNecAuthor);
        }
        TaxonName<?,?> name = TaxonName.castAndDeproxy(nvn);

        if (hasStatus){
            try {
                NomenclaturalStatusType status = NomenclaturalStatusType.getNomenclaturalStatusTypeByAbbreviation(parsedSynStr[3], name);
                name.addStatus(status, null, null);
            } catch (UnknownCdmTypeException e) {
                logger.warn(line + "Nom. status not recognized: " + parsedSynStr[3]);
            }
        }
        name = replaceNameAuthorsAndReferences(state, name);


        TaxonBase<?> result;
        if (isMisapplied){
            result = Taxon.NewInstance(name, getMisappliedRef(state, parsedSynStr[1]));
            acceptedTaxon.addMisappliedName((Taxon)result, getSecReference(state), null);
            if (isNec){
                logger.warn(line + "nec for misapplied names still needs to be checked: " + synonymStr);
            }
        }else{
            SynonymType synType = null;
            result = acceptedTaxon.addSynonymName(name, getSecReference(state), null, synType);
            if (hasNonAuthor){
                handleSynonymNon(state, name, parsedSynStr[2], line);
            }
        }
        result.addImportSource(lineId, getWorksheetName(), getSourceCitation(state), null);

        return result;

    }



    /**
     * @param state
     * @param name
     * @param parsedSynStr
     */
    private void handleSynonymNon(SimpleExcelTaxonImportState<CONFIG> state,
            TaxonName<?, ?> name, String nonPart, String line) {
        String[] splits = nonPart.split(" nec ");

        TaxonNameBase<?,?> lastHomonym = null;
        for (String split : splits){
            split = split.trim();
//            Saponaria illyrica Ard.
//            Crepis nemausensis Gouan
//            S. columnae Aurnier
//            S. columnae Aurnier nec (Rchb. f.) H. Fleischm.
//            T. glaucescens Rchb.
            TaxonName<?,?> nonName;
            if (split.matches("(Saponaria illyrica Ard.|Crepis nemausensis Gouan|S. columnae Aurnier|T. glaucescens Rchb.|Linaria stricta Guss.)"
                    + "")){
                if (split.startsWith("S.")){
                    split = split.replace("S.", "Serapias");
                }else if (split.startsWith("T.")){
                    split = split.replace("T.", "Taraxacum");
                }
                nonName = TaxonName.castAndDeproxy(this.parser.parseFullName(split));
                nonName = replaceNameAuthorsAndReferences(state, nonName);
                name.addRelationshipFromName(nonName, NameRelationshipType.BLOCKING_NAME_FOR(), null);
            }else{
                String nameStr = name.getNameCache().replace(" hort.", "") + " " + split;
                nonName = TaxonName.castAndDeproxy(this.parser.parseFullName(nameStr));
                nonName = replaceNameAuthorsAndReferences(state, nonName);
                name.addRelationshipToName(nonName, NameRelationshipType.LATER_HOMONYM(), null);
                if (lastHomonym != null){
                    nonName.addRelationshipToName(lastHomonym, NameRelationshipType.LATER_HOMONYM(), null);
                }
                lastHomonym = nonName;
            }
            getNameService().saveOrUpdate(nonName);
            if (nonName.isProtectedTitleCache()){
                logger.warn(line + "Non-Name could not be parsed: " + nonName.getTitleCache());
            }
        }
        //seems to work correctly
//        if (splits.length>1){
//            logger.warn(line + "nec synonyms maybe not yet correctly implemented: " + name.getTitleCache() + "; " + nonPart);
//        }
    }

    private Reference flGraecReference;
    private Reference balkanReference;
    {
        flGraecReference = ReferenceFactory.newBook();
        flGraecReference.setTitle("fl. graec.");
        balkanReference = ReferenceFactory.newBook();
        balkanReference.setTitle("balc.");
    }
    /**
     * @param state
     * @param string
     * @return
     */
    private Reference getMisappliedRef(SimpleExcelTaxonImportState<CONFIG> state, String refString) {
        if ("fl. graec.".equals(refString)){
            return flGraecReference;
        }else if ("balc.".equals(refString)){
            return balkanReference;
        }else{
            logger.warn("Auct. reference not recognized: " + refString);
            return null;
        }
    }

    private String regExMisapplied = "(.+) auct\\. (fl\\. graec\\.|balc\\.), non (.+)";
    private Pattern patternMisapplied = Pattern.compile(regExMisapplied);

    private String regExNon = "(.+), non (.+)";
    private Pattern patternNon = Pattern.compile(regExNon);

    private String regExStatus = "(.+),\\s+((?:nom.|comb.|orth.)\\s+(.+))";
    private Pattern patternStat = Pattern.compile(regExStatus);

    /**
     * @param synonymStr
     */
    private String[] parseAuct(String synonymStr, String line) {
        String[] result = new String[4];
        if (synonymStr != null){
            result[0] = synonymStr;
            Matcher matcher = patternMisapplied.matcher(synonymStr);
            if (matcher.matches()){
                result[0] = matcher.group(1);
                result[1] = matcher.group(2);
                if (! result[1].equals("fl. graec.") && ! result[1].equals("balc.")){
                    logger.warn(line + "Misapplied sensu not recognized: " +  result[1]);
                }
                result[2] = matcher.group(3);
            }else{
                matcher = patternNon.matcher(synonymStr);
                if (matcher.matches()){
                    result[0] = matcher.group(1);
                    result[2] = matcher.group(2);
                }else{
                    matcher = patternStat.matcher(synonymStr);
                    if (matcher.matches()){
                        result[0] = matcher.group(1);
                        result[3] = matcher.group(2);
                    }
                }
            }
        }
        return result;
    }

}
