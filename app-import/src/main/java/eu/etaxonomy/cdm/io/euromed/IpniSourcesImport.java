/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.euromed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.pager.Pager;
import eu.etaxonomy.cdm.io.berlinModel.BerlinModelTransformer;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImport;
import eu.etaxonomy.cdm.io.mexico.SimpleExcelTaxonImportState;
import eu.etaxonomy.cdm.model.description.DescriptionElementSource;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.term.TermVocabulary;

/**
 * This import adds the distribution source to data imported via E+M IPNI new names import.
 *
 * @author a.mueller
 * @since 10.07.2020
 */
@Component
public class IpniSourcesImport<CONFIG extends IpniSourcesImportConfigurator>
        extends SimpleExcelTaxonImport<CONFIG> {

    private static final long serialVersionUID = -6723116237971852295L;

    private static final Logger logger = Logger.getLogger(IpniSourcesImport.class);

    private static final String NAMECACHE = "full_name_without_family_and_authors";
    private static final String GENUS = "genus";
    private static final String SPECIES = "species";
    private static final String INFRA_SPECIES = "infraspecies";
    private static final String RANK = "rank";
    private static final String EM_GEO = "EM-geo";

    private Map<String,NamedArea> areaMap;

    @Override
    protected String getWorksheetName(CONFIG config) {
        return "_11_IPNI_name_w_EM_genus_tax_m2";
    }

    private boolean isFirst = true;
    private TransactionStatus tx = null;

    @Override
    protected void firstPass(SimpleExcelTaxonImportState<CONFIG> state) {
        if (isFirst){
            tx = this.startTransaction();
            isFirst = false;
        }
        getAreaMap();

        String line = state.getCurrentLine() + ": ";
        Map<String, String> record = state.getOriginalRecord();

        String uninomial = getValue(record, GENUS);
        String specificEpithet = getValue(record, SPECIES);
        String infraspecificEpithet = getValue(record, INFRA_SPECIES);
        String nameCache = getValue(record, NAMECACHE);
        List<String> propertyPaths = null;
        String authorshipCache = "*";
        Rank rank = getRank(state);
        Pager<Taxon> taxonPager = getTaxonService().findTaxaByName(Taxon.class, uninomial, "*", specificEpithet,
                infraspecificEpithet, authorshipCache, rank, null, null, propertyPaths);
        List<Taxon> taxa = getPublishedTaxa(taxonPager, line);
        if (taxa.size() == 1){
            Taxon taxon = taxa.get(0);
            makeDistributionSource(state, line, taxon);
        }else{
            logger.warn(line + "Could not find unique taxon. Count: " + taxa.size() +"; "  + nameCache);
        }

    }

    private List<Taxon> getPublishedTaxa(Pager<Taxon> taxonPager, String line) {
        List<Taxon> result = new ArrayList<>();
        for (Taxon taxon : taxonPager.getRecords()){
            if (!taxon.isPublish()){
                logger.warn(line + "Unpublished taxon exists: " +  taxon.getTitleCache());
            }else{
                result.add(taxon);
            }
        }
        return result;
    }

    private void makeDistributionSource(SimpleExcelTaxonImportState<CONFIG> state, String line, Taxon taxon) {

        Set<Distribution> distributions = taxon.getDescriptionItems(Feature.DISTRIBUTION(), Distribution.class);

        //single areas
        Map<String, String> record = state.getOriginalRecord();
        String allAreaStr = getValue(record, EM_GEO);

        //E+M area
        NamedArea emArea = getAreaMap().get("EM");
        handleArea(line, taxon, distributions, emArea);

        if(isBlank(allAreaStr)){
            logger.warn(line + "No distribution data exists in IPNI file: " + taxon.getName().getTitleCache());
        }else{
            String[] areaSplit = allAreaStr.split(",");
            for (String areaStr: areaSplit){
                NamedArea area = getAreaMap().get(areaStr.trim());
                handleArea(line, taxon, distributions, area);
            }
        }
    }

    private void handleArea(String line, Taxon taxon, Set<Distribution> distributions, NamedArea area) {
        Distribution distribution = findDistribution(distributions, area);
        if (distribution == null){
            logger.warn(line + "Distribution not found: " + taxon.getName().getTitleCache() + ": " + area.getTitleCache());
        }else{
            if (distribution.getSources().isEmpty()){
                addNomenclaturalSource(taxon, distribution, line);
            }else{
                logger.warn(line + "Distribution has source already: " + taxon.getName().getTitleCache() + ": " + area.getTitleCache() + "; Source(s): " + getSourceString(distribution.getSources()));
            }
        }
    }

    private String getSourceString(Set<DescriptionElementSource> sources) {
        String result = "";
        boolean isFirst = true;
        for (DescriptionElementSource source : sources){
            if (!isFirst){
                result += ";";
            }
            if (source.getCitation() != null){
                result += source.getCitation().getTitleCache();
            }else{
                result += "--source has no citation--";
            }
            isFirst = false;
        }
        return result;
    }

    private void addNomenclaturalSource(Taxon taxon, Distribution distribution, String line) {
        distribution.addSource(OriginalSourceType.PrimaryTaxonomicSource,
                null, null, taxon.getName().getNomenclaturalReference(),
                taxon.getName().getNomenclaturalMicroReference(),
                taxon.getName(), null);
        if(taxon.getName().getNomenclaturalReference() == null){
            logger.warn(line + "No nomenclatural source available" + taxon.getName().getTitleCache());
        }
    }

    private Distribution findDistribution(Set<Distribution> distributions, NamedArea area) {
        Distribution result = null;
        for (Distribution distribution : distributions){
            if (area.equals(distribution.getArea())){
                result = distribution;
            }
        }
        return result;
    }

    private Map<String, NamedArea> getAreaMap() {
        if (areaMap == null){
            makeAreaMap();
        }
        return areaMap;
    }

    private void makeAreaMap() {
        areaMap = new HashMap<>();
        @SuppressWarnings("unchecked")
        TermVocabulary<NamedArea> emAreaVoc = getVocabularyService().find(BerlinModelTransformer.uuidVocEuroMedAreas);
        for (NamedArea area: emAreaVoc.getTerms()){
            areaMap.put(area.getIdInVocabulary(), area);
        }
    }

    private Rank getRank(SimpleExcelTaxonImportState<CONFIG> state) {
        Map<String, String> record = state.getOriginalRecord();
        String rankStr = getValue(record, RANK);
        if ("spec.".equals(rankStr)){
            return Rank.SPECIES();
        }else if ("subsp.".equals(rankStr)){
            return Rank.SUBSPECIES();
        }else{
            logger.warn("Unknown rank: " + rankStr);
            return null;
        }
    }

    @Override
    protected void secondPass(SimpleExcelTaxonImportState<CONFIG> state) {
        if (tx != null){
            this.commitTransaction(tx);
            tx = null;
        }
    }
}
