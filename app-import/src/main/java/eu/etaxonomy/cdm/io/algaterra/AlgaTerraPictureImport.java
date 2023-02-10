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

import eu.etaxonomy.cdm.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.facade.DerivedUnitFacadeNotSupportedException;
import eu.etaxonomy.cdm.io.algaterra.validation.AlgaTerraTypeImportValidator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelTaxonImport;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationBase;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * Import for AlgaTerra images from table
 * @author a.mueller
 * @since 18.01.2013
 */
@Component
public class AlgaTerraPictureImport  extends AlgaTerraImageImportBase {

    private static final long serialVersionUID = 3910940848080132170L;
    private static final Logger logger = LogManager.getLogger();

	private static int modCount = 5000;
	private static final String pluralString = "pictures";
	private static final String dbTableName = "Picture";  //??

	public AlgaTerraPictureImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		String result = " SELECT p.PictureId "
				+ " FROM Picture p  INNER JOIN Fact f ON p.PictureId = f.ExtensionFk LEFT OUTER JOIN PTaxon pt ON f.PTNameFk = pt.PTNameFk AND f.PTRefFk = pt.PTRefFk "
				+ " WHERE f.FactCategoryFk = 205 AND p.RestrictedFlag = 0 "
				+ " ORDER BY p.PictureId ";
		return result;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
			String strQuery =

				" SELECT p.*, f.*, pt.RIdentifier, p.PicturePhrase as FigurePhrase, p.PictureFile as fileName, p.PicturePath as filePath " +
	            " FROM Picture p INNER JOIN Fact f ON p.PictureId = f.ExtensionFk LEFT OUTER JOIN PTaxon pt ON f.PTNameFk = pt.PTNameFk AND f.PTRefFk = pt.PTRefFk "
	            + 	" WHERE f.FactCategoryFk = 205 AND ( p.PictureID IN (" + ID_LIST_TOKEN + ")     )"
	            + " ORDER BY p.PictureId, f.factId, pt.RIdentifier ";
		return strQuery;
	}

	@Override
    public boolean doPartition(ResultSetPartitioner partitioner, BerlinModelImportState bmState) {
		boolean success = true;

		AlgaTerraImportState state = (AlgaTerraImportState)bmState;

		Set<TaxonBase> taxaToSave = new HashSet<>();

		Map<String, TaxonBase> taxonMap = partitioner.getObjectMap(BerlinModelTaxonImport.NAMESPACE);
		Map<String, DerivedUnit> specimenMap = partitioner.getObjectMap(AlgaTerraFactEcologyImport.FACT_ECOLOGY_NAMESPACE);

		ResultSet rs = partitioner.getResultSet();

		try {
			int i = 0;
			//for each reference
            while (rs.next()){

        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}

				int pictureId = rs.getInt("PictureID");
				int taxonId = rs.getInt("RIdentifier");
				int factId = rs.getInt("FactId");
				//TODO etc. Created, Notes, Copyright, TermsOfUse etc.
				try {

					TaxonBase<?> taxonBase = taxonMap.get(String.valueOf(taxonId));
					if (taxonBase == null){
						logger.warn("Could not find taxon (" + taxonId +") for picture fact " +  factId);
					}else if (! taxonBase.isInstanceOf(Taxon.class)){
						logger.warn("Taxon is not of class Taxon but " + taxonBase.getClass() + ". RIdentifier: " + taxonId + " PictureId: " +  pictureId + ", FactId: "+  factId);

					}else{
						Taxon taxon = CdmBase.deproxy(taxonBase, Taxon.class);

						Media media = handleSingleImage(rs, taxon, state, partitioner, PathType.Image);

						handlePictureSpecificFields(rs, media, state, specimenMap);

						taxaToSave.add(taxon);
					}
				} catch (Exception e) {
					logger.warn("Exception in " + getTableName() + ": PictureId " + pictureId + ". " + e.getMessage());
					e.printStackTrace();
				}
            }

			logger.warn(pluralString + " to save: " + taxaToSave.size());
			getTaxonService().save(taxaToSave);

			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}

	private void handlePictureSpecificFields(ResultSet rs, Media media, AlgaTerraImportState state, Map<String, DerivedUnit> specimenMap) throws SQLException {
		Integer specimenFactId = nullSafeInt(rs, "FactFk");
		if (specimenFactId != null){
			DerivedUnit specimen = specimenMap.get(String.valueOf(specimenFactId));
			if (specimen == null){
				logger.warn("Specimen not found for FactFK: " + specimenFactId);
			}else{
				try {
					DerivedUnitFacade facade = DerivedUnitFacade.NewInstance(specimen);
					facade.addDerivedUnitMedia(media);
					getOccurrenceService().saveOrUpdate(specimen);
				} catch (DerivedUnitFacadeNotSupportedException e) {
					e.printStackTrace();
					logger.error(e.getMessage());
				}
			}
		}
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> taxonIdSet = new HashSet<>();
			Set<String> specimenIdSet = new HashSet<>();

			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "RIdentifier");
				handleForeignKey(rs, specimenIdSet, "FactFk");
			}

			//taxon map
			nameSpace = BerlinModelTaxonImport.NAMESPACE;
			idSet = taxonIdSet;
			@SuppressWarnings("rawtypes")
            Map<String, TaxonBase> taxonMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, nameSpace);
			result.put(nameSpace, taxonMap);

			//fact specimen map
			nameSpace = AlgaTerraSpecimenImportBase.FACT_ECOLOGY_NAMESPACE;
			idSet = specimenIdSet;
			@SuppressWarnings("rawtypes")
            Map<String, SpecimenOrObservationBase> specimenMap = getCommonService().getSourcedObjectsByIdInSourceC(SpecimenOrObservationBase.class, idSet, nameSpace);
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
		return !  ( config.isDoTypes() && config.isDoImages()) ;
	}
}
