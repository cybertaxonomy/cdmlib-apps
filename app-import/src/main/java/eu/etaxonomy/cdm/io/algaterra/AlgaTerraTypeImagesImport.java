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

import eu.etaxonomy.cdm.io.algaterra.validation.AlgaTerraTypeImportValidator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelReferenceImport;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationBase;
import eu.etaxonomy.cdm.model.reference.Reference;


/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class AlgaTerraTypeImagesImport  extends AlgaTerraImageImportBase {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LogManager.getLogger();

	private static int modCount = 5000;
	private static final String pluralString = "type images";
	private static final String dbTableName = "SpecimenFigure";  //??

	public AlgaTerraTypeImagesImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String result = " SELECT SpecimenFigureId "
				+ " FROM SpecimenFigure "
				+ " WHERE TypeSpecimenFk is NOT NULL "
				+ " ORDER BY TypeSpecimenFk ";
		return result;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
			String strQuery =

			" SELECT sf.*, sf.SpecimenFigurePhrase as FigurePhrase, sf.SpecimenFigure as fileName, sf.PicturePath as filePath" +
            " FROM SpecimenFigure sf  "
     	+ 	" WHERE (sf.SpecimenFigureId IN (" + ID_LIST_TOKEN + ")  )"
     	+ " ORDER BY TypeSpecimenFk ";
            ;
		return strQuery;
	}

	@Override
    public boolean doPartition(ResultSetPartitioner partitioner, BerlinModelImportState bmState) {
		boolean success = true;

		AlgaTerraImportState state = (AlgaTerraImportState)bmState;

		Set<SpecimenOrObservationBase> unitsToSave = new HashSet<SpecimenOrObservationBase>();

		Map<String, DerivedUnit> typeSpecimenMap = partitioner.getObjectMap(AlgaTerraSpecimenImportBase.TYPE_SPECIMEN_DERIVED_UNIT_NAMESPACE);
		Map<String, Reference> referenceMap = partitioner.getObjectMap(BerlinModelReferenceImport.REFERENCE_NAMESPACE);


		ResultSet rs = partitioner.getResultSet();

		try {

			int i = 0;

			//for each reference
            while (rs.next()){

        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}

				int figureId = rs.getInt("SpecimenFigureId");
				int typeSpecimenFk = rs.getInt("typeSpecimenFk");


				//TODO etc. Created, Notes, Copyright, TermsOfUse etc.

				try {

					//source ref
					Reference sourceRef = state.getTransactionalSourceReference();

					DerivedUnit derivedUnit = typeSpecimenMap.get(String.valueOf(typeSpecimenFk));

					if (derivedUnit == null){
						logger.warn("Could not find type specimen (" + typeSpecimenFk +") for specimen figure " +  figureId);
					}else{

						//field observation
						Media media = handleSingleImage(rs, derivedUnit, state, partitioner, PathType.Image);

						handleTypeImageSpecificFields(rs, media, state);

						unitsToSave.add(derivedUnit);
					}


				} catch (Exception e) {
					logger.warn("Exception in " + getTableName() + ": SpecimenFigureId " + figureId + ". " + e.getMessage());
					e.printStackTrace();
				}

            }

//            logger.warn("Specimen: " + countSpecimen + ", Descriptions: " + countDescriptions );

			logger.warn(pluralString + " to save: " + unitsToSave.size());
			getOccurrenceService().saveOrUpdate(unitsToSave);

			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}



	private void handleTypeImageSpecificFields(ResultSet rs, Media media, AlgaTerraImportState state) throws SQLException {
		//TODO refFk, refDetailFk, publishFlag
		Integer refFk = nullSafeInt(rs, "refFk");
		Integer refDetailFk = nullSafeInt(rs, "refDetailFk");

		//TODO
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> typeSpecimenIdSet = new HashSet<>();

			while (rs.next()){
				handleForeignKey(rs, typeSpecimenIdSet, "TypeSpecimenFk");
			}

			//type specimen map
			nameSpace = AlgaTerraSpecimenImportBase.TYPE_SPECIMEN_DERIVED_UNIT_NAMESPACE;
			idSet = typeSpecimenIdSet;
			Map<String, DerivedUnit> typeSpecimenMap = getCommonService().getSourcedObjectsByIdInSourceC(DerivedUnit.class, idSet, nameSpace);
			result.put(nameSpace, typeSpecimenMap);


		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new AlgaTerraTypeImportValidator();
		return validator.validate(state);
	}


	@Override
	protected boolean isIgnore(BerlinModelImportState bmState){
		AlgaTerraImportConfigurator config = ((AlgaTerraImportState) bmState).getAlgaTerraConfigurator();
		return !  ( config.isDoTypes() && config.isDoImages()) ;
//		return false;
	}
}