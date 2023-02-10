/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.app.viennaImport;

import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.app.common.CdmDestinations;
import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.common.URI;
import eu.etaxonomy.cdm.config.AccountStore;
import eu.etaxonomy.cdm.facade.DerivedUnitFacade;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.IdentifiableSource;
import eu.etaxonomy.cdm.model.media.ImageFile;
import eu.etaxonomy.cdm.model.media.Media;
import eu.etaxonomy.cdm.model.name.INonViralName;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.DeterminationEvent;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;

/**
 * @author AM
 * @since 08.05.2008
 */
public class ViennaActivator {

    private static final Logger logger = LogManager.getLogger();

	static final Source berlinModelSource = ViennaActivator.VIENNA();


	public static Source VIENNA(){
		//	Vienna Asteraceae
		String dbms = "ODBC";
		String strServer = "AsteraceaeViennaAccess";
		String strDB = "AsteraceaeViennaAccess";
		String userName = "webUser";
		return  makeSource(dbms, strServer, strDB, -1, userName, null);
	}

	public boolean invoke(){
		boolean result = true;
		boolean withCdm = false;
		berlinModelSource.setQuery("SELECT * FROM vienna"); // WHERE ID1 <> 1
		CdmApplicationController app = null;


		try {
			if (withCdm){
				app = CdmApplicationController.NewInstance(CdmDestinations.localH2());
			}else{
				//app = CdmApplicationController.NewInstance(DbSchemaValidation.VALIDATE);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
			result = false;
			return result;
		}


		ResultSet rs = berlinModelSource.getResultSet();
		try {
			while (rs.next()){
				String uriPath = "http://131.130.131.9/database/img/imgBrowser.php?ID=";
				int id = rs.getInt("ID");
				String strId = String.valueOf(id);
				String catalogNumber = rs.getString("catalogueNumber");
				String strTaxonName = rs.getString("TaxonName");
				String annotation = rs.getString("Annotation");
				String typeInformation = rs.getString("TypeInformation");
				String typifiedBy = rs.getString("TypifiedBy");
				String family = rs.getString("Family");
				String strActor = rs.getString("Actor");
				String timePeriod = rs.getString("TimePeriod");
				String collectingArea = rs.getString("CollectingArea");
				String locality = rs.getString("Locality");
				String assigned = rs.getString("assigned");
				String history = rs.getString("history");

				if (! family.equals("Asteraceae")){
					logger.warn("Family not Asteracea: ID= " + strId);
				}
				Reference sec = ReferenceFactory.newDatabase();
				sec.setTitleCache("Vienna Asteraceae Images", true);

				INonViralName taxonName = NonViralNameParserImpl.NewInstance().parseFullName(strTaxonName);
				if (withCdm){
					List<TaxonName> names = app.getNameService().findByName(null, strTaxonName, null, null, null, null, null, null).getRecords();
					if (names.size() == 0){
						logger.warn("Name not found: " + strTaxonName);
					}else{
						if (names.size() > 1){
							logger.warn("More then 1 name found: " + strTaxonName);
						}
						taxonName = names.get(0);
					}
				}
				Taxon taxon = Taxon.NewInstance(taxonName, sec);

				logger.info("Create new specimen ...");
				DerivedUnit specimenx = DerivedUnit.NewPreservedSpecimenInstance();
				DerivedUnitFacade specimen = DerivedUnitFacade.NewInstance(SpecimenOrObservationType.PreservedSpecimen);

				specimen.setCatalogNumber(catalogNumber);
				specimen.setStoredUnder(TaxonName.castAndDeproxy(taxonName));   //??
				//TODO
				//specimen.setCollection(collection);
				specimen.addAnnotation(Annotation.NewDefaultLanguageInstance(annotation));
				specimen.addDetermination(getDetermination(taxon, strActor));
				specimen.addDerivedUnitMedia(getMedia(uriPath, strId));

				//Original ID
				specimen.addSource(IdentifiableSource.NewDataImportInstance(strId));


			}
		} catch (SQLException e) {
			logger.warn("Error when reading record!!");
			e.printStackTrace();
			result = false;
		}
		return result;
	}

	private Media getMedia(String uriPath, String id){
		//"http://131.130.131.9/database/img/imgBrowser.php?ID=50599";
		String uriString = uriPath + id;
		if (CdmUtils.urlExists(uriString, false)){
			String suffix = "jpg";
			String mimeType = "image/jpg";
			URI uri = null;
			try {
				uri = new URI(uriString);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			Media media = ImageFile.NewMediaInstance(null, null, uri, mimeType, suffix,  null, null, null);
			return media;
		}else{
			logger.warn("URI does not exist: " + uriString);
			return null;
		}
	}

	private DeterminationEvent getDetermination(Taxon taxon, String actor){
		logger.info("Create determination event");
		DeterminationEvent determinationEvent = DeterminationEvent.NewInstance();
		determinationEvent.setTaxon(taxon);
		Person person = Person.NewTitledInstance(actor);
		determinationEvent.setActor(person);
		return determinationEvent;
	}



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ViennaActivator viennaAct = new ViennaActivator();
		viennaAct.invoke();
	}


	/**
	 * Initialises source
	 * @return true, if connection established
	 */
	private static Source makeSource(String dbms, String strServer, String strDB, int port, String userName, String pwd ){
		//establish connection
		pwd = AccountStore.readOrStorePassword(dbms, strServer, userName, pwd);
		Source source = new Source(dbms, strServer, strDB);
		source.setPort(port);
		source.setUserAndPwd(userName, pwd);
		return source;
	}
}
