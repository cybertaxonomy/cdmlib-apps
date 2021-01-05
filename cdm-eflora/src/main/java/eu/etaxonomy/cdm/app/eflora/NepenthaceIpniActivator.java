/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.eflora;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.api.application.ICdmRepository;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.ext.ipni.IpniService;
import eu.etaxonomy.cdm.ext.ipni.IpniServiceNamesConfigurator;
import eu.etaxonomy.cdm.io.common.utils.ImportDeduplicationHelper;
import eu.etaxonomy.cdm.model.name.IBotanicalName;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;

/**
 * @author a.mueller
 * @since 27.02.2017
 */
public class NepenthaceIpniActivator {

    static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;

////    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_nepenthes_production();
//  static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql();
//  static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();

  static final URI ipniFile = URI.create("file:////BGBM-PESIHPC/FloraMalesianaXml/advPlantNameSearch.txt");

  private void doImport(ICdmDataSource cdmDestination){
      if (cdmDestination.getDatabaseType().equals(CdmDestinations.localH2().getDatabaseType())){
          hbm2dll = DbSchemaValidation.CREATE;
      }

      CdmApplicationController app = CdmApplicationController.NewInstance(cdmDestination, hbm2dll);

      ICdmRepository repository = null  ; //app  for now we do not want to deduplicate against repository
      ImportDeduplicationHelper<?> deduplicationHelper = new ImportDeduplicationHelper<>(repository);

      TransactionStatus txStatus = app.startTransaction();
      Reference ipniSec = app.getReferenceService().find(UUID.fromString("17ffcbc7-7f80-42cd-a95e-25d0289c9f71"));
      if (ipniSec == null){
          ipniSec = ReferenceFactory.newGeneric();
          ipniSec.setTitle("IPNI");
      }

      IpniServiceNamesConfigurator namesConfig = IpniServiceNamesConfigurator.NewInstance();
      namesConfig.setDoApni(false);
      namesConfig.setDoGci(false);

      Classification classification = Classification.NewInstance("IPNI Nepenthes (IK)");
      classification.setUuid(UUID.fromString("89b1a50a-15f5-415b-9ab8-b98ffb6d3c5a"));
      classification.setReference(ipniSec);
      app.getClassificationService().save(classification);
      doSingleImport(app, ipniSec, classification, namesConfig, deduplicationHelper);

      classification = Classification.NewInstance("IPNI Nepenthes (APNI)");
      classification.setUuid(UUID.fromString("c9d1371a-c77a-4249-8406-bb0cb0c1b6f2"));
      classification.setReference(ipniSec);
      app.getClassificationService().save(classification);
      namesConfig.setDoApni(true);
      namesConfig.setDoIk(false);
      doSingleImport(app, ipniSec, classification, namesConfig, deduplicationHelper);

      classification = Classification.NewInstance("IPNI Nepenthes (GCI)");
      classification.setUuid(UUID.fromString("ecddac67-95d6-49ff-a1f3-83e167d581f5"));
      classification.setReference(ipniSec);
      app.getClassificationService().save(classification);
      namesConfig.setDoApni(false);
      namesConfig.setDoGci(true);
      doSingleImport(app, ipniSec, classification, namesConfig, deduplicationHelper);

      app.commitTransaction(txStatus);

////      CSVReader csvReader = new CSVReader(reader);
//
//      List<String[]> allLines = csvReader.readAll();
//
//      for (String[] nextLine : allLines){
//          handleSingleLine(app, nextLine);
//      }
   }

    private void doSingleImport(CdmApplicationController app, Reference ipniSec, Classification classification,
        IpniServiceNamesConfigurator namesConfig, ImportDeduplicationHelper<?> deduplicationHelper) {

        IpniService ipniService = new IpniService();//(IpniService)app.getBean("ipniService");

        Rank rank = null;
        List<IBotanicalName> names = ipniService.getNamesAdvanced(null, "Nepenthes", null, null, null, null,
              null, null, rank, namesConfig, app);

        System.out.println(names.size());

        List<TaxonBase> taxaToSave  = new ArrayList<>();
        for (IBotanicalName name : names){
//          System.out.println(name.getTitleCache());
            deduplicationHelper.replaceAuthorNamesAndNomRef(null, name);
            Taxon taxon = Taxon.NewInstance(name, ipniSec);
            classification.addChildTaxon(taxon, null, null);
//          app.getTaxonService().saveOrUpdate(taxon);
            taxaToSave.add(taxon);
        }
        app.getTaxonService().saveOrUpdate(taxaToSave);
        app.getClassificationService().saveOrUpdate(classification);
    }

    public static void main(String[] args) {
        NepenthaceIpniActivator me = new NepenthaceIpniActivator();
        me.doImport(cdmDestination);
        System.exit(0);
    }
}