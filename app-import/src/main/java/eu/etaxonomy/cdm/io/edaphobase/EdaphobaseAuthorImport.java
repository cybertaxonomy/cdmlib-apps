// $Id$
/**
* Copyright (C) 2015 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.edaphobase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;

/**
 * @author a.mueller
 * @date 21.12.2015
 *
 */
@Component
public class EdaphobaseAuthorImport extends EdaphobaseImportBase {
    private static final long serialVersionUID = -9138378836474086070L;

    private static final Logger logger = Logger.getLogger(EdaphobaseAuthorImport.class);

    private static final String tableName = "tax_taxon";

    private static final String pluralString = "authors";


    /**
     * @param tableName
     * @param pluralString
     */
    public EdaphobaseAuthorImport() {
        super(tableName, pluralString);
    }

    @Override
    protected String getIdQuery(EdaphobaseImportState state) {
      //not relevant here
        return null;
    }

    @Override
    protected String getRecordQuery(EdaphobaseImportConfigurator config) {
      //not relevant here
        return null;
    }

    @Override
    protected void doInvoke(EdaphobaseImportState state) {
        makeAuthors(state);
    }


    @Override
    public boolean doPartition(ResultSetPartitioner partitioner, EdaphobaseImportState state) {
        //not relevant here
        return true;
    }

    @Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs,
            EdaphobaseImportState state) {
        Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
        return result;
    }


    /**
     * @param state
     */
    private void makeAuthors(EdaphobaseImportState state) {
        Map<String, Person> personMap = new HashMap<>();
        Map<String, TeamOrPersonBase<?>> authorMap = new HashMap<>();

        String query = "SELECT DISTINCT t.tax_author_name "
                + " FROM tax_taxon t"
                + " WHERE TRIM(t.tax_author_name) <>'' AND t.tax_author_name IS NOT NULL";

        ResultSet rs = state.getConfig().getSource().getResultSet(query);
        try {
            while(rs.next()){
                List<Person> singlePersons = new ArrayList<>();
                String authorStr = rs.getString("tax_author_name");

                String[] splits = authorStr.split("\\s*&\\s*");
                for (String split : splits){
                    String[] commaSplits = split.split("\\s*,\\s*");
                    for (String commaSplit : commaSplits){
                        Person person = personMap.get(commaSplit);
                        if (person == null){
                            person = Person.NewTitledInstance(commaSplit);
                            personMap.put(commaSplit, person);
                        }
                        singlePersons.add(person);
                    }
                }
                if (singlePersons.size() > 1){
                    Team team = Team.NewInstance();
                    for (Person person: singlePersons){
                        team.addTeamMember(person);
                    }
                    authorMap.put(authorStr, team);
                }else{
                    authorMap.put(authorStr, singlePersons.get(0));
                }
            }
            Map<String, UUID> authorUuidMap = new HashMap<>();
            for (String key : authorMap.keySet() ){
                TeamOrPersonBase<?> author = authorMap.get(key);
                authorUuidMap.put(key, author.getUuid());
            }
            state.setAuthorMap(authorUuidMap);
            logger.info("Save " + authorMap.keySet().size() + " authors ...");
            getAgentService().saveOrUpdate((Collection)authorMap.values());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected boolean doCheck(EdaphobaseImportState state) {
        return false;
    }

    @Override
    protected boolean isIgnore(EdaphobaseImportState state) {
        return ! state.getConfig().isDoTaxa();
    }

}
