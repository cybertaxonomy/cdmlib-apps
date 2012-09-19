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
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade.DerivedUnitType;
import eu.etaxonomy.cdm.api.facade.DerivedUnitFacadeNotSupportedException;
import eu.etaxonomy.cdm.io.algaterra.validation.AlgaTerraCollectionImportValidator;
import eu.etaxonomy.cdm.io.algaterra.validation.AlgaTerraSpecimenImportValidator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelTaxonImport;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.DefinedTermBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.common.TermVocabulary;
import eu.etaxonomy.cdm.model.description.CategoricalData;
import eu.etaxonomy.cdm.model.description.DescriptionBase;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.IndividualsAssociation;
import eu.etaxonomy.cdm.model.description.MeasurementUnit;
import eu.etaxonomy.cdm.model.description.Modifier;
import eu.etaxonomy.cdm.model.description.QuantitativeData;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.description.StatisticalMeasure;
import eu.etaxonomy.cdm.model.description.StatisticalMeasurementValue;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.name.BotanicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.occurrence.Collection;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.FieldObservation;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;


/**
 * @author a.mueller
 * @created 01.09.2012
 */
@Component
public class AlgaTerraCollectionImport  extends AlgaTerraSpecimenImportBase {
	private static final Logger logger = Logger.getLogger(AlgaTerraCollectionImport.class);

	
	private static int modCount = 5000;
	private static final String pluralString = "collections";
	private static final String dbTableName = "Collection";  //??  
	
	public static final String NAMESPACE_COLLECTION = "Collection"; 
	public static final String NAMESPACE_SUBCOLLECTION = "Collection (Subcollection)"; 


	public AlgaTerraCollectionImport(){
		super();
	}
	
	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getIdQuery()
	 */
	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String result = " SELECT CollectionId " + 
				" FROM Collection c "
				+ " ORDER BY c.CollectionId ";
		return result;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getRecordQuery(eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator)
	 */
	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
			String strQuery =   
            " SELECT CollectionId, Name, Town, IHCode, Subcollection, TDWGGazetteerFk, Address, CultCollFlag, " +
            		" Created_When, Updated_When, Created_Who, Updated_Who, Notes " +
            " FROM Collection c " + 
            " WHERE c.CollectionId IN (" + ID_LIST_TOKEN + ") "  
            + " ORDER BY c.CollectionId "
            ;
		return strQuery;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.IPartitionedIO#doPartition(eu.etaxonomy.cdm.io.berlinModel.in.ResultSetPartitioner, eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState)
	 */
	public boolean doPartition(ResultSetPartitioner partitioner, BerlinModelImportState bmState) {
		boolean success = true;
		
		AlgaTerraImportState state = (AlgaTerraImportState)bmState;
		Set<Collection> collectionsToSave = new HashSet<Collection>();
		
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
        		Integer tdwgArea = nullSafeInt(rs, "TDWGGazetteerFk");  //somehow redundant with town
        		String address = rs.getString("Address");           //only available for BGBM
        		Boolean cultCollFlag = rs.getBoolean("CultCollFlag");  //?? not really needed according to Henning
        		//TODO createdUpdates, NOtes		
      
        		try {
					
					//source ref
					Reference<?> sourceRef = state.getTransactionalSourceReference();
				
					
					Collection collection = Collection.NewInstance();
					collection.setName(name);
					if (isNotBlank("ihCode")){
						collection.setCode(ihCode);
						collection.setCodeStandard("Index Herbariorum");
					}
					
					collection.setTownOrLocation(town);
					collection.addSource(String.valueOf(collectionId), NAMESPACE_COLLECTION, sourceRef, null);
					
					collectionsToSave.add(collection);  //or subcollection ? 

					//subcollection
					if (isNotBlank(subCollectionStr)){
						Collection subCollection = Collection.NewInstance();
						subCollection.setName(subCollectionStr);
						subCollection.setSuperCollection(collection);
						collectionsToSave.add(subCollection);  //or subcollection ? 
						collection.addSource(String.valueOf(collectionId), NAMESPACE_SUBCOLLECTION, sourceRef, null);	
					}
					

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



	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.IPartitionedIO#getRelatedObjectsForPartition(java.sql.ResultSet)
	 */
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs) {
		//no related objects are needed
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<Object, Map<String, ? extends CdmBase>>();

		return result;
		
	}


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doCheck(eu.etaxonomy.cdm.io.common.IoStateBase)
	 */
	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new AlgaTerraCollectionImportValidator();
		return validator.validate(state);
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getTableName()
	 */
	@Override
	protected String getTableName() {
		return dbTableName;
	}
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getPluralString()
	 */
	@Override
	public String getPluralString() {
		return pluralString;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	protected boolean isIgnore(BerlinModelImportState bmState){
		AlgaTerraImportState state = (AlgaTerraImportState)bmState;
		return ! ( state.getAlgaTerraConfigurator().isDoSpecimen() ||  state.getAlgaTerraConfigurator().isDoTypes() );
	}
	
}
