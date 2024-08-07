/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.globis;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.Country;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;


/**
 * @author a.mueller
 * @since 20.02.2010
 *
 * OPEN ISSUES:
 *
 * ...
 */
@Component
public class GlobisCommonNameImport  extends GlobisImportBase<Taxon> {

    private static final long serialVersionUID = 2462539699442149049L;
    private static final Logger logger = LogManager.getLogger();

	private int modCount = 10000;
	private static final String pluralString = "common names";
	private static final String dbTableName = "species_language";
	private static final Class<?> cdmTargetClass = Taxon.class;  //not needed

	public GlobisCommonNameImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}

	//dirty but acceptable for globis environment
	private Map<Integer,Reference> refMap = new HashMap<Integer,Reference>();

	@Override
	protected String getIdQuery() {
		String strRecordQuery =
			" SELECT ID " +
			" FROM " + dbTableName;
		return strRecordQuery;
	}


	@Override
	protected String getRecordQuery(GlobisImportConfigurator config) {
		String strRecordQuery =
			" SELECT * " +
			" FROM " + getTableName() + " sl " +
			" WHERE ( sl.ID IN (" + ID_LIST_TOKEN + ") )";
		return strRecordQuery;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, GlobisImportState state) {
		boolean success = true;

		@SuppressWarnings("rawtypes")
        Set<TaxonBase> objectsToSave = new HashSet<>();

		@SuppressWarnings("unchecked")
        Map<String, Taxon> taxonMap = partitioner.getObjectMap(TAXON_NAMESPACE);

		ResultSet rs = partitioner.getResultSet();

		try {

			int i = 0;

			//for each common name
            while (rs.next()){

        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}

        		Integer idTaxon = nullSafeInt(rs,"IDCurrentSpec");

        		try {

					//source ref
					Reference sourceRef = state.getTransactionalSourceReference();

					//common names
					Integer id = nullSafeInt(rs,"ID");
					String isoLang = rs.getString("ISO");
					String strCommonName = rs.getString("commonname");
					Integer refID = nullSafeInt(rs,"ReferenceID");
					String strCountryCode = rs.getString("Code2");


					Taxon taxon = taxonMap.get(String.valueOf(idTaxon));
					if (taxon == null){
						logger.warn("No taxon found for taxonId " + idTaxon);
					}else if (isBlank(strCommonName)){
						logger.warn("No common name string defined for common name ID: " + id);
					}else{
						Language language = getLanguage(isoLang);
						if (language == null){
							logger.warn("No language found for common name ID: " + id);
						}
						NamedArea area = Country.getCountryByIso3166A2(strCountryCode);
						if (area == null){
							logger.warn("No country found for common name ID: " + id);
						}

						TaxonDescription taxonDescription = getTaxonDescription(taxon, sourceRef, ! IMAGE_GALLERY,  CREATE);
						CommonTaxonName commonName = CommonTaxonName.NewInstance(strCommonName, language, area);
						taxonDescription.addElement(commonName);

						Reference ref = handleReference(state, refID);
						if (ref == null && refID != null){
							logger.warn("No reference found for common name ID: " + id);
						}else{
							commonName.addSource(OriginalSourceType.Import, String.valueOf(refID), "reference", sourceRef, null);
						}

						objectsToSave.add(taxon);
					}

				} catch (Exception e) {
					logger.warn("Exception in current_species: IDcurrentspec " + idTaxon + ". " + e.getMessage());
					e.printStackTrace();
				}

            }

			logger.warn(pluralString + " to save: " + objectsToSave.size());
			getTaxonService().save(objectsToSave);

			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}

	private Map<String,Language> languageMap = new HashMap<>();
	private Language getLanguage(String isoLang) {
		Language result = languageMap.get(isoLang);
		if (result == null){

			result = getTermService().getLanguageByIso(isoLang);
			if (result == null){
				logger.warn("No language found for iso code: " + isoLang);
			}
		}
		return result;

	}

	private Reference handleReference(GlobisImportState state, Integer refId){
		if (refId == null){
			return null;
		}
		Reference result = refMap.get(refId);

		if (result == null){
			try {
				String sql = "SELECT * FROM [references] WHERE ReferenceID = " + refId;
				ResultSet rs = state.getConfig().getSource().getResultSet(sql);
				rs.next();

				String authors = rs.getString("Author(s)");
				String title = rs.getString("Title");
				String details = rs.getString("Details");
				Integer year = nullSafeInt(rs, "year");
				result = ReferenceFactory.newGeneric();
				result.setTitleCache(details, true);
				result.setTitle(title);
				result.setDatePublished(VerbatimTimePeriod.NewVerbatimInstance(year));

				TeamOrPersonBase<?> author;
				String[] authorSplit = authors.split("&");
				if (authorSplit.length > 1){
					Team team = Team.NewInstance();
					author = team;
					for (String singleAuthor : authorSplit){
						Person person = makeSingleAuthor(singleAuthor);
						team.addTeamMember(person);
					}
				}else{
					author = makeSingleAuthor(authors);
				}

				result.setAuthorship(author);
				refMap.put(refId,result);
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}


		}

		return result;
	}

	private Person makeSingleAuthor(String authors) {
		Person result = Person.NewTitledInstance(authors);
		return result;
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, GlobisImportState state) {
		String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
		try{
			Set<String> taxonIdSet = new HashSet<>();

			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "IDCurrentSpec");
			}

			//taxon map
			nameSpace = TAXON_NAMESPACE;
			idSet = taxonIdSet;
			Map<String, Taxon> objectMap = getCommonService().getSourcedObjectsByIdInSourceC(Taxon.class, idSet, nameSpace);
			result.put(nameSpace, objectMap);


		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
    protected boolean doCheck(GlobisImportState state){
//		IOValidator<GlobisImportState> validator = new GlobisCurrentSpeciesImportValidator();
		return true;
	}

	@Override
	protected boolean isIgnore(GlobisImportState state){
		return ! state.getConfig().isDoCommonNames();
	}
}
