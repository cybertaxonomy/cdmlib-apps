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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.name.IBotanicalName;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.INomenclaturalReference;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.reference.ReferenceType;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.IdentifierType;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;
import eu.etaxonomy.cdm.strategy.parser.INonViralNameParser;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public class MexicoEfloraTaxonImport  extends MexicoEfloraImportBase {

    private static final long serialVersionUID = -1186364983750790695L;
    private static final Logger logger = LogManager.getLogger();

	public static final String NAMESPACE = "Taxon";

	private static final String pluralString = "Taxa";
	protected static final String dbTableName = "EFlora_Taxonomia4CDM2";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected static INonViralNameParser<TaxonName> nameParser = (INonViralNameParser)NonViralNameParserImpl.NewInstance();

	public MexicoEfloraTaxonImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(MexicoEfloraImportState state) {
		String sql = " SELECT IdCAT "
		        + " FROM " + dbTableName
		        + " WHERE IdCAT NOT IN ('2PLANT') "
//		        + "   AND CitaNomenclatural LIKE 'Nov. Gen. Sp. Pl. (folio ed.) 4:%. 1820 [1818]' "
		        + " ORDER BY IdCAT ";
		return sql;
	}

	@Override
	protected String getRecordQuery(MexicoEfloraImportConfigurator config) {
		String sqlSelect = " SELECT * ";
		String sqlFrom = " FROM " + dbTableName;
		String sqlWhere = " WHERE ( IdCAT IN (" + ID_LIST_TOKEN + ") )";

		String strRecordQuery =sqlSelect + " " + sqlFrom + " " + sqlWhere ;
		return strRecordQuery;
	}

	boolean firstMissingSec = true;

	Reference sourceReference;
	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, MexicoEfloraImportState state) {
	    sourceReference = this.getSourceReference(state.getConfig().getSourceReference());

	    state.getDeduplicationHelper().reset();

	    boolean success = true ;
	    @SuppressWarnings("rawtypes")
        Set<TaxonBase> taxaToSave = new HashSet<>();

	    @SuppressWarnings("unchecked")
        Map<String, Reference> refMap = partitioner.getObjectMap(MexicoEfloraReferenceImportBase.NAMESPACE);

	    int i = 0;
		ResultSet rs = partitioner.getResultSet();
		try{
//		    System.out.println();
			while (rs.next()){
			    success = handleSingleRecord(partitioner, state, success, taxaToSave, refMap, rs, i++);
			}
		} catch (Exception e) {
		    e.printStackTrace();
			logger.error("Exception:" +  e);
			return false;
		}

		getTaxonService().save(taxaToSave);
		return success;
	}

    private boolean handleSingleRecord(ResultSetPartitioner<?> partitioner, MexicoEfloraImportState state, boolean success,
            @SuppressWarnings("rawtypes") Set<TaxonBase> taxaToSave, Map<String, Reference> refMap, ResultSet rs, int i) throws SQLException {
		if ((i % 1000) == 0 && i!= 1 ){ logger.info("Taxa handled: " + (i-1));}
//			    System.out.println("i++");
		//create Taxon element
		String taxonId = rs.getString("IdCAT");
		String status = rs.getString("EstatusNombre");
		String rankStr = rs.getString("CategoriaTaxonomica");
		String nameStr = rs.getString("Nombre");
		String autorStr = rs.getString("AutorSinAnio");
		String citaNomenclaturalStr = rs.getString("CitaNomenclatural");
	    String annotationStr = rs.getString("AnotacionTaxon");
	    String type = rs.getString("NomPublicationType");
	    String year = rs.getString("Anio");
	    String uuidStr = rs.getString("uuid");
	    UUID uuid = UUID.fromString(uuidStr);
		Integer secFk = nullSafeInt(rs, "IdBibliografiaSec");

        TaxonName taxonName= makeName(taxonId, state, autorStr,
           nameStr, citaNomenclaturalStr, type, rankStr, annotationStr, year);

		//sec
		Reference sec = null;
		if (secFk != null) {
		    String refFkStr = String.valueOf(secFk);
		    sec = refMap.get(refFkStr);
		    if (sec == null && firstMissingSec) {
//		        logger.warn("There are missing sec refs but they are not logged anymore.");
		        logger.warn("Sec not found for taxonId " +  taxonId +" and secId " + refFkStr);
		        firstMissingSec = true;
		    }
		}

		//taxon
		TaxonBase<?> taxonBase;
		Synonym synonym;
		Taxon taxon;
		try {
			if ("aceptado".equals(status)){
				taxon = Taxon.NewInstance(taxonName, sec);
				taxonBase = taxon;
			}else if ("sinónimo".equals(status)){
				synonym = Synonym.NewInstance(taxonName, sec);
				taxonBase = synonym;
			}else {
			    taxonBase = null;
			    logger.error("Status not yet implemented: " + status);
			    return false;
			}
			taxonBase.setUuid(uuid);

			partitioner.startDoSave();
			taxaToSave.add(taxonBase);
		} catch (Exception e) {
			logger.warn("An exception (" +e.getMessage()+") occurred when creating taxon with id " + taxonId + ". Taxon could not be saved.");
			success = false;
		}
        return success;
    }

	boolean isFirstDedup = true;
    private TaxonName makeName(String taxonId, MexicoEfloraImportState state,
            String authorStr, String nameStr, String nomRefStr, String refType, String rankStr,
            String annotation, String year) {

        //rank
        Rank rank = getRank(rankStr);

        boolean isRace = Rank.RACE().equals(rank);
        String raceEpithet = null;
        if (isRace) {
            raceEpithet = nameStr.substring(nameStr.indexOf(" raza ") + 6).trim();
            nameStr = nameStr.substring(0, nameStr.indexOf(" raza ")).trim();
        }

        nameStr = removeSubgenusBracket(nameStr, rank);
        nameStr = removeVarForSubvar(nameStr, rank);

        //name + author
        String fullNameStr = nameStr + (authorStr != null ? " " + authorStr : "");

        TaxonName fullName = nameParser.parseFullName(fullNameStr, NomenclaturalCode.ICNAFP, rank);

        if (fullName.isProtectedTitleCache()){
            logger.warn(taxonId + ": Name could not be parsed: " + fullNameStr );
        }

        //reference
        String refNameStr = getRefNameStr(nomRefStr, refType, fullNameStr, taxonId);

        TaxonName referencedName = nameParser.parseReferencedName(refNameStr, NomenclaturalCode.ICNAFP, rank);
        if (referencedName.isProtectedFullTitleCache() || referencedName.isProtectedTitleCache()){
            logger.warn(taxonId + ": Referenced name could not be parsed: " + refNameStr );
        }else{
            addSourcesToReferences(referencedName, state);
        }
        if (isRace) {
            //TODO Cultivar Race, shouldn't it be grex?
            referencedName.setRank(Rank.GREX_ICNCP());
            referencedName.setNameType(NomenclaturalCode.ICNCP);
            referencedName.setCultivarEpithet(raceEpithet);
        }

        adaptRefTypeForGeneric(referencedName, refType);
        Reference nomRef = referencedName.getNomenclaturalReference();
        if (isNotBlank(year)) {
            //if explicit year is given and year could not be parsed for nomRef we use the explicit year,
            // otherwise if parsed date and explicit year differ it is logged
            if (nomRef == null) {
                nomRef = ReferenceFactory.newGeneric();
            }
            String nomRefYear = nomRef.getYear();
            if (isBlank(nomRefYear)) {
                String nomRefDateStr = nomRef.getDatePublishedString();
                nomRef.setDatePublished(TimePeriodParser.parseStringVerbatim(year));
                if (isNotBlank(nomRefDateStr) && !nomRefDateStr.equals(year)) {
                    Matcher matcher = Pattern.compile("([0-9]{4}(?:-[0-9]{4})?)\\s?\\[([0-9]{4})\\]").matcher(nomRefDateStr);
                    if (matcher.matches() && matcher.group(2).equals(year)){
                        nomRef.getDatePublished().setVerbatimDate(matcher.group(1));
                        logger.warn(taxonId + ": need to verify real year vs. verbatim year: "+ nomRef.getDatePublishedString() + " ("+referencedName.getTitleCache()+")");
                    }else {
                        logger.warn(taxonId + ": year and parsed date published are not equal: "+ year + "<->" + nomRefDateStr + " ("+referencedName.getTitleCache()+")");
                    }
                }
            }else if (! nomRefYear.equals(year)){
                logger.warn(taxonId + ": year and parsed year are not equal: "+ year + "<->" + nomRefYear + " ("+referencedName.getTitleCache()+")");
            }
        }
        state.getDeduplicationHelper().replaceAuthorNamesAndNomRef(referencedName);

        TaxonName result = referencedName;
        state.getNameMap().put(result.getTitleCache(), result.getUuid());
        state.getNameMap().put(result.getNameCache(), result.getUuid());

        //status
        if (annotation != null && (annotation.equals("nom. illeg.")
                || annotation.equals("nom. cons.")
                || annotation.equals("nom. superfl.")
                || annotation.equals("nom. inval.")
                )){
            try {
                NomenclaturalStatusType nomStatusType = NomenclaturalStatusType.getNomenclaturalStatusTypeByAbbreviation(annotation, result);
                result.addStatus(NomenclaturalStatus.NewInstance(nomStatusType));
            } catch (UnknownCdmTypeException e) {
                logger.warn(taxonId + ": nomStatusType not recognized: " + annotation);
            }
        }

        if(result.getNomenclaturalReference() != null && result.getNomenclaturalReference().getTitleCache().equals("null")){
            logger.warn("null");
        }

        IdentifierType conabioIdentifier = getIdentiferType(state, MexicoConabioTransformer.uuidConabioTaxonIdIdentifierType,
                "CONABIO Taxon Identifier", "CONABIO Taxon Identifier", "CONABIO", null);
        result.addIdentifier(taxonId, conabioIdentifier);

        return result;
    }

    private String removeSubgenusBracket(String nameStr, @SuppressWarnings("unused") Rank rank) {
        if (nameStr.matches("[A-Z][a-z]+\\s+\\([A-Za-z]+\\)\\s+[a-z]+.*")) {
            //species and below: remove bracket completely
            nameStr = nameStr.substring(0, nameStr.indexOf("(")) + nameStr.substring(nameStr.indexOf(")")+1);
        }else if (nameStr.matches("[A-Z][a-z]+\\s+\\([A-Za-z]+\\)")) {
            //subgenus: replace (...) bei subg. ...
            nameStr = nameStr.substring(0, nameStr.indexOf("(")) + "subg. " + nameStr.substring(nameStr.indexOf("(")+1, nameStr.length()-1);
        }
        return nameStr;
    }

    private String removeVarForSubvar(String nameStr, Rank rank) {
        if (rank.equals(Rank.SUBVARIETY()) && nameStr.matches(".* var\\. .* subvar\\. .*")) {
            nameStr = nameStr.substring(0, nameStr.indexOf(" var.")) + nameStr.substring(nameStr.indexOf(" subvar."));
        }
        return nameStr;
    }

    private void adaptRefTypeForGeneric(IBotanicalName referencedName, String refTypeStr) {
        INomenclaturalReference ref = referencedName.getNomenclaturalReference();
        if (ref == null){
            return;
        }
        ReferenceType refType = refTypeByRefTypeStr(refTypeStr);
        if (ref.getType() != refType && refType == ReferenceType.Book){
            ref.setType(refType);
        }
    }

    private String getRefNameStr(String nomRefStr, String refTypeStr, String fullNameStr, String taxonID) {
        String refNameStr = fullNameStr;
        ReferenceType refType = refTypeByRefTypeStr(refTypeStr);
        if (isBlank(nomRefStr)){
            //do nothing
        }else if (refType == ReferenceType.Article){
            refNameStr = fullNameStr + " in " + nomRefStr;
        }else if (refType == ReferenceType.Book){
            refNameStr = fullNameStr + ", " + nomRefStr;
        }else if (refType == null){
            logger.warn(taxonID + ": RefType is null but nomRefStr exists");
        }
        return refNameStr;
    }

    private ReferenceType refTypeByRefTypeStr(String refType){
        if ("A".equals(refType)){  //Article
            return ReferenceType.Article;
        }else if ("B".equals(refType)){   //Book
            return ReferenceType.Book;
        }else if (refType == null || isBlank(refType)){   //Book
            return null;
        }else{
            throw new IllegalArgumentException("RefType not supported " + refType);
        }
    }

    private void addSourcesToReferences(IBotanicalName name, MexicoEfloraImportState state) {
        Reference nomRef = name.getNomenclaturalReference();
        if (nomRef != null){
            nomRef.addSource(makeOriginalSource(state));
            if (nomRef.getInReference() != null){
                nomRef.getInReference().addSource(makeOriginalSource(state));
            }
        }
    }

   protected IdentifiableSource makeOriginalSource(@SuppressWarnings("unused") MexicoEfloraImportState state) {
        return IdentifiableSource.NewDataImportInstance(null, null, sourceReference);
    }

    private Rank getRank(String rank) {
        Rank result = null;
        if ("Reino".equals(rank)){ return Rank.KINGDOM();}
        else if ("división".equals(rank)){ return Rank.DIVISION();}
        else if ("clase".equals(rank)){ return Rank.CLASS();}
        else if ("subclase".equals(rank)){ return Rank.SUBCLASS();}
        else if ("superorden".equals(rank)){ return Rank.SUPERORDER();}
        else if ("orden".equals(rank)){ return Rank.ORDER();}
        else if ("suborden".equals(rank)){ return Rank.SUBORDER();}
        else if ("familia".equals(rank)){ return Rank.FAMILY();}
        else if ("subfamilia".equals(rank)){ return Rank.SUBFAMILY();}
        else if ("tribu".equals(rank)){ return Rank.TRIBE();}
        else if ("subtribu".equals(rank)){ return Rank.SUBTRIBE();}
        else if ("género".equals(rank)){ return Rank.GENUS();}
        else if ("subgénero".equals(rank)){ return Rank.SUBGENUS();}
        else if ("sección".equals(rank)){ return Rank.SECTION_BOTANY();}
        else if ("subsección".equals(rank)){ return Rank.SUBSECTION_BOTANY();}
        else if ("serie".equals(rank)){ return Rank.SERIES();}
        else if ("grupo".equals(rank)){ return Rank.SPECIESGROUP();}
        else if ("híbrido".equals(rank)){ return null;}  //will be handled later
        else if ("especie".equals(rank)){ return Rank.SPECIES();}
        else if ("subespecie".equals(rank)){ return Rank.SUBSPECIES();}
        else if ("raza".equals(rank)){ return Rank.RACE();}
        else if ("variedad".equals(rank)){ return Rank.VARIETY();}
        else if ("subvariedad".equals(rank)){ return Rank.SUBVARIETY();}
        else if ("forma".equals(rank)){ return Rank.FORM();}
        else if ("subforma".equals(rank)){ return Rank.SUBFORM();}
        else if ("raza".equals(rank)){ return Rank.RACE();}
        else {
            logger.warn("Rank not recognized: "+ rank);
        }

        return result;
    }

    @Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, MexicoEfloraImportState state) {

        String nameSpace;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> referenceIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, referenceIdSet, "IdBibliografiaSec");
			}

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