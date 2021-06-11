/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.berlinModel.in;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.common.CdmUtils;
import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelAuthorTeamImportValidator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.io.common.utils.ImportDeduplicationHelper;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.strategy.cache.agent.INomenclaturalAuthorCacheStrategy;

/**
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelAuthorTeamImport extends BerlinModelImportBase {

    private static final long serialVersionUID = -4318481607033688522L;
    private static final Logger logger = Logger.getLogger(BerlinModelAuthorTeamImport.class);

	public static final String NAMESPACE = "AuthorTeam";
	   public static final String NAMESPACE_SPLIT = "AuthorTeam_Split";

	private static final String pluralString = "AuthorTeams";
	private static final String dbTableName = "AuthorTeam";

	private ResultSet rsSequence;
	private Source source;

    private ImportDeduplicationHelper<BerlinModelImportState> deduplicationHelper;


	public BerlinModelAuthorTeamImport(){
		super(dbTableName, pluralString);
	}


	@Override
    protected void doInvoke(BerlinModelImportState state){
		BerlinModelImportConfigurator config = state.getConfig();
		source = config.getSource();
		this.deduplicationHelper = ImportDeduplicationHelper.NewInstance(this, state);

		logger.info("start make " + pluralString + " ...");

		//queryStrings
		String strIdQuery = getIdQuery(state);

		String strRecordQuery = getRecordQuery(config);
		String strWhere = " WHERE (1=1) ";
		if (state.getConfig().getAuthorTeamFilter() != null){
			strWhere += " AND " + state.getConfig().getAuthorTeamFilter();
			strWhere = strWhere.replaceFirst("authorTeamId", "authorTeamFk");
		}
		String strQuerySequence =
			" SELECT *  " +
            " FROM AuthorTeamSequence " +
                (state.getConfig().isEuroMed() ? "" : strWhere) +
            " ORDER By authorTeamFk, Sequence ";

		int recordsPerTransaction = config.getRecordsPerTransaction();
		try{
			ResultSetPartitioner<BerlinModelImportState> partitioner = ResultSetPartitioner.NewInstance(source, strIdQuery, strRecordQuery, recordsPerTransaction);
			rsSequence = source.getResultSet(strQuerySequence) ; //only here, to reduce deadlock/timeout probability
			while (partitioner.nextPartition()){
				partitioner.doPartition(this, state);
			}
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			state.setUnsuccessfull();
			return;
		}

		logger.info("end make " + pluralString + " ... " + getSuccessString(true));
		this.deduplicationHelper = null;
		return;
	}

	@Override
	protected String getIdQuery(BerlinModelImportState state){
		if (state.getConfig().isEuroMed()){
		    return " SELECT authorTeamId "
		         + " FROM v_cdm_exp_authorTeamsAll ORDER BY authorTeamId "
		         ;
		}

	    String strWhere = " WHERE (1=1) ";
		if (state.getConfig().getAuthorTeamFilter() != null){
			strWhere += " AND " + state.getConfig().getAuthorTeamFilter();
		}
		String idQuery =
				" SELECT authorTeamId " +
                " FROM AuthorTeam " +
                strWhere +
                " ORDER BY authorTeamId ";
		return idQuery;
	}


	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
		String strRecordQuery =
			" SELECT *  " +
            " FROM AuthorTeam " +
            " WHERE authorTeamId IN ( " + ID_LIST_TOKEN + " )" +
            " ORDER By authorTeamId ";
		return strRecordQuery;
	}


	@Override
    public boolean doPartition(@SuppressWarnings("rawtypes") ResultSetPartitioner partitioner, BerlinModelImportState state) {
		boolean success = true ;
		deduplicationHelper.restartSession();
		BerlinModelImportConfigurator config = state.getConfig();
		Set<TeamOrPersonBase<?>> authorsToSave = new HashSet<>();
		@SuppressWarnings("unchecked")
        Map<String, Person> personMap = partitioner.getObjectMap(BerlinModelAuthorImport.NAMESPACE);

		ResultSet rs = partitioner.getResultSet();
		//for each reference
		try{
			while (rs.next()){
				try{
					//if ((i++ % modCount ) == 0 && i!= 1 ){ logger.info(""+pluralString+" handled: " + (i-1));}

					//create Agent element
					int teamId = rs.getInt("AuthorTeamId");
					if (teamId == 0 && config.isIgnore0AuthorTeam()){
						continue;
					}

					Team team = Team.NewInstance();

					boolean preliminaryFlag = rs.getBoolean("PreliminaryFlag");
					String authorTeamCache = rs.getString("AuthorTeamCache");
					String fullAuthorTeamCache = rs.getString("FullAuthorTeamCache");
					if (isBlank(fullAuthorTeamCache)){
//						fullAuthorTeamCache = authorTeamCache;
						if (isBlank(authorTeamCache) && preliminaryFlag){
						    logger.warn("authorTeamCache and fullAuthorTeamCache are blank/null and preliminaryFlag is true. This makes no sense and should not happen: " + teamId);
						}
					}else{
					    fullAuthorTeamCache = fullAuthorTeamCache.trim();
					}
					if (isNotBlank(authorTeamCache)){
					    authorTeamCache = authorTeamCache.trim();
					}
//					team.setTitleCache(fullAuthorTeamCache, preliminaryFlag);
//					team.setNomenclaturalTitle(authorTeamCache, preliminaryFlag);

					success &= makeSequence(state, team, teamId, rsSequence, personMap);

					team.setTitleCache(fullAuthorTeamCache, preliminaryFlag);
					team.setNomenclaturalTitleCache(authorTeamCache, preliminaryFlag);
					//not yet supported by model
//	                team.setOriginalNomenclaturalTitle(authorTeamCache);

					TeamOrPersonBase<?> author = handleTeam(state, team, authorTeamCache,
					        fullAuthorTeamCache, preliminaryFlag, teamId);

					//in case preliminary flag is set incorrectly in BM
					if (author == team && team.getTeamMembers().size() == 0 && preliminaryFlag == false){
                        team.setProtectedTitleCache(true);
                        team.setProtectedNomenclaturalTitleCache(true);
                    }

		            String uuid = null;
		            if (resultSetHasColumn(rs,"UUID")){
		                uuid = rs.getString("UUID");
		                if (uuid != null && !author.isPersited()){
		                    author.setUuid(UUID.fromString(uuid));
		                }
		            }

					//created, notes
//					doIdCreatedUpdatedNotes(state, author, rs, teamId, NAMESPACE);
					doCreatedUpdatedNotes(state, author, rs);
					if (!importSourceExists(author, String.valueOf(teamId), NAMESPACE, state.getTransactionalSourceReference())){
					    doId(state, author, teamId, NAMESPACE);
					}

					authorsToSave.add(author);
				}catch(Exception ex){
					logger.error(ex.getMessage());
					ex.printStackTrace();
					success = false;
				}
			} //while rs.hasNext()
		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			return false;
		}

		//logger.info(i + " " + pluralString + " handled");
		getAgentService().saveOrUpdate((Collection)authorsToSave);

		return success;
	}

    private TeamOrPersonBase<?> handleTeam(BerlinModelImportState state, Team team, String authorTeamCache,
            String fullAuthorTeamCache, boolean preliminaryFlag, int authorTeamId) {

        if (!team.getTeamMembers().isEmpty()){
            return team;
        }

        TeamOrPersonBase<?> result = team;
        if (isBlank(authorTeamCache)){
            logger.warn("Blank authorTeamCache not yet handled: " + authorTeamId);
        }

        //single person
        if (!hasTeamSeparator(authorTeamCache) && !hasTeamSeparator(fullAuthorTeamCache)){
            Person person = makePerson(fullAuthorTeamCache, authorTeamCache, preliminaryFlag, authorTeamId);
            result = deduplicatePerson(state, person);
            if (result != person){
                logger.debug("Single person team deduplicated: " + authorTeamId);
            }else{
                String idInSource = String.valueOf(authorTeamId);
                if (!importSourceExists(person, idInSource, NAMESPACE, state.getTransactionalSourceReference())){
                    person.addImportSource(idInSource, NAMESPACE, state.getTransactionalSourceReference(), null);
                }
            }
        //team
        }else{
            String[] fullTeams = splitTeam(fullAuthorTeamCache);
            String[] nomTeams = splitTeam(authorTeamCache);
            if (fullTeams.length != nomTeams.length && fullTeams.length != 0){
                logger.warn("AuthorTeamCache and fullAuthorTeamCache have not the same team size: " + authorTeamCache + " <-> " + fullAuthorTeamCache+ " ; authorTeamId=" + authorTeamId);
            }else{
                for (int i = 0; i< nomTeams.length ;i++){
                    String fullTeam = fullTeams.length == 0? null: fullTeams[i].trim();
                    Person member = makePerson(fullTeam, nomTeams[i].trim(), preliminaryFlag, authorTeamId);
                    if (member == null){
                        logger.warn("Unexpected short nom. author: " + nomTeams[i].trim() + "; " + authorTeamId);
                        continue;
                    }
                    if (i == nomTeams.length -1 && isEtAl(member)){
                        team.setHasMoreMembers(true);
                    }else{
                        Person dedupMember = deduplicatePerson(state, member);
                        if (dedupMember != member){
                            logger.debug("Member deduplicated: " + authorTeamId);
                        }else{
                            String idInSource = String.valueOf(authorTeamId);
                            if (!importSourceExists(member, idInSource, NAMESPACE_SPLIT, state.getTransactionalSourceReference())){
                                member.addImportSource(idInSource, NAMESPACE_SPLIT, state.getTransactionalSourceReference(), null);
                            }
                        }
                        //TODO add idInBM
                        team.addTeamMember(dedupMember);
                    }
                }
                //check nomenclatural title
                //TODO
                checkTeamNomenclaturalTitle(team, authorTeamCache, authorTeamId);
                //check titleCache
                checkTeamTitleCache(team, fullAuthorTeamCache, authorTeamId);
            }//same size team
            result = deduplicateTeam(state, team);
            if (result != team){
                logger.debug("Dedup team");
            }else{
                String idInSource = String.valueOf(authorTeamId);
                if (!importSourceExists(result, idInSource, NAMESPACE, state.getTransactionalSourceReference())){
                    result.addImportSource(idInSource, NAMESPACE, state.getTransactionalSourceReference(), null);
                }
            }
        }//team

        return result;
    }

    protected void checkTeamNomenclaturalTitle(Team team, String authorTeamCache, int authorTeamId) {
        if (team.getCacheStrategy().getNomenclaturalTitleCache(team).equals(authorTeamCache)){
            team.setProtectedNomenclaturalTitleCache(false);
        }else if(team.getCacheStrategy().getNomenclaturalTitleCache(team).replace(" ,", ",").equals(authorTeamCache)){
            //also accept teams with ' , ' as separator as not protected
            team.setProtectedNomenclaturalTitleCache(false);
        }else{
            team.setNomenclaturalTitleCache(authorTeamCache, true);
            logger.warn("Creation of nomTitle for team with members did not work: " + authorTeamCache + " <-> " + team.getCacheStrategy().getNomenclaturalTitleCache(team)+ " : " + authorTeamId);
        }
    }

    protected void checkTeamTitleCache(Team team, String fullAuthorTeamCache, int authorTeamId) {
        INomenclaturalAuthorCacheStrategy<Team> formatter = team.getCacheStrategy();
        if (team.generateTitle().equals(fullAuthorTeamCache)){
            team.setProtectedTitleCache(false);
        }else if(fullAuthorTeamCache == null){
            team.setProtectedTitleCache(false);
        }else if(team.generateTitle().replace(" & ", ", ").equals(fullAuthorTeamCache.replace(" & ", ", "))){
            //also accept teams with ', ' as final member separator as not protected
            team.setProtectedTitleCache(false);
        }else if(formatter.getFullTitle(team).replace(" & ", ", ").equals(fullAuthorTeamCache.replace(" & ", ", "))){
            //also accept teams with ', ' as final member separator as not protected
            team.setProtectedTitleCache(false);
        }else{
            String fullTitle = formatter.getFullTitle(team).replace(" & ", ", ");
            team.setTitleCache(fullAuthorTeamCache, true);
            logger.warn("Creation of titleCache for team with members did not work: " + fullAuthorTeamCache + " <-> " + team.generateTitle()+ " : " + authorTeamId);
        }
    }

    private Person deduplicatePerson(BerlinModelImportState state, Person person) {
        Person result = deduplicationHelper.getExistingAuthor(state, person);
        return result;
    }

    private Team deduplicateTeam(BerlinModelImportState state, Team team) {
        Team result = deduplicationHelper.getExistingAuthor(state, team);
        return result;
    }

    protected static boolean isEtAl(Person member) {
        if (member != null && isEtAl(member.getTitleCache()) && isEtAl(member.getNomenclaturalTitleCache())){
            return true;
        }
        return false;
    }

    private static boolean isEtAl(String str) {
        if (str == null || !str.equals("al.")){
            return false;
        }else{
            return true;
        }
    }

    private Person makePerson(String full, String nom, boolean preliminaryFlag, int authorTeamId) {
        Person person = Person.NewInstance(nom, null, null, null);
        if (isBlank(full)){
            //do nothing
        }else if (!full.matches(".*[\\s\\.].*")){
            //no whitespace and no . => family name
            person.setFamilyName(full);
        }else if (nom.equals(full)){
            parsePerson(person, full, preliminaryFlag);
        }else{
            parsePerson(person, full, true);
        }
        if (nom.length() <= 2 || (nom.length() == 3 && nom.endsWith(".")) ){
            if (!nom.matches("((L|Sm|DC|al|Sw|Qz|Fr|Ib)\\.|Hu|Ma|Hy|Wu)")){
                logger.warn("Unexpected short nom author name part: " + nom + "; " + authorTeamId);
            }
        }

        return person;
    }

    private void parsePerson(Person person, String str, boolean preliminary) {
        String capWord = "\\p{javaUpperCase}\\p{javaLowerCase}{2,}";
        String famStart = "(Le |D'|'t |Mc|Mac|Des |d'|Du |De )";
        String regEx = "(\\p{javaUpperCase}\\.([\\s-]\\p{javaUpperCase}\\.)*(\\s(de|del|da|von|van|v.|af|zu))?\\s)("
                + famStart + "?" + capWord + "((-| y | é | de | de la )" + capWord + ")?)";
        Matcher matcher = Pattern.compile(regEx).matcher(str);
        if (matcher.matches()){

            person.setProtectedTitleCache(false);
            //Initials + family name
//            String[] splits = str.split("\\s");
//            int n = matcher.groupCount();
//            for (int i = 0; i< n; i++){
//                String s = matcher.group(i);
//                System.out.println(s);
//            }
            person.setFamilyName(matcher.group(5).trim());

//            String initials = splits[0];
//            for (int i = 1; i < splits.length -1; i++ ){
//                initials += " " + splits[i];
//            }
            person.setInitials(matcher.group(1).trim());
        }else{
            person.setTitleCache(str, preliminary);
        }
    }

    private static final String TEAM_SPLITTER = "(,|&)";

    protected static String[] splitTeam(String teamCache) {
        if (teamCache == null){
            return new String[0];
        }
        return teamCache.split(TEAM_SPLITTER);
    }

    protected static boolean hasTeamSeparator(String teamCache) {
        if (isBlank(teamCache)){
            return false;
        }else if (teamCache.contains(",") || teamCache.contains("&")||teamCache.contains(" et ")||teamCache.endsWith(" al.")){
            return true;
        }else{
            return false;
        }
    }

    @Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state)  {

        String nameSpace;
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();

		//person map
		Set<String> idInSourceList = makeAuthorIdList(rs);
		nameSpace = BerlinModelAuthorImport.NAMESPACE;
		Map<String, Person> personMap = getCommonService().getSourcedObjectsByIdInSourceC(Person.class, idInSourceList, nameSpace);
		result.put(nameSpace, personMap);

		return result;
	}

	private Set<String> makeAuthorIdList(ResultSet rs) {
		Set<String> result = new HashSet<>();

		String authorTeamIdList = "";
		try {
			while (rs.next()){
				int id = rs.getInt("AuthorTeamId");
				authorTeamIdList = CdmUtils.concat(",", authorTeamIdList, String.valueOf(id));
			}

			String strQuerySequence =
				" SELECT DISTINCT authorFk " +
	            " FROM AuthorTeamSequence " +
	            " WHERE authorTeamFk IN (@) ";
			strQuerySequence = strQuerySequence.replace("@", authorTeamIdList);

			rs = source.getResultSet(strQuerySequence) ;
			while (rs.next()){
				int authorFk = rs.getInt("authorFk");
				result.add(String.valueOf(authorFk));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	private boolean makeSequence(BerlinModelImportState state, Team team, int teamId, ResultSet rsSequence, Map<String, Person> personMap){
		try {
			if (rsSequence.isBeforeFirst()){
				rsSequence.next();
			}
			if (rsSequence.isAfterLast()){
				return true;
			}
			int sequenceTeamFk;
			try {
				sequenceTeamFk = rsSequence.getInt("AuthorTeamFk");
			} catch (SQLException e) {
				if (rsSequence.next() == false){
					return true;
				}else{
					throw e;
				}
			}
			while (sequenceTeamFk < teamId){
				if (! state.getConfig().isEuroMed()){
				    logger.warn("Sequence team FK is smaller then team ID. Some teams for a sequence may not be available");
				}
				rsSequence.next();
				sequenceTeamFk = rsSequence.getInt("AuthorTeamFk");
			}
			while (sequenceTeamFk == teamId){
				int authorFk = rsSequence.getInt("AuthorFk");
				Person author = personMap.get(String.valueOf(authorFk));
				if (author != null){
				team.addTeamMember(author);
				}else{
					logger.error("Author " + authorFk + " was not found for team " + teamId);
				}
				if (rsSequence.next()){
					sequenceTeamFk = rsSequence.getInt("AuthorTeamFk");
				}else{
					break;
				}
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelAuthorTeamImportValidator();
		return validator.validate(state);
	}

	@Override
    protected boolean isIgnore(BerlinModelImportState state){
		return ! state.getConfig().isDoAuthors();
	}
}
