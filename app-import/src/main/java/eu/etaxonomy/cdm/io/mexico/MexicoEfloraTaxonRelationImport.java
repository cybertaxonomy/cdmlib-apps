/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.mexico;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public class MexicoEfloraTaxonRelationImport extends MexicoEfloraImportBase {

    private static final long serialVersionUID = 8616047381536678637L;
    private static final Logger logger = Logger.getLogger(MexicoEfloraTaxonRelationImport.class);

	protected static final String NAMESPACE = "TaxonRelation";

	private static final String pluralString = "Taxon relations";
	private static final String dbTableName = "EFlora_Taxonomia4CDM2";


	public MexicoEfloraTaxonRelationImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(MexicoEfloraImportState state) {
		String sql = " SELECT IdCAT "
		        + " FROM " + dbTableName + " t "
		        + " LEFT JOIN cv1_Controlled_vocabulary_for_name_Ranks r ON t.CategoriaTaxonomica = r.NombreCategoriaTaxonomica "
		        + " WHERE t.IdCAT_AscendenteHerarquico4CDM NOT IN ('2PLANT','79217TRACH') "
		        + " ORDER BY r.Nivel1, IdCAT ";
		return sql;
	}

	@Override
	protected String getRecordQuery(MexicoEfloraImportConfigurator config) {
		String sqlSelect = " SELECT t.*, acc.uuid accUuid, p.uuid pUuid, bas.uuid basUuid, p.IDCat pID ";
		String sqlFrom = " FROM " + dbTableName + " t "
		        + " LEFT JOIN "+dbTableName+" acc ON acc.IdCat = t.IdCATRel "
		        + " LEFT JOIN "+dbTableName+" p ON p.IdCat = t.IdCAT_AscendenteHerarquico4CDM "
		        + " LEFT JOIN "+dbTableName+" bas ON bas.IdCat = t.IdCAT_BasNomOrig "
		        ;
		String sqlWhere = " WHERE ( t.IdCAT IN (" + ID_LIST_TOKEN + ") )";

		String strRecordQuery =sqlSelect + " " + sqlFrom + " " + sqlWhere ;
		return strRecordQuery;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, MexicoEfloraImportState state) {
	    classification = null;
	    boolean success = true ;
	    @SuppressWarnings("rawtypes")
        Set<TaxonBase> taxaToSave = new HashSet<>();
		@SuppressWarnings("unchecked")
        Map<String, TaxonBase<?>> taxonMap = partitioner.getObjectMap(MexicoEfloraTaxonImport.NAMESPACE);

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){

			//	if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("PTaxa handled: " + (i-1));}

				//create TaxonName element
			    String taxonId = rs.getString("IdCAT");
                UUID uuid = UUID.fromString(rs.getString("uuid"));
			    String accUuidStr = rs.getString("accUuid"); //accepted for synonym
			    String parentUuidStr = rs.getString("pUuid");  //parent
				String basUuidStr = rs.getString("basUuid"); //basionyms of accepted taxa
				String parentId = rs.getString("pID");

				if ("2PLANT".equals(parentId) || "79217TRACH".equals(parentId)) {
				    parentUuidStr = null;
				}

				TaxonBase<?> taxonBase = taxonMap.get(uuid.toString());

				try {
				    if (taxonBase.isInstanceOf(Synonym.class) && accUuidStr != null) {
				        Synonym syn = CdmBase.deproxy(taxonBase, Synonym.class);
				        TaxonBase<?> related = taxonMap.get(accUuidStr);
				        if (!related.isInstanceOf(Taxon.class)){
				            logger.warn(taxonId + ":  Accepted taxon for synonym is not accepted: " + accUuidStr);
				        }else {
				            Taxon acc = CdmBase.deproxy(related, Taxon.class);
				            //TODO type
				            acc.addSynonym(syn, SynonymType.SYNONYM_OF());
				        }
				    }else if (taxonBase.isInstanceOf(Taxon.class) && parentUuidStr != null) {
                        Taxon child = CdmBase.deproxy(taxonBase, Taxon.class);
                        TaxonBase<?> parentBase = taxonMap.get(parentUuidStr);
                        if (!parentBase.isInstanceOf(Taxon.class)){
                            logger.warn(taxonId + ":  Parent is not accepted: " + parentUuidStr);
                        }else {
                            Taxon parent = CdmBase.deproxy(parentBase, Taxon.class);
                            //TODO
                            Reference parentChildReference = null;
                            getClassification(state).addParentChild(parent, child, parentChildReference, null);
                        }
                    }else {
                        logger.warn(taxonId + ": Taxon has no valid relationship: " + taxonBase.getName().getTitleCache());
                        if (taxonBase.isInstanceOf(Taxon.class)) {
                            Taxon taxon = CdmBase.deproxy(taxonBase, Taxon.class);
                            getClassification(state).addChildTaxon(taxon, null);
                        }
                    }

				    if (basUuidStr != null) {
				        TaxonName name = taxonBase.getName();
				        TaxonBase<?> basionymTaxon = taxonMap.get(basUuidStr);
				        name.addBasionym(basionymTaxon.getName());
				        //TODO synrel type
				    }

					partitioner.startDoSave();
					taxaToSave.add(taxonBase);
				} catch (Exception e) {
					logger.warn("An exception (" +e.getMessage()+") occurred when trying to create relation for id " + taxonId + ". Relation could not be saved.");
					success = false;
				}
			}
		} catch (Exception e) {
			logger.error("SQLException:" +  e);
			return false;
		}

		getTaxonService().save(taxaToSave);
		logger.warn("Partition finished");
		return success;
	}

	Classification classification;
    private Classification getClassification(MexicoEfloraImportState state) {
        if (classification == null) {
            classification = getClassificationService().find(state.getConfig().getClassificationUuid());
            if (classification == null) {
                classification = Classification.NewInstance(state.getConfig().getClassificationName());
                classification.setUuid(state.getConfig().getClassificationUuid());
                classification.setTitleCache(state.getConfig().getClassificationName(), true);
                getClassificationService().save(classification);
            }
        }
        return classification;
    }

    @Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, MexicoEfloraImportState state) {

        String nameSpace;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<UUID> taxonIdSet = new HashSet<>();
			while (rs.next()){
                handleForeignUuidKey(rs, taxonIdSet, "uuid");
                handleForeignUuidKey(rs, taxonIdSet, "accUuid");
                handleForeignUuidKey(rs, taxonIdSet, "pUuid");
                handleForeignUuidKey(rs, taxonIdSet, "basUuid");
			}

            //taxon map
            nameSpace = MexicoEfloraTaxonImport.NAMESPACE;
            @SuppressWarnings("rawtypes")
            Map<String, TaxonBase> taxonMap = new HashMap<>();
            @SuppressWarnings("rawtypes")
            List<TaxonBase> taxa = getTaxonService().find(taxonIdSet);
            taxa.stream().forEach(t->taxonMap.put(t.getUuid().toString(), t));
            result.put(nameSpace, taxonMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	protected String getTableName() {
		return dbTableName;
	}

	@Override
	public String getPluralString() {
		return pluralString;
	}

    @Override
    protected boolean doCheck(MexicoEfloraImportState state){
        return true;
    }

	@Override
	protected boolean isIgnore(MexicoEfloraImportState state){
		return ! state.getConfig().isDoTaxa();
	}
}