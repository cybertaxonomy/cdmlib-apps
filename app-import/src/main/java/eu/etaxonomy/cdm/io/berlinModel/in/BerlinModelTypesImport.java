/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.berlinModel.in;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelTypesImportValidator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.SpecimenDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.media.ImageFile;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.name.SpecimenTypeDesignation;
import eu.etaxonomy.cdm.model.name.SpecimenTypeDesignationStatus;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelTypesImport extends BerlinModelImportBase /*implements IIO<BerlinModelImportConfigurator>*/ {

    private static final long serialVersionUID = -8468807718014749046L;
    private static final Logger logger = LogManager.getLogger();

	private static int modCount = 10000;
	private static final String pluralString = "types";
	private static final String dbTableName = "TypeDesignation";


	public BerlinModelTypesImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
		String strRecordQuery =
			" SELECT TypeDesignation.*, TypeStatus.Status " +
			" FROM TypeDesignation LEFT OUTER JOIN " +
			" TypeStatus ON TypeDesignation.TypeStatusFk = TypeStatus.TypeStatusId " +
			" WHERE (TypeDesignationId IN ("+ ID_LIST_TOKEN + ") )";
		return strRecordQuery;
	}

	@Override
    public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState state) {

	    boolean result = true;
		Set<TaxonName> namesToSave = new HashSet<>();
		Map<Integer, DerivedUnit> typeMap = new HashMap<>();

		@SuppressWarnings("unchecked")
        Map<String, TaxonName> nameMap = partitioner.getObjectMap(BerlinModelTaxonNameImport.NAMESPACE);
		@SuppressWarnings("unchecked")
        Map<String, Reference> refMap = partitioner.getObjectMap(BerlinModelReferenceImport.REFERENCE_NAMESPACE);

		BerlinModelImportConfigurator config = state.getConfig();
		Source source = config.getSource();

		ResultSet rs = partitioner.getResultSet();

		try {

			int i = 0;
			//for each reference
			while (rs.next()){

				if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("Types handled: " + (i-1));}

				int typeDesignationId = rs.getInt("typeDesignationId");
				int nameId = rs.getInt("nameFk");
				int typeStatusFk = rs.getInt("typeStatusFk");
				Object refFkObj = rs.getObject("refFk");
				String refDetail = rs.getString("refDetail");
				String status = rs.getString("Status");
				String typePhrase = rs.getString("typePhrase");
				String notes = rs.getString("notes");

				//TODO
				boolean isNotDesignated = false;


				//TODO
				//TypeCache leer
				//RejectedFlag false
				//PublishFlag xxx

				TaxonName taxonNameBase = nameMap.get(String.valueOf(nameId));

				if (taxonNameBase != null){
					try{
						SpecimenTypeDesignationStatus typeDesignationStatus = BerlinModelTransformer.typeStatusId2TypeStatus(typeStatusFk);
						Reference citation = null;
						if (refFkObj != null){
							String relRefFk = String.valueOf(refFkObj);
							//get nomRef
							citation = refMap.get(relRefFk);
						}

						DerivedUnit specimen = DerivedUnit.NewPreservedSpecimenInstance();
						specimen.putDefinition(Language.DEFAULT(), typePhrase);

						if (typePhrase == null){
							String message = "No type phrase available for type with typeDesignationId %s of taxon name %s";
							message = String.format(message, typeDesignationId, taxonNameBase.getTitleCache());
							logger.warn(message);
						}else if (typePhrase.length()> 255){
							typePhrase = typePhrase.substring(0, 255);
							specimen.setTitleCache(typePhrase, true);
						}
						boolean addToAllNames = true;
						String originalNameString = null;
						SpecimenTypeDesignation type = taxonNameBase.addSpecimenTypeDesignation(specimen, typeDesignationStatus, citation, refDetail, originalNameString, isNotDesignated, addToAllNames);
						this.doNotes(type, notes);

						typeMap.put(typeDesignationId, specimen);
						namesToSave.add(taxonNameBase);

					}catch (UnknownCdmTypeException e) {
						logger.warn("TypeStatus '" + status + "' not yet implemented");
						result = false;
					}catch (Exception e) {
						logger.warn("Unexpected exception occurred while processing type with typeDesignationId " + String.valueOf(typeDesignationId));
						result = false;
					}
				}else{
					//TODO
					logger.warn("TaxonName for TypeDesignation " + typeDesignationId + " does not exist in store");
					result = false;
				}
				//put
			}

			result &= makeFigures(typeMap, source);
			logger.info("Names to save: " + namesToSave.size());
			getNameService().save(namesToSave);
			return result;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> nameIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, nameIdSet, "NameFk");
				handleForeignKey(rs, referenceIdSet, "RefFk");
	}

			//name map
			nameSpace = BerlinModelTaxonNameImport.NAMESPACE;
			idSet = nameIdSet;
			Map<String, TaxonName> objectMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonName.class, idSet, nameSpace);
			result.put(nameSpace, objectMap);

			//reference map
			nameSpace = BerlinModelReferenceImport.REFERENCE_NAMESPACE;
			idSet = referenceIdSet;
			Map<String, Reference> referenceMap = getCommonService().getSourcedObjectsByIdInSourceC(Reference.class, idSet, nameSpace);
			result.put(nameSpace, referenceMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}


	private boolean makeFigures(Map<Integer, DerivedUnit> typeMap, Source source){
		boolean success = true;
		try {
			//get data from database
			String strQuery =
					" SELECT * " +
					" FROM TypeFigure " +
                    " WHERE (1=1) ";
			ResultSet rs = source.getResultSet(strQuery) ;

			int i = 0;
			//for each reference
			while (rs.next()){

				if ((i++ % modCount) == 0){ logger.info("TypesFigures handled: " + (i-1));}

				Integer typeFigureId = rs.getInt("typeFigureId");
				Integer typeDesignationFk = rs.getInt("typeDesignationFk");
				Integer collectionFk = rs.getInt("collectionFk");
				String filename = rs.getString("filename");

				String figurePhrase = rs.getString("figurePhrase");

				String mimeType = null; //"image/jpg";
				String suffix = null; //"jpg";
				URI uri = new URI(filename);
				Media media = ImageFile.NewMediaInstance(null, null, uri, mimeType, suffix, null, null, null);
				if (figurePhrase != null) {
					media.addAnnotation(Annotation.NewDefaultLanguageInstance(figurePhrase));
				}
				DerivedUnit typeSpecimen = typeMap.get(typeDesignationFk);
				if (typeSpecimen != null) {
					SpecimenDescription desc = this.getSpecimenDescription(typeSpecimen, IMAGE_GALLERY, CREATE);
					if (desc.getElements().isEmpty()){
						desc.addElement(TextData.NewInstance(Feature.IMAGE()));
					}
					TextData textData = CdmBase.deproxy(desc.getElements().iterator().next(), TextData.class);
					textData.addMedia(media);
//					typeSpecimen.addMedia(media);  #3597
				}

				//mimeType + suffix
				//TODO
				//RefFk
				//RefDetail
				//VerifiedBy
				//VerifiedWhen
				//PrefFigureFlag
				//PublishedFlag
				//etc.
			}
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		} catch (URISyntaxException e) {
			logger.error("URISyntaxException:" +  e);
			return false;
		}

		return success;
	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelTypesImportValidator();
		return validator.validate(state);
	}

	@Override
    protected boolean isIgnore(BerlinModelImportState state){
		return ! state.getConfig().isDoTypes();
	}
}
