/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.berlinModel.in;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import au.com.bytecode.opencsv.CSVReader;
import eu.etaxonomy.cdm.api.service.media.MediaInfoFileReader;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.common.UTF8;
import eu.etaxonomy.cdm.common.media.CdmImageInfo;
import eu.etaxonomy.cdm.database.update.DatabaseTypeNotSupportedException;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelFactsImportValidator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.agent.AgentBase;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.LanguageString;
import eu.etaxonomy.cdm.model.common.Marker;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.DescriptionElementBase;
import eu.etaxonomy.cdm.model.description.DescriptionElementSource;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.NamedAreaLevel;
import eu.etaxonomy.cdm.model.location.NamedAreaType;
import eu.etaxonomy.cdm.model.media.ImageFile;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.media.MediaRepresentation;
import eu.etaxonomy.cdm.model.media.Rights;
import eu.etaxonomy.cdm.model.media.RightsType;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.Representation;
import eu.etaxonomy.cdm.model.term.TermBase;
import eu.etaxonomy.cdm.model.term.TermTree;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelFactsImport  extends BerlinModelImportBase {

    private static final long serialVersionUID = 4095154818163504795L;
    private static final Logger logger = LogManager.getLogger();

	public static final String NAMESPACE = "Fact";

	public static final String SEQUENCE_PREFIX = "ORDER: ";

	private int modCount = 10000;
	private static final String pluralString = "facts";
	private static final String dbTableName = "Fact";

	//FIXME don't use as class variable
	private Map<Integer, Feature> featureMap;

	public BerlinModelFactsImport(){
		super(dbTableName, pluralString);
	}


	private TermVocabulary<Feature> getFeatureVocabulary(){
	    TermVocabulary<Feature> newVoc = TermVocabulary.NewInstance(TermType.Feature, Feature.class,
	            "Berlin Model Import Feature Vocabulary", "Berlin Model Import Feature Vocabulary", null, null);
	    getVocabularyService().save(newVoc);

	    return newVoc;
	}

	private Map<Integer, Feature>  invokeFactCategories(BerlinModelImportState state){

		Map<Integer, Feature>  result = state.getConfig().getFeatureMap();
		Source source = state.getConfig().getSource();
        boolean createFeatureTree = state.getConfig().isSalvador();  //for some reason feature tree creation does not work for salvador

        TermTree<Feature> featureTree = (!createFeatureTree) ? null : TermTree.NewFeatureInstance(state.getConfig().getFeatureTreeUuid());
        if (featureTree!= null && createFeatureTree){
            featureTree.setTitleCache(state.getConfig().getFeatureTreeTitle(), true);
        }

        try {
			//get data from database
			String strQuery =
					" SELECT FactCategory.* " +
					" FROM FactCategory "+
                    " WHERE (1=1)";
			if (state.getConfig().isSalvador()){
			    strQuery += " AND " + state.getConfig().getFactFilter().replace("factCategoryFk", "factCategoryId");
			}else if (state.getConfig().isMcl()) {
			    strQuery += " AND factCategoryId IN (20, 21) ";
			}

			ResultSet rs = source.getResultSet(strQuery) ;

			TermVocabulary<Feature> featureVocabulary = getFeatureVocabulary();
			int i = 0;
			//for each reference
			while (rs.next()){

				if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("FactCategories handled: " + (i-1));}

				int factCategoryId = rs.getInt("factCategoryId");
				String factCategory = rs.getString("factCategory");

				Feature feature;
				try {
					feature = BerlinModelTransformer.factCategory2Feature(factCategoryId);
				} catch (UnknownCdmTypeException e) {
					UUID featureUuid = null;
					featureUuid = BerlinModelTransformer.getFeatureUuid(String.valueOf(factCategoryId+"-"+factCategory));
					if (featureUuid == null){
						logger.warn("New Feature (FactCategoryId: " + factCategoryId + ")");
						featureUuid = UUID.randomUUID();
					}
					feature = getFeature(state, featureUuid, factCategory, factCategory, null, featureVocabulary);
					if (state.getConfig().isSalvador()){
					    adaptNewSalvadorFeature(factCategoryId, feature);
					}
                    //id
                    doId(state, feature, factCategoryId, "FactCategory");

					//TODO
//					MaxFactNumber	int	Checked
//					ExtensionTableName	varchar(100)	Checked
//					Description	nvarchar(1000)	Checked
//					locExtensionFormName	nvarchar(80)	Checked
//					RankRestrictionFk	int	Checked
				}

				result.put(factCategoryId, feature);
				if (createFeatureTree && isPublicFeature(factCategoryId)){
				    featureTree.getRoot().addChild(feature);
				}
			}
			if (createFeatureTree){
			    featureTree.getRoot().addChild(Feature.DISTRIBUTION(),2);
                featureTree.getRoot().addChild(Feature.NOTES(), featureTree.getRoot().getChildCount()-1);
                getTermTreeService().save(featureTree);
			}
			return result;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return null;
		}
	}

    private void adaptNewSalvadorFeature(int factCategoryId, Feature feature) {
        if (factCategoryId == 306){
            addSpanishRepresentationLabel(feature, "Nombre(s) común(es)");
        } else if (factCategoryId == 307){
            addSpanishRepresentationLabel(feature, "Muestras de herbario");
        } else if (factCategoryId == 310){
            addEnglishFactCategoryName(feature, "Other references for taxon");
        } else if (factCategoryId == 309){
            addEnglishFactCategoryName(feature, "Report (reference) for El Salvador");
        } else if (factCategoryId == 311){
            addEnglishFactCategoryName(feature, "Taxon illustration references");
        } else if (factCategoryId == 312){
            addSpanishRepresentationLabel(feature, "Imágen");
        } else if (factCategoryId == 350){
            addSpanishRepresentationLabel(feature, "Descripción");
        } else if (factCategoryId == 303){
            addEnglishFactCategoryName(feature, "General distribution");
        } else if (factCategoryId == 2000){
            addEnglishFactCategoryName(feature, "Habitat in El Salvador");
        } else if (factCategoryId == 302){
            addSpanishRepresentationLabel(feature, "Usos");
        } else if (factCategoryId == 1800){
            addEnglishFactCategoryName(feature, "Specimen notes");
        } else if (factCategoryId == 1900){
            addEnglishFactCategoryName(feature, "Editorial notes");
        }
    }

    private void addSpanishRepresentationLabel(TermBase term, String label) {
        term.getRepresentations().add(Representation.NewInstance(label, label, null, Language.SPANISH_CASTILIAN()));
    }

    private void addEnglishFactCategoryName(Feature feature, String label) {
        feature.getRepresentations().iterator().next().setLanguage(Language.SPANISH_CASTILIAN());
        feature.getRepresentations().add(Representation.NewInstance(label, label, null, Language.ENGLISH()));
    }

    @Override
	protected void doInvoke(BerlinModelImportState state) {
        if (state.getConfig().isSalvador()){
            invokeSpanishTermLabels();
        }
		featureMap = invokeFactCategories(state);
		super.doInvoke(state);
		return;
	}

    private void invokeSpanishTermLabels() {
        addSpanishRepresentationLabel(Feature.NOTES(), "Notas");
        addSpanishRepresentationLabel(NamedAreaLevel.DEPARTMENT(), "Departamento");
        addSpanishRepresentationLabel(PresenceAbsenceTerm.NATIVE(), "nativo");
        addSpanishRepresentationLabel(PresenceAbsenceTerm.CULTIVATED(), "cultivado");
        addSpanishRepresentationLabel(PresenceAbsenceTerm.PRESENT(), "presente");
    }

    @Override
	protected String getIdQuery(BerlinModelImportState state) {
		String result = super.getIdQuery(state);
		if (StringUtils.isNotBlank(state.getConfig().getFactFilter())){
			result += " WHERE " + state.getConfig().getFactFilter();
		}else{
			result = super.getIdQuery(state);
		}
		result += getOrderBy(state.getConfig());
		return result;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
			String strQuery =
					" SELECT Fact.*, PTaxon.RIdentifier as taxonId, RefDetail.Details " +
					" FROM Fact " +
                      	" INNER JOIN PTaxon ON Fact.PTNameFk = PTaxon.PTNameFk AND Fact.PTRefFk = PTaxon.PTRefFk " +
                      	" LEFT OUTER JOIN RefDetail ON Fact.FactRefDetailFk = RefDetail.RefDetailId AND Fact.FactRefFk = RefDetail.RefFk " +
              	" WHERE (FactId IN (" + ID_LIST_TOKEN + "))";
			    strQuery += getOrderBy(config);

		return strQuery;
	}


	private String getOrderBy(BerlinModelImportConfigurator config) {
		String result;
		try{
			if (config.getSource().checkColumnExists("Fact", "Sequence")){
				result = " ORDER By Fact.Sequence, Fact.FactId";
				if (config.isSalvador()){
				    result = " ORDER By Fact.FactCategoryFk, Fact.Sequence, Fact.FactId";
				}
			}else{
				result = " ORDER By Fact.FactId";
			}
		} catch (DatabaseTypeNotSupportedException e) {
			logger.info("checkColumnExists not supported");
			result = " ORDER By Fact.FactId";
		}
		return result;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState state) {
		boolean success = true ;
		@SuppressWarnings("rawtypes")
        Set<TaxonBase> taxaToSave = new HashSet<>();
		@SuppressWarnings({ "unchecked", "rawtypes" })
        Map<String, TaxonBase> taxonMap = partitioner.getObjectMap(BerlinModelTaxonImport.NAMESPACE);
		@SuppressWarnings("unchecked")
        Map<String, Reference> refMap = partitioner.getObjectMap(BerlinModelReferenceImport.REFERENCE_NAMESPACE);

		ResultSet rs = partitioner.getResultSet();

		Reference sourceRef = state.getTransactionalSourceReference();

		try{
			int i = 0;
			//for each fact
			while (rs.next()){
				try{
					if ((i++ % modCount) == 0){ logger.info("Facts handled: " + (i-1));}

					int factId = rs.getInt("factId");
					Integer taxonId = nullSafeInt(rs, "taxonId");
					Integer factRefFkInt = nullSafeInt(rs, "factRefFk");
					Integer categoryFkInt = nullSafeInt(rs, "factCategoryFk");
					String details = rs.getString("Details");
					String fact = CdmUtils.Nz(rs.getString("Fact"));
					String notes = CdmUtils.Nz(rs.getString("notes"));
					Boolean doubtfulFlag = rs.getBoolean("DoubtfulFlag");

					TaxonBase<?> taxonBase = getTaxon(taxonMap, taxonId, taxonId);
					Feature feature = getFeature(featureMap, categoryFkInt) ;

					if (taxonBase == null){
						logger.warn("Taxon for Fact " + factId + " does not exist in store");
						success = false;
					}else{
						TaxonDescription taxonDescription;
						if ( (taxonDescription = getMyTaxonDescripion(taxonBase, state, categoryFkInt, taxonId, factId, fact, sourceRef)) == null){
							success = false;
							continue;
						}

						DescriptionElementBase deb;

                        if (state.getConfig().isMcl()) {
                            if (categoryFkInt.equals(20)) {
                                deb = handleMclState(state, taxonDescription, fact);
                            }else if (categoryFkInt.equals(21)) {
                                deb = handleMclDistribution(state, taxonDescription, fact);
                            }else {
                                throw new RuntimeException("Unhandled fact category: " + categoryFkInt);
                            }
                            continue;
                        }else {

    						//textData
    						TextData textData = null;
    						boolean newTextData = true;

    						// For Cichorieae DB: If fact category is 31 (Systematics) and there is already a Systematics TextData
    						// description element append the fact text to the existing TextData
    						if(categoryFkInt.equals(31)) {
    							Set<DescriptionElementBase> descriptionElements = taxonDescription.getElements();
    							for (DescriptionElementBase descriptionElement : descriptionElements) {
    								String featureString = descriptionElement.getFeature().getRepresentation(Language.DEFAULT()).getLabel();
    								if (descriptionElement instanceof TextData && featureString.equals("Systematics")) { // TODO: test
    									textData = (TextData)descriptionElement;
    									String factTextStr = textData.getText(Language.DEFAULT());
    									// FIXME: Removing newlines doesn't work
    									if (factTextStr.contains("\\r\\n")) {
    										factTextStr = factTextStr.replaceAll("\\r\\n","");
    									}
    									StringBuilder factText = new StringBuilder(factTextStr);
    									factText.append(fact);
    									fact = factText.toString();
    									newTextData = false;
    									break;
    								}
    							}
    						}

    						if (taxonDescription.isImageGallery()){
    						    newTextData = false;
    						    textData = (TextData)taxonDescription.getElements().iterator().next();
    						}
    						if(newTextData == true)	{
    							textData = TextData.NewInstance();
    						}else if (textData == null){
    						    throw new RuntimeException("Textdata must not be null");  //does not happen, only to avoid potential NPE warnings
    						}


    						//for diptera database
    						if (categoryFkInt.equals(99) && notes.contains("<OriginalName>")){
    							fact = notes + ": " +  fact ;
    						}
    						//for E+M maps
    						if (categoryFkInt.equals(14) && state.getConfig().isRemoveHttpMapsAnchor() && fact.contains("<a href")){
    							//example <a href="http://euromed.luomus.fi/euromed_map.php?taxon=280629&size=medium">distribution</a>
    							fact = fact.replace("<a href=\"", "").replace("\">distribution</a>", "");
    						}

    						//TODO textData.putText(fact, bmiConfig.getFactLanguage());  //doesn't work because  bmiConfig.getFactLanguage() is not not a persistent Language Object
    						//throws  in thread "main" org.springframework.dao.InvalidDataAccessApiUsageException: object references an unsaved transient instance - save the transient instance before flushing: eu.etaxonomy.cdm.model.common.Language; nested exception is org.hibernate.TransientObjectException: object references an unsaved transient instance - save the transient instance before flushing: eu.etaxonomy.cdm.model.common.Language

    						Language lang = Language.DEFAULT();
    						if (state.getConfig().isSalvador()){
    						    lang = getSalvadorFactLanguage(categoryFkInt);
    						}
    						if (! taxonDescription.isImageGallery()){
    							textData.putText(lang, fact);
    							textData.setFeature(feature);
    						}

    						deb = textData;

    						if (state.getConfig().isSalvador()){
    						    if (categoryFkInt == 306){
    						        NamedArea area = null;  // for now we do not set an area as it can not be disabled in dataportals via css yet
    						        deb = CommonTaxonName.NewInstance(fact, Language.SPANISH_CASTILIAN(), area);
    						    }else if (categoryFkInt == 307){
    						        Distribution salvadorDistribution = salvadorDistributionFromMuestrasDeHerbar(state, (Taxon)taxonBase, fact);
    						        if (salvadorDistribution != null){
    						            //id
    						            doId(state, salvadorDistribution, factId, "Fact");
    						            mergeSalvadorDistribution(taxonDescription, salvadorDistribution);
    						        }
    						    }
    						}
                        }


						//reference
						Reference citation = null;
						String factRefFk = String.valueOf(factRefFkInt);
						if (factRefFkInt != null){
							citation = refMap.get(factRefFk);
						}
						if (citation == null && (factRefFkInt != null)){
							logger.warn("Citation not found in referenceMap: " + factRefFk);
							success = false;
						}
						if (citation != null || StringUtils.isNotBlank(details)){
							DescriptionElementSource originalSource = DescriptionElementSource.NewPrimarySourceInstance(citation, details);
							deb.addSource(originalSource);
						}
						taxonDescription.addElement(deb);
						//doubtfulFlag
						if (doubtfulFlag){
							deb.addMarker(Marker.NewInstance(MarkerType.IS_DOUBTFUL(), true));
						}
						//publisheFlag
						String strPublishFlag = "publishFlag";
						boolean publishFlagExists = state.getConfig().getSource().checkColumnExists(dbTableName, strPublishFlag);
						if (publishFlagExists){
							Boolean publishFlag = rs.getBoolean(strPublishFlag);
							if (publishFlag == false){
							    deb.addMarker(Marker.NewInstance(MarkerType.PUBLISH(), publishFlag));
							}
						}

						//Sequence
						Integer sequence = rs.getInt("Sequence");
						if (sequence != 999){
							String strSequence = String.valueOf(sequence);
							strSequence = SEQUENCE_PREFIX + strSequence;
							//TODO make it an Extension when possible
							//Extension datesExtension = Extension.NewInstance(textData, strSequence, ExtensionType.ORDER());
							Annotation annotation = Annotation.NewInstance(strSequence, AnnotationType.INTERNAL(), Language.ENGLISH());
							deb.addAnnotation(annotation);
						}

						//						if (categoryFkObj == FACT_DESCRIPTION){
	//						//;
	//					}else if (categoryFkObj == FACT_OBSERVATION){
	//						//;
	//					}else if (categoryFkObj == FACT_DISTRIBUTION_EM){
	//						//
	//					}else {
	//						//TODO
	//						//logger.warn("FactCategory " + categoryFk + " not yet implemented");
	//					}

						//notes
						doCreatedUpdatedNotes(state, deb, rs);
						doId(state, deb, factId, "Fact");

						//TODO
						//Designation References -> unclear how to map to CDM


						//sequence -> textData is not an identifiable entity therefore extensions are not possible
						//fact category better

						taxaToSave.add(taxonBase);
					}
				} catch (Exception re){
					logger.error("An exception occurred during the facts import");
					re.printStackTrace();
					success = false;
				}
				//put
			}
			logger.info("Facts handled: " + (i-1));
			logger.info("Taxa to save: " + taxaToSave.size());
			getTaxonService().save(taxaToSave);
		}catch(SQLException e){
			throw new RuntimeException(e);
		}
		return success;
	}

    private Distribution handleMclDistribution(BerlinModelImportState state, TaxonDescription taxonDescription, String fact) {
        String areaStr = fact.substring(0, 2);
        NamedArea mclArea = mclAreaUuid(state, areaStr);
        String statusStr = fact.substring(2);
        PresenceAbsenceTerm status = mclDistributionStatus(statusStr);

        Distribution distribution = Distribution.NewInstance(mclArea, status);
        taxonDescription.addElement(distribution);
        return distribution;
    }

    private NamedArea mclAreaUuid(BerlinModelImportState state, String areaStr) {
        UUID uuidArea;
        if (areaStr.equals("AE")) {uuidArea = UUID.fromString("e2367915-828b-4151-a3af-1a278abf1cd5");}
        else if (areaStr.equals("Ag")) {uuidArea = UUID.fromString("9dea2928-65fc-4999-8a5d-f63552553f9f");}
        else if (areaStr.equals("Al")) {uuidArea = UUID.fromString("53e87d91-f5a8-434b-86c9-268750c3473b");}
        else if (areaStr.equals("An")) {uuidArea = UUID.fromString("96394d80-85b7-4b5d-940e-28772cb8fe46");}
        else if (areaStr.equals("Bl")) {uuidArea = UUID.fromString("b9259337-c216-44b2-be26-337e1beebf5f");}
        else if (areaStr.equals("Bu")) {uuidArea = UUID.fromString("bb85aa3f-18cb-4961-866e-8bdedaf0c41b");}
        else if (areaStr.equals("Co")) {uuidArea = UUID.fromString("bcc5a02c-b37f-4639-9f50-e5623da46c95");}
        else if (areaStr.equals("Cr")) {uuidArea = UUID.fromString("0773529f-e230-4397-8ba5-cd12d5af4172");}
        else if (areaStr.equals("Cy")) {uuidArea = UUID.fromString("3b502256-4db5-47be-96da-c20341f7984e");}
        else if (areaStr.equals("Eg")) {uuidArea = UUID.fromString("9564126a-e24c-4ca1-a229-dd5c084dd543");}
        else if (areaStr.equals("Ga")) {uuidArea = UUID.fromString("1a0b6e9d-9568-434f-8346-f33cd31b0c5f");}
        else if (areaStr.equals("Gr")) {uuidArea = UUID.fromString("4a9a0f92-eb51-428a-b82f-96ee7ed77c60");}
        else if (areaStr.equals("Hs")) {uuidArea = UUID.fromString("29f6ac94-2573-4122-87c7-165710037ff6");}
        else if (areaStr.equals("IJ")) {uuidArea = UUID.fromString("abaa10ea-7da4-4940-81e3-6a6023b0d6b5");}
        else if (areaStr.equals("It")) {uuidArea = UUID.fromString("7003c0d4-ffab-4ee6-888d-47fc483fa8bd");}
        else if (areaStr.equals("Ju")) {uuidArea = UUID.fromString("6409b4b8-2b3d-440c-b22c-ef98ed3257f3");}
        else if (areaStr.equals("Li")) {uuidArea = UUID.fromString("a5418d46-5f3c-4964-8b12-19509bb313e1");}
        else if (areaStr.equals("LS")) {uuidArea = UUID.fromString("023f8dba-40f5-4d4e-b2ca-4e3004a25d1c");}
        else if (areaStr.equals("Lu")) {uuidArea = UUID.fromString("6b5018ed-d637-4dd2-a08e-e0d2f7633688");}
        else if (areaStr.equals("Ma")) {uuidArea = UUID.fromString("b9b85f84-6e3e-4f6c-b3bc-2f42d17bf077");}
        else if (areaStr.equals("Me")) {uuidArea = UUID.fromString("6394ee61-999c-473e-88ed-aad9259ffa81");}
        else if (areaStr.equals("RK")) {uuidArea = UUID.fromString("60ea0344-0639-451d-81fd-33263014f30f");}
        else if (areaStr.equals("Sa")) {uuidArea = UUID.fromString("c3a7d579-998d-472b-a238-26d893ee03cb");}
        else if (areaStr.equals("Si")) {uuidArea = UUID.fromString("5187232e-38d3-4bec-9094-2f90abde78b8");}
        else if (areaStr.equals("Sn")) {uuidArea = UUID.fromString("54a53738-2e04-433e-9657-dd1fac45094e");}
        else if (areaStr.equals("Tn")) {uuidArea = UUID.fromString("46d2df14-0b45-4413-bbf0-022d769cb479");}
        else if (areaStr.equals("Tu")) {uuidArea = UUID.fromString("aa93af77-033f-4096-a4ca-468ba07c64d9");}
        else {
            throw new RuntimeException("Unknown area" + areaStr);
        }
        return state.getNamedArea(uuidArea);
    }


    private PresenceAbsenceTerm mclDistributionStatus(String statusStr) {
        if (statusStr.equals("+")) {
            //"present as native"
            return PresenceAbsenceTerm.NATIVE();
        }else if (statusStr.equals("?")) {
            //"doubtfully present"
            return PresenceAbsenceTerm.PRESENT_DOUBTFULLY();
        }else if (statusStr.equals("-")) {
            //"absent but reported in error"
            return PresenceAbsenceTerm.REPORTED_IN_ERROR();
        }else if (statusStr.equals("P")) {
            //"doubtfully naturalized"
            return PresenceAbsenceTerm.INTRODUCED_DOUBTFULLY_INTRODUCED();
        }else if (statusStr.equals("D")) {
            //"doubtfully native"
            return PresenceAbsenceTerm.NATIVE_DOUBTFULLY_NATIVE();
        }else if (statusStr.equals("N")) {
            //"naturalized"
            return PresenceAbsenceTerm.NATURALISED();
        }else if (statusStr.equals("A")) {
            //"casual alien"
            return PresenceAbsenceTerm.CASUAL();
        }else if (statusStr.equals("E")) {
            //"(presumably) extinct"
            return PresenceAbsenceTerm.NATIVE_FORMERLY_NATIVE();
        }else {
            throw new RuntimeException("Unhandled state: " + statusStr);
        }
    }

    private Distribution handleMclState(BerlinModelImportState state, TaxonDescription taxonDescription, String fact) {
        NamedArea mclArea = getNamedArea(state, UUID.fromString("f0500f01-0a59-4a6b-83cf-4070182f7266"), null, null, null, null, null, null, null, null);
        PresenceAbsenceTerm status;
        if (fact.equals("E")) {
            status = PresenceAbsenceTerm.ENDEMIC_FOR_THE_RELEVANT_AREA();
        }else {
            status = getPresenceTerm(state, BerlinModelTransformer.uuidStatusXenophyte,
                    "xenophyte", "Xenophyte in the overall area", "X", false, PresenceAbsenceTerm.PRESENT().getVocabulary());
        }
        Distribution distribution = Distribution.NewInstance(mclArea, status);
        taxonDescription.addElement(distribution);
        return distribution;
    }

    private void mergeSalvadorDistribution(TaxonDescription taxonDescription,
            @NotNull Distribution newDistribution) {

        Distribution existingDistribution = null;
        for (DescriptionElementBase deb : taxonDescription.getElements()){
            if (deb.isInstanceOf(Distribution.class)){
                Distribution distribution = CdmBase.deproxy(deb, Distribution.class);
                if (distribution.getArea() != null && distribution.getArea().equals(newDistribution.getArea())){
                    existingDistribution = distribution;
                    break;
                }else if (distribution.getArea() == null){
                    logger.warn("Area for distribution is null: " + distribution.getUuid());
                }
            }
        }
        if (existingDistribution == null){
            taxonDescription.addElement(newDistribution);
        }else if(!existingDistribution.getStatus().equals(newDistribution.getStatus())){
            //should not happen
            logger.warn("Taxon has areas with different distribution states: " + taxonDescription.getTaxon().getTitleCache());
        }else{
            //do nothing, distribution already exists
        }
    }

    private Map<String, NamedArea> salvadorAreaMap = null;
    private Distribution salvadorDistributionFromMuestrasDeHerbar(BerlinModelImportState importState, Taxon taxon, String fact) {
        if (salvadorAreaMap == null){
            salvadorAreaMap = new HashMap<>();
            TermVocabulary<NamedArea> salvadorAreas = getVocabulary(importState, TermType.NamedArea, BerlinModelTransformer.uuidSalvadorAreas,
                    "Salvador areas", "Salvador areas", null, null, true, NamedArea.NewInstance());
            getVocabularyService().save(salvadorAreas);
        }
        Distribution result = null;
        String[] areaStrings = fact.split(":");
        if (areaStrings.length > 1){
            String areaString = areaStrings[0];
            NamedArea area = salvadorAreaMap.get(areaString);
            if (area == null){
                logger.info("Added Salvador area: " + areaString);
                TermVocabulary<NamedArea> voc = getVocabulary(importState, TermType.NamedArea, BerlinModelTransformer.uuidSalvadorAreas,
                        "Salvador departments", "Salvador departments", null, null, true, NamedArea.NewInstance());
                if (voc.getRepresentation(Language.SPANISH_CASTILIAN()) == null){
                    voc.addRepresentation(Representation.NewInstance("Salvador departamentos", "Salvador departamentos", "dep.", Language.SPANISH_CASTILIAN()));
                    getVocabularyService().saveOrUpdate(voc);
                }
                NamedArea newArea = NamedArea.NewInstance(areaString, areaString, null);
                newArea.getRepresentations().iterator().next().setLanguage(Language.SPANISH_CASTILIAN());
                newArea.setLevel(NamedAreaLevel.DEPARTMENT());
                newArea.setType(NamedAreaType.ADMINISTRATION_AREA());
                voc.addTerm(newArea);
                getTermService().saveOrUpdate(newArea);
                salvadorAreaMap.put(areaString, newArea);
                area = newArea;
            }
            PresenceAbsenceTerm state = getSalvadorDistributionState(taxon);
            result = Distribution.NewInstance(area, state);
            return result;
        }else{
            return null;
        }
    }

    private PresenceAbsenceTerm getSalvadorDistributionState(Taxon taxon) {
        boolean hasGlobalDist = false;
        for (TaxonDescription desc : taxon.getDescriptions()){
            for (DescriptionElementBase deb : desc.getElements()){
                if (deb.getFeature().getUuid().equals(BerlinModelTransformer.uuidFeatureDistributionGlobal)){
                    hasGlobalDist = true;
                    TextData textData = CdmBase.deproxy(deb, TextData.class);
                    for (LanguageString text : textData.getMultilanguageText().values()){
                        if (text.getText().contains("El Salvador")){
                            return PresenceAbsenceTerm.NATIVE();
                        }
                    }
                }
            }
        }
        if (!hasGlobalDist){
            logger.warn("No global distribution found: " + taxon.getTitleCache());
        }
        return hasGlobalDist ? PresenceAbsenceTerm.CULTIVATED(): PresenceAbsenceTerm.PRESENT();
    }


    /**
     * @param factId
     * @return
     */
    private Language getSalvadorFactLanguage(int categoryFkInt) {
        if (categoryFkInt == 350){
            return Language.ENGLISH();
        }else if (categoryFkInt == 1800 || categoryFkInt == 1900){
            return Language.UNDETERMINED();
        }
        return Language.SPANISH_CASTILIAN();
    }


    private TaxonDescription getMyTaxonDescripion(@SuppressWarnings("rawtypes") TaxonBase taxonBase, BerlinModelImportState state, Integer categoryFk, Integer taxonId, int factId, String fact, Reference sourceRef) {
		Taxon taxon = null;
		if ( taxonBase instanceof Taxon ) {
			taxon = (Taxon) taxonBase;
		}else{
			logger.warn("TaxonBase " + (taxonId==null?"(null)":taxonId) + " for Fact " + factId + " was not of type Taxon but: " + taxonBase.getClass().getSimpleName());
			return null;
		}

		TaxonDescription taxonDescription = null;
		Set<TaxonDescription> descriptionSet= taxon.getDescriptions();

		Media media = null;
		//for diptera / salvador images
		if (categoryFk == 51 || categoryFk == 312){  //TODO check also FactCategory string
			media = Media.NewInstance();
			taxonDescription = makeImage(state, fact, media, descriptionSet, taxon);

			if (taxonDescription == null){
				return null;
			}

			TextData textData = null;
			for (DescriptionElementBase el:  taxonDescription.getElements()){
				if (el.isInstanceOf(TextData.class)){
					textData = CdmBase.deproxy(el, TextData.class);
				}
			}
			if (textData == null){
				textData = TextData.NewInstance(Feature.IMAGE());
				taxonDescription.addElement(textData);
			}
			textData.addMedia(media);
		}
		//all others (no image) -> getDescription
		else{
			boolean isPublic = isPublicFeature(categoryFk);
		    for (TaxonDescription desc: descriptionSet){

			    if (! desc.isImageGallery()){
					if (state.getConfig().isSalvador()){
					    if (desc.isDefault() && isPublic || !desc.isDefault() && !isPublic){
					        taxonDescription = desc;
					        break;
					    }
					}else{
					    taxonDescription = desc;
					    break;
					}
				}
			}
			if (taxonDescription == null){
				taxonDescription = TaxonDescription.NewInstance();
				if (state.getConfig().isSalvador()){
				    String title = "Factual data for " + taxon.getName().getTitleCache();
				    if (isPublic){
				        taxonDescription.setDefault(isPublic);
				    }else{
				        title = "Non public f" + title.substring(1);
				    }
				    taxonDescription.setTitleCache(title, true);
				}else if(state.getConfig().isEuroMed()){
				    taxonDescription.setDefault(true);
				}else{
				    taxonDescription.setTitleCache(sourceRef == null ? null : sourceRef.getTitleCache(), true);
				}
				taxon.addDescription(taxonDescription);
			}
		}
		return taxonDescription;
	}

    private boolean isPublicFeature(Integer categoryFk) {
        return ! (categoryFk == 1800 || categoryFk == 1900 || categoryFk == 2000);
    }

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> taxonIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			Set<String> refDetailIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "taxonId");
				handleForeignKey(rs, referenceIdSet, "FactRefFk");
				handleForeignKey(rs, referenceIdSet, "PTDesignationRefFk");
				handleForeignKey(rs, refDetailIdSet, "FactRefDetailFk");
				handleForeignKey(rs, refDetailIdSet, "PTDesignationRefDetailFk");
		}

			//taxon map
			nameSpace = BerlinModelTaxonImport.NAMESPACE;
			idSet = taxonIdSet;
			@SuppressWarnings("rawtypes")
            Map<String, TaxonBase> taxonMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, nameSpace);
			result.put(nameSpace, taxonMap);

			//reference map
			nameSpace = BerlinModelReferenceImport.REFERENCE_NAMESPACE;
			idSet = referenceIdSet;
			Map<String, Reference> referenceMap = getCommonService().getSourcedObjectsByIdInSourceC(Reference.class, idSet, nameSpace);
			result.put(nameSpace, referenceMap);

			//refDetail map
			nameSpace = BerlinModelRefDetailImport.REFDETAIL_NAMESPACE;
			idSet = refDetailIdSet;
			Map<String, Reference> refDetailMap= getCommonService().getSourcedObjectsByIdInSourceC(Reference.class, idSet, nameSpace);
			result.put(nameSpace, refDetailMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
	}
		return result;
	}


	/**
	 * @param state
	 * @param media
	 * @param media
	 * @param descriptionSet
	 * @throws URISyntaxException
	 *
	 */
	private TaxonDescription makeImage(BerlinModelImportState state, String fact, Media media, Set<TaxonDescription> descriptionSet, Taxon taxon) {
		TaxonDescription taxonDescription = null;
		try {
	        Reference sourceRef = state.getTransactionalSourceReference();
    		URI uri;
    		URI thumbUri;
    		if (state.getConfig().isSalvador()){
    		    String thumbs = "thumbs/";
    		    String uriStrFormat = "http://media.e-taxonomy.eu/salvador/berendsohn-et-al-%s/%s.jpg";
    		    Integer intFact = Integer.valueOf(fact);
    		    String vol = "2009";
    		    int page = intFact + 249;
                if (intFact >= 263){
    		        vol = "2016";
    		        page = intFact - (intFact < 403 ? 95 : 94);
    		    }else if (intFact >= 142){
    		        vol = "2012";
    		        page = intFact + (intFact < 255 ? 3 : 4);
    		    }

                String title = getSalvadorImageTitle(intFact, vol);
                media.putTitle(Language.LATIN(), title);
                String description = getSalvadorImageDescription(intFact);
                media.putDescription(Language.SPANISH_CASTILIAN(), description);

    		    Reference ref = getSalvadorReference(vol);
    		    String originalName = getSalvadorImageNameInfo(intFact);
    		    IdentifiableSource source = media.addSource(OriginalSourceType.PrimaryMediaSource, fact, "Fig.", ref, "p. " + page);
    		    source.setOriginalInfo(originalName);
    		    media.setArtist(getSalvadorArtist());
    		    media.addRights(getSalvadorCopyright(vol));
    		    String uriStr = String.format(uriStrFormat, vol, fact);
    		    String thumbUriStr = String.format(uriStrFormat, vol, thumbs + fact);
    		    uri = new URI(uriStr);
    		    thumbUri = new URI(thumbUriStr);
    		}else{
    		    uri = new URI(fact.trim());
    		    thumbUri = null;
    		}

    		makeMediaRepresentation(media, uri);
    		if (thumbUri != null){
                makeMediaRepresentation(media, thumbUri);
    		}

    		taxonDescription = taxon.getOrCreateImageGallery(sourceRef == null ? null :sourceRef.getTitleCache());
		} catch (URISyntaxException e) {
            logger.warn("URISyntaxException. Image could not be imported: " + fact);
            return null;
        }
		return taxonDescription;
	}


	/**
     * @param intFact
     * @param vol
     * @return
     */
    private String getSalvadorImageTitle(Integer intFact, String vol) {
        initSalvadorImagesFile();
        String[] line = salvadorImages.get(intFact);
        if (line == null){
            logger.warn("Could not find salvador image metadata for " + intFact);
            return String.valueOf(intFact);
        }else{
            String name = getSalvadorImageNameInfo(intFact);
            String result = UTF8.QUOT_DBL_LEFT +  name + UTF8.QUOT_DBL_RIGHT + " [Berendsohn & al. " + vol + "]";
            return result;
        }
    }


    private Map<Integer, String[]> salvadorImages = null;
    private String getSalvadorImageDescription(Integer intFact) {
        initSalvadorImagesFile();
        String[] line = salvadorImages.get(intFact);
        if (line == null){
            logger.warn("Could not find salvador image metadata for " + intFact);
            return String.valueOf(intFact);
        }else{
            int i = 2;
            String result = CdmUtils.concat(" " + UTF8.EN_DASH + " ", line[i], line[i + 1]);
            return result;
        }
    }

    private String getSalvadorImageNameInfo(Integer intFact) {
        initSalvadorImagesFile();
        String[] line = salvadorImages.get(intFact);
        if (line == null){
            logger.warn("Could not find salvador image metadata for " + intFact);
            return String.valueOf(intFact);
        }else{
            int i = 1;
            String result = line[i].substring("Fig. ".length() + line[0].length()).trim();
            return result;
        }
    }

    private void initSalvadorImagesFile() {
        if (salvadorImages == null){
            salvadorImages = new HashMap<>();
            try {
                CSVReader reader = new CSVReader(CdmUtils.getUtf8ResourceReader("salvador" + File.separator + "SalvadorImages.csv"),';');
                List<String[]> lines = reader.readAll();
                for (String[] line : lines){
                    String first = line[0];
                    if(! "ID".equals(first)){
                        try {
                            salvadorImages.put(Integer.valueOf(first), line);
                        } catch (NumberFormatException e) {
                            logger.warn("Number not recognized: " + first);
                        }
                    }
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private Rights rights1;
    private Rights rights2;
    private Rights rights3;

    private Rights getSalvadorCopyright(String vol) {
        initRights();
        if ("2009".equals(vol)){
            return rights1;
        }else if ("2012".equals(vol)){
            return rights1;
        }else if ("2016".equals(vol)){
            return rights3;
        }else{
            throw new RuntimeException("Volume not recognized: " + vol);
        }
    }

    private void initRights(){
        if (rights1 == null){
            String text = "(c) Jardín Botánico y Museo Botánico Berlin-Dahlem & Asociación Jardín Botánico La Laguna. Berlin, Antiguo Cuscatlán 2009.";
            rights1 = Rights.NewInstance(text, Language.SPANISH_CASTILIAN(), RightsType.COPYRIGHT());
            text = "(c) Jardín Botánico y Museo Botánico Berlin-Dahlem & Asociación Jardín Botánico La Laguna. Berlin, Antiguo Cuscatlán 2012.";
            rights2 = Rights.NewInstance(text, Language.SPANISH_CASTILIAN(), RightsType.COPYRIGHT());
            text = "(c) Jardín Botánico y Museo Botánico Berlin-Dahlem & Asociación Jardín Botánico La Laguna. Berlin, Antiguo Cuscatlán 2016.";
            rights3 = Rights.NewInstance(text, Language.SPANISH_CASTILIAN(), RightsType.COPYRIGHT());
            getCommonService().save(rights1);
            getCommonService().save(rights2);
            getCommonService().save(rights3);
        }
    }

    private Integer salvadorArtistId;
    private AgentBase<?> getSalvadorArtist() {
        if (salvadorArtistId == null){
            Person person = Person.NewInstance();
            person.setGivenName("José Gerver");
            person.setFamilyName("Molina");
            salvadorArtistId = getAgentService().save(person).getId();
            return person;
        }else{
            return getAgentService().find(salvadorArtistId);
        }
    }

    private Integer salvadorRef1Id;
    private Integer salvadorRef2Id;
    private Integer salvadorRef3Id;

    private Reference getSalvadorReference(String vol){
        if (salvadorRef1Id == null){
            makeSalvadorReferences();
        }
        if ("2009".equals(vol)){
            return getReferenceService().find(salvadorRef1Id);
        }else if ("2012".equals(vol)){
            return getReferenceService().find(salvadorRef2Id);
        }else if ("2016".equals(vol)){
            return getReferenceService().find(salvadorRef3Id);
        }else{
            throw new RuntimeException("Volume not recognized: " + vol);
        }

    }

    private void makeSalvadorReferences() {
        Person walter = Person.NewTitledInstance("Berendsohn, W. G.");
        walter.setGivenName("Walter G.");
        walter.setFamilyName("Berendsohn");
        Person katja = Person.NewTitledInstance("Gruber, Anne Kathrina");
        katja.setGivenName("Anne Katharina");
        katja.setFamilyName("Gruber");
        Person monte = Person.NewTitledInstance("Monterrosa Salomón, J.");
        Person olmedo = Person.NewTitledInstance("Olmedo Galán, P.");
        Person rodriguez = Person.NewTitledInstance("Rodríguez Delcid, D");

        Team team1 = Team.NewInstance();
        team1.addTeamMember(walter);
        team1.addTeamMember(katja);
        team1.addTeamMember(monte);

        Team team2 = Team.NewInstance();
        team2.addTeamMember(walter);
        team2.addTeamMember(katja);
        team2.addTeamMember(rodriguez);
        team2.addTeamMember(olmedo);

        Reference vol1 = ReferenceFactory.newBook();
        Reference vol2 = ReferenceFactory.newBook();
        Reference vol3 = ReferenceFactory.newBook();

        vol1.setAuthorship(team1);
        vol2.setAuthorship(team1);
        vol3.setAuthorship(team2);

        vol1.setDatePublished(TimePeriodParser.parseStringVerbatim("2009"));
        vol2.setDatePublished(TimePeriodParser.parseStringVerbatim("2012"));
        vol3.setDatePublished(TimePeriodParser.parseStringVerbatim("2016"));

        Reference englera = ReferenceFactory.newPrintSeries("Englera");
        vol1.setInSeries(englera);
        vol2.setInSeries(englera);
        vol3.setInSeries(englera);

        vol1.setTitle("Nova Silva Cuscatlanica, Árboles nativos e introducidos de El Salvador - Parte 1: Angiospermae - Familias A a L");
        vol2.setTitle("Nova Silva Cuscatlanica, Árboles nativos e introducidos de El Salvador - Parte 2: Angiospermae - Familias M a P y Pteridophyta");
        vol3.setTitle("Nova Silva Cuscatlanica, Árboles nativos e introducidos de El Salvador - Parte 3: Angiospermae - Familias R a Z y Gymnospermae");

        vol1.setVolume("29(1)");
        vol2.setVolume("29(2)");
        vol3.setVolume("29(3)");

        vol1.setPages("1-438");
        vol2.setVolume("1-300");
        vol3.setVolume("1-356");

        String placePublished = "Berlin: Botanic Garden and Botanical Museum Berlin; Antiguo Cuscatlán: Asociación Jardín Botánico La Laguna, El Salvador";
        vol1.setPlacePublished(placePublished);
        vol2.setPlacePublished(placePublished);
        vol3.setPlacePublished(placePublished);

        salvadorRef1Id = getReferenceService().save(vol1).getId();
        salvadorRef2Id = getReferenceService().find(getReferenceService().saveOrUpdate(vol2)).getId();
        salvadorRef3Id = getReferenceService().find(getReferenceService().saveOrUpdate(vol3)).getId();
        return;
    }

    private void makeMediaRepresentation(Media media, URI uri) {
        CdmImageInfo imageInfo = null;
        Integer size = null;
        try {
            //imageInfo = CdmImageInfo.NewInstance(uri, 30);
            imageInfo = MediaInfoFileReader.legacyFactoryMethod(uri)
                    .readBaseInfo()
                    .getCdmImageInfo();
        } catch (IOException | HttpException e) {
            logger.error("Error when reading image meta: " + e + ", "+ uri.toString());
        }
        String mimeType = imageInfo == null ? null : imageInfo.getMimeType();
        String suffix = imageInfo == null ? null : imageInfo.getSuffix();
        MediaRepresentation mediaRepresentation = MediaRepresentation.NewInstance(mimeType, suffix);
        media.addRepresentation(mediaRepresentation);
        ImageFile image = ImageFile.NewInstance(uri, size, imageInfo);
        mediaRepresentation.addRepresentationPart(image);
    }

	private TaxonBase<?> getTaxon(@SuppressWarnings("rawtypes") Map<String, TaxonBase> taxonMap, Integer taxonIdObj, Number taxonId){
		if (taxonIdObj != null){
			return taxonMap.get(String.valueOf(taxonId));
		}else{
			return null;
		}

	}

	private Feature getFeature(Map<Integer, Feature>  featureMap, Integer categoryFkInt){
		if (categoryFkInt != null){
			return featureMap.get(categoryFkInt);
		}else{
			return null;
		}

	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelFactsImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(BerlinModelImportState state){
		return ! state.getConfig().isDoFacts();
	}



}
