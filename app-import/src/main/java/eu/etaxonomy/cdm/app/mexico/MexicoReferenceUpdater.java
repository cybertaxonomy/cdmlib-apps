/**
* Copyright (C) 2021 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.mexico;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.service.dto.IdentifiedEntityDTO;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.ExcelUtils;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.config.CdmSourceException;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.mexico.MexicoConabioTransformer;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.reference.ReferenceType;
import eu.etaxonomy.cdm.model.term.IdentifierType;
import eu.etaxonomy.cdm.persistence.query.MatchMode;

/**
 * @author a.mueller
 * @since 09.09.2021
 */
public class MexicoReferenceUpdater {

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;

//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_mexico();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_mexico_flora();

    private void doUpdate(ICdmDataSource dataSource) throws CdmSourceException {

        dataSource.checkConnection();
        CdmApplicationController app = CdmApplicationController.NewInstance(dataSource, hbm2dll);
        IdentifierType identifierType = (IdentifierType)app.getTermService().find(MexicoConabioTransformer.uuidConabioTaxonIdIdentifierType);

        Map<String,Reference> journalMap = getJournalMap(app);
        try {
            List<Map<String, String>> excelList = ExcelUtils.parseXLS(referenceCorr(), "TaxonomyInclRefType");
            Map<String, String> newRefs = new HashMap<>();
            int line = 1;
            for (Map<String, String> record : excelList){
                line++;
                if (line <= 5335){
                    continue;
                }
                String refType = record.get("ReferenceType");
                if (!"A".equals(refType)){
                    continue;
                }
                String id = record.get("IdCAT");
                String nom = record.get("CitaNomenclatural");
                newRefs.put(id, nom);
                handleRef(app, id, nom, identifierType, journalMap, line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void handleRef(CdmApplicationController app, String id, String nomRefStr, IdentifierType identifierType, Map<String, Reference> journalMap, int line) {
        String lineStr = " " + id +"; line: " + line;
        try {
            List<String> props = Arrays.asList(new String[]{"combinationAuthor.teamMembers","identifiers","nomenclaturalSource.citation.authorTeam.teamMembers"});
            List<IdentifiedEntityDTO<TaxonName>> names = app.getNameService().findByIdentifier(TaxonName.class, id, identifierType, MatchMode.EXACT, true, null, null, props).getRecords();
            boolean doPrint = false;
            if (names.size()>1){
                System.out.println("More than 1 name found: " + lineStr);
            }else if (names.isEmpty()){
                System.out.println("Name not found: " + lineStr);
            }else{
                TaxonName name = names.get(0).getCdmEntity().getEntity();
                Reference existingNomRef = CdmBase.deproxy(name.getNomenclaturalReference());
                if (!existingNomRef.getType().equals(ReferenceType.Book)){
                    if (doPrint) {
                        System.out.println("  Ref is not a book anymore: "+ lineStr);
                    }
                }else if (existingNomRef.getCreated().compareTo(DateTime.parse("2021-08-26T10:00:00.000+02:00")) < 0 ){
                    System.out.println("  Ref was created before: "+ lineStr + ", " + existingNomRef.getCreated() );
                }else if (existingNomRef.getUpdated()!= null){
                    System.out.println("!! Ref was previously updated: "+ lineStr );
                }else{
                    if (doPrint){ System.out.println("    " + lineStr + ":" + existingNomRef.getTitleCache());}
                    String abbrevTitle = existingNomRef.getAbbrevTitle();

                    if (existingNomRef.isProtectedTitleCache() || existingNomRef.isProtectedAbbrevTitleCache()){
                        System.out.println("!! Ref is protected: " + lineStr + "; " + name.getFullTitleCache());
                        existingNomRef.setType(ReferenceType.Article);
                        app.getReferenceService().update(existingNomRef);
                    }else if (StringUtils.isBlank(abbrevTitle)){
                        System.out.println("!! Ref has no abbrevTitle: "+ lineStr);
                    }else{
                        Reference journal = journalMap.get(existingNomRef.getAbbrevTitle());
                        if (journal == null){
//                        System.out.println(" ! Journal does not yet exist. Create new one: " + abbrevTitle + ", " + lineStr);
                            journal = ReferenceFactory.newJournal();
                            journal.setAbbrevTitle(abbrevTitle);
                            app.getReferenceService().save(journal);
                            journalMap.put(abbrevTitle, journal);
                        }

                        existingNomRef.setType(ReferenceType.Article);
                        existingNomRef.setInJournal(journal);
                        existingNomRef.setAbbrevTitle(null);
                        app.getReferenceService().update(existingNomRef);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error in " + lineStr);
//            throw e;
        }
    }

    private void handleRefs(Map<String, String> newRefs, CdmApplicationController app, IdentifierType identifierType) {
        List<String> props = Arrays.asList(new String[]{"identifiers","nomenclaturalSource.citation"});
        for (String id : newRefs.keySet()){
            List<IdentifiedEntityDTO<TaxonName>> names = app.getNameService().findByIdentifier(TaxonName.class, id, identifierType, MatchMode.EXACT, true, null, null, props).getRecords();
            if (names.size()>1){
                System.out.println("More than 1 name found: " + id);
            }else if (names.isEmpty()){
                System.out.println("Name not found: " + id);
            }else{
                TaxonName name = names.get(0).getCdmEntity().getEntity();
                Reference ref = CdmBase.deproxy(name.getNomenclaturalReference());
                System.out.println(ref.getType());
                System.out.println( ref.getCreated());
                System.out.println( ref.getUpdated());
                if (!ref.getType().equals(ReferenceType.Book)){
                    System.out.println("Ref is not a book anymore: "+ id );
                }else if (ref.getCreated().compareTo(DateTime.parse("2021-08-26T10:00:00.000+02:00")) < 0 ){
                    System.out.println("Ref was created before: "+ id + ", " + ref.getCreated() );
                }else if (ref.getUpdated()!= null){
                    System.out.println("Ref was updated: "+ id );
                }else{
                    System.out.println(ref.getTitleCache());
                    System.out.println(ref.getAbbrevTitle());
//                    ref.setType(ReferenceType.Article);
                }
            }
        }
    }

    private Map<String, Reference> getJournalMap(CdmApplicationController app) {
        List<Reference> refs = app.getReferenceService().list(null, null, null, null, null);
        Map<String, Reference> result = new HashMap<>();
        for (Reference ref : refs){
            if (ref.getType().equals(ReferenceType.Journal)){
                result.put(ref.getAbbrevTitleCache(), ref);
            }
        }
        return result;
    }

    public static URI referenceCorr() {
        return URI.create("file:////BGBM-PESIHPC/Mexico/TaxonomyInclRefTypeCorr.xlsx");
    }

    public static void main(String[] args) {

        MexicoReferenceUpdater refUpdater = new MexicoReferenceUpdater();
        try {
            refUpdater.doUpdate(cdmDestination);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
  }
}