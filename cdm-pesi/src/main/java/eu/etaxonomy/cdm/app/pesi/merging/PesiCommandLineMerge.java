/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.pesi.merging;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.DeleteResult;
import eu.etaxonomy.cdm.api.service.config.TaxonDeletionConfigurator;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.CdmRegEx;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.io.common.mapping.out.DbLastActionMapper;
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

/**
 * @author a.mueller
 * @since 20.01.2020
 */
public class PesiCommandLineMerge extends PesiMergeBase {

    private static final Logger logger = Logger.getLogger(PesiCommandLineMerge.class);

    static final ICdmDataSource pesiSource = CdmDestinations.cdm_pesi2019_final();

    private CdmIoApplicationController app;

    private void invoke(ICdmDataSource source){
        app = CdmIoApplicationController.NewInstance(source, DbSchemaValidation.VALIDATE, false);

        while(booleanAnswer("New merge")){
            TransactionStatus tx = app.startTransaction();
            Taxon[] taxa = null;
            while (taxa == null) {
                taxa = readTaxa();
            }
            boolean commit = compareTaxa(taxa);
            if (commit){
                moveTaxonInformation(taxa[0],taxa[1]);
            }
            if (commit){
                app.commitTransaction(tx);
                if (booleanAnswer("Information moved. Delete old taxon")){
                    removeTaxon(taxa[0]);
                }
            }else{
                app.rollbackTransaction(tx);
            }
        }
    }

    private boolean compareTaxa(Taxon[] taxa) {
        Taxon removeTaxon = taxa[0];
        Taxon stayTaxon = taxa[1];
        String nc1 = removeTaxon.getName().getNameCache();
        String nc2 = stayTaxon.getName().getNameCache();

        String ft1 = removeTaxon.getName().getFullTitleCache();
        String ft2 = stayTaxon.getName().getFullTitleCache();
        System.out.println("Remove: " + ft1);
        System.out.println("Stay  : " + ft2);
        if (!nc1.equals(nc2)){
            return booleanAnswer("Name Cache differs!!! Do you really want to merge???");
        }else if (!ft1.equals(ft2)){
            return booleanAnswer("Full title cache differs! Do you really want to merge anyway");
        }else{
            return booleanAnswer("Same title. Merge");
        }
    }

    private void removeTaxon(Taxon taxon) {
        TaxonNode nodeToRemove = taxon.getTaxonNodes().iterator().next();
        TaxonDeletionConfigurator config = new TaxonDeletionConfigurator();
        DeleteResult result = app.getTaxonNodeService().deleteTaxonNode(nodeToRemove.getUuid(), config);
        if (!result.isOk()){
            System.out.println("Remove taxon was not successful.");
        }
    }

    private boolean booleanAnswer(String message) {
        String answer = CdmUtils.readInputLine(message + " (y/n)? ");
        return answer.equalsIgnoreCase("y");
    }

    private boolean moveTaxonInformation(Taxon removeTaxon, Taxon stayTaxon) {
        try {
            //mergeNames;
            TaxonName removeName = CdmBase.deproxy(removeTaxon.getName());
            TaxonName stayName = CdmBase.deproxy(stayTaxon.getName());
            mergeSources(removeName, stayName);
            mergeAnnotations(removeName, stayName);
            mergeMarkers(removeName, stayName);
            mergeExtensions(removeName, stayName);
            mergeCredits(removeName, stayName);
            mergeNameRelationships(removeName, stayName);
            mergeHybridRelationships(removeName, stayName);
            mergeNameDescriptions(removeName, stayName);

            //mergeTaxa;
            mergeSources(removeTaxon, stayTaxon);
            mergeAnnotations(removeTaxon, stayTaxon);
            mergeMarkers(removeTaxon, stayTaxon);
            mergeExtensions(removeTaxon, stayTaxon);
            mergeCredits(removeTaxon, stayTaxon);
            mergeDescriptions(removeTaxon, stayTaxon);
            mergeSynonyms(removeTaxon, stayTaxon);
            mergeChildren(removeTaxon, stayTaxon);
            mergeTaxonRelations(removeTaxon, stayTaxon);
            return booleanAnswer("Commit moved information");
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void mergeTaxonRelations(Taxon removeTaxon, Taxon stayTaxon) {
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

    private void mergeChildren(Taxon removeTaxon, Taxon stayTaxon) {
        TaxonNode removeNode = removeTaxon.getTaxonNodes().iterator().next();
        TaxonNode stayNode = stayTaxon.getTaxonNodes().iterator().next();
        Set<UUID> removeNodeChildrenUuids = removeNode.getChildNodes()
                .stream().map(tn->tn.getUuid()).collect(Collectors.toSet());

        if(!removeNodeChildrenUuids.isEmpty()){
            app.getTaxonNodeService().moveTaxonNodes(removeNodeChildrenUuids,
                    stayNode.getUuid(), 0, null);
            System.out.println("Child nodes moved: " + removeNodeChildrenUuids.size());

        }
    }

    private void mergeSynonyms(Taxon removeTaxon, Taxon stayTaxon) {
        for (Synonym synonym : removeTaxon.getSynonyms()){
            if (!synonymExists()){
                //TODO homotypical group
                stayTaxon.addSynonym(synonym, synonym.getType());
            }else{
                //TODO merge synonym names
            }
        }
    }

    private boolean synonymExists() {
        logger.warn("Synonym dulicate check - not yet implemented");
        return false;
    }

    private void mergeDescriptions(Taxon remove, Taxon stay) {
        //TODO handle duplicates for taxon descriptions
        for (TaxonDescription description: remove.getDescriptions()){
            System.out.println("Move taxon description: " + description.getTitleCache());
            stay.addDescription((TaxonDescription)description.clone());
        }
    }

    private void mergeCredits(IdentifiableEntity<?> removeEntity,
            IdentifiableEntity<?> stayEntity) throws CloneNotSupportedException {
        String className = removeEntity.getClass().getSimpleName();
        for (Credit credit: removeEntity.getCredits()){
            System.out.println("Move "+className+" credit: " + credit.toString());
            stayEntity.addCredit((Credit)credit.clone());
        }
    }

    private void mergeExtensions(IdentifiableEntity<?> removeEntity,
            IdentifiableEntity<?> stayEntity) throws CloneNotSupportedException {
        String className = removeEntity.getClass().getSimpleName();
        for (Extension extension: removeEntity.getExtensions()){
            System.out.println("Move "+className+" extension: " + extension.getType().getTitleCache() + ": " + extension.getValue());
            stayEntity.addExtension((Extension)extension.clone());
        }
    }

    private void mergeMarkers(IdentifiableEntity<?> removeEntity,
            IdentifiableEntity<?> stayEntity) throws CloneNotSupportedException {
        String className = removeEntity.getClass().getSimpleName();
        for (Marker marker: removeEntity.getMarkers()){
            if (!filterMarker(marker, removeEntity, stayEntity)){
                System.out.println("Move "+className+" marker: " + marker.getMarkerType().getTitleCache() + ": " + marker.getValue());
                stayEntity.addMarker((Marker)marker.clone());
            }
        }
    }

    private void mergeAnnotations(IdentifiableEntity<?> removeEntity,
            IdentifiableEntity<?> stayEntity) throws CloneNotSupportedException {
        String className = removeEntity.getClass().getSimpleName();
        for (Annotation annotation: removeEntity.getAnnotations()){
            if (!filterAnnotation(annotation, removeEntity, stayEntity)){
                System.out.println("Move "+className+" note: " + annotation.getAnnotationType().getTitleCache() + ": " + annotation.getText());
                handleRemoveAnnotation(annotation, removeEntity, stayEntity);
                stayEntity.addAnnotation((Annotation)annotation.clone());
            }
        }
    }

    private void mergeSources(IdentifiableEntity<?> removeEntity,
            IdentifiableEntity<?> stayEntity) throws CloneNotSupportedException {
        String className = removeEntity.getClass().getSimpleName();
        for (IdentifiableSource source: removeEntity.getSources()){
            System.out.println("Move "+className+" source: " + source.getType().getMessage() + ": " + source.getCitation().getTitleCache() + "; " + source.getIdInSource() + "/" + source.getIdNamespace());
            stayEntity.addSource((IdentifiableSource)source.clone());
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
        return annotation.getAnnotationType().getUuid().equals(DbLastActionMapper.uuidAnnotationTypeLastActionDate)
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
        };
    }

    private boolean isNoLastActionMarker(Marker marker) {
        return marker.getMarkerType().getUuid().equals(PesiTransformer.uuidMarkerTypeHasNoLastAction)
                && marker.getValue() == true;
    }

    @SuppressWarnings("unused")
    private boolean filterAnnotation(Annotation annotation, IdentifiableEntity<?> removeEntity, IdentifiableEntity<?> stayEntity) {
        return false;
    }

    private Taxon[] readTaxa() {

        try {
            Taxon taxon1 = readTaxon("Taxon to be removed");
            Taxon taxon2 = readTaxon("Taxon to stay");
            return new Taxon[]{taxon1, taxon2};
        } catch (Exception e) {
            System.out.println("Reading taxon not successful");
            return null;
        }
    }

    private Taxon readTaxon(String message) {
        String strTaxon = CdmUtils.readInputLine(message + ": ");
        TaxonBase<?> taxon;
        if (strTaxon.matches("\\d{1,10}")){
            taxon = app.getTaxonService().find(Integer.valueOf(strTaxon));
        }else if (strTaxon.matches(CdmRegEx.UUID_RE)){
            taxon = app.getTaxonService().find(UUID.fromString(strTaxon));
        }else{
            throw new IllegalArgumentException("Input not recognized as id or uuid.");
        }
        if (taxon == null){
            throw new IllegalArgumentException("Input was not a valid taxon id.");
        }else if (taxon.isInstanceOf(Synonym.class)){
            throw new IllegalArgumentException("Input was synonym but accepted taxon required.");
        }else{
            return CdmBase.deproxy(taxon, Taxon.class);
        }
    }



    public static void main(String[] args) {
        PesiCommandLineMerge merger = new PesiCommandLineMerge();
        merger.invoke(pesiSource);
        System.exit(0);
    }
}
