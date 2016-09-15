package eu.etaxonomy.cdm.io.iapt;

import eu.etaxonomy.cdm.model.occurrence.Collection;
import eu.etaxonomy.cdm.model.occurrence.FieldUnit;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.Assert;

/**
 * Created by andreas on 9/15/16.
 */
public class IAPTImportTest {

    IAPTExcelImport importer = null;

    @Before
    public void setup(){
        System.getProperties().put("TEST_MODE", "1");
        importer = new IAPTExcelImport();
    }

    @Test
    public void testDateParser(){

        String[] dateStrings = new String[]{
                "April 12, 1969",
                "april 12th 1999",
                "April 99",
                "April, 1999",
                "Apr. 12",
                "12.04.1969",
                "12. 04. 1969",
                "12/04/1969",
                "12-04-1969",
                "12 de Enero de 1999",
                "Enero de 1999",
                "04.1969",
                "04/1969",
                "04-1969",
                "1999-04",
                "VI-1969",
                "12-VI-1969",
                "12. April 1969",
                "april 1999",
                "22 Dec.1999"
        };

        for (String d: dateStrings) {
            Assert.notNull(importer.parseDate("0", d), "Could not parse " + d);
        }
    }

    @Test
    public void testSpecimentTypeParser(){

        FieldUnit fu = FieldUnit.NewInstance();
        Collection collection = null;

        String[] typeStrings = new String[]{
                "Coll. Lange-Bertalot, Bot. Inst., Univ. Frankfurt/Main, Germany Praep. No. Eu-PL 72",
                "LE 1700b-114",
                "AD 99530159",
                "STU P 1425",
                "GAUF (Gansu Agricultural University) No. 1207-1222",
                "KASSEL Coll. Krasske, Praep. DII 78",
                "Coll. Lange-Bertalot, Botanisches Institut, Frankfurt am Main slide Eh-B 91",
                "Coll. Østrup, Botan. Museum Copenhagen, Dänemark Praep. 3944",
                "Coll. L.P.B.V. No. 0736",
                "Coll. Ruhr University-Bochum, Inst. of Geology No. 11532",
                "Coll. Paläontol. Inst. Univ. Bucuresti. Nr. 2515",
                "Coll. Dr.h.c. R. Mundlos (Bad Friedrichshall, später Stuttgart) Inv. Nr. P 1396",
                "Inst. Geological Sciences, Acad. Sci. Belarus, Minsk N 212 A",
                "Coll. Lange-Bertalot, Bot. Inst., Univ. Frankfurt/Main, Germany",
                "in coll. H. F. Paulus (Wien)",


        };
        for (String t: typeStrings) {
            Assert.notNull(importer.parseSpecimenType(fu, IAPTExcelImport.TypesName.holotype, collection, t, "0"), "Could not parse: " + t);
        }

    }
}
