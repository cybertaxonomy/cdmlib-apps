package eu.etaxonomy.cdm.io.pesi.indexFungorum;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.config.MatchingTaxonConfigurator;
import eu.etaxonomy.cdm.io.pesi.out.PesiTransformer;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author k.luther
 * @since 19.08.2014
 */
@Component
public class IndexFungorumSubSpeciesImport extends IndexFungorumImportBase {

    private static final long serialVersionUID = -2877755674188760685L;
    private static Logger logger = LogManager.getLogger();

	private static final String pluralString = "subSpecies";

	public IndexFungorumSubSpeciesImport(){
		super(pluralString, null);
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(
			ResultSet rs, IndexFungorumImportState state) {
		return null;  //not used here
	}

	@Override
	protected String getRecordQuery(IndexFungorumImportConfigurator config) {
		return null;  //not used here
	}

	@Override
    protected void doInvoke(IndexFungorumImportState state){

		logger.info("create infraspecific - specific relationship: " + state.getInfraspecificTaxaUUIDs().size() + " taxa");

		List<String> propertyPaths = new ArrayList<>();
        propertyPaths.add("taxonNodes.*");
        propertyPaths.add("taxonNodes.classification");
        propertyPaths.add("taxonNodes.childNodes.*");
        propertyPaths.add("taxonNodes.childNodes.taxon.*");
        propertyPaths.add("taxonNodes.parent.*");
        propertyPaths.add("taxonNodes.parent.taxon.*");
        Classification classification = getClassification(state);
        for (UUID infraspecificTaxonUUID: state.getInfraspecificTaxaUUIDs().keySet()){
        	TransactionStatus txStatus = startTransaction();
        	Taxon infraspecificTaxon = (Taxon)getTaxonService().load(infraspecificTaxonUUID, propertyPaths);

            TaxonName name = infraspecificTaxon.getName();

            String parentNameString = getParentNameInfraSpecific(name);
            MatchingTaxonConfigurator matchingConfig = new MatchingTaxonConfigurator();
            matchingConfig.setTaxonNameTitle(parentNameString);

            matchingConfig.setPropertyPath(propertyPaths);
            @SuppressWarnings("rawtypes")
            List<TaxonBase> potentialParents = getTaxonService().findTaxaByName(matchingConfig)
                    .stream().filter(tb->isIndexFungorumTaxon(tb)).collect(Collectors.toList());
            boolean matched = false;
            if (potentialParents.size()>1){
                for (@SuppressWarnings("rawtypes") TaxonBase potentialParent : potentialParents){
                    if (potentialParent.getTitleCache().equals(parentNameString + " sec*")){
                        classification.addParentChild((Taxon)potentialParent, infraspecificTaxon, null, null);
                        matched = true;
                        if(matched == true){
                            logger.warn("There is more than 1 potential parent: " + parentNameString);
                        }
                    }else{
                        logger.warn("Potential parent is not sec*: " + potentialParent.getTitleCache());
                    }
                }
                if (matched == false){
                    logger.warn("Multiple match candidates but no match for " + name.getTitleCache());
                }
            }else if (!potentialParents.isEmpty()){
                Taxon parent = CdmBase.deproxy(potentialParents.get(0), Taxon.class);
                classification.addParentChild(parent, infraspecificTaxon, null, null);
                matched = true;
            } else{
                Integer genusId = state.getInfraspecificTaxaUUIDs().get(infraspecificTaxonUUID);
                Taxon genusParent = getParentGenus(state, genusId);
                classification.addParentChild(genusParent, infraspecificTaxon, null, null);
                logger.warn("Added infraspecific taxon to genus because species does not exist: " + infraspecificTaxon.getTitleCache());
                matched = true;
            }
            if (!matched){
                System.out.println("No parent for: " + name.getTitleCache());
            }

            getTaxonService().saveOrUpdate(infraspecificTaxon);
            commitTransaction(txStatus);
        }
	}

	private boolean isIndexFungorumTaxon(TaxonBase<?> taxon){
	    return taxon.getSources().stream()
	            .anyMatch(osb->osb.getCitation() != null && osb.getCitation().getUuid().equals(PesiTransformer.uuidSourceRefIndexFungorum));
	}

    private Taxon getParentGenus(IndexFungorumImportState state, Integer genusId) {
        Taxon result = getCommonService().getSourcedObjectByIdInSource(Taxon.class, String.valueOf(genusId), NAMESPACE_GENERA);
        return result;
    }

    private String getParentNameInfraSpecific(TaxonName taxonName){
        String parentName = taxonName.getGenusOrUninomial() + " " + taxonName.getSpecificEpithet();
        return parentName;
    }

    @Override
    protected boolean doCheck(IndexFungorumImportState state) {
        return false;
    }

    @Override
    protected boolean isIgnore(IndexFungorumImportState state) {
        return false;
    }
}
