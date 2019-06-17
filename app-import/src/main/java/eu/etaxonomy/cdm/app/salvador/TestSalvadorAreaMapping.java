/**
* Copyright (C) 2017 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.salvador;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

import eu.etaxonomy.cdm.api.service.dto.DistributionInfoDTO.InfoPart;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.ext.geo.EditGeoService;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;

/**
 * @author a.mueller
 * @since 08.07.2017
 *
 */
public class TestSalvadorAreaMapping {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(TestSalvadorAreaMapping.class);

    //database validation status (create, update, validate ...)
    static DbSchemaValidation hbm2dll = DbSchemaValidation.VALIDATE;

//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_test_local_mysql_test();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_salvador_preview();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_salvador();


    protected void doTest(ICdmDataSource cdmDestination){

        CdmIoApplicationController app = CdmIoApplicationController.NewInstance(cdmDestination, hbm2dll);
        try {
            doTest2(app);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * @param app
     * @throws Exception
     */
    private void doTest2(CdmIoApplicationController app)  {
        UUID taxonUuid = UUID.fromString("eae896f0-3194-4b7b-a502-ad1d54ec36e6");
//      Taxon taxon = (Taxon)app.getTaxonService().find(taxonUuid);
      Object geoServiceObj = app.getBean("editGeoService");
      EditGeoService geoService;
        try {
            geoService = getTargetObject(geoServiceObj);


          Set<InfoPart> partSet = new HashSet<>();
          partSet.add(InfoPart.mapUriParams);

          EnumSet<InfoPart> parts = EnumSet.copyOf(partSet);
          geoService.composeDistributionInfoFor(parts, taxonUuid, false, false, null, null, null, null, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected <T> T getTargetObject(Object proxy) throws Exception {
        while( (AopUtils.isJdkDynamicProxy(proxy))) {
            return (T) getTargetObject(((Advised)proxy).getTargetSource().getTarget());
        }
        return (T) proxy; // expected to be cglib proxy then, which is simply a specialized class
    }


    /**
     * @param args
     */
    public static void main(String[] args) {
        TestSalvadorAreaMapping me = new TestSalvadorAreaMapping();
        me.doTest(cdmDestination);

        System.exit(0);
    }
}
