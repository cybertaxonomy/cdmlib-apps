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
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.DbImportBase;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.agent.AgentBase;
import eu.etaxonomy.cdm.model.agent.Person;
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

    private static final String AUTHOR_KOMB_NAMESPACE = "author_komb";
    private static final String AUTHOR_BASI_NAMESPACE = "author_basi";

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
        super.doInvoke(state);
    }


    @Override
    public boolean doPartition(ResultSetPartitioner partitioner, RedListGefaesspflanzenImportState state) {
        ResultSet rs = partitioner.getResultSet();
        Map<String, AgentBase> teamsOrPersonToSave = new HashMap<String, AgentBase>();
        try {
            while (rs.next()){
                makeSingleAuthor(state, rs, teamsOrPersonToSave);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getAgentService().saveOrUpdate(teamsOrPersonToSave.values());
        //add partition to state map
        for (Entry<String, AgentBase> entry: teamsOrPersonToSave.entrySet()) {
            state.getAgentMap().put(entry.getKey(), entry.getValue().getUuid());
        }
        return true;
    }

    private void makeSingleAuthor(RedListGefaesspflanzenImportState state, ResultSet rs, Map<String, AgentBase> teamsOrPersonToSave)
            throws SQLException {
        long id = rs.getLong("NAMNR");
        String authorName = rs.getString("AUTOR");
        String authorBasiName = rs.getString("AUTOR_BASI");
        String authorKombName = rs.getString("AUTOR_KOMB");
        String zusatz = rs.getString("ZUSATZ");

        //check null values
        if(CdmUtils.isBlank(authorName) && CdmUtils.isBlank(authorBasiName) && CdmUtils.isBlank(authorKombName)){
            logger.error("No author found for NAMNR "+id);
            return;
        }
        Person authorKomb = null;
        Person authorBasi = null;
        Person author = null;
        authorKomb = importPerson(state, teamsOrPersonToSave, id, authorKombName, AUTHOR_KOMB_NAMESPACE);

        authorBasi = importPerson(state, teamsOrPersonToSave, id, authorBasiName, AUTHOR_BASI_NAMESPACE);

//        if(authorBasi!=null && authorKomb!=null){
//            Team team = Team.NewInstance();
//            team.addTeamMember(authorBasi);
//            team.addTeamMember(authorKomb);
//            teamsOrPersonToSave.add(team);
//            ImportHelper.setOriginalSource(team, state.getTransactionalSourceReference(), id, AUTHOR_NAMESPACE);
//        }
        if(authorBasi==null && authorKomb==null){
            logger.warn("Author not atomised in authorKomb and authorBasi");
            author = Person.NewTitledInstance(authorName);
            teamsOrPersonToSave.put(authorName, author);
            ImportHelper.setOriginalSource(author, state.getTransactionalSourceReference(), id, AUTHOR_KOMB_NAMESPACE);
        }
        //check author column consistency
        String authorCheckString = "";
        if(!CdmUtils.isBlank(authorKombName)){
            authorCheckString = "("+authorBasiName+")"+" "+authorKombName;
        }
        else{
            authorCheckString = authorBasiName;
        }
        boolean isAuthorStringCorrect = false;
        if(authorName.startsWith(authorCheckString)){
            isAuthorStringCorrect = true;
            if(!CdmUtils.isBlank(zusatz) && !authorName.contains(zusatz)){
                isAuthorStringCorrect = false;
            }
        }
        if(!isAuthorStringCorrect){
            String errorString = "ID: "+id+", Author string not consistent! Is \""+authorName+"\" Should start with \""+authorCheckString+"\"";
            if(!CdmUtils.isBlank(zusatz)){
                errorString +=" and contain \""+zusatz+"\"";
            }
            logger.error(errorString);
        }

    }

    private Person importPerson(RedListGefaesspflanzenImportState state, Map<String, AgentBase> teamsOrPersonToSave,
            long id, String agentName, String namespace) {
        Person person = null;
        if(!CdmUtils.isBlank(agentName) && !state.getAgentMap().containsKey(agentName)){
            person = Person.NewTitledInstance(agentName);
            teamsOrPersonToSave.put(agentName, person);
            state.getAgentMap().put(agentName, person.getUuid());
            ImportHelper.setOriginalSource(person, state.getTransactionalSourceReference(), id, namespace);
        }
        return person;
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
