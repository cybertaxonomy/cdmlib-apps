/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.euromed;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.common.CdmImportBase;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @since 08.10.2019
 */
@Component
public class EuroMedSourcesImport
        extends CdmImportBase<EuroMedSourcesImportConfigurator, EuroMedSourcesImportState>{

    private static final Logger logger = Logger.getLogger(EuroMedSourcesImportConfigurator.class);
    private static final long serialVersionUID = -8740059585975302440L;

    @Override
    protected void doInvoke(EuroMedSourcesImportState state) {
        List<String> propPath = Arrays.asList(new String[]{"sources"});
        TransactionStatus tx = startTransaction();
        Reference sourceRef = getSourceRef(state);
        List<Reference> references = getReferenceService().list(null, null, null, null, propPath);
        for (Reference reference : references){
            reference.addImportSource(String.valueOf(reference.getId()),
                    Reference.class.getSimpleName(), sourceRef, null);
        }
        int count = references.size();
        references = null;
        commitTransaction(tx);
        logger.info(count + " references imported");
        //taxa
        tx = startTransaction();
        sourceRef = getSourceRef(state);
        List<TaxonBase<?>> taxa = getTaxonService().list(null, null, null, null, propPath);
        for (TaxonBase<?> taxon : taxa){
            taxon.addImportSource(String.valueOf(taxon.getId()),
                    TaxonBase.class.getSimpleName(), sourceRef, null);
        }
        count = taxa.size();
        taxa = null;
        commitTransaction(tx);
        logger.info(count + " taxa imported");

        //names
        tx = startTransaction();
        sourceRef = getSourceRef(state);
        List<TaxonName> names = getNameService().list(null, null, null, null, propPath);
        for (TaxonName name : names){
            name.addImportSource(String.valueOf(name.getId()),
                    TaxonName.class.getSimpleName(), sourceRef, null);
        }
        count = names.size();
        names = null;
        commitTransaction(tx);
        logger.info(count + " names imported");

    }

    private Reference getSourceRef(EuroMedSourcesImportState state) {
        UUID uuid = state.getConfig().getSourceRefUuid();
        Reference ref = getReferenceService().find(uuid);
        if (ref == null){
            ref = state.getConfig().getSourceReference();
            getReferenceService().save(ref);
        }
        return ref;
    }

    @Override
    protected boolean doCheck(EuroMedSourcesImportState state) {
        return true;
    }

    @Override
    protected boolean isIgnore(EuroMedSourcesImportState state) {
        return false;
    }
}
