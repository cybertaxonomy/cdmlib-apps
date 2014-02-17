/**
 * Copyright (C) 2007 EDIT
 * European Distributed Institute of Taxonomy 
 * http://www.e-taxonomy.eu
 * 
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */

package eu.etaxonomy.cdm.io.pesi.faunaEuropaea;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.common.IImportConfigurator;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.agent.AgentBase;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.OriginalSourceBase;
import eu.etaxonomy.cdm.model.common.User;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;


/**
 * @author a.babadshanjan
 * @created 23.08.2010
 */
@Component
public class FaunaEuropaeaUsersImport extends FaunaEuropaeaImportBase {
	private static final Logger logger = Logger.getLogger(FaunaEuropaeaUsersImport.class);

	/* Interval for progress info message when retrieving taxa */
	private int modCount = 10000;

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doCheck(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
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
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doInvoke(eu.etaxonomy.cdm.io.common.IImportConfigurator, eu.etaxonomy.cdm.api.application.CdmApplicationController, java.util.Map)
	 */
	@Override
	protected void doInvoke(FaunaEuropaeaImportState state) {				
		/*
		logger.warn("Start User doInvoke");
		ProfilerController.memorySnapshot();
		*/
		TransactionStatus txStatus = null;
		Map<String, AgentBase<?>> persons = null;
		Map<Integer, User> users= null;
		Map<Integer, UUID> userUuids = new HashMap<Integer, UUID>();
		int limit = state.getConfig().getLimitSave();

		FaunaEuropaeaImportConfigurator fauEuConfig = state.getConfig();
		Source source = fauEuConfig.getSource();

		String namespace = "User";
		int i = 0;

		String selectCountUsers = 
			" SELECT count(*) FROM Users";

		String selectColumnsUsers = 
			" SELECT usr_id, usr_title, usr_firstname, usr_lastname, usr_createdat, usr_password FROM Users";

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
	        
	        while (rsUser.next()){
	        	int userId = rsUser.getInt("usr_id");
				String userTitle = rsUser.getString("usr_title");
				String userFirstname = rsUser.getString("usr_firstname");
				String userLastname = rsUser.getString("usr_lastname");
				String createdDate = rsUser.getString("usr_createdat");
				String userPwd = rsUser.getString("usr_password");

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

				// build year
				String year = null;
				if (createdDate != null) {
					year = createdDate.substring(0, createdDate.indexOf("-"));
				}
				
				if ((i++ % limit) == 0) {

					txStatus = startTransaction();
					persons= new HashMap<String,AgentBase<?>>(limit);
					users = new HashMap<Integer,User>(limit);
					
					if(logger.isInfoEnabled()) {
						logger.info("i = " + i + " - User import transaction started"); 
					}
				}
				
				AgentBase<?> person = null;
				User user = null;
				person= Person.NewTitledInstance(userTitle);
				user = User.NewInstance(userPerson, userPwd);
				//reference.setTitle("" + refId); // This unique key is needed to get a hand on this Reference in PesiTaxonExport
				//reference.setDatePublished(ImportHelper.getDatePublished(year));
				
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
						logger.debug("Not imported user with duplicated user_id (" + userId + 
							") " + userPerson);
					}
				}
				
				// set protected titleCache
				/*StringBuilder referenceTitleCache = new StringBuilder(user.getTitleCache() + ".");
				if (year != null) {
					referenceTitleCache.append(" " + year);
				}
				reference.setTitleCache(referenceTitleCache.toString(), true);
				
				reference.setAuthorTeam(author);*/
				
				//ImportHelper.setOriginalSource(user, fauEuConfig.getSourceReference(), userId, namespace);
				ImportHelper.setOriginalSource(person, fauEuConfig.getSourceReference(), userId, namespace);

				
				// Store persons
				if (!users.containsKey(userId)) {

					if (user == null) {
						logger.warn("User is null");
					}
					users.put(userId, user);
					if (logger.isTraceEnabled()) { 
						logger.trace("Stored user (" + userTitle + ")"); 
					}
				} else {
					if (logger.isDebugEnabled()) { 
						logger.debug("Duplicated user(" + userId + ", " + userTitle+ ")");
					}
					//continue;
				}
				
				if (((i % limit) == 0 && i > 1 ) || i == count ) { 
					
					commitUsers(txStatus, persons, users,
							userUuids, i);
					
					users = null;					
					persons= null;
				}
	        	
	        }
	        if (users != null){
	        	commitUsers(txStatus, persons, users, userUuids, i);
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
			Map<String, AgentBase<?>> persons,
			Map<Integer, User> users,
			Map<Integer, UUID> userUuids, int i) {
		Map<UUID, AgentBase> userMap =getAgentService().save((Collection)persons.values());
		logger.info("i = " + i + " - users saved"); 

		Iterator<Entry<UUID, AgentBase>> it = userMap.entrySet().iterator();
		while (it.hasNext()){
			AgentBase person = it.next().getValue();
			int userID = Integer.valueOf(((OriginalSourceBase)person.getSources().iterator().next()).getIdInSource());
			UUID uuid = person.getUuid();
			userUuids.put(userID, uuid);
		}
		
		getUserService().save((Collection)users.values());
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

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	protected boolean isIgnore(FaunaEuropaeaImportState state){
		return (state.getConfig().getDoReferences() == IImportConfigurator.DO_REFERENCES.NONE);
	}

}
