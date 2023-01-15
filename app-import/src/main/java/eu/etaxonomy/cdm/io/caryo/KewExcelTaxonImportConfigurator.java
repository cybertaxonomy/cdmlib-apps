/**
* Copyright (C) 2016 EDIT
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
import eu.etaxonomy.cdm.io.common.mapping.IInputTransformer;
import eu.etaxonomy.cdm.io.excel.common.ExcelImportConfiguratorBase;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * Configurator for Kew excel taxon import for Caryophyllaceae.
 *
 * @author a.mueller
 * @since 05.01.2022
 */
public class KewExcelTaxonImportConfigurator
        extends ExcelImportConfiguratorBase{

    private static final long serialVersionUID = -1819917445326422841L;

    private static IInputTransformer defaultTransformer = null;
    private Reference secReference;

    private UUID rootTaxonUuid;
    private UUID unplacedTaxonUuid;
    private UUID orphanedPlaceholderTaxonUuid;

    public static KewExcelTaxonImportConfigurator NewInstance(URI source, ICdmDataSource destination) {
        return new KewExcelTaxonImportConfigurator(source, destination);
    }

    private KewExcelTaxonImportConfigurator(URI source, ICdmDataSource destination) {
        super(source, destination, defaultTransformer);
        setNomenclaturalCode(NomenclaturalCode.ICNAFP);
        setSource(source);
        setDestination(destination);
     }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public SimpleExcelTaxonImportState getNewState() {
        return new SimpleExcelTaxonImportState<>(this);
    }

    @SuppressWarnings("unchecked")
	@Override
    protected void makeIoClassList() {
        ioClassList = new Class[]{
                KewExcelTaxonImport.class,
        };
    }

    public Reference getSecReference() {
        return secReference;
    }
    public void setSecReference(Reference secReference) {
        this.secReference = secReference;
    }

    public UUID getRootTaxonUuid() {
        return rootTaxonUuid;
    }
    public void setRootTaxonUuid(UUID rootTaxonUuid) {
        this.rootTaxonUuid = rootTaxonUuid;
    }

    public UUID getUnplacedTaxonUuid() {
        return unplacedTaxonUuid;
    }
    public void setUnplacedTaxonUuid(UUID unplacedTaxonUuid) {
        this.unplacedTaxonUuid = unplacedTaxonUuid;
    }

    public UUID getOrphanedPlaceholderTaxonUuid() {
        return orphanedPlaceholderTaxonUuid;
    }
    public void setOrphanedPlaceholderTaxonUuid(UUID orphanedPlaceholderTaxonUuid) {
        this.orphanedPlaceholderTaxonUuid = orphanedPlaceholderTaxonUuid;
    }
}