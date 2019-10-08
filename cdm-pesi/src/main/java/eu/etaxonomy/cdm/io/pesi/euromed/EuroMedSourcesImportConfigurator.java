/**
* Copyright (C) 2019 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.euromed;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.ImportConfiguratorBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * @author a.mueller
 * @since 08.10.2019
 */
public class EuroMedSourcesImportConfigurator
            extends ImportConfiguratorBase<EuroMedSourcesImportState, ICdmDataSource>{

    private static final long serialVersionUID = 2306216658307408917L;

    public static EuroMedSourcesImportConfigurator NewInstance(ICdmDataSource cdmDB) {
        return new EuroMedSourcesImportConfigurator(cdmDB);
    }

    protected EuroMedSourcesImportConfigurator(ICdmDataSource cdmDB) {
        super(null);
        setDestination(cdmDB);
    }

    @SuppressWarnings("unchecked")
    @Override
    public EuroMedSourcesImportState getNewState() {
        return new EuroMedSourcesImportState(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void makeIoClassList() {
        ioClassList = new Class[]{
                EuroMedSourcesImport.class
        };
    }

    Reference sourceRef;
    @Override
    public Reference getSourceReference() {
        if (sourceRef == null){
            sourceRef = ReferenceFactory.newDatabase();
    //        result.setTitle(getSourceReferenceTitle());
            sourceRef.getTitleCache();
            sourceRef.setUuid(getSourceRefUuid());
        }
        return sourceRef;
    }

    @Override
    public boolean isValid(){
        //we should make this a normal script, not an Import (it is only an update, not an import)
        return true;
    }

}
