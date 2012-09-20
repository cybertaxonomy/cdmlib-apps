/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.algaterra;

import java.net.URI;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.common.media.ImageInfo;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportBase;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelTaxonNameImport;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.OrderedTermVocabulary;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.model.description.DescriptionBase;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.SpecimenDescription;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.Point;
import eu.etaxonomy.cdm.model.location.ReferenceSystem;
import eu.etaxonomy.cdm.model.location.TdwgArea;
import eu.etaxonomy.cdm.model.location.WaterbodyOrCountry;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.occurrence.Collection;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnitBase;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationBase;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @created 12.09.2012
 */
public abstract class AlgaTerraImageImportBase extends BerlinModelImportBase{
	private static final Logger logger = Logger.getLogger(AlgaTerraImageImportBase.class);

	public static final String TERMS_NAMESPACE = "ALGA_TERRA_TERMS";



	
	protected Media handleSingleImage(ResultSet rs, SpecimenOrObservationBase derivedUnit, AlgaTerraImportState state, ResultSetPartitioner partitioner) throws SQLException {
		try {
			String fileName = rs.getString("fileName");
			String figurePhrase = rs.getString("FigurePhrase");
			Integer refFk = nullSafeInt(rs, "refFk");
			Integer refDetailFk = nullSafeInt(rs, "refDetailFk");
			Boolean publishFlag = rs.getBoolean("RestrictedFlag");
			
			
			if (isBlank(fileName)){
				throw new RuntimeException("FileName is missing");
			}
			String fullPath = state.getAlgaTerraConfigurator().getImageBaseUrl() + fileName;
			
			boolean isFigure = false;
			Media media = getImageMedia(fullPath, READ_MEDIA_DATA, isFigure);
			
			if (media == null){
				throw new RuntimeException ("Media not found for " +fullPath);
			}
			if (isNotBlank(figurePhrase)){
				media.putTitle(Language.DEFAULT(), figurePhrase);
			}
			
			//TODO ref
			Reference<?> ref = null;
			if (derivedUnit != null){
				getSpecimenDescription(derivedUnit, ref, IMAGE_GALLERY, CREATE);
			}else{
				logger.warn("Derived unit is null. Can't add media ");
			}
			
			//notes
			
			//TODO restrictedFlag
			
			//TODO id, created for 
			//    	this.doIdCreatedUpdatedNotes(state, descriptionElement, rs, id, namespace);
		
			return media;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    	
	}
	

	


}
