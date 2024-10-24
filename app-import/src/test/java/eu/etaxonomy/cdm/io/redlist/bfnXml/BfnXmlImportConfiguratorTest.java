/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.redlist.bfnXml;

//
//import java.net.URISyntaxException;
//import java.net.URL;
//
//import org.apache.logging.log4j.Logger;
//import org.apache.logging.log4j.LogManager;
//import org.aspectj.lang.annotation.Before;
//import org.junit.Test;
//import org.unitils.spring.annotation.SpringBeanByName;
//import org.unitils.spring.annotation.SpringBeanByType;

//import eu.etaxonomy.cdm.api.service.IClassificationService;
//import eu.etaxonomy.cdm.api.service.INameService;
//import eu.etaxonomy.cdm.api.service.ITaxonService;
//import eu.etaxonomy.cdm.io.common.CdmApplicationAwareDefaultImport;
//import eu.etaxonomy.cdm.io.common.IImportConfigurator;
//import eu.etaxonomy.cdm.test.integration.CdmTransactionalIntegrationTest;

/**
 * @author a.mueller
 * @since 29.01.2009
 */
//public class BfnXmlImportConfiguratorTest extends CdmTransactionalIntegrationTest {
//	Logger logger = LogManager.getLogger();
//	@SpringBeanByName
//	CdmApplicationAwareDefaultImport<?> defaultImport;
//
//	@SpringBeanByType
//	INameService nameService;
//
//	@SpringBeanByType
//	ITaxonService taxonService;
//
//	@SpringBeanByType
//	IClassificationService classificationService;
//
//	private IImportConfigurator configurator;
//
//	@Before(value = "")
//	public void setUp() throws URISyntaxException {
//
//		String inputFile = "/eu/etaxonomy/cdm/io/bfnXml/bfnXmlTest-input.xml";
//		URL url = this.getClass().getResource(inputFile);
//		assertNotNull("URL for the test file '" + inputFile + "' does not exist", url);
//		configurator = BfnXmlImportConfigurator.NewInstance(url.toURI(), null);
//		assertNotNull("Configurator could not be created", configurator);
//	}
//
//	@Test
//	public void testInit() {
//		assertNotNull("cdmTcsXmlImport should not be null", defaultImport);
//		assertNotNull("nameService should not be null", nameService);
//	}
//
//	@Test
//	public void testDoInvoke() {
//		boolean result = defaultImport.invoke(configurator);
//		assertTrue("Return value for import.invoke should be true", result);


//		List<Classification> classificationList = classificationService.list(Classification.class, null, null, null, null);
//		if(classificationList != null && !classificationList.isEmpty()){
//			for(Classification classification:classificationList){
//				Set<TaxonNode> tnSet = classification.getAllNodes();
//				for(TaxonNode tn:tnSet){
//					logger.info(tn.getTaxon().getTitleCache());
//				}
//			}
//		}else{
//			logger.info("classification is empty");
//		}
//		List<TaxonBase> taxonBaseList = taxonService.list(TaxonBase.class, null, null, null, null);
//		for(TaxonBase taxon:taxonBaseList){
//
//			if(taxon instanceof Taxon){
//			logger.info("Taxon: "+taxon.getTitleCache());
//			}
//			if(taxon instanceof Synonym){
//				logger.info("Synonym: "+taxon.getTitleCache());
//			}
//		}
//		assertEquals("Number of TaxonBase should be 11", 11, nameService.count(null));
//	}

//}
