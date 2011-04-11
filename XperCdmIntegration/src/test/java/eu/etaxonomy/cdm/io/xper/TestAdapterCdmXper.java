package eu.etaxonomy.cdm.io.xper;

import java.io.File;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.xper.AdaptaterCdmXper;
import fr_jussieu_snv_lis.Xper;
import fr_jussieu_snv_lis.edition.XPDisplay;
import fr_jussieu_snv_lis.utils.Utils;

public class TestAdapterCdmXper {
	
	AdaptaterCdmXper adapterCdmXper;
	
	/**
	 * 
	 */
	private void startApplications() {
		DbSchemaValidation dbSchemaValidation = DbSchemaValidation.VALIDATE;
		ICdmDataSource datasource = CdmDestinations.cdm_test_local_xper();
		System.out.println("cdm start");
		CdmApplicationController appCtr = CdmApplicationController.NewInstance(datasource, dbSchemaValidation);
		System.out.println("cdm started :::");
		
		adapterCdmXper = new AdaptaterCdmXper(appCtr);
		
		Thread t = new Thread() {
			public void run() {
				new Xper(adapterCdmXper);
			}
		};
		System.out.println("xper2 start");
		t.start();
		while(!Utils.xperReady){
			//TODO wait
		}
		System.out.println("xper2 started :::");
	}
	
	public void xperloadDataFromCdm(){
		System.out.println("start load data");
		// display a loading gif
		Utils.displayLoadingGif(true);
		
		
		// create a new empty base and load data from CDM
		if(Utils.cdmAdapter != null){
			// create a new base
			Xper.getMainframe().newBase("baseTest");
			// specify that the current base is not new (needed to be able to add images)
			Utils.isNewBase = false;
			// delete the variable create by default and update the frame
			XPDisplay.getControler().getBase().deleteVariable(XPDisplay.getControler().getBase().getVariableAt(0));
			XPDisplay.displayNbVariable();
			XPDisplay.getControler().displayJifVarTree();
			
			if (Utils.currentBase != null) {
//				adaptaterCdmXper.createWorkingSet();
				adapterCdmXper.load();

				XPDisplay.displayNbVariable();
				XPDisplay.getControler().displayJifVarTree();
			}
		}
		// undisplay a loading gif
		Utils.displayLoadingGif(false);
		System.out.println("data loaded :::");
	}

	/**
	 * 
	 */
	private void createThumbnailDirectory() {
		// create a _thumbnail directory to store thumbnails
		new File(System.getProperty("user.dir") + Utils.sep + "images" + Utils.sep + "_thumbnails").mkdirs();
	}
	
	/**
	 * 
	 */
	private void generateThumbnails() {
		System.out.println("start generate thumbnails");
		// generate all thumbnails (a loading gif is automatically displayed
		Utils.generateThumbnailsFromURLImage(XPDisplay.getControler().getBase().getAllResources());
		System.out.println("stop generate thumbnails");
	}
	

	private void startPartialCdm() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("start test");
		//start CDM and Xper
		TestAdapterCdmXper testAdapter = new TestAdapterCdmXper();
		testAdapter.startApplications();
		testAdapter.createThumbnailDirectory();
		if (args.length >= 1 && "-p".equals(args[0]) ){
			testAdapter.startPartialCdm();
		}else{
			// load the data from CDM
			testAdapter.xperloadDataFromCdm();
			// use the current directory as working directory for Xper2
			XPDisplay.getControler().getBase().setPathName(System.getProperty("user.dir") + Utils.sep);
			
			testAdapter.generateThumbnails();
		}


		
	}




}
