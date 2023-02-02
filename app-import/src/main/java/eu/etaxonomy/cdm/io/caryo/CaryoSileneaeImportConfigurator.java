/**
* Copyright (C) 2020 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.caryo;

import java.util.UUID;

import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;

/**
 * @author a.mueller
 * @since 17.02.2020
 */
public class CaryoSileneaeImportConfigurator extends ExcelImportConfiguratorBase{

    private static final long serialVersionUID = -3833210622605834032L;

    private boolean doDeduplicate = true;

    private UUID acceptedNodeUuid;
    private UUID unresolvedNodeUuid;

    private static IInputTransformer defaultTransformer = new CaryoAizoaceaeTransformer();

    public static CaryoSileneaeImportConfigurator NewInstance(URI source, ICdmDataSource destination) {
        return new CaryoSileneaeImportConfigurator(source, destination);
    }

    private CaryoSileneaeImportConfigurator(URI source, ICdmDataSource destination) {
        super(source, destination, defaultTransformer);
        setNomenclaturalCode(NomenclaturalCode.ICNAFP);
        setSource(source);
        setDestination(destination);
     }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ImportStateBase getNewState() {
        return new SimpleExcelTaxonImportState<>(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void makeIoClassList() {
        ioClassList = new Class[]{
                CaryoSileneaeNameImport.class
        };
    }

    public boolean isDoDeduplicate() {
        return this.doDeduplicate;
    }
    public void setDoDeduplicate(boolean doDeduplicate) {
        this.doDeduplicate = doDeduplicate;
    }

    public UUID getAcceptedNodeUuid() {
        return acceptedNodeUuid;
    }
    public void setAcceptedNodeUuid(UUID acceptedNodeUuid) {
        this.acceptedNodeUuid = acceptedNodeUuid;
    }

    public UUID getUnresolvedNodeUuid() {
        return unresolvedNodeUuid;
    }
    public void setUnresolvedNodeUuid(UUID unresolvedNodeUuid) {
        this.unresolvedNodeUuid = unresolvedNodeUuid;
    }

}
