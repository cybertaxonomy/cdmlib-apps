/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.berlinModel.in;

import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.TAX_REL_IS_HETEROTYPIC_SYNONYM_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.TAX_REL_IS_HOMOTYPIC_SYNONYM_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.TAX_REL_IS_INCLUDED_IN;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.TAX_REL_IS_MISAPPLIED_NAME_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.TAX_REL_IS_PARTIAL_HETEROTYPIC_SYNONYM_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.TAX_REL_IS_PARTIAL_HOMOTYPIC_SYNONYM_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.TAX_REL_IS_PARTIAL_SYN_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.TAX_REL_IS_PROPARTE_HETEROTYPIC_SYNONYM_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.TAX_REL_IS_PROPARTE_HOMOTYPIC_SYNONYM_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.TAX_REL_IS_PROPARTE_SYN_OF;
import static eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer.TAX_REL_IS_SYNONYM_OF;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.common.ResultWrapper;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelTaxonRelationImportValidator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.common.AnnotatableEntity;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.name.NameRelationshipType;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.taxon.TaxonNodeStatus;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelTaxonRelationImport  extends BerlinModelImportBase  {

    private static final long serialVersionUID = -7234926279240842557L;
    private static final Logger logger = LogManager.getLogger();

	public static final String TREE_NAMESPACE = "PTRefFk";

	private static int modCount = 30000;
	private static final String pluralString = "taxon relations";
	private static final String dbTableName = "RelPTaxon";

	private boolean hasProvisional = true;

	public BerlinModelTaxonRelationImport(){
		super(dbTableName, pluralString);
	}

	/**
	 * Creates a classification for each PTaxon reference which belongs to a taxon that is
	 * included at least in one <i>taxonomically included</i> relationship.
	 *
	 * @param state
	 * @throws SQLException
	 */
	private void makeClassifications(BerlinModelImportState state) throws SQLException{
		logger.info("start make classification ...");

		Set<String> idSet = getTreeReferenceIdSet(state);

		//reference map
		String nameSpace = BerlinModelReferenceImport.REFERENCE_NAMESPACE;
		Class<Reference> cdmClass = Reference.class;
        Map<String, Reference> refMap = getCommonService().getSourcedObjectsByIdInSourceC(cdmClass, idSet, nameSpace);

		String classificationName = "Classification - No Name";

		ResultSet rs = state.getConfig().getSource().getResultSet(getClassificationQuery(state)) ;
		int i = 0;
		//for each reference
		try {
			//TODO handle case useSingleClassification = true && sourceSecId = null, which returns no record
			boolean isFirst = true;
		    while (rs.next()){

				try {
					if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("RelPTaxa handled: " + (i-1));}

					Integer ptRefFkInt = nullSafeInt(rs,"PTRefFk");
					String ptRefFk= String.valueOf(ptRefFkInt);
					Reference ref = refMap.get(ptRefFk);

					String refCache = rs.getString("RefCache");
					if (isNotBlank(refCache)){
						classificationName = refCache;
					}
					if (ref != null && StringUtils.isNotBlank(ref.getTitleCache())){
						classificationName = ref.getTitleCache();
					}
					if (isFirst && isNotBlank(state.getConfig().getClassificationName())){
					    classificationName = state.getConfig().getClassificationName();
					}
					Classification tree = Classification.NewInstance(classificationName);
					tree.setReference(ref);
					if (i == 1 && state.getConfig().getClassificationUuid() != null){
						tree.setUuid(state.getConfig().getClassificationUuid());
					}
					IdentifiableSource identifiableSource = IdentifiableSource.NewDataImportInstance(ptRefFk, TREE_NAMESPACE);
					tree.addSource(identifiableSource);

					getClassificationService().save(tree);
					state.putClassificationUuidInt(ptRefFkInt, tree);
					isFirst = false;
				} catch (Exception e) {
					logger.error("Error in BerlinModleTaxonRelationImport.makeClassifications: " + e.getMessage());
					e.printStackTrace();
				}
			}
		} catch (SQLException e) {
			logger.error("Error in BerlinModleTaxonRelationImport.makeClassifications: " + e.getMessage());
			throw e;
		}
		logger.info("end make classification ...");

		return;
	}

	private Set<String> getTreeReferenceIdSet(BerlinModelImportState state) throws SQLException {
		Source source = state.getConfig().getSource();
		Set<String> result = new HashSet<>();
		ResultSet rs = source.getResultSet(getClassificationQuery(state)) ;
		while (rs.next()){
			Object id = rs.getObject("PTRefFk");
			result.add(String.valueOf(id));
		}
		return result;
	}

	private String getClassificationQuery(BerlinModelImportState state) {
		boolean includeAllClassifications = state.getConfig().isIncludeAllNonMisappliedRelatedClassifications();
		String strQuerySelect = "SELECT PTaxon.PTRefFk, r.RefCache ";
		String strQueryFrom = " FROM RelPTaxon " +
							" INNER JOIN PTaxon AS PTaxon ON RelPTaxon.PTNameFk2 = PTaxon.PTNameFk AND RelPTaxon.PTRefFk2 = PTaxon.PTRefFk " +
							" INNER JOIN Reference r ON PTaxon.PTRefFk = r.RefId ";
		String strQueryWhere = " WHERE (RelPTaxon.RelQualifierFk = 1) ";
		if (includeAllClassifications){
			strQueryWhere = " WHERE (RelPTaxon.RelQualifierFk <> 3) ";
		}else{
			if (state.getConfig().isUseSingleClassification()){
				if (state.getConfig().getSourceSecId()!= null){
					strQueryWhere += " AND PTaxon.PTRefFk = " + state.getConfig().getSourceSecId() +  " ";
				}else{
					strQueryWhere += " AND (1=0) ";
				}
			}
		}

		String strQueryGroupBy = " GROUP BY PTaxon.PTRefFk, r.RefCache ";
		String strQuery = strQuerySelect + " " + strQueryFrom + " " + strQueryWhere + " " + strQueryGroupBy;


		if (includeAllClassifications){
			//add otherdirection
			strQuerySelect = "SELECT PTaxon.PTRefFk, r.RefCache ";
			strQueryFrom = " FROM RelPTaxon rel " +
								" INNER JOIN PTaxon AS PTaxon ON rel.PTNameFk1 = PTaxon.PTNameFk AND rel.PTRefFk1 = PTaxon.PTRefFk " +
								" INNER JOIN Reference r ON PTaxon.PTRefFk = r.RefId ";
			strQueryWhere =" WHERE (rel.RelQualifierFk <> 3) ";
			String strAllQuery =  strQuerySelect + " " + strQueryFrom + " " + strQueryWhere + " " + strQueryGroupBy;
			strQuery = strQuery + " UNION " + strAllQuery;
		}



		boolean includeFlatClassifications = state.getConfig().isIncludeFlatClassifications();
		//concepts with
		if (includeFlatClassifications){
			String strFlatQuery =
					" SELECT pt.PTRefFk AS secRefFk, r.RefCache AS secRef " +
					" FROM PTaxon AS pt LEFT OUTER JOIN " +
					          " Reference r ON pt.PTRefFk = r.RefId LEFT OUTER JOIN " +
					          " RelPTaxon rel1 ON pt.PTNameFk = rel1.PTNameFk2 AND pt.PTRefFk = rel1.PTRefFk2 LEFT OUTER JOIN " +
					          " RelPTaxon AS rel2 ON pt.PTNameFk = rel2.PTNameFk1 AND pt.PTRefFk = rel2.PTRefFk1 " +
					" WHERE (rel2.RelQualifierFk IS NULL) AND (rel1.RelQualifierFk IS NULL) " +
					" GROUP BY pt.PTRefFk, r.RefCache "
					;

			strQuery = strQuery + " UNION " + strFlatQuery;
		}



		if (state.getConfig().getClassificationQuery() != null){
			strQuery = state.getConfig().getClassificationQuery();
		}
		return strQuery;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
		String strQuery =
			" SELECT RelPTaxon.*, fromTaxon.RIdentifier as taxon1Id, toTaxon.RIdentifier as taxon2Id, toTaxon.PTRefFk as treeRefFk, fromTaxon.PTRefFk as fromRefFk, q.is_concept_relation " +
			" FROM PTaxon as fromTaxon " +
              	" INNER JOIN RelPTaxon ON fromTaxon.PTNameFk = relPTaxon.PTNameFk1 AND fromTaxon.PTRefFk = relPTaxon.PTRefFk1 " +
              	" INNER JOIN PTaxon AS toTaxon ON RelPTaxon.PTNameFk2 = ToTaxon.PTNameFk AND RelPTaxon.PTRefFk2 = ToTaxon.PTRefFk " +
              	" INNER JOIN RelPTQualifier q ON q.RelPTQualifierId = RelPTaxon.RelQualifierFk " +
            " WHERE RelPTaxon.RelPTaxonId IN ("+ID_LIST_TOKEN+") " +
            " ORDER BY RelPTaxon.RelPTaxonId ";
		return strQuery;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState state) {
		boolean success = true ;
		@SuppressWarnings("rawtypes")
        Set<TaxonBase> taxaToSave = new HashSet<>();
		@SuppressWarnings({ "unchecked", "rawtypes" })
        Map<String, TaxonBase> taxonMap = partitioner.getObjectMap(BerlinModelTaxonImport.NAMESPACE);
		Map<Integer, Classification> classificationMap = new HashMap<>();
		@SuppressWarnings("unchecked")
        Map<String, Reference> refMap = partitioner.getObjectMap(BerlinModelReferenceImport.REFERENCE_NAMESPACE);

		ResultSet rs = partitioner.getResultSet();

		try{
			int i = 0;
			//for each reference
			while (rs.next()){

				if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("RelPTaxa handled: " + (i-1));}

				int relPTaxonId = rs.getInt("RelPTaxonId");
				Integer taxon1Id = nullSafeInt(rs, "taxon1Id");
				Integer taxon2Id = nullSafeInt(rs, "taxon2Id");
				Integer ptRefFk1 = nullSafeInt(rs, "PTRefFk1");
				Integer ptRefFk2 = nullSafeInt(rs, "PTRefFk2");

				int relQualifierFk = -1;
				try {
					Integer relRefFk = nullSafeInt(rs,"relRefFk");
					int treeRefFk = rs.getInt("treeRefFk");
					int fromRefFk = rs.getInt("fromRefFk");

					relQualifierFk = rs.getInt("relQualifierFk");
					String notes = rs.getString("notes");
					boolean isConceptRelationship = rs.getBoolean("is_concept_relation");

					TaxonBase<?> taxon1 = taxonMap.get(String.valueOf(taxon1Id));
					TaxonBase<?> taxon2 = taxonMap.get(String.valueOf(taxon2Id));

					String refFk = String.valueOf(relRefFk);
					Reference citation = refMap.get(refFk);

					String microcitation = null; //does not exist in RelPTaxon

					if (taxon2 != null && taxon1 != null){
						if (!(taxon2 instanceof Taxon)){
							logger.error("ToTaxon (ID = " + taxon2.getId()+ ", RIdentifier = " + taxon2Id + ") can't be casted to Taxon. RelPTaxon: " + relPTaxonId );
							success = false;
							continue;
						}
						AnnotatableEntity taxonRelationship = null;
						Taxon toTaxon = (Taxon)taxon2;
						if (isTaxonRelationship(relQualifierFk)){
							if (!(taxon1 instanceof Taxon)){
								logger.error("TaxonBase (ID = " + taxon1.getId()+ ", RIdentifier = " + taxon1Id + ") for TaxonRelation ("+relPTaxonId+") can't be casted to Taxon");
								success = false;
								continue;
							}
                            if(state.getConfig().isEuroMed() && CdmUtils.nullSafeEqual(relRefFk, ptRefFk2)){
                                citation = null;
                            }
							Taxon fromTaxon = (Taxon)taxon1;
							if (relQualifierFk == TAX_REL_IS_INCLUDED_IN){
							    Boolean provisional = makeProvisional(rs);
							    taxonRelationship = makeTaxonomicallyIncluded(state, classificationMap, treeRefFk, fromTaxon, toTaxon, citation, microcitation, provisional);
							}else if (relQualifierFk == TAX_REL_IS_MISAPPLIED_NAME_OF){
							    boolean isProParte = "p.p.".equals(notes);
							    if (isProParte){
								    notes = null;
								}
							    boolean isDoubtful = "?".equals(notes);
                                if (isDoubtful){
                                    notes = null;
                                }

                                if (notes!= null && notes.startsWith("{non ") && notes.endsWith("}")){
                                    notes = notes.substring(1, notes.length() - 1);
                                }
							    //handle auct. author
						        if (fromTaxon.getSec() == null || fromTaxon.getSec().getTitleCache().startsWith("auct.")){
							        String existingSecTitle = fromTaxon.getSec() == null ? null : fromTaxon.getSec().getTitleCache().trim();
						            String existingAppendedPhrase = fromTaxon.getAppendedPhrase();
							        if (fromTaxon.getSec() == null && isBlank(existingAppendedPhrase)){
							            existingAppendedPhrase = "auct.";
							        }
						            fromTaxon.setSec(null);
							        if (isNotBlank(existingAppendedPhrase) && isNotBlank(notes)){
							            logger.warn("Misapplied name has >1 MA relation with a note, RelId: " + relPTaxonId);
							        }

							        String newAppendedPhrase = CdmUtils.concat(", ", existingSecTitle, notes);
							        fromTaxon.setAppendedPhrase(CdmUtils.concat("; ", existingAppendedPhrase, newAppendedPhrase));
							        if (isBlank(fromTaxon.getAppendedPhrase())){
							            logger.warn("Appended phrase is empty. This is probably not correct. RelID: " + relPTaxonId);
							        }else if ("auct.".equals(fromTaxon.getAppendedPhrase())){
							            fromTaxon.setAppendedPhrase(null);
							        }
							        notes = null;
							    }else if (notes != null && notes.startsWith("non ")){
							        fromTaxon.setAppendedPhrase(CdmUtils.concat(", ", fromTaxon.getAppendedPhrase(), notes));
							        notes = null;
							    }

							    if (isProParte){
							        taxonRelationship = toTaxon.addProParteMisappliedName(fromTaxon, citation, microcitation);
							    }else{
							        taxonRelationship = toTaxon.addMisappliedName(fromTaxon, citation, microcitation);
	                            }
							    if (isDoubtful){
							        ((TaxonRelationship)taxonRelationship).setDoubtful(true);
							    }
                            }else if (relQualifierFk == TAX_REL_IS_PROPARTE_SYN_OF ||
                                    relQualifierFk == TAX_REL_IS_PROPARTE_HOMOTYPIC_SYNONYM_OF ||
                                    relQualifierFk == TAX_REL_IS_PROPARTE_HETEROTYPIC_SYNONYM_OF ){
                                if(relQualifierFk == TAX_REL_IS_PROPARTE_HOMOTYPIC_SYNONYM_OF){
                                    logger.warn("Homotypic pro parte synonyms not handled in CDM. Please add homotypie manually. RelPTID: " +  relPTaxonId);
                                }
                                //heterotypic we expect as the normal relationship, don't store explicitly
                                toTaxon.addProparteSynonym(fromTaxon, citation, microcitation);
                            }else if(relQualifierFk == TAX_REL_IS_PARTIAL_SYN_OF ||
                                    relQualifierFk == TAX_REL_IS_PARTIAL_HOMOTYPIC_SYNONYM_OF ||
                                    relQualifierFk == TAX_REL_IS_PARTIAL_HETEROTYPIC_SYNONYM_OF ){
                                if (relQualifierFk == TAX_REL_IS_PARTIAL_HOMOTYPIC_SYNONYM_OF ||
                                        relQualifierFk == TAX_REL_IS_PARTIAL_HETEROTYPIC_SYNONYM_OF){
                                    logger.warn("Homotypie and heterotypie not yet handled for partial synonyms");
                                }
                                toTaxon.addPartialSynonym(fromTaxon, citation, microcitation);
                            }else{
								handleAllRelatedTaxa(state, fromTaxon, classificationMap, fromRefFk);
								handleAllRelatedTaxa(state, toTaxon, classificationMap, treeRefFk);
								logger.warn("Unhandled taxon relationship: RelId:" + relPTaxonId + "; QualifierId: " + relQualifierFk);
							}
						}else if (isSynonymRelationship(relQualifierFk)){
							if (!(taxon1 instanceof Synonym)){
								logger.warn("Validated: Taxon (ID = " + taxon1.getId()+ ", RIdentifier = " + taxon1Id + ") can't be casted to Synonym");
								success = false;
								continue;
							}
							handleAllRelatedTaxa(state, toTaxon, classificationMap, treeRefFk);
							Synonym synonym = (Synonym)taxon1;
							if (synonym.getAcceptedTaxon()!= null){
							    logger.warn("RelID: " + relPTaxonId + ". Synonym ("+taxon1Id +") already has an accepted taxon. Create clone.");
							    synonym = synonym.clone();
							}
							makeSynRel(state, relQualifierFk, toTaxon, synonym, citation, microcitation);

							if (relQualifierFk == TAX_REL_IS_SYNONYM_OF ||
									relQualifierFk == TAX_REL_IS_HOMOTYPIC_SYNONYM_OF ||
									relQualifierFk == TAX_REL_IS_HETEROTYPIC_SYNONYM_OF){
							}else{
								success = false;
								logger.warn("Synonym relationship type not yet implemented: " + relQualifierFk);
							}
							//
							notes = handleSynonymNotes(state, toTaxon, synonym, notes, relPTaxonId);
						}else if (isConceptRelationship){
							ResultWrapper<Boolean> isInverse = ResultWrapper.NewInstance(false);
							ResultWrapper<Boolean> isDoubtful = ResultWrapper.NewInstance(false);
							try {
								TaxonRelationshipType relType = BerlinModelTransformer.taxonRelId2TaxonRelType(relQualifierFk, isInverse, isDoubtful);

								if (! (taxon1 instanceof Taxon)){
									success = false;
									logger.error("TaxonBase (ID = " + taxon1.getId()+ ", RIdentifier = " + taxon1Id + ") can't be casted to Taxon");
								}else{
									Taxon fromTaxon = (Taxon)taxon1;
									if (isInverse.getValue() == true){
										Taxon tmp = fromTaxon;
										fromTaxon = toTaxon;
										toTaxon = tmp;
									}
									taxonRelationship = fromTaxon.addTaxonRelation(toTaxon, relType, citation, microcitation);
									handleAllRelatedTaxa(state, toTaxon, classificationMap, treeRefFk);
									handleAllRelatedTaxa(state, fromTaxon, classificationMap, fromRefFk);
									if (isDoubtful.getValue() == true){
										((TaxonRelationship)taxonRelationship).setDoubtful(true);
									}
								}
							} catch (UnknownCdmTypeException e) {
								logger.warn("TaxonRelationShipType " + relQualifierFk + " (conceptRelationship) not yet implemented");
								 success = false;
							}
						}else {
							logger.warn("TaxonRelationShipType " + relQualifierFk + " not yet implemented: RelPTaxonId = " + relPTaxonId );
							success = false;
						}

						if (taxonRelationship != null && isNotBlank(notes)){
						    doNotes(taxonRelationship, notes);
						}
						if (isNotBlank(notes)){
						    logger.warn("Notes in RelPTaxon should all be handled explicitly and should not exist as notes anymore. RelID: " + relPTaxonId + ". Note: " + notes);
						}
						taxaToSave.add(taxon2);

						//TODO
						//etc.
					}else{
						if (taxon2 != null && taxon1 == null){
							logger.warn("First taxon ("+taxon1Id+") for RelPTaxon " + relPTaxonId + " does not exist in store. RelType: " + relQualifierFk);
						}else if (taxon2 == null && taxon1 != null){
							logger.warn("Second taxon ("+taxon2Id +") for RelPTaxon " + relPTaxonId + " does not exist in store. RelType: " + relQualifierFk);
						}else{
							logger.warn("Both taxa ("+taxon1Id+","+taxon2Id +") for RelPTaxon " + relPTaxonId + " do not exist in store. RelType: " + relQualifierFk);
						}

						success = false;
					}
				} catch (Exception e) {
					logger.error("Exception occurred when trying to handle taxon relationship " + relPTaxonId + " relQualifierFK " + relQualifierFk + " (" + taxon1Id + ","+ taxon2Id + "): " + e.getMessage());
					e.printStackTrace();
				}
			}
		}catch(SQLException e){
			throw new RuntimeException(e);
		}
		logger.info("Taxa to save: " + taxaToSave.size());
		partitioner.startDoSave();
		getTaxonService().saveOrUpdate(taxaToSave);
		classificationMap = null;
		taxaToSave = null;

		return success;
	}

    private boolean makeProvisional(ResultSet rs){
        try {
            return hasProvisional? rs.getBoolean("Provisional") : false;
        } catch (SQLException e) {
            hasProvisional = false;
            return false;
        }
    }

    private String handleSynonymNotes(BerlinModelImportState state, Taxon toTaxon, Synonym synonym, String notes, int relId) {
        if (state.getConfig().isEuroMed() && isNotBlank(notes)){
            notes = notes.trim();
            if (notes.startsWith("[non ") && notes.endsWith("]")){
                notes = notes.substring(5, notes.length()-1).trim();
                String[] splits = notes.split(", nec ");
                for (String split : splits){
                    String nameStr = split.replace("<i>", "").replace("</i>", "");
                    NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();
                    TaxonName name;
                    NomenclaturalStatusType status = null;
                    if (nameStr.endsWith(", nom. rej.") || nameStr.endsWith(", nom. cons.")||nameStr.endsWith(", nom. illeg.")){
                        String statusStr = nameStr.endsWith(", nom. rej.")? ", nom. rej.":
                            nameStr.endsWith(", nom. cons.")? ", nom. cons.":
                            ", nom. illeg.";
                        nameStr = nameStr.replace(statusStr, "");
                        statusStr = statusStr.replace(", ", "");
                        try {
                            status = NomenclaturalStatusType.getNomenclaturalStatusTypeByAbbreviation(statusStr, null);
                        } catch (UnknownCdmTypeException e) {
                            logger.warn("NomStatusType not recognized: "+  statusStr + ", RelId: " +  relId);
                        }
                    }

                    if (nameStr.contains(",") || nameStr.contains(" in ") ){
                        name = parser.parseReferencedName(nameStr, state.getConfig().getNomenclaturalCode(), null);
                    }else if (nameStr.matches(".*\\s\\d{4}")){
                        String nameStr2 = nameStr.substring(0, nameStr.length() - 5).trim();
                        String yearStr = nameStr.substring(nameStr.length()-4);
                        name = (TaxonName)parser.parseFullName(nameStr2, state.getConfig().getNomenclaturalCode(), null);
                        Reference nomRef = name.getNomenclaturalReference();
                        if (nomRef == null){
                            nomRef = ReferenceFactory.newGeneric();
                            name.setNomenclaturalReference(nomRef);
                        }
                        nomRef.setDatePublished(TimePeriodParser.parseStringVerbatim(yearStr));
                    }else if (nameStr.endsWith(" 1831-1832")){
                        String nameStr2 = nameStr.substring(0, nameStr.length() - 10).trim();
                        name = (TaxonName)parser.parseFullName(nameStr2, state.getConfig().getNomenclaturalCode(), null);
                        Reference nomRef = name.getNomenclaturalReference();
                        if (nomRef == null){
                            nomRef = ReferenceFactory.newGeneric();
                            name.setNomenclaturalReference(nomRef);
                        }
                        nomRef.setDatePublished(TimePeriodParser.parseStringVerbatim("1831-1832"));
                    }else{
                        name = parser.parseReferencedName(nameStr, state.getConfig().getNomenclaturalCode(), null);
                    }
                    if (name.isProtectedTitleCache() || name.isProtectedNameCache()
                            || name.getNomenclaturalReference() != null && (name.getNomenclaturalReference().isProtectedAbbrevTitleCache()|| name.getNomenclaturalReference().isProtectedTitleCache() )){
                        logger.warn("Blocking name for synonym relation could not be parsed: " + nameStr + ", RelId: "+ relId);
                    }
                    if (status != null){
                        name.addStatus(NomenclaturalStatus.NewInstance(status));
                    }
                    synonym.getName().addRelationshipFromName(name, NameRelationshipType.BLOCKING_NAME_FOR(), null, null, null, null);

                    getNameService().saveOrUpdate(name);
                }
                return null;
            }else{
                return notes;
            }
        }else{
            return notes;
        }
    }

    private void handleAllRelatedTaxa(BerlinModelImportState state, Taxon taxon,
            Map<Integer, Classification> classificationMap, Integer secRefFk) {
		if (taxon.getTaxonNodes().size() > 0){
			return;
		}else{
			Classification classification = getClassificationTree(state, classificationMap, secRefFk);
			classification.addChildTaxon(taxon, null, null);
		}
	}

	@Override
	protected void doInvoke(BerlinModelImportState state){
		try {
			makeClassifications(state);
			super.doInvoke(state);
			makeFlatClassificationTaxa(state);
			return;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

	}


	private void makeFlatClassificationTaxa(BerlinModelImportState state) {
		//Note: this part still does not use partitions
		logger.info("Flat classifications start");
		TransactionStatus txStatus = startTransaction();
		if (! state.getConfig().isIncludeFlatClassifications()){
			return;
		}
		String sql = " SELECT pt.PTRefFk AS secRefFk, pt.RIdentifier " +
						" FROM PTaxon AS pt " +
							" LEFT OUTER JOIN RelPTaxon ON pt.PTNameFk = RelPTaxon.PTNameFk2 AND pt.PTRefFk = RelPTaxon.PTRefFk2 " +
							"  LEFT OUTER JOIN RelPTaxon AS RelPTaxon_1 ON pt.PTNameFk = RelPTaxon_1.PTNameFk1 AND pt.PTRefFk = RelPTaxon_1.PTRefFk1 " +
						" WHERE (RelPTaxon_1.RelQualifierFk IS NULL) AND (dbo.RelPTaxon.RelQualifierFk IS NULL) " +
						" ORDER BY pt.PTRefFk "	;
		ResultSet rs = state.getConfig().getSource().getResultSet(sql);
		Map<Object, Map<String, ? extends CdmBase>> maps = getRelatedObjectsForFlatPartition(rs);

		Map<String, TaxonBase> taxonMap = (Map<String, TaxonBase>) maps.get(BerlinModelTaxonImport.NAMESPACE);
		Map<Integer, Classification> classificationMap = new HashMap<>();

		rs = state.getConfig().getSource().getResultSet(sql);
		try {
			while (rs.next()){
				Integer treeRefFk = rs.getInt("secRefFk");
				String taxonId = rs.getString("RIdentifier");
				Classification classification = getClassificationTree(state, classificationMap, treeRefFk);
				TaxonBase<?> taxon = taxonMap.get(taxonId);
				if (taxon == null){
					String message = "TaxonBase for taxon id (%s) not found in taxonMap";
					logger.warn(String.format(message, taxonId, taxonId));
				}else if (taxon.isInstanceOf(Taxon.class)){
					classification.addChildTaxon(CdmBase.deproxy(taxon, Taxon.class), null, null);
				}else{
					String message = "TaxonBase for taxon is not of class Taxon but %s (RIdentifier %s)";
					logger.warn(String.format(message, taxon.getClass(), taxonId));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		commitTransaction(txStatus);
		logger.info("Flat classifications end");

	}

	@Override
	protected String getIdQuery(BerlinModelImportState state) {
		if (state.getConfig().getRelTaxaIdQuery() != null){
			return state.getConfig().getRelTaxaIdQuery();
		}else{
			return super.getIdQuery(state);
		}
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> taxonIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
//			Set<String> classificationIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "taxon1Id");
				handleForeignKey(rs, taxonIdSet, "taxon2Id");
//				handleForeignKey(rs, classificationIdSet, "treeRefFk");
				handleForeignKey(rs, referenceIdSet, "RelRefFk");
	}

			//taxon map
			nameSpace = BerlinModelTaxonImport.NAMESPACE;
			idSet = taxonIdSet;
			@SuppressWarnings("rawtypes")
            Map<String, TaxonBase> taxonMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, nameSpace);
			result.put(nameSpace, taxonMap);

//			//tree map
//			nameSpace = "Classification";
//			idSet = classificationIdSet;
//			Map<String, Classification> treeMap = getCommonService().getSourcedObjectsByIdInSourceC(Classification.class, idSet, nameSpace);
//			result.put(cdmClass, treeMap);
//			Set<UUID> treeUuidSet = state
//			getClassificationService().find(uuidSet);
//
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


	private Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForFlatPartition( ResultSet rs) {
		String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> taxonIdSet = new HashSet<>();
//			Set<String> classificationIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "RIdentifier");
//				handleForeignKey(rs, classificationIdSet, "treeRefFk");
			}

			//taxon map
			nameSpace = BerlinModelTaxonImport.NAMESPACE;
			idSet = taxonIdSet;
			@SuppressWarnings("rawtypes")
            Map<String, TaxonBase> taxonMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, nameSpace);
			result.put(nameSpace, taxonMap);

//			//tree map
//			nameSpace = "Classification";
//			cdmClass = Classification.class;
//			idSet = classificationIdSet;
//			Map<String, Classification> treeMap = (Map<String, Classification>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
//			result.put(cdmClass, treeMap);
//			Set<UUID> treeUuidSet = state
//			getClassificationService().find(uuidSet);
//

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}


	private void makeSynRel (BerlinModelImportState state, int relQualifierFk, Taxon toTaxon, Synonym synonym, Reference citation, String microcitation){
		if (state.getConfig().isWarnForDifferingSynonymReference() && citation != null && !citation.equals(synonym.getSec())){
		    logger.warn("A synonym relationship citation is given and differs from synonym secundum. This can not be handled in CDM");
		}
		if (isNotBlank(microcitation)  && !microcitation.equals(synonym.getSecMicroReference())){
            logger.warn("A synonym relationship microcitation is given and differs from synonym secundum micro reference. This can not be handled in CDM");
        }
	    if (relQualifierFk == TAX_REL_IS_HOMOTYPIC_SYNONYM_OF ||
				relQualifierFk == TAX_REL_IS_PROPARTE_HOMOTYPIC_SYNONYM_OF ||
				relQualifierFk == TAX_REL_IS_PARTIAL_HOMOTYPIC_SYNONYM_OF){
			toTaxon.addHomotypicSynonym(synonym);
		}else if (relQualifierFk == TAX_REL_IS_HETEROTYPIC_SYNONYM_OF ||
				relQualifierFk == TAX_REL_IS_PROPARTE_HETEROTYPIC_SYNONYM_OF ||
				relQualifierFk == TAX_REL_IS_PARTIAL_HETEROTYPIC_SYNONYM_OF){
			toTaxon.addSynonym(synonym, SynonymType.HETEROTYPIC_SYNONYM_OF());
		}else if (relQualifierFk == TAX_REL_IS_SYNONYM_OF ||
				relQualifierFk == TAX_REL_IS_PROPARTE_SYN_OF ||
				relQualifierFk == TAX_REL_IS_PARTIAL_SYN_OF){
			toTaxon.addSynonym(synonym, SynonymType.SYNONYM_OF());
		}else{
			logger.warn("SynonymyRelationShipType could not be defined for relQualifierFk " + relQualifierFk + ". 'Unknown'-Type taken instead.");
			toTaxon.addSynonym(synonym, SynonymType.SYNONYM_OF());
		}
		return;

	}

	private  boolean isSynonymRelationship(int relQualifierFk){
		if (relQualifierFk == TAX_REL_IS_SYNONYM_OF ||
			relQualifierFk == TAX_REL_IS_HOMOTYPIC_SYNONYM_OF ||
			relQualifierFk == TAX_REL_IS_HETEROTYPIC_SYNONYM_OF ||
			relQualifierFk == TAX_REL_IS_PROPARTE_SYN_OF ||
			relQualifierFk == TAX_REL_IS_PARTIAL_SYN_OF ||
			relQualifierFk == TAX_REL_IS_PROPARTE_HOMOTYPIC_SYNONYM_OF ||
			relQualifierFk == TAX_REL_IS_PROPARTE_HETEROTYPIC_SYNONYM_OF ||
			relQualifierFk == TAX_REL_IS_PARTIAL_HOMOTYPIC_SYNONYM_OF ||
			relQualifierFk == TAX_REL_IS_PARTIAL_HETEROTYPIC_SYNONYM_OF
		){
			return true;
		}else{
			return false;
		}
	}

	private  boolean isTaxonRelationship(int relQualifierFk){
		if (relQualifierFk == TAX_REL_IS_INCLUDED_IN ||
				relQualifierFk == TAX_REL_IS_MISAPPLIED_NAME_OF){
			return true;
		}else if (relQualifierFk == TAX_REL_IS_PROPARTE_SYN_OF ||
	                relQualifierFk == TAX_REL_IS_PARTIAL_SYN_OF ||
	                relQualifierFk == TAX_REL_IS_PROPARTE_HOMOTYPIC_SYNONYM_OF ||
	                relQualifierFk == TAX_REL_IS_PROPARTE_HETEROTYPIC_SYNONYM_OF ||
	                relQualifierFk == TAX_REL_IS_PARTIAL_HOMOTYPIC_SYNONYM_OF ||
	                relQualifierFk == TAX_REL_IS_PARTIAL_HETEROTYPIC_SYNONYM_OF
                    ){
	            return true;
		}else{
			return false;
		}
	}

	private TaxonNode makeTaxonomicallyIncluded(BerlinModelImportState state, Map<Integer, Classification> classificationMap,
	        int treeRefFk, Taxon child, Taxon parent, Reference citation, String microCitation, Boolean provisional){
		Classification tree = getClassificationTree(state, classificationMap, treeRefFk);
		TaxonNode result = tree.addParentChild(parent, child, citation, microCitation);
		if (provisional){
		    result.setStatus(TaxonNodeStatus.DOUBTFUL);
		}
		return result;
	}

	private Classification getClassificationTree(BerlinModelImportState state, Map<Integer, Classification> classificationMap, int treeRefFk) {
		if (state.getConfig().isUseSingleClassification()){
			if (state.getConfig().getSourceSecId() != null){
				treeRefFk = (Integer)state.getConfig().getSourceSecId();
			}else{
				treeRefFk = 1;
			}
		}
		Classification tree = classificationMap.get(treeRefFk);
		if (tree == null){
			UUID treeUuid = state.getTreeUuidByIntTreeKey(treeRefFk);
			if (treeUuid == null){
				throw new IllegalStateException("treeUUID does not exist in state for " + treeRefFk );
			}
			tree = getClassificationService().find(treeUuid);
			classificationMap.put(treeRefFk, tree);
		}
		if (tree == null){
			throw new IllegalStateException("Tree for ToTaxon reference " + treeRefFk + " does not exist.");
		}
		return tree;
	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelTaxonRelationImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(BerlinModelImportState state){
		return ! state.getConfig().isDoRelTaxa();
	}
}
