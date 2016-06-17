// $Id$
/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.excel.common.ExcelImporterBase;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.strategy.parser.INonViralNameParser;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * Simple Excel import class that works without default state class
 * {@link SimpleExcelTaxonImportState}
 * @author a.mueller
 * @date 16.06.2016
 */
public abstract class SimpleExcelTaxonImport<CONFIG extends ExcelImportConfiguratorBase>
        extends ExcelImporterBase<SimpleExcelTaxonImportState<CONFIG>>{

    private static final long serialVersionUID = -4345647703312616421L;

    private static final Logger logger = Logger.getLogger(SimpleExcelTaxonImport.class);

    protected static INonViralNameParser<?> nameParser = NonViralNameParserImpl.NewInstance();


    @Override
    protected void analyzeRecord(HashMap<String, String> record, SimpleExcelTaxonImportState<CONFIG> state) {
        //override only if needed
    }

//    @Override
//    protected void firstPass(SimpelExcelTaxonImportState<CONFIG> state) {
//        // TODO Auto-generated method stub
//    }

    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CONFIG> state) {
        //override only if needed
    }

    @Override
    protected boolean isIgnore(SimpleExcelTaxonImportState<CONFIG> state) {
        return false;
    }

//***************************** METHODS *********************************/
    /**
     * Returns the value of the record map for the given key.
     * The value is trimmed and empty values are set to <code>null</code>.
     * @param record
     * @param originalKey
     * @return
     */
    protected String getValue(Map<String, String> record, String originalKey) {
        String value = record.get(originalKey);
        if (! StringUtils.isBlank(value)) {
            if (logger.isDebugEnabled()) { logger.debug(originalKey + ": " + value); }
            value = CdmUtils.removeDuplicateWhitespace(value.trim()).toString();
            return value;
        }else{
            return null;
        }
    }

    /**
     * @param state
     * @return
     */
    protected IdentifiableSource makeOriginalSource(SimpleExcelTaxonImportState<CONFIG> state) {
        return IdentifiableSource.NewDataImportInstance("line: " + state.getCurrentLine(), null, state.getConfig().getSourceReference());
    }

    /**
     * @param state
     * @param speciesName
     * @param sec
     * @param taxon
     * @param familyNode
     */
    protected void makeGenus(SimpleExcelTaxonImportState<CONFIG> state,
            BotanicalName speciesName,
            Reference sec,
            Taxon taxon,
            TaxonNode familyNode) {

        TaxonNode higherNode;
        if (speciesName.isProtectedTitleCache()){
            higherNode = familyNode;
        }else{
            String genusStr = speciesName.getGenusOrUninomial();
            Taxon genus = state.getHigherTaxon(genusStr);
            if (genus != null){
                higherNode = genus.getTaxonNodes().iterator().next();
            }else{
                BotanicalName genusName = BotanicalName.NewInstance(Rank.GENUS());
                genusName.addSource(makeOriginalSource(state));
                genusName.setGenusOrUninomial(genusStr);
                genus = Taxon.NewInstance(genusName, sec);
                genus.addSource(makeOriginalSource(state));
                higherNode = familyNode.addChildTaxon(genus, null, null);
                state.putHigherTaxon(genusStr, genus);
            }
        }

        higherNode.addChildTaxon(taxon, null, null);
        taxon.addSource(makeOriginalSource(state));
    }

    /**
     * @param line
     * @param keys
     * @param expectedKeys
     */
    protected void checkAllKeysExist(String line, Set<String> keys, List<String> expectedKeys) {
        for (String key: keys) {
            if (! expectedKeys.contains(key)){
                logger.warn(line + "Unexpected Key: " + key);
            }
        }
    }


    /**
     * @param state
     * @param name
     */
    protected void replaceAuthorNames(SimpleExcelTaxonImportState<CONFIG> state, BotanicalName name) {
        TeamOrPersonBase<?> combAuthor = name.getCombinationAuthorship();
        name.setCombinationAuthorship(getExistingAuthor(state, combAuthor));

        TeamOrPersonBase<?> exAuthor = name.getExCombinationAuthorship();
        name.setExCombinationAuthorship(getExistingAuthor(state, exAuthor));

        TeamOrPersonBase<?> basioAuthor = name.getBasionymAuthorship();
        name.setBasionymAuthorship(getExistingAuthor(state, basioAuthor));

        TeamOrPersonBase<?> exBasioAuthor = name.getExBasionymAuthorship();
        name.setExBasionymAuthorship(getExistingAuthor(state, exBasioAuthor));

    }

    /**
     * @param state
     * @param combAuthor
     * @return
     */
    protected TeamOrPersonBase<?> getExistingAuthor(SimpleExcelTaxonImportState<CONFIG> state,
            TeamOrPersonBase<?> author) {
        if (author == null){
            return null;
        }else{
            TeamOrPersonBase<?> result = state.getAgentBase(author.getTitleCache());
            if (result == null){
                state.putAgentBase(author.getTitleCache(), author);
                if (author instanceof Team){
                    handleTeam(state, (Team)author);
                }
                result = author;
            }
            return result;
        }
    }

    /**
     * @param state
     * @param author
     */
    private void handleTeam(SimpleExcelTaxonImportState<CONFIG> state, Team team) {
        List<Person> members = team.getTeamMembers();
        for (int i =0; i< members.size(); i++){
            Person person = members.get(i);
            //FIXME cast find a better way to guarantee that only persons are returned
            Person existingPerson = (Person)state.getAgentBase(person.getTitleCache());
            if (existingPerson != null){
                members.set(i, existingPerson);
            }else{
                state.putAgentBase(person.getTitleCache(), person);
            }
        }

    }


}
