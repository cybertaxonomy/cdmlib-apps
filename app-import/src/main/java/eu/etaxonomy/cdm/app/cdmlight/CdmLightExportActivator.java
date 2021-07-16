/**
* Copyright (C) 2021 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.cdmlight;

import java.io.File;
import java.util.UUID;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.monitor.DefaultProgressMonitor;
import eu.etaxonomy.cdm.common.monitor.IProgressMonitor;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.filter.TaxonNodeFilter;
import eu.etaxonomy.cdm.io.cdmLight.CdmLightExportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultExport;
import eu.etaxonomy.cdm.io.common.ExportResult;

/**
 * TODO this is not ready yet as the result is not correctly written into a file
 * Probably the similar named activator in the caryophyllales package shows how
 * to do this.
 *
 * @author a.mueller
 * @since 12.07.2021
 */
public class CdmLightExportActivator {

    //database validation status (create, update, validate ...)
    private static final String fileDestination = "C:\\opt\\data\\tmp\\cdmlight.test";
//  private static final ICdmDataSource cdmSource = CdmDestinations.cdm_local_cichorieae();
  private static final ICdmDataSource cdmSource = CdmDestinations.cdm_local_cuba();
//  private static final ICdmDataSource cdmSource = CdmDestinations.cdm_production_cichorieae();
//    private static final ICdmDataSource cdmSource = CdmDestinations.cdm_production_cyprus();

    private static final UUID subtreeCactaceaeUuid = UUID.fromString("876aa727-dc51-4912-8b19-1368303318bd");
    private static final UUID subtreeCubaUuid = UUID.fromString("206d42e4-ac32-4f20-a093-14826014e667");

    private IProgressMonitor monitor = DefaultProgressMonitor.NewInstance();

//    private static DateTime dateTime = new DateTime();
//    private static String date = dateTime.getYear() + "-" + dateTime.getMonthOfYear() + "-" + dateTime.getDayOfMonth();

// ****************** ALL *****************************************

// ************************ NONE **************************************** //


    public ExportResult doExport(ICdmDataSource source){

        System.out.println("Start export to CDM light ("+ fileDestination + ") ...");

//        CdmUpdater.NewInstance().updateToCurrentVersion(source, monitor);

        //make file destination
        File destination = new File(fileDestination);

        CdmLightExportConfigurator config = CdmLightExportConfigurator.NewInstance(source, destination);
        TaxonNodeFilter taxonNodeFilter = TaxonNodeFilter.NewSubtreeInstance(subtreeCubaUuid);
        config.setTaxonNodeFilter(taxonNodeFilter);
        config.setProgressMonitor(DefaultProgressMonitor.NewInstance());
        CdmDefaultExport<CdmLightExportConfigurator> cdmLightExport = new CdmDefaultExport<>();

        // invoke export
        ExportResult result = cdmLightExport.invoke(config);
        System.out.println("End export to CDM light ("+ fileDestination + ")..." + "("+result.getState().toString()+")");
        return result;
    }


    public static void main(String[] args) {
        CdmLightExportActivator ex = new CdmLightExportActivator();
        ICdmDataSource source = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmSource;
        ex.doExport(source);
        System.exit(0);
    }
}