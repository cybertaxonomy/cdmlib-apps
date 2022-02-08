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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.database.update.DatabaseTypeNotSupportedException;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportState;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelReferenceImport;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelTaxonNameImport;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TextData;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.term.DefinedTerm;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;


/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class MexicoEfloraTaxonImport  extends MexicoEfloraImportBase {

    private static final long serialVersionUID = -1186364983750790695L;

    private static final Logger logger = Logger.getLogger(MexicoEfloraTaxonImport.class);

	public static final String NAMESPACE = "Taxon";

	private static final String pluralString = "Taxa";
	private static final String dbTableName = "Efloa_Taxonomia4CDM ";

	private static final String LAST_SCRUTINY_FK = "lastScrutinyFk";

	/**
	 * How should the publish flag in table PTaxon be interpreted
	 * NO_MARKER: No marker is set
	 * ONLY_FALSE:
	 */
	public enum PublishMarkerChooser{
		NO_MARKER,
		ONLY_FALSE,
		ONLY_TRUE,
		ALL;

		boolean doMark(boolean value){
			if (value == true){
				return this == ALL || this == ONLY_TRUE;
			}else{
				return this == ALL || this == ONLY_FALSE;
			}
		}
	}

	public MexicoEfloraTaxonImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(BerlinModelImportState state) {
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
	protected boolean doCheck(MexicoEfloraImportState state){
		//IOValidator<BerlinModelImportState> validator = new BerlinModelTaxonImportValidator();
		//return validator.validate(state);
	    return true;
	}

	@Override
	public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, MexicoEfloraImportState state) {

	    boolean success = true ;
	    MexicoEfloraImportConfigurator config = state.getConfig();
		@SuppressWarnings("rawtypes")
        Set<TaxonBase> taxaToSave = new HashSet<>();
//		@SuppressWarnings("unchecked")
//        Map<String, TaxonName> taxonNameMap = partitioner.getObjectMap(BerlinModelTaxonNameImport.NAMESPACE);
		@SuppressWarnings("unchecked")
        Map<String, Reference> refMap = partitioner.getObjectMap(BerlinModelReferenceImport.REFERENCE_NAMESPACE);

		ResultSet rs = partitioner.getResultSet();
		try{
			while (rs.next()){

			//	if ((i++ % modCount) == 0 && i!= 1 ){ logger.info("PTaxa handled: " + (i-1));}

				//create TaxonName element
				String taxonId = rs.getString("IdCAT");
				String status = rs.getString("EstatusNombre");
				String rankStr = rs.getString("CategoriaTaxonomica");
				String nameStr = rs.getString("Nombre");
				String autorStr = rs.getString("AutorSinAnio");
				String fullNameStr = nameStr + " " + autorStr;
			    String annotationStr = rs.getString("AnotacionTaxon");
//			    String type = rs.getString("NomPublicationType");
			    String year = rs.getString("Anio");
				int secFk = rs.getInt("IdBibliografiaSec");
			    int nameId = rs.getInt("idNombre");


				//IdCATRel => Accepted Taxon => TaxonRel
				//IdCAT_AscendenteHerarquico4CDM => Parent => TaxonRel
				//IdCAT_BasNomOrig => Basionyme der akzeptierten Taxa => TaxonRel

				Rank rank = Rank.GENUS(); //FIXME
				NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();
				TaxonName taxonName = (TaxonName)parser.parseFullName(fullNameStr);

				String refFkStr = String.valueOf(secFk);
				Reference sec = refMap.get(refFkStr);

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
					    logger.error("Status not yet implemented: " + status);
					}


					DefinedTerm taxonIdType = DefinedTerm.IDENTIFIER_NAME_IPNI();
					taxonBase.addIdentifier(taxonId, taxonIdType);

					//namePhrase
					String namePhrase = rs.getString("NamePhrase");
					if (StringUtils.isNotBlank(namePhrase)){
						taxonBase.setAppendedPhrase(namePhrase);
					}

					//Notes
					boolean excludeNotes = state.getConfig().isTaxonNoteAsFeature() && taxonBase.isInstanceOf(Taxon.class);
					String notes = rs.getString("Notes");

					doIdCreatedUpdatedNotes(state, taxonBase, rs, taxonId, NAMESPACE, false, excludeNotes || notes == null);
					if (excludeNotes && notes != null){
					    makeTaxonomicNote(state, CdmBase.deproxy(taxonBase, Taxon.class), rs.getString("Notes"));
					}

					partitioner.startDoSave();
					taxaToSave.add(taxonBase);
				} catch (Exception e) {
					logger.warn("An exception (" +e.getMessage()+") occurred when creating taxon with id " + taxonId + ". Taxon could not be saved.");
					success = false;
				}
			}
		} catch (DatabaseTypeNotSupportedException e) {
			logger.error("MethodNotSupportedException:" +  e);
			return false;
		} catch (Exception e) {
			logger.error("SQLException:" +  e);
			return false;
		}

		getTaxonService().save(taxaToSave);
		return success;
	}

    private boolean isMclIdentifier(BerlinModelImportState state, ResultSet rs, String idInSource) throws SQLException {
        if (idInSource.contains("-")){
            return true;
        }else if (idInSource.matches("(293|303)")){
            String created = rs.getString("Created_Who");
            if (created.endsWith(".xml")){
                return true;
            }
        }
        return false;
    }

    @Override
    protected String getIdInSource(BerlinModelImportState state, ResultSet rs) throws SQLException {
        String id = rs.getString("idInSource");
        return id;
    }

    private void makeTaxonomicNote(BerlinModelImportState state, Taxon taxon, String notes) {
        if (isNotBlank(notes)){
            TaxonDescription desc = getTaxonDescription(taxon, false, true);
            desc.setDefault(true);  //hard coded for Salvador, not used elsewhere as far as I can see
            TextData textData = TextData.NewInstance(Feature.NOTES() , notes, Language.SPANISH_CASTILIAN(), null);
            desc.addElement(textData);
        }
    }

    @Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state) {

        String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> nameIdSet = new HashSet<>();
			Set<String> referenceIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, nameIdSet, "PTNameFk");
				handleForeignKey(rs, referenceIdSet, "PTRefFk");
				if (state.getConfig().isUseLastScrutinyAsSec() && resultSetHasColumn(rs, LAST_SCRUTINY_FK)){
				    handleForeignKey(rs, referenceIdSet, LAST_SCRUTINY_FK);
				}
			}

			//name map
			nameSpace = BerlinModelTaxonNameImport.NAMESPACE;
			idSet = nameIdSet;
			Map<String, TaxonName> nameMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonName.class, idSet, nameSpace);
			result.put(nameSpace, nameMap);

			//reference map
			nameSpace = BerlinModelReferenceImport.REFERENCE_NAMESPACE;
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
	protected boolean isIgnore(MexicoEfloraImportState state){
		return ! state.getConfig().isDoTaxa();
	}
}