/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.greece;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.LanguageString;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.taxon.TaxonNodeStatus;

/**
 * @author a.mueller
 * @since 18.07.2020
 */
public class GreeceExcludedNoteDeleterActivator {

    private static final Logger logger = LogManager.getLogger();

	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_greece();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_greece_checklist();

	static boolean testOnly = false;

	private void deleteExcludedNote(ICdmDataSource cdmDestination){
        CdmApplicationController app = CdmIoApplicationController.NewInstance(cdmDestination, DbSchemaValidation.VALIDATE);
        TransactionStatus tx = app.startTransaction();

        List<TaxonNode> list = app.getTaxonNodeService().list(TaxonNode.class, null, null, null, null);
        for (TaxonNode taxonNode : list){
            if (!taxonNode.getStatusNote().isEmpty()){
                Collection<LanguageString> nodeNotes = taxonNode.getStatusNote().values();
                if (nodeNotes.size() > 1){
                    logger.warn("More than 1 language reprepresentation exists. Taxon not handled. NodeId: " + taxonNode.getId());
                }else{
                    String noteStr = nodeNotes.iterator().next().getText();
                    if (taxonNode.getStatus() != TaxonNodeStatus.EXCLUDED){
                        logger.warn("NoteStr exists but status was not 'excluded' but " + taxonNode.getStatus() + "; " + taxonNode.getTaxon().getName().getTitleCache() + "; NodeId: " + taxonNode.getId());
                         continue;
                    }
                    Set<TextData> descrNote = taxonNode.getTaxon().getDescriptionItems(Feature.NOTES(), TextData.class);
                    if (descrNote.size()>1){
                        logger.warn("More than 1 language reprepresentation exists. Taxon not handled. Taxon: " + taxonNode.getTaxon().getName().getTitleCache() + "; NodeId: " + taxonNode.getId());
                    }else if (descrNote.isEmpty()){
                        logger.warn("No description note found. Taxon: "+ taxonNode.getTaxon().getName().getTitleCache() + "; NodeId: " + taxonNode.getId());
                    }else{
                        TextData textNote = descrNote.iterator().next();
                        Map<Language, LanguageString> multiText = textNote.getMultilanguageText();
                        if (multiText == null ||multiText.isEmpty()){
                            logger.warn("MultiText is empty or null. NodeId: " + taxonNode.getId());
                        }else if (multiText.values().size() > 1){
                            logger.warn("More than 1 language exists for multitext. Taxon not handled. NodeId: " + taxonNode.getId());
                        }else{
                            String singleText = multiText.values().iterator().next().getText();
                            if (CdmUtils.nullSafeEqual(noteStr, singleText)){
                                TaxonDescription description = CdmBase.deproxy(textNote.getInDescription(), TaxonDescription.class);
                                description.removeElement(textNote);
                                if (description.getElements().isEmpty()){
                                    app.getDescriptionService().deleteDescription(description);
//                                    logger.warn("Removed description. " + taxonNode.getTaxon().getName().getTitleCache() + "; " + taxonNode.getId());
                                }else{
                                    logger.warn("Removed. " + taxonNode.getTaxon().getName().getTitleCache() + "; " + taxonNode.getId());
                                }
                            }else{
                                logger.warn("NoteStr and textNote exist but differ. NoteStr: " + noteStr + "; textNote: "+ textNote + "; Taxon: " + taxonNode.getTaxon().getName().getTitleCache() + "; TaxonNode id: " + taxonNode.getId());
                            }
                        }
                    }
                }
            }
        }

        if (testOnly){
            tx.setRollbackOnly();
        }
        app.commitTransaction(tx);

	}

	public static void main(String[] args) {
		GreeceExcludedNoteDeleterActivator me = new GreeceExcludedNoteDeleterActivator();
		try {
            me.deleteExcludedNote(cdmDestination);
        } catch (Exception e) {
            e.printStackTrace();
        }
		System.exit(0);
	}

}
