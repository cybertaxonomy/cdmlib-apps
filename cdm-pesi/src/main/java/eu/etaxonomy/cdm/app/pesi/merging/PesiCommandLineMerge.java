/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.pesi.merging;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.DeleteResult;
import eu.etaxonomy.cdm.api.service.config.SynonymDeletionConfigurator;
import eu.etaxonomy.cdm.api.service.config.TaxonDeletionConfigurator;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.CdmRegEx;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.io.common.mapping.out.DbLastActionMapper;
import eu.etaxonomy.cdm.io.pesi.erms.ErmsTransformer;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Credit;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.IdentifiableEntity;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;

/**
 * @author a.mueller
 * @since 20.01.2020
 */
public class PesiCommandLineMerge extends PesiMergeBase {

    private static Logger logger = LogManager.getLogger();

    static final ICdmDataSource pesiSource = CdmDestinations.cdm_pesi2019_final();

    private CdmIoApplicationController app;

    private void invoke(ICdmDataSource source){
        app = CdmIoApplicationController.NewInstance(source, DbSchemaValidation.VALIDATE, false);
        doInvoke();
    }

    private void doInvoke(){
        List<List<String>> fileData = null;
        String next = nextMerge(fileData);
        while(next.equalsIgnoreCase("m")|| next.equals("f")){
            TransactionStatus tx = app.startTransaction();
            TaxonInformation taxonInformation;
            if (next.equalsIgnoreCase("f")){
                if (fileData == null){
                    fileData = getFileData();
                }else if (fileData.isEmpty()){
                    fileData = null;
                    next = nextMerge(fileData);
                    continue;
                }
                taxonInformation = readLineFromFile(fileData);
                if (taxonInformation == null){
                    app.rollbackTransaction(tx);
                    nextMerge(fileData);
                    continue;
                }
            }else{
                TaxonBase<?>[] taxa = null;
                while (taxa == null) {
                    taxa = readTaxa();
                }
                taxonInformation = new TaxonInformation();
                taxonInformation.taxon2 = taxa[0];
                taxonInformation.taxon1 = taxa[1];
            }

            try {
                mergeTaxa(tx, taxonInformation);
            } catch (Exception e) {
                e.printStackTrace();
                app.rollbackTransaction(tx);
                continue;
            }
            next = nextMerge(fileData);
        }
    }

    private boolean mergeTaxa(TransactionStatus tx, TaxonInformation taxonInformation) {
        boolean commit = compareTaxa(taxonInformation);
        if (commit){
            moveTaxonInformation(taxonInformation);
        }
        if (commit){
            app.commitTransaction(tx);
            if (isAutomatedAnswer(taxonInformation)){
                removeTaxon(taxonInformation.taxon2);
            }else if (booleanAnswer("Information moved. Delete old taxon")){
                removeTaxon(taxonInformation.taxonToUse == 2 ? taxonInformation.taxon1 : taxonInformation.taxon2);
            }
        }else{
            app.rollbackTransaction(tx);
        }
        return commit;
    }

    private boolean isAutomatedAnswer(TaxonInformation taxonInformation) {
        return taxonInformation.taxonToUse == 2 && taxonInformation.nameToUse == 2 && false;
    }

    private class TaxonInformation{
        TaxonBase<?> taxon1;
        TaxonBase<?> taxon2;
        int taxonToUse = 1;   //
        int nameToUse = 1;
    }

    /**
     * Reads a line from the file, returns it's taxon information and removes
     * the line from the input list.
     */
    private TaxonInformation readLineFromFile(List<List<String>> fileData) {
        List<String> line = fileData.get(0);
        TaxonInformation taxonInformation = new TaxonInformation();
        taxonInformation.taxon1 = taxonByString(line.get(0));
        taxonInformation.taxon2 = taxonByString(line.get(1));

        if (taxonInformation.taxon1 == null || taxonInformation.taxon2 == null){
            boolean cancel = booleanAnswer("Taxon1 or Taxon2 could not be read from DB! Cancel record");
            if (cancel){
                fileData.remove(0);
            }
            return null;
        }

        try {
            Integer taxonToUse = Integer.valueOf(line.get(2));
            if (1 != taxonInformation.taxonToUse){
                booleanAnswer("Stay taxon is not '1'");
            }
            Integer nameToUse = "".equals(line.get(3))? taxonToUse: Integer.valueOf(line.get(3));
            if (taxonToUse != 1 && taxonToUse != 2 && taxonToUse != 0){
                boolean cancel = booleanAnswer("taxonToUse is not 0, 1 or 2. Cancel record");
                if (cancel){
                    fileData.remove(0);
                }
                return null;
            }else if (taxonToUse == 0){
                logger.warn("Record marked as homonym. No merge: " + taxonInformation.taxon1.getName().getNameCache());
                fileData.remove(0);
                return null;
            }else{
                taxonInformation.taxonToUse = taxonToUse;
            }
            if (nameToUse == null){
                nameToUse = taxonToUse;
            }
            if (nameToUse != 1 && nameToUse != 2){
                logger.warn("Name to use has incorrect value: " +  nameToUse);
            }else{
                taxonInformation.nameToUse = nameToUse;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            taxonInformation = null;
        }
        fileData.remove(0);
        return taxonInformation;
    }

    private List<List<String>> getFileData() {
        List<List<String>> result = null;
        while(result == null){
            String input = CdmUtils.readInputLine("Path and filename: ");
            result = readCsvFile(input);
        }
        return result;
    }

    private String nextMerge(List<List<String>> fileData) {
        if (fileData != null){
            return "f";
        }
        do{
            String input = CdmUtils.readInputLine("Next input: manual[m], file[f], quit[q]: ");
            if (input.matches("[mMfFqQ]")){
                return input;
            }
        }while (true);
    }

    private boolean compareTaxa(TaxonInformation taxonInformation) {
        TaxonBase<?> removeTaxon = taxonInformation.taxon2;
        TaxonBase<?> stayTaxon = taxonInformation.taxon1;
        if(removeTaxon.getId() == stayTaxon.getId()){
            logger.warn("Same taxon: "+  removeTaxon.getTitleCache());
            return false;
        }
        String nc1 = removeTaxon.getName().getNameCache();
        String nc2 = stayTaxon.getName().getNameCache();

        String ft1 = removeTaxon.getName().getFullTitleCache();
        String ft2 = stayTaxon.getName().getFullTitleCache();
        System.out.println("Remove " + getStatusStr(removeTaxon) + ft1);
        System.out.println("Stay   " + getStatusStr(stayTaxon) + ft2);
        boolean isStandard = isAutomatedAnswer(taxonInformation);
        if (!nc1.equals(nc2)){
            return booleanAnswer("Name Cache differs!!! Do you really want to merge???");
        }else if (!ft1.equals(ft2)){
            return isStandard || booleanAnswer("Full title cache differs! Do you really want to merge anyway");
        }else{
            return isStandard || booleanAnswer("Same title. Merge");
        }
    }

    private String getStatusStr(TaxonBase<?> taxon) {
        //TODO MAN and Taxon Synonyms
        if (taxon.isInstanceOf(Synonym.class)){
            return "Syn: ";
        }else{
            return "Acc: ";
        }
    }

    private void removeTaxon(TaxonBase<?> taxonBase) {
        DeleteResult result;
        if (taxonBase.isInstanceOf(Taxon.class)){
            Taxon taxonToRemove = CdmBase.deproxy(taxonBase, Taxon.class);
            TaxonDeletionConfigurator config = new TaxonDeletionConfigurator();
            if (isTaxonSynonym(taxonToRemove)){
                result = app.getTaxonService().deleteTaxon(taxonToRemove.getUuid(), config, null);
            }else{
                TaxonNode nodeToRemove = taxonToRemove.getTaxonNodes().iterator().next();
                result = app.getTaxonNodeService().deleteTaxonNode(nodeToRemove.getUuid(), config);
            }
        }else{
            Synonym syn = CdmBase.deproxy(taxonBase, Synonym.class);
            SynonymDeletionConfigurator config = new SynonymDeletionConfigurator();
            result = app.getTaxonService().deleteSynonym(syn.getUuid(), config);
        }
        if (!result.isOk()){
            System.out.println("Remove taxon was not successful.");
        }
    }

    private boolean booleanAnswer(String message) {
        String answer = "";
        while (!(answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("n"))){
            answer = CdmUtils.readInputLine(message + " (y/n)? ");
        }
        return answer.equalsIgnoreCase("y");
    }

    private boolean moveTaxonInformation(TaxonInformation taxonInformation) {
        try {

            TaxonBase<?> removeTaxon = CdmBase.deproxy(taxonInformation.taxonToUse == 2 ? taxonInformation.taxon1: taxonInformation.taxon2);
            TaxonBase<?> stayTaxon = CdmBase.deproxy(taxonInformation.taxonToUse == 2 ? taxonInformation.taxon2 : taxonInformation.taxon1);

            //mergeTaxa;
            mergeSources(removeTaxon, stayTaxon);
            mergeAnnotations(removeTaxon, stayTaxon);
            mergeMarkers(removeTaxon, stayTaxon);
            //TODO for
            mergeExtensions(removeTaxon, stayTaxon);
            mergeCredits(removeTaxon, stayTaxon);
            if (removeTaxon.isInstanceOf(Taxon.class)){
                Taxon removeAccTaxon = CdmBase.deproxy(removeTaxon, Taxon.class);

                Taxon stayAccTaxon = accTaxon(stayTaxon);
                mergeDescriptions(removeAccTaxon, accTaxon(stayTaxon));
                boolean isTaxonSynonym = isTaxonSynonym(removeAccTaxon);
                mergeSynonyms(removeAccTaxon, stayAccTaxon, isTaxonSynonym);
                mergeChildren(removeAccTaxon, stayAccTaxon, isTaxonSynonym);
                //TODO taxon synonym relations
                mergeTaxonRelations(removeAccTaxon, stayAccTaxon, isTaxonSynonym);
            }

            //mergeNames;
            TaxonName removeName;
            TaxonName stayName;
            if (taxonInformation.nameToUse == taxonInformation.taxonToUse){
                removeName = CdmBase.deproxy(removeTaxon.getName());
                stayName = CdmBase.deproxy(stayTaxon.getName());
            }else{
                removeName = CdmBase.deproxy(stayTaxon.getName());
                stayName = CdmBase.deproxy(removeTaxon.getName());
                stayTaxon.setName(stayName);
            }
            //TODO unclear if name information should be merged at all
            mergeSources(removeName, stayName);
            mergeAnnotations(removeName, stayName);
            mergeMarkers(removeName, stayName);
            mergeExtensions(removeName, stayName);
            mergeCredits(removeName, stayName);
            mergeNameRelationships(removeName, stayName);
            mergeHybridRelationships(removeName, stayName);
            mergeNameDescriptions(removeName, stayName);

            if(isAutomatedAnswer(taxonInformation)){
                return true;
            }else{
                return booleanAnswer("Commit moved information");
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isTaxonSynonym(Taxon removeAccTaxon) {
        for (TaxonRelationship rel:  removeAccTaxon.getRelationsFromThisTaxon()){
            boolean isPseudo = TaxonRelationshipType.pseudoTaxonUuids().contains(rel.getType().getUuid());
            if (isPseudo){
                return true;
            }
        }
        return false;
    }

    private Taxon accTaxon(TaxonBase<?> stayTaxon) {
        if (stayTaxon.isInstanceOf(Synonym.class)){
            return CdmBase.deproxy(stayTaxon, Synonym.class).getAcceptedTaxon();
        }else{
            return CdmBase.deproxy(stayTaxon, Taxon.class);
        }
    }

    private boolean mergeTaxonRelations(Taxon removeTaxon, Taxon stayTaxon, boolean isTaxonSynonym) {
        if (isTaxonSynonym){
            if (!removeTaxon.getRelationsToThisTaxon().isEmpty()){
                logger.warn("taxon synonym has taxon relations to itself. This should not happen. Handle manually.");
                return false;
            }else{
                return true;
            }
        }
        for (TaxonRelationship rel : removeTaxon.getRelationsToThisTaxon()){
            System.out.println("Move taxon relationship: " + rel.getType().getTitleCache() + ": " + rel.getFromTaxon().getTitleCache());

            rel.setToTaxon(stayTaxon);
//            if (!synonymExists()){
//                //TODO homotypical group
//                stayTaxon.addSynonym(synonym, synonym.getType());
//            }else{
//                //TODO merge synonym names
//            }
        }
        if(!removeTaxon.getRelationsFromThisTaxon().isEmpty()){
            logger.warn("Taxon-from-relations not yet implemented");
        }
        return true;
    }

    private void mergeNameDescriptions(TaxonName removeName, @SuppressWarnings("unused") TaxonName stayName) {
        if(!removeName.getDescriptions().isEmpty()){
            logger.warn("Name description exist but merge not yet implemented");
        }
    }

    private void mergeHybridRelationships(TaxonName removeName, @SuppressWarnings("unused") TaxonName stayName) {
        if(!removeName.getHybridChildRelations().isEmpty()){
            logger.warn("Hybrid child relation exist but merge not yet implemented");
        }
        if(!removeName.getHybridParentRelations().isEmpty()){
            logger.warn("Hybrid parent relation exist but merge not yet implemented");
        }
    }

    private void mergeNameRelationships(TaxonName removeName, @SuppressWarnings("unused") TaxonName stayName) {
        if(!removeName.getNameRelations().isEmpty()){
            logger.warn("Name relations exist but merge not yet implemented");
        }
    }

    private boolean mergeChildren(Taxon removeTaxon, Taxon stayTaxon, boolean isTaxonSynonym) {
        if (isTaxonSynonym){
            if (!removeTaxon.getTaxonNodes().isEmpty()){
                logger.warn("taxon synonym has taxon node itself. This should not happen. Handle manually.");
                return false;
            }else{
                return true;
            }
        }
        TaxonNode removeNode = removeTaxon.getTaxonNodes().iterator().next();
        if(removeNode.getChildNodes().isEmpty()){
            return true;
        }

        stayTaxon = reallyAccTaxon(stayTaxon);
        TaxonNode stayNode = stayTaxon.getTaxonNodes().iterator().next();
        Set<UUID> removeNodeChildrenUuids = removeNode.getChildNodes()
                .stream().map(tn->tn.getUuid()).collect(Collectors.toSet());

        if(!removeNodeChildrenUuids.isEmpty()){
            app.getTaxonNodeService().moveTaxonNodes(removeNodeChildrenUuids,
                    stayNode.getUuid(), 0, null);
            System.out.println("Child nodes moved: " + removeNodeChildrenUuids.size());
        }
        return true;
    }

    private Taxon reallyAccTaxon(Taxon stayTaxon) {
        if (isTaxonSynonym(stayTaxon)){
            for (TaxonRelationship rel: stayTaxon.getRelationsFromThisTaxon()){
                boolean isPseudo = TaxonRelationshipType.pseudoTaxonUuids().contains(rel.getType().getUuid());
                if (isPseudo){
                    return rel.getToTaxon();
                }
            }
        }
        return stayTaxon;
    }
    private boolean mergeSynonyms(Taxon removeTaxon, Taxon stayTaxon, boolean isTaxonSynonym) {
        if (isTaxonSynonym){
            if (!removeTaxon.getSynonyms().isEmpty()){
                logger.warn("taxon synonym has synonyms itself. This should not happen. Handle manually.");
                return false;
            }else{
                return true;
            }
        }
        Set<Synonym> synonymsToAdd = new HashSet<>();
        for (Synonym synonym : removeTaxon.getSynonyms()){
            if (!synonymExists()){
                //TODO homotypical group
                synonymsToAdd.add(synonym);
            }else{
                //TODO merge synonym names
            }
        }
        for (Synonym synonym: synonymsToAdd){
            stayTaxon.addSynonym(synonym, synonym.getType());
        }
        return true;
    }

    private boolean synonymExists() {
        logger.warn("Synonym dulicate check - not yet implemented");
        return false;
    }

    private void mergeDescriptions(Taxon remove, Taxon stay) {
        //TODO handle duplicates for taxon descriptions
        for (TaxonDescription description: remove.getDescriptions()){
            System.out.println("Move taxon description: " + description.getTitleCache());
            stay.addDescription(description.clone());
        }
    }

    private void mergeCredits(IdentifiableEntity<?> removeEntity,
            IdentifiableEntity<?> stayEntity) throws CloneNotSupportedException {
        String className = removeEntity.getClass().getSimpleName();
        for (Credit credit: removeEntity.getCredits()){
            System.out.println("Move "+className+" credit: " + credit.toString());
            stayEntity.addCredit(credit.clone());
        }
    }

    private void mergeExtensions(IdentifiableEntity<?> removeEntity,
            IdentifiableEntity<?> stayEntity) throws CloneNotSupportedException {

        String className = removeEntity.getClass().getSimpleName();
        for (Extension extension: removeEntity.getExtensions()){
            if (!filterExtension(extension, removeEntity, stayEntity)){
                System.out.println("Move "+className+" extension: " + extension.getType().getTitleCache() + ": " + extension.getValue());

                IdentifiableEntity<?> thisStayEntity = selectStay(removeEntity, stayEntity, "Extension");
                if (thisStayEntity != null){
                    thisStayEntity.addExtension(extension.clone());
                }
            }
        }
    }

    private <T extends IdentifiableEntity<?>> T selectStay(T removeEntity, T stayEntity, String type) {
        if(removeEntity.isInstanceOf(Taxon.class) && stayEntity.isInstanceOf(Synonym.class)){
            String answer = "";
            while(!(answer.matches("[sSaAcC]"))){
                answer = CdmUtils.readInputLine(type + ": Stay is Synonym. Move information to [s]ynonym, to [a]ccepted or [c]ancel extension merge?: ");
            }
            if (answer.equalsIgnoreCase("c")){
                return null;
            }else if (answer.equalsIgnoreCase("a")){
               return (T)accTaxon(CdmBase.deproxy(stayEntity, Synonym.class));
            }else{
                return stayEntity;
            }
        }
        return stayEntity;
    }

    private boolean filterExtension(Extension extension,
            @SuppressWarnings("unused") IdentifiableEntity<?> removeEntity,
            @SuppressWarnings("unused") IdentifiableEntity<?> stayEntity) {
        if (extension.getType().getUuid().equals(ErmsTransformer.uuidExtDisplayName)){
            //for merged taxa display name information is not relevant because name is formatted according to "stay" taxon.
            return true;
        }
        return false;
    }

    private void mergeMarkers(IdentifiableEntity<?> removeEntity,
            IdentifiableEntity<?> stayEntity) throws CloneNotSupportedException {
        String className = removeEntity.getClass().getSimpleName();
        for (Marker marker: removeEntity.getMarkers()){
            if (!filterMarker(marker, removeEntity, stayEntity)){
                System.out.println("Move "+className+" marker: " + marker.getMarkerType().getTitleCache() + ": " + marker.getValue());
                IdentifiableEntity<?> thisStayEntity = selectStay(removeEntity, stayEntity, "Marker");
                if (thisStayEntity != null){
                    thisStayEntity.addMarker(marker.clone());
                }
            }
        }
    }

    private void mergeAnnotations(IdentifiableEntity<?> removeEntity,
            IdentifiableEntity<?> stayEntity) throws CloneNotSupportedException {
        String className = removeEntity.getClass().getSimpleName();
        for (Annotation annotation: removeEntity.getAnnotations()){
            if (!filterAnnotation(annotation, removeEntity, stayEntity)){
                String type = annotation.getAnnotationType() == null? "no type" : annotation.getAnnotationType().getTitleCache();
                System.out.println("Move "+className+" note: " + type + ": " + annotation.getText());
                handleRemoveAnnotation(annotation, removeEntity, stayEntity);
                stayEntity.addAnnotation(annotation.clone());
            }
        }
    }

    private void mergeSources(IdentifiableEntity<?> removeEntity,
            IdentifiableEntity<?> stayEntity) throws CloneNotSupportedException {
        String className = removeEntity.getClass().getSimpleName();
        for (IdentifiableSource source: removeEntity.getSources()){
            System.out.println("Move "+className+" source: " + source.getType().getLabel() + ": " + source.getCitation().getTitleCache() + "; " + source.getIdInSource() + "/" + source.getIdNamespace());
            stayEntity.addSource(source.clone());
        }
    }

    private boolean filterMarker(Marker marker, @SuppressWarnings("unused") IdentifiableEntity<?> removeEntity,
            IdentifiableEntity<?> stayEntity) {
        if (isNoLastActionMarker(marker)){
            for (Annotation annotation : stayEntity.getAnnotations()){
                if (isLastActionDateAnnotation(annotation)){
                        return true;
                }
            }
        }
        return false;
    }

    private boolean isLastActionDateAnnotation(Annotation annotation) {
        return annotation.getAnnotationType()!= null
                && annotation.getAnnotationType().getUuid().equals(DbLastActionMapper.uuidAnnotationTypeLastActionDate)
                && !isBlank(annotation.getText());
    }

    private void handleRemoveAnnotation(Annotation annotation,
            @SuppressWarnings("unused") IdentifiableEntity<?> removeEntity,
            IdentifiableEntity<?> stayEntity) {
        if (isLastActionDateAnnotation(annotation)){
            Optional<Marker> noLastActionMarker = stayEntity.getMarkers().stream().filter(m->isNoLastActionMarker(m)).findFirst();
            if (noLastActionMarker.isPresent()){
                stayEntity.removeMarker(noLastActionMarker.get());
                System.out.println("  NoLastActionDate annotation removed from 'stay' " + stayEntity.getClass().getSimpleName());
            }
        }
    }

    private boolean isNoLastActionMarker(Marker marker) {
        return marker.getMarkerType().getUuid().equals(PesiTransformer.uuidMarkerTypeHasNoLastAction)
                && marker.getValue() == true;
    }

    @SuppressWarnings("unused")
    private boolean filterAnnotation(Annotation annotation, IdentifiableEntity<?> removeEntity, IdentifiableEntity<?> stayEntity) {
        return false;
    }

    private TaxonBase<?>[] readTaxa() {
        TaxonBase<?> taxon1 = readTaxon("Taxon to be removed");
        TaxonBase<?> taxon2 = readTaxon("Taxon to stay");
        if (taxon1 == null || taxon2 == null){
            return null;
        }else{
            return new TaxonBase<?>[]{taxon1, taxon2};
        }
    }

    private TaxonBase<?> readTaxon(String message) {
        TaxonBase<?> taxon = null;
        boolean quit = false;
        while (taxon == null && quit == false){
            String strTaxon = CdmUtils.readInputLine(message + ": ");
            if (strTaxon.equalsIgnoreCase("q")){
                quit = true;
            }else{
                taxon = taxonByString(strTaxon);
            }
        }
        if (taxon == null){
            return null;
        }else if (taxon.isInstanceOf(Synonym.class)){
            return CdmBase.deproxy(taxon, Synonym.class);
        }else{
            return CdmBase.deproxy(taxon, Taxon.class);
        }
    }

    /**
     * Reads a taxon from database using it's id or uuid as String
     */
    private TaxonBase<?> taxonByString(String strTaxon) {
        TaxonBase<?> taxon = null;
        if (strTaxon.matches("\\d{1,10}")){
            taxon = app.getTaxonService().find(Integer.valueOf(strTaxon));
        }else if (strTaxon.matches(CdmRegEx.UUID_RE)){
            taxon = app.getTaxonService().find(UUID.fromString(strTaxon));
        }
        return taxon;
    }



    public static void main(String[] args) {
        PesiCommandLineMerge merger = new PesiCommandLineMerge();
        merger.invoke(pesiSource);
        System.exit(0);
    }
}
