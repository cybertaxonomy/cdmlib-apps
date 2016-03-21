// $Id$
/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.gefaesspflanzen;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.DbImportBase;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;

/**
 * @author pplitzner
 * @date Feb 29, 2016
 *
 */
@Component
@SuppressWarnings("serial")
public class RedListGefaesspflanzenImportAuthor extends DbImportBase<RedListGefaesspflanzenImportState, RedListGefaesspflanzenImportConfigurator> {

    private static final Logger logger = Logger.getLogger(RedListGefaesspflanzenImportAuthor.class);

    private static final String tableName = "Rote Liste Gefäßpflanzen";

    private static final String pluralString = "authors";

    public RedListGefaesspflanzenImportAuthor() {
        super(tableName, pluralString);
    }

    @Override
    protected String getIdQuery(RedListGefaesspflanzenImportState state) {
        return "SELECT NAMNR "
                + "FROM V_TAXATLAS_D20_EXPORT t "
                + " ORDER BY NAMNR";
    }

    @Override
    protected String getRecordQuery(RedListGefaesspflanzenImportConfigurator config) {
        String result = " SELECT * "
                + " FROM V_TAXATLAS_D20_EXPORT t "
                + " WHERE t.NAMNR IN (@IDSET)";
        result = result.replace("@IDSET", IPartitionedIO.ID_LIST_TOKEN);
        return result;
    }

    @Override
    protected void doInvoke(RedListGefaesspflanzenImportState state) {
        makeAuthors(state, "AUTOR_KOMB");
        makeAuthors(state, "AUTOR_BASI");
    }


    @Override
    public boolean doPartition(ResultSetPartitioner partitioner, RedListGefaesspflanzenImportState state) {
        return true;
    }

    private void makeAuthors(RedListGefaesspflanzenImportState state, String columnName) {

        String query = "select distinct "+columnName+" from V_TAXATLAS_D20_EXPORT t"
                + " WHERE TRIM(t."+columnName+") <>''";

        ResultSet rs = state.getConfig().getSource().getResultSet(query);

        try{
            while(rs.next()){
                String authorName = rs.getString(columnName);
                TeamOrPersonBase teamOrPerson = null;
                if(!CdmUtils.isBlank(authorName)){
                    makePerson(state, authorName);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void makePerson(RedListGefaesspflanzenImportState state, String authorName) {
        //check if misapplied name
        if(authorName.trim().equals(RedListUtil.AUCT)){
            return;
        }
        TeamOrPersonBase teamOrPerson;
        //check if there are ex authors
        if(authorName.contains(" ex ")){
            String[] split = authorName.split(" ex ");
            for (int i = 0; i < split.length; i++) {
                makePerson(state, split[i].trim());
            }
        }
        //check if it is a team
        if(authorName.contains("&")){
            teamOrPerson = Team.NewInstance();
            String[] split = authorName.split("&");
            for (int i = 0; i < split.length; i++) {
                ((Team) teamOrPerson).addTeamMember(Person.NewTitledInstance(split[i].trim()));
            }
        }
        else{
            teamOrPerson = Person.NewTitledInstance(authorName);
        }
        getAgentService().saveOrUpdate(teamOrPerson);
        state.getAuthorMap().put(authorName, teamOrPerson.getUuid());
    }


    @Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs,
            RedListGefaesspflanzenImportState state) {
        Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
        return result;
    }

    @Override
    protected boolean doCheck(RedListGefaesspflanzenImportState state) {
        return false;
    }

    @Override
    protected boolean isIgnore(RedListGefaesspflanzenImportState state) {
        return false;
    }

}
