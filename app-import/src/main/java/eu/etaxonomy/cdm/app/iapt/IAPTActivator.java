/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.iapt;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import eu.etaxonomy.cdm.common.monitor.DefaultProgressMonitor;
import eu.etaxonomy.cdm.database.DatabaseTypeEnum;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.iapt.IAPTImportConfigurator;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;


/**
 * @author a.kohlbecker
 * @date Jul 26, 2016
 *
 */
public class IAPTActivator {
    private static final Logger logger = Logger.getLogger(IAPTActivator.class);

    public static final String DATA_FILE_FULL = "Registration_DB_from_BGBM17.xls";
    public static final String DATA_FILE_0_100 = "iapt-100.xls";
    public static final String DATA_ENCODING_PROBLEMS = "encoding-problems.xls";
    public static final String DATA_IAPT_TYPES_100 = "iapt-types-100.xls";
    public static final String DATA_TYPE_LEG_100 = "iapt-type-leg-100.xls";
    public static final String DATA_NAME_TYPES = "iapt-name-types.xls";
    public static final String DATA_SINGLE = "single.xls";
    public static final String DATA_FILE = DATA_FILE_FULL;

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;

    static ICdmDataSource cdmDestination = null;
    static {
        DatabaseTypeEnum dbType = DatabaseTypeEnum.MySQL;
        String cdmServer = "127.0.0.1";
        String cdmDB = "cdm_algea_registry";
        String cdmUserName = "edit";
        cdmDestination =  CdmDestinations.makeDestination(dbType, cdmServer, cdmDB, -1, cdmUserName, null);
        // cdmDestination = CdmDestinations.localH2();
    }

    static boolean invers = true;

    static boolean include = !invers;

    //classification
    static final UUID classificationUuid = UUID.fromString("8c51efb4-3d67-4bea-8f87-4bc1cba1310d");
    private static final String classificationName = "IAPT";
    static final String sourceReferenceTitle = "IAPT Import";

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;


    private void doImport(ICdmDataSource cdmDestination){

        URI source = fileURI();

        Reference secRef = ReferenceFactory.newDatabase();
        secRef.setTitle("IAPT");

        Reference sourceRef = ReferenceFactory.newDatabase();
        sourceRef.setTitle("IAPT Registration of Plant Names Database");
        sourceRef.setDatePublished(TimePeriod.NewInstance(1998, 2016));
        sourceRef.setOrganization("International Association for Plant Taxonomy");
        try {
            sourceRef.setUri(new URI("http://archive.bgbm.org/scripts/ASP/registration/regSearch.asp"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }


        //make Source
        IAPTImportConfigurator config= IAPTImportConfigurator.NewInstance(source, cdmDestination);
        config.setClassificationUuid(classificationUuid);
        config.setClassificationName(classificationName);
        config.setCheck(check);
        config.setDbSchemaValidation(hbm2dll);
        config.setSourceReference(sourceRef);
        config.setSecReference(secRef);
        config.setProgressMonitor(DefaultProgressMonitor.NewInstance());
        // config.setBatchSize(100); // causes Error during managed flush [Don't change the reference to a collection with delete-orphan enabled : eu.etaxonomy.cdm.model.taxon.TaxonNode.annotations]

        CdmDefaultImport<IAPTImportConfigurator> myImport = new CdmDefaultImport<>();

        doSingleSource(fileURI(), config, myImport);

        System.exit(0);

    }

    /**
     * @param source
     * @param config
     * @param myImport
     */
    private void doSingleSource(URI source, IAPTImportConfigurator config, CdmDefaultImport<IAPTImportConfigurator> myImport) {
        config.setSource(source);
        String fileName = source.toString();
        fileName = fileName.substring(fileName.lastIndexOf("/") + 1 );

        String message = "Start import from ("+ fileName + ") ...";
        System.out.println(message);
        logger.warn(message);
        config.setSourceReference(getSourceReference(fileName));
        myImport.invoke(config);

        System.out.println("End import from ("+ source.toString() + ")...");
    }

    private final Reference inRef = ReferenceFactory.newGeneric();
    private Reference getSourceReference(String string) {
        Reference result = ReferenceFactory.newGeneric();
        result.setTitleCache(string, true);
        result.setInReference(inRef);
        inRef.setTitleCache(sourceReferenceTitle, true);
        return result;
    }



    public static URI fileURI() {
        File f = new File(System.getProperty("user.home") + "/data/Projekte/Algea Name Registry/registry/sources/IAPT/" + DATA_FILE);
        return f.toURI();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        IAPTActivator me = new IAPTActivator();
        me.doImport(cdmDestination);
    }

}
