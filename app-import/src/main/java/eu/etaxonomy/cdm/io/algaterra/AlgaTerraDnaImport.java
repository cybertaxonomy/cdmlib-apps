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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade.DerivedUnitType;
import eu.etaxonomy.cdm.io.algaterra.validation.AlgaTerraDnaImportValidator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.DefinedTermBase;
import eu.etaxonomy.cdm.model.molecular.DnaSample;
import eu.etaxonomy.cdm.model.molecular.Locus;
import eu.etaxonomy.cdm.model.molecular.Sequence;
import eu.etaxonomy.cdm.model.occurrence.Collection;
import eu.etaxonomy.cdm.model.occurrence.FieldObservation;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationBase;
import eu.etaxonomy.cdm.model.reference.Reference;


/**
 * @author a.mueller
 * @created 01.09.2012
 */
@Component
public class AlgaTerraDnaImport  extends AlgaTerraSpecimenImportBase {
	private static final Logger logger = Logger.getLogger(AlgaTerraDnaImport.class);

	
	private static int modCount = 5000;
	private static final String pluralString = "dna facts";
	private static final String dbTableName = "DNAFact";  //??  


	public AlgaTerraDnaImport(){
		super(dbTableName, pluralString);
	}
	
	
	
	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getIdQuery()
	 */
	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String result = " SELECT df.DNAFactId " + 
				" FROM DNAFact df INNER JOIN Fact f ON  f.ExtensionFk = df.DNAFactID " +
				" ORDER BY df.DNAFactID ";
		return result;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase#getRecordQuery(eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator)
	 */
	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
			String strQuery =   
	            " SELECT * " +
	            " FROM DNAFact df INNER JOIN Fact f ON  f.ExtensionFk = df.DNAFactID " +
	              " WHERE (df.DNAFactId IN (" + ID_LIST_TOKEN + ")  )"  
	            + " ORDER BY DNAFactID "
            ;
		return strQuery;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.IPartitionedIO#doPartition(eu.etaxonomy.cdm.io.berlinModel.in.ResultSetPartitioner, eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState)
	 */
	public boolean doPartition(ResultSetPartitioner partitioner, BerlinModelImportState bmState) {
		boolean success = true;
		
		AlgaTerraImportState state = (AlgaTerraImportState)bmState;
		try {
//			makeVocabulariesAndFeatures(state);
		} catch (Exception e1) {
			logger.warn("Exception occurred when trying to create Ecofact vocabularies: " + e1.getMessage());
			e1.printStackTrace();
		}
		Set<SpecimenOrObservationBase> objectsToSave = new HashSet<SpecimenOrObservationBase>();
		
		//TODO do we still need this map? EcoFacts are not handled separate from Facts.
		//However, they have duplicates on derived unit level. Also check duplicateFk. 
		Map<String, FieldObservation> ecoFactFieldObservationMap = (Map<String, FieldObservation>) partitioner.getObjectMap(ECO_FACT_FIELD_OBSERVATION_NAMESPACE);
		
		ResultSet rs = partitioner.getResultSet();

		try {
			
			int i = 0;

			//for each reference
            while (rs.next()){
                
        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}
				
				int dnaFactId = rs.getInt("DNAFactId");
				int ecoFactFk = nullSafeInt(rs, "EcoFactFk");
				String sequenceStr = rs.getString("PlainSequence");
				String keywordsStr = rs.getString("Keywords");
				String locusStr = rs.getString("Locus");
				String definitionStr = rs.getString("Definition");
				
				try {
					
					//source ref
					Reference<?> sourceRef = state.getTransactionalSourceReference();
				
					//facade
					DnaSample dnaSample = DnaSample.NewInstance();
					
					Sequence sequence = Sequence.NewInstance(sequenceStr);
					dnaSample.addSequences(sequence);
					
					Locus locus = Locus.NewInstance(locusStr, definitionStr);
					
					sequence.setLocus(locus);
					
					
					
//					handleFirstDerivedSpecimen(rs, facade, state, partitioner);
//					handleEcoFactSpecificDerivedUnit(rs,facade, state);

					
					objectsToSave.add(dnaSample); 
					

				} catch (Exception e) {
					logger.warn("Exception in ecoFact: ecoFactId " + dnaFactId + ". " + e.getMessage());
					e.printStackTrace();
				} 
                
            }
           
//            logger.warn("Specimen: " + countSpecimen + ", Descriptions: " + countDescriptions );

			logger.warn("Taxa to save: " + objectsToSave.size());
			getOccurrenceService().save(objectsToSave);	
			
			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}
	
	protected String getDerivedUnitNameSpace(){
		return ECO_FACT_DERIVED_UNIT_NAMESPACE;
	}
	
	protected String getFieldObservationNameSpace(){
		return ECO_FACT_FIELD_OBSERVATION_NAMESPACE;
	}


	private DerivedUnitType makeDerivedUnitType(String recordBasis) {
		DerivedUnitType result = null;
		if (StringUtils.isBlank(recordBasis)){
			result = DerivedUnitType.DerivedUnit;
		} else if (recordBasis.equalsIgnoreCase("FossileSpecimen")){
			result = DerivedUnitType.Fossil;
		}else if (recordBasis.equalsIgnoreCase("HumanObservation")){
			result = DerivedUnitType.Observation;
		}else if (recordBasis.equalsIgnoreCase("Literature")){
			logger.warn("Literature record basis not yet supported");
			result = DerivedUnitType.DerivedUnit;
		}else if (recordBasis.equalsIgnoreCase("LivingSpecimen")){
			result = DerivedUnitType.LivingBeing;
		}else if (recordBasis.equalsIgnoreCase("MachineObservation")){
			logger.warn("MachineObservation record basis not yet supported");
			result = DerivedUnitType.Observation;
		}else if (recordBasis.equalsIgnoreCase("PreservedSpecimen")){
			result = DerivedUnitType.Specimen;
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.IPartitionedIO#getRelatedObjectsForPartition(java.sql.ResultSet)
	 */
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs) {
		String nameSpace;
		Class cdmClass;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<Object, Map<String, ? extends CdmBase>>();
		
		try{
			Set<String> fieldObservationIdSet = new HashSet<String>();
			Set<String> termsIdSet = new HashSet<String>();
			Set<String> collectionIdSet = new HashSet<String>();
			
			while (rs.next()){
//				handleForeignKey(rs, fieldObservationIdSet, "DuplicateFk");
//				handleForeignKey(rs, termsIdSet, "LifeFormFk");
//				handleForeignKey(rs, collectionIdSet, "CollectionFk");
			}
			
			//field observation map for duplicates
			nameSpace = AlgaTerraDnaImport.ECO_FACT_FIELD_OBSERVATION_NAMESPACE;
			cdmClass = FieldObservation.class;
			idSet = fieldObservationIdSet;
			Map<String, FieldObservation> fieldObservationMap = (Map<String, FieldObservation>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, fieldObservationMap);

			//collections
			nameSpace = AlgaTerraCollectionImport.NAMESPACE_COLLECTION;
			cdmClass = Collection.class;
			idSet = collectionIdSet;
			Map<String, Collection> collectionMap = (Map<String, Collection>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, collectionMap);

			//sub-collections
			nameSpace = AlgaTerraCollectionImport.NAMESPACE_SUBCOLLECTION;
			cdmClass = Collection.class;
			idSet = collectionIdSet;
			Map<String, Collection> subCollectionMap = (Map<String, Collection>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, subCollectionMap);

			//terms
			nameSpace = AlgaTerraDnaImport.TERMS_NAMESPACE;
			cdmClass = FieldObservation.class;
			idSet = termsIdSet;
			Map<String, DefinedTermBase> termMap = (Map<String, DefinedTermBase>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
			result.put(nameSpace, termMap);
			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}



	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doCheck(eu.etaxonomy.cdm.io.common.IoStateBase)
	 */
	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new AlgaTerraDnaImportValidator();
		return validator.validate(state);
	}


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	protected boolean isIgnore(BerlinModelImportState state){
		return ! ((AlgaTerraImportState)state).getAlgaTerraConfigurator().isDoDna();
	}
	
}
