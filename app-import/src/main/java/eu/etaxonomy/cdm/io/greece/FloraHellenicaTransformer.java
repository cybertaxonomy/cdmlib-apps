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

    //Chorological Category
    protected static final UUID uuidFloraHellenicaChorologicalVoc = UUID.fromString("7c95ac7f-e3ea-46c2-863f-9ad663f5086c");
    //Chorological Category
    public static final UUID uuidFloraHellenicaChorologyFeature = UUID.fromString("fab27b99-c480-4873-bcfc-39235f6d0c5d");

    protected static final UUID uuidChorologicalBk = UUID.fromString("53a75041-b8ab-4890-82a3-f29117db8609");
    protected static final UUID uuidChorologicalBI = UUID.fromString("bc031c19-8f30-474d-b091-81795652920e");
    protected static final UUID uuidChorologicalBA = UUID.fromString("4bc0cb8c-5d06-466d-9ace-7f1f4d54f8a3");
    protected static final UUID uuidChorologicalBC = UUID.fromString("f9cfa10d-5d7c-4e5a-a3ca-602420e993fc");
    protected static final UUID uuidChorologicalEM = UUID.fromString("0ba71db1-b1e3-4878-bc0b-b00dc0e020ed");
    protected static final UUID uuidChorologicalMe = UUID.fromString("3120f73c-252a-4399-a691-7c042f0cf46a");
    protected static final UUID uuidChorologicalMA = UUID.fromString("dafd0326-9c20-42cd-8e48-22c1ec2fb294");
    protected static final UUID uuidChorologicalME = UUID.fromString("166b2e99-9638-4814-85e1-8d83a1b9c134");
    protected static final UUID uuidChorologicalMS = UUID.fromString("54aa0d56-e76a-49fa-bde0-874aa5475e76");
    protected static final UUID uuidChorologicalEA = UUID.fromString("8c7124a7-34f6-4c04-bae3-f5e356da8336");
    protected static final UUID uuidChorologicalES = UUID.fromString("d953cd36-c58b-47ee-8347-8d812195ca41");
    protected static final UUID uuidChorologicalEu = UUID.fromString("714e4572-4d31-4529-acb2-1f323c601b0d");
    protected static final UUID uuidChorologicalPt = UUID.fromString("54bce677-c050-44c9-9ee2-8fc15b0cc4f3");
    protected static final UUID uuidChorologicalCt = UUID.fromString("2fdbd40d-3ac4-4a84-a6ff-438c24f9ddd7");
    protected static final UUID uuidChorologicalIT = UUID.fromString("22007c0f-85ec-46e3-b288-88c5e0b7a66a");
    protected static final UUID uuidChorologicalSS = UUID.fromString("d5ccaabf-0f18-4521-979e-9d27c3b712ea");
    protected static final UUID uuidChorologicalST = UUID.fromString("4f1415db-37ac-4f9b-ade8-1df9182350ec");
    protected static final UUID uuidChorologicalBo = UUID.fromString("0760211d-a081-410d-a41d-4f3acfc69ed0");
    protected static final UUID uuidChorologicalAA = UUID.fromString("ddb45c19-135f-49f3-8dc5-04125b546d8e");
    protected static final UUID uuidChorologicalCo = UUID.fromString("9ec7e14c-43a7-4086-9840-9beb6689655d");
    protected static final UUID uuidChorologicalStar = UUID.fromString("2fb30e13-4fa6-46e8-928e-4ddecec63d62");


}
