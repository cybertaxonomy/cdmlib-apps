// $Id$
/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/

package eu.etaxonomy.cdm.io.cuba;

import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.io.cyprus.CyprusTransformer;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;

/**
 * @author a.mueller
 * @created 01.03.2010
 */
public final class CubaTransformer extends InputTransformerBase {
    private static final long serialVersionUID = 1070018208741186271L;

    @SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(CubaTransformer.class);

    //presenceTerm
    public static final UUID nonNativeDoubtfullyNaturalisedUuid = UUID.fromString("a1e26234-831e-4190-9fe3-011aca09ddba");
    public static final UUID adventiveAlienUuid = UUID.fromString("06e48a0b-3e48-4ef8-9bdd-0755880e99ce");
    public static final UUID cultivatedOnlyUuid = UUID.fromString("9cca5c3b-aced-41c4-accc-f800b67168f8");
    public static final UUID occasionallyCultivatedUuid = UUID.fromString("936c3f9a-6099-4322-9792-0a72c6c2ce25");
    public static final UUID doubtfulIndigenousUuid = UUID.fromString("f47f4f4e-9d84-459a-b747-27a1af24ab7a");
    public static final UUID doubtfulIndigenousDoubtfulUuid = UUID.fromString("7ddfd94d-01a4-496c-a6d6-18584c00af59");

    public static final UUID doubtfullyNaturalisedUuid = UUID.fromString("9e0b413b-5a68-4e5b-91f2-227b4f832466");
    public static final UUID doubtfullyNonNativeUuid = UUID.fromString("c42ca644-1773-4230-a2ee-328a5d4a21ab");

    public static final UUID endemicInErrorUuid = UUID.fromString("679b215d-c231-4ee2-ae12-3ffc3dd528ad");
    public static final UUID adventiveInErrorUuid = UUID.fromString("9b910b7b-43e3-4260-961c-6063b11cb7dc");
    public static final UUID nonNativeInErrorUuid = UUID.fromString("b9153d90-9e31-465a-a28c-79077a8ed4c2");
    public static final UUID naturalisedInErrorUuid = UUID.fromString("8d918a37-3add-4e1c-a233-c37dbee209aa");


//    public static final UUID indigenousUuid = UUID.fromString("b325859b-504b-45e0-9ef0-d5c1602fcc0f");
//    public static final UUID casualUuid = UUID.fromString("5e81353c-38a3-4ca6-b979-0d9abc93b877");
//    public static final UUID nonInvasiveUuid = UUID.fromString("1b025e8b-901a-42e8-9739-119b410c6f03");
//    public static final UUID invasiveUuid = UUID.fromString("faf2d271-868a-4bf7-b0b8-a1c5ab309de2");
//    public static final UUID questionableUuid = UUID.fromString("4b48f675-a6cf-49f3-a5ba-77e2c2979eb3");
//
//    public static final UUID indigenousDoubtfulUuid = UUID.fromString("17bc601f-53eb-4997-a4bc-c03ce5bfd1d3");
//    public static final UUID casualDoubtfulUuid = UUID.fromString("73f75493-1185-4a3e-af1e-9a1f2e8dadb7");
//    public static final UUID nonInvasiveDoubtfulUuid = UUID.fromString("11f56e2f-c16c-4b3d-a870-bb5d3b20e624");
//    public static final UUID invasiveDoubtfulUuid = UUID.fromString("ac429d5f-e8ad-49ae-a41c-e4779b58b96a");
//    public static final UUID questionableDoubtfulUuid = UUID.fromString("914e7393-1314-4632-bc45-5eff3dc1e424");
//
//    public static final UUID cultivatedDoubtfulUuid = UUID.fromString("4f31bfc8-3058-4d83-aea5-3a1fe9773f9f");


    //Named Areas
    public static final UUID uuidCubaVocabulary = UUID.fromString("2119f610-1f93-4d87-af28-40aeefaca100");
//    public static final UUID uuidCyprusDivisionsAreaLevel = UUID.fromString("ff52bbd9-f73d-4476-af39-f3991fa892bd");

    public static final UUID uuidCuba = UUID.fromString("d0144a6e-0e17-4a1d-bce5-d464a2aa7229");

    public static final UUID uuidWesternCuba = UUID.fromString("53ee35a5-03dd-4c1e-9b23-1a0d08489684");
    public static final UUID uuidPinarDelRio = UUID.fromString("ee1bae89-b6a8-4b89-a864-238fe3c4dbf3");
    public static final UUID uuidHabana = UUID.fromString("ff906b63-6e74-4a38-b492-73d32817ad3a");
    public static final UUID uuidCiudadHabana = UUID.fromString("af96dc90-ef03-4e2c-9c65-50dd1cca4f4f");
    public static final UUID uuidMayabeque = UUID.fromString("7813fc47-1038-49d0-9fc7-07d245abcf2d");
    public static final UUID uuidArtemisa = UUID.fromString("7e236945-1097-43f7-9deb-dedf3f45dfe1");
    public static final UUID uuidMatanzas = UUID.fromString("bc280278-1b67-4766-ba28-e3a5c215d6a9");
    public static final UUID uuidIslaDeLaJuventud = UUID.fromString("0e0683d6-90d4-4b0f-834e-05737ca9b2b4");

    public static final UUID uuidCentralCuba = UUID.fromString("25eb5879-358e-4ff7-837d-101569d5d843");
    public static final UUID uuidVillaClara = UUID.fromString("4de5e35d-fdad-49d0-a5c8-dc44e4e844a0");
    public static final UUID uuidCienfuegos = UUID.fromString("c4189205-4543-4f4d-b211-6f899734a2f1");
    public static final UUID uuidSanctiSpiritus = UUID.fromString("70ee99b9-d006-4a08-a8c2-19269d60865f");
    public static final UUID uuidCiegoDeAvila = UUID.fromString("2ce7f4bc-4142-4866-b156-cf5300973c6d");
    public static final UUID uuidCamaguey = UUID.fromString("4a4e2ab9-bce1-4018-8654-e7dfe6ea9a0f");
    public static final UUID uuidLasTunas = UUID.fromString("6e0e9c28-23fe-4ea7-8ae1-75e1dce385e9");

    public static final UUID uuidEastCuba = UUID.fromString("6a9ed0e2-7d3a-4620-9376-720c166674ee");
    public static final UUID uuidGranma = UUID.fromString("7098418f-992a-4888-b4a0-722870bc7c69");
    public static final UUID uuidHolguin = UUID.fromString("1595e1f2-5ae6-4db7-982c-552c26130051");
    public static final UUID uuidSantiagoDeCuba = UUID.fromString("838a2e44-f3cc-4d89-83a8-6f83c6f9726d");
    public static final UUID uuidGuantanamo = UUID.fromString("3a76b1af-da3a-44a3-859f-eeba5ad6f58b");

    public static final UUID uuidEspanola = UUID.fromString("27d9d0f3-cd5f-4f3e-979d-64e47b6b5768");
    public static final UUID uuidJamaica = UUID.fromString("2825a0ee-0bd1-49d5-afb4-bac80db5551f");
    public static final UUID uuidPuertoRico = UUID.fromString("9fca701d-4899-4266-a29f-0d136670c795");
    public static final UUID uuidSmallerAntilles = UUID.fromString("9720309a-2467-4aad-992e-b3c34b95d8d7");
    public static final UUID uuidBahamas = UUID.fromString("23615e27-e916-48f4-8d49-8c148106216c");
    public static final UUID uuidCaymanIslands = UUID.fromString("f53ea0b5-3bca-4e95-9a10-2d13edcd7501");
    public static final UUID uuidNorthAmerica = UUID.fromString("5ab08324-baa2-4121-8c37-e1d558b51f2f");
    public static final UUID uuidCentralAmerica = UUID.fromString("4d972402-5ef8-43d8-a377-f0b6dd88d32e");
    public static final UUID uuidSouthAmerica = UUID.fromString("0fccc041-ce9d-40d5-8b9b-d7d833feed38");
    public static final UUID uuidOldWorld = UUID.fromString("c6b45544-01df-4c97-bb29-9058964c5b57");

//    public static final UUID uuid = UUID.fromString("");
//    public static final UUID uuid = UUID.fromString("");
//    public static final UUID uuid = UUID.fromString("");

    private static final UUID uuidCubaTdwg = UUID.fromString("66ce6bb4-b48e-483b-aed1-62646e9c80b8");
    @Override
    public UUID getNamedAreaUuid(String key) throws UndefinedTransformerMethodException {
        if (StringUtils.isBlank(key)){return null;


        }else if (key.equalsIgnoreCase("C")){return uuidCuba;
        }else if (key.equalsIgnoreCase("CuW")){return uuidWesternCuba;
        }else if (key.equalsIgnoreCase("PR*")){return uuidPinarDelRio;
        }else if (key.equalsIgnoreCase("PR PR*")){return uuidPinarDelRio;
//        }else if (key.equals("HAB")){return uuidHabana;
        }else if (key.equalsIgnoreCase("Hab*")){return uuidCiudadHabana;
        }else if (key.equalsIgnoreCase("Hab(*)")){return uuidCiudadHabana;
        }else if (key.equalsIgnoreCase("May")){return uuidMayabeque;
        }else if (key.equalsIgnoreCase("Art")){return uuidArtemisa;

        }else if (key.equalsIgnoreCase("Mat")){return uuidMatanzas;
        }else if (key.equalsIgnoreCase("IJ")){return uuidIslaDeLaJuventud;

        }else if (key.equalsIgnoreCase("CuC")){return uuidCentralCuba;
        }else if (key.equalsIgnoreCase("VC")){return uuidVillaClara;
        }else if (key.equalsIgnoreCase("Ci")){return uuidCienfuegos;
        }else if (key.equalsIgnoreCase("SS")){return uuidSanctiSpiritus;
        }else if (key.equalsIgnoreCase("CA")){return uuidCiegoDeAvila;
        }else if (key.equalsIgnoreCase("Cam")){return uuidCamaguey;
        }else if (key.equalsIgnoreCase("LT")){return uuidLasTunas;

        }else if (key.equalsIgnoreCase("CuE")){return uuidEastCuba;
        }else if (key.equalsIgnoreCase("Gr")){return uuidGranma;
        }else if (key.equalsIgnoreCase("Ho")){return uuidHolguin;
        }else if (key.equalsIgnoreCase("SC")){return uuidSantiagoDeCuba;
        }else if (key.equalsIgnoreCase("Gu")){return uuidGuantanamo;

        }else if (key.equalsIgnoreCase("Esp")){return uuidEspanola;
        }else if (key.equalsIgnoreCase("Ja")){return uuidJamaica;
        }else if (key.equalsIgnoreCase("PR")){return uuidPuertoRico;
        }else if (key.equalsIgnoreCase("Men")){return uuidSmallerAntilles;
        }else if (key.equalsIgnoreCase("Bah")){return uuidBahamas;
        }else if (key.equalsIgnoreCase("Cay")){return uuidCaymanIslands;
        }else if (key.equalsIgnoreCase("AmN")){return uuidNorthAmerica;
        }else if (key.equalsIgnoreCase("AmC")){return uuidCentralAmerica;
        }else if (key.equalsIgnoreCase("AmS")){return uuidSouthAmerica;
        }else if (key.equalsIgnoreCase("VM")){return uuidOldWorld;
        }else{
            return null;
        }
    }



    @Override
    public UUID getPresenceTermUuid(String key) throws UndefinedTransformerMethodException {
        if (StringUtils.isBlank(key)){return null;
        }else if (key.equalsIgnoreCase("Ind.")){return CyprusTransformer.indigenousUuid;
        }else if (key.equalsIgnoreCase("+")){return CyprusTransformer.indigenousUuid;
        }else if (key.equalsIgnoreCase("?Ind.")){return CyprusTransformer.indigenousDoubtfulUuid;
        }else if (key.equalsIgnoreCase("?")){return CyprusTransformer.indigenousDoubtfulUuid;
        }else if (key.equalsIgnoreCase("?Cult.")){return CyprusTransformer.cultivatedDoubtfulUuid;


        }else if (key.equalsIgnoreCase("Ind.?")){return doubtfulIndigenousUuid;
        }else if (key.equalsIgnoreCase("D")){return doubtfulIndigenousUuid;
        }else if (key.equalsIgnoreCase("?Ind.?")){return doubtfulIndigenousUuid;
        }else if (key.equalsIgnoreCase("??")){return doubtfulIndigenousDoubtfulUuid;

        }else if (key.equalsIgnoreCase("Dud.")){return nonNativeDoubtfullyNaturalisedUuid;
        }else if (key.equalsIgnoreCase("P")){return nonNativeDoubtfullyNaturalisedUuid;

        }else if (key.equalsIgnoreCase("Adv.")){return adventiveAlienUuid;
        }else if (key.equalsIgnoreCase("A")){return adventiveAlienUuid;

        }else if (key.equalsIgnoreCase("Cult.")){return cultivatedOnlyUuid;
        }else if (key.equalsIgnoreCase("C")){return cultivatedOnlyUuid;
        }else if (key.equalsIgnoreCase("(C)")){return occasionallyCultivatedUuid;

        }else if (key.equalsIgnoreCase("?Nat.")){return doubtfullyNaturalisedUuid;
        }else if (key.equalsIgnoreCase("?N")){return doubtfullyNaturalisedUuid;
        }else if (key.equalsIgnoreCase("?Dud.")){return doubtfullyNonNativeUuid;
        }else if (key.equalsIgnoreCase("?P")){return doubtfullyNonNativeUuid;
        }else if (key.equalsIgnoreCase("-End.")){return endemicInErrorUuid;
        }else if (key.equalsIgnoreCase("-E")){return endemicInErrorUuid;
        }else if (key.equalsIgnoreCase("-Nat.")){return naturalisedInErrorUuid;
        }else if (key.equalsIgnoreCase("-N")){return naturalisedInErrorUuid;
        }else if (key.equalsIgnoreCase("-Dud.")){return nonNativeInErrorUuid;
        }else if (key.equalsIgnoreCase("-P")){return nonNativeInErrorUuid;
        }else if (key.equalsIgnoreCase("-Adv.")){return adventiveInErrorUuid;
        }else if (key.equalsIgnoreCase("-A")){return adventiveInErrorUuid;


        }else{
            return null;
        }
    }


    @Override
    public PresenceAbsenceTerm getPresenceTermByKey(String key) throws UndefinedTransformerMethodException {
        if (StringUtils.isBlank(key)){return null;

        }else if (key.equalsIgnoreCase("E")){return PresenceAbsenceTerm.ENDEMIC_FOR_THE_RELEVANT_AREA();

        }else if (key.equalsIgnoreCase("-Ind.")){return PresenceAbsenceTerm.NATIVE_REPORTED_IN_ERROR();

        }else if (key.equalsIgnoreCase("Nat.")){return PresenceAbsenceTerm.NATURALISED();
        }else if (key.equalsIgnoreCase("N")){return PresenceAbsenceTerm.NATURALISED();

        }else if (key.equalsIgnoreCase("Cult.")){return PresenceAbsenceTerm.CULTIVATED();
        }else if (key.equalsIgnoreCase("C")){return PresenceAbsenceTerm.CULTIVATED();

        }else if (key.equalsIgnoreCase("-Cult.")){return PresenceAbsenceTerm.CULTIVATED_REPORTED_IN_ERROR();

        }else{
            return null;
        }
    }

}
