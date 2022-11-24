/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.salvador;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.ICdmApplication;
import eu.etaxonomy.cdm.app.berlinModelImport.BerlinModelSources;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.berlinModel.in.BerlinModelImportConfigurator;
import eu.etaxonomy.cdm.io.common.CdmDefaultImport;
import eu.etaxonomy.cdm.io.common.IImportConfigurator;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.CHECK;
import eu.etaxonomy.cdm.io.common.IImportConfigurator.DO_REFERENCES;
import eu.etaxonomy.cdm.io.common.ImportResult;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.NomenclaturalCode;
import eu.etaxonomy.cdm.model.permission.GrantedAuthorityImpl;
import eu.etaxonomy.cdm.model.permission.Group;
import eu.etaxonomy.cdm.model.permission.User;
import eu.etaxonomy.cdm.persistence.permission.Role;

/**
 * TODO add the following to a wiki page:
 * HINT: If you are about to import into a mysql data base running under windows and if you wish to dump and restore the resulting data bas under another operation systen
 * you must set the mysql system variable lower_case_table_names = 0 in order to create data base with table compatible names.
 *
 * @author a.mueller
 */
public class SalvadorActivator {

    private static final Logger logger = LogManager.getLogger();

	//database validation status (create, update, validate ...)
	static DbSchemaValidation hbm2dll = DbSchemaValidation.CREATE;
	static final Source berlinModelSource = BerlinModelSources.El_Salvador();
//	static final ICdmDataSource cdmDestination = CdmDestinations.localH2();
//    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_salvador_preview();
    static final ICdmDataSource cdmDestination = CdmDestinations.cdm_production_salvador();
	static final UUID treeUuid = UUID.fromString("b010c84d-6049-45f4-9f13-c065101eaa26");
	static final UUID secUuid = UUID.fromString("d03ef02a-f226-4cb1-bdb4-f6c154f08a34");
	static final int sourceSecId = 7331;

	static final UUID featureTreeUuid = UUID.fromString("9d0e5628-2eda-43ed-bc59-138a7e39ce56");
	static final Object[] featureKeyList = new Integer[]{302, 303, 306, 307, 309, 310, 311, 312, 1500, 1800, 1900, 1950, 1980, 2000};
	static boolean isIgnore0AuthorTeam = true;  //special case for Salvador.
	static boolean doExport = false;
	static boolean useClassification = true;

	//check - import
	static final CHECK check = CHECK.IMPORT_WITHOUT_CHECK;
	static final IImportConfigurator.EDITOR editor = IImportConfigurator.EDITOR.EDITOR_AS_EDITOR;

	//NomenclaturalCode
	static final NomenclaturalCode nomenclaturalCode  = NomenclaturalCode.ICNAFP;

	//ignore null
	static final boolean ignoreNull = true;

    static final boolean isSalvador = true;

// ****************** ALL *****************************************

	//authors
	static final boolean doAuthors = true;
	//references
	static final DO_REFERENCES doReferences =  DO_REFERENCES.ALL;
	//names
	static final boolean doTaxonNames = true;
	static final boolean doRelNames = true;
	static final boolean doNameStatus = true;
	static final boolean doTypes = false;  //Types do not exist in El_Salvador DB
	static final boolean doNameFacts = false;  //Name Facts do not exist in El_Salvador DB

	//taxa
	static final boolean doTaxa = true;
	static final boolean doRelTaxa = true;
	static final boolean doFacts = true;
	static final boolean doOccurences = false; //Occurrences do not exist in El_Salvador DB
	static final boolean doCommonNames = false; //CommonNames do not exist in Salvador DB

	//etc.
	static final boolean doMarker = false;   //#3937  markers must not be imported
	static final boolean doUser = true;


	static String factFilter = " factCategoryFk NOT IN  ("
//	        + "302, 303, 306, 307, 309, 311, 310, "
	        + "1980, 1500, 1950, 1700, 350) ";


// ************************ NONE **************************************** //

//	//authors
//	static final boolean doAuthors = false;
//	//references
//	static final DO_REFERENCES doReferences =  DO_REFERENCES.CONCEPT_REFERENCES;
//	//names
//	static final boolean doTaxonNames = false;
//	static final boolean doRelNames = false;
//	static final boolean doNameStatus = false;
//	static final boolean doTypes = false;
//	static final boolean doNameFacts = false;
//
//	//taxa
//	static final boolean doTaxa = false;
//	static final boolean doRelTaxa = false;
//	static final boolean doFacts = false;
//	static final boolean doOccurences = false;
//	static final boolean doCommonNames = false;
//
//	//etc.
//	static final boolean doMarker = false;
//	static final boolean doUser = true;


	public ImportResult doImport(ICdmDataSource destination){
		System.out.println("Start import from BerlinModel("+ berlinModelSource.getDatabase() + ") ...");

		//make BerlinModel Source
		Source source = berlinModelSource;

		BerlinModelImportConfigurator config = BerlinModelImportConfigurator.NewInstance(source,  destination);

		config.setClassificationUuid(treeUuid);
		config.setSecUuid(secUuid);
		config.setSourceSecId(sourceSecId);
		config.setNomenclaturalCode(nomenclaturalCode);
		config.setIgnoreNull(ignoreNull);

		config.setDoAuthors(doAuthors);
		config.setDoReferences(doReferences);
		config.setDoTaxonNames(doTaxonNames);
		config.setDoRelNames(doRelNames);
		config.setDoNameStatus(doNameStatus);
		config.setDoTypes(doTypes);
		config.setDoNameFacts(doNameFacts);

		config.setDoTaxa(doTaxa);
		config.setDoRelTaxa(doRelTaxa);
		config.setDoFacts(doFacts);
		config.setDoOccurrence(doOccurences);
		config.setDoCommonNames(doCommonNames);

		config.setDoMarker(doMarker);
		config.setDoUser(doUser);

		config.setTaxonNoteAsFeature(true);

		config.setDbSchemaValidation(hbm2dll);

		config.setCheck(check);
		config.setEditor(editor);
		config.setIgnore0AuthorTeam(isIgnore0AuthorTeam);
		config.setUseClassification(useClassification);

		config.setNamerelationshipTypeMethod(getHandleNameRelationshipTypeMethod());
		config.setUserTransformationMethod(getTransformUsernameMethod());
		config.setSalvador(isSalvador);
		config.setFactFilter(factFilter);

		config.setFeatureTreeUuid(featureTreeUuid);
		config.setFeatureTreeTitle("Salvador Portal Feature Tree");

		// invoke import
		CdmDefaultImport<BerlinModelImportConfigurator> bmImport = new CdmDefaultImport<>();
		ImportResult result = bmImport.invoke(config);

		addUsers(config, bmImport);

		System.out.println("End import from BerlinModel ("+ source.getDatabase() + ")...");
		return result;
	}

    private void addUsers(BerlinModelImportConfigurator config, CdmDefaultImport<BerlinModelImportConfigurator> bmImport) {
        if (config.isDoUser()){
            ICdmApplication app = bmImport.getCdmAppController();
            TransactionStatus tx = app.startTransaction(false);

            //admin
            Group adminGroup = Group.NewInstance("Admins");
            GrantedAuthorityImpl roleAdmin = app.getGrantedAuthorityService().findAuthorityString(Role.ROLE_ADMIN.getAuthority());
            adminGroup.addGrantedAuthority(roleAdmin);

//            UserDetails wgbDetails = app.getUserService().loadUserByUsername("w.berendsohn");
            List<User> users = app.getUserService().listByUsername("w.berendsohn", null, null, null, null, null, null);
            for (User user: users){
                adminGroup.addMember(user);
            }
            users = app.getUserService().listByUsername("admin", null, null, null, null, null, null);
            for (User user: users){
                adminGroup.addMember(user);
            }
            app.getGroupService().saveOrUpdate(adminGroup);

            //gruber
            List<Group> editorGroups = app.getGroupService().listByName("Editor", null, null, null, null, null, null);
            for (Group editorGroup: editorGroups){
                users = app.getUserService().listByUsername("k.gruber", null, null, null, null, null, null);
                for (User user: users){
                    editorGroup.addMember(user);
                }
            }
            app.commitTransaction(tx);
        }

    }

	public static void main(String[] args) {
		SalvadorActivator activator = new SalvadorActivator();
		ICdmDataSource destination = CdmDestinations.chooseDestination(args) != null ? CdmDestinations.chooseDestination(args) : cdmDestination;

		activator.doImport(destination);
		if (doExport == true){
			SalvadorExport export = new SalvadorExport();
			export.doExport(destination);
		}
		System.exit(0);
	}


	private Method getHandleNameRelationshipTypeMethod(){
		String methodName = "handleNameRelationshipType";
		try {
			Method method = this.getClass().getDeclaredMethod(methodName, Integer.class, INonViralName.class, INonViralName.class);
			method.setAccessible(true);
			return method;
		} catch (Exception e) {
			logger.error("Problem creating Method: " + methodName);
			return null;
		}
	}


	//used by BerlinModelImportConfigurator
	@SuppressWarnings("unused")
	private static boolean handleNameRelationshipType(Integer relQualifierFk, INonViralName nameTo, INonViralName nameFrom){
		if (relQualifierFk == 72){
			nameTo.getHomotypicalGroup().merge(nameFrom.getHomotypicalGroup());
			return true;
		}
		return false;
	}

	private Method getTransformUsernameMethod(){
		String methodName = "transformUsername";
		try {
			Method method = this.getClass().getDeclaredMethod(methodName, String.class);
			method.setAccessible(true);
			return method;
		} catch (Exception e) {
			logger.error("Problem creating Method: " + methodName);
			return null;
		}
	}

	//used by BerlinModelImportConfigurator
	@SuppressWarnings("unused")
	private static String transformUsername(String nameToBeTransformed){
		if (nameToBeTransformed == null){
			return null;
		}else if ("W.G.Berendsohn".equals(nameToBeTransformed)){
			return "wgb";
		}else if(nameToBeTransformed.startsWith("fs") || nameToBeTransformed.equals("BGBM\\fs")){
			return "Frank Specht";
		}
		return nameToBeTransformed;
	}

}
