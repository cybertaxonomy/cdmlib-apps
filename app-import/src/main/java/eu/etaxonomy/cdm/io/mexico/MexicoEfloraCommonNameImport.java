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

import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.TermType;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public class MexicoEfloraCommonNameImport extends MexicoEfloraImportBase {

    private static final long serialVersionUID = 8616047381536678637L;
    private static final Logger logger = Logger.getLogger(MexicoEfloraCommonNameImport.class);

	protected static final String NAMESPACE = "CommonNames";

	private static final String pluralString = "common names";
	private static final String dbTableName = "Eflora_NombresComunes4CDM";

	private Map<String,Language> languageMap = new HashMap<>();

	public MexicoEfloraCommonNameImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(MexicoEfloraImportState state) {
		String sql = " SELECT IdCombinado "
		        + " FROM " + dbTableName
		        + " ORDER BY IdCat, IdCombinado ";
		return sql;
	}

	@Override
	protected String getRecordQuery(MexicoEfloraImportConfigurator config) {
		String sqlSelect = " SELECT cn.*, t.uuid taxonUuid ";
		String sqlFrom = " FROM " + dbTableName + " cn "
		                + " LEFT JOIN " + MexicoEfloraTaxonImport.dbTableName + " t ON cn.IdCAT = t.IdCAT ";
		String sqlWhere = " WHERE ( IdCombinado IN (" + ID_LIST_TOKEN + ") )";

		String strRecordQuery =sqlSelect + " " + sqlFrom + " " + sqlWhere ;
		return strRecordQuery;
	}

	@Override
    protected void doInvoke(MexicoEfloraImportState state) {
        createLanguageMap(state);
        super.doInvoke(state);
    }

    private void createLanguageMap(MexicoEfloraImportState state) {
        TermVocabulary<Language> voc = createLanguagesVoc(state);

        String sql = "SELECT * FROM cv4_Controlled_vocabulary_for_languages ";
        ResultSet rs = state.getConfig().getSource().getResultSet(sql);
        try {
            while (rs.next()) {
                String lang = rs.getString("Lengua");
                Language language = Language.NewInstance(lang, lang, null, Language.SPANISH_CASTILIAN());
                voc.addTerm(language);
                getTermService().save(language);  //not sure if necessary
                languageMap.put(lang, language);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private TermVocabulary<Language> createLanguagesVoc(MexicoEfloraImportState state) {
        URI termSourceUri = null;
        String label = "Mexican Languages";
        String description = "Mexican languages as used by the CONABIO database";
        TermVocabulary<Language> languagesVoc = TermVocabulary.NewInstance(TermType.Language, Language.class,
                description, label, null, termSourceUri);
        languagesVoc.setUuid(MexicoConabioTransformer.uuidMexicanLanguagesVoc);
        this.getVocabularyService().save(languagesVoc);

        return languagesVoc;
    }

    @Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, MexicoEfloraImportState state) {

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
				String idCombi = rs.getString("IdCombinado");
//			    String idNomComun = rs.getString("IdNomComun");
			    String taxonUuid = rs.getString("taxonUuid");

			    String nomComunStr = rs.getString("NomComun");
			    String lenguaStr = rs.getString("Lengua");
			    //TODO handle country in
//			    String paisStr = rs.getString("Pais");
//			    String estadoStr = rs.getString("Estado");
			    int idRegion = rs.getInt("IdRegion");

			    try {
    				TaxonBase<?> taxonBase = taxonMap.get(taxonUuid);
    				Taxon taxon;
    				if (taxonBase.isInstanceOf(Taxon.class)) {
    				    taxon = CdmBase.deproxy(taxonBase, Taxon.class);
    				}else {
    				    logger.warn(idCombi + ": Taxon is not accepted: " + taxonUuid);
    				    continue;
    				}

    				Language language = getLanguage(state, lenguaStr);
    				NamedArea area = getArea(state, idRegion);
    				CommonTaxonName commonName = CommonTaxonName.NewInstance(nomComunStr,
    				        language, area);
    				//TODO
    				Reference ref = null;
    				TaxonDescription description = this.getTaxonDescription(taxon, ref,
    				        false, true);
    				description.addElement(commonName);

    				state.getCommonNameMap().put(idCombi, commonName);

					partitioner.startDoSave();
					taxaToSave.add(taxonBase);
				} catch (Exception e) {
					logger.warn("An exception (" +e.getMessage()+") occurred when trying to create common name for id " + idCombi + ".");
					success = false;
				}
			}
		} catch (Exception e) {
			logger.error("SQLException:" +  e);
			return false;
		}

		getTaxonService().save(taxaToSave);
		return success;
	}

    private NamedArea getArea(MexicoEfloraImportState state, Integer idRegion) {
        NamedArea area = state.getAreaMap().get(idRegion);
        if (idRegion != null && area == null) {
            logger.warn("Area not found: " + idRegion);
        }
        return area;
    }

    private Language getLanguage(MexicoEfloraImportState state, String lenguaStr) {
        Language language = languageMap.get(lenguaStr);
        if (isNotBlank(lenguaStr) && language == null) {
            logger.warn("Language not found: " + lenguaStr);
        }
        return language;
    }

    @Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, MexicoEfloraImportState state) {

        String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
            Set<UUID> taxonIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			while (rs.next()){
                handleForeignUuidKey(rs, taxonIdSet, "taxonUuid");
//				handleForeignKey(rs, referenceIdSet, "PTRefFk");
			}

            //taxon map
            nameSpace = MexicoEfloraTaxonImport.NAMESPACE;
            @SuppressWarnings("rawtypes")
            Map<String, TaxonBase> taxonMap = new HashMap<>();
            @SuppressWarnings("rawtypes")
            List<TaxonBase> taxa = getTaxonService().find(taxonIdSet);
            taxa.stream().forEach(t->taxonMap.put(t.getUuid().toString(), t));
            result.put(nameSpace, taxonMap);

			//reference map
			nameSpace = MexicoEfloraRefArticlesImport.NAMESPACE;
			idSet = referenceIdSet;
			Map<String, Reference> referenceMap = getCommonService().getSourcedObjectsByIdInSourceC(Reference.class, idSet, nameSpace);
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