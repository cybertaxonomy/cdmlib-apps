/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.lichenes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.common.utils.ImportDeduplicationHelper;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.NameRelationshipType;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * Lichenes genera taxon import.
 *
 * @author a.mueller
 * @since 10.03.2020
 */
@Component
public class LichenesGeneraTaxonImport<CONFIG extends LichenesGeneraImportConfigurator>
            extends SimpleExcelTaxonImport<CONFIG>{

	private static final long serialVersionUID = -2448091105224956821L;
	private static final Logger logger = Logger.getLogger(LichenesGeneraTaxonImport.class);

	private static final String INCERTAE_SEDIS = "Incertae sedis";

	private static UUID rootUuid = UUID.fromString("61187f71-96d1-419d-958b-25ab4c01a93c");

    private  static List<String> expectedKeys= Arrays.asList(new String[]{
            "SORT","GENUS_X","PHYLUM","SUBPHYLUM","CLASS","SUBCLASS","ORDER","SUBORDER","FAMILY",
            "FAM_SYNONYMS","SUBFAMILY","GENUS","SYNONYMS","SPECIES","REFERENCES","MOLECULAR","NOTES"
    });

    private Reference sourceReference;
    private Reference secReference;

    private NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();

//    @Override
//    protected String getWorksheetName(CONFIG config) {
//        return "valid taxa names";
//    }

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {

        String line = state.getCurrentLine() + ": ";
        Map<String, String> record = state.getOriginalRecord();

        Set<String> keys = record.keySet();
        for (String key: keys) {
            if (! expectedKeys.contains(key)){
                logger.warn(line + "Unexpected Key: " + key);
            }
        }

        makeTaxon(state, line, record);

//        state.putTaxon(noStr, taxon);
    }

    private Taxon makeTaxon(SimpleExcelTaxonImportState<CONFIG> state, String line, Map<String, String> record) {

        TaxonNode parentNode = getParentNode(record, state, line);
        if (parentNode == null){
            logger.warn(line + "Parent not created");
        }

        Reference sec = getSecReference(state);
        String genusStr = getValue(record, "GENUS");
        String[] split = parseNomStatus(genusStr);
        genusStr = split[0];
        TaxonName genusName = (TaxonName)parser.parseFullName(genusStr, state.getConfig().getNomenclaturalCode(), Rank.GENUS());
        checkParsed(TaxonName.castAndDeproxy(genusName), genusStr, line);
        makeStatusAndHomonym(state, genusName, split);
        replaceNameAuthorsAndReferences(state, genusName);
        Taxon genusTaxon = Taxon.NewInstance(genusName, sec);
        genusName.addSource(makeOriginalSource(state));
        genusTaxon.addSource(makeOriginalSource(state));

        makeSynonyms(state, line, record, genusTaxon, "SYNONYMS");

        //TODO reference  => for now we don't use it

        makeNotes(genusTaxon, record, line);
        TaxonNode genusNode = parentNode.addChildTaxon(genusTaxon, null, null);
        getTaxonNodeService().saveOrUpdate(genusNode);

        return genusTaxon;
    }

    private void makeStatusAndHomonym(SimpleExcelTaxonImportState<CONFIG> state, TaxonName name, String[] split) {
		if (isNotBlank(split[1])) {
			try {
				NomenclaturalStatusType statusType = NomenclaturalStatusType.getNomenclaturalStatusTypeByAbbreviation(split[1], name);
				name.addStatus(statusType, null, null);
			} catch (UnknownCdmTypeException e) {
				e.printStackTrace();
			}
		}
		if (isNotBlank(split[2])) {
			TaxonName earlierName = (TaxonName)parser.parseFullName(split[2], NomenclaturalCode.ICNAFP, Rank.GENUS());
			replaceNameAuthorsAndReferences(state, earlierName);
			name.addRelationshipToName(earlierName, NameRelationshipType.LATER_HOMONYM());
			name.addSource(makeOriginalSource(state));
			getNameService().saveOrUpdate(earlierName);
		}
	}

	private String[] parseNomStatus(String nameStr) {
		String[] result = new String[3];
		if (nameStr.endsWith(", non Tayloriella Kylin (Rhodophyta)")) {
			result[2] = "Tayloriella Kylin";
			nameStr = nameStr.replace(", non Tayloriella Kylin (Rhodophyta)".trim(), "");
		}
		if (nameStr.endsWith("[nom. illeg.]")) {
			result[1] = "nom. illeg.";
			result[0] = nameStr.replace("[nom. illeg.]".trim(), "");
		}else if (nameStr.endsWith("[nom. inval.]")) {
			result[1] = "nom. inval.";
			result[0] = nameStr.replace("[nom. inval.]".trim(), "");
		}else if (nameStr.endsWith("[nom. cons. prop.]")) {
			result[1] = "nom. cons. prop.";
			result[0] = nameStr.replace("[nom. cons. prop.]".trim(), "");
		}else{
			result[0] = nameStr;
		}
		return result;
	}

	private void makeNotes(Taxon genusTaxon, Map<String, String> record, String line) {
    	String notesStr = getValue(record, "NOTES");
    	if (isNotBlank(notesStr)) {
    		if (notesStr.startsWith("Notes.")) {
    			notesStr = notesStr.substring(6);
    		}
    		//TODO or handle as fact of type "Notes"
    		genusTaxon.addAnnotation(Annotation.NewInstance(notesStr, AnnotationType.EDITORIAL(), Language.ENGLISH()));
    	}
 	}

	private void checkParsed(TaxonName name, String nameStr, String line) {
		if (name.isProtectedTitleCache() || name.isProtectedFullTitleCache() || name.isProtectedNameCache()) {
			logger.warn(line + "Name could not be parsed: " + nameStr);
		}
	}

	private void makeSynonyms(SimpleExcelTaxonImportState<CONFIG> state,
    		String line, Map<String, String> record, Taxon accepted, String fieldName) {

    	String synonymsStr = getValue(record, fieldName);
    	if (isBlank(synonymsStr)) {
    		return;
    	}else if (!synonymsStr.matches("\\(syn\\.:.*\\)")) {
    		logger.warn(line + "Synonyms has unexpected format. No synonyms imported: " + synonymsStr);
    		return;
    	}else{
    		String synonymsStr2 = synonymsStr.substring(7).trim();
    		synonymsStr2 = synonymsStr2.substring(0, synonymsStr2.length()-1);
    		String[] splits = synonymsStr2.split(";");
    		for (String singleSynonymStr: splits) {
    			String[] split2 = parseNomStatus(singleSynonymStr);
    	        singleSynonymStr = split2[0];

    			TaxonName synonymName = (TaxonName)parser.parseFullName(singleSynonymStr, state.getConfig().getNomenclaturalCode(), null);
    			checkParsed(TaxonName.castAndDeproxy(synonymName), singleSynonymStr, line);
    			makeStatusAndHomonym(state, synonymName, split2);
    			replaceNameAuthorsAndReferences(state, synonymName);
    	        synonymName.addSource(makeOriginalSource(state));
    	        Synonym synonym = Synonym.NewInstance(synonymName, accepted.getSec());
    	        accepted.addSynonym(synonym, SynonymType.SYNONYM_OF());
    			synonymName.addSource(makeOriginalSource(state));
    			synonym.addSource(makeOriginalSource(state));
    		}
    	}
	}

	private String lastPhylum = "";
	private String lastSubphylum = "";
	private String lastClassname = "";
	private String lastSubclass = "";
	private String lastOrder = "";
	private String lastSubOrder = "";
	private String lastFamily = "";
	private String lastSubFamily = "";

	private Taxon taxonPhylum = null;
	private Taxon taxonSubphylum = null;
	private Taxon taxonClass = null;
	private Taxon taxonSubclass = null;
	private Taxon taxonOrder = null;
	private Taxon taxonSubOrder = null;
	private Taxon taxonFamily = null;
	private Taxon taxonSubFamily = null;

	private TaxonNode getParentNode(Map<String, String> record, SimpleExcelTaxonImportState<CONFIG> state, String line) {

	    Taxon higherTaxon = null;
		Classification classification = getClassification(state).getClassification();

		String phylum =  Nz(getValue(record, "PHYLUM"));
		String subphylum = Nz(getValue(record, "SUBPHYLUM"));
		String classname = Nz(getValue(record, "CLASS"));
		String subclass = Nz(getValue(record, "SUBCLASS"));
		String order = Nz(getValue(record, "ORDER"));
		String suborder = Nz(getValue(record, "SUBORDER"));
		String family = Nz(getValue(record, "FAMILY"));
		String subfamily = Nz(getValue(record, "SUBFAMILY"));

		if (isNewTaxon(subfamily, lastSubFamily)){
			if (isNewTaxon(family, lastFamily)){
				if (isNewTaxon(suborder, lastSubOrder)){
					if (isNewTaxon(order, lastOrder)){
						if (isNewTaxon(subclass, lastSubclass)){
							if (isNewTaxon(classname, lastClassname)){
								if (isNewTaxon(subphylum, lastSubphylum)){
									if (isNewTaxon(phylum, lastPhylum)){
										//new phylum
										taxonPhylum = makeHigherTaxon(state, phylum, Rank.PHYLUM());
										lastPhylum = phylum;
										logger.info("Import phylum " +  phylum);
										getTaxonService().saveOrUpdate(taxonPhylum);
									}
									higherTaxon = taxonPhylum;
									//new subphylum
									taxonSubphylum = makeHigherTaxon(state, subphylum, Rank.SUBPHYLUM());
									if (taxonSubphylum != null){  //no null expected
										classification.addParentChild(higherTaxon,taxonSubphylum, null, null);
									}
									higherTaxon = isIncertisSedis(subphylum) ? higherTaxon : taxonSubphylum;
									lastSubphylum = subphylum;
								}else{
									higherTaxon = taxonSubphylum;
								}
								//new class
								taxonClass = makeHigherTaxon(state, classname, Rank.CLASS());
								if (taxonClass != null){
									classification.addParentChild(higherTaxon, taxonClass, null, null);
								}
								higherTaxon = isIncertisSedis(classname) ? higherTaxon : taxonClass;
								lastClassname = classname;
							}else{
								higherTaxon = taxonClass;
							}
							//new subclass
							taxonSubclass = makeHigherTaxon(state, subclass, Rank.SUBCLASS());
							if (taxonSubclass != null){
								classification.addParentChild(higherTaxon, taxonSubclass,null, null);
							}
							higherTaxon = isIncertisSedis(subclass) ? higherTaxon : taxonSubclass;
							lastSubclass = subclass;
						}else{
							higherTaxon = taxonSubclass;
						}
						//new order
						taxonOrder = makeHigherTaxon(state, order, Rank.ORDER());
						if (taxonOrder != null){
							classification.addParentChild(higherTaxon, taxonOrder, null, null);
						}
						higherTaxon = isIncertisSedis(order) ? higherTaxon : taxonOrder;
						lastOrder = order;
					}else{
						higherTaxon = taxonOrder;
					}
					//new suborder
					taxonSubOrder = makeHigherTaxon(state, suborder, Rank.SUBORDER());
					if (taxonSubOrder != null){
						classification.addParentChild(higherTaxon, taxonSubOrder, null, null);
					}
					higherTaxon = isIncertisSedis(suborder) ? higherTaxon : taxonSubOrder;
					lastSubOrder = suborder;
				}else{
					higherTaxon = taxonSubOrder;
				}
				taxonFamily = makeHigherTaxon(state, family, Rank.FAMILY());
				if (taxonFamily != null){
					classification.addParentChild(higherTaxon, taxonFamily, null, null);
					makeSynonyms(state, line, record, taxonFamily, "FAM_SYNONYMS");
				}
				higherTaxon = isIncertisSedis(family) ? higherTaxon : taxonFamily;
				lastFamily = family;
				getTaxonService().saveOrUpdate(higherTaxon);
			}else{
				higherTaxon = taxonFamily;
			}
			taxonSubFamily = makeHigherTaxon(state, subfamily, Rank.SUBFAMILY());
			if (taxonSubFamily != null){
				classification.addParentChild(higherTaxon, taxonSubFamily, null, null);
			}
			higherTaxon = isIncertisSedis(subfamily) ? higherTaxon : taxonSubFamily;
			lastSubFamily = subfamily;
		}else {
			higherTaxon = taxonSubFamily;
		}

		getTaxonService().saveOrUpdate(higherTaxon);

		return higherTaxon.getTaxonNode(classification);
	}

	private String Nz(String value) {
		return CdmUtils.Nz(value);
	}

	private boolean isIncertisSedis(String uninomial) {
		return isBlank(uninomial) || uninomial.equalsIgnoreCase(INCERTAE_SEDIS);
	}

	private boolean isNewTaxon(String uninomial, String lastUninomial) {
		boolean result = isBlank(uninomial) || !uninomial.equalsIgnoreCase(lastUninomial);
//		result |= !uninomial.equalsIgnoreCase(lastUninomial);
		result |= lastUninomial.equalsIgnoreCase(INCERTAE_SEDIS);
		return result;
	}

	private Taxon makeHigherTaxon(SimpleExcelTaxonImportState<CONFIG> state, String nameStr, Rank rank) {
		if (isBlank(nameStr) || nameStr.equalsIgnoreCase(INCERTAE_SEDIS)){
			return null;
		}
		//name
		INonViralName name = parser.parseFullName(nameStr, NomenclaturalCode.Fungi, rank);
		replaceNameAuthorsAndReferences(state, name);
		//taxon
		Reference secRef = getSecReference(state);
		Taxon taxon = Taxon.NewInstance(name, secRef);
        name.addSource(makeOriginalSource(state));
        taxon.addSource(makeOriginalSource(state));
		return taxon;
	}

    private TaxonNode rootNode;

    private TaxonNode getClassification(SimpleExcelTaxonImportState<CONFIG> state) {
        if (rootNode == null){
            Reference sec = getSecReference(state);
            String classificationName = state.getConfig().getClassificationName();
            Language language = Language.DEFAULT();
            Classification classification = Classification.NewInstance(classificationName, sec, language);
            classification.setUuid(state.getConfig().getClassificationUuid());
            classification.getRootNode().setUuid(rootUuid);
            getClassificationService().save(classification);

            rootNode = classification.getRootNode();
        }
        return rootNode;
    }

    private Reference getSecReference(SimpleExcelTaxonImportState<CONFIG> state) {
        if (this.secReference == null){
            this.secReference = getPersistentReference(state.getConfig().getSecReference());
        }
        return this.secReference;
    }

    protected Reference getSourceCitation(SimpleExcelTaxonImportState<CONFIG> state) {
        if (this.sourceReference == null){
            this.sourceReference = getPersistentReference(state.getConfig().getSourceReference());
        }
        return this.sourceReference;
    }

    private Reference getPersistentReference(Reference reference) {
        Reference result = getReferenceService().find(reference.getUuid());
        if (result == null){
            result = reference;
        }
        return result;
    }

    private void replaceNameAuthorsAndReferences(SimpleExcelTaxonImportState<CONFIG> state, INonViralName name) {
        dedupHelper().replaceAuthorNamesAndNomRef(state, name);
    }

    private ImportDeduplicationHelper<SimpleExcelTaxonImportState<CONFIG>> dedupHelper;
	private ImportDeduplicationHelper<SimpleExcelTaxonImportState<CONFIG>> dedupHelper() {
    	if (dedupHelper == null) {
    		dedupHelper = (ImportDeduplicationHelper)ImportDeduplicationHelper.NewStandaloneInstance();
    	}
    	return dedupHelper;
    }

    @Override
    protected IdentifiableSource makeOriginalSource(SimpleExcelTaxonImportState<CONFIG> state) {
    	String noStr = getValue(state.getOriginalRecord(), "SORT");
        return IdentifiableSource.NewDataImportInstance(noStr, "SORT", state.getConfig().getSourceReference());
    }

}
