/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.common.tasks;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * Deduplicates certain classes.
 *
 * CAUTION: Deduplicating teams does not yet work correctly. Team members are often duplicated.
 *
 * @author a.mueller
 * @date 24.11.2017
 *
 */
public class Deduplicator {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(Deduplicator.class);

    //database validation status (create, update, validate ...)
//  static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;
//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_cichorieae();
//  static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_caryophyllales_spp();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_flora_cuba();
    //  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_caryo_spp();



   static boolean doPersons = true;
   static boolean doTeams = false;
   static boolean doReferences = false;


    private void doInvoke(ICdmDataSource destination){

        try {
            CdmApplicationController app = CdmApplicationController.NewInstance(destination, DbSchemaValidation.VALIDATE);

            if (doPersons){
                app.getAgentService().deduplicate(Person.class, null, null);
            }
            if (doTeams){
                app.getAgentService().deduplicate(Team.class, null, null);
            }
            if (doReferences){
                app.getReferenceService().deduplicate(Reference.class, null, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        ICdmDataSource destination = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;

        System.out.println("Start updating caches for "+ destination.getDatabase() + "...");
        Deduplicator me = new Deduplicator();
        me.doInvoke(destination);
        System.exit(0);

    }
}
