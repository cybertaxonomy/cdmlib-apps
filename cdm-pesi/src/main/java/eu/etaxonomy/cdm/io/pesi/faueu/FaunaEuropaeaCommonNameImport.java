/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.faueu;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.CommonTaxonName;
import eu.etaxonomy.cdm.model.description.DescriptionElementSource;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;
import eu.etaxonomy.cdm.persistence.query.MatchMode;

/**
 * @author a.mueller
 * @since 01.07.2025
 */
@Component
public class FaunaEuropaeaCommonNameImport
            extends FaunaEuropaeaCommonNameImportBase {

    private static final long serialVersionUID = -7850991527750321133L;
    private static Logger logger = LogManager.getLogger();

	private static final String pluralString = "species";
	private static final String dbTableName = "[PESI_FaEu_vernaculars_export]";
	private static int countImported = 0;

	public FaunaEuropaeaCommonNameImport(){}

	protected String getIdQuery() {
		String result = " SELECT VerID "
		        + " FROM " + getTableName()
				+ " ORDER BY FullName, VerId ";
		return result;
	}

	protected String getRecordQuery(FaunaEuropaeaCommonNameImportConfigurator config) {
		String strRecordQuery =
				"   SELECT * "
				+ " FROM " + getTableName()
				+ " WHERE ( VerID IN (" + ID_LIST_TOKEN + ") )" +
			"";
		return strRecordQuery;
	}


    @Override
    protected void doInvoke(FaunaEuropaeaCommonNameImportState state){

        System.out.println("start make " + getPluralString() + " ...");
        FaunaEuropaeaCommonNameImportConfigurator config = state.getConfig();
        Source source = config.getSource();

        String strIdQuery = getIdQuery();
        String strRecordQuery = getRecordQuery(config);

        int recordsPerTransaction = config.getRecordsPerTransaction();
        try{
            ResultSetPartitioner<FaunaEuropaeaCommonNameImportState> partitioner = ResultSetPartitioner.NewInstance(source, strIdQuery, strRecordQuery, recordsPerTransaction);
            while (partitioner.nextPartition()){
                partitioner.doPartition(this, state);
            }
        } catch (SQLException e) {
            logger.error("SQLException:" +  e);
            state.setUnsuccessfull();
            return;
        }

        logger.info("end make " + getPluralString() + " ... " + getSuccessString(true));
        return;
    }

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner,
	        FaunaEuropaeaCommonNameImportState state) {

		boolean success = true;
		Reference sourceReference = state.getRelatedObject(NAMESPACE_REFERENCE, SOURCE_REFERENCE, Reference.class);
		ResultSet rs = partitioner.getResultSet();

		try {
			while (rs.next()){

				Integer id = rs.getInt("VerId");
				String commonName = rs.getString("VerName");
				String languageStr = rs.getString("LanguageCache");
				String source = rs.getString("VerSource");

				String fullName = rs.getString("fullName");
				String line = id + ": ";

				Taxon taxon;
//				TaxonName name;
				List<TaxonName> nameCandidates = getNameService().findNamesByNameCache(fullName, MatchMode.EXACT, null);
				if (nameCandidates.isEmpty()) {
				    logger.warn(line + "Name not found in FauEu: "+ fullName);
				    return false;
				}else {

				    List<String> excludedHomonyms = Arrays.asList(new String[] {
				            "Brachypoda Birshteyn, 1960",  //TODO unclear
				            "Branchiura Beddard, 1892",
				            "Ctenophora Meigen, 1803",
				            "Ctenophora (Grunow) D.M.Williams & Round, 1986",
				            "Digenea C.Agardh, 1822",
				            "Oxyporus Fabricius, 1775",  //the common name is for the fungus
				            "Solenopsis C. Presl",
				            "Cnestrum I. Hagen"
				    });
				    Set<Taxon> taxonSet = nameCandidates.stream()
				            .filter(n->!excludedHomonyms.contains(n.getTitleCache()))
				            .flatMap(n->n.getTaxa().stream())
				            .collect(Collectors.toSet());

				    Set<TaxonNode> nodes = taxonSet.stream()
				            .flatMap(t->t.getTaxonNodes().stream())
				            .collect(Collectors.toSet());
				    if (nodes.isEmpty()) {
	                    //name is not an accepted taxon in PESI
				        Set<Taxon> acceptedForSynonym = nameCandidates.stream()
                            .flatMap(n->n.getSynonyms().stream())
                            .map(s->s.getAcceptedTaxon())
                            .collect(Collectors.toSet());

	                    Set<Taxon> acceptedForPseudoSynonym = taxonSet.stream()
	                            .flatMap(t->t.getRelationsFromThisTaxon().stream())
	                            .filter(rel->TaxonRelationshipType.pseudoTaxonUuids().contains(rel.getType().getUuid()))
	                            .map(rel->rel.getToTaxon())
	                            .collect(Collectors.toSet());
	                    acceptedForSynonym.addAll(acceptedForPseudoSynonym);
				        Set<TaxonNode> synCandidates = acceptedForSynonym.stream()
				                .flatMap(t->t.getTaxonNodes().stream())
				                .collect(Collectors.toSet());

				        if (synCandidates.isEmpty()) {
				            logger.warn(line + "No accepted taxon found for synonym name: "+ fullName);
				            return false;
	                    } else if (synCandidates.size() == 1) {
	                        taxon = synCandidates.iterator().next().getTaxon();
	                    } else {
	                        logger.warn(line + "More than 1 accepted taxon found for synonym name: " + fullName);
	                        taxon = synCandidates.iterator().next().getTaxon();
	                    }
				    } else if (nodes.size() == 1) {
				        taxon = nodes.iterator().next().getTaxon();
				    } else {
                        logger.warn(line + "More than 1 name found with accepted taxon in DB: " + fullName);
                        taxon = nodes.iterator().next().getTaxon();
				    }
				}

				Language lang = getLanguage(state, languageStr, line);
				if (lang == null) {
				    logger.warn(line + "Language not recognized: " + languageStr);
				    return false;
				}
				if (!commonNameExists(taxon, lang, commonName)){
				    TaxonName name = null; //TODO
				    TaxonDescription desc = this.getTaxonDescription(taxon, !IMAGE_GALLERY, CREATE);
				    CommonTaxonName cn = CommonTaxonName.NewInstance(commonName, lang);
				    addSource(state, cn, name, sourceReference,source, id, line);
				    desc.addElement(cn);
				    countImported++;
				} else {
				    logger.debug(line + "Common name '" + commonName + "'/" + languageStr + " exists already for " + fullName);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			state.setSuccess(false);
			success = false;
		}

		logger.info("Imported: " + countImported);
		return success;
	}

	private Map<String, Reference> refMap = new HashMap<>();
	private Map<String, UUID> refUuidMap = new HashMap<>();

	private void addSource(FaunaEuropaeaCommonNameImportState state,
	        CommonTaxonName cn, TaxonName name, Reference importRef, String refStr, Integer verId, String line) {

	    if (StringUtils.isBlank(refStr)) {
            return;
        }

        Reference ref = refMap.get(refStr);
        if (ref == null) {
            UUID refUuid = refUuidMap.get(refStr);
            if (refUuid == null) {
                ref = ReferenceFactory.newGeneric();
                ref.setTitle(refStr);
                getReferenceService().save(ref);
                refUuidMap.put(refStr, ref.getUuid());
            } else {
                ref = getReferenceService().find(refUuid);
                if (ref == null) {
                    logger.warn(line + "Ref for uuid not found: " + refStr + "/" + refUuid);
                    return;
                }
            }
            refMap.put(refStr, ref);
        }

        //add source
        DescriptionElementSource primSource = cn.addPrimaryTaxonomicSource(ref);
        primSource.setNameUsedInSource(name);
        DescriptionElementSource importSource = cn.addImportSource(String.valueOf(verId), getTableName(),
                importRef, line);
        importSource.setNameUsedInSource(name);
    }

    private boolean commonNameExists(Taxon taxon, Language lang, String commonName) {
        return taxon.getDescriptions().stream()
            .flatMap(d->d.getElements().stream())
            .filter(deb->deb.isInstanceOf(CommonTaxonName.class))
            .map(deb->CdmBase.deproxy(deb, CommonTaxonName.class))
            .filter(ctn->commonName.equals(ctn.getName()) && lang.equals(ctn.getLanguage()))
            .findAny()
            .isPresent();
    }

    private Map<String, Language> languageMap = new HashMap<>();
    private Language getLanguage(FaunaEuropaeaCommonNameImportState state, String languageStr, String line) {
        if (languageMap.get(languageStr) != null) {
            return languageMap.get(languageStr);
        }else {
            Language language = getTermService().getLanguageByLabel(languageStr);
            if (language == null) {
                try {
                    language = state.getTransformer().getLanguageByKey(languageStr);
                } catch (UndefinedTransformerMethodException e) {
                    e.printStackTrace();
                }
            }

            if  (language == null) {
                logger.warn(line + "Language not recognized: " + languageStr);
                return null;
            }else {
                languageMap.put(languageStr, language);
                return language;
            }
        }
    }

    /**
     * Returns true if i is a multiple of recordsPerTransaction
     */
    private boolean loopNeedsHandling(int i, int recordsPerLoop) {
        startTransaction();
        return (i % recordsPerLoop) == 0;
    }

    private void doLogPerLoop(int count, int recordsPerLog, String pluralString){
        if ((count % recordsPerLog ) == 0 && count!= 0 ){ logger.info(pluralString + " handled: " + (count));}
    }

    @Override
    public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs,
            FaunaEuropaeaCommonNameImportState state) {

        Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

        //sourceReference
        Reference sourceReference = getReferenceService().find(PesiTransformer.uuidSourceRefFaunaEuropaeaCommonNames);
        Map<String, Reference> referenceMap = new HashMap<>();
        referenceMap.put(SOURCE_REFERENCE, sourceReference);
        result.put(NAMESPACE_REFERENCE, referenceMap);

        return result;
    }

    @Override
    public String getPluralString(){
        return pluralString;
    }

    protected String getTableName(){
        return dbTableName;
    }

    @Override
	protected boolean doCheck(FaunaEuropaeaCommonNameImportState state){
		return true;
	}

	@Override
	protected boolean isIgnore(FaunaEuropaeaCommonNameImportState state){
		return false;
	}

}