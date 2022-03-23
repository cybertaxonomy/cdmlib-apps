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
import eu.etaxonomy.cdm.model.description.DescriptionElementSource;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public class MexicoEfloraDistributionRefImport extends MexicoEfloraImportBase {

    private static final long serialVersionUID = -3358763003286536675L;
    private static final Logger logger = Logger.getLogger(MexicoEfloraDistributionRefImport.class);

	protected static final String NAMESPACE = "DistributionRef";

	private static final String pluralString = "distribution sources";
	private static final String dbTableName = "Eflora_RelRegionBibliografia";

	public MexicoEfloraDistributionRefImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(MexicoEfloraImportState state) {
		String sql = " SELECT IdDist "
		        + " FROM " + dbTableName
		        + " ORDER BY IdDist ";
		return sql;
	}

	@Override
	protected String getRecordQuery(MexicoEfloraImportConfigurator config) {
		String sqlSelect = " SELECT * ";
		String sqlFrom = " FROM " + dbTableName;
		String sqlWhere = " WHERE ( IdDist IN (" + ID_LIST_TOKEN + ") )";

		String strRecordQuery =sqlSelect + " " + sqlFrom + " " + sqlWhere ;
		return strRecordQuery;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, MexicoEfloraImportState state) {

	    boolean success = true ;

	    @SuppressWarnings("unchecked")
        Map<String, Distribution> distributionMap = partitioner.getObjectMap(MexicoEfloraDistributionImport.NAMESPACE);

	    @SuppressWarnings("unchecked")
        Map<String, Reference> referenceMap = partitioner.getObjectMap(MexicoEfloraReferenceImportBase.NAMESPACE);

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){

			//	if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("PTaxa handled: " + (i-1));}

				//create TaxonName element
				String idCombi = rs.getString("IdDist");
				int idBibliografia = rs.getInt("IdBibliografia");
			    String observaciones = rs.getString("Observaciones");

			    try {
    				Distribution distribution = distributionMap.get(idCombi);
    				if (distribution == null) {
    				    logger.warn("Distribution not found for " + idCombi);
    				    continue;
    				}
    				Reference ref = referenceMap.get(String.valueOf(idBibliografia));
    				String detail = null;

    				DescriptionElementSource source = distribution.addPrimaryTaxonomicSource(ref, detail);
                    if (source!= null) {
                        TaxonName nameUsedInSource = getNameUsedInSource(state, observaciones, distribution, ref);
                        source.setNameUsedInSource(nameUsedInSource);
                    } else {
                        logger.warn("Source not found for " + idCombi + " and bibID: " + idBibliografia);
                    }

					partitioner.startDoSave();
//					taxaToSave.add(taxonBase);
				} catch (Exception e) {
				    e.printStackTrace();
					logger.warn("An exception (" +e.getMessage()+") occurred when trying to create common name for id " + idCombi + ".");
					success = false;
				}
			}
		} catch (Exception e) {
			logger.error("SQLException:" +  e);
			return false;
		}

		logger.warn("Partition finished");
		return success;
	}

    private TaxonName getNameUsedInSource(MexicoEfloraImportState state, String observaciones, Distribution distribution, Reference ref) {
        if (observaciones != null) {
            if (observaciones.matches("^\\(?como .*")){
                String nameStr = observaciones
                        .replaceAll("^\\(?como ", "")
                        .replaceAll("\\)$", "").trim();
                if (nameStr.contains("registro obtenido a partir")) {
                    nameStr = nameStr.substring(0, nameStr.indexOf("registro obtenido a partir")).trim();
                }

                UUID nameUuid = state.getNameMap().get(nameStr);
                TaxonName name = getName(state, nameUuid);
                if (name == null) {
                    String taxon =  CdmBase.deproxy(distribution.getInDescription(),TaxonDescription.class).getTaxon().getName().getTitleCache();
                    logger.warn("Name in source ("+observaciones+") could not be found for " + taxon + " - Area: " + distribution.getArea().getLabel() + " - Biblio: " + ref.getTitleCache());
                }
                return name;
            }
        }
        return null;
    }

    //quick and dirty and slow
    private TaxonName getName(@SuppressWarnings("unused") MexicoEfloraImportState state, UUID nameUuid) {
        return getNameService().find(nameUuid);
    }

    @Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, MexicoEfloraImportState state) {

        String nameSpace;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> distributionIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			while (rs.next()){
			    handleForeignKey(rs, referenceIdSet, "IdBibliografia");
                handleForeignKey(rs,distributionIdSet, "IdDist");
			}

            //distribution map
            nameSpace = MexicoEfloraDistributionImport.NAMESPACE;
            Map<UUID,String> distributionUuidMap = new HashMap<>();
            distributionIdSet.stream().forEach(dId->distributionUuidMap.put(state.getDistributionMap().get(dId),dId));
            @SuppressWarnings({ "rawtypes", "unchecked" })
            List<Distribution> distributions = (List)getDescriptionElementService().find(distributionUuidMap.keySet());
            Map<String, Distribution> distributionMap = new HashMap<>();
            distributions.stream().forEach(d->distributionMap.put(distributionUuidMap.get(d.getUuid()), d));
            result.put(nameSpace, distributionMap);

            //reference map
            nameSpace = MexicoEfloraReferenceImportBase.NAMESPACE;
            Map<UUID,String> referenceUuidMap = new HashMap<>();
            referenceIdSet.stream().forEach(rId->referenceUuidMap.put(state.getReferenceUuidMap().get(Integer.valueOf(rId)), rId));
            List<Reference> references = getReferenceService().find(referenceUuidMap.keySet());
            Map<String, Reference> referenceMap = new HashMap<>();
            references.stream().forEach(r->referenceMap.put(referenceUuidMap.get(r.getUuid()), r));
            result.put(nameSpace, referenceMap);

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