package eu.etaxonomy.cdm.io.phycobank;

import java.util.regex.Matcher;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import eu.etaxonomy.cdm.io.phycobank.IAPTExcelImport;
import eu.etaxonomy.cdm.io.phycobank.IAPTImportConfigurator;
import eu.etaxonomy.cdm.model.occurrence.Collection;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.FieldUnit;

/**
 * Created by andreas on 9/15/16.
 */
public class IAPTImportTest extends Assert {

    IAPTExcelImport<IAPTImportConfigurator> importer = null;

    @Before
    public void setup(){
        System.getProperties().put("TEST_MODE", "1");
        importer = new IAPTExcelImport<>();
    }

    @Test
    public void testDateParser(){

        String[] dateStrings = new String[]{
                "25 februari 1998",
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
                "17 de dezembro 1997",
                "15 diciembre de 1997",
                "Enero de 1999",
                "04.1969",
                "04/1969",
                "04-1969",
                "1999-04",
                "VI-1969",
                "12-VI-1969",
                "12. April 1969",
                "april 1999",
                "22 Dec.1999",
        };

        for (String d: dateStrings) {
            Assert.assertNotNull("Could not parse " + d, importer.parseDate("0", d));
        }
    }

    @Test
    public void testTypeSpecimenSplit(){

        String[][] typeStrings = new String[][]{
                new String[]{
                        "Type: Willershausen, ehemalige Ziegelei-Grube am Ostrand der Ortschaft. - Hellgraue, feingeschichtete Mergelsteinknollen, Pliozän.Holotype: STU P 1425.",
                        "STU P 1425",
                        ""},
                new String[]{
                        "Type: Armenia, Shirak distr. in vicinitate pag. Areg. m. Arteni in steppis tragacanthaceis, 1500-1700 m s.m. 9.4.1998, E. Gabrielian legitHolotype: ERE 146518. Isotype(s): B 147519-147520, LE 146520.",
                        "ERE 146518.",
                        "B 147519-147520, LE 146520."}
        };
        for (String[] t: typeStrings) {
            Matcher m = IAPTExcelImport.typeSpecimenSplitPattern.matcher(t[0]);
            assertTrue("typeSpecimenSplitPattern is not matching: " + t[0], m.matches());
            if(!t[1].isEmpty()){
                assertEquals(t[1], m.group("holotype").trim());
            }
            if(!t[2].isEmpty()){
                assertEquals(t[2], m.group("isotype").trim());
            }
        }

    }

    @Test
    public void testSpecimentTypeParser(){

        FieldUnit fu = FieldUnit.NewInstance();
        Collection collection = null;

        String[][] typeStrings = new String[][]{
                new String[]{ "Coll. Lange-Bertalot, Bot. Inst., Univ. Frankfurt/Main, Germany Praep. No. Eu-PL 72", "Praep. No. Eu-PL 72"},
                new String[]{ "LE 1700b-114", "1700b-114"},
                new String[]{ "AD 99530159", "99530159"},
                new String[]{"STU P 1425", "P 1425"},
                new String[]{"GAUF (Gansu Agricultural University) No. 1207-1222", " No. 1207-1222"},
                new String[]{ "KASSEL Coll. Krasske, Praep. DII 78", "Praep. DII 78"},
                new String[]{ "Coll. Lange-Bertalot, Botanisches Institut, Frankfurt am Main slide Eh-B 91", "slide Eh-B 91"},
                new String[]{ "Coll. Østrup, Botan. Museum Copenhagen, Dänemark Praep. 3944", "Praep. 3944"},
                new String[]{ "Coll. L.P.B.V. No. 0736", "No. 0736"},
                new String[]{ "Coll. Ruhr University-Bochum, Inst. of Geology No. 11532", "No. 11532"},
                new String[]{ "Coll. Paläontol. Inst. Univ. Bucuresti. Nr. 2515", "Nr. 2515"},
                new String[]{ "Coll. Dr.h.c. R. Mundlos (Bad Friedrichshall, später Stuttgart) Inv. Nr. P 1396", "Inv. Nr. P 1396"},
                new String[]{ "Inst. Geological Sciences, Acad. Sci. Belarus, Minsk N 212 A", "N 212 A"},
                new String[]{ "Coll. Lange-Bertalot, Bot. Inst., Univ. Frankfurt/Main, Germany"},
                new String[]{ "in coll. H. F. Paulus (Wien)"},
        };

        for (String t[]: typeStrings) {
            DerivedUnit specimen = importer.parseSpecimenType(fu, IAPTExcelImport.TypesName.holotype, collection, t[0], "0");
            assertNotNull("Could not parse: " + t[0], specimen);
            if(t.length > 1){
                assertEquals(t[1], specimen.getAccessionNumber());
            }
        }

    }

    @Test
    public void testParseFieldUnit(){

        String[] typeStrings = new String[]{
                "Lake Bungarby, (36°09'S, 149°08'E), south-eastern New South Wales. - leg. Greg Jordan, Graham Taylor & Leanne Dansie.",
                "Mt. Koghis, Nouvelle-Calédonie (leg. Moser et al., 06.03.1994).",
                "Salt marsh, Wladyslawowo, Puck Bay, Poland (leg. A. Witkowski, 1993).",
                "Blankaart, Woumen (Belgium), sediment sample Jun 1993, core III, 16-17 cm depth (leg. L. Denys, January 1997). In sediment and epiphyton.",
                "Rivière des Lacs, Cascade (Chutes de la Madeleine), Nouvelle-Calédonie (leg. Moser et al., 10.03.1994).",
                "Bulgaria austro-occidentalis. In graminosis saxosis prope vic. Strumesnitza, cca 120 m s.m., Petric district. Leg. D.Delipavlov 03.06.1987.",
                "Lesbos 152, mit sechs Schliffen, ein kleines Geröll mit einem Durchmesser von ca. 4,5 x 5,5 cm. - Strand von Lapsarna, nordwestlich von Antissa. Versteinerter Wald von Lesbos, Griechenland. - Tertiär, Oberoligozän/Untermiozän. - Leg.: E. Velitzelo",
                "Haute Hienghène, au nord-est de l'île Nouvelle-Calédonie. Expression de mousses (leg. Guillaumin).",
                "leg. J. J. Halda 18.3.1997"
        };
        for (String t: typeStrings) {
            assertTrue("collectorPattern is not matching: " + t, IAPTExcelImport.collectorPattern.matcher(t).matches());
        }
    }
}
