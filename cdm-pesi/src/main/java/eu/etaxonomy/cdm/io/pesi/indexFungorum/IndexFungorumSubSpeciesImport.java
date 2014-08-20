package eu.etaxonomy.cdm.io.pesi.indexFungorum;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.service.config.MatchingTaxonConfigurator;
import eu.etaxonomy.cdm.hibernate.HibernateProxyHelper;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.name.NonViralName;
import eu.etaxonomy.cdm.model.name.TaxonNameBase;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author k.luther
 * @created 19.08.2014
 */
@Component
public class IndexFungorumSubSpeciesImport extends IndexFungorumImportBase {
	private static final Logger logger = Logger.getLogger(IndexFungorumSpeciesImport.class);

	private static final String pluralString = "subSpecies";
	
	
	public IndexFungorumSubSpeciesImport(){
		super(pluralString, null, null);

	}
	
	public IndexFungorumSubSpeciesImport(String pluralString,
			String dbTableName, Class cdmTargetClass) {
		super(pluralString, dbTableName, cdmTargetClass);
		
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(
			ResultSet rs, IndexFungorumImportState state) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String getRecordQuery(IndexFungorumImportConfigurator config) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean doCheck(IndexFungorumImportState state) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean isIgnore(IndexFungorumImportState state) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
    protected void doInvoke(IndexFungorumImportState state){
		System.out.println("create infraspecific - specific relationship: " + state.getInfraspecificTaxaUUIDs().size() + " taxa");
		
		 List<String> propertyPaths = new ArrayList<String>();
         propertyPaths.add("taxonNodes.*");
         propertyPaths.add("taxonNodes.classification");
         propertyPaths.add("taxonNodes.childNodes.*");
         propertyPaths.add("taxonNodes.childNodes.taxon.*");
         propertyPaths.add("taxonNodes.parent.*");
         propertyPaths.add("taxonNodes.parent.taxon.*");
         Classification classification = getClassification(state);
        for (UUID infraspecificTaxonUUID: state.getInfraspecificTaxaUUIDs()){
        	TransactionStatus txStatus = startTransaction();
        	Taxon infraspecificTaxon = (Taxon)getTaxonService().load(infraspecificTaxonUUID, propertyPaths);
            //HibernateProxyHelper.deproxy(infraspecificTaxon);
            TaxonNameBase name = infraspecificTaxon.getName();

            UUID uuid = getNameService().saveOrUpdate(name);
            String parentNameString = getParentNameInfraSpecific(name);
            System.out.println("Parent name string: " + parentNameString);
            MatchingTaxonConfigurator matchingConfig = new MatchingTaxonConfigurator();
            matchingConfig.setTaxonNameTitle(parentNameString);
           
            matchingConfig.setPropertyPath(propertyPaths);
            List<TaxonBase> potentialParents = getTaxonService().findTaxaByName(matchingConfig);
                    //Taxon.class, parentNameString + "sec. ", MatchMode.BEGINNING, , pageSize, pageNumber, orderHints, propertyPaths)
                    //.searchNames(String uninomial,String infraGenericEpithet, String specificEpithet, String infraspecificEpithet, Rank rank, Integer pageSize, Integer pageNumber, List<OrderHint> orderHints,
            if (potentialParents.size()>1){
                for (TaxonBase potentialParent:potentialParents){
                    if (potentialParent.getTitleCache().equals(parentNameString + " sec*")){
                        classification.addParentChild((Taxon)potentialParent, (Taxon)infraspecificTaxon, null, null);
                    }
                }
            }else if (!potentialParents.isEmpty()){
                Taxon parent = HibernateProxyHelper.deproxy(potentialParents.get(0), Taxon.class);
                Taxon child = (Taxon)getTaxonService().load(infraspecificTaxon.getUuid(), propertyPaths);
                classification.addParentChild(parent, child, null, null);
            } else{
                System.out.println("No parent for: " + name.getTitleCache());
            }
            getTaxonService().saveOrUpdate(infraspecificTaxon);
            commitTransaction(txStatus);
        }
	}
	/**
     * @param taxon
     * @return
     */
    private String getParentNameInfraSpecific(TaxonNameBase taxonName){
       NonViralName<NonViralName> name =  HibernateProxyHelper.deproxy(taxonName, NonViralName.class);
       String parentName = name.getGenusOrUninomial() + " " + name.getSpecificEpithet();

       return parentName;
    }

}
