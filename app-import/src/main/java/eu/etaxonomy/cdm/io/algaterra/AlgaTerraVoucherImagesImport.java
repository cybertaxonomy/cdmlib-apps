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
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationBase;


/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class AlgaTerraVoucherImagesImport  extends AlgaTerraImageImportBase {

    private static final long serialVersionUID = -1702110625354900442L;
    private static final Logger logger = LogManager.getLogger();

	private static int modCount = 5000;
	private static final String pluralString = "voucher images";
	private static final String dbTableName = "VoucherImages";  //??

	public AlgaTerraVoucherImagesImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String result = " SELECT VoucherImageID "
				+ " FROM VoucherImages "
				+ " ORDER BY EcoFactFk ";
		return result;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
			String strQuery =

				" SELECT vi.*, vi.Comment as FigurePhrase, vi.PictureFile as fileName, vi.PictuePath as filePath " +
	            " FROM VoucherImages vi  "
	            + 	" WHERE (vi.VoucherImageID IN (" + ID_LIST_TOKEN + ")  )"
	            + " ORDER BY EcoFactFk ";

		return strQuery;
	}

	@Override
    public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState bmState) {
		boolean success = true;

		AlgaTerraImportState state = (AlgaTerraImportState)bmState;

		@SuppressWarnings("rawtypes")
        Set<SpecimenOrObservationBase> unitsToSave = new HashSet<>();

		@SuppressWarnings("unchecked")
        Map<String, DerivedUnit> ecoFactMap = partitioner.getObjectMap(AlgaTerraSpecimenImportBase.ECO_FACT_DERIVED_UNIT_NAMESPACE);

		ResultSet rs = partitioner.getResultSet();

		try {

			int i = 0;

			//for each reference
            while (rs.next()){

        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}

				int figureId = rs.getInt("VoucherImageID");
				int ecoFactFk = rs.getInt("EcoFactFk");


				//TODO etc. Created, Notes, Copyright, TermsOfUse etc.

				try {

					DerivedUnit derivedUnit = ecoFactMap.get(String.valueOf(ecoFactFk));

					if (derivedUnit == null){
						logger.warn("Could not find eco fact specimen (" + ecoFactFk +") for voucher image " +  figureId);
					}

					//field observation
					Media media = handleSingleImage(rs, derivedUnit, state, partitioner, PathType.Voucher);

					handleVoucherImageSpecificFields(rs, media, state);

					if (derivedUnit != null){
						unitsToSave.add(derivedUnit);
					}else{
						logger.warn("DerivedUnit is null");
					}

				} catch (Exception e) {
					logger.warn("Exception in " + getTableName() + ": VoucherImageId " + figureId + ". " + e.getMessage());
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



	private void handleVoucherImageSpecificFields(ResultSet rs, Media media, AlgaTerraImportState state) throws SQLException {
		//TODO

	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> ecoFactIdSet = new HashSet<>();

			while (rs.next()){
				handleForeignKey(rs, ecoFactIdSet, "EcoFactFk");
			}

			//type specimen map
			nameSpace = AlgaTerraSpecimenImportBase.ECO_FACT_DERIVED_UNIT_NAMESPACE;
			idSet = ecoFactIdSet;
			Map<String, DerivedUnit> specimenMap = getCommonService().getSourcedObjectsByIdInSourceC(DerivedUnit.class, idSet, nameSpace);
			result.put(nameSpace, specimenMap);


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
		return !  ( config.isDoEcoFacts() && config.isDoImages()) ;
	}
}
