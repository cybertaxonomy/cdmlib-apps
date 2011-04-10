package eu.etaxonomy.cdm.io.xper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.ITermService;
import eu.etaxonomy.cdm.api.service.IVocabularyService;
import eu.etaxonomy.cdm.api.service.pager.Pager;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.DefinedTermBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.Representation;
import eu.etaxonomy.cdm.model.common.TermVocabulary;
import eu.etaxonomy.cdm.model.description.CategoricalData;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.FeatureNode;
import eu.etaxonomy.cdm.model.description.FeatureTree;
import eu.etaxonomy.cdm.model.description.MeasurementUnit;
import eu.etaxonomy.cdm.model.description.QuantitativeData;
import eu.etaxonomy.cdm.model.description.State;
import eu.etaxonomy.cdm.model.description.StateData;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.WorkingSet;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import fr_jussieu_snv_lis.Xper;
import fr_jussieu_snv_lis.IO.ICdmAdapter;
import fr_jussieu_snv_lis.base.BaseObjectResource;
import fr_jussieu_snv_lis.base.Individual;
import fr_jussieu_snv_lis.base.Mode;
import fr_jussieu_snv_lis.base.Variable;
import fr_jussieu_snv_lis.base.XPResource;
import fr_jussieu_snv_lis.utils.Utils;

public class AdaptaterCdmXper implements ICdmAdapter{
	private static final Logger logger = Logger.getLogger(AdaptaterCdmXper.class);
	
	TransactionStatus tx = Xper.getCdmApplicationController().startTransaction();
	
	public AdaptaterCdmXper() {
		
	}
	
	
	public void load(){
		loadFeatures();
		loadTaxaAndDescription();

	}

	// Load the featureTree with the UUID
	public void loadFeatures() {
		
		UUID featureTreeUUID = UUID.fromString("1045f91b-6f1a-4a7d-8783-82a58a01ab25");
//		UUID featureTreeUUID = UUID.fromString("43ab1efd-fa15-419a-8cd6-05477e4b37bc");
		List<String> featureTreeInit = Arrays.asList(new String[]{"root.children.feature.representations"});
		
		TransactionStatus tx = Xper.getCdmApplicationController().startTransaction();
		FeatureTree featureTree = Xper.getCdmApplicationController().getFeatureTreeService().load(featureTreeUUID, featureTreeInit);
		if (featureTree != null) {
			loadFeatureNode(featureTree.getRoot(), -1);
		}else{
			logger.warn("Feature tree " + featureTreeUUID.toString() + " not found");
		}
		Xper.getCdmApplicationController().commitTransaction(tx);
	}
	
	/**
	 * Recursive methode to load FeatureNode and all its children
	 * 
	 * @param featureNode
	 * @param indiceParent
	 */
	public void loadFeatureNode(FeatureNode featureNode, int indiceParent){
		List<FeatureNode> featureList = featureNode.getChildren();
		for(FeatureNode child : featureList){
			boolean alreadyExist = false;
			Variable variable = new Variable(child.getFeature().getLabel());
			variable.setUuid(child.getFeature().getUuid());
			List<Variable> vars = Utils.currentBase.getVariables();
			for(Variable var : vars){
				if(var.getName().equals(variable.getName()))
					alreadyExist = true;
			}
			
			if(!alreadyExist && (child.getFeature().isSupportsCategoricalData() || child.getFeature().isSupportsQuantitativeData())){
				
				Utils.currentBase.addVariable(variable);
				
				if(child.getFeature().isSupportsCategoricalData()){
					// Add states to the character
					Set<TermVocabulary<State>> termVocabularySet = child.getFeature().getSupportedCategoricalEnumerations();
					for(TermVocabulary<State> termVocabulary : termVocabularySet){
						for(State state : termVocabulary.getTerms()){
							Mode mode = new Mode(state.getLabel());
							mode.setUuid(state.getUuid());
							variable.addMode(mode);
						}
					}
				}else if (child.getFeature().isSupportsQuantitativeData()) {
					// Specify the character type (numerical)
					variable.setType(Utils.numType);
				}
				
				if(indiceParent != -1 && Utils.currentBase.getVariableAt(indiceParent) != null){
					variable.addMother(((Variable)Utils.currentBase.getVariableAt(indiceParent -1)));
				}
				
				loadFeatureNode(child, variable.getIndexInt());
			}else{
				loadFeatureNode(child, indiceParent);
			}
		}
	}
	
	// Load all the taxa and 1 description
	public void loadTaxaAndDescription() {
		TransactionStatus tx = Xper.getCdmApplicationController().startTransaction();
		List<TaxonBase> taxonList = Xper.getCdmApplicationController().getTaxonService().list(Taxon.class , null, null, null, null);
		for(TaxonBase taxonBase : taxonList){
			if (Utils.currentBase != null) {
				Individual individual = new Individual(taxonBase.getName().toString());
				individual.setUuid(taxonBase.getUuid());
				
				// Add a image to the taxon
				BaseObjectResource bor = new BaseObjectResource(new XPResource("http://www.cheloniophilie.com/Images/Photos/Chelonia-mydas/tortue-marine.JPG"));
                individual.addResource(bor); 
                
				// Add an empty description
                List<Variable> vars = Utils.currentBase.getVariables();
				for(Variable var : vars){
					individual.addMatrix(var, new ArrayList<Mode>());
				}
				loadDescription(individual, taxonBase);
				Utils.currentBase.addIndividual(individual);
			}
		}
		Xper.getCdmApplicationController().commitTransaction(tx);
	}

	// Load the first taxonDescription
	public void loadDescription(Individual individual, TaxonBase taxonBase) {
		
		Pager<TaxonDescription> taxonDescriptionPager = Xper.getCdmApplicationController().getDescriptionService().getTaxonDescriptions((Taxon)taxonBase, null, null, null, 0, Arrays.asList(new String[]{"elements.states", "elements.feature"} ));
		List<TaxonDescription> taxonDescriptionList = taxonDescriptionPager.getRecords();
		TaxonDescription taxonDescription = taxonDescriptionList.get(0);
		Set<DescriptionElementBase> DescriptionElementBaseList = taxonDescription.getElements();
		for(DescriptionElementBase descriptionElementBase : DescriptionElementBaseList){
			if(descriptionElementBase instanceof CategoricalData){
				// find the xper variable corresponding
				Variable variable = null;
				List<Variable> vars = Utils.currentBase.getVariables();
				for(Variable var : vars){
					if(var.getUuid().equals(((CategoricalData)descriptionElementBase).getFeature().getUuid())){
						variable = var;
					}
				}
				if(variable != null){
					// create a list of xper Mode corresponding
					List<Mode> modesList = variable.getModes();
					List<StateData> stateList = ((CategoricalData)descriptionElementBase).getStates();
					for(StateData state : stateList){
						for(Mode mode : modesList){
							if(state.getState().getUuid().equals(mode.getUuid())){
								// Add state to the Description
								individual.addModeMatrix(variable, mode);
							}
						}
					}
				}
			}else if(descriptionElementBase instanceof QuantitativeData){
				// find the xper variable corresponding
				Variable variable = null;
				List<Variable> vars = Utils.currentBase.getVariables();
				for(Variable var : vars){
					if(var.getUuid().equals(((QuantitativeData)descriptionElementBase).getFeature().getUuid())){
						variable = var;
					}
				}
				if(variable != null){
					fr_jussieu_snv_lis.base.QuantitativeData qdXper = new fr_jussieu_snv_lis.base.QuantitativeData();
					QuantitativeData qdCDM = ((QuantitativeData)descriptionElementBase);
					
					if(qdCDM.getMax() != null)
						qdXper.setMax(new Double(qdCDM.getMax()));
					if(qdCDM.getMin() != null)
						qdXper.setMin(new Double(qdCDM.getMin()));
					if(qdCDM.getTypicalLowerBoundary() != null)
						qdXper.setUmethLower(new Double(qdCDM.getTypicalLowerBoundary()));
					if(qdCDM.getTypicalUpperBoundary() != null)
						qdXper.setUmethUpper(new Double(qdCDM.getTypicalUpperBoundary()));
					
					// Does not work
					//qdXper.setMean(new Double(qdCDM.getAverage()));
					//qdXper.setSd(new Double(qdCDM.getStandardDeviation()));
					//qdXper.setNSample(new Double(qdCDM.getSampleSize()));
					
					individual.addNumMatrix(variable, qdXper);
				}
				
			}
		}
	}
	
	// Create a workingSet if not exist
	public void createWorkingSet(){
		
		if(Xper.getCdmApplicationController().getWorkingSetService().list(WorkingSet.class, null, null, null, null).size() <= 0){
			WorkingSet ws = WorkingSet.NewInstance();
			
			UUID featureTreeUUID = UUID.fromString("47eda782-89c7-4c69-9295-e4052ebe16c6");
			List<String> featureTreeInit = Arrays.asList(new String[]{"root.children.feature.representations"});
			
			FeatureTree featureTree = Xper.getCdmApplicationController().getFeatureTreeService().load(featureTreeUUID, featureTreeInit);
			ws.setDescriptiveSystem(featureTree);
			
			List<TaxonBase> taxonList = Xper.getCdmApplicationController().getTaxonService().list(Taxon.class , null, null, null, null);
			for(TaxonBase taxonBase : taxonList){
				Pager<TaxonDescription> taxonDescriptionPager = Xper.getCdmApplicationController().getDescriptionService().getTaxonDescriptions((Taxon)taxonBase, null, null, null, 0, Arrays.asList(new String[]{"elements.states", "elements.feature"} ));
				List<TaxonDescription> taxonDescriptionList = taxonDescriptionPager.getRecords();
				TaxonDescription taxonDescription = taxonDescriptionList.get(0);
				ws.addDescription(taxonDescription);
				System.out.println(taxonDescription.getUuid());
			}
			
			Xper.getCdmApplicationController().getWorkingSetService().save(ws);
		}
	}

	@Override
	public void save() {
		List<Variable> vars = Utils.currentBase.getVariables();
		saveFeatureTree(vars);
		saveFeatures(vars);
	}


	private void saveFeatureTree(List<Variable> vars) {
		logger.warn("Save feature tree  not yet implemented");
	}

	/**
	 * @param vars
	 */
	private void saveFeatures(List<Variable> vars) {
		tx = Xper.getCdmApplicationController().startTransaction();
		for (Variable variable : vars){
			Feature feature = getFeature(variable);
			if (Utils.numType.equals(variable.getType())){
				saveNumericalFeature(variable, feature);
			}else if (Utils.catType.equals(variable.getType())){
				saveCategoricalFeature(variable, feature);
			}else{
				logger.warn("variable type undefined");
			}
		}
		Xper.getCdmApplicationController().commitTransaction(tx);
	}


	/**
	 * @param variable
	 * @return
	 */
	private Feature getFeature(Variable variable) {
		UUID uuid = variable.getUuid();
		ITermService termService = Xper.getCdmApplicationController().getTermService();
		DefinedTermBase<?> term = termService.find(uuid);
		Feature feature = CdmBase.deproxy(term, Feature.class);
		return feature;
	}

	private void saveCategoricalFeature(Variable variable, Feature feature) {
		ITermService termService = Xper.getCdmApplicationController().getTermService();
		IVocabularyService vocService = Xper.getCdmApplicationController().getVocabularyService();
		if (feature == null){
			saveNewFeature(variable, termService, vocService);
		}else{
			if (isChanged(feature, variable)){
				feature.setLabel(variable.getName());
				termService.save(feature);
			}else{
				logger.info("No change for variable: " + variable.getName());
			}
			int numberOfVocs = feature.getSupportedCategoricalEnumerations().size();
			HashMap<UUID, State> allStates = getAllSupportedStates(feature);
			for (Mode mode : variable.getModes()){
				State state = allStates.get(mode.getUuid());
				if (state == null){
					saveNewState(mode, numberOfVocs, feature, termService, vocService);
				}else{
					allStates.remove(state.getUuid());
					if (modeHasChanged(mode, state)){
						String stateDescription = null;
						String stateLabel = mode.getName();
						String stateAbbrev = null;
						Language lang = Language.DEFAULT();
						Representation rep = state.getRepresentation(lang);
						rep.setLabel(stateLabel);
						termService.saveOrUpdate(state);
//						State state = State.NewInstance(stateDescription, stateLabel, stateAbbrev);
//						termService.save(state);
//						voc.addTerm(state);
//						vocService.save(voc);
					}
				}
			}
			for (State state : allStates.values()){
				logger.warn("There is a state to delete: " + feature.getLabel() + "-" + state.getLabel());
				for (TermVocabulary<State> voc :feature.getSupportedCategoricalEnumerations()){
					voc.removeTerm(state);
				}
			}
		}
	}


	private boolean modeHasChanged(Mode mode, State state) {
		if (CdmUtils.nullSafeEqual(mode.getName(), state.getLabel())){
			return false;
		}else{
			return true;
		}
	}


	private void saveNewState(Mode mode, int numberOfVocs, Feature feature, ITermService termService, IVocabularyService vocService) {
		TermVocabulary<State> voc;
		if (numberOfVocs <= 0){
			//new voc
			String vocDescription = null;
			String vocLabel = "Vocabulary for feature " + feature.getLabel();
			String vocAbbrev = null;
			String termSourceUri = null;
			voc = TermVocabulary.NewInstance(vocDescription, vocLabel, vocAbbrev, termSourceUri);
		}else if (numberOfVocs == 1){
			voc = feature.getSupportedCategoricalEnumerations().iterator().next();
		}else{
			//numberOfVocs > 1
			//FIXME preliminary
			voc = feature.getSupportedCategoricalEnumerations().iterator().next();
		}
		saveNewModeToVoc(termService, vocService, voc, mode);
	}
	

	/**
	 * @param variable
	 * @param termService
	 * @param vocService
	 */
	private void saveNewFeature(Variable variable, ITermService termService,
			IVocabularyService vocService) {
		Feature feature;
		//new feature
		String description = null;
		String label = variable.getName();
		String labelAbbrev = null;
		feature = Feature.NewInstance(description, label, labelAbbrev);
		variable.setUuid(feature.getUuid());
		termService.save(feature);
		//new voc
		String vocDescription = null;
		String vocLabel = "Vocabulary for feature " + label;
		String vocAbbrev = null;
		String termSourceUri = null;
		TermVocabulary<State> voc = TermVocabulary.NewInstance(vocDescription, vocLabel, vocAbbrev, termSourceUri);
		for (Mode mode:variable.getModes()){
			saveNewModeToVoc(termService, vocService, voc, mode);
		}
		feature.addSupportedCategoricalEnumeration(voc);
		termService.saveOrUpdate(feature);
	}


	/**
	 * @param termService
	 * @param vocService
	 * @param voc
	 * @param mode
	 */
	private void saveNewModeToVoc(ITermService termService, IVocabularyService vocService, TermVocabulary<State> voc, Mode mode) {
		String stateDescription = null;
		String stateLabel = mode.getName();
		String stateAbbrev = null;
		State state = State.NewInstance(stateDescription, stateLabel, stateAbbrev);
		mode.setUuid(state.getUuid());
		termService.save(state);
		voc.addTerm(state);
		vocService.saveOrUpdate(voc);
	}



	private HashMap<UUID, State> getAllSupportedStates(Feature feature) {
		HashMap<UUID, State> result = new HashMap<UUID,State>();
		Set<TermVocabulary<State>> vocs = feature.getSupportedCategoricalEnumerations();
		for (TermVocabulary<State> voc : vocs){
			for (State state : voc.getTerms()){
				result.put(state.getUuid(), state);
			}
		}
		return result;
	}


	private boolean isChanged(Feature feature, Variable variable) {
		//preliminary
		return ! variable.getName().equals(feature.getLabel());
	}



	private void saveNumericalFeature(Variable variable, Feature feature) {
		ITermService termService = Xper.getCdmApplicationController().getTermService();
		IVocabularyService vocService = Xper.getCdmApplicationController().getVocabularyService();
		String variableUnit = variable.getUnit();
		Set<MeasurementUnit> units = feature.getRecommendedMeasurementUnits();
		//preliminary
		if (StringUtils.isBlank(variableUnit) ){
			//unit is empty
			if (!units.isEmpty()){
				feature.getRecommendedMeasurementUnits().clear();
			}
		}else{
			// unit is not empty
			boolean unitExists = false;
			for (MeasurementUnit measurementUnit: units){
				//TODO ??
				String labelOfUnit = measurementUnit.getLabel();
				if (variableUnit.equals(labelOfUnit)){
					unitExists = true;
					break;
				}
			}
			if (! unitExists){
				units.clear();
				MeasurementUnit existingUnit = findExistingUnit(variableUnit, termService);
				if (existingUnit == null){
					String unitDescription = null;
					String unitLabel = variableUnit;
					String labelAbbrev = null;
					MeasurementUnit newUnit = MeasurementUnit.NewInstance(unitDescription, unitLabel, labelAbbrev);
					termService.save(newUnit);
					UUID defaultMeasurmentUnitVocabularyUuid = UUID.fromString("3b82c375-66bb-4636-be74-dc9cd087292a");
					TermVocabulary voc = vocService.find(defaultMeasurmentUnitVocabularyUuid);
					if (voc == null){
						logger.warn("Could not find MeasurementService vocabulary");
					}else{
						voc.addTerm(newUnit);
						vocService.saveOrUpdate(voc);
					}
					existingUnit = newUnit;
				}
				feature.addRecommendedMeasurementUnit(existingUnit);
			}
		}
	}


	private MeasurementUnit findExistingUnit(String variableUnit, ITermService termService) {
		Pager<MeasurementUnit> existingUnits = termService.findByRepresentationText(variableUnit, MeasurementUnit.class, null, null);
		for (MeasurementUnit exUnit : existingUnits.getRecords()){
			if (variableUnit.equals(exUnit.getLabel())){
				return exUnit;
			}
		}
		return null;
	}

}
