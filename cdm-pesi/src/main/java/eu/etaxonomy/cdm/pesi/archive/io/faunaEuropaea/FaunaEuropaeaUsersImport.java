/**
 * Copyright (C) 2007 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */
package eu.etaxonomy.cdm.pesi.archive.io.faunaEuropaea;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.io.common.IImportConfigurator;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.agent.AgentBase;
import eu.etaxonomy.cdm.model.agent.Contact;
import eu.etaxonomy.cdm.model.agent.Institution;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.permission.User;
import eu.etaxonomy.cdm.model.reference.OriginalSourceBase;


/**
 * @author a.babadshanjan
 * @since 23.08.2010
 */
@Component
public class FaunaEuropaeaUsersImport extends FaunaEuropaeaImportBase {

    private static final long serialVersionUID = 2307694402632743697L;
    private static Logger logger = LogManager.getLogger();

	/* Interval for progress info message when retrieving taxa */
	private final int modCount = 10000;

	 protected DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();

	@Override
	protected boolean doCheck(FaunaEuropaeaImportState state) {
		boolean result = true;
		FaunaEuropaeaImportConfigurator fauEuConfig = state.getConfig();
		logger.warn("Checking for References not yet fully implemented");
		result &= checkReferenceStatus(fauEuConfig);

		return result;
	}

	private boolean checkReferenceStatus(FaunaEuropaeaImportConfigurator fauEuConfig) {
		boolean result = true;
//		try {
		Source source = fauEuConfig.getSource();
		String sqlStr = "";
//		ResultSet rs = source.getResultSet(sqlStr);
		return result;
//		} catch (SQLException e) {
//		e.printStackTrace();
//		return false;
//		}
	}

	@Override
	protected void doInvoke(FaunaEuropaeaImportState state) {
		/*
		logger.warn("Start User doInvoke");
		ProfilerController.memorySnapshot();
		*/

		if (!state.getConfig().isDoAuthors()){
			return;
		}
		TransactionStatus txStatus = null;
		Map<String, Person> persons = null;
		Map<String, User> users= null;

		Map<Integer, UUID> userUuids = new HashMap<Integer, UUID>();
		Map<Integer, Institution> institiutions= new HashMap<Integer, Institution>();
		Collection<Institution> institutionsToSave = new HashSet<Institution>();
		int limit = state.getConfig().getLimitSave();
		//this.authenticate("admin", "00000");

		FaunaEuropaeaImportConfigurator fauEuConfig = state.getConfig();
		Source source = fauEuConfig.getSource();

		String namespace = "User";
		int i = 0;

		String selectCountUsers =
			" SELECT count(*) FROM Users";

		String selectColumnsUsers =
			" SELECT u.usr_id as userId, u.usr_title as title, u.usr_firstname as firstname, u.usr_lastname as lastname, u.usr_createdat as created, u.usr_password as password, u.usr_cou_id as country, u.usr_email as mail, "
			+ "u.usr_homepage as homepage, u.usr_description as description, u.usr_active as active, o.org_name as organisationName, o.org_homepage as organisationHomepage, o.org_id as institutionId FROM Users u LEFT JOIN Organisation o ON o.org_id= u.usr_org_id";

		int count;
		if(logger.isInfoEnabled()) { logger.info("Start making References (Users)..."); }

		try {
			ResultSet rsUser = source.getResultSet(selectCountUsers);
			rsUser.next();
			count = rsUser.getInt(1);

			rsUser= source.getResultSet(selectColumnsUsers);

	        if (logger.isInfoEnabled()) {
	        	logger.info("Get all References...");
				logger.info("Number of rows: " + count);
				logger.info("Count Query: " + selectCountUsers);
				logger.info("Select Query: " + selectColumnsUsers);
			}

	        while (rsUser.next()){//usr_country_id, usr_org_id, usr_email, usr_homepage, usr_groupname, usr_groupicon, usr_groupnote, usr_description, usr_active)
	        	int refId = rsUser.getInt("userId");
				String userTitle = rsUser.getString("title");
				String userFirstname = rsUser.getString("firstname");
				String userLastname = rsUser.getString("lastname");
				String createdDate = rsUser.getString("created");
				String userPwd = rsUser.getString("password");
				String userCountry = rsUser.getString("country");

				String userMail = rsUser.getString("mail");
				String userHomepage = rsUser.getString("homepage");
				//String userGroupName = rsUser.getString("u.usr_groupname");
				String userDescription = rsUser.getString("description");
				int userActive = rsUser.getInt("active");
				String orgName = rsUser.getString("organisationName");
				String orgHomepage = rsUser.getString("organisationHomepage");
				int institutionId = rsUser.getInt("institutionId");

				// build person
				String userPerson = "";
				if (userTitle != null) {
					userPerson = userTitle;
					if (! userTitle.endsWith(".")) {
						userPerson += ".";
					}
				}
				userPerson += userTitle == null ? NullToEmpty(userFirstname) : " " + NullToEmpty(userFirstname);
				if ((userTitle != null || userFirstname != null) && userLastname != null) {
					userPerson += " " + userLastname;
				}
				this.authenticate("admin", "00000");
				// build year
				String year = null;
				if (createdDate != null) {
					year = createdDate.substring(0, createdDate.indexOf("-"));
				}

				if ((i++ % limit) == 0) {

					txStatus = startTransaction();
					persons= new HashMap<String,Person>(limit);
					users = new HashMap<String,User>(limit);


					if(logger.isInfoEnabled()) {
						logger.info("i = " + i + " - User import transaction started");
					}
				}

				Person person = null;
				User user = null;

				person= Person.NewTitledInstance(userPerson);

				person.addEmailAddress(userMail);
				try{
				    if (!StringUtils.isBlank(userHomepage)){
				        person.addUrl(URI.create(userHomepage));
				    }
				}catch(IllegalArgumentException e){
				    logger.debug(e.getMessage());
				}
				if (institutionId != 1){//1 = private
				    Institution institution ;
				    if (!institiutions.containsKey(institutionId)){
				        institution = Institution.NewInstance();
				        institution.setName(orgName);
				        Contact contact = Contact.NewInstance();
	                    try{
	                        if (!StringUtils.isBlank(orgHomepage)){
	                            contact.addUrl(URI.create(orgHomepage));
	                        }
	                    }catch(IllegalArgumentException e){
	                        logger.debug(e.getMessage());
	                    }
	                    institution.setContact(contact);
	                    institutionsToSave.add(institution);
				    } else {
				        institution = institiutions.get(institutionId);
				    }


    				person.addInstitutionalMembership(institution, null, null, null);
				}
				user = User.NewInstance(userPerson, userPwd);
				user.setPerson(person);
				if (userActive == FaunaEuropaeaTransformer.U_ACTIVE){
				    user.setAccountNonLocked(false);
				} else{
				    user.setAccountNonLocked(true);
				}

				if (!persons.containsKey(userPerson)) {
					if (userPerson == null) {
						logger.warn("User is null");
					}

					persons.put(userPerson, person);
					if (logger.isTraceEnabled()) {
						logger.trace("Stored user (" + userPerson + ")");
					}
				//}

				} else {
					person = persons.get(userPerson);
					if (logger.isDebugEnabled()) {
						logger.debug("Not imported user with duplicated ref_id (" + refId +
							") " + userPerson);
					}
				}

				// set protected titleCache
				StringBuilder referenceTitleCache = new StringBuilder(person.getTitleCache() + ".");
				if (year != null) {
					referenceTitleCache.append(" " + year);
				}


				//ImportHelper.setOriginalSource(user, fauEuConfig.getSourceReference(), userId, namespace);
				ImportHelper.setOriginalSource(person, fauEuConfig.getSourceReference(), refId, namespace);


				// Store persons
				if (!users.containsKey(userPerson.toLowerCase())) {

					if (user == null) {
						logger.warn("User is null");
					}
					users.put(userPerson.toLowerCase(), user);
					if (logger.isTraceEnabled()) {
						logger.trace("Stored user (" + userPerson + ")");
					}
				} else {
					//if (logger.isDebugEnabled()) {
						logger.info("Duplicated user(" + userPerson +")");
					//}
					//continue;
				}

				if (((i % limit) == 0 && i > 1 ) || i == count ) {

					commitUsers(txStatus, persons, users,
							state.getAgentMap(),institutionsToSave, i);

					users = null;
					persons= null;
				}

	        }
	        if (users != null){
	        	commitUsers(txStatus, persons, users,  state.getAgentMap(), institutionsToSave,i);
	        	users = null;
				persons= null;
	        }
		}catch(SQLException e) {
			logger.error("SQLException:" +  e);
			state.setUnsuccessfull();
		}
		/*
		logger.warn("End User doInvoke");
		ProfilerController.memorySnapshot();
		*/
		if(logger.isInfoEnabled()) { logger.info("End making References (Users) ..."); }

		return;
	}

	private void commitUsers(TransactionStatus txStatus,
			Map<String, Person> persons,
			Map<String, User> users,
			Map<Integer, UUID> agentsUUID,
			Collection<Institution> institutionsToSave,
			int i) {

	    Map<UUID, AgentBase> instMap = getAgentService().save(institutionsToSave);
		Map<UUID, AgentBase> userMap = getAgentService().save(persons.values());
		logger.info("i = " + i + " - persons saved");



		Iterator<Entry<UUID, AgentBase>> it = userMap.entrySet().iterator();
		while (it.hasNext()){
			AgentBase person = it.next().getValue();
			int userID = Integer.valueOf(((OriginalSourceBase)person.getSources().iterator().next()).getIdInSource());
			UUID uuid = person.getUuid();
			agentsUUID.put(userID, uuid);
		}

		getUserService().save(users.values());
		logger.info("i = " + users.size() + " - users saved");
		//getReferenceService().save(references.values());
		//logger.info("i = " +references.size() + " - references saved");
		commitTransaction(txStatus);
	}

	/**
	 * Returns an empty string in case of a null string.
	 * This avoids having the string "null" when using StringBuilder.append(null);
	 * @param string
	 * @return
	 */
	private String NullToEmpty(String string) {
		if (string == null) {
			return "";
		} else {
			return string;
		}
	}

	@Override
    protected boolean isIgnore(FaunaEuropaeaImportState state){
		return (state.getConfig().getDoReferences() == IImportConfigurator.DO_REFERENCES.NONE);
	}

}
