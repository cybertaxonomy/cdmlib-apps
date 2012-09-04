/**
 * 
 */
package eu.etaxonomy.cdm.io.algaterra;

import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelAuthorImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelAuthorTeamImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelCommonNamesImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelFactsImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelNameFactsImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelNameStatusImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelOccurrenceImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelOccurrenceSourceImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelRefDetailImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelReferenceImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelTaxonImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelTaxonNameImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelTaxonNameRelationImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelTaxonRelationImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelTypesImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelUserImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelWebMarkerCategoryImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelWebMarkerImport;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelGeneralImportValidator;
import eu.etaxonomy.cdm.io.common.ImportStateBase;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;

/**
 * @author pesiimport
 *
 */
public class AlgaTerraImportConfigurator extends BerlinModelImportConfigurator {

	private boolean doSpecimen = true;
	
	public static AlgaTerraImportConfigurator NewInstance(Source berlinModelSource, ICdmDataSource destination){
		return new AlgaTerraImportConfigurator(berlinModelSource, destination);
	}
	
	private AlgaTerraImportConfigurator(Source berlinModelSource, ICdmDataSource destination) {
		super(berlinModelSource, destination);
	}
	
	protected void makeIoClassList(){
		ioClassList = new Class[]{
				BerlinModelGeneralImportValidator.class
				, BerlinModelUserImport.class
				, BerlinModelAuthorImport.class
				, BerlinModelAuthorTeamImport.class
				, BerlinModelRefDetailImport.class
				, BerlinModelReferenceImport.class
				, BerlinModelTaxonNameImport.class
				, BerlinModelTaxonNameRelationImport.class
				, BerlinModelNameStatusImport.class
				, BerlinModelTypesImport.class
				, BerlinModelTaxonImport.class
				, BerlinModelTaxonRelationImport.class
				, BerlinModelCommonNamesImport.class
				, BerlinModelFactsImport.class
				, BerlinModelOccurrenceImport.class
				, BerlinModelOccurrenceSourceImport.class
				, BerlinModelWebMarkerCategoryImport.class
				, BerlinModelWebMarkerImport.class
				
				, AlgaTerraSpecimenImport.class
				
		};	
		
	
	}
	

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator#getNewState()
	 */
	@Override
	public ImportStateBase getNewState() {
		return new AlgaTerraImportState(this);
	}

	public boolean isDoSpecimen() {
		return doSpecimen;
	}

	public void setDoSpecimen(boolean doSpecimen) {
		this.doSpecimen = doSpecimen;
	}


}
