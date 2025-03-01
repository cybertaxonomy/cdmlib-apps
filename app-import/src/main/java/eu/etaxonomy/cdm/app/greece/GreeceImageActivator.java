/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.greece;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DatabaseTypeEnum;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.io.media.in.MediaExcelImportConfigurator;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;

/**
 * Import for new images for the Flora of Greece.
 *
 * https://dev.e-taxonomy.eu/redmine/issues/7075
 *
 * How to import:
 *
 *  <UL>
 *  <LI>In Excel file we should have 3 URI columns for large, medium and small.</LI>
 *    If they not exists create cells with something like "=WECHSELN(C2;"large";"medium")"  (German Excel version)
 *    Label these columns url_size1, url_size2 and url_size3</LI>
 *  <LI>If the medium and small versions of the images do not yet exist, create them in an according folder
 *    (you may need to ask for access to the server in Greece)</LI>
 *  <LI>Copy only the new images into a local folder (use date to
 *      determine new images if more images are in the original folder)</LI>
 *  <LI>Open IrfanView and open File->BatchConversion/Rename</LI>
 *  <LI>Use Advanced options for bulk resize and use the following parameters:</LI>
 *       <UL><LI> for small: -> Set one or both sides to width=210 (height=-empty-)</LI>
 *        <LI> for medium: -> Set short side to 350</LI>
 *        </UL>
 *  <LI>Select all images and move them to the "input files"</LI>
 *  <LI>Choose correct "Output folder for result files" (repeat for both sizes)</LI>
 *  <LI>Upload images in both folders to correct place in remote server</LI>
 *  <LI>Update the filename parameter in code below.</LI>
 *  <LI>Upload the Excel file to /BGBM-PESIHPC/Greece/images/  (see {@link #greekChecklist()}) below</LI>
 *  <LI>Run with localH2 to test if everything works, if yes
 *  <LI>Run
 *  </UL>
 *
 * @author a.mueller
 * @since 13.12.2016
 */
public class GreeceImageActivator {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger();

    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_greece_checklist();

    private static final UUID sourceUuid = UUID.fromString("a5f11fe9-7bd6-4dff-abb0-1b147ed49c34");
//    private static final UUID sourceUuid = UUID.fromString("74a805b0-17cb-4d8c-8a55-9414a7597e79");

//    private static final String fileName = "images-20-04-23_v2.xlsx";
    private static final String fileName = "images-08-10-23_Franzen.xlsx";


//    NOTE!!: Darauf achten, dass die Header case sensitiv sind und keine Leerzeichen am Ende sein sollten, trim funktioniert seltsamerweise nicht immer

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    private void doImport(ICdmDataSource cdmDestination){

        DbSchemaValidation schemaVal = cdmDestination.getDatabaseType() == DatabaseTypeEnum.H2 ? DbSchemaValidation.CREATE : DbSchemaValidation.VALIDATE;
        URI source = greekChecklist();  //just any
        //make Source
        MediaExcelImportConfigurator config = MediaExcelImportConfigurator.NewInstance(source, cdmDestination);
        config.setCheck(check);
        config.setDbSchemaValidation(schemaVal);
        config.setSourceReference(getSourceReference());
        config.setDescriptionLanguage(Language.uuidEnglish);
        config.setTitleLanguageUuid(Language.uuidEnglish);
        config.setNomenclaturalCode(NomenclaturalCode.ICNAFP);

        CdmDefaultImport<MediaExcelImportConfigurator> myImport = new CdmDefaultImport<>();
        ImportResult result = myImport.invoke(config);
        System.out.println(result.createReport());
    }

    private URI greekChecklist(){
        return URI.create("file:////BGBM-PESIHPC/Greece/images/" + fileName);
    }

    private Reference getSourceReference(){
        Reference result = ReferenceFactory.newDatabase();
        result.setTitle(fileName);
        result.setUuid(sourceUuid);

        return result;
    }

    public static void main(String[] args) {
        GreeceImageActivator me = new GreeceImageActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}