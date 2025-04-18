/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.caryo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.common.DbImportBase;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.Annotation;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Language;
import eu.etaxonomy.cdm.model.common.VerbatimTimePeriod;
import eu.etaxonomy.cdm.model.name.IBotanicalName;
import eu.etaxonomy.cdm.model.name.NameTypeDesignation;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatus;
import eu.etaxonomy.cdm.model.name.NomenclaturalStatusType;
import eu.etaxonomy.cdm.model.name.Rank;
import eu.etaxonomy.cdm.model.name.TaxonName;
import eu.etaxonomy.cdm.model.name.TaxonNameFactory;
import eu.etaxonomy.cdm.model.reference.IArticle;
import eu.etaxonomy.cdm.model.reference.IBook;
import eu.etaxonomy.cdm.model.reference.IBookSection;
import eu.etaxonomy.cdm.model.reference.IJournal;
import eu.etaxonomy.cdm.model.reference.INomenclaturalReference;
import eu.etaxonomy.cdm.model.reference.OriginalSourceType;
import eu.etaxonomy.cdm.model.reference.Reference;
import eu.etaxonomy.cdm.model.reference.ReferenceFactory;
import eu.etaxonomy.cdm.model.taxon.Classification;
import eu.etaxonomy.cdm.model.taxon.Synonym;
import eu.etaxonomy.cdm.model.taxon.SynonymType;
import eu.etaxonomy.cdm.model.taxon.Taxon;
import eu.etaxonomy.cdm.model.taxon.TaxonBase;
import eu.etaxonomy.cdm.strategy.exceptions.UnknownCdmTypeException;

/**
 * @author a.mueller
 * @since 20.02.2010
 */
@Component
public class CaryoTaxonImport  extends DbImportBase<CaryoImportState, CaryoImportConfigurator> {

    private static final long serialVersionUID = 5369739490456155285L;
    private static final Logger logger = LogManager.getLogger();

	private final int modCount = 10000;
	private static final String pluralString = "taxa";
	private static final String dbTableName = "CARYOPHYLLALES";

	private final Map<String, Taxon> familyMap = new HashMap<>();
	private final Map<String, Person> personMap = new HashMap<>();
	private final Map<String, Team> teamMap = new HashMap<>();
	private final Map<String, TeamOrPersonBase> inAuthorMap = new HashMap<>();
	private final Map<String, IJournal> journalMap = new HashMap<>();
	private final Map<String, IBook> bookMap = new HashMap<>();

	private Classification classification;

	public CaryoTaxonImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(CaryoImportState state) {
		String strRecordQuery =
			" SELECT ID " +
			" FROM " + dbTableName +
			" ORDER BY id ";
		return strRecordQuery;
	}

	@Override
	protected String getRecordQuery(CaryoImportConfigurator config) {
		String strRecordQuery =
			" SELECT t.* " +
			" FROM " + getTableName() + " t " +
			" WHERE ( t.ID IN (" + ID_LIST_TOKEN + ") )";
		return strRecordQuery;
	}

	@Override
	public boolean doPartition(ResultSetPartitioner partitioner, CaryoImportState state) {
		boolean success = true;

		Set<TaxonBase> objectsToSave = new HashSet<>();

		classification = getClassification(state);

		try {
			doFamilies(state);
			doAuthors(state);
			doInAuthors(state);
			doJournals(state);
			doBooks(state);

			ResultSet rs = partitioner.getResultSet();

			int i = 0;
    		Reference sec = state.getTransactionalSourceReference();

			//for each reference
            while (rs.next()){

        		if ((i++ % modCount) == 0 && i!= 1 ){ logger.info(pluralString + " handled: " + (i-1));}

        		Integer id = rs.getInt("Id");
        		Integer taxonId = rs.getInt("NCUGenID");
        		String genus = rs.getString("Genus");
        		String family = rs.getString("Family");
				String pages = rs.getString("Pages");
				String autoren = rs.getString("Autoren");
				String typeStr = rs.getString("Type");
				String nomStatusStr = rs.getString("NomenclaturalStatus");
				String basioStr = rs.getString("Basionym");

//        	      ,[EtInCitation]
//        	      ,[Gender]

//        	      ,[Basionym]
//        	      ,[OriginalCitation]

				IBotanicalName name = TaxonNameFactory.NewBotanicalInstance(Rank.GENUS());
				name.setGenusOrUninomial(genus);
				makeAuthors(name, autoren, id);
				INomenclaturalReference nomRef = makeNomRef(state, rs, id);
        		name.setNomenclaturalReference(nomRef);
				name.setNomenclaturalMicroReference(pages);
				makeStatus(name, nomStatusStr, id);

				Taxon taxon = Taxon.NewInstance(name, state.getTransactionalSourceReference());
				handleTypes(state, rs, taxon, typeStr, id);
				handleBasionym(state, rs, taxon, basioStr, id);

				Taxon parent = familyMap.get(family);

				classification.addParentChild(parent, taxon, sec, null);

				taxon.addSource(OriginalSourceType.Import, String.valueOf(taxonId), "NCUGenID", sec, null);

				objectsToSave.add(taxon);

            }

			logger.warn(pluralString + " to save: " + objectsToSave.size());
			getTaxonService().save(objectsToSave);

			return success;
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}
	}

	private void handleBasionym(CaryoImportState state, ResultSet rs, Taxon taxon, String basioStr, Integer id) {
		if (StringUtils.isNotBlank(basioStr)){
			IBotanicalName name = taxon.getName();
			TaxonName basionym = (TaxonName)TaxonNameFactory.PARSED_BOTANICAL_REFERENCE(basioStr);
			if (basionym.hasProblem()){
				logger.warn("Problem when parsing basionym ("+id+"): " +  basioStr);
			}
			name.addBasionym(basionym);
			Synonym syn = Synonym.NewInstance(basionym, state.getTransactionalSourceReference());
			taxon.addSynonym(syn, SynonymType.HOMOTYPIC_SYNONYM_OF);
			getTaxonService().save(syn);
		}
	}

	private void handleTypes(CaryoImportState state, ResultSet rs, Taxon taxon, String origType, Integer id) {
		NameTypeDesignation desig = NameTypeDesignation.NewInstance();
		String type = origType;
		if (StringUtils.isBlank(type) || "to be designated".equalsIgnoreCase(type)){
			return;
		}else{
			IBotanicalName name = taxon.getName();
			TaxonName typeName = TaxonNameFactory.NewBotanicalInstance(Rank.SPECIES());
			if ("not designated".equalsIgnoreCase(type)){
				desig.setNotDesignated(true);
			}else{
				String genus = name.getGenusOrUninomial();
				typeName.setGenusOrUninomial(genus);
				if (! type.startsWith(genus.substring(0,1) + ". " )){
					int i = type.indexOf(" ");
					String genusOrig = type.substring(0, i);
					logger.info("First genus letter not recognized: " + genusOrig + "-" + genus + ":"+  id);
					typeName.setGenusOrUninomial(genusOrig);
					type = type.substring(i + 1).trim();
				}else{
					type = type.substring(3);
				}
				int i = type.indexOf(" ");
				if (i <= 0){
					logger.warn("No space: " + type +"; " + id);
				}else{
					String species = type.substring(0, i);
					typeName.setSpecificEpithet(species);
					type = type.substring(i + 1).trim();

					int posBracket = type.indexOf("(", 2);
					if (posBracket > 0){
						String bracket = type.substring(posBracket);
//						logger.warn("Type has bracket("+id+"): " + bracket);
						taxon.addAnnotation(Annotation.NewInstance("Type-bracket: " + bracket, Language.DEFAULT()));
						type = type.substring(0, posBracket).trim();
					}else{
						Taxon speciesTaxon = Taxon.NewInstance(typeName, state.getTransactionalSourceReference());
						classification.addParentChild(taxon, speciesTaxon, null, null);
					}
					type = makeTypeNomStatus(typeName, type);


					makeAuthors(typeName, type, id);
				}

				desig.setTypeName(typeName);
			}
			name.addTypeDesignation(desig, true);
		}
	}

	private String makeTypeNomStatus(IBotanicalName typeName, String type) {
		if (type.endsWith(", nom. illeg.")){
			type = type.replaceAll(", nom. illeg.", "");
			typeName.addStatus(NomenclaturalStatus.NewInstance(NomenclaturalStatusType.ILLEGITIMATE()));
		}
		return type;
	}

	private void makeStatus(IBotanicalName name, String nomStatusStr, Integer id) throws SQLException {

		if (StringUtils.isNotBlank(nomStatusStr)){
			NomenclaturalStatusType nomStatusType;
			try {
				nomStatusType = NomenclaturalStatusType.getNomenclaturalStatusTypeByAbbreviation(nomStatusStr, name);
			} catch (UnknownCdmTypeException e) {
				if (nomStatusStr.startsWith("nom. rej. prop.")){
					nomStatusType = NomenclaturalStatusType.REJECTED_PROP();
					logger.info("in favour not supported ("+id+"): " + nomStatusStr);
				}else if (nomStatusStr.startsWith("nom. rej. in favour")){
					nomStatusType = NomenclaturalStatusType.REJECTED();
					logger.info("in favour not supported ("+id+"): " + nomStatusStr);
				}else if (nomStatusStr.startsWith("nom. cons. against")){
					nomStatusType = NomenclaturalStatusType.CONSERVED();
					logger.info("against not supported ("+id+"): " + nomStatusStr);
				}else if (nomStatusStr.startsWith("nom. cons. prop. against")){
					nomStatusType = NomenclaturalStatusType.CONSERVED_PROP();
					logger.info("against not supported ("+id+"): " + nomStatusStr);
				}else{
					logger.warn("Unknown status type ("+id+"): " + nomStatusStr);
					nomStatusType = NomenclaturalStatusType.DOUBTFUL();
				}
			}

			NomenclaturalStatus status = NomenclaturalStatus.NewInstance(nomStatusType);
			name.addStatus(status);
		}
	}

	private INomenclaturalReference makeNomRef(CaryoImportState state, ResultSet rs, Integer id) throws SQLException {
		INomenclaturalReference result;
		String periodicalTitle = rs.getString("PeriodicalTitle");
		String volume = rs.getString("PeriodicalVolume");
		String bookTitle = rs.getString("BookTitle");
		String inAutorStr = rs.getString("InAutor");
		String autorenStr = rs.getString("Autoren");

		TeamOrPersonBase<?> author = getNomRefAuthor(autorenStr, id);
		if (StringUtils.isNotBlank(periodicalTitle)){
			IJournal journal = journalMap.get(periodicalTitle);
			if (journal == null){
				logger.warn("Journal not found: " + periodicalTitle + ";" + id);
			}
			IArticle article = ReferenceFactory.newArticle();
			article.setInJournal(journal);
			article.setVolume(volume);
			result = article;
		}else if (StringUtils.isNotBlank(bookTitle)){
			IBook book = bookMap.get(bookTitle);
			if (inAutorStr != null){
				IBookSection section = ReferenceFactory.newBookSection();
				section.setInBook(book);
				TeamOrPersonBase<?> inAuthor = getInAuthor(inAutorStr);
				book.setAuthorship(inAuthor);
				result = section;
			}else{
				result = book;
			}
		}else{
			logger.warn("No nomRef found: " +  id);
			result = null;
		}
		if (result != null){
			result.setAuthorship(author);
			makeDate(state, rs, result, id);
		}
		return result;
	}

	private void makeDate(CaryoImportState state, ResultSet rs, INomenclaturalReference ref, Integer id) throws SQLException {
	    VerbatimTimePeriod tp = VerbatimTimePeriod.NewVerbatimInstance();
		String pre1 = rs.getString("DatePre1");
		String pre2 = rs.getString("DatePre2");
		Float year1 = nullSafeFloat(rs, "DateYear1");
		Float year2 = nullSafeFloat(rs, "DateYear2");
		if (year2 == 0.0 ){
			year2 = null;
		}
		String modi1 = rs.getString("DateModi1");
		String modi2 = rs.getString("DateModi2");
		String date = rs.getString("Date");

		tp.setStartYear(year1.intValue());
		Integer[] preDate1 = getDay(pre1,id);
//		tp.setStartMonth(preDate1[1]);
//		tp.setStartDay(preDate1[0]);
		if (year2 != null){
			tp.setEndYear(year2.intValue());
		}
		Integer[] preDate2 = getDay(pre2, id);
//		tp.setEndMonth(preDate2[1]);
//		tp.setEndDay(preDate2[0]);

//		if (StringUtils.isNotBlank(modi1) || StringUtils.isNotBlank(modi2)){
//			tp.setFreeText(date);
//		}
		ref.setDatePublished(tp);
	}

	private Integer[] getDay(String pre, Integer id) {
		Integer[] result = new Integer[2];
		if (! StringUtils.isBlank(pre)){
			try {
				String[] split = pre.split("\\s");
				String monthStr;
				if (split.length > 2){
					logger.warn("L > 2: " + pre);
					monthStr = "";
				}else if(split.length == 2){
					result[0] = Integer.valueOf(split[0]);
					monthStr = split[1];
				}else{
					monthStr = split[0];
				}
				Integer month;
				if ("Jan".equalsIgnoreCase(monthStr)){
					month = 1;
				}else if ("Feb".equalsIgnoreCase(monthStr)){
					month = 2;
				}else if ("Mar".equalsIgnoreCase(monthStr)){
					month = 3;
				}else if ("Apr".equalsIgnoreCase(monthStr)){
					month = 4;
				}else if ("Mai".equalsIgnoreCase(monthStr)){
					month = 5;
				}else if ("Jun".equalsIgnoreCase(monthStr)){
					month = 6;
				}else if ("Jul".equalsIgnoreCase(monthStr)){
					month = 7;
				}else if ("Aug".equalsIgnoreCase(monthStr)){
					month = 8;
				}else if ("Sep".equalsIgnoreCase(monthStr)){
					month = 9;
				}else if ("Oct".equalsIgnoreCase(monthStr)){
					month = 10;
				}else if ("Nov".equalsIgnoreCase(monthStr)){
					month = 11;
				}else if ("Dec".equalsIgnoreCase(monthStr)){
					month = 12;
				}else{
					logger.warn("Unknown month ("+id+"): " + monthStr );
					month = null;
				}
				result[1]= month;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	private TeamOrPersonBase<?> getInAuthor(String inAutorStr) {
		if (StringUtils.isBlank(inAutorStr)){
			return null;
		}
		TeamOrPersonBase<?> inAuthor = inAuthorMap.get(inAutorStr);
		if (inAuthor == null){
			logger.warn("Inauthor not found: " +  inAutorStr);
		}
		return inAuthor;
	}

	private void makeAuthors(IBotanicalName name, String autoren, Integer id) {
		String[] parsedAuthorTeams = getParsedAuthors(autoren);
		name.setBasionymAuthorship(getTeam(parsedAuthorTeams[0], id));
		name.setExBasionymAuthorship(getTeam(parsedAuthorTeams[1], id));
		name.setCombinationAuthorship(getTeam(parsedAuthorTeams[2], id));
		name.setExCombinationAuthorship(getTeam(parsedAuthorTeams[3], id));
	}

	private TeamOrPersonBase<?> getNomRefAuthor(String authorStr, Integer id) {
		String[] parsedAuthorTeams = getParsedAuthors(authorStr);
		TeamOrPersonBase<?> team = getTeam(parsedAuthorTeams[2], id);
		return team;
	}

	private TeamOrPersonBase<?> getTeam(String author, Integer id) {
		if (StringUtils.isBlank(author)){
			return null;
		}
		TeamOrPersonBase<?> result;
		if (personMap.get(author) != null){
			result = personMap.get(author);
		}else{
			result = teamMap.get(author);
		}
		if (result == null){
			logger.warn("Team not found ("+id+"): " + author);
		}
		return result;
	}

	private void doInAuthors(CaryoImportState state) throws SQLException {
		Source source = state.getConfig().getSource();
		String sql = "SELECT DISTINCT inAutor FROM " + getTableName() + " WHERE inAutor IS NOT NULL AND inAutor <> '' ";
		ResultSet rs = source.getResultSet(sql);
		while (rs.next()){
			String inAutorStr = rs.getString("inAutor");
			if (inAuthorMap.get(inAutorStr) == null){
				Team team = Team.NewTitledInstance(inAutorStr, inAutorStr);

				inAuthorMap.put(inAutorStr, team);
				getAgentService().save(team);
			}
		}
	}

	private void doAuthors(CaryoImportState state) throws SQLException {
		Source source = state.getConfig().getSource();
		String sql = "SELECT DISTINCT Autoren FROM " + getTableName() + " WHERE Autoren IS NOT NULL AND Autoren <> '' ";
		ResultSet rs = source.getResultSet(sql);
		doTypeAuthors(state);
		while (rs.next()){
			String autorenStr = rs.getString("Autoren");
			String[] parsedAuthorTeams = getParsedAuthors(autorenStr);
			for (String teamStr : parsedAuthorTeams){
				doTeam(teamStr);
			}
		}
	}

	private void doTypeAuthors(CaryoImportState state) {
		doTeam("Dinter & Derenb.");
		doTeam("Marloth");
		doTeam("Engl.");
		doTeam("Kensit");
		doTeam("Sond.");
		doTeam("L. f.");
		doTeam("Dinter & A. Berger");
		doTeam("Schltr.");
		doTeam("Dinter & Berger");
		doTeam("Poir.");
		doTeam("J. C. Wendl.");
		doTeam("Baker & Clarke");
		doTeam("Vahl");
		doTeam("Nicolai");
		doTeam("Gürke");
		doTeam("Cels");
		doTeam("Dams");
		doTeam("Coult.");
		doTeam("A. Weber");
		doTeam("Vaupel");
		doTeam("Gay");
		doTeam("Pall.");
		doTeam("Moq. & Coss.");
		doTeam("Durieu & Moq.");
		doTeam("Lag. & Rodrigues");
		doTeam("M. Martens & Galeotti");
		doTeam("Steud.");
		doTeam("Aitch. & Hemsl.");
		doTeam("Ikonn.-Gal.");
		doTeam("Freitag");
		doTeam("Regel");
		doTeam("Ledeb.");
		doTeam("Schur");
		doTeam("Asch.");
		doTeam("G. Forst.");
		doTeam("Gray");
		doTeam("Curran");
		doTeam("Donn. Sm.");
		doTeam("Diels");
		doTeam("Colla");
		doTeam("Miers");
		doTeam("Gillis");
		doTeam("Royle");
		doTeam("Monv.");
		doTeam("Werderm. & Backeb.");
		doTeam("Wright");
		doTeam("Meyen");
		doTeam("Runge");
		doTeam("Böd.");
		doTeam("Rol.-Goss.");
		doTeam("Poselg.");
		doTeam("Andreae & Backeberg");
		doTeam("Miq.");
		doTeam("Rol.");
		doTeam("Backeb. & Voll");
		doTeam("Engelm. & Bigelow");
		doTeam("Pfeiffer & Otto");
		doTeam("Humb. & Bonpl.");
		doTeam("Schmalh.");
		doTeam("Preobr.");
		doTeam("Labill.");
		doTeam("Barkoudah");
		doTeam("Regel & Schmalh.");
		doTeam("Cambess.");
		doTeam("Pax & K. Hoff.");
		doTeam("Bergeret");
		doTeam("Walp.");
		doTeam("Huds.");
		doTeam("Kit.");
		doTeam("Schott, Nymann & Kotschy");
		doTeam("Boiss. & Buhse");
		doTeam("Medik.");
		doTeam("Coss. & Germ.");
		doTeam("Moss");
		doTeam("Pax & Hoffm.");
		doTeam("Schischk.");
		doTeam("Lipsch.");
		doTeam("Maerkl.");
		doTeam("Vierh.");
		doTeam("Exell");
	}

	protected void doTeam(String teamStr) {
		if (StringUtils.isBlank(teamStr)){
			return;
		}
		String[] parsedTeam = parseTeam(teamStr);
		if (parsedTeam.length == 1){
			savePerson(parsedTeam[0]);
		}else{
			Team team = teamMap.get(teamStr);
			if (team == null){
				team = Team.NewInstance();
				for (String member : parsedTeam){
					Person person = savePerson(member);
					team.addTeamMember(person);
				}
				teamMap.put(teamStr, team);
				getAgentService().saveOrUpdate(team);
			}
		}
		return;
	}

	private String[] parseTeam(String teamStr) {
		String[] split = teamStr.split("[&,]");
		for (int i = 0; i < split.length; i++){
			split[i] = split[i].trim();
		}
		return split;
	}

	private Person savePerson(String personStr) {
		Person result = personMap.get(personStr);
		if (result == null ){
			Person person = Person.NewTitledInstance(personStr);
			personMap.put(personStr, person);
			getAgentService().save(person);
			result = person;
		}
		return result;
	}

	private String[] getParsedAuthors(String autorenStr) {
		String[] result = new String[4];
		String basioFull = null;
		String origFull;

			String[]  split = autorenStr.split("\\)");
		if (split.length > 1){
			basioFull = split[0].replace("(", "").trim();
			origFull = split[1].trim();
		}else{
			origFull = split[0].trim();
		}
		String[] splitBasio = splitExAuthors(basioFull);
		String[] splitOrig = splitExAuthors(origFull);
		result[0] = splitBasio[0];
		result[1] = splitBasio[1];
		result[2] = splitOrig[0];
		result[3] = splitOrig[1];

		return result;
	}

	private String[] splitExAuthors(String author) {
		String[] result = new String[2];
		if (author != null){
			String[]  split = author.split("\\sex\\s");
			if (split.length > 1){
				result[0] = split[1].trim();
				result[1] = split[0].trim();
			}else{
				result[0] = split[0].trim();
			}
		}
		return result;
	}

	private void doBooks(CaryoImportState state) throws SQLException {
		Source source = state.getConfig().getSource();
		String sql = "SELECT DISTINCT BookTitle FROM " + getTableName() + " WHERE BookTitle IS NOT NULL AND BookTitle <> '' ";
		ResultSet rs = source.getResultSet(sql);
		while (rs.next()){
			String bookStr = rs.getString("BookTitle");
			if (bookMap.get(bookStr) == null ){

				IBook book = ReferenceFactory.newBook();

				book.setTitle(bookStr);

				bookMap.put(bookStr, book);
				getReferenceService().save((Reference)book);
			}
		}
	}

	private void doJournals(CaryoImportState state) throws SQLException {
		Source source = state.getConfig().getSource();
		String sqlPeriodical = "SELECT DISTINCT PeriodicalTitle FROM " + getTableName() + " WHERE PeriodicalTitle IS NOT NULL AND PeriodicalTitle <> '' ";
		ResultSet rs = source.getResultSet(sqlPeriodical);
		while (rs.next()){
			String periodical = rs.getString("PeriodicalTitle");
			if (journalMap.get(periodical) == null ){

				Reference journal = ReferenceFactory.newJournal();

				journal.setTitle(periodical);

				journalMap.put(periodical, journal);
				getReferenceService().save(journal);
			}
		}
	}

	private void doFamilies(CaryoImportState state) throws SQLException {
		Source source = state.getConfig().getSource();
		String sqlFamily = "SELECT DISTINCT Family FROM " + getTableName() + " WHERE Family IS NOT NULL";
		ResultSet rs = source.getResultSet(sqlFamily);
		while (rs.next()){
			String family = rs.getString("family");
			if (familyMap.get(family) == null ){

				IBotanicalName name = TaxonNameFactory.NewBotanicalInstance(Rank.FAMILY());
				name.setGenusOrUninomial(family);
				Taxon taxon = Taxon.NewInstance(name, state.getTransactionalSourceReference());
				classification.addChildTaxon(taxon, null, null);
	//			taxon.addSource(id, idNamespace, citation, null);

				familyMap.put(family, taxon);
				getTaxonService().save(taxon);
			}
		}
	}

	private Classification getClassification(CaryoImportState state) {
		if (this.classification == null){
			String name = state.getConfig().getClassificationName();
			Reference reference = state.getTransactionalSourceReference();
			this.classification = Classification.NewInstance(name, reference, Language.DEFAULT());
			if (state.getConfig().getClassificationUuid() != null){
				classification.setUuid(state.getConfig().getClassificationUuid());
			}
			getClassificationService().save(classification);
		}
		return this.classification;
	}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, CaryoImportState state) {
//		String nameSpace;
//		Class cdmClass;
//		Set<String> idSet;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
//		try{
//			Set<String> taxonIdSet = new HashSet<>();
//
//			while (rs.next()){
////				handleForeignKey(rs, taxonIdSet, "taxonId");
//			}
//
//			//taxon map
//			nameSpace = TAXON_NAMESPACE;
//			cdmClass = Taxon.class;
//			idSet = taxonIdSet;
//			Map<String, Taxon> objectMap = (Map<String, Taxon>)getCommonService().getSourcedObjectsByIdInSource(cdmClass, idSet, nameSpace);
//			result.put(nameSpace, objectMap);
//
//
//		} catch (SQLException e) {
//			throw new RuntimeException(e);
//		}
		return result;
	}

	@Override
	protected boolean doCheck(CaryoImportState state){
		return true;
	}

	@Override
    protected boolean isIgnore(CaryoImportState state){
		return ! state.getConfig().isDoTaxa();
	}
}
