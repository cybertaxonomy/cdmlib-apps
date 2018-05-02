/**
* Copyright (C) 2007 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package eu.etaxonomy.cdm.io.pesi.faunaEuropaea;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import eu.etaxonomy.cdm.io.common.ICdmIO;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.MapWrapper;
import eu.etaxonomy.cdm.io.common.Source;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.agent.Team;
import eu.etaxonomy.cdm.model.agent.TeamOrPersonBase;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.strategy.parser.NonViralNameParserImpl;


/**
 * @author a.babadshanjan
 * @since 12.05.2009
 * @version 1.0
 */
@Component
public class FaunaEuropaeaAuthorImport extends FaunaEuropaeaImportBase {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(FaunaEuropaeaAuthorImport.class);

	private static int modCount = 1000;
	private final static String authorSeparator = ", ";
	private final static String lastAuthorSeparator = " & ";
	private static String capitalWord = "\\p{javaUpperCase}\\p{javaLowerCase}*";
	 protected static String fWs = "\\s*";
	 protected static String oWs = "\\s+";
	 protected static String finalTeamSplitter = "(" + fWs + "(&)" + fWs + "|" + oWs + "et" + oWs + ")";
	protected static String notFinalTeamSplitter = "((?:" + fWs + "," + fWs + ")(?!([A-Z][\\.]))"+"|" + finalTeamSplitter + ")";
	protected static String test = "(, \\s(?!([A-Z].|\\s|$))|,$)" ;
	//protected static String test = "((,\\s("+capitalWord+")+)|(,($|,?!(\\s))))";


	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doCheck(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	@Override
	protected boolean doCheck(FaunaEuropaeaImportState state){
		boolean result = true;
		logger.warn("No checking for Authors not implemented");

		return result;
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#doInvoke(eu.etaxonomy.cdm.io.common.IImportConfigurator, eu.etaxonomy.cdm.api.application.CdmApplicationController, java.util.Map)
	 */
	@Override
	protected void doInvoke(FaunaEuropaeaImportState state){
		/*
		logger.warn("Start author doInvoke");
		ProfilerController.memorySnapshot();
		*/
		if (!state.getConfig().isDoAuthors()){
			return;
		}
		Map<String, MapWrapper<? extends CdmBase>> stores = state.getStores();
		MapWrapper<TeamOrPersonBase<?>> authorStore = (MapWrapper<TeamOrPersonBase<?>>)stores.get(ICdmIO.TEAM_STORE);
		TransactionStatus txStatus = null;

		FaunaEuropaeaImportConfigurator fauEuConfig = state.getConfig();
		Source source = fauEuConfig.getSource();

		String namespace = "AuthorTeam";

		if(logger.isInfoEnabled()) { logger.info("Start making Authors..."); }

		try {

			String strQuery =
				" SELECT *  " +
				" FROM author " ;
			ResultSet rs = source.getResultSet(strQuery) ;

			int i = 0;
			while (rs.next()) {

				if ((i++ % modCount) == 0 && i!= 1 ) {
					if(logger.isDebugEnabled()) {
						logger.debug("Authors retrieved: " + (i-1));
					}
				}

				int authorId = rs.getInt("aut_id");
				String authorName = rs.getString("aut_name");

				String auctWithNecRegEx = "\\bauct\\b\\.?.*\\bnec\\b\\.?.*";
				String necAuctRegEx = "\\bnec\\b\\.?.*\\bauct\\b\\.?.*";

				boolean auctWithNecFound = expressionMatches(auctWithNecRegEx, authorName);
				boolean necAuctFound = expressionMatches(necAuctRegEx, authorName);
				if (auctWithNecFound){
					logger.debug("authorName before auct nec string is removed" + authorName);
					authorName = authorName.substring(expressionEnd("nec\\.?", authorName)+1, authorName.length());
					logger.debug("authorName after auct nec string is removed" + authorName);
				}

				if (necAuctFound){
					logger.debug("authorName before nec auct string is removed" + authorName);
					authorName = authorName.substring(0, authorName.indexOf("nec")-1);
					logger.debug("authorName before nec auct string is removed" + authorName);
				}
				TeamOrPersonBase<?> author = null;

				try {
				    NonViralNameParserImpl parser = NonViralNameParserImpl.NewInstance();

			        if (StringUtils.isNotBlank(authorName)){
			            //author = parser.author(authorName);
			            author = this.parseNomAuthorString(authorName);
    			        ImportHelper.setOriginalSource(author, fauEuConfig.getSourceReference(), authorId, namespace);

    					if (!authorStore.containsId(authorId)) {
    						authorStore.put(authorId, author);
    						if (logger.isDebugEnabled()) { logger.debug("Stored author (" + authorId + ") " + authorName); }
    					} else {
    						logger.warn("Not imported author with duplicated aut_id (" + authorId + ") " + authorName);
    					}
			        }
				} catch (Exception e) {
					logger.warn("An exception occurred when creating author with id " + authorId + ". Author could not be saved." + e.getMessage());
				}
			}

			if(logger.isInfoEnabled()) { logger.info("Saving authors ..."); }

			txStatus = startTransaction();

			// save authors
			getAgentService().save((Collection)authorStore.objects());

			commitTransaction(txStatus);

			if(logger.isInfoEnabled()) { logger.info("End making authors ..."); }

			return;

		} catch (SQLException e) {
			logger.error("SQLException:" +  e);
			state.setUnsuccessfull();
		}
	}

	/* (non-Javadoc)
	 * @see eu.etaxonomy.cdm.io.common.CdmIoBase#isIgnore(eu.etaxonomy.cdm.io.common.IImportConfigurator)
	 */
	@Override
    protected boolean isIgnore(FaunaEuropaeaImportState state){
		return ! state.getConfig().isDoAuthors();
	}

	public static TeamOrPersonBase<?> parseAuthorStringOld(String authorName){
	    TeamOrPersonBase<?> author = null;
	    String[] teamMembers = authorName.split(authorSeparator);
        String lastMember;
        String[] lastMembers;
        Person teamMember;
        if (teamMembers.length>1){
            lastMember = teamMembers[teamMembers.length -1];
            lastMembers = lastMember.split(lastAuthorSeparator);
            teamMembers[teamMembers.length -1] = "";
            author = Team.NewInstance();
            for(String member:teamMembers){
                if (!member.equals("")){
                    teamMember = Person.NewInstance();
                    teamMember.setTitleCache(member, true);
                   ((Team)author).addTeamMember(teamMember);
                }
            }
            if (lastMembers != null){
                for(String member:lastMembers){
                   teamMember = Person.NewInstance();
                   teamMember.setTitleCache(member, true);
                   ((Team)author).addTeamMember(teamMember);
                }
            }

        } else {
            teamMembers = authorName.split(lastAuthorSeparator);
            if (teamMembers.length>1){
                author = Team.NewInstance();
                for(String member:teamMembers){
                  teamMember = Person.NewInstance();
                  teamMember.setTitleCache(member, true);
                  ((Team)author).addTeamMember(teamMember);

                }
            }else{
                author = Person.NewInstance();
                author.setTitleCache(authorName, true);
            }
        }
        author.getTitleCache();
        return author;
	}

    /**
     * @param refAuthor
     * @return
     */
    public static TeamOrPersonBase<?> parseNomAuthorString(String refAuthor) {
        TeamOrPersonBase<?> author = null;
        //possible strings: Lastname, A., Lastname B. & Lastname C.
        //Lastname A, Lastname B & Lastname
        //Lastname A Lastname B & Lastname C
        //Lastname, J & Lastname, L
        String[] firstTeamMembers = refAuthor.split(finalTeamSplitter);
        String[] teamMembers = null;
        String lastMember = null;
        lastMember = firstTeamMembers[firstTeamMembers.length-1];

        if (firstTeamMembers.length == 2){
            teamMembers = firstTeamMembers[0].split(test);
        }
        Person teamMember;
        author = Team.NewInstance();
        if (teamMembers != null){
            for(String member:teamMembers){
                if (!member.trim().equals("")){
                    teamMember = Person.NewInstance();
                    teamMember.setTitleCache(member, true);
                   ((Team)author).addTeamMember(teamMember);
                }
            }
            teamMember = Person.NewInstance();
            teamMember.setTitleCache(lastMember, true);
            ((Team)author).addTeamMember(teamMember);

        }else{
            teamMembers = lastMember.split(test);
            if (teamMembers.length >1){
                for(String member:teamMembers){
                    if (!member.trim().equals("")){
                        teamMember = Person.NewInstance();
                        teamMember.setTitleCache(member, true);
                       ((Team)author).addTeamMember(teamMember);
                    }
                }
            }else{
                author = Person.NewInstance();
                author.setTitleCache(lastMember, true);
            }


        }
        author.getTitleCache();
        return author;
    }

}
