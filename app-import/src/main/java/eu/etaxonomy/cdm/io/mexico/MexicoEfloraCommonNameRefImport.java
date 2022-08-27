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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.DescriptionElementSource;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public class MexicoEfloraCommonNameRefImport extends MexicoEfloraImportBase {

    private static final long serialVersionUID = -1712022169834400067L;
    private static final Logger logger = LogManager.getLogger();

	protected static final String NAMESPACE = "CommonNameRef";

	private static final String pluralString = "common name sources";
	private static final String dbTableName = "Eflora_RelBiblioNomComun";

	public MexicoEfloraCommonNameRefImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(MexicoEfloraImportState state) {
		String sql = " SELECT IdCombinado "
		        + " FROM " + dbTableName
		        + " ORDER BY IdCombinado ";
		return sql;
	}

	@Override
	protected String getRecordQuery(MexicoEfloraImportConfigurator config) {
		String sqlSelect = " SELECT * ";
		String sqlFrom = " FROM " + dbTableName;
		String sqlWhere = " WHERE ( IdCombinado IN (" + ID_LIST_TOKEN + ") )";

		String strRecordQuery =sqlSelect + " " + sqlFrom + " " + sqlWhere ;
		return strRecordQuery;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, MexicoEfloraImportState state) {

	    boolean success = true ;

	    @SuppressWarnings("unchecked")
        Map<String, CommonTaxonName> commonNameMap = partitioner.getObjectMap(MexicoEfloraCommonNameImport.NAMESPACE);

	    @SuppressWarnings("unchecked")
	    Map<String, Reference> referenceMap = partitioner.getObjectMap(MexicoEfloraReferenceImportBase.NAMESPACE);

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){

			//	if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("PTaxa handled: " + (i-1));}

				//create TaxonName element
				String idCombi = rs.getString("IdCombinado");
				int idBibliografia = rs.getInt("IdBibliografia");
				String observaciones = rs.getString("Observaciones");
				//needed?
//				String idNomComun = rs.getString("IdNomComun");

			    try {
			        CommonTaxonName commonName = commonNameMap.get(idCombi);

    				Reference ref = referenceMap.get(String.valueOf(idBibliografia));
    				String detail = null;

    				if (commonName != null) {
    				    DescriptionElementSource source = commonName.addPrimaryTaxonomicSource(ref, detail);
    				    if (source!= null) {
    				        TaxonName nameUsedInSource = getNameUsedInSource(state, observaciones, commonName, ref);
    				        source.setNameUsedInSource(nameUsedInSource);
    				    } else {
    				        logger.warn("Source not found for " + idCombi + " and bibID: " + idBibliografia);
    				    }
    				}else {
    				    logger.warn("CommonName not found for " + idCombi);
    				}

					partitioner.startDoSave();
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

		logger.warn("Finished partition");

		return success;
	}

    private TaxonName getNameUsedInSource(MexicoEfloraImportState state, String observaciones, CommonTaxonName commonName, Reference ref) {
        if (observaciones != null) {
            if (observaciones.matches("^(reportada |\\()?como .*")){
                String nameStr = observaciones
                        .replaceAll("^(reportada |\\()?como ", "")
                        .replaceAll("\\)$", "");
                UUID nameUuid = state.getNameMap().get(nameStr);
                TaxonName name = getName(state, nameUuid);
                if (name == null) {
                    String taxon =  CdmBase.deproxy(commonName.getInDescription(),TaxonDescription.class).getTaxon().getName().getTitleCache();
                    logger.warn("Name in source ("+observaciones+") could not be found for " + taxon + "-" + commonName.getLanguage().getLabel() + "-" + commonName.getArea().getLabel() + "-" + ref.getTitleCache());
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
			Set<String> commonNameIdSet = new HashSet<>();
            Set<String> referenceIdSet = new HashSet<>();
            while (rs.next()){
                handleForeignKey(rs, referenceIdSet, "IdBibliografia");
                handleForeignKey(rs, commonNameIdSet, "IdCombinado");
            }

			//common name map
			nameSpace = MexicoEfloraCommonNameImport.NAMESPACE;
			Map<UUID,String> commonNameUuidMap = new HashMap<>();
			commonNameIdSet.stream().forEach(cnId->commonNameUuidMap.put(state.getCommonNameMap().get(cnId), cnId));
			@SuppressWarnings({ "rawtypes", "unchecked" })
            List<CommonTaxonName> commonNames = (List)getDescriptionElementService().find(commonNameUuidMap.keySet());
			Map<String, CommonTaxonName> commonNameMap = new HashMap<>();
			commonNames.stream().forEach(cn->commonNameMap.put(commonNameUuidMap.get(cn.getUuid()), cn));
			result.put(nameSpace, commonNameMap);

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