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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.DefinedTerm;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * @author a.mueller
 * @since 08.02.2022
 */
@Component
public class MexicoEfloraTaxonImport  extends MexicoEfloraImportBase {

    private static final long serialVersionUID = -1186364983750790695L;

    private static final Logger logger = Logger.getLogger(MexicoEfloraTaxonImport.class);

	public static final String NAMESPACE = "Taxon";

	private static final String pluralString = "Taxa";
	protected static final String dbTableName = "EFlora_Taxonomia4CDM2";


	public MexicoEfloraTaxonImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(MexicoEfloraImportState state) {
		String sql = " SELECT IdCAT "
		        + " FROM " + dbTableName
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

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, MexicoEfloraImportState state) {

	    boolean success = true ;
	    @SuppressWarnings("rawtypes")
        Set<TaxonBase> taxaToSave = new HashSet<>();

	    @SuppressWarnings("unchecked")
        Map<String, Reference> refMap = partitioner.getObjectMap(MexicoEfloraReferenceImportBase.NAMESPACE);

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){

			//	if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("PTaxa handled: " + (i-1));}

				//create Taxon element
				String taxonId = rs.getString("IdCAT");
				String status = rs.getString("EstatusNombre");
				String rankStr = rs.getString("CategoriaTaxonomica");
				String nameStr = rs.getString("Nombre");
				String autorStr = rs.getString("AutorSinAnio");
				String fullNameStr = nameStr + " " + autorStr;
				String citaNomenclaturalStr = rs.getString("CitaNomenclatural");
			    String annotationStr = rs.getString("AnotacionTaxon");
			    String type = rs.getString("NomPublicationType");
			    String year = rs.getString("Anio");
			    String uuidStr = rs.getString("uuid");
			    UUID uuid = UUID.fromString(uuidStr);
				Integer secFk = nullSafeInt(rs, "IdBibliografiaSec");

				Rank rank = getRank(rankStr);
				NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();
				TaxonName taxonName = (TaxonName)parser.parseFullName(fullNameStr, NomenclaturalCode.ICNAFP, rank);
				//FIXME TODO
				Reference nomRef = ReferenceFactory.newGeneric();
				nomRef.setAbbrevTitleCache(citaNomenclaturalStr, true);

				nomRef.setDatePublished(TimePeriodParser.parseStringVerbatim(year));
				taxonName.setNomenclaturalReference(nomRef);

				//sec
				Reference sec = null;
				if (secFk != null) {
				    String refFkStr = String.valueOf(secFk);
				    sec = refMap.get(refFkStr);
				    if (sec == null) {
				        logger.warn("Sec not found for taxonId " +  taxonId +" and secId " + refFkStr);
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

					//TODO
					DefinedTerm taxonIdType = DefinedTerm.IDENTIFIER_NAME_IPNI();
					taxonName.addIdentifier(taxonId, taxonIdType);

					//Notes
//					boolean excludeNotes = state.getConfig().isTaxonNoteAsFeature() && taxonBase.isInstanceOf(Taxon.class);
//					String notes = rs.getString("Notes");

//					doIdCreatedUpdatedNotes(state, taxonBase, rs, taxonId, NAMESPACE, false);
//					if (excludeNotes && notes != null){
//					    makeTaxonomicNote(state, CdmBase.deproxy(taxonBase, Taxon.class), rs.getString("Notes"));
//					}

					partitioner.startDoSave();
					taxaToSave.add(taxonBase);
				} catch (Exception e) {
					logger.warn("An exception (" +e.getMessage()+") occurred when creating taxon with id " + taxonId + ". Taxon could not be saved.");
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
        //TODO
//        else if ("híbrido".equals(rank)){ return Rank.GENUS;}
        else if ("especie".equals(rank)){ return Rank.SPECIES();}
        else if ("subespecie".equals(rank)){ return Rank.SUBSPECIES();}
        //TODO
        else if ("raza".equals(rank)){ return Rank.RACE();}
        else if ("variedad".equals(rank)){ return Rank.VARIETY();}
        else if ("subvariedad".equals(rank)){ return Rank.SUBVARIETY();}
        else if ("forma".equals(rank)){ return Rank.FORM();}
        else if ("subforma".equals(rank)){ return Rank.SUBFORM();}
        //TODO
        else if ("raza".equals(rank)){ return Rank.RACE();}
        else {
            logger.warn("Rank not recognized: "+ rank);
        }


        return result;
    }

    @Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, MexicoEfloraImportState state) {

        String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> referenceIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, referenceIdSet, "IdBibliografiaSec");
			}

			//reference map
			nameSpace = MexicoEfloraReferenceImportBase.NAMESPACE;
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