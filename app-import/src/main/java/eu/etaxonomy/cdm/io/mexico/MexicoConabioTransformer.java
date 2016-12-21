/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.mexico;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;
import eu.etaxonomy.cdm.io.common.mapping.UndefinedTransformerMethodException;
import eu.etaxonomy.cdm.model.description.PresenceAbsenceTerm;
import eu.etaxonomy.cdm.model.name.Rank;

/**
 * @author a.mueller
 * @date 16.06.2016
 *
 */
public class MexicoConabioTransformer extends InputTransformerBase{

    private static final long serialVersionUID = -5618949703252325141L;

    private static final Logger logger = Logger.getLogger(MexicoConabioTransformer.class);

    public static UUID uuidTropicosNameIdentifier = UUID.fromString("6205e531-75b0-4f2a-9a9c-b1247fb080ab");
    public static UUID uuidReferenceBorhidi = UUID.fromString("f76a535b-a1fd-437c-a09d-f94eddae2b5e");
    public static UUID uuidReferenceConabio = UUID.fromString("7183652f-d7ca-45a7-9383-265996d3d10f");

    public static final UUID uuidNomRefExtension = UUID.fromString("0a7e2f5f-c62d-43e1-874a-07cb1dbb9fa0");

    public static final UUID uuidStatusExotica = UUID.fromString("f7264a1d-5037-4d9a-9758-d81fea057e1b");
    public static final UUID uuidStatusExoticaInvasora = UUID.fromString("4d135810-4af8-48ca-82f4-d4741d00d067");

    public static final UUID uuidMexicanStatesVoc = UUID.fromString("88556d4c-a171-4e14-ac56-3154d88f429b");

    public static final UUID uuidMexicoCountry = UUID.fromString("b451ccd8-dd40-4804-b507-1ba46fad151a");
    public static final UUID uuidAguascalientes = UUID.fromString("fff6867a-bfa4-479b-8f35-bf273a790c8f");
    public static final UUID uuidBaja_california = UUID.fromString("a76e3818-37b6-4854-aaf7-fa02d0a99d02");
    public static final UUID uuidBaja_california_sur = UUID.fromString("f298f095-5d55-49bd-9e1c-6aedee9e53ae");
    public static final UUID uuidCampeche = UUID.fromString("1f955f0f-1a63-406b-951f-426f55f32f54");
    public static final UUID uuidCoahuila_de_zaragoza = UUID.fromString("eb9ff874-d598-4b1c-b1b8-97c33c69155f");
    public static final UUID uuidColima = UUID.fromString("f3da7526-21a9-4ac0-bf3f-56bc9ea699d5");
    public static final UUID uuidChiapas = UUID.fromString("413e70cc-a39c-4b00-af60-82a3650aba0f");
    public static final UUID uuidChihuahua = UUID.fromString("8a94f64d-822b-4dea-9161-9f130ca22850");
    public static final UUID uuidDistrito_federal = UUID.fromString("5686a49e-e40d-4bc5-b0c7-d2bcb489baed");
    public static final UUID uuidDurango = UUID.fromString("a0b4c414-95c1-4d1f-b6de-589baec83890");
    public static final UUID uuidGuanajuato = UUID.fromString("641de041-f468-4c46-84f8-bbc9e6eb2db0");
    public static final UUID uuidGuerrero = UUID.fromString("2c4a7e98-154c-480c-804d-0acf285087b4");
    public static final UUID uuidHidalgo = UUID.fromString("40c40034-c712-4793-87ba-1523d0a17411");
    public static final UUID uuidJalisco = UUID.fromString("315c8fb8-08d6-4acc-acad-d7c3ab02a6b7");
    public static final UUID uuidMexico = UUID.fromString("0730370d-66c8-4e1f-a40b-c8941edc12d3");
    public static final UUID uuidMichoacan_de_ocampo = UUID.fromString("aaf89369-139b-4524-8f2d-5010aab755c7");
    public static final UUID uuidMorelos = UUID.fromString("c9a5cd7e-6828-4505-8e74-b2e0e56d0f0a");
    public static final UUID uuidNayarit = UUID.fromString("6f3cf768-7b22-4351-98db-e47f88501d8c");
    public static final UUID uuidNuevo_leon = UUID.fromString("5ce156f4-db40-4404-8610-b46727e93252");
    public static final UUID uuidOaxaca = UUID.fromString("47a15fd7-4d5f-4d03-949b-0ffc873f112e");
    public static final UUID uuidPuebla = UUID.fromString("e1c52112-cb27-4bdf-a891-9da8eed591e8");
    public static final UUID uuidQueretaro_de_arteaga = UUID.fromString("d09512dc-d3c8-4454-a595-cd610790e9b9");
    public static final UUID uuidQuintana_roo = UUID.fromString("bf2ade90-12fd-48eb-9425-9932a0333d90");
    public static final UUID uuidSan_luis_potosi = UUID.fromString("cd54fd65-b5f2-4109-bd5e-6cb935eace1f");
    public static final UUID uuidSinaloa = UUID.fromString("924dc40f-63f3-4a25-9b64-8700710409de");
    public static final UUID uuidSonora = UUID.fromString("2b8dc6bb-5ccc-44bc-b34b-17b9faa620cb");
    public static final UUID uuidTabasco = UUID.fromString("430f94ee-6e12-40c4-be39-7563017c359a");
    public static final UUID uuidTamaulipas = UUID.fromString("99caa082-d67e-4025-9d73-110f0edf862d");
    public static final UUID uuidTlaxcala = UUID.fromString("a06acf81-8d76-40ec-83fa-15a0fa0e8b95");
    public static final UUID uuidVeracruz_de_ignacio_de_la_llave = UUID.fromString("0f98fcb1-7345-49f5-af6f-5f781a525a3a");
    public static final UUID uuidYucatan = UUID.fromString("5dd5d5fa-77f0-42f6-a45c-384130e5f16d");
    public static final UUID uuidZacatecas = UUID.fromString("0e9bc1f2-0154-4424-85c7-1bfc8cf3696c");


    public static final UUID uuidMexicanLanguagesVoc = UUID.fromString("d37d043e-94af-4cb0-b702-e6f45318b039");

    public static final UUID uuidChontal = UUID.fromString("e2ea70d0-605b-4e21-9848-aefe38ea6abb");
    public static final UUID uuidChinanteco = UUID.fromString("18394cf7-9027-4253-be77-a136f8293d37");
    public static final UUID uuidChiapaneca = UUID.fromString("bd98cda2-3fc3-432a-b283-06acbeba054c");
    public static final UUID uuidHuasteco = UUID.fromString("dff1a7d4-7520-4ad3-9cdb-7fa9d8ee4989");
    public static final UUID uuidEspanol_Maya = UUID.fromString("ebd5fb21-628c-424e-9add-bf29a1e51b28");
    public static final UUID uuidGuarijio = UUID.fromString("f7064648-a033-40f5-b271-9c577c123a30");
    public static final UUID uuidHuave = UUID.fromString("494abcf2-d027-4cc4-a48e-4c5a55e205a9");
    public static final UUID uuidEspanol = UUID.fromString("fffdc8a6-a8c3-4914-b108-d3397140a692");
    public static final UUID uuidMaya = UUID.fromString("3c1a6981-13c1-410f-8b8d-b99612db7aba");
    public static final UUID uuidLacandon = UUID.fromString("6ca007a9-6c7b-4381-a5ae-c29177b6ee6c");
    public static final UUID uuidIngles = UUID.fromString("90affeb7-cb08-4095-98de-74923c02e98f");
    public static final UUID uuidItzmal = UUID.fromString("68c0c780-c724-419f-9797-2311d091d817");
    public static final UUID uuidNahuatl = UUID.fromString("4c487f72-2e57-4a8c-96c2-52047c812cda");
    public static final UUID uuidTarahumara = UUID.fromString("98e67390-b41f-4763-ac27-eddf978f4e5d");
    public static final UUID uuidOtomi = UUID.fromString("0b7a6fdb-a797-4551-81b8-d2ad51048d4a");
    public static final UUID uuidMixe = UUID.fromString("9c9a9752-a3b4-46b6-bb9a-481e22027d4d");
    public static final UUID uuidTseltal = UUID.fromString("7cc0b188-cfa6-42e0-9641-38aeb6a6ec3b");
    public static final UUID uuidZapoteco = UUID.fromString("9ad65652-5b14-4580-9b4e-28e0a28753b4");
    public static final UUID uuidTotonaco = UUID.fromString("2eae8d72-a45c-48be-b9a4-aaf96d21df0a");
    public static final UUID uuidTarasco = UUID.fromString("cf597916-0422-495f-bf0b-ea0841e32a52");


    @Override
    public Rank getRankByKey(String key) throws UndefinedTransformerMethodException {
        if (StringUtils.isBlank(key)){
            return null;
        }else if (key.equals("orden")) {return Rank.ORDER();
        }else if (key.equals("subclase")){return Rank.SUBCLASS();
        }else if (key.equals("clase")){return Rank.CLASS();
        }else if (key.equals("familia")){return Rank.FAMILY();
        }else if (key.equals("tribu")){return Rank.TRIBE();
        }else if (key.equals("superorden")){return Rank.SUPERORDER();
        }else if (key.equals("división")){return Rank.DIVISION();

        }else if (key.equals("género")){return Rank.GENUS();
        }else if (key.equals("especie")){return Rank.SPECIES();
        }else if (key.equals("subespecie")){return Rank.SUBSPECIES();
        }else if (key.equals("variedad")){return Rank.VARIETY();
        }else{
            logger.warn("Rank not defined: " + key);
            return null;
        }
    }


    @Override
    public PresenceAbsenceTerm getPresenceTermByKey(String key) throws UndefinedTransformerMethodException {
        if (StringUtils.isBlank(key)){
            return null;
        }else if (key.equals("Endémica")){
            return PresenceAbsenceTerm.ENDEMIC_FOR_THE_RELEVANT_AREA();
        }else{
            return null;
        }
    }


    @Override
    public UUID getPresenceTermUuid(String key) throws UndefinedTransformerMethodException {
        if (StringUtils.isBlank(key)){
            return null;
        }else if (key.equals("Exótica")){
            return uuidStatusExotica;
        }else if (key.equals("Exótica-Invasora")){
            return uuidStatusExoticaInvasora;
        }else{
            logger.warn("PresenceTerm not yet implemented: " + key);
            return null;
        }
    }



}
