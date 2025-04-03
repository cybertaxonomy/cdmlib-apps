/**
* Copyright (C) 2025 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.common.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.service.UpdateResult;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.model.agent.Team;

/**
 * @author muellera
 * @since 18.02.2025
 */
public class SingleMemberTeamConverter {

    private static final Logger logger = LogManager.getLogger();

//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_local_salvador();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_phycobank();

    private void doInvoke(ICdmDataSource destination) {
        try {
            CdmApplicationController app = CdmApplicationController.NewInstance(destination, DbSchemaValidation.VALIDATE);

            List<String> propPath = Arrays.asList(new String[]{"teamMembers"});
            List<Team> teamList = app.getAgentService().list(Team.class, null, null, null, propPath);

            List<UUID> uuidList = new ArrayList<>();
            for (Team team : teamList){
                if  (team.getTeamMembers().size() == 1 && team.isHasMoreMembers() == false) {
                    if (!team.isProtectedTitleCache() && !team.isProtectedNomenclaturalTitleCache() && !team.isProtectedCollectorTitleCache()) {
                        uuidList.add(team.getUuid());
                    } else {
                        logger.warn("Team is protected: " + team.getId() + "; " + team.getTitleCache());
                    }
                }
            }

            for (UUID uuid: uuidList) {
                UpdateResult updateResult = app.getAgentService().convertTeam2Person(uuid);
                Collection<Exception> ex = updateResult.getExceptions();
                if (!ex.isEmpty()){
                    logger.warn("There are exceptions: " +uuid );
                }
//                if (updateResult.get

            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        ICdmDataSource destination = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;

        System.out.println("Start updating caches for "+ destination.getDatabase() + "...");
        SingleMemberTeamConverter me = new SingleMemberTeamConverter();
        me.doInvoke(destination);
        System.exit(0);

    }

}