/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.fauEu2Cdm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import eu.etaxonomy.cdm.common.monitor.IProgressMonitor;
import eu.etaxonomy.cdm.io.common.TaxonNodeOutStreamPartitioner;
import eu.etaxonomy.cdm.io.common.XmlExportState;
import eu.etaxonomy.cdm.model.common.AnnotatableEntity;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.VersionableEntity;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;

/**
 * @author a.mueller
 * @since 17.08.2019
 */
public class FauEu2CdmImport
        extends FauEu2CdmImportBase {

    private static final long serialVersionUID = -2111102574346601573L;

    @Override
    protected boolean doCheck(FauEu2CdmImportState state) {
        return false;
    }

    @Override
    protected void doInvoke(FauEu2CdmImportState state) {
        IProgressMonitor monitor = state.getConfig().getProgressMonitor();
        FauEu2CdmImportConfigurator config = state.getConfig();
        if (config.getTaxonNodeFilter().getClassificationFilter() != null
                && !config.getTaxonNodeFilter().getClassificationFilter().isEmpty()) {
            Classification classification = getClassificationService()
                    .load(config.getTaxonNodeFilter().getClassificationFilter().get(0).getUuid());
            state.setRootId(classification.getRootNode().getUuid());

        } else if (config.getTaxonNodeFilter().getSubtreeFilter() != null
                && !config.getTaxonNodeFilter().getSubtreeFilter().isEmpty()) {
            state.setRootId(config.getTaxonNodeFilter().getSubtreeFilter().get(0).getUuid());
        }
        @SuppressWarnings("unchecked")
        TaxonNodeOutStreamPartitioner<XmlExportState> partitioner = TaxonNodeOutStreamPartitioner.NewInstance(this,
                state, state.getConfig().getTaxonNodeFilter(), 100, monitor, null);

        monitor.subTask("Start partitioning");

        TaxonNode node = partitioner.next();
        while (node != null) {
            handleTaxonNode(state, node);
            node = partitioner.next();
        }
    }

    /**
     * @param state
     * @param node
     */
    private void handleTaxonNode(FauEu2CdmImportState state, TaxonNode node) {
//        Integer id = state.getTarget(createdBy.getUuid(), createdBy.getClass());
        handleAnnotatableEntity(node);


    }

    private void handleAnnotatableEntity(AnnotatableEntity entity) {
        handleVersionableEntity(entity);
        for (Annotation a : getTargetCollection(entity.getAnnotations())){
            entity.addAnnotation(a);
        }
        for (Marker a : getTargetCollection(entity.getMarkers())){
            entity.addMarker(a);
        }
//        entity.setUpdatedBy(getTarget(entity.getUpdatedBy()));
    }

    /**
     * @param annotations
     * @return
     */
    private <T extends Collection<S>, S extends CdmBase> List<S> getTargetCollection(T sourceCollection) {
        List<S> result = new ArrayList<>();
        for (S entity : sourceCollection){
            S target = getTarget(entity);
            result.add(target);
        }
        return result;
    }

    private void handleVersionableEntity(VersionableEntity entity) {
        handleCdmBase(entity);
        entity.setUpdatedBy(getTarget(entity.getUpdatedBy()));
    }

    private void handleCdmBase(CdmBase cdmBase) {
        cdmBase.setCreatedBy(getTarget(cdmBase.getCreatedBy()));
    }

    //TODO this should be cached for partition
    private <T extends CdmBase> T getTarget(T source) {
        Class<T> clazz = (Class<T>)source.getClass();
        T result = getCommonService().find(clazz, source.getUuid());
        if (result == null){
            //TODO recursive
            //TODO session evict
            result = CdmBase.deproxy(source);
            result.setId(0);
        }
        return result;
    }

    @Override
    protected boolean isIgnore(FauEu2CdmImportState state) {
        return false;
    }

}
