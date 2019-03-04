/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.common;

import java.net.URI;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.common.monitor.DefaultProgressMonitor;
import eu.etaxonomy.cdm.common.monitor.IProgressMonitor;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.io.sdd.in.SDDImportConfigurator;
import eu.etaxonomy.cdm.model.description.CategoricalData;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.description.StateData;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.term.DefinedTerm;

/**
 * @author a.babadshanjan
 * @since 12.05.2009
 */
public class TestActivator {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(TestActivator.class);

//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql();
//	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_test1();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_edit_cichorieae_preview();



	static final int limitSave = 2000;

	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;
	static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.CREATE;
	static final NomenclaturalCode nomenclaturalCode  = NomenclaturalCode.ICNAFP;



	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    new TestActivator().invoke();
	    System.exit(0);
	}

	private void invoke(){
       ICdmDataSource destination = cdmDestination;

        CdmApplicationController app;

//	      applicationEventMulticaster
//	      app = CdmIoApplicationController.NewInstance(destination, dbSchemaValidation);

        IProgressMonitor progressMonitor = DefaultProgressMonitor.NewInstance();
        String resourcePath= "/eu/etaxonomy/cdm/appimportTestApplicationContext.xml";
        ClassPathResource resource = new ClassPathResource(resourcePath);
//	      ApplicationListener<?> listener = new AppImportApplicationListener();
//	      List<ApplicationListener> listeners = new ArrayList<>();
//	      listeners.add(listener);
//	      app = CdmApplicationController.NewInstance(resource, destination, dbSchemaValidation, false, progressMonitor, listeners);
//	        app = CdmApplicationController.NewInstance(resource, destination, dbSchemaValidation, false, progressMonitor);
	        app = CdmApplicationController.NewInstance(destination, dbSchemaValidation, false);

	        if (true){
	            return;
	        }

	        TransactionStatus tx = app.startTransaction();

	        State state = (State)app.getTermService().find(UUID.fromString("881b9c80-626d-47a6-b308-a63ee5f4178f"));
	        DefinedTerm modifier = (DefinedTerm)app.getTermService().find(UUID.fromString("efc38dad-205c-4028-ad9d-ae509a14b37a"));
	        CategoricalData cd = CategoricalData.NewInstance();
	        StateData stateData = StateData.NewInstance();
	        stateData.setState(state);
	        stateData.addModifier(modifier);

	        StateData stateData2 = StateData.NewInstance();
	        stateData2.setState(state);
	        stateData2.addModifier(modifier);

	        cd.addStateData(stateData2);

	        app.getDescriptionService().saveDescriptionElement(cd);
	        System.out.println("Saved");

	        app.commitTransaction(tx);

	        URI uri = URI.create("file:///C:/localCopy/Data/xper/Cichorieae-DA2.sdd.xml");
	        SDDImportConfigurator configurator = SDDImportConfigurator.NewInstance(uri, destination);
	        CdmDefaultImport<SDDImportConfigurator> myImport = new CdmDefaultImport<>();

	        myImport.setCdmAppController(app);

	        ImportResult result = myImport.invoke(configurator);
	        System.out.println(result.toString());

	        if (true){
	            return;
	        }


	}


}
