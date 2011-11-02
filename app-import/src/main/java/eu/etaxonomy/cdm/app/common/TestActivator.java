/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy 
* http://www.e-taxonomy.eu
* 
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.app.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.api.service.config.ITaxonServiceConfigurator;
import eu.etaxonomy.cdm.api.service.config.TaxonServiceConfiguratorImpl;
import eu.etaxonomy.cdm.api.service.pager.Pager;
import eu.etaxonomy.cdm.common.monitor.DefaultProgressMonitor;
import eu.etaxonomy.cdm.common.monitor.IProgressMonitor;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.model.common.IdentifiableEntity;
import eu.etaxonomy.cdm.model.description.Distribution;
import eu.etaxonomy.cdm.model.description.PresenceTerm;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.location.NamedArea;
import eu.etaxonomy.cdm.model.location.TdwgArea;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.persistence.query.MatchMode;

/**
 * @author a.babadshanjan
 * @created 12.05.2009
 */
public class TestActivator {
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(TestActivator.class);

	//static final Source faunaEuropaeaSource = FaunaEuropaeaSources.faunEu();
	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_flora_central_africa_production();
//	static final ICdmDataSource cdmDestination = CdmDestinations.cdm_edit_cichorieae_preview();
	
	
	static final int limitSave = 2000;

//	static final CHECK check = CHECK.CHECK_AND_IMPORT;
	static final CHECK check = CHECK.CHECK_ONLY;
//	static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.NONE;
//	static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.UPDATE;
	static DbSchemaValidation dbSchemaValidation = DbSchemaValidation.VALIDATE;
	static final NomenclaturalCode nomenclaturalCode  = NomenclaturalCode.ICBN;


	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		ICdmDataSource destination = cdmDestination;
		
		CdmApplicationController app;
		
//		applicationEventMulticaster
//		app = CdmIoApplicationController.NewInstance(destination, dbSchemaValidation);
		
		IProgressMonitor progressMonitor = DefaultProgressMonitor.NewInstance();
		String resourcePath= "/eu/etaxonomy/cdm/appimportTestApplicationContext.xml";
		ClassPathResource resource = new ClassPathResource(resourcePath);
//		ApplicationListener<?> listener = new AppImportApplicationListener();
//		List<ApplicationListener> listeners = new ArrayList<ApplicationListener>();
//		listeners.add(listener);
//		app = CdmApplicationController.NewInstance(resource, destination, dbSchemaValidation, false, progressMonitor, listeners);
		app = CdmApplicationController.NewInstance(resource, destination, dbSchemaValidation, false, progressMonitor);
		
		TransactionStatus txStatus = app.startTransaction();
		List<TaxonNode> nodeList = app.getTaxonNodeService().list(null, null, null, null, null);
		List<TaxonBase> taxonList = new ArrayList<TaxonBase>();
//		UUID uuidArea = UUID.fromString("c6a2c418-1aee-448f-9836-44d85f9dd139");
		UUID uuidArea = UUID.fromString("1ca78cd4-7c24-46e9-a45a-ea96a2bb0ecd");
		NamedArea area = (NamedArea)app.getTermService().find(uuidArea);
		PresenceTerm status = (PresenceTerm)app.getTermService().find(UUID.fromString("cef81d25-501c-48d8-bbea-542ec50de2c2"));
 		for (TaxonNode node:nodeList){
			if (node.getClassification() != null){
				Taxon taxon = node.getTaxon();
				TaxonDescription desc = TaxonDescription.NewInstance(taxon, false);
				desc.setTitleCache("Full area distribution", true);
				//only for test
				Distribution distr = Distribution.NewInstance(area, status);
				desc.addElement(distr);
				taxonList.add(taxon);
			}
		}
		app.getTaxonService().saveOrUpdate(taxonList);
		app.commitTransaction(txStatus);
		if (true){
			return;
		}
		
//		app.changeDataSource(destination);
//		ICdmDataSource cdmDestination = CdmDestinations.cdm_edit_cichorieae_preview();
//		app.changeDataSource(cdmDestination);
		ITaxonServiceConfigurator<?> conf = TaxonServiceConfiguratorImpl.NewInstance();
		conf.setDoSynonyms(true);
		conf.setDoTaxa(true);
		conf.setMatchMode(MatchMode.BEGINNING);
		conf.setTitleSearchString("L*");
		conf.setPageNumber(0);
		conf.setPageSize(50);
		Set<NamedArea> areas = new HashSet<NamedArea>();
		areas.add(TdwgArea.getAreaByTdwgAbbreviation("GER"));
		//conf.setNamedAreas(areas);
		
		Pager<IdentifiableEntity> taxaAndSyn = app.getTaxonService().findTaxaAndNames(conf);
		List<IdentifiableEntity> taxList = taxaAndSyn.getRecords();
		
		for (IdentifiableEntity<?> ent: taxList){
			
			System.err.println(ent.getTitleCache());
		}
		
	
		
		System.out.println("End importing Fauna Europaea data");
	}

}
