/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.algaterra;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.algaterra.validation.AlgaTerraCollectionImportValidator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.occurrence.Collection;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @since 01.09.2012
 */
@Component
public class AlgaTerraCollectionImport  extends BerlinModelImportBase {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LogManager.getLogger();

	private static int modCount = 5000;
	private static final String pluralString = "collections";
	private static final String dbTableName = "Collection";  //??

	public static final String NAMESPACE_COLLECTION = "Collection";
	public static final String NAMESPACE_SUBCOLLECTION = "Collection (Subcollection)";


	public AlgaTerraCollectionImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String result = " SELECT CollectionId " +
				" FROM Collection c "
				+ " ORDER BY partOfFk, c.CollectionId ";
		return result;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
			String strQuery =
            " SELECT CollectionId, Name, Town, IHCode, Subcollection, partOfFk, TDWGGazetteerFk, Address, CultCollFlag, " +
            		" Created_When, Updated_When, Created_Who, Updated_Who, Notes " +
            " FROM Collection c " +
            " WHERE c.CollectionId IN (" + ID_LIST_TOKEN + ") "
            + " ORDER BY partOfFk, c.CollectionId "
            ;
		return strQuery;
	}

	@Override
    public boolean doPartition(ResultSetPartitioner partitioner, BerlinModelImportState bmState) {
		boolean success = true;

		AlgaTerraImportState state = (AlgaTerraImportState)bmState;
		Set<Collection> collectionsToSave = new HashSet<Collection>();


		@SuppressWarnings("unchecked")
        Map<String, Collection> collectionMap = partitioner.getObjectMap(NAMESPACE_COLLECTION);

		ResultSet rs = partitioner.getResultSet();

		try {

			int i = 0;

			//for each reference
            while (rs.next()){

        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}

        		int collectionId = rs.getInt("CollectionId");
        		String name = rs.getString("Name");
        		String town = rs.getString("Town");
        		String ihCode = rs.getString("IHCode");
        		String subCollectionStr = rs.getString("Subcollection");
        		Integer partOfFk = nullSafeInt(rs, "PartOfFk");

//        		Integer tdwgArea = nullSafeInt(rs, "TDWGGazetteerFk");  //somehow redundant with town
//        		String address = rs.getString("Address");           //only available for BGBM
//        		Boolean cultCollFlag = rs.getBoolean("CultCollFlag");  //?? not really needed according to Henning

        		//TODO createdUpdates, NOtes

        		try {

					//source ref
					Reference sourceRef = state.getTransactionalSourceReference();


					//collection
					Collection collection;
					if (partOfFk == null){
						collection = makeCollection(collectionsToSave,
								collectionId, name, town, ihCode, sourceRef, NAMESPACE_COLLECTION, collectionMap);
					}else{
						collection = collectionMap.get(String.valueOf(partOfFk));
						if (collection == null){
							logger.warn("PartOf collection not found");
						}
					}


					//subcollection
					if (isNotBlank(subCollectionStr)){
						Collection subCollection = makeCollection(collectionsToSave, collectionId, subCollectionStr, town, ihCode, sourceRef, NAMESPACE_SUBCOLLECTION, collectionMap);
						subCollection.setSuperCollection(collection);
					}

					//TODO movedToFk (extension ??)

				} catch (Exception e) {
					logger.warn("Exception in collection: CollectionId " + collectionId + ". " + e.getMessage());
//					e.printStackTrace();
				}
            }

			logger.warn(pluralString + " to save: " + collectionsToSave.size());
			getCollectionService().save(collectionsToSave);

			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}

	private Collection makeCollection(Set<Collection> collectionsToSave, int collectionId,
			String name, String town, String ihCode, Reference sourceRef, String namespace, Map<String, Collection> collectionMap) {
		Collection collection = Collection.NewInstance();
		collection.setName(name);
		if (isNotBlank(ihCode) && ! "--".equals(ihCode)){
			collection.setCode(ihCode);
			collection.setCodeStandard("Index Herbariorum");
		}

		collection.setTownOrLocation(town);
		collection.addSource(OriginalSourceType.Import, String.valueOf(collectionId), namespace, sourceRef, null);

		collectionMap.put(String.valueOf(collectionId), collection);
		collectionsToSave.add(collection);  //or subcollection ?
		return collection;
	}




	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {
		String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> collectionIdSet = new HashSet<>();

			while (rs.next()){
				handleForeignKey(rs, collectionIdSet, "partOfFk");
			}

			//type specimen map
			nameSpace = NAMESPACE_COLLECTION;
			idSet = collectionIdSet;
			Map<String, Collection> collectionMap = getCommonService().getSourcedObjectsByIdInSourceC(Collection.class, idSet, nameSpace);
			result.put(nameSpace, collectionMap);


		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;

	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new AlgaTerraCollectionImportValidator();
		return validator.validate(state);
	}

	@Override
    protected boolean isIgnore(BerlinModelImportState bmState){
		AlgaTerraImportState state = (AlgaTerraImportState)bmState;
		return ! ( state.getAlgaTerraConfigurator().isDoEcoFacts() ||  state.getAlgaTerraConfigurator().isDoTypes() );
	}

}
