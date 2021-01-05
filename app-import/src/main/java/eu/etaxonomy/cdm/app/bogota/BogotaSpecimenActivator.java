/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.bogota;

import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.bogota.BogotaSpecimenImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.reference.IDatabase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * Activator for import of Bogota Checklist
 *
 * @author a.mueller
 * @since 21.04.2017
 *
 */
public class BogotaSpecimenActivator {
    private static final Logger logger = Logger.getLogger(BogotaSpecimenActivator.class);

//    static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_bogota();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_bogota();

    int minRow = 1;
    int maxRow = 1000000; //minRow + 11999;
    boolean onlyNonCdmTaxa = true;


    boolean dedupRefs = false;
    boolean dedupAuthors = false;

    //check - import
    static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;

    protected void doImport(ICdmDataSource cdmDestination){

        URI source = bogotaSpecimens();

        //make Source
        BogotaSpecimenImportConfigurator config= BogotaSpecimenImportConfigurator.NewInstance(source, cdmDestination);
        config.setNomenclaturalCode(NomenclaturalCode.ICNAFP);
        config.setCheck(check);
        config.setDbSchemaValidation(DbSchemaValidation.VALIDATE);

        config.setMinLineNumber(minRow);
        config.setMaxLineNumber(maxRow);
        config.setDeduplicateReferences(dedupRefs);
        config.setSecReference(getSecReference());
        config.setDeduplicateAuthors(dedupAuthors);
        config.setOnlyNonCdmTaxa(onlyNonCdmTaxa);

        config.setSource(source);
        String fileName = source.toString();
        fileName = fileName.substring(fileName.lastIndexOf("/") + 1 );

        String message = "Start import from ("+ fileName + ") ...";
        System.out.println(message);
        logger.warn(message);

        config.setSourceReference(getSourceReference());

        CdmDefaultImport<BogotaSpecimenImportConfigurator> myImport = new CdmDefaultImport<>();

        ImportResult result = myImport.invoke(config);
        System.out.println(result.createReport());

        System.out.println("End import from (" + source.toString() + ")...");

    }


    //bogotaChecklist
    public static URI bogotaSpecimens() {
        return URI.create("file:////BGBM-PESIHPC/Bogota/Flora_de_Bogota_Dataset_20170901_GB20171011_14607-entries-to-import_GB_20171016.xlsx");
    }


    private Reference getSourceReference() {
        Reference result = ReferenceFactory.newGeneric();
        result.setTitle("Flora_de_Bogota_Dataset_20170901_GB20171011_14607-entries-to-import_GB_20171016.xlsx");
        result.setUuid(UUID.fromString("05e8c346-4809-4323-a484-822c92ad033d"));
        return result;
    }

    private Reference getSecReference() {

        IDatabase result = ReferenceFactory.newDatabase();
//        result.setTitleCache("Herbario. 2017. Identificaciones de muestras de herbario en el banco de datos del Jardín Botánico Nacional José Celestino Mutis. Bogotá [exportados 18-sep-2017]", true);
        result.setTitle("Identificaciones de muestras de herbario en el banco de datos del Jardín Botánico Nacional José Celestino Mutis.");
        result.setPlacePublished("Bogotá");
        result.setDatePublished(TimePeriodParser.parseStringVerbatim("2017"));
        result.getDatePublished().setFreeText("2017 [exportados 18-sep-2017]");

        Team team = Team.NewTitledInstance("Herbario", null);
        result.setAuthorship(team);

        result.setUuid(UUID.fromString("2bbc08ba-20d2-46cf-bf57-88b90a717733"));
        return (Reference)result;
    }


    /**
     * @param args
     */
    public static void main(String[] args) {
        BogotaSpecimenActivator me = new BogotaSpecimenActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}
