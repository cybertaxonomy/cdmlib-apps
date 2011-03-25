package eu.etaxonomy.xper;

import java.io.File;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import fr_jussieu_snv_lis.Xper;
import fr_jussieu_snv_lis.edition.XPDisplay;
import fr_jussieu_snv_lis.utils.Utils;

public class XperStart {
	
	
	
	public XperStart(){
		
		DbSchemaValidation dbSchemaValidation = DbSchemaValidation.VALIDATE;
		ICdmDataSource datasource = CdmDestinations.cdm_test_local_xper();

		final CdmApplicationController appCtr = CdmApplicationController.NewInstance(datasource, dbSchemaValidation);
		
		
		Thread t = new Thread() {
			public void run() {
				new Xper(appCtr);
			}
		};
		t.start();
	}
	
	public static void xperloadDataFromCdm(){
		// create a new empty base and load data from CDM
		if(Xper.getCdmApplicationController() != null){
			// create a new base
			Xper.getMainframe().newBase("baseTest");
			// specify that the current base is not new (needed to be able to add images)
			Utils.isNewBase = false;
			// use the current directory as working directory for Xper2
			XPDisplay.getControler().getBase().setPathName(System.getProperty("user.dir") + Utils.sep);
			// create a _thumbnail directory to store thumbnails
			new File(System.getProperty("user.dir") + Utils.sep + "images" + Utils.sep + "_thumbnails").mkdirs();
			// delete the variable create by default and update the frame
			XPDisplay.getControler().getBase().deleteVariable(XPDisplay.getControler().getBase().getVariableAt(0));
			XPDisplay.displayNbVariable();
			XPDisplay.getControler().displayJifVarTree();
			
			AdaptaterCdmXper adapterCdmXper = new AdaptaterCdmXper();
			Utils.cdmAdapter = adapterCdmXper;
			
			if (Utils.currentBase != null) {
//				adaptaterCdmXper.createWorkingSet();
				adapterCdmXper.load();

				XPDisplay.displayNbVariable();
				XPDisplay.getControler().displayJifVarTree();
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("cdm start");
		new XperStart();
		System.out.println("cdm started :::");
		
		while(!Utils.xperReady){
			//TODO wait
		}
		System.out.println("start load data");
		// display a loading gif
		Utils.displayLoadingGif(true);
		// load the data from CDM
		
		xperloadDataFromCdm();
		// undisplay a loading gif
		Utils.displayLoadingGif(false);
		System.out.println("stop load data");
		System.out.println("start generate thumbnails");
		// generate all thumbnails (a loading gif is automatically displayed
		Utils.generateThumbnailsFromImageURL(XPDisplay.getControler().getBase().getAllResources());
		System.out.println("stop generate thumbnails");
		
	}

}
