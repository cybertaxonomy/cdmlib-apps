/**
* Copyright (C) 2025 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.out;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.io.common.mapping.out.DbLastActionMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.DbStringMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.IdMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.MethodMapper;
import eu.etaxonomy.cdm.io.common.mapping.out.ObjectChangeMapper;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.IdentifiableEntity;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationship;
import eu.etaxonomy.cdm.model.taxon.TaxonRelationshipType;

/**
 * @author muellera
 * @since 16.06.2025
 */
public abstract class PesiTaxonExportBase extends PesiExportBase {

    private static final long serialVersionUID = 2229782438440109860L;
    private static Logger logger = LogManager.getLogger();

    static final String dbTableName = "Taxon";

    static final String pluralString = "Taxa";

    PreparedStatement rankUpdateStmt;

    private static final Class<? extends CdmBase> standardMethodParameter = TaxonBase.class;


    @Override
    public Class<? extends CdmBase> getStandardMethodParameter() {
        return standardMethodParameter;
    }

    /**
     * Returns the CDM to PESI specific export mappings.
     * @return The {@link PesiExportMapping PesiExportMapping}.
     */
    protected PesiExportMapping getMapping() {

        PesiExportMapping mapping = new PesiExportMapping(dbTableName);

        mapping.addMapper(IdMapper.NewInstance("TaxonId"));
        mapping.addMapper(MethodMapper.NewInstance("SourceFk", PesiTaxonExport.class, "getSourceFk", standardMethodParameter, PesiExportState.class));
        mapping.addMapper(MethodMapper.NewInstance("TaxonStatusFk", PesiTaxonExport.class, "getTaxonStatusFk", standardMethodParameter, PesiExportState.class));
        mapping.addMapper(MethodMapper.NewInstance("TaxonStatusCache", PesiTaxonExport.class, "getTaxonStatusCache", standardMethodParameter, PesiExportState.class));

        mapping.addMapper(MethodMapper.NewInstance("GUID", PesiTaxonExport.class, this));

        mapping.addMapper(MethodMapper.NewInstance("DerivedFromGuid", PesiTaxonExport.class, this));
        mapping.addMapper(MethodMapper.NewInstance("CacheCitation", PesiTaxonExport.class, this));
        mapping.addMapper(MethodMapper.NewInstance("AuthorString", PesiTaxonExport.class, this));  //For Taxon because misapplied names are handled differently
        mapping.addMapper(MethodMapper.NewInstance("FullName", PesiTaxonExport.class, this));    //For Taxon because misapplied names are handled differently
        mapping.addMapper(MethodMapper.NewInstance("WebShowName", PesiTaxonExport.class, this));

        // DisplayName
        mapping.addMapper(MethodMapper.NewInstance("DisplayName", PesiTaxonExport.class, this));

        // FossilStatus (Fk, Cache)
        mapping.addMapper(MethodMapper.NewInstance("FossilStatusCache", PesiTaxonExport.class, IdentifiableEntity.class, PesiExportState.class));
        mapping.addMapper(MethodMapper.NewInstance("FossilStatusFk", PesiTaxonExport.class, IdentifiableEntity.class, PesiExportState.class)); // PesiTransformer.FossilStatusCache2FossilStatusFk?

        //handled by name mapping
        mapping.addMapper(DbLastActionMapper.NewInstance("LastActionDate", false));
        mapping.addMapper(DbLastActionMapper.NewInstance("LastAction", true));

        //experts
//      mapping.addMapper(DbExtensionMapper.NewInstance(extensionTypeSpeciesExpertName, "SpeciesExpertName"));
        mapping.addMapper(MethodMapper.NewInstance("SpeciesExpertName", PesiTaxonExport.class, TaxonBase.class));
//      ExtensionType extensionTypeExpertName = (ExtensionType)getTermService().find(PesiTransformer.uuidExtExpertName);
//      mapping.addMapper(DbExtensionMapper.NewInstance(extensionTypeExpertName, "ExpertName"));
        mapping.addMapper(MethodMapper.NewInstance("ExpertName", PesiTaxonExport.class, TaxonBase.class));

        //ParentTaxonFk handled in Phase02 now
        mapping.addMapper(ObjectChangeMapper.NewInstance(TaxonBase.class, TaxonName.class, "Name"));

        addNameMappers(mapping);

        return mapping;
    }

    void addNameMappers(PesiExportMapping mapping) {

        //epithets
        mapping.addMapper(DbStringMapper.NewInstance("GenusOrUninomial", "GenusOrUninomial"));
        mapping.addMapper(DbStringMapper.NewInstance("InfraGenericEpithet", "InfraGenericEpithet"));
        mapping.addMapper(DbStringMapper.NewInstance("SpecificEpithet", "SpecificEpithet"));
        mapping.addMapper(DbStringMapper.NewInstance("InfraSpecificEpithet", "InfraSpecificEpithet"));

        //full name
//      mapping.addMapper(DbStringMapper.NewInstance("NameCache", "WebSearchName"));  //does not work as we need other cache strategy
        mapping.addMapper(MethodMapper.NewInstance("WebSearchName", PesiTaxonExport.class, TaxonName.class));

        //nom ref
        mapping.addMapper(MethodMapper.NewInstance("NomRefString", PesiTaxonExport.class, TaxonName.class));

        //status
        mapping.addMapper(MethodMapper.NewInstance("NameStatusFk", PesiTaxonExport.class, TaxonName.class));
        mapping.addMapper(MethodMapper.NewInstance("NameStatusCache", PesiTaxonExport.class, TaxonName.class, PesiExportState.class));
        mapping.addMapper(MethodMapper.NewInstance("QualityStatusFk", PesiTaxonExport.class, TaxonName.class));
        mapping.addMapper(MethodMapper.NewInstance("QualityStatusCache", PesiTaxonExport.class, TaxonName.class, PesiExportState.class));

        //types
        mapping.addMapper(MethodMapper.NewInstance("TypeFullnameCache", PesiTaxonExport.class, TaxonName.class));
        //TypeNameFk handled in Phase3

        //supplemental
        mapping.addMapper(MethodMapper.NewInstance("IdInSource", PesiTaxonExport.class, TaxonName.class));
        mapping.addMapper(MethodMapper.NewInstance("OriginalDB", PesiTaxonExport.class, IdentifiableEntity.class) );

        //mapping.addMapper(ExpertsAndLastActionMapper.NewInstance());
    }

    static Integer findKingdomIdFromTreeIndex(TaxonBase<?> taxonBase, PesiExportState state) {
        Taxon taxon;
        if (taxonBase instanceof Synonym){
            taxon = ((Synonym) taxonBase).getAcceptedTaxon();
        }else{
            taxon = checkPseudoOrRelatedTaxon((Taxon)taxonBase);
        }
        if (taxon == null){
            NomenclaturalCode nomenclaturalCode = taxonBase.getName().getNameType();
            logger.warn("Taxon is synonym with no accepted taxon attached: " + taxonBase.getTitleCache() + ". The kingdom is taken from the nomenclatural code: " + PesiTransformer.nomenclaturalCode2Kingdom(nomenclaturalCode) );
            return PesiTransformer.nomenclaturalCode2Kingdom(nomenclaturalCode);
        } else{
            Set<TaxonNode> nodes = taxon.getTaxonNodes();
            if (nodes.isEmpty()){
                NomenclaturalCode nomenclaturalCode = taxon.getName().getNameType();
                logger.warn("The taxon has no nodes: " + taxon.getTitleCache() + ". The kingdom is taken from the nomenclatural code: " + PesiTransformer.nomenclaturalCode2Kingdom(nomenclaturalCode));
                return PesiTransformer.nomenclaturalCode2Kingdom(nomenclaturalCode);
            } else {
                if (nodes.size()>1){
                    logger.warn("The taxon has more then 1 taxon node: " + taxon.getTitleCache() + ". Take arbitrary one.");
                }
                String treeIndex = nodes.iterator().next().treeIndex();

                Pattern pattern = Pattern.compile("#t[0-9]+#([0-9]+#){3}");
                Matcher matcher = pattern.matcher(treeIndex);
                Integer kingdomID = null;
                if(matcher.find()) {
                    String treeIndexKingdom = matcher.group(0);
                    kingdomID = state.getTreeIndexKingdomMap().get(treeIndexKingdom);
                }
                if (kingdomID == null){
                    pattern = Pattern.compile("#t[0-9]+#([0-9]+#){2}");
                    matcher = pattern.matcher(treeIndex);
                    if(matcher.find()) {
                        String treeIndexKingdom = matcher.group(0);
                        Map<String, Integer> map = state.getTreeIndexKingdomMap();
                        kingdomID = map.get(treeIndexKingdom);
                    }
                }
                if(Rank.DOMAIN().equals(taxon.getName().getRank())){
                    return 0;
                }
                if(kingdomID == null){
                    logger.warn("Kingdom could not be defined for treeindex " + treeIndex);
                }
                return kingdomID;
            }
        }
    }

    private static Taxon checkPseudoOrRelatedTaxon(Taxon taxon) {
        if (!taxon.getTaxonNodes().isEmpty()){
            return taxon;
        }else if(hasPseudoTaxonRelationship(taxon)){
            return acceptedPseudoTaxon(taxon);
        }else if(isMisappliedNameOrProParteSynonym(taxon)){
            return acceptedTaxonConcept(taxon);
        }else{
            return taxon;
        }
    }

    private static Taxon acceptedPseudoTaxon(Taxon taxon) {
        for (TaxonRelationship rel : taxon.getRelationsFromThisTaxon()){
            if (TaxonRelationshipType.pseudoTaxonUuids().contains(rel.getType().getUuid())){
                return rel.getToTaxon();
            }
        }
        return taxon;
    }

    private static Taxon acceptedTaxonConcept(Taxon taxon) {
       for (TaxonRelationship rel : taxon.getRelationsFromThisTaxon()){
            if (TaxonRelationshipType.misappliedNameUuids().contains(rel.getType().getUuid())||
                    TaxonRelationshipType.proParteOrPartialSynonymUuids().contains(rel.getType().getUuid())){
                return rel.getToTaxon();
            }
        }
        return taxon;
    }

    private static boolean hasPseudoTaxonRelationship(Taxon taxon) {
        for (TaxonRelationship rel : taxon.getRelationsFromThisTaxon()){
            if (TaxonRelationshipType.pseudoTaxonUuids().contains(rel.getType().getUuid())){
                return true;
            }
        }
        return false;
    }

    private static boolean isMisappliedNameOrProParteSynonym(Taxon taxon) {
        for (TaxonRelationship rel : taxon.getRelationsFromThisTaxon()){
            if (TaxonRelationshipType.misappliedNameUuids().contains(rel.getType().getUuid())||
                    TaxonRelationshipType.proParteOrPartialSynonymUuids().contains(rel.getType().getUuid())){
                return true;
            }
        }
        return false;
    }

    protected void initRankUpdateStatement(PesiExportState state) throws SQLException {
        Connection connection = state.getConfig().getDestination().getConnection();
        String rankSql = "UPDATE Taxon SET RankFk = ?, RankCache = ?, KingdomFk = ? WHERE TaxonId = ?";
        rankUpdateStmt = connection.prepareStatement(rankSql);
    }

    /**
     * Returns the <code>RankFk</code> attribute.
     * @param taxonName The {@link TaxonNameBase TaxonName}.
     * @param nomenclaturalCode The {@link NomenclaturalCode NomenclaturalCode}.
     * @return The <code>RankFk</code> attribute.
     * @see MethodMapper
     */
    static Integer getRankFk(TaxonName taxonName, Integer kingdomId) {
        Integer result = null;
        try {
            if (taxonName != null) {
                if (taxonName.getRank() == null) {
                    logger.warn("Rank is null: " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
                } else {
                    result = PesiTransformer.rank2RankId(taxonName.getRank(), kingdomId);
                }
                if (result == null) {
                    logger.warn("Rank could not be determined for PESI-Kingdom-Id " + kingdomId + " and TaxonName " + taxonName.getUuid() + " (" + taxonName.getTitleCache() + ")");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    //  @SuppressWarnings("unused")  //used by pure name mapper and by getRankFk
    static Integer getKingdomFk(TaxonName taxonName){
        EnumSet<PesiSource> origin = getSources(taxonName);
        if (origin.size() == 1 && origin.contains(PesiSource.EM)){
            //maybe simply replace by
            //return PesiTransformer.KINGDOM_PLANTAE;
            return PesiTransformer.nomenclaturalCode2Kingdom(taxonName.getNameType());
        }else{
            logger.warn("getKingdomFk not yet implemented for non-EuroMed pure names: " + taxonName.getTitleCache() + "/" + taxonName.getUuid());
            return null;
        }
    }


    @SuppressWarnings("unused")  //used by mapper
    private static String getRankCache(TaxonName taxonName, PesiExportState state) {
        List<TaxonNode> nodes = getTaxonNodes(taxonName);
        Integer kingdomId;
        if (nodes == null||nodes.isEmpty()){
            kingdomId = getKingdomFk(taxonName);
        }else{
            //should not happen, method exists only pure names
            kingdomId = findKingdomIdFromTreeIndex(nodes.iterator().next().getTaxon(), state);
        }
        return getRankCache(taxonName, kingdomId, state);
    }

    static String getRankCache(TaxonName taxonName, Integer kingdomFk, PesiExportState state) {
        if (Rank.DOMAIN().equals(taxonName.getRank())){
            return state.getTransformer().getCacheByRankAndKingdom(Rank.DOMAIN(), null);
        }else if (kingdomFk != null) {
            return state.getTransformer().getCacheByRankAndKingdom(taxonName.getRank(), kingdomFk);
        }else if (taxonName.getNameType() != null){
            return state.getTransformer().getCacheByRankAndKingdom(taxonName.getRank(), PesiTransformer.nomenclaturalCode2Kingdom(taxonName.getNameType()));
        }else{
            logger.warn("No kingdom ID could be defined for name " + taxonName.getUuid());
            return null;
        }
    }

    private static List<TaxonNode> getTaxonNodes(TaxonName taxonName) {
        List<TaxonNode> result = new ArrayList<>();
        for (TaxonBase<?> tb:taxonName.getTaxonBases()){
            Taxon taxon;
            //TODO handle ERMS taxon relationships
            if (tb.isInstanceOf(Taxon.class)){
                taxon = CdmBase.deproxy(tb, Taxon.class);
            }else{
                taxon = CdmBase.deproxy(tb, Synonym.class).getAcceptedTaxon();
            }
            if (isPesiTaxon(taxon)){
                for (TaxonNode node : taxon.getTaxonNodes()){
                    result.add(node);
                }
            }
        }
        return result;
    }
}
