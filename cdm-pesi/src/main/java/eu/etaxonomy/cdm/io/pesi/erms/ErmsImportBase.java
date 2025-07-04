/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.erms;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.CdmImportBase;
import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.EDITOR;
import eu.etaxonomy.cdm.io.common.IPartitionedIO;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.model.common.AnnotatableEntity;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.IdentifiableEntity;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.permission.User;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.DefinedTermBase;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
public abstract class ErmsImportBase<CDM_BASE extends CdmBase>
             extends CdmImportBase<ErmsImportConfigurator, ErmsImportState>
             implements ICdmIO<ErmsImportState>, IPartitionedIO<ErmsImportState> {

    private static final long serialVersionUID = 3856605408484122428L;
    private static Logger logger = LogManager.getLogger();

	public static final UUID ID_IN_SOURCE_EXT_UUID = UUID.fromString("23dac094-e793-40a4-bad9-649fc4fcfd44");

	//NAMESPACES

	protected static final String AREA_NAMESPACE = "gu";
	protected static final String DR_NAMESPACE = "dr";
	protected static final String IMAGE_NAMESPACE = "Images";
	protected static final String LINKS_NAMESPACE = "Links";
	protected static final String NOTES_NAMESPACE = "Notes";
	protected static final String LANGUAGE_NAMESPACE = "Language";
	protected static final String REFERENCE_NAMESPACE = "Source";
	protected static final String SOURCEUSE_NAMESPACE = "tu_sources";
	protected static final String TAXON_NAMESPACE = "Taxon";
	protected static final String NAME_NAMESPACE = "TaxonName";
	protected static final String VERNACULAR_NAMESPACE = "Vernaculars";
	protected static final String FEATURE_NAMESPACE = "note.type";
	protected static final String EXTENSION_TYPE_NAMESPACE = "ExtensionType";

	private String pluralString;
	private String dbTableName;
	//TODO needed?
	private Class cdmTargetClass;

	public ErmsImportBase(String pluralString, String dbTableName, Class cdmTargetClass) {
		this.pluralString = pluralString;
		this.dbTableName = dbTableName;
		this.cdmTargetClass = cdmTargetClass;
	}

	@Override
    protected void doInvoke(ErmsImportState state){
		logger.info("start make " + getPluralString() + " ...");
		ErmsImportConfigurator config = state.getConfig();
		Source source = config.getSource();

		String strIdQuery = getIdQuery();
		String strRecordQuery = getRecordQuery(config);

		int recordsPerTransaction = config.getRecordsPerTransaction();
		recordsPerTransaction = recordsPerTransaction / divideCountBy();

		try{
			ResultSetPartitioner<ErmsImportState> partitioner = ResultSetPartitioner.NewInstance(source, strIdQuery, strRecordQuery, recordsPerTransaction);
			while (partitioner.nextPartition()){
				partitioner.doPartition(this, state);
			}
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			state.setUnsuccessfull();
			return;
		}

		logger.info("end make " + getPluralString() + " ... " + getSuccessString(true));
		return;
	}

	@Override
    public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, ErmsImportState state) {
		boolean success = true ;
		Set<CdmBase> objectsToSave = new HashSet<>();

 		DbImportMapping<?, ?> mapping = getMapping();
		mapping.initialize(state, cdmTargetClass);

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){
			    if (!ignoreRecord(rs)) {
                    success &= mapping.invoke(rs, objectsToSave);
                }
			}
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}

		partitioner.startDoSave();
		saveNonCascadingObjects(objectsToSave);
		getCommonService().save(objectsToSave);
		return success;
	}


    /**
     * Saves objects that do not cascade since #10524
     */
    protected void saveNonCascadingObjects(Set<CdmBase> objectsToSave) {
        objectsToSave.forEach(o->saveNonCascading(o));
    }

    private void saveNonCascading(CdmBase o) {

        if (o.isInstanceOf(TaxonName.class)) {
            CdmBase.deproxy(o, TaxonName.class)
                    .getTypeDesignations().stream()
                    .filter(td->!td.isPersisted())
                    .forEach(td->getCommonService().save(td));
        }else if (o.isInstanceOf(TaxonBase.class)) {
            CdmBase.deproxy(o, TaxonBase.class)
                .getName()
                .getTypeDesignations().stream()  //TODO pot. NPE
                .filter(td->!td.isPersisted())
                .forEach(td->getCommonService().save(td));
        }else if (o.isInstanceOf(DescriptionElementBase.class)) {
            DescriptionElementBase deb = CdmBase.deproxy(o, DescriptionElementBase.class);
            if (!deb.getInDescription().isPersisted()) {
                getCommonService().save(deb.getInDescription());
            }
        }
        return;
    }

    /**
     * Returns <code>true</code> if the current
     * record should be ignored. Should be overriden
     * if a subclass does not want to handle all records.
     * This is primarily important for those subclasses
     * handling last action information which create multiple
     * records per base record.
     * @throws SQLException
     */
    protected boolean ignoreRecord(@SuppressWarnings("unused") ResultSet rs) throws SQLException {
        return false;
    }

    protected abstract DbImportMapping<?, ?> getMapping();

	protected abstract String getRecordQuery(ErmsImportConfigurator config);

	protected String getIdQuery(){
		String result = " SELECT id FROM " + getTableName();
		return result;
	}

	@Override
    public String getPluralString(){
		return pluralString;
	}

	protected String getTableName(){
		return this.dbTableName;
	}

	protected boolean doIdCreatedUpdatedNotes(ErmsImportState state, IdentifiableEntity identifiableEntity, ResultSet rs, long id, String namespace)
			throws SQLException{
		boolean success = true;
		//id
		success &= ImportHelper.setOriginalSource(identifiableEntity, state.getConfig().getSourceReference(), id, namespace);
		//createdUpdateNotes
		success &= doCreatedUpdatedNotes(state, identifiableEntity, rs, namespace);
		return success;
	}

	protected boolean doCreatedUpdatedNotes(ErmsImportState state, AnnotatableEntity annotatableEntity, ResultSet rs, String namespace)
			throws SQLException{

		ErmsImportConfigurator config = state.getConfig();
		Object createdWhen = rs.getObject("Created_When");
		String createdWho = rs.getString("Created_Who");
		Object updatedWhen = null;
		String updatedWho = null;
		try {
			updatedWhen = rs.getObject("Updated_When");
			updatedWho = rs.getString("Updated_who");
		} catch (SQLException e) {
			//Table "Name" has no updated when/who
		}
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

	private User getUser(String userString, ErmsImportState state){
		if (StringUtils.isBlank(userString)){
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

	private User getTransformedUser(String userString, ErmsImportState state){
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

	private User makeNewUser(String userString, ErmsImportState state){
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

	protected boolean resultSetHasColumn(ResultSet rs, String columnName){
		try {
			ResultSetMetaData metaData = rs.getMetaData();
			for (int i = 0; i < metaData.getColumnCount(); i++){
				if (metaData.getColumnName(i + 1).equalsIgnoreCase(columnName)){
					return true;
				}
			}
			return false;
		} catch (SQLException e) {
            logger.warn("Exception in resultSetHasColumn");
            return false;
		}
	}

	protected boolean checkSqlServerColumnExists(Source source, String tableName, String columnName){
		String strQuery = "SELECT  Count(t.id) as n " +
				" FROM sysobjects AS t " +
				" INNER JOIN syscolumns AS c ON t.id = c.id " +
				" WHERE (t.xtype = 'U') AND " +
				" (t.name = '" + tableName + "') AND " +
				" (c.name = '" + columnName + "')";
		ResultSet rs = source.getResultSet(strQuery) ;
		int n;
		try {
			rs.next();
			n = rs.getInt("n");
			return n>0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Returns a map that holds all values of a ResultSet. This is needed if a value needs to
	 * be accessed twice
	 */
	protected Map<String, Object> getValueMap(ResultSet rs) throws SQLException{
		try{
			Map<String, Object> valueMap = new HashMap<>();
			int colCount = rs.getMetaData().getColumnCount();
			for (int c = 0; c < colCount ; c++){
				Object value = rs.getObject(c+1);
				String label = rs.getMetaData().getColumnLabel(c+1).toLowerCase();
				if (value != null && ! CdmUtils.Nz(value.toString()).trim().equals("")){
					valueMap.put(label, value);
				}
			}
			return valueMap;
		}catch(SQLException e){
			throw e;
		}
	}

	protected ExtensionType getExtensionType(UUID uuid, String label, String text, String labelAbbrev){
		ExtensionType extensionType = (ExtensionType)getTermService().find(uuid);
		if (extensionType == null){
			extensionType = ExtensionType.NewInstance(text, label, labelAbbrev);
			extensionType.setUuid(uuid);
			UUID vocUuid = ErmsTransformer.ermsExtensionTypeVocabularyUuid;
            TermVocabulary<ExtensionType> voc = getVocabulary(TermType.ExtensionType,
                    vocUuid, "Erms Extenion Type Vocabulary", ExtensionType.class);
            voc.addTerm(extensionType);
			getTermService().save(extensionType);
		}
		return extensionType;
	}

	protected MarkerType getMarkerType(UUID uuid, String label, String text, String labelAbbrev){
		MarkerType markerType = (MarkerType)getTermService().find(uuid);
		if (markerType == null){
			markerType = MarkerType.NewInstance(label, text, labelAbbrev);
			markerType.setUuid(uuid);
			UUID vocUuid = ErmsTransformer.ermsMarkerTypeVocabularyUuid;
            TermVocabulary<MarkerType> voc = getVocabulary(TermType.MarkerType,
                    vocUuid, "Erms Marker Type Vocabulary", MarkerType.class);
            voc.addTerm(markerType);
			getTermService().save(markerType);
		}
		return markerType;
	}

    protected AnnotationType getAnnotationType(UUID uuid, String label, String text, String labelAbbrev){
        AnnotationType annotationType = (AnnotationType)getTermService().find(uuid);
        if (annotationType == null){
            annotationType = AnnotationType.NewInstance(label, text, labelAbbrev);
            annotationType.setUuid(uuid);
            UUID vocUuid = ErmsTransformer.ermsAnnotationTypeVocabularyUuid;
            TermVocabulary<AnnotationType> voc = getVocabulary(TermType.AnnotationType,
                    vocUuid, "Erms Annotation Type Vocabulary", AnnotationType.class);
            voc.addTerm(annotationType);
            getTermService().save(annotationType);
        }
        return annotationType;
    }

    private <T extends DefinedTermBase<T>> TermVocabulary<T> getVocabulary(TermType termType, UUID vocUuid,
            String label, Class<T> clazz) {

        TermVocabulary<T> voc = getVocabularyService().find(vocUuid);
        if (voc == null) {
            voc = TermVocabulary.NewInstance(termType, clazz, label, label, null, null);
            getVocabularyService().save(voc);
        }
        return voc;
    }

    /**
	 * Reads a foreign key field from the result set and adds its value to the idSet.
	 */
	protected void handleForeignKey(ResultSet rs, Set<String> idSet, String attributeName)
			throws SQLException {
		Object idObj = rs.getObject(attributeName);
		if (idObj != null){
			String id  = String.valueOf(idObj);
			idSet.add(id);
		}
	}

	protected int divideCountBy() { return 1;}

	/**
	 * Returns true if i is a multiple of recordsPerTransaction
	 */
	protected boolean loopNeedsHandling(int i, int recordsPerLoop) {
		startTransaction();
		return (i % recordsPerLoop) == 0;
	}

	protected void doLogPerLoop(int count, int recordsPerLog, String pluralString){
		if ((count % recordsPerLog ) == 0 && count!= 0 ){ logger.info(pluralString + " handled: " + (count));}
	}
}
