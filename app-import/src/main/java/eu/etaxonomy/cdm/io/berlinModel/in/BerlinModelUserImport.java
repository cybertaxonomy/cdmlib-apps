/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.berlinModel.in;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.config.Configuration;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelUserImportValidator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.utils.ImportDeduplicationHelper;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.permission.User;


/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelUserImport extends BerlinModelImportBase {
    private static final long serialVersionUID = 3277951604022442721L;

    private static final Logger logger = Logger.getLogger(BerlinModelUserImport.class);

	public static final String NAMESPACE = "User";

	private static int modCount = 100;
	private static final String dbTableName = "webAuthorisation";
	private static final String pluralString = "Users";

	private ImportDeduplicationHelper<BerlinModelImportState> deduplicationHelper;


	public BerlinModelUserImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelUserImportValidator();
		return validator.validate(state);
	}

	@Override
	protected void doInvoke(BerlinModelImportState state){
		boolean success = true;
	    this.deduplicationHelper = ImportDeduplicationHelper.NewInstance(this, state);

		BerlinModelImportConfigurator config = state.getConfig();
		Source source = config.getSource();
		String dbAttrName;
		String cdmAttrName;

		logger.info("start make "+pluralString+" ...");

		//get data from database
		String strQuery =
				" SELECT *  " +
                " FROM " + dbTableName + " " ;
		ResultSet rs = source.getResultSet(strQuery) ;
		Collection<User> users = new ArrayList<>();
		Set<String> existingUsernames = new HashSet<>();


		TransactionStatus tx = this.startTransaction();
		int i = 0;
		//for each user
		try{
			while (rs.next()){
				try{
					if ((i++ % modCount ) == 0 && i!= 1 ){ logger.info(""+pluralString+" handled: " + (i-1));}

					//
					String username = rs.getString("Username");
					String pwd = rs.getString("Password");
					Integer id = nullSafeInt(rs, "AuthorisationId");

					if (username != null){
					    username = normalizeUsername(state, username);
					}
					if (existingUsernames.contains(username)){
					    continue;
					}
					User user = User.NewInstance(username, pwd);

					dbAttrName = "RealName";
					String realName = rs.getString(dbAttrName);
					if (isNotBlank(realName)){
					    cdmAttrName = "TitleCache";
					    Person person = Person.NewInstance();
					    success &= ImportHelper.addStringValue(rs, person, dbAttrName, cdmAttrName, false);
					    //only to make deduplication work, due to issue that nomenclaturalTitle does not match because set automatically during save
					    cdmAttrName = "nomenclaturalTitle";
					    success &= ImportHelper.addStringValue(rs, person, dbAttrName, cdmAttrName, false);

					    Person dedupPerson = deduplicatePerson(state, person);
			            if (dedupPerson != person){
			                logger.debug("User person deduplicated: " + id);
			            }else{
			                person.addImportSource(String.valueOf(id), dbTableName, state.getTransactionalSourceReference(), null);
			            }
			            user.setPerson(dedupPerson);
					}

					/*
					 * this is a crucial call, otherwise the password will not be set correctly
					 * and the whole authentication will not work
					 */
					authenticate(Configuration.adminLogin, Configuration.adminPassword);
					getUserService().createUser(user);
					existingUsernames.add(username);

					users.add(user);
					state.putUser(username, user);
				}catch(Exception ex){
					logger.error(ex.getMessage());
					ex.printStackTrace();
					state.setUnsuccessfull();
					success = false;
				}
			} //while rs.hasNext()
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			state.setUnsuccessfull();
			return;
		}

		logger.info("save " + i + " "+pluralString + " ...");
		getUserService().saveOrUpdate(users);

		this.commitTransaction(tx);

		logger.info("end make "+pluralString+" ..." + getSuccessString(success));;
		if (!success){
			state.setUnsuccessfull();
		}
		return;
	}


    private Person deduplicatePerson(BerlinModelImportState state, Person person) {
        Person result = deduplicationHelper.getExistingAuthor(state, person);
        return result;
    }


	@Override
	protected boolean isIgnore(BerlinModelImportState state){
		return ! state.getConfig().isDoUser();
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
		return null; // not needed at the moment
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState state) {
		return true;  // not needed at the moment
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {
		return null; //not needed at the moment
	}

}
