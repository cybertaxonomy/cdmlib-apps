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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import eu.etaxonomy.cdm.io.berlinModel.in.validation.BerlinModelAuthorImportValidator;
import eu.etaxonomy.cdm.io.common.IOValidator;
import eu.etaxonomy.cdm.io.common.ImportHelper;
import eu.etaxonomy.cdm.io.common.ResultSetPartitioner;
import eu.etaxonomy.cdm.model.agent.Person;
import eu.etaxonomy.cdm.model.common.CdmBase;
import eu.etaxonomy.cdm.model.common.Extension;
import eu.etaxonomy.cdm.model.common.ExtensionType;
import eu.etaxonomy.cdm.model.common.TimePeriod;
import eu.etaxonomy.cdm.strategy.parser.TimePeriodParser;

/**
 * Supported attributes:
 * - AuthorId, Abbrev, FirstName, LastName, Dates, AreaOfInterest, NomStandard, createUpdateNotes
 *
 * @author a.mueller
 * @since 20.03.2008
 */
@Component
public class BerlinModelAuthorImport extends BerlinModelImportBase {

    private static final long serialVersionUID = 2155984573495140615L;
    private static final Logger logger = LogManager.getLogger();

    private static final boolean BLANK_TO_NULL = true;

	public static final String NAMESPACE = "Author";

//	private static int recordsPerLog = 5000;
	private static final String dbTableName = "Author";
	private static final String pluralString = "Authors";

	public BerlinModelAuthorImport(){
		super(dbTableName, pluralString);
	}

	@Override
	protected String getIdQuery(BerlinModelImportState state) {
	    if (state.getConfig().isEuroMed() && state.getConfig().getAuthorFilter() != null ){
	        //for performance reasons we do not use a subquery
	        return " SELECT authorId "
	               + " FROM v_cdm_exp_authorsAll "
	               + " ORDER BY authorId "
	               ;
	    }else if ("v_cdmExport_authors".equals(state.getConfig().getAuthorFilter())) {
	        //Moose
	        return " SELECT authorId "
            + " FROM v_cdmExport_authors "
            + " ORDER BY authorId "
            ;
	    }

	    String result = " SELECT authorId FROM " + getTableName();
		if (StringUtils.isNotBlank(state.getConfig().getAuthorFilter())){
			result += " WHERE " +  state.getConfig().getAuthorFilter();
		}
		return result;
	}

	@Override
	protected String getRecordQuery(BerlinModelImportConfigurator config) {
		String strRecordQuery =
			" SELECT * " +
            " FROM " + dbTableName + " " +
            " WHERE authorId IN ( " + ID_LIST_TOKEN + " )";
		return strRecordQuery;
	}

	@Override
	public boolean doPartition(ResultSetPartitioner partitioner, BerlinModelImportState state)  {
		String dbAttrName;
		String cdmAttrName;
		Map<Integer, Person> personMap = new HashMap<>();

		boolean success = true;
		ResultSet rs = partitioner.getResultSet();
		try{
			//for each author
			while (rs.next()){

			    //	partitioner.doLogPerLoop(recordsPerLog, pluralString);

				//create Agent element
				int authorId = rs.getInt("AuthorId");

				Person author = Person.NewInstance();

				dbAttrName = "Abbrev";
				cdmAttrName = "nomenclaturalTitle";
				success &= ImportHelper.addStringValue(rs, author, dbAttrName, cdmAttrName, BLANK_TO_NULL);
				//not yet supported by model
				success &= ImportHelper.addStringValue(rs, author, dbAttrName, "originalNomenclaturalTitle", BLANK_TO_NULL);

				dbAttrName = "FirstName";
				cdmAttrName = "givenName";
				success &= ImportHelper.addStringValue(rs, author, dbAttrName, cdmAttrName, BLANK_TO_NULL);

				dbAttrName = "LastName";
				cdmAttrName = "familyName";
				success &= ImportHelper.addStringValue(rs, author, dbAttrName, cdmAttrName, BLANK_TO_NULL);

				String dates = rs.getString("dates");
				if (dates != null){
					dates.trim();
					TimePeriod lifespan = TimePeriodParser.parseString(dates);
					author.setLifespan(lifespan);
				}

			    //AreaOfInterest
				String areaOfInterest = rs.getString("AreaOfInterest");
				if (isNotBlank(areaOfInterest)){
					Extension.NewInstance(author, areaOfInterest, ExtensionType.AREA_OF_INTREREST());
				}

				//nomStandard
				String nomStandard = rs.getString("NomStandard");
				if (isNotBlank(nomStandard)){
					Extension.NewInstance(author, nomStandard, ExtensionType.NOMENCLATURAL_STANDARD());
				}


				//initials
				String initials = null;
				for (int j = 1; j <= rs.getMetaData().getColumnCount(); j++){
					String label = rs.getMetaData().getColumnLabel(j);
					if (label.equalsIgnoreCase("Initials") || label.equalsIgnoreCase("Kürzel")){
						initials = rs.getString(j);
						break;
					}
				}
				if (isNotBlank(initials)){
					author.setInitials(initials);
				}

                String uuid = null;
                if (resultSetHasColumn(rs,"UUID")){
                    uuid = rs.getString("UUID");
                    if (uuid != null){
                        author.setUuid(UUID.fromString(uuid));
                    }
                }

			    //created, notes
				doIdCreatedUpdatedNotes(state, author, rs, authorId, NAMESPACE);

				personMap.put(authorId, author);

			} //while rs.hasNext()
			//logger.info("save " + i + " "+pluralString + " ...");
			getAgentService().save(personMap.values());

		}catch(Exception ex){
			logger.error(ex.getMessage());
			ex.printStackTrace();
			success = false;
		}
		return success;
		}

	@Override
	public Map<Object, Map<String, ? extends CdmBase>> getRelatedObjectsForPartition(ResultSet rs, BerlinModelImportState state)  {
		Map<Object, Map<String, ? extends CdmBase>> result = new HashMap<>();
		// no related objects exist
		return result;
	}

	@Override
	protected boolean doCheck(BerlinModelImportState state){
		IOValidator<BerlinModelImportState> validator = new BerlinModelAuthorImportValidator();
		return validator.validate(state);
	}

	@Override
	protected boolean isIgnore(BerlinModelImportState state){
		return ! state.getConfig().isDoAuthors();
	}

}
