/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.pesi.erms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.mapping.DbIgnoreMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportAnnotationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportDistributionCreationMapper;
import eu.etaxonomy.cdm.io.common.mapping.DbImportMapping;
import eu.etaxonomy.cdm.io.common.mapping.DbImportObjectMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbLastActionMapper;
import eu.etaxonomy.cdm.io.pesi.erms.validation.ErmsDistributionImportValidator;
import eu.etaxonomy.cdm.model.common.AnnotationType;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.MarkerType;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @since 20.02.2010
 */
@Component
public class ErmsDistributionImport
        extends ErmsImportBase<Distribution> {

    private static final long serialVersionUID = 6169103238671736935L;

    @SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(ErmsDistributionImport.class);

	private DbImportMapping<ErmsImportState, ErmsImportConfigurator> mapping;

	private static final String pluralString = "distributions";
	private static final String dbTableName = "dr";
	private static final Class<?> cdmTargetClass = Distribution.class;

	public ErmsDistributionImport(){
		super(pluralString, dbTableName, cdmTargetClass);
	}

	@Override
	protected String getRecordQuery(ErmsImportConfigurator config) {
		String strRecordQuery =
			" SELECT dr.*, ISNULL(ISNULL(tu.tu_acctaxon, tu.tu_accfinal), tu.id) acctaxon, " +
	                  " s.sessiondate lastActionDate, a.action_name lastAction, s.ExpertName " +
			" FROM dr INNER JOIN tu ON dr.tu_id = tu.id " +
            "     LEFT OUTER JOIN dr_sessions MN ON MN.dr_id = dr.id " +
            "     LEFT OUTER JOIN [sessions] s ON s.id = MN.session_id " +
            "     LEFT OUTER JOIN actions a ON a.id = MN.action_id " +
			" WHERE ( dr.id IN (" + ID_LIST_TOKEN + ") )" +
			" ORDER BY dr.id, s.sessiondate DESC, a.id DESC ";
		return strRecordQuery;
	}

	@Override
	protected DbImportMapping<ErmsImportState, ErmsImportConfigurator> getMapping() {
		if (mapping == null){
			mapping = new DbImportMapping<>();

			PresenceAbsenceTerm status = PresenceAbsenceTerm.PRESENT();
			DbImportDistributionCreationMapper<?> distributionMapper = DbImportDistributionCreationMapper
			        .NewFixedStatusInstance("id", DR_NAMESPACE, "acctaxon", ErmsImportBase.TAXON_NAMESPACE, status);
			distributionMapper.setSource("source_id", REFERENCE_NAMESPACE, null);
			mapping.addMapper(distributionMapper);

			mapping.addMapper(DbImportObjectMapper.NewInstance("gu_id", "area", ErmsImportBase.AREA_NAMESPACE));
			mapping.addMapper(DbImportAnnotationMapper.NewInstance("note", AnnotationType.EDITORIAL()));
            //last action
			AnnotationType speciesExpertNameAnnType = getAnnotationType(ErmsTransformer.uuidAnnSpeciesExpertName, "species expert name", "species expert name", "species expert name");
            mapping.addMapper(DbImportAnnotationMapper.NewInstance("ExpertName", speciesExpertNameAnnType)); //according to sql script ExpertName maps to SpeciesExpertName in ERMS
            AnnotationType lastActionDateType = getAnnotationType(DbLastActionMapper.uuidAnnotationTypeLastActionDate, "Last action date", "Last action date", null);
            mapping.addMapper(DbImportAnnotationMapper.NewInstance("lastActionDate", lastActionDateType));
            AnnotationType lastActionType = getAnnotationType(DbLastActionMapper.uuidAnnotationTypeLastAction, "Last action", "Last action", null);
            MarkerType hasNoLastActionMarkerType = getMarkerType(DbLastActionMapper.uuidMarkerTypeHasNoLastAction, "has no last action", "No last action information available", "no last action");
            mapping.addMapper(DbImportAnnotationMapper.NewInstance("lastAction", lastActionType, hasNoLastActionMarkerType));

			mapping.addMapper(DbIgnoreMapper.NewInstance("unacceptsource_id"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("unacceptreason"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("valid_flag"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("certain_flag"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("map_flag"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("endemic_flag"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("exotic_flag"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("typelocality_flag"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("specimenflag"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("lat"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("long"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("depthshallow"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("depthdeep"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("beginyear"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("beginmonth"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("beginday"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("endyear"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("endmonth"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("endday"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("min_abundance"));
			mapping.addMapper(DbIgnoreMapper.NewInstance("max_abundance"));
		}
		return mapping;
	}

    Integer lastDistributionId = null;
    @Override
    protected boolean ignoreRecord(ResultSet rs) throws SQLException {
        Integer id = rs.getInt("id");
        boolean result = id.equals(lastDistributionId);
        lastDistributionId = id;
        return result;
    }

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, ErmsImportState state) {

	    String nameSpace;
		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		try{
			Set<String> taxonIdSet = new HashSet<>();
			Set<String> areaIdSet = new HashSet<>();
			Set<String> sourceIdSet = new HashSet<>();
			while (rs.next()){
				handleForeignKey(rs, taxonIdSet,"acctaxon" );
				handleForeignKey(rs, areaIdSet, "gu_id");
				handleForeignKey(rs, sourceIdSet, "source_id");
			}

			//taxon map
			nameSpace = ErmsImportBase.TAXON_NAMESPACE;
			idSet = taxonIdSet;
			@SuppressWarnings("rawtypes")
            Map<String, TaxonBase> taxonMap = getCommonService().getSourcedObjectsByIdInSourceC(TaxonBase.class, idSet, nameSpace);
			result.put(nameSpace, taxonMap);

			//areas
			nameSpace = ErmsImportBase.AREA_NAMESPACE;
			idSet = areaIdSet;
			Map<String, NamedArea> areaMap = getCommonService().getSourcedObjectsByIdInSourceC(NamedArea.class, idSet, nameSpace);
			result.put(nameSpace, areaMap);

			//reference map
			nameSpace = ErmsImportBase.REFERENCE_NAMESPACE;
			idSet = sourceIdSet;
			Map<String, Reference> referenceMap = getCommonService().getSourcedObjectsByIdInSourceC(Reference.class, idSet, nameSpace);
			result.put(nameSpace, referenceMap);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	private void addSource(Distribution distribution, Integer source_id, ErmsImportState state) {
		Reference ref = (Reference)state.getRelatedObject(ErmsImportBase.REFERENCE_NAMESPACE, String.valueOf(source_id));
		distribution.addSource(OriginalSourceType.PrimaryTaxonomicSource, null, null, ref, null);
	}

    @SuppressWarnings("unused")
    public Distribution createObject(ResultSet rs, ErmsImportState state)
			throws SQLException {
		return null;  //not needed
	}

	@Override
	protected boolean doCheck(ErmsImportState state){
		IOValidator<ErmsImportState> validator = new ErmsDistributionImportValidator();
		return validator.validate(state);
	}

	@Override
    protected boolean isIgnore(ErmsImportState state){
		return ! state.getConfig().isDoDistributions();
	}
}
