/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.common;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.service.dto.DistributionInfoDTO;
import eu.etaxonomy.cdm.api.service.dto.DistributionInfoDTO.InfoPart;
import eu.etaxonomy.cdm.api.util.DistributionOrder;
import eu.etaxonomy.cdm.api.util.DistributionTree;
import eu.etaxonomy.cdm.common.TreeNode;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.ext.geo.CondensedDistributionConfiguration;
import eu.etaxonomy.cdm.ext.geo.IEditGeoService;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaLevel;

/**
 * @author a.babadshanjan
 * @since 12.05.2009
 */
public class TestActivator {

	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(TestActivator.class);

//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql();
//	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_euroMed();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_euromed();

	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;
	static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.VALIDATE;

	private void invoke(){
       ICdmDataSource destination = cdmDestination;

        CdmIoApplicationController app;

//	      applicationEventMulticaster
//	      app = CdmIoApplicationController.NewInstance(destination, dbSchemaValidation);

//        IProgressMonitor progressMonitor = DefaultProgressMonitor.NewInstance();
//        String resourcePath= "/eu/etaxonomy/cdm/appimportTestApplicationContext.xml";
//        ClassPathResource resource = new ClassPathResource(resourcePath);
//	      ApplicationListener<?> listener = new AppImportApplicationListener();
//	      List<ApplicationListener> listeners = new ArrayList<>();
//	      listeners.add(listener);
//	      app = CdmApplicationController.NewInstance(resource, destination, dbSchemaValidation, false, progressMonitor, listeners);
//	        app = CdmApplicationController.NewInstance(resource, destination, dbSchemaValidation, false, progressMonitor);
	        app = CdmIoApplicationController.NewInstance(destination, dbSchemaValidation);


	        doDistributionInfo(app);

//	        URI uri = URI.create("file:///C:/localCopy/Data/xper/Cichorieae-DA2.sdd.xml");
//	        SDDImportConfigurator configurator = SDDImportConfigurator.NewInstance(uri, destination);
//	        CdmDefaultImport<SDDImportConfigurator> myImport = new CdmDefaultImport<>();
//
//	        myImport.setCdmAppController(app);
//
//	        ImportResult result = myImport.invoke(configurator);
//	        System.out.println(result.toString());

	}


    /**
     * @param app
     */
    private void doDistributionInfo(CdmIoApplicationController app) {
        IEditGeoService geoService = (IEditGeoService)app.getBean("editGeoService");

        Set<InfoPart> partSet = new HashSet<>(Arrays.asList(InfoPart.values()));
        EnumSet<InfoPart> parts = EnumSet.copyOf(partSet);
        CondensedDistributionConfiguration config = CondensedDistributionConfiguration.NewDefaultInstance();
        DistributionOrder distributionOrder = DistributionOrder.LABEL;
        List<String> propertyPaths = new ArrayList<>();
        boolean statusOrderPreference = true;
        boolean subAreaPreference = true;
        Set<MarkerType> hiddenMarker = new HashSet<MarkerType>((List)app.getTermService().find(new HashSet<>(Arrays.asList(
                new UUID[]{UUID.fromString("0318c67d-e323-4e9c-bffb-bc0c7f8f9f40"),UUID.fromString("e2b42891-aa85-4a09-981b-b7d8f5749c54")}
                ))));
        boolean fallbackAsParent = true;
        Map<PresenceAbsenceTerm, Color> presenceAbsenceTermColors = null;
        Set<NamedAreaLevel> omitLevels = new HashSet<>();
        List<Language> languages = new ArrayList<>();

        UUID taxonUuid = UUID.fromString("70b157e2-b96d-44e8-8430-c4c2b6353244");
        DistributionInfoDTO dto = geoService.composeDistributionInfoFor(parts, taxonUuid, subAreaPreference,
                statusOrderPreference, hiddenMarker, fallbackAsParent, omitLevels,
                presenceAbsenceTermColors, languages, propertyPaths, config, distributionOrder, true);

         DistributionTree tree = dto.getTree();
         List<TreeNode<Set<Distribution>, NamedArea>> list = tree.toList();
         System.out.println(list);
    }


    public static void main(String[] args) {
        new TestActivator().invoke();
        System.exit(0);
    }
}