/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.algaterra;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.format.datetime.joda.DateTimeParser;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.io.algaterra.validation.AlgaTerraDnaImportValidator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelTaxonImport;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.IndividualsAssociation;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.molecular.DnaSample;
import eu.etaxonomy.cdm.model.molecular.Sequence;
import eu.etaxonomy.cdm.model.occurrence.DerivationEvent;
import eu.etaxonomy.cdm.model.occurrence.DerivationEventType;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.FieldUnit;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.DefinedTerm;

/**
 * @author a.mueller
 * @since 01.09.2012
 */
@Component
public class AlgaTerraDnaImport  extends AlgaTerraSpecimenImportBase {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LogManager.getLogger();

	private static int modCount = 5000;
	private static final String pluralString = "dna facts";
	private static final String dbTableName = "DNAFact";  //??


	public AlgaTerraDnaImport(){
		super(dbTableName, pluralString);
	}


	@Override
	protected String getIdQuery(BerlinModelImportState bmState) {
		AlgaTerraImportState state = (AlgaTerraImportState)bmState;
		String result = " SELECT df.DNAFactId " +
				" FROM DNAFact df " +
					" INNER JOIN Fact f ON  f.ExtensionFk = df.DNAFactID " +
					" WHERE f.FactCategoryFk = 203 ";
		if (state.getAlgaTerraConfigurator().isRemoveRestricted()){
				result = result + " AND df.ProtectedFlag = 0 ";
				logger.warn("DNA with protectedFlag = 0 is currently not imported");
		}
		result += " ORDER BY df.DNAFactID ";
		return result;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
			String strQuery =
	            " SELECT df.*, pt.RIdentifier as taxonId, f.FactId, f.restrictedFlag, ecoFact.ecoFactId as ecoFactId " +
	            " FROM DNAFact df INNER JOIN Fact f ON  f.ExtensionFk = df.DNAFactID " +
	            	" LEFT OUTER JOIN PTaxon pt ON f.PTNameFk = pt.PTNameFk AND f.PTRefFk = pt.PTRefFk " +
	            	" LEFT OUTER JOIN EcoFact ecoFact ON ecoFact.CultureStrain = df.CultureStrainNo " +
	              " WHERE f.FactCategoryFk = 203 AND (df.DNAFactId IN (" + ID_LIST_TOKEN + ")  )"
	            + " ORDER BY DNAFactID "
            ;
		return strQuery;
	}

	@Override
	public boolean doPartition(ResultSetPartitioner partitioner, BerlinModelImportState bmState) {
		boolean success = true;

		AlgaTerraImportState state = (AlgaTerraImportState)bmState;
		try {
//			makeVocabulariesAndFeatures(state);
		} catch (Exception e1) {
			logger.warn("Exception occurred when trying to create Ecofact vocabularies: " + e1.getMessage());
			e1.printStackTrace();
		}
		Set<SpecimenOrObservationBase> samplesToSave = new HashSet<SpecimenOrObservationBase>();
		Set<TaxonBase> taxaToSave = new HashSet<TaxonBase>();

		Map<String, FieldUnit> ecoFactFieldObservationMap = partitioner.getObjectMap(ECO_FACT_FIELD_OBSERVATION_NAMESPACE);

		ResultSet rs = partitioner.getResultSet();

		Map<String, Reference> referenceMap = new HashMap<String, Reference>();


		try {

			int i = 0;

			//for each reference
            while (rs.next()){

        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}

				int dnaFactId = rs.getInt("DNAFactId");
				String keywordsStr = rs.getString("Keywords");
				String locusStr = rs.getString("Locus");
				String definitionStr = rs.getString("Definition");


				try {

					//source ref
					Reference sourceRef = state.getTransactionalSourceReference();

					//import date
					DateTime importDateTime = makeImportDateTime(rs);

					//DNA Sample
					DnaSample dnaSample = DnaSample.NewInstance();
					dnaSample.setCreated(importDateTime);

					//ecoFactFk
					makeDerivationFromEcoFact(state, rs, dnaSample, samplesToSave, dnaFactId);

					//sequence
					Sequence sequence = makeSequence(rs, dnaSample, dnaFactId, importDateTime);

					//locus
					//FIXME Deduplicate DnaMarker
					DefinedTerm locus = DefinedTerm.NewDnaMarkerInstance(definitionStr, keywordsStr, null);
					locus.setCreated(importDateTime);
					this.getTermService().save(locus);

					sequence.setDnaMarker(locus);

					//GenBank Accession
					makeGenBankAccession(rs, sequence, importDateTime, dnaFactId);

					//Comment
					String commentStr = rs.getString("Comment");
					if (isNotBlank(commentStr)){
						Annotation annotation = Annotation.NewInstance(commentStr, AnnotationType.EDITORIAL(), Language.DEFAULT());
						annotation.setCreated(importDateTime);
						sequence.addAnnotation(annotation);
					}

					//Indiv.Assoc.
					makeIndividualsAssociation(partitioner, rs, state, taxaToSave, dnaSample);

					//TODO titleCache
					//prelim implementation:
					String cultStrain = rs.getString("CultureStrainNo");
					String title = String.format("DNA Sample for %s at %s", cultStrain, keywordsStr);
					dnaSample.setTitleCache(title, true);

					//TODO preliminary implementation
					String referenceStr = rs.getString("FactReference");
					if (isNotBlank(referenceStr)){
						Reference ref = referenceMap.get(referenceStr);
						if (ref == null){
							ref = ReferenceFactory.newGeneric();
							ref.setTitleCache(referenceStr, true);
							referenceMap.put(referenceStr, ref);
						}
						sequence.addCitation(ref);
					}

					//save
					samplesToSave.add(dnaSample);


				} catch (Exception e) {
					logger.warn("Exception in ecoFact: ecoFactId " + dnaFactId + ". " + e.getMessage());
					e.printStackTrace();
				}

            }

			logger.warn("DNASample or EcoFacts to save: " + samplesToSave.size());
			getOccurrenceService().saveOrUpdate(samplesToSave);
			logger.warn("Taxa to save: " + samplesToSave.size());
			getTaxonService().saveOrUpdate(taxaToSave);

			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}


	private void makeDerivationFromEcoFact(AlgaTerraImportState state, ResultSet rs, DnaSample dnaSample, Set<SpecimenOrObservationBase> samplesToSave, Integer dnaFactId) throws SQLException {
		Integer ecoFactFk = nullSafeInt(rs, "ecoFactId");
		if (ecoFactFk != null){

			DerivedUnit ecoFact = (DerivedUnit)state.getRelatedObject(ECO_FACT_DERIVED_UNIT_NAMESPACE, ecoFactFk.toString());
			if (ecoFact == null){
				logger.warn("EcoFact is null for ecoFactFk: " + ecoFactFk + ", DnaFactId: " + dnaFactId);
			}else{
				DerivationEvent.NewSimpleInstance(ecoFact, dnaSample, DerivationEventType.DNA_EXTRACTION());
				samplesToSave.add(ecoFact);
			}
		}
	}



	private void makeIndividualsAssociation(ResultSetPartitioner partitioner, ResultSet rs, AlgaTerraImportState state, Set<TaxonBase> taxaToSave, DnaSample dnaSample) throws SQLException{
		Reference sourceRef = state.getTransactionalSourceReference();
		Map<String, TaxonBase> taxonMap = partitioner.getObjectMap(BerlinModelTaxonImport.NAMESPACE);
		Integer taxonId = rs.getInt("taxonId");
		Integer factId = rs.getInt("factId");
		Taxon taxon = getTaxon(state, taxonId, taxonMap, factId);
		TaxonDescription desc = getTaxonDescription(state, taxon, sourceRef);
		IndividualsAssociation assoc = IndividualsAssociation.NewInstance(dnaSample);
		desc.addElement(assoc);
		taxaToSave.add(taxon);
	}


	private DateTime makeImportDateTime(ResultSet rs) throws SQLException,
			ParseException {
		DateTime importDateTime = null;
		String importDateTimeStr = rs.getString("ImportDateTime");
		if (isNotBlank(importDateTimeStr)){
			importDateTimeStr = importDateTimeStr.substring(0,10);
			DateTimeFormatter dayFormatter = DateTimeFormat.forPattern("dd.MM.yyyy");

//						DateTimeFormatter formatter = new DateTimeFormatterBuilder().
//								append;
			DateTimeParser p = new DateTimeParser(dayFormatter);
			importDateTime = p.parse(importDateTimeStr, Locale.GERMANY);

		}
		return importDateTime;
	}

	private Sequence makeSequence(ResultSet rs, DnaSample dnaSample, int dnaFactId, DateTime importDateTime) throws SQLException {
		String sequenceStr = rs.getString("PlainSequence");
		Integer seqLen = nullSafeInt(rs, "SeqLen");

		if (sequenceStr == null){
			logger.warn("PlainSequence is null. Id: " + dnaFactId);
		}else{
			if (seqLen == null){
				logger.warn("SeqLen is null for dnaFact: "  + dnaFactId);
			}else if (sequenceStr.length() != seqLen){
				logger.warn("SeqLen (" + seqLen+ ") and OriginalLen ("+sequenceStr.length()+") differ for dnaFact: "  + dnaFactId);
			}
		}

		Sequence sequence = Sequence.NewInstance(sequenceStr, seqLen);
		sequence.setCreated(importDateTime);
		dnaSample.addSequence(sequence);
		return sequence;
	}

	private void makeGenBankAccession(ResultSet rs, Sequence sequence, DateTime importDateTime, Integer dnaFactId) throws SQLException {
		String accessionStr = rs.getString("Accession");
		String notesStr = rs.getString("Notes");
		String versionStr = rs.getString("Version");

		URI genBankUri = null;
		if (StringUtils.isNotBlank(notesStr)){
			if (notesStr.startsWith("http")){
				genBankUri = URI.create(notesStr);
			}else{
				logger.warn("Notes do not start with URI: " +  notesStr);
			}
		}

		if (isNotBlank(accessionStr) || genBankUri != null){
			if (accessionStr != null && accessionStr.trim().equals("")){
				accessionStr = null;
			}
			if (isGenBankAccessionNumber(accessionStr, versionStr, genBankUri, dnaFactId) || genBankUri != null){
				sequence.setGeneticAccessionNumber(accessionStr);
			}
		}
	}

	private boolean isGenBankAccessionNumber(String accessionStr, String versionStr, URI genBankUri, Integer dnaFactId) {
		boolean isGenBankAccessionNumber = accessionStr.matches("[A-Z]{2}\\d{6}");
		boolean versionHasGenBankPart = versionStr.matches(".*GI:.*");
		if (isGenBankAccessionNumber && versionHasGenBankPart){
			return true;
		}else {
			if (genBankUri != null){
				logger.warn("GenBank Uri exists but accession or version have been identified to use GenBank syntax. DNAFactID: " + dnaFactId);
			}
			if(isGenBankAccessionNumber || versionHasGenBankPart){
				logger.warn("Either accession ("+ accessionStr +") or version ("+versionStr+") use GenBank syntax but the other does not. DNAFactID: " + dnaFactId);
			}
			return false;
		}
	}



	@Override
    protected String getDerivedUnitNameSpace(){
		return ECO_FACT_DERIVED_UNIT_NAMESPACE;
	}

	@Override
    protected String getFieldObservationNameSpace(){
		return ECO_FACT_FIELD_OBSERVATION_NAMESPACE;
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {
		String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> taxonIdSet = new HashSet<>();
			Set<String> ecoFactFkSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet, "taxonId");
				handleForeignKey(rs, ecoFactFkSet, "ecoFactId");
			}

			//taxon map
			nameSpace = BerlinModelTaxonImport.NAMESPACE;
			idSet = taxonIdSet;
			Map<String, TaxonBase> objectMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, nameSpace);
			result.put(nameSpace, objectMap);


			//eco fact derived unit map
			nameSpace = AlgaTerraSpecimenImportBase.ECO_FACT_DERIVED_UNIT_NAMESPACE;
			idSet = ecoFactFkSet;
			Map<String, DerivedUnit> derivedUnitMap = getCommonService().getSourcedObjectsByIdInSourceC(DerivedUnit.class, idSet, nameSpace);
			result.put(nameSpace, derivedUnitMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new AlgaTerraDnaImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(BerlinModelImportState state){
		return ! ((AlgaTerraImportState)state).getAlgaTerraConfigurator().isDoDna();
	}

}
