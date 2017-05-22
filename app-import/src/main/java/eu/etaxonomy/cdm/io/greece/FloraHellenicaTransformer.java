/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.greece;

import java.util.UUID;

import org.apache.log4j.Logger;

import eu.etaxonomy.cdm.io.common.mapping.InputTransformerBase;

/**
 * @author a.mueller
 * @date 14.12.2016
 *
 */
public class FloraHellenicaTransformer extends InputTransformerBase{

    private static final long serialVersionUID = -3400280126782787668L;
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FloraHellenicaTransformer.class);

    //taxon info feature
    protected static final UUID uuidFloraHellenicaTaxonInfoFeature = UUID.fromString("81f92518-eeff-43d2-8dfd-d277efdbecbe");

    //areas
    protected static final UUID uuidFloraHellenicaAreasVoc = UUID.fromString("4516586a-d988-456f-afbe-ed8f969fd933");

    protected static final UUID uuidAreaGreece = UUID.fromString("0429196a-7a72-4a11-baaf-21a56a6f8726");

    protected static final UUID uuidAreaIoI = UUID.fromString("87ce6f55-b720-48e4-8dd0-4c8a471f0b3b");
    protected static final UUID uuidAreaNPi = UUID.fromString("c701a567-3af8-4b67-918c-c96874ba230c");
    protected static final UUID uuidAreaSPi = UUID.fromString("cba230b1-8053-4803-9218-55af5f70dcde");
    protected static final UUID uuidAreaPe = UUID.fromString("a90daea5-4145-4a0c-964b-75d8c4be7f20");
    protected static final UUID uuidAreaStE = UUID.fromString("e75997fd-f86c-4305-9856-ce6a29e11cd5");
    protected static final UUID uuidAreaEC = UUID.fromString("06d14fb0-4d66-4abf-83f8-a2f84b6d700e");
    protected static final UUID uuidAreaNC = UUID.fromString("6dda6631-f5e3-4e36-aafc-e0bf6880ee89");
    protected static final UUID uuidAreaNE = UUID.fromString("c2877f57-69e5-4bb4-8094-f405f2b54b1b");
    protected static final UUID uuidAreaNAe = UUID.fromString("afd706c1-6b48-4471-b01a-705d8905487d");
    protected static final UUID uuidAreaWAe = UUID.fromString("d7d99e01-297a-400f-8839-a6c2ea478dab");
    protected static final UUID uuidAreaKik = UUID.fromString("951288c4-fcc6-4fa5-a86f-313eb325aa70");
    protected static final UUID uuidAreaKK = UUID.fromString("9ea69f67-710a-4db2-b46b-bb056afd0ed7");
    protected static final UUID uuidAreaEAe = UUID.fromString("06af05de-8d08-42e2-9eec-e3bd9d1d690f");

    //Lifeforms
    protected static final UUID uuidFloraHellenicaLifeformVoc = UUID.fromString("e76daed0-0540-4635-ba9e-04c77e6f379f");

    protected static final UUID uuidLifeformA = UUID.fromString("d27a36ac-25f0-4055-a7fd-93f0eb50782d");
    protected static final UUID uuidLifeformC = UUID.fromString("5bf4a77f-bb56-486b-8be2-98a8edd55f96");
    protected static final UUID uuidLifeformG = UUID.fromString("4c7c857f-ee70-4c22-baee-0b9d1d1f3b37");
    protected static final UUID uuidLifeformH = UUID.fromString("c0ca9ff1-07fe-4601-843f-c29233dd30e1");
    protected static final UUID uuidLifeformP = UUID.fromString("2302f4e8-d525-4e96-9a3f-736a724678c2");
    protected static final UUID uuidLifeformT = UUID.fromString("73eb4a05-b818-4faf-8e35-95cb1759b353");

    //Habitats
    protected static final UUID uuidFloraHellenicaHabitatVoc = UUID.fromString("159ee95c-2d25-48b9-be00-391520d4dda8");
    protected static final UUID uuidHabitatA = UUID.fromString("86938704-b3e7-43c3-838e-09a8e27aa70e");
    protected static final UUID uuidHabitatC = UUID.fromString("c9e11750-a8b7-4b58-8cb4-23997306d27e");
    protected static final UUID uuidHabitatG = UUID.fromString("910beaea-818d-4ef4-8b03-b0f67ca38579");
    protected static final UUID uuidHabitatH = UUID.fromString("768b2e8c-11a8-46d6-aa02-9044d0dbd9aa");
    protected static final UUID uuidHabitatM = UUID.fromString("89c188f8-53ef-44aa-bf09-c0d7fc8634dd");
    protected static final UUID uuidHabitatP = UUID.fromString("10f44eb6-c566-44ed-862b-9952cc0b5fc1");
    protected static final UUID uuidHabitatR = UUID.fromString("ebe8ffba-7155-4dbe-9b09-faa38a61276a");
    protected static final UUID uuidHabitatW = UUID.fromString("b72dc722-d85b-4496-8828-1eeccb37849a");

    //status
    protected static final UUID uuidFloraHellenicaStatusVoc = UUID.fromString("a0d39c0a-4e6f-4d0f-827c-cefd793d5b09");
    protected static final UUID uuidStatusRangeRestricted = UUID.fromString("3ba59506-9f4d-4eb7-b22d-0085f6cc27d0");
    protected static final UUID uuidStatusRangeRestrictedDoubtfully = UUID.fromString("78e64023-e9d6-4bb2-88cd-a400118ddfcf");
    protected static final UUID uuidStatusXenophyte = UUID.fromString("dcc006d1-a6ea-4679-8da3-7496638b1774");
    protected static final UUID uuidStatusXenophyteDoubtfully = UUID.fromString("d0b28959-9b5d-4f88-8eee-0fe35de91692");
    protected static final UUID uuidStatusNative = UUID.fromString("a6d89ab3-23ab-44c8-ab85-895599e88260");

    //Chorological Category
    protected static final UUID uuidFloraHellenicaChorologicalVoc = UUID.fromString("7c95ac7f-e3ea-46c2-863f-9ad663f5086c");
    public static final UUID uuidFloraHellenicaChorologyFeature = UUID.fromString("fab27b99-c480-4873-bcfc-39235f6d0c5d");

    protected static final UUID uuidChorologicalBk = UUID.fromString("53a75041-b8ab-4890-82a3-f29117db8609");
    protected static final UUID uuidChorologicalBI = UUID.fromString("bc031c19-8f30-474d-b091-81795652920e");
    protected static final UUID uuidChorologicalBA = UUID.fromString("4bc0cb8c-5d06-466d-9ace-7f1f4d54f8a3");
    protected static final UUID uuidChorologicalBC = UUID.fromString("f9cfa10d-5d7c-4e5a-a3ca-602420e993fc");
    protected static final UUID uuidChorologicalEM = UUID.fromString("0ba71db1-b1e3-4878-bc0b-b00dc0e020ed");
    protected static final UUID uuidChorological_dEM = UUID.fromString("1b39b3b1-b09a-42b7-8f7d-9e0ee274e313");
    protected static final UUID uuidChorologicalMe = UUID.fromString("3120f73c-252a-4399-a691-7c042f0cf46a");
    protected static final UUID uuidChorologicalMA = UUID.fromString("dafd0326-9c20-42cd-8e48-22c1ec2fb294");
    protected static final UUID uuidChorologicalME = UUID.fromString("166b2e99-9638-4814-85e1-8d83a1b9c134");
    protected static final UUID uuidChorologicalMS = UUID.fromString("54aa0d56-e76a-49fa-bde0-874aa5475e76");
    protected static final UUID uuidChorological_dMS_ = UUID.fromString("d9966842-9926-473e-8edd-bdb1d40c86c5");
    protected static final UUID uuidChorologicalEA = UUID.fromString("8c7124a7-34f6-4c04-bae3-f5e356da8336");
    protected static final UUID uuidChorologicalES = UUID.fromString("d953cd36-c58b-47ee-8347-8d812195ca41");
    protected static final UUID uuidChorologicalEu = UUID.fromString("714e4572-4d31-4529-acb2-1f323c601b0d");
    protected static final UUID uuidChorologicalPt = UUID.fromString("54bce677-c050-44c9-9ee2-8fc15b0cc4f3");
    protected static final UUID uuidChorologicalCt = UUID.fromString("2fdbd40d-3ac4-4a84-a6ff-438c24f9ddd7");
    protected static final UUID uuidChorologicalIT = UUID.fromString("22007c0f-85ec-46e3-b288-88c5e0b7a66a");
    protected static final UUID uuidChorologicalSS = UUID.fromString("d5ccaabf-0f18-4521-979e-9d27c3b712ea");
    protected static final UUID uuidChorological_SS_ = UUID.fromString("97758f76-8f4d-4315-b62e-393e59a9be41");
    protected static final UUID uuidChorologicalST = UUID.fromString("4f1415db-37ac-4f9b-ade8-1df9182350ec");
    protected static final UUID uuidChorologicalBo = UUID.fromString("0760211d-a081-410d-a41d-4f3acfc69ed0");
    protected static final UUID uuidChorologicalAA = UUID.fromString("ddb45c19-135f-49f3-8dc5-04125b546d8e");
    protected static final UUID uuidChorologicalCo = UUID.fromString("9ec7e14c-43a7-4086-9840-9beb6689655d");
    protected static final UUID uuidChorologicalStar = UUID.fromString("2fb30e13-4fa6-46e8-928e-4ddecec63d62");

    protected static final UUID uuidChorologicaltrop = UUID.fromString("fc204567-efac-4677-a978-56e8c1cff5be");
    protected static final UUID uuidChorologicalsubtrop = UUID.fromString("d3acbf5e-ee13-41a0-89f2-520f43b71f59");
    protected static final UUID uuidChorologicalpaleotrop = UUID.fromString("989c6edb-92ed-48f1-b6ed-4f24ecec73c8");
    protected static final UUID uuidChorologicalneotrop = UUID.fromString("92ed494d-f294-424b-a8b6-e9cec6f5c7d3");
    protected static final UUID uuidChorologicalpantrop = UUID.fromString("1b5b74ea-1ebf-4822-b4b6-121c513efe1d");
    protected static final UUID uuidChorologicalN_Am = UUID.fromString("ab2d5adb-b29f-4463-a99a-4c5ed21f131f");
    protected static final UUID uuidChorologicalS_Am = UUID.fromString("6b587c6e-b96a-4d8d-a963-3e1b82aea483");
    protected static final UUID uuidChorologicalE_As = UUID.fromString("9f818f24-87ba-4cf7-adfb-442573ab0e66");
    protected static final UUID uuidChorologicalSE_As = UUID.fromString("f63700d1-3327-4300-ac54-3474c7cd6630");
    protected static final UUID uuidChorologicalS_Afr = UUID.fromString("a2918074-a09e-4491-9789-72984230f647");
    protected static final UUID uuidChorologicalArab = UUID.fromString("ed62dc2f-e741-4728-936d-fc02b74e4ec0");
    protected static final UUID uuidChorologicalArab_NE_Afr = UUID.fromString("07ce7798-585d-4488-a2fa-cdd9464e9761");
    protected static final UUID uuidChorologicalCaucas = UUID.fromString("0656ed2c-5523-4ae1-bc37-75feeed8c914");
    protected static final UUID uuidChorologicalPontic = UUID.fromString("b70cbdc3-8201-4da3-b8ca-cfa2dd2852b9");
    protected static final UUID uuidChorologicalEurop = UUID.fromString("14bad9e4-fd7c-4d6b-910a-d74f48a38eb1");
    protected static final UUID uuidChorologicalAustral = UUID.fromString("13217177-94c4-4b4f-84bb-6b04729e5063");

    protected static final UUID uuidChorological__Co_ = UUID.fromString("72ffbc62-7c0a-4d88-9663-ead7499b8a26");
    protected static final UUID uuidChorologicalW_Med = UUID.fromString("9dca0ce0-177b-4674-b43a-ce2802c27010");
    protected static final UUID uuidChorologicalC_Med = UUID.fromString("0026029d-6aa0-4b71-bcf4-572ed7d2eec1");
    protected static final UUID uuidChorologicalW_Eur = UUID.fromString("e9aeb881-f659-4f77-94bc-140d30d53627");
    protected static final UUID uuidChorologicalS_Eur = UUID.fromString("d4726968-6334-4073-9ff9-271adb8114a8");
    protected static final UUID uuidChorologicalC_Am = UUID.fromString("f93f3e62-048a-4d3a-a481-c3a43c329811");
    protected static final UUID uuidChorologicalC_As = UUID.fromString("3b384884-5862-4426-b906-82d3049cf87a");
    protected static final UUID uuidChorologicalSW_As = UUID.fromString("ce6bd344-049c-4e48-b2c5-dd9baeed73af");
    protected static final UUID uuidChorologicalUnknown = UUID.fromString("e5a98a56-4916-4f3d-849f-10276051208a");
    protected static final UUID uuidChorologicalN_Afr = UUID.fromString("6cf2b057-1c8c-4756-98e6-559c1e330915");
    protected static final UUID uuidChorologicalAm = UUID.fromString("2474ada3-d6ee-402f-a59f-df6164066766");
    protected static final UUID uuidChorologicalPaleosubtrop = UUID.fromString("bd65ea55-6841-424f-8ecc-57c5adb9b558");
    protected static final UUID uuidChorologicalSW_Eur = UUID.fromString("394bb97f-7fbd-44e3-ba2d-af52abf5a302");

    protected static final UUID uuidChorologicalS_As = UUID.fromString("6440fb40-6304-423b-a952-2e99911c4fc5");
    protected static final UUID uuidChorologicalNE_Afr = UUID.fromString("48fb80a7-e350-4563-812b-7931d5cf1675");
    protected static final UUID uuidChorologicalNW_Afr = UUID.fromString("1c713f25-0f1f-43f4-b644-2d985b9d2f6e");
    protected static final UUID uuidChorologicalTrop_Afr = UUID.fromString("a67c04e6-4d1f-4238-9b25-0454d3611513");
    protected static final UUID uuidChorologicalAfr = UUID.fromString("751c28cc-05af-4778-9eba-dfa5b23aa664");
    protected static final UUID uuidChorologicalAs = UUID.fromString("8258ab53-2f30-4b4d-8e67-69219b79eb1b");
    protected static final UUID uuidChorologicalW_As = UUID.fromString("537c7670-3f35-4295-a177-8ac2b3e3315c");
    protected static final UUID uuidChorologicalC_Eur = UUID.fromString("88c129f8-d45b-48f3-b8bc-6f1e4ea1d902");
    protected static final UUID uuidChorologicalE_Afr = UUID.fromString("1a56fc84-93a2-4414-af63-3d3c83f8e8d9");
    protected static final UUID uuidChorologicalW_Austr = UUID.fromString("271d503b-9279-46cb-ac14-184f44e07498");
    protected static final UUID uuidChorologicaltrop_As = UUID.fromString("c357a2e4-85d1-46ab-abb1-df0211f5f8bd");

}
