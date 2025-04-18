/**
* Copyright (C) 2008 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*/
package eu.etaxonomy.cdm.app.common.tasks;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.api.application.CdmApplicationController;
import eu.etaxonomy.cdm.config.AccountStore;
import eu.etaxonomy.cdm.database.CdmDataSource;
import eu.etaxonomy.cdm.database.DbSchemaValidation;
import eu.etaxonomy.cdm.database.ICdmDataSource;
import eu.etaxonomy.cdm.io.api.application.CdmIoApplicationController;
import eu.etaxonomy.cdm.io.jaxb.DataSet;
import eu.etaxonomy.cdm.model.agent.AgentBase;
import eu.etaxonomy.cdm.model.agent.Institution;
import eu.etaxonomy.cdm.model.agent.InstitutionalMembership;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.common.AnnotatableEntity;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.model.common.VersionableEntity;
import eu.etaxonomy.cdm.model.description.Feature;
import eu.etaxonomy.cdm.model.description.IndividualsAssociation;
import eu.etaxonomy.cdm.model.description.QuantitativeData;
import eu.etaxonomy.cdm.model.description.StatisticalMeasure;
import eu.etaxonomy.cdm.model.description.StatisticalMeasurementValue;
import eu.etaxonomy.cdm.model.description.TaxonDescription;
import eu.etaxonomy.cdm.model.description.TaxonInteraction;
import eu.etaxonomy.cdm.model.description.TaxonNameDescription;
import eu.etaxonomy.cdm.model.location.Country;
import eu.etaxonomy.cdm.model.location.NamedAreaType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.occurrence.DerivedUnit;
import eu.etaxonomy.cdm.model.occurrence.SpecimenOrObservationBase;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.model.taxon.TaxonNode;
import eu.etaxonomy.cdm.model.term.DefinedTermBase;

/**
 * @author a.babadshanjan
 * @since 28.10.2008
 */
public class TestDatabase {

    private static final Logger logger = LogManager.getLogger();

    private static final String server = "192.168.2.10";
	private static final String username = "edit";

	public static ICdmDataSource CDM_DB(String dbname) {

		logger.info("Setting DB " + dbname);
		String password = AccountStore.readOrStorePassword(dbname, server, username, null);
		ICdmDataSource datasource = CdmDataSource.NewMySqlInstance(server, dbname, username, password);
//		ICdmDataSource datasource = CdmDestinations.cdm_production_flora_cuba();
		return datasource;
	}

    public static CdmApplicationController initDb(ICdmDataSource db, DbSchemaValidation dbSchemaValidation, boolean omitTermLoading) {

		logger.info("Initializing database '" + db.getName() + "'");

		CdmApplicationController appCtrInit = CdmIoApplicationController.NewInstance(db, dbSchemaValidation, omitTermLoading);

		return appCtrInit;
    }

	public static void loadTestData(String dbname, CdmApplicationController appCtr) {

		logger.info("Loading test data into " + dbname);

		TransactionStatus txStatus = appCtr.startTransaction();
		DataSet dataSet = buildDataSet();

		appCtr.getTermService().save(dataSet.getTerms());
		appCtr.getTaxonService().save(dataSet.getTaxonBases());

		appCtr.commitTransaction(txStatus);
		appCtr.close();
    }

	/**
	 * This method constructs a small sample classification to test JAXB marshaling.
	 * The sample tree contains four taxa. The root taxon has two children taxa, and
	 * there is one "free" taxon without a parent and children.
	 */
	private static DataSet buildDataSet() {

		List<AgentBase> agents = new ArrayList<>();
	    List<VersionableEntity> agentData = new ArrayList<>();
	    //List<Agent> agentData = new ArrayList<>();

	    //List<TermBase> terms = new ArrayList<>();
	    List<DefinedTermBase> terms = new ArrayList<>();
	    List<Reference> references = new ArrayList<Reference>();
	    List<TaxonName> taxonomicNames = new ArrayList<>();
	    List<TaxonBase> taxonBases = new ArrayList<>();

	    List<Feature> features = new ArrayList<Feature>();

	    Feature feature1 = Feature.BIOLOGY_ECOLOGY();

	    TaxonNameDescription taxNameDescription = TaxonNameDescription.NewInstance();
//	    taxNameDescription.addFeature(feature1);    //no longer supported since v3.3
	    QuantitativeData element = QuantitativeData.NewInstance();
	    StatisticalMeasurementValue statisticalValue = StatisticalMeasurementValue.NewInstance();
	    statisticalValue.setType(StatisticalMeasure.MAX());
	    statisticalValue.setValue(new BigDecimal("2.1"));
	    element.addStatisticalValue(statisticalValue);
	    taxNameDescription.addElement(element);

	    SpecimenOrObservationBase<?> specimen = DerivedUnit.NewPreservedSpecimenInstance();

	    specimen.setIndividualCount("12");


	    Feature featureIndAss = Feature.INDIVIDUALS_ASSOCIATION();
	    TaxonNameDescription newTaxNameDesc = TaxonNameDescription.NewInstance();
//	    newTaxNameDesc.addFeature(featureIndAss);   //no longer supported since v3.3
	    IndividualsAssociation indAss = IndividualsAssociation.NewInstance();
	    indAss.setAssociatedSpecimenOrObservation(specimen);

	    newTaxNameDesc.addElement(indAss);




//	    List<Synonym> synonyms = new ArrayList<Synonym>();
	    List<AnnotatableEntity> homotypicalGroups;

		Reference citRef, sec;
		TaxonName name1, name2, name21, nameRoot1, nameFree, synName11, synName12, synName2, synNameFree;
		TaxonName nameRoot2, nameR2_1, nameR2_2;
		Taxon child1, child2, child21, root1T, root2T, freeT;
		Taxon childR2_1, childR2_2;
		TaxonNode child1Node, child2Node, child21Node, root1TNode, root2TNode, freeTNode;
		TaxonNode childR2_1Node, childR2_2Node;
		Classification taxTree, taxTree2;
		Synonym syn11, syn12, syn2, synFree;
		Rank rankSpecies, rankSubspecies, rankGenus;

		// agents
		// - persons, institutions

		Person linne = new Person("Carl", "Linne", "L.");
		linne.setTitleCache("Linne & Karl", true);
		GregorianCalendar birth = new GregorianCalendar(1707, 4, 23);
		GregorianCalendar death = new GregorianCalendar(1778, 0, 10);
		TimePeriod period = TimePeriod.NewInstance(birth, death);
		linne.setLifespan(period);

//		Keyword keyword = Keyword.NewInstance("plantarum", "lat", "");
//		linne.addKeyword(keyword);

		Institution institute = Institution.NewInstance();

		agents.add(linne);
		agents.add(institute);

		// agent data
		// - contacts, addresses, memberships

		//Contact contact1 = new Contact();
		//contact1.setEmail("someone@somewhere.org");
		InstitutionalMembership membership
		= new InstitutionalMembership(institute, linne, period, "Biodiversity", "Head");
		//agentData.add(contact1);

		agentData.add(membership);

		// terms
		// - ranks, keywords

		rankSpecies = Rank.SPECIES();
		rankSubspecies = Rank.SUBSPECIES();
		rankGenus = Rank.GENUS();

//		terms.add(keyword);

        // taxonomic names

		nameRoot1 = TaxonNameFactory.NewBotanicalInstance(rankGenus,"Calendula",null,null,null,linne,null,"p.100", null);
		nameRoot1.addDescription(taxNameDescription);
		nameRoot1.addDescription(newTaxNameDesc);
		name1 = TaxonNameFactory.NewBotanicalInstance(rankSpecies,"Calendula",null,"arvensis",null,linne,null,"p.1", null);
		synName11 = TaxonNameFactory.NewBotanicalInstance(rankSpecies,"Caltha",null,"arvensis",null,linne,null,"p.11", null);
		synName12 = TaxonNameFactory.NewBotanicalInstance(rankSpecies,"Calendula",null,"sancta",null,linne,null,"p.12", null);

		name2 = TaxonNameFactory.NewBotanicalInstance(rankSpecies,"Calendula",null,"lanzae",null,linne,null,"p.2", null);
		synName2 = TaxonNameFactory.NewBotanicalInstance(rankSpecies,"Calendula",null,"echinata",null,linne,null,"p.2", null);

		name21 = TaxonNameFactory.NewBotanicalInstance(rankSubspecies,"Calendula",null,"lanzea","something",linne,null,"p.1", null);
		//name211 = TaxonNameFactory.NewBotanicalInstance(rankSpecies,"Calendula",null,"lanzea",null,linne,null,"p.1", null);
		//name212 = TaxonNameFactory.NewBotanicalInstance(rankSpecies,"Calendula",null,"lanzea",null,linne,null,"p.1", null);

		nameRoot2 =
			TaxonNameFactory.NewBotanicalInstance(rankGenus,"Sonchus",null,null,null,linne,null,"p.200", null);
		nameR2_1 = TaxonNameFactory.NewBotanicalInstance(rankSpecies,"Sonchus",null,"child1",null,linne,null,"p.1", null);
		nameR2_2 = TaxonNameFactory.NewBotanicalInstance(rankSpecies,"Sonchus",null,"child2",null,linne,null,"p.2", null);

		nameFree = TaxonNameFactory.NewBotanicalInstance(rankSpecies,"Cichorium",null,"intybus",null,linne,null,"p.200", null);
		synNameFree = TaxonNameFactory.NewBotanicalInstance(rankSpecies,"Cichorium",null,"balearicum",null,linne,null,"p.2", null);

		taxonomicNames.add(nameRoot1);
		taxonomicNames.add(name1);
		taxonomicNames.add(synName11);
		taxonomicNames.add(synName12);
		taxonomicNames.add(name2);
		taxonomicNames.add(name21);
		taxonomicNames.add(synName2);
		taxonomicNames.add(nameFree);
		taxonomicNames.add(synNameFree);
		taxonomicNames.add(nameRoot2);

        // references
		sec = ReferenceFactory.newBook();
		sec.setAuthorship(linne);
		sec.setTitleCache("Plant Specification & Taxonomy", true);
		references.add(sec);

		citRef = ReferenceFactory.newDatabase();
		citRef.setAuthorship(linne);
		citRef.setTitleCache("BioCASE", true);
		references.add(citRef);

		// taxa

		root1T = Taxon.NewInstance(nameRoot1, sec);
		root2T = Taxon.NewInstance(nameRoot2, sec);
		freeT = Taxon.NewInstance(nameFree, sec);
		child1 = Taxon.NewInstance(name1, sec);
		child2 = Taxon.NewInstance(name2, sec);
		child21 = Taxon.NewInstance(name21, sec);
		childR2_1 = Taxon.NewInstance(nameR2_1, sec);
		childR2_2 = Taxon.NewInstance(nameR2_2, sec);

		//TaxonInteractions

		TaxonInteraction descBase = TaxonInteraction.NewInstance();
		descBase.setTaxon2(root1T);
		Feature hostplant = Feature.HOSTPLANT();

		descBase.setFeature(hostplant);
		TaxonDescription taxDesc = TaxonDescription.NewInstance();
		taxDesc.addElement(descBase);
		root2T.addDescription(taxDesc);

		//locations

		taxDesc = TaxonDescription.NewInstance();
		Feature locationFeature = Feature.DISTRIBUTION();


		//locationFeature.
		Country area = Country.NewInstance("", "locationTest", null);
		area.setType(NamedAreaType.NATURAL_AREA());

		//Country woC= Country.NewInstance();
		area.addCountry(Country.AFGHANISTAN());
		taxDesc.addGeoScope(area);
//		taxDesc.addFeature(locationFeature);   //no longer supported since v3.3
		root1T.addDescription(taxDesc);


		// synonyms

		synFree = Synonym.NewInstance(synNameFree, sec);
		syn11 = Synonym.NewInstance(synName11, sec);
		syn12 = Synonym.NewInstance(synName12, sec);
		syn2 = Synonym.NewInstance(synName2, sec);

		child1.addSynonym(syn11, SynonymType.HOMOTYPIC_SYNONYM_OF);
		child1.addSynonym(syn12, SynonymType.HETEROTYPIC_SYNONYM_OF);
		child2.addSynonym(syn2, SynonymType.HETEROTYPIC_SYNONYM_OF);
		freeT.addSynonym(synFree, SynonymType.HETEROTYPIC_SYNONYM_OF);

		taxonBases.add(synFree);
		taxonBases.add(syn11);
		taxonBases.add(syn12);
		taxonBases.add(syn2);

		// taxonomic children

		//TODO: Adapt to classification
		taxTree = Classification.NewInstance("TestTree");

		root1TNode = taxTree.addChildTaxon(root1T, sec, null);
		child1Node = root1TNode.addChildTaxon(child1, null, null);
		child2Node = root1TNode.addChildTaxon(child2, null, null);
		child21Node = child2Node.addChildTaxon(child21, null, null);

		taxTree2 = Classification.NewInstance("TestTree2");

		root2TNode = taxTree2.addChildTaxon(root2T, sec, null);
		root2TNode.addChildTaxon(child1, sec, "p.1010").setSynonymToBeUsed(syn11);
		root2TNode.addChildTaxon(child2, null, null);

		/*
		root1T.addTaxonomicChild(child1, sec, "p.1010");
		root1T.addTaxonomicChild(child2, sec, "p.1020");
		child2.addTaxonomicChild(child21, sec, "p.2000");

		root2T.addTaxonomicChild(child1, sec, "p.1010");
		root2T.addTaxonomicChild(child2, sec, "p.1020");
		*/
		//

		taxonBases.add(root1T);
		taxonBases.add(root2T);
		taxonBases.add(freeT);
		taxonBases.add(child1);
		taxonBases.add(child2);
		taxonBases.add(child21);
		taxonBases.add(childR2_1);
		taxonBases.add(childR2_2);

		DataSet dataSet = new DataSet();

		logger.warn("WARNING: TestDatabase has been commented in parts. Mainly, must be adapted to classification.");

		dataSet.setTerms(terms);
		dataSet.setAgents(agents);
//		dataSet.setAgentData(agentData); //TODO
		dataSet.setReferences(references);
		dataSet.setTaxonomicNames(taxonomicNames);
		dataSet.setTaxonBases((List)taxonBases);

		return dataSet;
	}

	public static void main(String[] args) {
	    DbSchemaValidation schemaValidation = DbSchemaValidation.VALIDATE;
	    initDb(CDM_DB(null), schemaValidation, false);
	    System.exit(0);
	}
}