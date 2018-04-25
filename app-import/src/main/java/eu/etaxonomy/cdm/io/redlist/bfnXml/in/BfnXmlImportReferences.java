/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.redlist.bfnXml.in;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.reference.IBook;
import eu.etaxonomy.cdm.model.reference.IBookSection;
import eu.etaxonomy.cdm.model.reference.IReference;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.reference.ReferenceType;

/**
 * Imports all Reference data, by first testing if they already exist.
 *
 * @author a.mueller
 * @since 23.07.2017
 */
@Component
public class BfnXmlImportReferences extends BfnXmlImportBase  {
    private static final long serialVersionUID = 3545757825059662424L;
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(BfnXmlImportReferences.class);

    public static final UUID uuidSeries = UUID.fromString("766a239f-08e9-45d2-bb13-0756149438e7");
    public static final UUID uuidBand1 = UUID.fromString("887c0dcf-7d1a-489a-bb45-ede77de98de");
    public static final UUID uuidBand2 = UUID.fromString("454dae82-61c2-413b-b183-e874d4e65e31");
    public static final UUID uuidBand3 = UUID.fromString("2c431fe6-95c8-4cc9-9107-33d30b9c2013");
    public static final UUID uuidBand6 = UUID.fromString("77660b1f-9359-4b7f-8114-95551d896b4c");
    public static final UUID uuidBand1_brutvoegel = UUID.fromString("436eb730-cca8-45be-b7f1-e3640e08333f");
    public static final UUID uuidBand1_kriechtiere = UUID.fromString("cd4d0b9c-e334-415b-9a2f-f965fad4ba75");
    public static final UUID uuidBand1_lurche = UUID.fromString("cd4d0b9c-e334-415b-9a2f-f965fad4ba75");
    public static final UUID uuidBand1_saeugetiere = UUID.fromString("8a9f0adf-2f61-4741-a788-36a3943d3e68");
    public static final UUID uuidBand1_suessfische = UUID.fromString("ecc6251c-d71f-4508-8166-e91a7dfbe5d1");

    public static final UUID uuidBand2_bodenlebendenWirbellosenMeerestiere = UUID.fromString("15f4157e-e8a6-43d0-96f4-49fdb8bd8238");
//    public static final UUID uuidBand2_artenarmeWeichtiergruppen = UUID.fromString("e756bbd9-5b20-4a1a-a579-91ffefa6190c");
//    public static final UUID uuidBand2_asselspinnen = UUID.fromString("5606dc08-e82d-4488-a12f-6b2b40f4242a");
//    public static final UUID uuidBand2_flohkrebse = UUID.fromString("54a42fbf-1b39-4037-a9fe-1a8869ce8883");
//    public static final UUID uuidBand2_igelwuermer = UUID.fromString("e91b73fe-0b5e-43b1-a94e-cf9b7cde3f80");
//    public static final UUID uuidBand2_kumazeen = UUID.fromString("d5afc47e-8635-4160-828f-c2fee41709c1");
//    public static final UUID uuidBand2_marineAsseln = UUID.fromString("050c96c4-5b90-4063-8ffc-aa3fb9db896a");
    public static final UUID uuidBand2_marineMakroalgen = UUID.fromString("6a56c6ab-a53e-4a08-af5d-aea2208b1679");
//    public static final UUID uuidBand2_marineMoostierchen = UUID.fromString("5d980095-5e48-4fd8-88a4-fed960d130e2");
//    public static final UUID uuidBand2_marineMuscheln = UUID.fromString("78f76828-7b4d-4955-a9fc-5e2ff700505c");
//    public static final UUID uuidBand2_marineSchnecken = UUID.fromString("438c9573-468c-44bb-bf63-1b4ab09ffa08");
    public static final UUID uuidBand2_meeresfischeUndNeunaugen = UUID.fromString("7de8dc79-6df1-426c-b671-ee8ec08f564c");
//    public static final UUID uuidBand2_nesseltiere = UUID.fromString("b58d00b3-8ba5-4c6a-ba29-9b8372b37c37");
//    public static final UUID uuidBand2_schaedellose = UUID.fromString("5f6e1bd9-fc12-474b-aa29-63a48cfce2f9");
//    public static final UUID uuidBand2_schwaemme = UUID.fromString("31a282a7-c1af-4ef1-bd52-aaea06fcb702");
//    public static final UUID uuidBand2_seepocken = UUID.fromString("d50ea051-9635-49f2-b876-8ce1be4c63a8");
//    public static final UUID uuidBand2_seescheiden = UUID.fromString("83349153-9fef-4826-8b0e-fcf8c762b895");
//    public static final UUID uuidBand2_stachelhaeuter = UUID.fromString("af05b488-3150-417b-8d4c-c67cfa9d05ec");
//    public static final UUID uuidBand2_vielborster = UUID.fromString("0d2db8d8-8100-4994-a672-97a047c78f1e");
//    public static final UUID uuidBand2_wenigborster = UUID.fromString("fcc1822d-4969-41ce-9737-b337ae9525f2");
//    public static final UUID uuidBand2_zehnfusskrebse = UUID.fromString("7c9c97b7-b7f3-4b71-b159-c8170e57031f");

    public static final UUID uuidBand3_ameisen = UUID.fromString("54f77e09-aefe-4148-a865-f6b01e84bccf");
    public static final UUID uuidBand3_bienen = UUID.fromString("8e76fb81-08ea-4fb4-a07f-b5014224355a");
    public static final UUID uuidBand3_binnenmollusken = UUID.fromString("d23781f3-65e8-474b-b227-c4bfa161be45");
    public static final UUID uuidBand3_eulenfalter = UUID.fromString("6fd1df12-0699-44df-aeb1-95e915d319c6");
    public static final UUID uuidBand3_fransenfluegler = UUID.fromString("fccfb2f3-b081-4b72-b8da-d1d913d23c02");
    public static final UUID uuidBand3_heuschrecken = UUID.fromString("879c4642-b4b2-48ae-aedf-6466b03393f5");
    public static final UUID uuidBand3_ohrwuermer = UUID.fromString("3707137c-0021-4d97-9b0f-22ba5533008a");
    public static final UUID uuidBand3_pflanzenwespen = UUID.fromString("f43bb9f7-75f2-4268-9f0a-2a9f787c9f0a");
    public static final UUID uuidBand3_raubfliegen = UUID.fromString("94a37110-56e6-4dc4-93fd-0c1a2838608d");
    public static final UUID uuidBand3_schaben = UUID.fromString("d7b8e917-ca20-4c89-8c1f-bafcbac96618");
    public static final UUID uuidBand3_schwebfliegen = UUID.fromString("48ac08a8-1a94-44fc-9f06-d4776123e87d");
    public static final UUID uuidBand3_spanner = UUID.fromString("db29c468-77f1-4bfb-83d1-335f548e7e8b");
    public static final UUID uuidBand3_spinner = UUID.fromString("b4bec7e2-3f4d-4755-ae15-109343260b00");
    public static final UUID uuidBand3_tagfalter = UUID.fromString("6cdc46c8-b69d-4e6c-9615-6beffc1b668c");
    public static final UUID uuidBand3_tanzfliegen = UUID.fromString("a2b702f3-5a27-4d2a-81fb-ef171043dfd7");
    public static final UUID uuidBand3_wespen = UUID.fromString("65a16438-df68-4972-980f-7de9df038436");
    public static final UUID uuidBand3_zuenslerfalter = UUID.fromString("d3b0ae83-ed88-4674-80be-b7eabcee7b08");

    public static final UUID uuidBand6_flechtenUndPilze = UUID.fromString("de05de86-517c-4d6f-8b59-6608f2f00938");
//    public static final UUID uuidBand6_flechten = UUID.fromString("02333f9e-5c4c-4887-b8e1-317b9ff5b155");
//    public static final UUID uuidBand6_flechtenaehnlichePilze = UUID.fromString("82fd4ff5-9172-4879-a7fb-4953dbffef27");
//    public static final UUID uuidBand6_lichenicolePilze = UUID.fromString("4610b86c-ef4e-484d-bfb3-4ee04b6d53d5");
    public static final UUID uuidBand6_myxomyzeten = UUID.fromString("ae7b2756-253c-4ea0-b2fc-aaf46754d237");

	public BfnXmlImportReferences(){
		super();
	}


	@Override
	public void doInvoke(BfnXmlImportState state){

	    Reference series = (Reference)createSeries(state);
		IBook band1 = createBand1(state, series);
		IBook band2 = createBand2(state, series);
		IBook band3 = createBand3(state, series);
		IBook band6 = createBand6(state, series);

        createBand1_brutvoegel(state, band1);
        createBand1_kriechtiere(state, band1);
        createBand1_lurche(state, band1);
        createBand1_saeugetiere(state, band1);
        createBand1_suessfische(state, band1);

        createBand2_bodenlebendenWirbellosenMeerestiere(state, band1);
//        createBand2_artenarmeWeichtiergruppen(state, band2);
//        createBand2_asselspinnen(state, band2);
//        createBand2_flohkrebse(state, band2);
//        createBand2_igelwuermer(state, band2);
//        createBand2_kumazeen(state, band2);
//        createBand2_marineAsseln(state, band2);
        createBand2_marineMakroalgen(state, band2);
//        createBand2_marineMoostierchen(state, band2);
//        createBand2_marineMuscheln(state, band2);
//        createBand2_marineSchnecken(state, band2);
        createBand2_meeresfischeUndNeunaugen(state, band2);
//        createBand2_nesseltiere(state, band2);
//        createBand2_schaedellose(state, band2);
//        createBand2_schwaemme(state, band2);
//        createBand2_seepocken(state, band2);
//        createBand2_seescheiden(state, band2);
//        createBand2_stachelhaeuter(state, band2);
//        createBand2_vielborster(state, band2);
//        createBand2_wenigborster(state, band2);
//        createBand2_zehnfusskrebse(state, band2);

        createBand3_ameisen(state, band3);
        createBand3_bienen(state, band3);
        createBand3_binnenmollusken(state, band3);
        createBand3_eulenfalter(state, band3);
        createBand3_fransenfluegler(state, band3);
        createBand3_heuschrecken(state, band3);
        createBand3_ohrwuermer(state, band3);
        createBand3_pflanzenwespen(state, band3);
        createBand3_raubfliegen(state, band3);
        createBand3_schaben(state, band3);
        createBand3_schwebfliegen(state, band3);
        createBand3_spanner(state, band3);
        createBand3_spinner(state, band3);
        createBand3_tagfalter(state, band3);
        createBand3_tanzfliegen(state, band3);
        createBand3_wespen(state, band3);
        createBand3_zuenslerfalter(state, band3);


        createBand6_flechtenUndPilze(state, band6);
//        createBand6_flechten(state, band6);
//        createBand6_flechtenaehnlichePilze(state, band6);
//        createBand6_lichenicolePilze(state, band6);
        createBand6_myxomyzeten(state, band6);

	}


    /**
     * @param state
     * @param band6
     * @return
     */
    private Reference createBand6_myxomyzeten(BfnXmlImportState state, IBook band6) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand6_myxomyzeten);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Schleimpilze (Myxomycetes) Deutschlands.");
        booksection.setPages("125-234");
        Team team = makeTeamBand6_myxomyzeten();
        booksection.setAuthorship(team);
        booksection.setTitleCache("SCHNITTLER, M.; KUMMER, V.; KUHNT, A.; KRIEGLSTEINER, L.; FLATAU, L.; MÜLLER, H. und TÄG¬LICH, U. (2011): Rote Liste und Gesamtartenliste der Schleimpilze (Myxomycetes) Deutschlands. – In: LUDWIG, G. & MATZKE-HAJEK, G. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands. Band 6: Pilze (Teil 2) – Flechten und Myxomyzeten. – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (6): 125-234.", true);
        booksection.setAbbrevTitleCache("SCHNITTLER et al. (2011)", true);
        booksection.setInBook(band6);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Team makeTeamBand6_myxomyzeten() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("M.");
        person1.setLastname("Schnittler");
        team.addTeamMember(person1);

        Person person2 = Person.NewInstance();
        person2.setInitials("V.");
        person2.setLastname("Kummer");
        team.addTeamMember(person2);

        Person person3 = Person.NewInstance();
        person3.setInitials("A.");
        person3.setLastname("Kuhnt");
        team.addTeamMember(person3);

        Person person4 = Person.NewInstance();
        person4.setInitials("L.");
        person4.setLastname("Kriegelsteiner");
        team.addTeamMember(person4);

        Person person5 = Person.NewInstance();
        person5.setInitials("L.");
        person5.setLastname("Flatau");
        team.addTeamMember(person5);

        Person person6 = Person.NewInstance();
        person6.setInitials("H.");
        person6.setLastname("Müller");
        team.addTeamMember(person5);

        Person person7 = Person.NewInstance();
        person7.setInitials("U.");
        person7.setLastname("Täglich");
        team.addTeamMember(person7);

        return team;
    }


    /**
     * @param state
     * @param band6
     * @return
     */
    private Reference createBand6_flechtenUndPilze(BfnXmlImportState state, IBook band6) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand6_flechtenUndPilze);
        booksection.setTitle("Rote Liste und Artenverzeichnis der Flechten und flechtenbewohnenden Pilze Deutschlands.");
        booksection.setPages("7-122");
        Team team = makeTeamBand6_flechtenUndPilze();
        booksection.setAuthorship(team);
        booksection.setTitleCache("WIRTH, V.; HAUCK, M.; VON BRACKEL, W.; CEZANNE, R.; DE BRUYN, U.; DÜRHAMMER, O.;  EICHLER, M.; GNÜCHTEL, A.; JOHN, V.; LITTERSKI, B.; OTTE, V.; SCHIEFELBEIN, U.; SCHOLZ, P.; SCHULTZ, M.; STORDEUR, R.; FEUERER, T. und HEINRICH, D. (2011): Rote Liste und Artenverzeichnis der Flechten und flechtenbewohnenden Pilze Deutschlands. – In: LUDWIG, G. & MATZKE-HAJEK, G. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands. Band 6: Pilze (Teil 2) – Flechten und Myxomyzeten. – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (6): 7-122.", true);
        booksection.setAbbrevTitleCache("WIRTH et al. (2011)", true);
        booksection.setInBook(band6);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Team makeTeamBand6_flechtenUndPilze() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("V.");
        person1.setLastname("Wirth");
        team.addTeamMember(person1);

        Person person2 = Person.NewInstance();
        person2.setInitials("M.");
        person2.setLastname("Hauck");
        team.addTeamMember(person2);

        Person person3 = Person.NewInstance();
        person3.setInitials("W.");
        person3.setLastname("von Brackel");
        team.addTeamMember(person3);

        Person person4 = Person.NewInstance();
        person4.setInitials("R.");
        person4.setLastname("Cezanne");
        team.addTeamMember(person4);

        Person person5 = Person.NewInstance();
        person5.setInitials("U.");
        person5.setLastname("de Bruyn");
        team.addTeamMember(person5);

        Person person6 = Person.NewInstance();
        person6.setInitials("O.");
        person6.setLastname("Dürhammer");
        team.addTeamMember(person5);

        Person person7 = Person.NewInstance();
        person7.setInitials("M.");
        person7.setLastname("Eichler");
        team.addTeamMember(person7);

        Person person8 = Person.NewInstance();
        person8.setInitials("A.");
        person8.setLastname("Gnüchtel");
        team.addTeamMember(person8);

        Person person9 = Person.NewInstance();
        person9.setInitials("V.");
        person9.setLastname("John");
        team.addTeamMember(person9);

        Person person10 = Person.NewInstance();
        person10.setInitials("B.");
        person10.setLastname("Litterski");
        team.addTeamMember(person10);

        Person person11 = Person.NewInstance();
        person11.setInitials("V.");
        person11.setLastname("Otte");
        team.addTeamMember(person11);

        Person person12 = Person.NewInstance();
        person12.setInitials("U.");
        person12.setLastname("Schiefelbein");
        team.addTeamMember(person12);

        Person person13 = Person.NewInstance();
        person13.setInitials("P.");
        person13.setLastname("Scholz");
        team.addTeamMember(person13);

        Person person14 = Person.NewInstance();
        person14.setInitials("M.");
        person14.setLastname("Schultz");
        team.addTeamMember(person14);

        Person person15 = Person.NewInstance();
        person15.setInitials("R.");
        person15.setLastname("Stordeur");
        team.addTeamMember(person15);

        Person person16 = Person.NewInstance();
        person16.setInitials("T.");
        person16.setLastname("Feuerer");
        team.addTeamMember(person16);

        Person person17 = Person.NewInstance();
        person17.setInitials("D.");
        person17.setLastname("Heinrich");
        team.addTeamMember(person17);

        return team;

    }


    /**
     * @param state
     * @param band3
     * @return
     */
    private Reference createBand3_zuenslerfalter(BfnXmlImportState state, IBook band3) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand3_zuenslerfalter);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Zünslerfalter (Lepidoptera: Pyraloidea) Deutschlands.");
        booksection.setPages("327-370");
        Person team = makeTeamBand3_zuenslerfalter();
        booksection.setAuthorship(team);
        booksection.setTitleCache("NUSS, M. (2011): Rote Liste und Gesamtartenliste der Zünslerfalter (Lepidoptera: Pyraloidea) Deutschlands. – In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3): 327-370.", true);
        booksection.setAbbrevTitleCache("NUSS (2011)", true);
        booksection.setInBook(band3);
        return saveReference(booksection);
    }


    private Person makeTeamBand3_zuenslerfalter() {
        Person person = Person.NewInstance();
        person.setInitials("M.");
        person.setLastname("Nuss");
        return person;
    }


    /**
     * @param state
     * @param band3
     * @return
     */
    private Reference createBand3_wespen(BfnXmlImportState state, IBook band3) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand3_wespen);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Wespen Deutschlands. Hymenoptera, Aculeata: Grabwespen (Ampulicidae, Crabronidae, Sphecidae), Wegwespen (Pompilidae), Goldwespen (Chrysididae), Faltenwespen (Vespidae), Spinnenameisen (Mutillidae), Dolchwespen (Scoliidae), Rollwespen (Tiphiidae) und Keulhornwespen (Sapygidae).");
        booksection.setPages("419-465");
        Person team = makeTeamBand3_wespen();
        booksection.setAuthorship(team);
        booksection.setTitleCache("SCHMID-EGGER, C. (2011): Rote Liste und Gesamtartenliste der Wespen Deutschlands. Hymenoptera, Aculeata: Grabwespen (Ampulicidae, Crabronidae, Sphecidae), Wegwespen (Pompilidae), Goldwespen (Chrysididae), Faltenwespen (Vespidae), Spinnenameisen (Mutillidae), Dolchwespen (Scoliidae), Rollwespen (Tiphiidae) und Keulhornwespen (Sapygidae). – In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3): 419-465.", true);
        booksection.setAbbrevTitleCache("SCHMID-EGGER (2011)", true);
        booksection.setInBook(band3);
        return saveReference(booksection);
    }

    private Person makeTeamBand3_wespen() {
        Person person = Person.NewInstance();
        person.setInitials("C.");
        person.setLastname("Schmid-Egger");
        return person;
    }


    /**
     * @param state
     * @param band3
     * @return
     */
    private Reference createBand3_tanzfliegen(BfnXmlImportState state, IBook band3) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand3_tanzfliegen);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Langbein-, Tanz- und Rennraubfliegen (Diptera, Empidoidea; Dolichopodidae, Atelestidae, Empididae, Hybotidae, Microphoridae) Deutschlands.");
        booksection.setPages("87-140");
        Team team = makeTeamBand3_tanzfliegen();
        booksection.setAuthorship(team);
        booksection.setTitleCache("MEYER, H. & WAGNER, R. (2011): Rote Liste und Gesamtartenliste der Langbein-, Tanz- und Rennraubfliegen (Diptera, Empidoidea; Dolichopodidae, Atelestidae, Empididae, Hybotidae, Microphoridae) Deutschlands. – In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3): 87-140.", true);
        booksection.setAbbrevTitleCache("MEYER & WAGNER (2011)", true);
        booksection.setInBook(band3);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Team makeTeamBand3_tanzfliegen() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("H.");
        person1.setLastname("Meyer");
        team.addTeamMember(person1);

        Person person2 = Person.NewInstance();
        person2.setInitials("R.");
        person2.setLastname("Wagner");
        team.addTeamMember(person2);

        return team;
    }


    /**
     * @param state
     * @param band3
     * @return
     */
    private Reference createBand3_tagfalter(BfnXmlImportState state, IBook band3) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand3_tagfalter);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Tagfalter (Rhopalocera) (Lepidoptera: Papilionoidea et Hesperioidea) Deutschlands.");
        booksection.setPages("167-194");
        Team team = makeTeamBand3_tagfalter();
        booksection.setAuthorship(team);
        booksection.setTitleCache("REINHARDT, R. & BOLZ, R. (2011): Rote Liste und Gesamtartenliste der Tagfalter (Rhopalocera) (Lepidoptera: Papilionoidea et Hesperioidea) Deutschlands. – In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3): 167-194.", true);
        booksection.setAbbrevTitleCache("REINHARDT & BOLZ (2011)", true);
        booksection.setInBook(band3);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Team makeTeamBand3_tagfalter() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("R.");
        person1.setLastname("Reinhardt");
        team.addTeamMember(person1);

        Person person2 = makeBolz();
        team.addTeamMember(person2);

        return team;
    }


    /**
     * @param state
     * @param band3
     * @return
     */
    private Reference createBand3_spinner(BfnXmlImportState state, IBook band3) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand3_spinner);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Spinnerartigen Falter (Lepidoptera: Bombyces, Sphinges s.l.) Deutschlands.");
        booksection.setPages("243-283");
        Team team = makeTeamBand3_spinner();
        booksection.setAuthorship(team);
        booksection.setTitleCache("RENNWALD, E.: SOBCZYK, T. & HOFMANN, A. (2011): Rote Liste und Gesamtartenliste der Spinnerartigen Falter (Lepidoptera: Bombyces, Sphinges s.l.) Deutschlands. – In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3): 243-283.", true);
        booksection.setAbbrevTitleCache("RENNWALD et al. (2011)", true);
        booksection.setInBook(band3);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Team makeTeamBand3_spinner() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("E.");
        person1.setLastname("Rennwald");
        team.addTeamMember(person1);

        Person person2 = Person.NewInstance();
        person2.setInitials("T.");
        person2.setLastname("Sobczyk");
        team.addTeamMember(person2);

        Person person3 = Person.NewInstance();
        person3.setInitials("A.");
        person3.setLastname("Hofmann");
        team.addTeamMember(person3);

        return team;
    }


    /**
     * @param state
     * @param band3
     * @return
     */
    private Reference createBand3_spanner(BfnXmlImportState state, IBook band3) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand3_spanner);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Spanner, Eulenspinner und Sichelflügler (Lepidoptera: Geometridae et Drepanidae) Deutschlands.");
        booksection.setPages("287-324");
        Team team = makeTeamBand3_spanner();
        booksection.setAuthorship(team);
        booksection.setTitleCache("TRUSCH, R.; GELBRECHT, J.; SCHMIDT, A.; SCHÖNBORN, C.; SCHUMACHER, H.; WEGNER, H. & WOLF, W. (2011): Rote Liste und Gesamtartenliste der Spanner, Eulenspinner und Sichelflügler (Lepidoptera: Geometridae et Drepanidae) Deutschlands. – In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3): 287-324.", true);
        booksection.setAbbrevTitleCache("TRUSCH et al. (2011)", true);
        booksection.setInBook(band3);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Team makeTeamBand3_spanner() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("R.");
        person1.setLastname("Trusch");
        team.addTeamMember(person1);

        Person person2 = Person.NewInstance();
        person2.setInitials("J.");
        person2.setLastname("Gelbrecht");
        team.addTeamMember(person2);

        Person person3 = Person.NewInstance();
        person3.setInitials("A.");
        person3.setLastname("Schmidt");
        team.addTeamMember(person3);

        Person person4 = Person.NewInstance();
        person4.setInitials("C.");
        person4.setLastname("Schönborn");
        team.addTeamMember(person4);

        Person person5 = Person.NewInstance();
        person5.setInitials("H.");
        person5.setLastname("Schumacher");
        team.addTeamMember(person5);

        Person person6 = Person.NewInstance();
        person6.setInitials("H.");
        person6.setLastname("Wegner");
        team.addTeamMember(person5);

        Person person7 = Person.NewInstance();
        person7.setInitials("W.");
        person7.setLastname("Wolf");
        team.addTeamMember(person7);

        return team;
    }


    /**
     * @param state
     * @param band3
     * @return
     */
    private Reference createBand3_schwebfliegen(BfnXmlImportState state, IBook band3) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand3_schwebfliegen);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Schwebfliegen (Diptera: Syrphidae) Deutschlands.");
        booksection.setPages("13-83");
        Team team = makeTeamBand3_schwebfliegen();
        booksection.setAuthorship(team);
        booksection.setTitleCache("SSYMANK, A.; DOCZKAL, D.; RENNWALD, K. & DZIOCK, F. (2011): Rote Liste und Gesamtartenliste der Schwebfliegen (Diptera: Syrphidae) Deutschlands. – In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3): 13-83.", true);
        booksection.setAbbrevTitleCache("SSYMANK et al. (2011)", true);
        booksection.setInBook(band3);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Team makeTeamBand3_schwebfliegen() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("A.");
        person1.setLastname("Ssymank");
        team.addTeamMember(person1);

        Person person2 = Person.NewInstance();
        person2.setInitials("D.");
        person2.setLastname("Doczkal");
        team.addTeamMember(person2);

        Person person3 = Person.NewInstance();
        person3.setInitials("K.");
        person3.setLastname("Rennwald");
        team.addTeamMember(person3);

        Person person4 = Person.NewInstance();
        person4.setInitials("F.");
        person4.setLastname("Dziock");
        team.addTeamMember(person4);

        return team;
    }


    /**
     * @param state
     * @param band3
     * @return
     */
    private Reference createBand3_schaben(BfnXmlImportState state, IBook band3) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand3_schaben);
        booksection.setTitle("Rote Liste der Wildschaben und Gesamtartenliste der Schaben (Blattoptera) Deutschlands.");
        booksection.setPages("609-625");
        Team team = makeTeamBand3_schaben();
        booksection.setAuthorship(team);
        booksection.setTitleCache("KÖHLER, G. & BOHN, H. (2011): Rote Liste der Wildschaben und Gesamtartenliste der Schaben (Blattoptera) Deutschlands. – In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3): 609-625.", true);
        booksection.setAbbrevTitleCache("KÖHLER & BOHN (2011)", true);
        booksection.setInBook(band3);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Team makeTeamBand3_schaben() {
        Team team = Team.NewInstance();

        Person person1 = makeKoehlerG();
        team.addTeamMember(person1);

        Person person2 = Person.NewInstance();
        person2.setInitials("H.");
        person2.setLastname("Bohn");
        team.addTeamMember(person2);

        return team;
    }


    /**
     * @param state
     * @param band3
     * @return
     */
    private Reference createBand3_raubfliegen(BfnXmlImportState state, IBook band3) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand3_raubfliegen);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Raubfliegen (Diptera: Asilidae) Deutschlands.");
        booksection.setPages("143-164");
        Person team = makeTeamBand3_raubfliegen();
        booksection.setAuthorship(team);
        booksection.setTitleCache("WOLFF, D. (2011): Rote Liste und Gesamtartenliste der Raubfliegen (Diptera: Asilidae) Deutschlands. – In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3): 143-164.", true);
        booksection.setAbbrevTitleCache("WOLFF (2011)", true);
        booksection.setInBook(band3);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Person makeTeamBand3_raubfliegen() {
        Person ludwig = Person.NewInstance();
        ludwig.setInitials("D.");
        ludwig.setLastname("Wolff");
        return ludwig;
    }


    /**
     * @param state
     * @param band3
     * @return
     */
    private Reference createBand3_pflanzenwespen(BfnXmlImportState state, IBook band3) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand3_pflanzenwespen);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Pflanzenwespen (Hymenoptera: Symphyta) Deutschlands.");
        booksection.setPages("491-556");
        Team team = makeTeamBand3_pflanzenwespen();
        booksection.setAuthorship(team);
        booksection.setTitleCache("LISTON, A.D.; JANSEN, E.; BLANK, S.M.; KRAUS, M. & TAEGER, A. (2011): Rote Liste und Gesamtartenliste der Pflanzenwespen (Hymenoptera: Symphyta) Deutschlands. – In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3): 491-556.", true);
        booksection.setAbbrevTitleCache("LISTON et al. (2011)", true);
        booksection.setInBook(band3);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Team makeTeamBand3_pflanzenwespen() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("A.D.");
        person1.setLastname("Liston");
        team.addTeamMember(person1);

        Person person2 = Person.NewInstance();
        person2.setInitials("E.");
        person2.setLastname("Jansen");
        team.addTeamMember(person2);

        Person person3 = Person.NewInstance();
        person3.setInitials("S.M.");
        person3.setLastname("Blank");
        team.addTeamMember(person3);

        Person person4 = Person.NewInstance();
        person4.setInitials("M.");
        person4.setLastname("Kraus");
        team.addTeamMember(person4);

        Person person5 = Person.NewInstance();
        person5.setInitials("A.");
        person5.setLastname("Taeger");
        team.addTeamMember(person5);

        return team;
    }


    /**
     * @param state
     * @param band3
     * @return
     */
    private Reference createBand3_ohrwuermer(BfnXmlImportState state, IBook band3) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand3_ohrwuermer);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Ohrwürmer (Dermaptera) Deutsch¬lands.");
        booksection.setPages("629-642");
        Team team = makeTeamBand3_ohrwuermer();
        booksection.setAuthorship(team);
        booksection.setTitleCache("MATZKE, D. & KÖHLER, G. (2011): Rote Liste und Gesamtartenliste der Ohrwürmer (Dermaptera) Deutsch¬lands. – In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3): 629-642.", true);
        booksection.setAbbrevTitleCache("MATZKE & KÖHLER (2011)", true);
        booksection.setInBook(band3);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Team makeTeamBand3_ohrwuermer() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("D.");
        person1.setLastname("Matzke");
        team.addTeamMember(person1);

        Person person2 = makeKoehlerG();
        team.addTeamMember(person2);

        return team;
    }

    private Reference createBand3_heuschrecken(BfnXmlImportState state, IBook band3) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand3_heuschrecken);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Heuschrecken (Saltatoria) Deutschlands.");
        booksection.setPages("577-606");
        Team team = makeTeamBand3_heuschrecken();
        booksection.setAuthorship(team);
        booksection.setTitleCache("MAAS, S.; DETZEL, P. & STAUDT, A. (2011): Rote Liste und Gesamtartenliste der Heuschrecken (Saltatoria) Deutschlands. – In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3): 577-606.", true);
        booksection.setAbbrevTitleCache("MAAS et al. (2011)", true);
        booksection.setInBook(band3);
        return saveReference(booksection);
    }

    /**
     * @return
     */
    private Team makeTeamBand3_heuschrecken() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("S.");
        person1.setLastname("Mass");
        team.addTeamMember(person1);

        Person person2 = Person.NewInstance();
        person2.setInitials("P.");
        person2.setLastname("Detzel");
        team.addTeamMember(person2);

        Person person3 = Person.NewInstance();
        person3.setInitials("A.");
        person3.setLastname("Staudt");
        team.addTeamMember(person3);

        return team;
    }


    private Reference createBand3_fransenfluegler(BfnXmlImportState state, IBook band3) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand3_fransenfluegler);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Fransenflügler (Thysanoptera) Deutschlands.");
        booksection.setPages("559-573");
        Person team = makeTeamBand3_fransenfluegler();
        booksection.setAuthorship(team);
        booksection.setTitleCache("ZUR STRASSEN, R. (2011): Rote Liste und Gesamtartenliste der Fransenflügler (Thysanoptera) Deutschlands. – In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3): 559-573.", true);
        booksection.setAbbrevTitleCache("ZUR STRASSEN (2011)", true);
        booksection.setInBook(band3);
        return saveReference(booksection);
    }

    private Person makeTeamBand3_fransenfluegler() {
        Person person = Person.NewInstance();
        person.setInitials("R.");
        person.setLastname("zur Strassen");
        return person;
    }


    /**
     * @param state
     * @param band3
     * @return
     */
    private Reference createBand3_eulenfalter(BfnXmlImportState state, IBook band3) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand3_eulenfalter);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Eulenfalter, Trägspinner und Graueulchen (Lepidoptera: Noctuoidea) Deutschlands.");
        booksection.setPages("197-239");
        Team team = makeTeamBand3_eulenfalter();
        booksection.setAuthorship(team);
        booksection.setTitleCache("WACHLIN, V. & BOLZ, R. (2011): Rote Liste und Gesamtartenliste der Eulenfalter, Trägspinner und Graueulchen (Lepidoptera: Noctuoidea) Deutschlands. – In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3): 197-239.", true);
        booksection.setAbbrevTitleCache("WACHLIN & BOLZ (2011)", true);
        booksection.setInBook(band3);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Team makeTeamBand3_eulenfalter() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("V.");
        person1.setLastname("Wachlin");
        team.addTeamMember(person1);

        Person person2 = makeBolz();
        team.addTeamMember(person2);

        return team;
    }


    /**
     * @param state
     * @param band3
     * @return
     */
    private Reference createBand3_binnenmollusken(BfnXmlImportState state, IBook band3) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand3_binnenmollusken);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Binnenmollusken (Schnecken und Muscheln; Gastropoda et Bivalvia) Deutschlands.");
        booksection.setPages("647-708");
        Team team = makeTeamBand3_binnenmollusken();
        booksection.setAuthorship(team);
        booksection.setTitleCache("JUNGBLUTH, J. & KNORRE, D. VON (2011): Rote Liste und Gesamtartenliste der Binnenmollusken (Schnecken und Muscheln; Gastropoda et Bivalvia) Deutschlands. – In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3): 647-708.", true);
        booksection.setAbbrevTitleCache("JUNGBLUTH & KNORRE (2011)", true);
        booksection.setInBook(band3);
        return saveReference(booksection);
    }

    private Team makeTeamBand3_binnenmollusken() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("J.");
        person1.setLastname("Jungbluth");
        team.addTeamMember(person1);

        Person person2 = Person.NewInstance();
        person2.setInitials("D. von");
        person2.setLastname("Knorre");
        team.addTeamMember(person2);

        return team;
    }


    /**
     * @param state
     * @param band3
     * @return
     */
    private Reference createBand3_bienen(BfnXmlImportState state, IBook band3) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand3_bienen);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Bienen (Hymenoptera, Apidae) Deutschlands.");
        booksection.setPages("373-416");
        Team team = makeTeamBand3_bienen();
        booksection.setAuthorship(team);
        booksection.setTitleCache("WESTRICH, P.; FROMMER, U.; MANDERY, K.; RIEMANN, H.; RUHNKE, H.; SAURE, C. & VOITH, J. (2011): Rote Liste und Gesamtartenliste der Bienen (Hymenoptera, Apidae) Deutschlands. – In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3): 373-416.", true);
        booksection.setAbbrevTitleCache("WESTRICH et al. (2011)", true);
        booksection.setInBook(band3);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Team makeTeamBand3_bienen() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("P.");
        person1.setLastname("Westrich");
        team.addTeamMember(person1);

        Person person2 = Person.NewInstance();
        person2.setInitials("U.");
        person2.setLastname("Frommer");
        team.addTeamMember(person2);

        Person person3 = Person.NewInstance();
        person3.setInitials("K.");
        person3.setLastname("Mandery");
        team.addTeamMember(person3);

        Person person4 = Person.NewInstance();
        person4.setInitials("H.");
        person4.setLastname("Riemann");
        team.addTeamMember(person4);

        Person person5 = Person.NewInstance();
        person5.setInitials("H.");
        person5.setLastname("Ruhnke");
        team.addTeamMember(person5);

        Person person6 = Person.NewInstance();
        person6.setInitials("C.");
        person6.setLastname("Saure");
        team.addTeamMember(person5);

        Person person7 = Person.NewInstance();
        person7.setInitials("J.");
        person7.setLastname("Voith");
        team.addTeamMember(person7);

        return team;
    }


    /**
     * @param state
     * @param band3
     * @return
     */
    private Reference createBand3_ameisen(BfnXmlImportState state, IBook band3) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand3_ameisen);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Ameisen (Hymenoptera: Formicidae) Deutschlands.");
        booksection.setPages("469-487");
        Person team = makeTeamBand3_ameisen();
        booksection.setAuthorship(team);
        booksection.setTitleCache("SEIFERT, B. (2011): Rote Liste und Gesamtartenliste der Ameisen (Hymenoptera: Formicidae) Deutschlands. – In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3): 469-487.", true);
        booksection.setAbbrevTitleCache("SEIFERT (2011)", true);
        booksection.setInBook(band3);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Person makeTeamBand3_ameisen() {
        Person person = Person.NewInstance();
        person.setInitials("B.");
        person.setLastname("Seifert");
        return person;
    }


    /**
     * @param state
     * @param band2
     * @return
     */
    private Reference createBand2_meeresfischeUndNeunaugen(BfnXmlImportState state, IBook band2) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand2_meeresfischeUndNeunaugen);
        booksection.setTitle("Rote Liste und Gesamtartenliste der etablierten Fische und Neunaugen (Elasmobranchii, Actinopterygii & Petromyzontida) der marinen Gewässer Deutschlands.");
        booksection.setPages("11-76");
        Team team = makeTeamBand2_meeresfischeUndNeunaugen();
        booksection.setAuthorship(team);
        booksection.setTitleCache("THIEL, R.; WINKLER, H.; BÖTTCHER, U.; DÄNHARDT, A.; FRICKE, R.; GEORGE, M.; KLOPP¬MANN, M.; SCHAARSCHMIDT, T.; UBL, C. & VORBERG, R. (2013): Rote Liste und Gesamtartenliste der etablierten Fische und Neunaugen (Elasmobranchii, Actinopterygii & Petromyzontida) der marinen Gewässer Deutschlands. – In: BECKER, N.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G. & NEHRING, S. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 2: Meeresorganismen. – Münster (Landwirt¬schaftsverlag). – Naturschutz und Biologische Vielfalt 70 (2): 11-76.", true);
        booksection.setAbbrevTitleCache("THIEL et al. (2013)", true);
        booksection.setInBook(band2);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Team makeTeamBand2_meeresfischeUndNeunaugen() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("R.");
        person1.setLastname("Thiel");
        team.addTeamMember(person1);

        Person person2 = Person.NewInstance();
        person2.setInitials("H.");
        person2.setLastname("Winkler");
        team.addTeamMember(person2);

        Person person3 = Person.NewInstance();
        person3.setInitials("U.");
        person3.setLastname("Böttcher");
        team.addTeamMember(person3);

        Person person4 = Person.NewInstance();
        person4.setInitials("A.");
        person4.setLastname("Dänhardt");
        team.addTeamMember(person4);

        Person person5 = Person.NewInstance();
        person5.setInitials("R.");
        person5.setLastname("Fricke");
        team.addTeamMember(person5);

        Person person6 = Person.NewInstance();
        person6.setInitials("M.");
        person6.setLastname("George");
        team.addTeamMember(person5);

        Person person7 = Person.NewInstance();
        person7.setInitials("M.");
        person7.setLastname("Kloppmann");
        team.addTeamMember(person7);

        Person person8 = Person.NewInstance();
        person8.setInitials("T.");
        person8.setLastname("Schaarschmidth");
        team.addTeamMember(person8);

        Person person9 = Person.NewInstance();
        person9.setInitials("C.");
        person9.setLastname("Ubl");
        team.addTeamMember(person9);

        Person person10 = Person.NewInstance();
        person10.setInitials("R.");
        person10.setLastname("Vorberg");
        team.addTeamMember(person10);

        return team;
    }


    /**
     * @param state
     * @param band2
     * @return
     */
    private Reference createBand2_marineMakroalgen(BfnXmlImportState state, IBook band2) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand2_marineMakroalgen);
        booksection.setTitle("Rote Liste und Gesamtartenliste der marinen Makroalgen (Chlorophyta, Phaeophyceae et Rhodophyta) Deutschlands.");
        booksection.setPages("179-229");
        Team team = makeTeamBand2_marineMakroalgen();
        booksection.setAuthorship(team);
        booksection.setTitleCache("SCHORIES, D.; KUHLENKAMP, R.; SCHUBERT, H. & SELIG, U. (2013): Rote Liste und Gesamtartenliste der marinen Makroalgen (Chlorophyta, Phaeophyceae et Rhodophyta) Deutschlands. – In: BECKER, N.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G. & NEHRING, S. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 2: Meeresorganismen. – Münster (Landwirt¬schaftsverlag). – Naturschutz und Biologische Vielfalt 70 (2): 179-229.", true);
        booksection.setAbbrevTitleCache("SCHORIES et al. (2013)", true);
        booksection.setInBook(band2);
        return saveReference(booksection);
    }



    /**
     * @return
     */
    private Team makeTeamBand2_marineMakroalgen() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("D.");
        person1.setLastname("Schories");
        team.addTeamMember(person1);

        Person person2 = Person.NewInstance();
        person2.setInitials("R.");
        person2.setLastname("Kuhlenkamp");
        team.addTeamMember(person2);

        Person person3 = Person.NewInstance();
        person3.setInitials("H.");
        person3.setLastname("Schubert");
        team.addTeamMember(person3);

        Person person4 = Person.NewInstance();
        person4.setInitials("U.");
        person4.setLastname("Selig");
        team.addTeamMember(person4);

        return team;
    }


    /**
     * @param state
     * @param band2
     * @return
     */
    private Reference createBand2_bodenlebendenWirbellosenMeerestiere(BfnXmlImportState state, IBook band2) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand2_bodenlebendenWirbellosenMeerestiere);
        booksection.setTitle("Rote Liste und Artenlisten der bodenlebenden wirbellosen Meerestiere.");
        booksection.setPages("81-176");
        Team team = makeTeamBand2_bodenlebendenWirbellosenMeerestiere();
        booksection.setAuthorship(team);
        booksection.setTitleCache("RACHOR, E.; BÖNSCH, R.; BOOS, K.; GOSSELCK, F.; GROTJAHN, M.; GÜNTER, C.-P.; GUSKY, M.; GUTOW, L.; HEIBER, W.; JANTSCHIK, P.; KRIEG, H.-J.; KRONE, R.; NEHMER, P.; REICHERT, K.; REISS, H.; SCHRÖDER, A.; WITT, J. & ZETTLER, M.L. (2013): Rote Liste und Artenlisten der bodenlebenden wirbellosen Meerestiere. – In: BECKER, N.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G. & NEHRING, S. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 2: Meeresorganismen. – Münster (Landwirt¬schaftsverlag). – Naturschutz und Biologische Vielfalt 70 (2): 81-176.", true);
        booksection.setAbbrevTitleCache("RACHOR et al. (2013)", true);
        booksection.setInBook(band2);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Team makeTeamBand2_bodenlebendenWirbellosenMeerestiere() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("E.");
        person1.setLastname("Rachor");
        team.addTeamMember(person1);

        Person person2 = Person.NewInstance();
        person2.setInitials("R.");
        person2.setLastname("Bönsch");
        team.addTeamMember(person2);

        Person person3 = Person.NewInstance();
        person3.setInitials("K.");
        person3.setLastname("Boos");
        team.addTeamMember(person3);

        Person person4 = Person.NewInstance();
        person4.setInitials("F.");
        person4.setLastname("Gosselck");
        team.addTeamMember(person4);

        Person person5 = Person.NewInstance();
        person5.setInitials("M.");
        person5.setLastname("Grotjahn");
        team.addTeamMember(person5);

        Person person6 = Person.NewInstance();
        person6.setInitials("C.-P.");
        person6.setLastname("Günter");
        team.addTeamMember(person5);

        Person person7 = Person.NewInstance();
        person7.setInitials("M.");
        person7.setLastname("Gusky");
        team.addTeamMember(person7);

        Person person8 = Person.NewInstance();
        person8.setInitials("L.");
        person8.setLastname("Gutow");
        team.addTeamMember(person8);

        Person person9 = Person.NewInstance();
        person9.setInitials("W.");
        person9.setLastname("Heiber");
        team.addTeamMember(person9);

        Person person10 = Person.NewInstance();
        person10.setInitials("P.");
        person10.setLastname("Jantschik");
        team.addTeamMember(person10);

        Person person11 = Person.NewInstance();
        person11.setInitials("H.-J.");
        person11.setLastname("Krieg");
        team.addTeamMember(person11);

        Person person12 = Person.NewInstance();
        person12.setInitials("R.");
        person12.setLastname("Krone");
        team.addTeamMember(person12);

        Person person13 = Person.NewInstance();
        person13.setInitials("P.");
        person13.setLastname("Nehmer");
        team.addTeamMember(person13);

        Person person14 = Person.NewInstance();
        person14.setInitials("K.");
        person14.setLastname("Reichert");
        team.addTeamMember(person14);

        Person person15 = Person.NewInstance();
        person15.setInitials("H.");
        person15.setLastname("Reiss");
        team.addTeamMember(person15);

        Person person16 = Person.NewInstance();
        person16.setInitials("A.");
        person16.setLastname("Schröder");
        team.addTeamMember(person16);

        Person person17 = Person.NewInstance();
        person17.setInitials("J.");
        person17.setLastname("Witt");
        team.addTeamMember(person17);

        Person person18 = Person.NewInstance();
        person18.setInitials("M.L.");
        person18.setLastname("Zettler");
        team.addTeamMember(person18);

        return team;
    }


    /**
     * @param state
     * @param band1
     * @return
     */
    private Reference createBand1_suessfische(BfnXmlImportState state, IBook band1) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand1_suessfische);
        booksection.setTitle("Rote Liste der im Süßwasser reproduzierenden Neunaugen und Fische (Cyclostomata & Pisces).");
        booksection.setPages("291-316");
        Person team = makeTeamBand1_suessfische();
        booksection.setAuthorship(team);
        booksection.setTitleCache("FREYHOF, J. (2009): Rote Liste der im Süßwasser reproduzierenden Neunaugen und Fische (Cyclostomata & Pisces). – In: HAUPT, H.; LUDWIG, G.; GRUTTKE, H.; BINOT-HAFKE, M.; OTTO, C. & PAULY, A. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 1: Wirbeltiere. – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (1): 291-316.", true);
        booksection.setAbbrevTitleCache("FREYHOF et al. (2009)", true);
        booksection.setInBook(band1);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Person makeTeamBand1_suessfische() {
        Person person = Person.NewInstance();
        person.setInitials("J.");
        person.setLastname("Freyhof");
        return person;
    }


    /**
     * @param state
     * @param band1
     * @return
     */
    private Reference createBand1_saeugetiere(BfnXmlImportState state, IBook band1) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand1_saeugetiere);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Säugetiere (Mammalia) Deutschlands.");
        booksection.setPages("115-153");
        Team team = makeTeamBand1_saeugetiere();
        booksection.setAuthorship(team);
        booksection.setTitleCache("MEINIG, H.; BOYE, P. & HUTTERER, R. (2009): Rote Liste und Gesamtartenliste der Säugetiere (Mammalia) Deutschlands. – In: HAUPT, H.; LUDWIG, G.; GRUTTKE, H.; BINOT-HAFKE, M.; OTTO, C. & PAULY, A. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 1: Wirbeltiere. – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (1): 115-153.", true);
        booksection.setAbbrevTitleCache("MEINIG et al. (2009)", true);
        booksection.setInBook(band1);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Team makeTeamBand1_saeugetiere() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("H.");
        person1.setLastname("Meinig");
        team.addTeamMember(person1);

        Person person2 = makeBoye();
        team.addTeamMember(person2);

        Person person3 = Person.NewInstance();
        person3.setInitials("R.");
        person3.setLastname("Hutterer");
        team.addTeamMember(person3);

        return team;
    }


    /**
     * @param state
     * @param band1
     * @return
     */
    private Reference createBand1_lurche(BfnXmlImportState state, IBook band1) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand1_lurche);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Lurche (Amphibia) Deutschlands.");
        booksection.setPages("259-288");
        Team team = makeTeamBand1_lurcheUndKriechtiere();
        booksection.setAuthorship(team);
        booksection.setTitleCache("KÜHNEL, K.-D.; GEIGER, A.; LAUFER, H.; PODLOUCKY, R. & SCHLÜPMANN, M. (2009): Rote Liste und Gesamtartenliste der Lurche (Amphibia) Deutschlands. – In: HAUPT, H.; LUDWIG, G.; GRUTTKE, H.; BINOT-HAFKE, M.; OTTO, C. & PAULY, A. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 1: Wirbeltiere. – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (1): 259-288.", true);
        booksection.setAbbrevTitleCache("KÜHNEL et al. (2009)", true);
        booksection.setInBook(band1);
        return saveReference(booksection);
    }


    /**
     * @return
     */
    private Team makeTeamBand1_lurcheUndKriechtiere() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("K.-D.");
        person1.setLastname("Kühnel");
        team.addTeamMember(person1);

        Person person2 = Person.NewInstance();
        person2.setInitials("A.");
        person2.setLastname("Geiger");
        team.addTeamMember(person2);

        Person person3 = Person.NewInstance();
        person3.setInitials("H.");
        person3.setLastname("Laufer");
        team.addTeamMember(person3);

        Person person4 = Person.NewInstance();
        person4.setInitials("R.");
        person4.setLastname("Podloucky");
        team.addTeamMember(person4);

        Person person5 = Person.NewInstance();
        person5.setInitials("M.");
        person5.setLastname("Schlüpmann");
        team.addTeamMember(person5);

        return team;
    }


    /**
     * @param state
     * @param band1
     * @return
     */
    private Reference createBand1_kriechtiere(BfnXmlImportState state, IBook band1) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand1_kriechtiere);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Kriechtiere (Reptilia) Deutschlands.");
        booksection.setPages("231-256");
        Team team = makeTeamBand1_lurcheUndKriechtiere();
        booksection.setAuthorship(team);
        booksection.setTitleCache("KÜHNEL, K.-D.; GEIGER, A.; LAUFER, H.; PODLOUCKY, R. & SCHLÜPMANN, M. (2009): Rote Liste und Gesamtartenliste der Kriechtiere (Reptilia) Deutschlands. – In: HAUPT, H.; LUDWIG, G.; GRUTTKE, H.; BINOT-HAFKE, M.; OTTO, C. & PAULY, A. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 1: Wirbeltiere. – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (1): 231-256.", true);
        booksection.setAbbrevTitleCache("KÜHNEL et al. (2009)", true);
        booksection.setInBook(band1);
        Reference result = (Reference)booksection;
        return saveReference(result);
    }


    /**
     * @param state
     * @param band1
     * @return
     */
    private Reference createBand1_brutvoegel(BfnXmlImportState state, IBook band1) {
        IBookSection booksection = ReferenceFactory.newBookSection();
        booksection.setUuid(uuidBand1_brutvoegel);
        booksection.setInBook(band1);
        booksection.setTitle("Rote Liste und Gesamtartenliste der Brutvögel (Aves) Deutschlands.");
        booksection.setPages("159-227");
        Team team = makeTeamBand1_brutvoegel();
        booksection.setAuthorship(team);
        booksection.setTitleCache("SÜDBECK, P.; BAUER, H.-G.; BOSCHERT, M.; BOYE, P. & KNIEF, W. (2009): Rote Liste und Gesamtartenliste der Brutvögel (Aves) Deutschlands. – In: HAUPT, H.; LUDWIG, G.; GRUTTKE, H.; BINOT-HAFKE, M.; OTTO, C. & PAULY, A. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 1: Wirbeltiere. – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (1): 159-227.", true);
        booksection.setAbbrevTitleCache("SÜDBECK et al. (2009)", true);
        Reference result = (Reference)booksection;
        return saveReference(result);
    }



    /**
     * @return
     */
    private Team makeTeamBand1_brutvoegel() {
        Team team = Team.NewInstance();

        Person person1 = Person.NewInstance();
        person1.setInitials("P.");
        person1.setLastname("Südbeck");
        team.addTeamMember(person1);

        Person person2 = Person.NewInstance();
        person2.setInitials("H.-G.");
        person2.setLastname("Bauer");
        team.addTeamMember(person2);

        Person person3 = Person.NewInstance();
        person3.setInitials("M.");
        person3.setLastname("Boschert");
        team.addTeamMember(person3);

        Person person4 = makeBoye();
        team.addTeamMember(person4);

        Person person5 = Person.NewInstance();
        person5.setInitials("W.");
        person5.setLastname("Knief");
        team.addTeamMember(person5);

        return team;
    }


    /**
     * @param state
     * @return
     */
    private Reference createBand6(BfnXmlImportState state, Reference series) {
        IBook result = ReferenceFactory.newBook();
        result.setUuid(uuidBand6);
        result.setInSeries(series);
        result.setTitle("Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 6: Pilze (Teil 2).");
        result.setPlacePublished("Münster (Landwirtschaftsverlag)");
        result.setSeriesPart("Naturschutz und Biologische Vielfalt 70 (6)");
        Team team = makeTeamBand6();
        result.setAuthorship(team);
        result.setTitleCache("LUDWIG, G. & MATZKE-HAJEK, G. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands. Band 6: Pilze (Teil 2) – Flechten und Myxomyzeten. – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (6)", true);
        return saveReference(result);
    }


    /**
     * @return
     */
    private Team makeTeamBand6() {
//        In: LUDWIG, G. & MATZKE-HAJEK, G. (Red.)
      Team team = Team.NewInstance();

      Person ludwig = makeLudwig();
      team.addTeamMember(ludwig);

      Person matzkeHajek = makeMatzkeHajek();
      team.addTeamMember(matzkeHajek);

      return team;
    }


    /**
     * @param state
     * @return
     */
    private Reference createBand3(BfnXmlImportState state, Reference series) {
        IBook result = ReferenceFactory.newBook();
        result.setUuid(uuidBand3);
        result.setInSeries(series);
        result.setTitle("Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1).");
        result.setPlacePublished("Münster (Landwirtschaftsverlag)");
        result.setSeriesPart("Naturschutz und Biologische Vielfalt 70 (3)");
        Team team = makeTeamBand3();
        result.setAuthorship(team);
        result.setTitleCache("BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 3: Wirbellose Tiere (Teil 1). – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (3)", true);
        return saveReference(result);
    }


    /**
     * @return
     */
    private Team makeTeamBand3() {
//        In: BINOT-HAFKE, M.; BALZER, S.; BECKER, N.; GRUTTKE, H.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G.; MATZKE-HAJEK, G. & STRAUCH, M. (Red.)
        Team team = Team.NewInstance();

        Person person1 = makeBinotHafke();
        team.addTeamMember(person1);

        Person person2 = Person.NewInstance();
        person2.setInitials("S.");
        person2.setLastname("Balzer");
        team.addTeamMember(person2);

        Person person3 = makeBeckerN();
        team.addTeamMember(person3);

        Person person4 = makeGruttkeH();
        team.addTeamMember(person4);

        Person person5 = makeHaupt();
        team.addTeamMember(person5);

        Person person6 = makeHofbauer();
        team.addTeamMember(person5);

        Person person7 = makeLudwig();
        team.addTeamMember(person7);

        Person person8 = makeMatzkeHajek();
        team.addTeamMember(person8);

        Person person9 = Person.NewInstance();
        person9.setInitials("M.");
        person9.setLastname("Strauch");
        team.addTeamMember(person9);

        return team;
    }


    /**
     * @param state
     * @return
     */
    private Reference createBand2(BfnXmlImportState state, Reference series) {
        IBook result = ReferenceFactory.newBook();
        result.setUuid(uuidBand2);
        result.setInSeries(series);
        result.setTitle("Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 2: Meeresorganismen.");
        result.setPlacePublished("Münster (Landwirtschaftsverlag)");
        result.setSeriesPart("Naturschutz und Biologische Vielfalt 70 (2)");
        Team team = makeTeamBand2();
        result.setAuthorship(team);
        result.setTitleCache("BECKER, N.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G. & NEHRING, S. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 2: Meeresorganismen. – Münster (Landwirt¬schaftsverlag). – Naturschutz und Biologische Vielfalt 70 (2)", true);
        return saveReference(result);
    }


    /**
     * @return
     */
    private Team makeTeamBand2() {
//      In: BECKER, N.; HAUPT, H.; HOFBAUER, N.; LUDWIG, G. & NEHRING, S. (Red.)

        Team team = Team.NewInstance();

        Person becker = makeBeckerN();
        team.addTeamMember(becker);

        Person haupt = makeHaupt();
        team.addTeamMember(haupt);

        Person hofbauer = makeHofbauer();
        team.addTeamMember(hofbauer);

        Person ludwig = makeLudwig();
        team.addTeamMember(ludwig);

        Person nehring = Person.NewInstance();
        nehring.setInitials("S.");
        nehring.setLastname("Nehring");
        team.addTeamMember(nehring);

        return team;
    }


    /**
     * @param state
     * @return
     */
    private Reference createBand1(BfnXmlImportState state, Reference series) {
        IBook result = ReferenceFactory.newBook();
        result.setUuid(uuidBand1);
        result.setInSeries(series);
        result.setTitle("Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 1: Wirbeltiere.");
        result.setPlacePublished("Münster (Landwirtschaftsverlag)");
        result.setSeriesPart("Naturschutz und Biologische Vielfalt 70 (1)");
        result.setTitleCache("HAUPT, H.; LUDWIG, G.; GRUTTKE, H.; BINOT-HAFKE, M.; OTTO, C. & PAULY, A. (Red.): Rote Liste gefährdeter Tiere, Pflanzen und Pilze Deutschlands, Band 1: Wirbeltiere. – Münster (Landwirtschaftsverlag). – Naturschutz und Biologische Vielfalt 70 (1)", true);
        Team team = makeTeamBand1();

        result.setAuthorship(team);
        return saveReference(result);
    }


    /**
     * @return
     */
    protected Team makeTeamBand1() {
        Team team = Team.NewInstance();

        Person haupt = makeHaupt();
        team.addTeamMember(haupt);

        Person ludwig = makeLudwig();
        team.addTeamMember(ludwig);

        Person gruttke = makeGruttkeH();
        team.addTeamMember(gruttke);

        Person binotHafke = makeBinotHafke();
        team.addTeamMember(binotHafke);

        Person otto = Person.NewInstance();
        otto.setInitials("C.");
        otto.setLastname("Otto");
        team.addTeamMember(otto);

        Person pauly = Person.NewInstance();
        pauly.setInitials("A.");
        pauly.setLastname("Pauly");
        team.addTeamMember(pauly);
        return team;
    }


    /**
     * @return
     */
    private Person makeBinotHafke() {
        Person binotHafke = Person.NewInstance();
        binotHafke.setInitials("M.");
        binotHafke.setLastname("Binot-Hafke");
        return binotHafke;
    }

    private Person makeHaupt() {
        Person haupt = Person.NewInstance();
        haupt.setInitials("H.");
        haupt.setLastname("Haupt");
        return haupt;
    }

    private Person makeLudwig() {
        Person ludwig = Person.NewInstance();
        ludwig.setInitials("G.");
        ludwig.setLastname("Ludwig");
        return ludwig;
    }

    private Person makeBolz() {
        Person person = Person.NewInstance();
        person.setInitials("R.");
        person.setLastname("Bolz");
        return person;
    }

    private Person makeKoehlerG() {
        Person person = Person.NewInstance();
        person.setInitials("G.");
        person.setLastname("Köhler");
        return person;
    }

    private Person makeHofbauer() {
        Person person = Person.NewInstance();
        person.setInitials("N.");
        person.setLastname("Hofbauer");
        return person;
    }

    private Person makeBoye() {
        Person person = Person.NewInstance();
        person.setInitials("P.");
        person.setLastname("Boye");
        return person;
    }

    private Person makeBeckerN() {
        Person person = Person.NewInstance();
        person.setInitials("N.");
        person.setLastname("Becker");
        return person;
    }

    private Person makeGruttkeH() {
        Person person = Person.NewInstance();
        person.setInitials("H.");
        person.setLastname("Gruttke");
        return person;
    }

    private Person makeMatzkeHajek() {
        Person person = Person.NewInstance();
        person.setInitials("G.");
        person.setLastname("Matzke-Hajek");
        return person;
    }

    /**
     * @param state
     * @return
     */
    private IReference createSeries(BfnXmlImportState state) {
        Reference result = ReferenceFactory.newPrintSeries("Rote Listen 2009");
        result.setUuid(uuidSeries);
        return saveReference(result);
    }


    /**
     * @param result
     */
    private Reference saveReference(IReference ref) {
        Reference result = getReferenceService().find(ref.getUuid());
        if (result == null){
            result = (Reference)ref;
            if (result.getType().equals(ReferenceType.BookSection)){
//                result.setProtectedTitleCache(false);
                result.setTitleCache(result.getAbbrevTitleCache(), true);
            }
            getReferenceService().saveOrUpdate(result);
        }
        return result;
    }


    @Override
    public boolean doCheck(BfnXmlImportState state){
        boolean result = true;
        return result;
    }

    @Override
	protected boolean isIgnore(BfnXmlImportState state){
		return ! state.getConfig().isDoFeature();
	}

}
