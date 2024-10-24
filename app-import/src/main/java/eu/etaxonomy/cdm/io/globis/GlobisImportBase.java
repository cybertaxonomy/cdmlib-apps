/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.globis;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.NonUniqueObjectException;
import org.joda.time.DateTime;

import eu.etaxonomy.cdm.io.common.CdmImportBase;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.EDITOR;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.AnnotatableEntity;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableEntity;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.location.Country;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.name.IZoologicalName;
import eu.etaxonomy.cdm.model.permission.User;
import eu.etaxonomy.cdm.strategy.exceptions.StringNotParsableException;
import eu.etaxonomy.cdm.strategy.parser.INonViralNameParser;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
public abstract class GlobisImportBase<CDM_BASE extends CdmBase> extends CdmImportBase<GlobisImportConfigurator, GlobisImportState> implements ICdmIO<GlobisImportState>, IPartitionedIO<GlobisImportState> {

    private static final long serialVersionUID = -7813164269152576841L;
    private static final Logger logger = LogManager.getLogger();

	public static final UUID ID_IN_SOURCE_EXT_UUID = UUID.fromString("23dac094-e793-40a4-bad9-649fc4fcfd44");

	//NAMESPACES

	protected static final String REFERENCE_NAMESPACE = "Literatur";
	protected static final String TAXON_NAMESPACE = "current_species";
	protected static final String COLLECTION_NAMESPACE = "Collection";
	protected static final String IMAGE_NAMESPACE = "Einzelbilder";
	protected static final String SPEC_TAX_NAMESPACE = "specTax";
	protected static final String TYPE_NAMESPACE = "specTax.SpecTypeDepository";

	private final String pluralString;
	private final String dbTableName;
	private final Class cdmTargetClass;

	private final INonViralNameParser<?> parser = NonViralNameParserImpl.NewInstance();


	/**
	 * @param dbTableName
	 * @param dbTableName2
	 */
	public GlobisImportBase(String pluralString, String dbTableName, Class<?> cdmTargetClass) {
		this.pluralString = pluralString;
		this.dbTableName = dbTableName;
		this.cdmTargetClass = cdmTargetClass;
	}

	@Override
    protected void doInvoke(GlobisImportState state){
		logger.info("start make " + getPluralString() + " ...");
		GlobisImportConfigurator config = state.getConfig();
		Source source = config.getSource();

		String strIdQuery = getIdQuery();
		String strRecordQuery = getRecordQuery(config);

		int recordsPerTransaction = config.getRecordsPerTransaction();
		try{
			ResultSetPartitioner<GlobisImportState> partitioner = ResultSetPartitioner.NewInstance(source, strIdQuery, strRecordQuery, recordsPerTransaction);
			while (partitioner.nextPartition()){
				partitioner.doPartition(this, state);
			}
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			state.setUnsuccessfull();
		}

		logger.info("end make " + getPluralString() + " ... " + getSuccessString(true));
		return;
	}

	/**
	 * @param authorAndYear
	 * @param zooName
	 */
	protected void handleAuthorAndYear(String authorAndYear, IZoologicalName zooName, Integer id, GlobisImportState state) {
		if (isBlank(authorAndYear)){
			return;
		}else if ("[Denis & Schifferm\u00FCller], 1775".equals(authorAndYear)){
			handleDenisSchiffermueller(zooName, state);
			return;
		}else{
			try {
				String doubtfulAuthorAndYear = null;
				if(authorAndYear.matches(".+\\,\\s\\[\\d{4}\\].*")){
					doubtfulAuthorAndYear = authorAndYear;
					authorAndYear = authorAndYear.replace("[", "").replace("]", "");
				}

				parser.parseAuthors(zooName, authorAndYear);
				deduplicateAuthors(zooName, state);

				if (doubtfulAuthorAndYear != null){
					zooName.setAuthorshipCache(doubtfulAuthorAndYear, true);
				}

			} catch (StringNotParsableException e) {
				logger.warn("Author could not be parsed: " + authorAndYear + " for id "  +id);
				zooName.setAuthorshipCache(authorAndYear, true);
			}
		}
	}

	/**
	 * @param zooName
	 * @param state
	 */
	private void handleDenisSchiffermueller(IZoologicalName zooName,
			GlobisImportState state) {
		String teamStr = "Denis & Schifferm\u00FCller";
		Team team = state.getTeam(teamStr);
		if (team == null){
			team = Team.NewInstance();
			state.putTeam(teamStr, team);
			getAgentService().save(team);
		}
		zooName.setCombinationAuthorship(team);
		zooName.setPublicationYear(1775);
		zooName.setAuthorshipCache("[Denis & Schifferm\u00FCller], 1775", true);
	}


	private void deduplicateAuthors(IZoologicalName zooName, GlobisImportState state) {
		zooName.setCombinationAuthorship(getExistingAuthor(zooName.getCombinationAuthorship(), state));
		zooName.setExCombinationAuthorship(getExistingAuthor(zooName.getExCombinationAuthorship(), state));
		zooName.setBasionymAuthorship(getExistingAuthor(zooName.getBasionymAuthorship(), state));
		zooName.setExBasionymAuthorship(getExistingAuthor(zooName.getExBasionymAuthorship(), state));
	}

	private TeamOrPersonBase<?> getExistingAuthor(TeamOrPersonBase<?> nomAuthor, GlobisImportState state) {
		TeamOrPersonBase<?> author = nomAuthor;
		if (author == null){
			return null;
		}
		if (author instanceof Person){
			Person person = state.getPerson(author.getTitleCache());
			return saveAndDecide(person, author, author.getTitleCache(), state);
		}else if (author instanceof Team){
			String key = GlobisAuthorImport.makeTeamKey((Team)author, state, getAgentService());
			Team existingTeam = state.getTeam(key);
			if (existingTeam == null){
				Team newTeam = Team.NewInstance();
				for (Person member :((Team) author).getTeamMembers()){
					Person existingPerson = state.getPerson(member.getTitleCache());
					if (existingPerson != null){
						try {
							getAgentService().update(existingPerson);
						} catch (NonUniqueObjectException nuoe){
							// person already exists in
							existingPerson = CdmBase.deproxy(getAgentService().find(existingPerson.getUuid()), Person.class);
							state.putPerson(existingPerson.getTitleCache(), existingPerson);
						} catch (Exception e) {
							throw new RuntimeException (e);
						}
						newTeam.addTeamMember(existingPerson);
//
//						logger.warn("newTeam " + newTeam.getId());
					}else{
						newTeam.addTeamMember(member);
					}
				}
				author = newTeam;
			}

			return saveAndDecide(existingTeam, author, key, state);
		}else{
			logger.warn("Author type not supported: " + author.getClass().getName());
			return author;
		}
	}

	private TeamOrPersonBase<?> saveAndDecide(TeamOrPersonBase<?> existing, TeamOrPersonBase<?> author, String key, GlobisImportState state) {
		if (existing != null){
			try {
				getAgentService().update(existing);
			} catch (NonUniqueObjectException nuoe){
				// person already exists in
				existing = CdmBase.deproxy(getAgentService().find(existing.getUuid()), TeamOrPersonBase.class);
				putAgent(existing, key, state);
			} catch (Exception e) {
				throw new RuntimeException (e);
			}
			return existing;
		}else{
			getAgentService().save(author);
			putAgent(author, key, state);
			return author;
		}
	}

	/**
	 * @param author
	 * @param key
	 * @param state
	 */
	private void putAgent(TeamOrPersonBase<?> agent, String key, GlobisImportState state) {
		if (agent instanceof Team){
			state.putTeam(key, (Team)agent);
		}else{
			state.putPerson(key, (Person)agent);
		}
	}

	/**
	 * @param state
	 * @param countryStr
	 * @return
	 */
	protected NamedArea getCountry(GlobisImportState state, String countryStr) {
		NamedArea country = Country.getCountryByLabel(countryStr);
		if (country == null){
			try {
				country = state.getTransformer().getNamedAreaByKey(countryStr);
			} catch (UndefinedTransformerMethodException e) {
				e.printStackTrace();
			}
		}
		return country;
	}



	@Override
    public boolean doPartition(ResultSetPartitioner partitioner, GlobisImportState state) {
		boolean success = true ;
		Set objectsToSave = new HashSet();

 		DbImportMapping<?, ?> mapping = getMapping();
		mapping.initialize(state, cdmTargetClass);

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){
				success &= mapping.invoke(rs,objectsToSave);
			}
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}

		partitioner.startDoSave();
		getCommonService().save(objectsToSave);
		return success;
	}



	/**
	 * @return
	 */
	protected /*abstract*/ DbImportMapping<?, ?> getMapping(){
		return null;
	}

	/**
	 * @return
	 */
	protected abstract String getRecordQuery(GlobisImportConfigurator config);

	/**
	 * @return
	 */
	protected String getIdQuery(){
		String result = " SELECT id FROM " + getTableName();
		return result;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.IPartitionedIO#getPluralString()
	 */
	@Override
    public String getPluralString(){
		return pluralString;
	}

	/**
	 * @return
	 */
	protected String getTableName(){
		return this.dbTableName;
	}

	protected boolean doIdCreatedUpdatedNotes(GlobisImportState state, IdentifiableEntity identifiableEntity, ResultSet rs, long id, String namespace)
			throws SQLException{
		boolean success = true;
		//id
		success &= ImportHelper.setOriginalSource(identifiableEntity, state.getTransactionalSourceReference(), id, namespace);
		//createdUpdateNotes
		success &= doCreatedUpdatedNotes(state, identifiableEntity, rs, namespace);
		return success;
	}


	protected boolean doCreatedUpdatedNotes(GlobisImportState state, AnnotatableEntity annotatableEntity, ResultSet rs, String namespace)
			throws SQLException{

		GlobisImportConfigurator config = state.getConfig();
		Object createdWhen = rs.getObject("Created_When");
		String createdWho = rs.getString("Created_Who");
		Object updatedWhen = rs.getObject("Updated_When");
		String updatedWho = rs.getString("Updated_who");
		String notes = rs.getString("notes");

		boolean success  = true;

		//Created When, Who, Updated When Who
		if (config.getEditor() == null || config.getEditor().equals(EDITOR.NO_EDITORS)){
			//do nothing
		}else if (config.getEditor().equals(EDITOR.EDITOR_AS_ANNOTATION)){
			String createdAnnotationString = "Berlin Model record was created By: " + String.valueOf(createdWho) + " (" + String.valueOf(createdWhen) + ") ";
			if (updatedWhen != null && updatedWho != null){
				createdAnnotationString += " and updated By: " + String.valueOf(updatedWho) + " (" + String.valueOf(updatedWhen) + ")";
			}
			Annotation annotation = Annotation.NewInstance(createdAnnotationString, Language.DEFAULT());
			annotation.setCommentator(config.getCommentator());
			annotation.setAnnotationType(AnnotationType.INTERNAL());
			annotatableEntity.addAnnotation(annotation);
		}else if (config.getEditor().equals(EDITOR.EDITOR_AS_EDITOR)){
			User creator = getUser(createdWho, state);
			User updator = getUser(updatedWho, state);
			DateTime created = getDateTime(createdWhen);
			DateTime updated = getDateTime(updatedWhen);
			annotatableEntity.setCreatedBy(creator);
			annotatableEntity.setUpdatedBy(updator);
			annotatableEntity.setCreated(created);
			annotatableEntity.setUpdated(updated);
		}else {
			logger.warn("Editor type not yet implemented: " + config.getEditor());
		}


		//notes
		if (StringUtils.isNotBlank(notes)){
			String notesString = String.valueOf(notes);
			if (notesString.length() > 65530 ){
				notesString = notesString.substring(0, 65530) + "...";
				logger.warn("Notes string is longer than 65530 and was truncated: " + annotatableEntity);
			}
			Annotation notesAnnotation = Annotation.NewInstance(notesString, null);
			//notesAnnotation.setAnnotationType(AnnotationType.EDITORIAL());
			//notes.setCommentator(bmiConfig.getCommentator());
			annotatableEntity.addAnnotation(notesAnnotation);
		}
		return success;
	}

	private User getUser(String userString, GlobisImportState state){
		if (isBlank(userString)){
			return null;
		}
		userString = userString.trim();

		User user = state.getUser(userString);
		if (user == null){
			user = getTransformedUser(userString,state);
		}
		if (user == null){
			user = makeNewUser(userString, state);
		}
		if (user == null){
			logger.warn("User is null");
		}
		return user;
	}

	private User getTransformedUser(String userString, GlobisImportState state){
		Method method = state.getConfig().getUserTransformationMethod();
		if (method == null){
			return null;
		}
		try {
			userString = (String)state.getConfig().getUserTransformationMethod().invoke(null, userString);
		} catch (Exception e) {
			logger.warn("Error when trying to transform userString " +  userString + ". No transformation done.");
		}
		User user = state.getUser(userString);
		return user;
	}

	private User makeNewUser(String userString, GlobisImportState state){
		String pwd = getPassword();
		User user = User.NewInstance(userString, pwd);
		state.putUser(userString, user);
		getUserService().save(user);
		logger.info("Added new user: " + userString);
		return user;
	}

	private String getPassword(){
		String result = UUID.randomUUID().toString();
		return result;
	}

	private DateTime getDateTime(Object timeString){
		if (timeString == null){
			return null;
		}
		DateTime dateTime = null;
		if (timeString instanceof Timestamp){
			Timestamp timestamp = (Timestamp)timeString;
			dateTime = new DateTime(timestamp);
		}else{
			logger.warn("time ("+timeString+") is not a timestamp. Datetime set to current date. ");
			dateTime = new DateTime();
		}
		return dateTime;
	}



	/**
	 * Reads a foreign key field from the result set and adds its value to the idSet.
	 * @param rs
	 * @param teamIdSet
	 * @throws SQLException
	 */
	protected void handleForeignKey(ResultSet rs, Set<String> idSet, String attributeName)
			throws SQLException {
		Object idObj = rs.getObject(attributeName);
		if (idObj != null){
			String id  = String.valueOf(idObj);
			idSet.add(id);
		}
	}




	/**
	 * Returns true if i is a multiple of recordsPerTransaction
	 * @param i
	 * @param recordsPerTransaction
	 * @return
	 */
	protected boolean loopNeedsHandling(int i, int recordsPerLoop) {
		startTransaction();
		return (i % recordsPerLoop) == 0;
	}

	protected void doLogPerLoop(int count, int recordsPerLog, String pluralString){
		if ((count % recordsPerLog ) == 0 && count!= 0 ){ logger.info(pluralString + " handled: " + (count));}
	}




}
